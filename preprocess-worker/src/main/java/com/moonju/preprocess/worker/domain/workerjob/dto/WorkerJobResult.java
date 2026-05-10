package com.moonju.preprocess.worker.domain.workerjob.dto;

import com.moonju.preprocess.worker.domain.workerjob.status.WorkerFailureCode;
import com.moonju.preprocess.worker.domain.workerjob.status.WorkerJobStatus;

public record WorkerJobResult(
    String messageId,
    Long jobId,
    Long itemId,
    WorkerJobStatus status,
    WorkerFailureCode failureCode,
    String message,
    boolean retryable
) {

    public static WorkerJobResult succeeded(PreprocessJobMessage source, String message) {
        return new WorkerJobResult(
            source.messageId(),
            source.jobId(),
            source.itemId(),
            WorkerJobStatus.SUCCEEDED,
            null,
            message,
            false
        );
    }

    public static WorkerJobResult failed(
        PreprocessJobMessage source,
        WorkerFailureCode failureCode,
        String message,
        boolean retryable
    ) {
        return new WorkerJobResult(
            source.messageId(),
            source.jobId(),
            source.itemId(),
            WorkerJobStatus.FAILED,
            failureCode,
            message,
            retryable
        );
    }

    public static WorkerJobResult invalid(String message) {
        return new WorkerJobResult(
            null,
            null,
            null,
            WorkerJobStatus.FAILED,
            WorkerFailureCode.INVALID_MESSAGE,
            message,
            false
        );
    }
}
