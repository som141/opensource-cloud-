package com.moonju.preprocess.api.domain.job.dto;

import com.moonju.preprocess.api.domain.job.entity.JobPriority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.Map;

public record JobCreateRequest(
    @NotNull
    Long projectId,

    @NotEmpty
    @Size(max = 10_000)
    List<Long> imageIds,

    @NotBlank
    String preset,

    Map<String, String> presetParameters,

    Boolean debug,

    JobPriority priority,

    JobOutputOptionsRequest outputOptions
) {

    public Map<String, String> safePresetParameters() {
        return presetParameters == null ? Map.of() : presetParameters;
    }

    public boolean debugEnabled() {
        return debug != null && debug;
    }

    public JobPriority normalizedPriority() {
        return priority == null ? JobPriority.NORMAL : priority;
    }

    public JobOutputOptionsRequest normalizedOutputOptions() {
        if (outputOptions == null) {
            return new JobOutputOptionsRequest(true, true, true, false);
        }
        return outputOptions;
    }
}
