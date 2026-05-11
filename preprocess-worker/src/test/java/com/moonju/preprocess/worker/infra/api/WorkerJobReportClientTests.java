package com.moonju.preprocess.worker.infra.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.moonju.preprocess.worker.domain.workerjob.dto.JobPriority;
import com.moonju.preprocess.worker.domain.workerjob.dto.PreprocessJobMessage;
import com.moonju.preprocess.worker.domain.workerjob.status.WorkerFailureCode;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class WorkerJobReportClientTests {

    private HttpServer server;

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void postsStartedReportWithWorkerToken() throws IOException {
        AtomicReference<String> token = new AtomicReference<>();
        AtomicReference<String> body = new AtomicReference<>();
        server = server(exchange -> {
            token.set(exchange.getRequestHeaders().getFirst("X-Worker-Token"));
            body.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            exchange.sendResponseHeaders(200, 0);
            exchange.close();
        });
        WorkerJobReportClient client = client(server);

        client.reportStarted(message());

        assertThat(token).hasValue("secret-token");
        assertThat(body.get()).contains("\"workerId\":\"worker-1\"");
        assertThat(body.get()).contains("\"attempt\":1");
    }

    @Test
    void postsFailedReport() throws IOException {
        AtomicReference<String> body = new AtomicReference<>();
        server = server(exchange -> {
            body.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            exchange.sendResponseHeaders(200, 0);
            exchange.close();
        });
        WorkerJobReportClient client = client(server);

        client.reportFailed(message(), WorkerFailureCode.PIPELINE_NOT_IMPLEMENTED, "not implemented", false);

        assertThat(body.get()).contains("\"errorCode\":\"PIPELINE_NOT_IMPLEMENTED\"");
        assertThat(body.get()).contains("\"errorMessage\":\"not implemented\"");
        assertThat(body.get()).contains("\"retryable\":false");
    }

    @Test
    void postsSucceededReportWithArtifactKeys() throws IOException {
        AtomicReference<String> body = new AtomicReference<>();
        server = server(exchange -> {
            body.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            exchange.sendResponseHeaders(200, 0);
            exchange.close();
        });
        WorkerJobReportClient client = client(server);

        client.reportSucceeded(
            message(),
            "processed/3/1/2/processed.png",
            "processed/3/1/2/preview.png",
            "processed/3/1/2/processing-report.json"
        );

        assertThat(body.get()).contains("\"processedObjectKey\":\"processed/3/1/2/processed.png\"");
        assertThat(body.get()).contains("\"previewObjectKey\":\"processed/3/1/2/preview.png\"");
        assertThat(body.get()).contains("\"reportObjectKey\":\"processed/3/1/2/processing-report.json\"");
    }

    @Test
    void throwsWhenBackendReturnsNon2xx() throws IOException {
        server = server(exchange -> {
            exchange.sendResponseHeaders(500, 0);
            exchange.close();
        });
        WorkerJobReportClient client = client(server);

        assertThatThrownBy(() -> client.reportHeartbeat(message()))
            .isInstanceOf(BackendApiReportException.class)
            .hasMessageContaining("status 500");
    }

    private HttpServer server(ExchangeHandler handler) throws IOException {
        HttpServer httpServer = HttpServer.create(new InetSocketAddress(0), 0);
        httpServer.createContext("/internal/v1/jobs/1/items/2/started", handler::handle);
        httpServer.createContext("/internal/v1/jobs/1/items/2/heartbeat", handler::handle);
        httpServer.createContext("/internal/v1/jobs/1/items/2/succeeded", handler::handle);
        httpServer.createContext("/internal/v1/jobs/1/items/2/failed", handler::handle);
        httpServer.start();
        return httpServer;
    }

    private WorkerJobReportClient client(HttpServer httpServer) {
        WorkerInternalApiProperties apiProperties = new WorkerInternalApiProperties();
        apiProperties.setBaseUrl("http://localhost:" + httpServer.getAddress().getPort());
        apiProperties.setWorkerToken("secret-token");
        WorkerRuntimeProperties workerProperties = new WorkerRuntimeProperties();
        workerProperties.setId("worker-1");
        return new WorkerJobReportClient(apiProperties, workerProperties);
    }

    private PreprocessJobMessage message() {
        return new PreprocessJobMessage(
            "msg",
            1L,
            2L,
            3L,
            4L,
            5L,
            "originals/scan.png",
            "A4_SCAN_300DPI",
            Map.of(),
            false,
            JobPriority.NORMAL,
            1,
            "trace",
            Instant.parse("2026-05-10T00:00:00Z")
        );
    }

    @FunctionalInterface
    private interface ExchangeHandler {

        void handle(HttpExchange exchange) throws IOException;
    }
}
