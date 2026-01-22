package com.sgagestudio.warm_follow_backend.repository;

import com.sgagestudio.warm_follow_backend.model.Template;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface TemplateRepository extends JpaRepository<Template, Long>, JpaSpecificationExecutor<Template> {
    Optional<Template> findByIdAndWorkspaceId(Long id, UUID workspaceId);

    boolean existsByWorkspaceId(UUID workspaceId);
}
