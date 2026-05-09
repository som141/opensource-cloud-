package com.moonju.preprocess.api.domain.image.repository;

import com.moonju.preprocess.api.domain.image.entity.ImageArtifact;
import com.moonju.preprocess.api.domain.image.entity.ImageArtifactType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ImageArtifactRepository extends JpaRepository<ImageArtifact, Long> {

    Optional<ImageArtifact> findFirstByImageIdAndTypeOrderByIdDesc(Long imageId, ImageArtifactType type);

    List<ImageArtifact> findAllByImageIdAndTypeOrderByIdAsc(Long imageId, ImageArtifactType type);
}
