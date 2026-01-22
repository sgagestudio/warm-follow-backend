package com.sgagestudio.warm_follow_backend.repository;

import com.sgagestudio.warm_follow_backend.model.Transaction;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface TransactionRepository extends JpaRepository<Transaction, UUID>, JpaSpecificationExecutor<Transaction> {
    Optional<Transaction> findFirstByIdempotencyKeyAndWorkspaceId(String idempotencyKey, UUID workspaceId);

    Optional<Transaction> findByIdAndWorkspaceId(UUID id, UUID workspaceId);
}
