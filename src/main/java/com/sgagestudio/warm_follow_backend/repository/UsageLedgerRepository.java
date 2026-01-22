package com.sgagestudio.warm_follow_backend.repository;

import com.sgagestudio.warm_follow_backend.model.UsageLedger;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UsageLedgerRepository extends JpaRepository<UsageLedger, UUID> {
    Optional<UsageLedger> findByWorkspaceIdAndPeriod(UUID workspaceId, String period);
}
