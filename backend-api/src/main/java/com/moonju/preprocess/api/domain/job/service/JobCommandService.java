package com.moonju.preprocess.api.domain.job.service;

import com.moonju.preprocess.api.domain.image.entity.Image;
import com.moonju.preprocess.api.domain.image.entity.ImageStatus;
import com.moonju.preprocess.api.domain.image.repository.ImageRepository;
import com.moonju.preprocess.api.domain.job.dto.JobCreateRequest;
import com.moonju.preprocess.api.domain.job.dto.JobCreateResponse;
import com.moonju.preprocess.api.domain.job.entity.Job;
import com.moonju.preprocess.api.domain.job.entity.JobItem;
import com.moonju.preprocess.api.domain.job.exception.InvalidJobRequestException;
import com.moonju.preprocess.api.domain.job.repository.JobItemRepository;
import com.moonju.preprocess.api.domain.job.repository.JobRepository;
import com.moonju.preprocess.api.domain.preprocess.dto.PresetValidateRequest;
import com.moonju.preprocess.api.domain.preprocess.dto.PresetValidateResponse;
import com.moonju.preprocess.api.domain.preprocess.exception.InvalidPresetParameterException;
import com.moonju.preprocess.api.domain.preprocess.service.PreprocessPresetService;
import com.moonju.preprocess.api.domain.project.service.ProjectPermissionService;
import com.moonju.preprocess.api.infra.rabbitmq.JobMessagePublisher;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class JobCommandService {

    private final JobRepository jobRepository;
    private final JobItemRepository jobItemRepository;
    private final ImageRepository imageRepository;
    private final ProjectPermissionService projectPermissionService;
    private final PreprocessPresetService preprocessPresetService;
    private final JobMessageFactory jobMessageFactory;
    private final JobMessagePublisher jobMessagePublisher;

    public JobCommandService(
        JobRepository jobRepository,
        JobItemRepository jobItemRepository,
        ImageRepository imageRepository,
        ProjectPermissionService projectPermissionService,
        PreprocessPresetService preprocessPresetService,
        JobMessageFactory jobMessageFactory,
        JobMessagePublisher jobMessagePublisher
    ) {
        this.jobRepository = jobRepository;
        this.jobItemRepository = jobItemRepository;
        this.imageRepository = imageRepository;
        this.projectPermissionService = projectPermissionService;
        this.preprocessPresetService = preprocessPresetService;
        this.jobMessageFactory = jobMessageFactory;
        this.jobMessagePublisher = jobMessagePublisher;
    }

    @Transactional
    public JobCreateResponse create(Long currentUserId, JobCreateRequest request) {
        projectPermissionService.validateEditable(request.projectId(), currentUserId);
        List<Long> imageIds = uniqueImageIds(request.imageIds());
        List<Image> images = findImages(request.projectId(), imageIds);
        PresetValidateResponse presetValidation = preprocessPresetService.validate(new PresetValidateRequest(
            request.preset(),
            request.safePresetParameters()
        ));
        if (!presetValidation.valid()) {
            throw new InvalidPresetParameterException(String.join(", ", presetValidation.errors()));
        }

        Job job = jobRepository.save(Job.create(
            request.projectId(),
            currentUserId,
            presetValidation.presetName(),
            presetValidation.resolvedParameters(),
            request.debugEnabled(),
            request.normalizedPriority(),
            images.size()
        ));
        List<JobItem> items = images.stream()
            .map(image -> jobItemRepository.save(JobItem.queued(job.getId(), image.getId())))
            .toList();
        job.markQueued(items.size());
        publishMessages(job, items, images);
        return JobCreateResponse.from(job);
    }

    private List<Long> uniqueImageIds(List<Long> imageIds) {
        List<Long> uniqueImageIds = new LinkedHashSet<>(imageIds).stream().toList();
        if (uniqueImageIds.size() != imageIds.size()) {
            throw new InvalidJobRequestException("Duplicate imageIds are not allowed.");
        }
        return uniqueImageIds;
    }

    private List<Image> findImages(Long projectId, List<Long> imageIds) {
        List<Image> images = imageRepository.findAllByProjectIdAndIdInAndStatusNot(
            projectId,
            imageIds,
            ImageStatus.DELETED
        );
        if (images.size() != imageIds.size()) {
            throw new InvalidJobRequestException("Some images do not belong to the project or do not exist.");
        }
        Map<Long, Image> imagesById = images.stream()
            .collect(java.util.stream.Collectors.toMap(Image::getId, image -> image));
        return imageIds.stream().map(imagesById::get).toList();
    }

    private void publishMessages(Job job, List<JobItem> items, List<Image> images) {
        Map<Long, Image> imagesById = images.stream()
            .collect(java.util.stream.Collectors.toMap(Image::getId, image -> image));
        for (JobItem item : items) {
            jobMessagePublisher.publishPreprocess(
                jobMessageFactory.create(job, item, imagesById.get(item.getImageId()))
            );
        }
    }
}
