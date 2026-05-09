package com.moonju.preprocess.api.domain.upload.repository;

import com.moonju.preprocess.api.domain.upload.entity.UploadSessionFile;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UploadSessionFileRepository extends JpaRepository<UploadSessionFile, Long> {

    boolean existsByProjectIdAndChecksumSha256(Long projectId, String checksumSha256);

    long countByUploadSessionId(Long uploadSessionId);

    @Query("""
        select coalesce(sum(file.sizeBytes), 0)
        from UploadSessionFile file
        where file.uploadSessionId = :uploadSessionId
        """)
    long sumSizeBytesByUploadSessionId(@Param("uploadSessionId") Long uploadSessionId);

    List<UploadSessionFile> findByUploadSessionId(Long uploadSessionId);

    List<UploadSessionFile> findByUploadSessionIdAndIdIn(Long uploadSessionId, Collection<Long> ids);
}
