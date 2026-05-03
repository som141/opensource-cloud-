package com.moonju.preprocess.api.domain.project.repository;

import com.moonju.preprocess.api.domain.project.entity.ProjectMember;
import com.moonju.preprocess.api.domain.project.entity.ProjectStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectMemberRepository extends JpaRepository<ProjectMember, Long> {

    @EntityGraph(attributePaths = {"project", "project.owner", "user"})
    Optional<ProjectMember> findByProject_IdAndUser_IdAndProject_Status(
        Long projectId,
        Long userId,
        ProjectStatus status
    );

    @EntityGraph(attributePaths = {"project", "project.owner", "user"})
    Page<ProjectMember> findAllByUser_IdAndProject_Status(Long userId, ProjectStatus status, Pageable pageable);

    @EntityGraph(attributePaths = {"project", "project.owner", "user"})
    List<ProjectMember> findAllByProject_IdAndProject_StatusOrderByIdAsc(Long projectId, ProjectStatus status);

    boolean existsByProject_IdAndUser_Id(Long projectId, Long userId);

    long countByProject_IdAndProject_Status(Long projectId, ProjectStatus status);
}
