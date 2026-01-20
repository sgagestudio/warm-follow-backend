package com.sgagestudio.warm_follow_backend.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

@Embeddable
public class ReminderRecipientId implements Serializable {
    @Column(name = "reminder_id")
    private UUID reminderId;

    @Column(name = "customer_id")
    private UUID customerId;

    public ReminderRecipientId() {
    }

    public ReminderRecipientId(UUID reminderId, UUID customerId) {
        this.reminderId = reminderId;
        this.customerId = customerId;
    }

    public UUID getReminderId() {
        return reminderId;
    }

    public void setReminderId(UUID reminderId) {
        this.reminderId = reminderId;
    }

    public UUID getCustomerId() {
        return customerId;
    }

    public void setCustomerId(UUID customerId) {
        this.customerId = customerId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ReminderRecipientId that = (ReminderRecipientId) o;
        return Objects.equals(reminderId, that.reminderId)
                && Objects.equals(customerId, that.customerId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(reminderId, customerId);
    }
}
