package com.moonju.preprocess.worker.infra.api;

public class BackendApiReportException extends RuntimeException {

    public BackendApiReportException(String message) {
        super(message);
    }

    public BackendApiReportException(String message, Throwable cause) {
        super(message, cause);
    }
}
