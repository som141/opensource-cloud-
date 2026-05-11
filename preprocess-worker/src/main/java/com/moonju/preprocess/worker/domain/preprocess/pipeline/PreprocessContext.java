package com.moonju.preprocess.worker.domain.preprocess.pipeline;

import com.moonju.preprocess.worker.domain.preprocess.model.DebugArtifactDescriptor;
import com.moonju.preprocess.worker.domain.preprocess.model.DebugArtifactSnapshot;
import com.moonju.preprocess.worker.domain.preprocess.model.FallbackNote;
import com.moonju.preprocess.worker.domain.preprocess.model.ImageMatHolder;
import com.moonju.preprocess.worker.domain.preprocess.step.PreprocessStepName;
import com.moonju.preprocess.worker.domain.workerjob.dto.PreprocessJobMessage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class PreprocessContext {

    private final Long jobId;
    private final Long projectId;
    private final Long itemId;
    private final String originalObjectKey;
    private final String presetName;
    private final Map<String, String> parameters;
    private final boolean debug;
    private final List<PreprocessStepExecution> stepExecutions = new ArrayList<>();
    private final List<FallbackNote> fallbackNotes = new ArrayList<>();
    private final List<DebugArtifactDescriptor> debugArtifacts = new ArrayList<>();
    private final List<DebugArtifactSnapshot> debugSnapshots = new ArrayList<>();
    private final Map<PreprocessStepName, String> stepNotes = new EnumMap<>(PreprocessStepName.class);
    private byte[] sourceImageBytes;
    private ImageMatHolder decodedImage;

    public PreprocessContext(
        Long jobId,
        Long itemId,
        String originalObjectKey,
        String presetName,
        Map<String, String> parameters,
        boolean debug
    ) {
        this(null, jobId, itemId, originalObjectKey, presetName, parameters, debug);
    }

    public PreprocessContext(
        Long projectId,
        Long jobId,
        Long itemId,
        String originalObjectKey,
        String presetName,
        Map<String, String> parameters,
        boolean debug
    ) {
        this.projectId = projectId;
        this.jobId = jobId;
        this.itemId = itemId;
        this.originalObjectKey = originalObjectKey;
        this.presetName = presetName;
        this.parameters = new LinkedHashMap<>(parameters);
        this.debug = debug;
    }

    public static PreprocessContext fromMessage(PreprocessJobMessage message) {
        return new PreprocessContext(
            message.projectId(),
            message.jobId(),
            message.itemId(),
            message.originalObjectKey(),
            message.preset(),
            message.safePresetParameters(),
            message.debug()
        );
    }

    public void recordStep(PreprocessStepName stepName, String note) {
        stepNotes.put(stepName, note);
    }

    public void recordStepExecution(PreprocessStepExecution execution) {
        stepExecutions.add(execution);
    }

    public String consumeStepNote(PreprocessStepName stepName) {
        String note = stepNotes.remove(stepName);
        return note == null ? "Step executed." : note;
    }

    public void recordFallback(PreprocessStepName stepName, String reason, String selectedStrategy) {
        fallbackNotes.add(new FallbackNote(stepName.name(), reason, selectedStrategy));
    }

    public void recordDebugArtifact(PreprocessStepName stepName, String fileName) {
        if (!debug) {
            return;
        }
        debugArtifacts.add(DebugArtifactDescriptor.image(stepName, projectId, jobId, itemId, fileName));
    }

    public void recordDebugSnapshot(PreprocessStepName stepName, String fileName, ImageMatHolder imageMatHolder) {
        if (!debug || imageMatHolder == null || !imageMatHolder.loaded()) {
            return;
        }
        DebugArtifactSnapshot snapshot = DebugArtifactSnapshot.image(
            stepName,
            projectId,
            jobId,
            itemId,
            fileName,
            imageMatHolder.mat()
        );
        debugSnapshots.add(snapshot);
        debugArtifacts.add(snapshot.descriptor());
    }

    public PreprocessContext withSourceImageBytes(byte[] imageBytes) {
        if (imageBytes == null) {
            this.sourceImageBytes = null;
            return this;
        }
        this.sourceImageBytes = Arrays.copyOf(imageBytes, imageBytes.length);
        return this;
    }

    public boolean hasSourceImageBytes() {
        return sourceImageBytes != null && sourceImageBytes.length > 0;
    }

    public byte[] sourceImageBytes() {
        if (sourceImageBytes == null) {
            return new byte[0];
        }
        return Arrays.copyOf(sourceImageBytes, sourceImageBytes.length);
    }

    public void storeDecodedImage(ImageMatHolder imageMatHolder) {
        releaseDecodedImage();
        this.decodedImage = imageMatHolder;
    }

    public Optional<ImageMatHolder> decodedImage() {
        return Optional.ofNullable(decodedImage);
    }

    public void releaseDecodedImage() {
        if (decodedImage != null) {
            decodedImage.release();
        }
    }

    public void releaseDebugSnapshots() {
        for (DebugArtifactSnapshot snapshot : debugSnapshots) {
            snapshot.release();
        }
    }

    public Long jobId() {
        return jobId;
    }

    public Long projectId() {
        return projectId;
    }

    public Long itemId() {
        return itemId;
    }

    public String originalObjectKey() {
        return originalObjectKey;
    }

    public String presetName() {
        return presetName;
    }

    public Map<String, String> parameters() {
        return Map.copyOf(parameters);
    }

    public boolean debug() {
        return debug;
    }

    public List<PreprocessStepExecution> stepExecutions() {
        return List.copyOf(stepExecutions);
    }

    public List<FallbackNote> fallbackNotes() {
        return List.copyOf(fallbackNotes);
    }

    public List<DebugArtifactDescriptor> debugArtifacts() {
        return List.copyOf(debugArtifacts);
    }

    public List<DebugArtifactSnapshot> debugSnapshots() {
        return List.copyOf(debugSnapshots);
    }
}
