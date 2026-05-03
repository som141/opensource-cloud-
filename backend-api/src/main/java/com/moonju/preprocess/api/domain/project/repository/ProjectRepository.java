package com.moonju.preprocess.api.domain.project.repository;

import com.moonju.preprocess.api.domain.project.entity.Project;
import com.moonju.preprocess.api.domain.project.entity.ProjectStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectRepository extends JpaRepository<Project, Long> {

    List<Project> findByOwnerIdAndStatus(Long ownerId, ProjectStatus status);

    Optional<Project> findByIdAndStatus(Long id, ProjectStatus status);
}
