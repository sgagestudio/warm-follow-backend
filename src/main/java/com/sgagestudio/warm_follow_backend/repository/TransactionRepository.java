package com.sgagestudio.warm_follow_backend.repository;

import com.sgagestudio.warm_follow_backend.model.Transaction;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface TransactionRepository extends JpaRepository<Transaction, UUID>, JpaSpecificationExecutor<Transaction> {
    Optional<Transaction> findFirstByIdempotencyKeyAndReminder_OwnerUserId(String idempotencyKey, UUID ownerUserId);

    Optional<Transaction> findByIdAndReminder_OwnerUserId(UUID id, UUID ownerUserId);
}
