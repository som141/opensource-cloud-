package com.moonju.preprocess.worker.domain.report.service;

import com.moonju.preprocess.worker.domain.report.dto.ProcessingReport;
import com.moonju.preprocess.worker.domain.report.dto.ProcessingReportJson;
import org.springframework.stereotype.Service;

@Service
public class ProcessingReportWriter {

    public ProcessingReportJson prepareJson(ProcessingReport report) {
        return ProcessingReportJson.from(report);
    }
}
