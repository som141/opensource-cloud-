package com.moonju.preprocess.api.domain.preprocess.repository;

import com.moonju.preprocess.api.domain.preprocess.entity.CustomPreprocessPreset;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomPreprocessPresetRepository extends JpaRepository<CustomPreprocessPreset, Long> {

    List<CustomPreprocessPreset> findAllByUserIdAndDeletedFalseOrderByIdDesc(Long userId);

    Optional<CustomPreprocessPreset> findByIdAndUserIdAndDeletedFalse(Long id, Long userId);
}
