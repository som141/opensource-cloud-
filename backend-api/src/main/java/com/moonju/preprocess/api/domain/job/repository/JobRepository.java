package com.moonju.preprocess.api.domain.job.repository;

import com.moonju.preprocess.api.domain.job.entity.Job;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JobRepository extends JpaRepository<Job, Long> {

    Optional<Job> findByIdAndUserId(Long id, Long userId);

    Page<Job> findAllByUserId(Long userId, Pageable pageable);
}
