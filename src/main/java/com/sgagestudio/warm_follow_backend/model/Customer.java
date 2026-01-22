package com.sgagestudio.warm_follow_backend.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "customers")
public class Customer {
    @Id
    @UuidGenerator
    private UUID id;

    @Column(name = "workspace_id", nullable = false)
    private UUID workspaceId;

    @Column(name = "owner_user_id", nullable = false)
    private UUID ownerUserId;

    @Column(name = "first_name")
    private String firstName;

    @Column(name = "last_name")
    private String lastName;

    private String email;

    private String phone;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "tags", columnDefinition = "text[]", nullable = false)
    private String[] tags;

    private String locale;

    private String timezone;

    @Enumerated(EnumType.STRING)
    @Column(name = "consent_status", nullable = false)
    private ConsentStatus consentStatus;

    @Column(name = "consent_date")
    private LocalDate consentDate;

    @Column(name = "consent_source", nullable = false)
    private String consentSource;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "consent_channels", columnDefinition = "text[]", nullable = false)
    private String[] consentChannels;

    @Column(name = "consent_proof_ref")
    private String consentProofRef;

    @Column(name = "do_not_email", nullable = false)
    private boolean doNotEmail;

    @Column(name = "do_not_sms", nullable = false)
    private boolean doNotSms;

    @Column(name = "email_bounce_reason")
    private String emailBounceReason;

    @Column(name = "email_bounce_at")
    private Instant emailBounceAt;

    @Column(name = "email_complaint_at")
    private Instant emailComplaintAt;

    @Column(name = "is_erased", nullable = false)
    private boolean erased;

    @Column(name = "erased_at")
    private Instant erasedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    public void onCreate() {
        Instant now = Instant.now();
        if (createdAt == null) {
            createdAt = now;
        }
        updatedAt = now;
        if (consentChannels == null) {
            consentChannels = new String[] {"email"};
        }
        if (tags == null) {
            tags = new String[] {};
        }
    }

    @PreUpdate
    public void onUpdate() {
        updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getOwnerUserId() {
        return ownerUserId;
    }

    public void setOwnerUserId(UUID ownerUserId) {
        this.ownerUserId = ownerUserId;
    }

    public UUID getWorkspaceId() {
        return workspaceId;
    }

    public void setWorkspaceId(UUID workspaceId) {
        this.workspaceId = workspaceId;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String[] getTags() {
        return tags;
    }

    public void setTags(String[] tags) {
        this.tags = tags;
    }

    public String getLocale() {
        return locale;
    }

    public void setLocale(String locale) {
        this.locale = locale;
    }

    public String getTimezone() {
        return timezone;
    }

    public void setTimezone(String timezone) {
        this.timezone = timezone;
    }

    public ConsentStatus getConsentStatus() {
        return consentStatus;
    }

    public void setConsentStatus(ConsentStatus consentStatus) {
        this.consentStatus = consentStatus;
    }

    public LocalDate getConsentDate() {
        return consentDate;
    }

    public void setConsentDate(LocalDate consentDate) {
        this.consentDate = consentDate;
    }

    public String getConsentSource() {
        return consentSource;
    }

    public void setConsentSource(String consentSource) {
        this.consentSource = consentSource;
    }

    public String[] getConsentChannels() {
        return consentChannels;
    }

    public void setConsentChannels(String[] consentChannels) {
        this.consentChannels = consentChannels;
    }

    public String getConsentProofRef() {
        return consentProofRef;
    }

    public void setConsentProofRef(String consentProofRef) {
        this.consentProofRef = consentProofRef;
    }

    public boolean isDoNotEmail() {
        return doNotEmail;
    }

    public void setDoNotEmail(boolean doNotEmail) {
        this.doNotEmail = doNotEmail;
    }

    public boolean isDoNotSms() {
        return doNotSms;
    }

    public void setDoNotSms(boolean doNotSms) {
        this.doNotSms = doNotSms;
    }

    public String getEmailBounceReason() {
        return emailBounceReason;
    }

    public void setEmailBounceReason(String emailBounceReason) {
        this.emailBounceReason = emailBounceReason;
    }

    public Instant getEmailBounceAt() {
        return emailBounceAt;
    }

    public void setEmailBounceAt(Instant emailBounceAt) {
        this.emailBounceAt = emailBounceAt;
    }

    public Instant getEmailComplaintAt() {
        return emailComplaintAt;
    }

    public void setEmailComplaintAt(Instant emailComplaintAt) {
        this.emailComplaintAt = emailComplaintAt;
    }

    public boolean isErased() {
        return erased;
    }

    public void setErased(boolean erased) {
        this.erased = erased;
    }

    public Instant getErasedAt() {
        return erasedAt;
    }

    public void setErasedAt(Instant erasedAt) {
        this.erasedAt = erasedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
