package com.sgagestudio.warm_follow_backend.repository;

import com.sgagestudio.warm_follow_backend.model.ReminderRecipient;
import com.sgagestudio.warm_follow_backend.model.ReminderRecipientId;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReminderRecipientRepository extends JpaRepository<ReminderRecipient, ReminderRecipientId> {
    List<ReminderRecipient> findByReminder_Id(UUID reminderId);

    long countByReminder_Id(UUID reminderId);
}
