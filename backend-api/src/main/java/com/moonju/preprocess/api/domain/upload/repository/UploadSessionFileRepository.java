package com.moonju.preprocess.api.domain.upload.repository;

import com.moonju.preprocess.api.domain.upload.entity.UploadSessionFile;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UploadSessionFileRepository extends JpaRepository<UploadSessionFile, Long> {

    boolean existsByProjectIdAndChecksumSha256(Long projectId, String checksumSha256);

    List<UploadSessionFile> findByUploadSessionId(Long uploadSessionId);

    List<UploadSessionFile> findByUploadSessionIdAndIdIn(Long uploadSessionId, Collection<Long> ids);
}
