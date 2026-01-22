package com.sgagestudio.warm_follow_backend.repository;

import com.sgagestudio.warm_follow_backend.model.Reminder;
import com.sgagestudio.warm_follow_backend.model.ReminderStatus;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ReminderRepository extends JpaRepository<Reminder, UUID>, JpaSpecificationExecutor<Reminder> {
    Optional<Reminder> findByIdAndWorkspaceId(UUID id, UUID workspaceId);

    boolean existsByTemplate_IdAndWorkspaceIdAndStatusIn(Long templateId, UUID workspaceId, Collection<ReminderStatus> statuses);

    boolean existsByWorkspaceIdAndStatus(UUID workspaceId, ReminderStatus status);

    List<Reminder> findByStatusInAndNextRunLessThanEqual(Collection<ReminderStatus> statuses, Instant nextRun);
}
