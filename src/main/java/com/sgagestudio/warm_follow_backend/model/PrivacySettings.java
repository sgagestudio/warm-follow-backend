package com.sgagestudio.warm_follow_backend.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "privacy_settings")
public class PrivacySettings {
    @Id
    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    private User user;

    @Column(name = "data_retention_enabled", nullable = false)
    private boolean dataRetentionEnabled = true;

    @Column(name = "anonymization_enabled", nullable = false)
    private boolean anonymizationEnabled = false;

    @Column(name = "audit_logs_enabled", nullable = false)
    private boolean auditLogsEnabled = true;

    @Column(name = "encryption_enabled", nullable = false)
    private boolean encryptionEnabled = true;

    @Column(name = "retention_days")
    private Integer retentionDays;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    public void onCreate() {
        updatedAt = Instant.now();
    }

    @PreUpdate
    public void onUpdate() {
        updatedAt = Instant.now();
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public boolean isDataRetentionEnabled() {
        return dataRetentionEnabled;
    }

    public void setDataRetentionEnabled(boolean dataRetentionEnabled) {
        this.dataRetentionEnabled = dataRetentionEnabled;
    }

    public boolean isAnonymizationEnabled() {
        return anonymizationEnabled;
    }

    public void setAnonymizationEnabled(boolean anonymizationEnabled) {
        this.anonymizationEnabled = anonymizationEnabled;
    }

    public boolean isAuditLogsEnabled() {
        return auditLogsEnabled;
    }

    public void setAuditLogsEnabled(boolean auditLogsEnabled) {
        this.auditLogsEnabled = auditLogsEnabled;
    }

    public boolean isEncryptionEnabled() {
        return encryptionEnabled;
    }

    public void setEncryptionEnabled(boolean encryptionEnabled) {
        this.encryptionEnabled = encryptionEnabled;
    }

    public Integer getRetentionDays() {
        return retentionDays;
    }

    public void setRetentionDays(Integer retentionDays) {
        this.retentionDays = retentionDays;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
