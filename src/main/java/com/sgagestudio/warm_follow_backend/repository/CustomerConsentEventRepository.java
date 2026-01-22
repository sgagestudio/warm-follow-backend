package com.sgagestudio.warm_follow_backend.repository;

import com.sgagestudio.warm_follow_backend.model.CustomerConsentEvent;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerConsentEventRepository extends JpaRepository<CustomerConsentEvent, UUID> {
    List<CustomerConsentEvent> findByCustomer_IdAndWorkspaceIdOrderByCreatedAtDesc(UUID customerId, UUID workspaceId);
}
