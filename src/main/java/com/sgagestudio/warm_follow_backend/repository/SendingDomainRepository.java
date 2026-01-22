package com.sgagestudio.warm_follow_backend.repository;

import com.sgagestudio.warm_follow_backend.model.SendingDomain;
import com.sgagestudio.warm_follow_backend.model.SendingDomainStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SendingDomainRepository extends JpaRepository<SendingDomain, UUID> {
    boolean existsByWorkspaceIdAndStatus(UUID workspaceId, SendingDomainStatus status);

    boolean existsByWorkspaceIdAndDomainIgnoreCase(UUID workspaceId, String domain);

    Optional<SendingDomain> findByIdAndWorkspaceId(UUID id, UUID workspaceId);

    List<SendingDomain> findByWorkspaceIdOrderByCreatedAtDesc(UUID workspaceId);
}
