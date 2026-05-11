package com.moonju.preprocess.worker.domain.preprocess.pipeline;

import com.moonju.preprocess.worker.domain.preprocess.preset.PreprocessPreset;
import com.moonju.preprocess.worker.domain.preprocess.preset.PreprocessPresetRegistry;
import com.moonju.preprocess.worker.domain.preprocess.model.ImageMatHolder;
import com.moonju.preprocess.worker.domain.preprocess.step.PreprocessStep;
import com.moonju.preprocess.worker.domain.preprocess.step.PreprocessStepCatalog;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.function.Consumer;
import org.springframework.stereotype.Service;

@Service
public class PreprocessPipelineRunner {

    private final PreprocessPresetRegistry presetRegistry;
    private final PreprocessStepCatalog stepCatalog;
    private final Clock clock;

    public PreprocessPipelineRunner(PreprocessPresetRegistry presetRegistry, PreprocessStepCatalog stepCatalog) {
        this(presetRegistry, stepCatalog, Clock.systemUTC());
    }

    PreprocessPipelineRunner(
        PreprocessPresetRegistry presetRegistry,
        PreprocessStepCatalog stepCatalog,
        Clock clock
    ) {
        this.presetRegistry = presetRegistry;
        this.stepCatalog = stepCatalog;
        this.clock = clock;
    }

    public PreprocessResult run(PreprocessContext context) {
        return run(context, ignored -> {
        });
    }

    public PreprocessResult run(PreprocessContext context, Consumer<ImageMatHolder> outputImageConsumer) {
        long pipelineStartNanos = System.nanoTime();
        boolean success = true;
        String errorMessage = null;
        try {
            PreprocessPreset preset = presetRegistry.findByName(context.presetName());
            PreprocessPipeline pipeline = PreprocessPipeline.from(preset, stepCatalog);
            for (PreprocessStep step : pipeline.steps()) {
                PreprocessStepExecution execution = executeStep(context, step);
                context.recordStepExecution(execution);
                if (!execution.success()) {
                    success = false;
                    errorMessage = execution.errorMessage();
                    break;
                }
            }
            Duration wallTime = Duration.ofNanos(System.nanoTime() - pipelineStartNanos);
            boolean outputImageAvailable = context.decodedImage()
                .map(ImageMatHolder::loaded)
                .orElse(false);
            if (success && outputImageAvailable) {
                outputImageConsumer.accept(context.decodedImage().orElseThrow());
            }
            return PreprocessResult.from(context, !outputImageAvailable, wallTime, success, errorMessage);
        } finally {
            context.releaseDecodedImage();
        }
    }

    private PreprocessStepExecution executeStep(PreprocessContext context, PreprocessStep step) {
        Instant startedAt = Instant.now(clock);
        long startedNanos = System.nanoTime();
        try {
            step.execute(context);
            Instant endedAt = Instant.now(clock);
            Duration wallTime = Duration.ofNanos(System.nanoTime() - startedNanos);
            return PreprocessStepExecution.succeeded(
                step.name(),
                context.consumeStepNote(step.name()),
                startedAt,
                endedAt,
                wallTime
            );
        } catch (RuntimeException exception) {
            Instant endedAt = Instant.now(clock);
            Duration wallTime = Duration.ofNanos(System.nanoTime() - startedNanos);
            return PreprocessStepExecution.failed(
                step.name(),
                context.consumeStepNote(step.name()),
                startedAt,
                endedAt,
                wallTime,
                exception.getMessage()
            );
        }
    }
}
