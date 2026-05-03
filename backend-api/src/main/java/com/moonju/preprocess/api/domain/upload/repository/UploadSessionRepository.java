package com.moonju.preprocess.api.domain.upload.repository;

import com.moonju.preprocess.api.domain.upload.entity.UploadSession;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UploadSessionRepository extends JpaRepository<UploadSession, Long> {

    Optional<UploadSession> findByIdAndUserId(Long id, Long userId);
}
