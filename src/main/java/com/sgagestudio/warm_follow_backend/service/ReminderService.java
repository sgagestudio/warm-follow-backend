package com.sgagestudio.warm_follow_backend.service;

import com.sgagestudio.warm_follow_backend.audit.AuditService;
import com.sgagestudio.warm_follow_backend.dto.PagedResponse;
import com.sgagestudio.warm_follow_backend.dto.ReminderCreateRequest;
import com.sgagestudio.warm_follow_backend.dto.ReminderResponse;
import com.sgagestudio.warm_follow_backend.dto.ReminderUpdateRequest;
import com.sgagestudio.warm_follow_backend.model.Channel;
import com.sgagestudio.warm_follow_backend.model.Customer;
import com.sgagestudio.warm_follow_backend.model.Reminder;
import com.sgagestudio.warm_follow_backend.model.ReminderFrequency;
import com.sgagestudio.warm_follow_backend.model.ReminderRecipient;
import com.sgagestudio.warm_follow_backend.model.ReminderRecipientId;
import com.sgagestudio.warm_follow_backend.model.ReminderStatus;
import com.sgagestudio.warm_follow_backend.model.Template;
import com.sgagestudio.warm_follow_backend.model.Transaction;
import com.sgagestudio.warm_follow_backend.model.TransactionStatus;
import com.sgagestudio.warm_follow_backend.model.User;
import com.sgagestudio.warm_follow_backend.repository.CustomerRepository;
import com.sgagestudio.warm_follow_backend.repository.ReminderRecipientRepository;
import com.sgagestudio.warm_follow_backend.repository.ReminderRepository;
import com.sgagestudio.warm_follow_backend.repository.TemplateRepository;
import com.sgagestudio.warm_follow_backend.repository.TransactionRepository;
import com.sgagestudio.warm_follow_backend.repository.UserRepository;
import com.sgagestudio.warm_follow_backend.security.SecurityUtils;
import com.sgagestudio.warm_follow_backend.util.ApiException;
import com.sgagestudio.warm_follow_backend.util.OffsetPageRequest;
import com.sgagestudio.warm_follow_backend.util.RequestContextHolder;
import jakarta.persistence.criteria.Predicate;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ReminderService {
    private final ReminderRepository reminderRepository;
    private final TemplateRepository templateRepository;
    private final CustomerRepository customerRepository;
    private final ReminderRecipientRepository reminderRecipientRepository;
    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;
    private final AuditService auditService;
    private final SecurityUtils securityUtils;
    private final ContactPolicyService contactPolicyService;
    private final LegalTermsService legalTermsService;
    private final TimeService timeService;

    public ReminderService(
            ReminderRepository reminderRepository,
            TemplateRepository templateRepository,
            CustomerRepository customerRepository,
            ReminderRecipientRepository reminderRecipientRepository,
            TransactionRepository transactionRepository,
            UserRepository userRepository,
            AuditService auditService,
            SecurityUtils securityUtils,
            ContactPolicyService contactPolicyService,
            LegalTermsService legalTermsService,
            TimeService timeService
    ) {
        this.reminderRepository = reminderRepository;
        this.templateRepository = templateRepository;
        this.customerRepository = customerRepository;
        this.reminderRecipientRepository = reminderRecipientRepository;
        this.transactionRepository = transactionRepository;
        this.userRepository = userRepository;
        this.auditService = auditService;
        this.securityUtils = securityUtils;
        this.contactPolicyService = contactPolicyService;
        this.legalTermsService = legalTermsService;
        this.timeService = timeService;
    }

    public PagedResponse<ReminderResponse> listReminders(
            ReminderStatus status,
            Channel channel,
            LocalDate from,
            LocalDate to,
            int limit,
            long offset
    ) {
        UUID ownerId = securityUtils.requireCurrentUserId();
        Pageable pageable = OffsetPageRequest.of(offset, limit, Sort.by("createdAt").descending());
        Specification<Reminder> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("ownerUserId"), ownerId));
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (channel != null) {
                predicates.add(cb.equal(root.get("channel"), channel));
            }
            if (from != null) {
                Instant start = timeService.startOfDay(from);
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), start));
            }
            if (to != null) {
                Instant end = timeService.startOfNextDay(to);
                predicates.add(cb.lessThan(root.get("createdAt"), end));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        Page<Reminder> page = reminderRepository.findAll(spec, pageable);
        List<ReminderResponse> items = page.getContent().stream()
                .map(this::toResponse)
                .toList();
        String nextCursor = page.hasNext() ? String.valueOf(offset + limit) : null;
        return new PagedResponse<>(items, nextCursor);
    }

    public ReminderResponse create(ReminderCreateRequest request) {
        UUID ownerId = securityUtils.requireCurrentUserId();
        legalTermsService.requireAccepted(ownerId);
        String idempotencyKey = RequestContextHolder.get()
                .map(ctx -> ctx.idempotencyKey())
                .orElse(null);
        if (StringUtils.hasText(idempotencyKey)) {
            var existing = transactionRepository.findFirstByIdempotencyKeyAndReminder_OwnerUserId(idempotencyKey, ownerId);
            if (existing.isPresent()) {
                return toResponse(existing.get().getReminder());
            }
        }
        Template template = templateRepository.findByIdAndOwnerUserId(request.template_id(), ownerId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "TEMPLATE_NOT_FOUND", "Template not found"));
        validateTemplateChannel(template, request.channel());

        List<Customer> customers = customerRepository.findAllById(request.customer_ids());
        if (customers.size() != request.customer_ids().size()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "CUSTOMERS_INVALID", "One or more customers not found");
        }
        for (Customer customer : customers) {
            if (!customer.getOwnerUserId().equals(ownerId) || customer.isErased()) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "CUSTOMERS_INVALID", "Customer not owned or erased");
            }
            if (!contactPolicyService.hasConsent(customer, request.channel())) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "CONSENT_REQUIRED", "Customer consent required");
            }
        }

        Reminder reminder = new Reminder();
        reminder.setOwnerUserId(ownerId);
        reminder.setTemplate(template);
        reminder.setChannel(request.channel());
        reminder.setFrequency(request.frequency());
        reminder.setScheduledTime(request.scheduled_time());
        reminder.setScheduledDate(request.scheduled_date());
        Instant nextRun = calculateNextRun(request.frequency(), request.scheduled_date(), request.scheduled_time());
        reminder.setNextRun(nextRun);
        reminder.setStatus(nextRun.isAfter(Instant.now()) ? ReminderStatus.pending : ReminderStatus.active);
        Reminder saved = reminderRepository.save(reminder);

        List<ReminderRecipient> recipients = new ArrayList<>();
        for (Customer customer : customers) {
            ReminderRecipient recipient = new ReminderRecipient();
            recipient.setId(new ReminderRecipientId(saved.getId(), customer.getId()));
            recipient.setReminder(saved);
            recipient.setCustomer(customer);
            recipients.add(recipient);
        }
        reminderRecipientRepository.saveAll(recipients);

        Transaction transaction = new Transaction();
        transaction.setReminder(saved);
        User actor = userRepository.findById(ownerId).orElse(null);
        transaction.setTriggeredBy(actor);
        transaction.setStatus(TransactionStatus.queued);
        transaction.setRequestId(RequestContextHolder.getRequestId());
        transaction.setIdempotencyKey(idempotencyKey);
        transactionRepository.save(transaction);

        ReminderResponse response = toResponse(saved);
        auditService.audit("reminder", saved.getId().toString(), "reminder.create", null, response);
        auditService.audit(
                "transaction",
                transaction.getId().toString(),
                "reminder.create.transaction",
                null,
                java.util.Map.of("status", transaction.getStatus().name(), "reminder_id", saved.getId().toString())
        );
        return response;
    }

    public ReminderResponse get(UUID reminderId) {
        Reminder reminder = findOwnedReminder(reminderId);
        return toResponse(reminder);
    }

    public ReminderResponse update(UUID reminderId, ReminderUpdateRequest request) {
        Reminder reminder = findOwnedReminder(reminderId);
        if (reminder.getStatus() != ReminderStatus.pending && reminder.getStatus() != ReminderStatus.active) {
            throw new ApiException(HttpStatus.CONFLICT, "REMINDER_NOT_EDITABLE", "Reminder cannot be updated");
        }
        ReminderResponse before = toResponse(reminder);
        if (request.template_id() != null) {
            Template template = templateRepository.findByIdAndOwnerUserId(request.template_id(), reminder.getOwnerUserId())
                    .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "TEMPLATE_NOT_FOUND", "Template not found"));
            reminder.setTemplate(template);
        }
        if (request.channel() != null) {
            reminder.setChannel(request.channel());
        }
        if (request.frequency() != null) {
            reminder.setFrequency(request.frequency());
        }
        if (request.scheduled_time() != null) {
            reminder.setScheduledTime(request.scheduled_time());
        }
        if (request.scheduled_date() != null) {
            reminder.setScheduledDate(request.scheduled_date());
        }
        validateTemplateChannel(reminder.getTemplate(), reminder.getChannel());
        Instant nextRun = calculateNextRun(reminder.getFrequency(), reminder.getScheduledDate(), reminder.getScheduledTime());
        reminder.setNextRun(nextRun);
        Reminder saved = reminderRepository.save(reminder);
        ReminderResponse response = toResponse(saved);
        auditService.audit("reminder", saved.getId().toString(), "reminder.update", before, response);
        return response;
    }

    public ReminderResponse cancel(UUID reminderId) {
        Reminder reminder = findOwnedReminder(reminderId);
        ReminderResponse before = toResponse(reminder);
        reminder.setStatus(ReminderStatus.completed);
        reminder.setNextRun(Instant.now());
        Reminder saved = reminderRepository.save(reminder);
        ReminderResponse response = toResponse(saved);
        auditService.audit("reminder", saved.getId().toString(), "reminder.cancel", before, response);
        return response;
    }

    private Reminder findOwnedReminder(UUID reminderId) {
        return reminderRepository.findByIdAndOwnerUserId(reminderId, securityUtils.requireCurrentUserId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "REMINDER_NOT_FOUND", "Reminder not found"));
    }

    private ReminderResponse toResponse(Reminder reminder) {
        long recipientsCount = reminderRecipientRepository.countByReminder_Id(reminder.getId());
        return new ReminderResponse(
                reminder.getId(),
                reminder.getTemplate().getId(),
                reminder.getChannel(),
                reminder.getFrequency(),
                reminder.getScheduledTime(),
                reminder.getScheduledDate(),
                reminder.getNextRun(),
                reminder.getStatus(),
                recipientsCount,
                reminder.getCreatedAt(),
                reminder.getUpdatedAt()
        );
    }

    private Instant calculateNextRun(ReminderFrequency frequency, LocalDate date, java.time.LocalTime time) {
        Instant now = Instant.now();
        LocalDate scheduleDate = date != null ? date : timeService.today();
        ZoneId zoneId = timeService.zoneId();
        ZonedDateTime scheduled = ZonedDateTime.of(scheduleDate, time, zoneId);
        Instant candidate = scheduled.toInstant();
        if (frequency == ReminderFrequency.once) {
            return candidate.isBefore(now) ? now : candidate;
        }
        while (!candidate.isAfter(now)) {
            switch (frequency) {
                case daily -> scheduled = scheduled.plusDays(1);
                case weekly -> scheduled = scheduled.plusWeeks(1);
                case monthly -> scheduled = scheduled.plusMonths(1);
                default -> scheduled = scheduled.plusDays(1);
            }
            candidate = scheduled.toInstant();
        }
        return candidate;
    }

    private void validateTemplateChannel(Template template, Channel requested) {
        if (template.getChannel() == Channel.both) {
            return;
        }
        if (template.getChannel() != requested) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "TEMPLATE_CHANNEL_MISMATCH", "Template channel mismatch");
        }
    }

 
}
