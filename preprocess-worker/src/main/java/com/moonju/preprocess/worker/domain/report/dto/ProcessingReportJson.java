package com.moonju.preprocess.worker.domain.report.dto;

public record ProcessingReportJson(
    String schemaVersion,
    String fileName,
    ProcessingReport report
) {

    public static ProcessingReportJson from(ProcessingReport report) {
        return new ProcessingReportJson("1.0", "processing-report.json", report);
    }
}
