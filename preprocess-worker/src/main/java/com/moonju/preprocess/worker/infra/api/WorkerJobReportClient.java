package com.moonju.preprocess.worker.infra.api;

import com.moonju.preprocess.worker.domain.workerjob.dto.PreprocessJobMessage;
import com.moonju.preprocess.worker.domain.workerjob.status.WorkerFailureCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class WorkerJobReportClient implements BackendApiClient {

    private static final Logger log = LoggerFactory.getLogger(WorkerJobReportClient.class);

    private final WorkerInternalApiProperties properties;

    public WorkerJobReportClient(WorkerInternalApiProperties properties) {
        this.properties = properties;
    }

    @Override
    public void reportStarted(PreprocessJobMessage message) {
        log.info(
            "Worker reportStarted skeleton: baseUrl={}, jobId={}, itemId={}",
            properties.getBaseUrl(),
            message.jobId(),
            message.itemId()
        );
    }

    @Override
    public void reportFailed(PreprocessJobMessage message, WorkerFailureCode failureCode, String failureMessage) {
        log.info(
            "Worker reportFailed skeleton: baseUrl={}, jobId={}, itemId={}, code={}, message={}",
            properties.getBaseUrl(),
            message.jobId(),
            message.itemId(),
            failureCode,
            failureMessage
        );
    }
}
