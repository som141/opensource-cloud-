package com.moonju.preprocess.worker.domain.report.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.moonju.preprocess.worker.domain.report.dto.ProcessingReport;
import com.moonju.preprocess.worker.domain.report.dto.ProcessingReportJson;
import java.nio.charset.StandardCharsets;
import org.springframework.stereotype.Service;

@Service
public class ProcessingReportWriter {

    private final ObjectMapper objectMapper;

    public ProcessingReportWriter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public ProcessingReportWriter() {
        this(new ObjectMapper().findAndRegisterModules());
    }

    public ProcessingReportJson prepareJson(ProcessingReport report) {
        return ProcessingReportJson.from(report);
    }

    public byte[] writeJsonBytes(ProcessingReport report) {
        try {
            return objectMapper.writeValueAsString(prepareJson(report)).getBytes(StandardCharsets.UTF_8);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to write processing report JSON.", exception);
        }
    }
}
