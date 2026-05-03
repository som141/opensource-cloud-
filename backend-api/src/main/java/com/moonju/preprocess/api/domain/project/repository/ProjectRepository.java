package com.moonju.preprocess.api.domain.project.repository;

import com.moonju.preprocess.api.domain.project.entity.Project;
import com.moonju.preprocess.api.domain.project.entity.ProjectStatus;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectRepository extends JpaRepository<Project, Long> {

    @EntityGraph(attributePaths = "owner")
    Optional<Project> findByIdAndStatus(Long id, ProjectStatus status);
}
