package com.moonju.preprocess.api.domain.job.service;

import com.moonju.preprocess.api.domain.image.entity.Image;
import com.moonju.preprocess.api.domain.image.entity.ImageStatus;
import com.moonju.preprocess.api.domain.image.repository.ImageRepository;
import com.moonju.preprocess.api.domain.job.dto.JobRetryResponse;
import com.moonju.preprocess.api.domain.job.entity.Job;
import com.moonju.preprocess.api.domain.job.entity.JobItem;
import com.moonju.preprocess.api.domain.job.entity.JobItemStatus;
import com.moonju.preprocess.api.domain.job.repository.JobItemRepository;
import com.moonju.preprocess.api.infra.rabbitmq.JobMessagePublisher;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class JobRetryService {

    private static final List<JobItemStatus> RETRYABLE_STATUSES = List.of(
        JobItemStatus.FAILED,
        JobItemStatus.DEAD_LETTERED
    );

    private final JobQueryService jobQueryService;
    private final JobItemRepository jobItemRepository;
    private final ImageRepository imageRepository;
    private final JobMessageFactory jobMessageFactory;
    private final JobMessagePublisher jobMessagePublisher;

    public JobRetryService(
        JobQueryService jobQueryService,
        JobItemRepository jobItemRepository,
        ImageRepository imageRepository,
        JobMessageFactory jobMessageFactory,
        JobMessagePublisher jobMessagePublisher
    ) {
        this.jobQueryService = jobQueryService;
        this.jobItemRepository = jobItemRepository;
        this.imageRepository = imageRepository;
        this.jobMessageFactory = jobMessageFactory;
        this.jobMessagePublisher = jobMessagePublisher;
    }

    @Transactional
    public JobRetryResponse retryFailed(Long currentUserId, Long jobId) {
        Job job = jobQueryService.findEditableJob(currentUserId, jobId);
        List<JobItem> retryItems = jobItemRepository.findAllByJobIdAndStatusIn(job.getId(), RETRYABLE_STATUSES);
        retryItems.forEach(JobItem::retry);
        job.markRetrying(retryItems.size());
        publishRetryMessages(job, retryItems);
        return JobRetryResponse.of(job, retryItems.size());
    }

    @Transactional
    public JobRetryResponse rerun(Long currentUserId, Long jobId) {
        Job job = jobQueryService.findEditableJob(currentUserId, jobId);
        List<JobItem> items = jobItemRepository.findAllByJobId(job.getId());
        items.forEach(JobItem::rerun);
        List<JobItem> retryItems = items.stream()
            .filter(item -> item.getStatus() == JobItemStatus.RETRYING)
            .toList();
        job.markRetrying(retryItems.size());
        publishRetryMessages(job, retryItems);
        return JobRetryResponse.of(job, retryItems.size());
    }

    private void publishRetryMessages(Job job, List<JobItem> items) {
        Map<Long, Image> imagesById = findImages(job.getProjectId(), items);
        for (JobItem item : items) {
            Image image = imagesById.get(item.getImageId());
            if (image != null) {
                jobMessagePublisher.publishRetry(jobMessageFactory.create(job, item, image));
            }
        }
    }

    private Map<Long, Image> findImages(Long projectId, List<JobItem> items) {
        List<Long> imageIds = items.stream().map(JobItem::getImageId).toList();
        if (imageIds.isEmpty()) {
            return Map.of();
        }
        return imageRepository.findAllByProjectIdAndIdInAndStatusNot(
                projectId,
                imageIds,
                ImageStatus.DELETED
            )
            .stream()
            .collect(java.util.stream.Collectors.toMap(Image::getId, image -> image));
    }
}
