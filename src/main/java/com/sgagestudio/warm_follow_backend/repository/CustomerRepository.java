package com.sgagestudio.warm_follow_backend.repository;

import com.sgagestudio.warm_follow_backend.model.Customer;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface CustomerRepository extends JpaRepository<Customer, UUID>, JpaSpecificationExecutor<Customer> {
    Optional<Customer> findByIdAndWorkspaceId(UUID id, UUID workspaceId);

    List<Customer> findByIdInAndWorkspaceId(List<UUID> ids, UUID workspaceId);

    long countByWorkspaceIdAndErasedFalse(UUID workspaceId);
}
