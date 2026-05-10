package com.moonju.preprocess.api.domain.image.repository;

import com.moonju.preprocess.api.domain.image.entity.Image;
import com.moonju.preprocess.api.domain.image.entity.ImageStatus;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ImageRepository extends JpaRepository<Image, Long> {

    boolean existsByUploadSessionFileId(Long uploadSessionFileId);

    long countByProjectIdAndStatusNot(Long projectId, ImageStatus status);

    Optional<Image> findByIdAndStatusNot(Long id, ImageStatus status);

    Page<Image> findAllByProjectIdAndStatusNot(Long projectId, ImageStatus status, Pageable pageable);

    List<Image> findAllByProjectIdAndIdInAndStatusNot(Long projectId, Collection<Long> ids, ImageStatus status);
}
