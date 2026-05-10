package com.moonju.preprocess.worker.infra.api;

import com.moonju.preprocess.worker.domain.workerjob.dto.PreprocessJobMessage;
import com.moonju.preprocess.worker.domain.workerjob.status.WorkerFailureCode;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import org.springframework.stereotype.Component;

@Component
public class WorkerJobReportClient implements BackendApiClient {

    private final WorkerInternalApiProperties properties;
    private final WorkerRuntimeProperties workerProperties;
    private final HttpClient httpClient;

    public WorkerJobReportClient(WorkerInternalApiProperties properties, WorkerRuntimeProperties workerProperties) {
        this(properties, workerProperties, HttpClient.newHttpClient());
    }

    WorkerJobReportClient(
        WorkerInternalApiProperties properties,
        WorkerRuntimeProperties workerProperties,
        HttpClient httpClient
    ) {
        this.properties = properties;
        this.workerProperties = workerProperties;
        this.httpClient = httpClient;
    }

    @Override
    public void reportStarted(PreprocessJobMessage message) {
        post(
            itemPath(message, "started"),
            "{\"workerId\":" + jsonString(workerProperties.getId()) + ",\"attempt\":" + message.attempt() + "}"
        );
    }

    @Override
    public void reportHeartbeat(PreprocessJobMessage message) {
        post(itemPath(message, "heartbeat"), "{\"workerId\":" + jsonString(workerProperties.getId()) + "}");
    }

    @Override
    public void reportSucceeded(
        PreprocessJobMessage message,
        String processedObjectKey,
        String previewObjectKey,
        String reportObjectKey
    ) {
        post(
            itemPath(message, "succeeded"),
            "{"
                + "\"workerId\":" + jsonString(workerProperties.getId()) + ","
                + "\"processedObjectKey\":" + jsonString(processedObjectKey) + ","
                + "\"previewObjectKey\":" + jsonString(previewObjectKey) + ","
                + "\"reportObjectKey\":" + jsonString(reportObjectKey)
                + "}"
        );
    }

    @Override
    public void reportFailed(
        PreprocessJobMessage message,
        WorkerFailureCode failureCode,
        String failureMessage,
        boolean retryable
    ) {
        post(
            itemPath(message, "failed"),
            "{"
                + "\"workerId\":" + jsonString(workerProperties.getId()) + ","
                + "\"errorCode\":" + jsonString(failureCode.name()) + ","
                + "\"errorMessage\":" + jsonString(failureMessage) + ","
                + "\"retryable\":" + retryable
                + "}"
        );
    }

    @Override
    public void registerArtifacts(
        PreprocessJobMessage message,
        String processedObjectKey,
        String previewObjectKey,
        String reportObjectKey
    ) {
        post(
            itemPath(message, "artifacts"),
            "{"
                + "\"processedObjectKey\":" + jsonString(processedObjectKey) + ","
                + "\"previewObjectKey\":" + jsonString(previewObjectKey) + ","
                + "\"reportObjectKey\":" + jsonString(reportObjectKey)
                + "}"
        );
    }

    private void post(String path, String body) {
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(normalizeBaseUrl() + path))
            .timeout(Duration.ofSeconds(5))
            .header("Content-Type", "application/json")
            .header("X-Worker-Token", properties.getWorkerToken())
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new BackendApiReportException(
                    "Backend internal API returned status " + response.statusCode() + " for " + path
                );
            }
        } catch (IOException exception) {
            throw new BackendApiReportException("Failed to call backend internal API: " + path, exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new BackendApiReportException("Interrupted while calling backend internal API: " + path, exception);
        }
    }

    private String itemPath(PreprocessJobMessage message, String action) {
        return "/internal/v1/jobs/" + message.jobId() + "/items/" + message.itemId() + "/" + action;
    }

    private String normalizeBaseUrl() {
        String baseUrl = properties.getBaseUrl();
        if (baseUrl.endsWith("/")) {
            return baseUrl.substring(0, baseUrl.length() - 1);
        }
        return baseUrl;
    }

    private String jsonString(String value) {
        if (value == null) {
            return "null";
        }
        return "\""
            + value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
            + "\"";
    }
}
