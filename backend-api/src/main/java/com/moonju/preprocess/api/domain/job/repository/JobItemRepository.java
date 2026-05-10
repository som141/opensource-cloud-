package com.moonju.preprocess.api.domain.job.repository;

import com.moonju.preprocess.api.domain.job.entity.JobItem;
import com.moonju.preprocess.api.domain.job.entity.JobItemStatus;
import java.util.Collection;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JobItemRepository extends JpaRepository<JobItem, Long> {

    Page<JobItem> findAllByJobId(Long jobId, Pageable pageable);

    List<JobItem> findAllByJobId(Long jobId);

    List<JobItem> findAllByJobIdAndStatusIn(Long jobId, Collection<JobItemStatus> statuses);
}
