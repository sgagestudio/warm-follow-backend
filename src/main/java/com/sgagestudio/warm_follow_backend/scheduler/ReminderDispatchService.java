package com.sgagestudio.warm_follow_backend.scheduler;

import com.sgagestudio.warm_follow_backend.audit.AuditService;
import com.sgagestudio.warm_follow_backend.model.Channel;
import com.sgagestudio.warm_follow_backend.model.Delivery;
import com.sgagestudio.warm_follow_backend.model.DeliveryChannel;
import com.sgagestudio.warm_follow_backend.model.DeliveryStatus;
import com.sgagestudio.warm_follow_backend.model.Reminder;
import com.sgagestudio.warm_follow_backend.model.ReminderFrequency;
import com.sgagestudio.warm_follow_backend.model.ReminderRecipient;
import com.sgagestudio.warm_follow_backend.model.ReminderStatus;
import com.sgagestudio.warm_follow_backend.model.Transaction;
import com.sgagestudio.warm_follow_backend.model.TransactionStatus;
import com.sgagestudio.warm_follow_backend.provider.EmailProvider;
import com.sgagestudio.warm_follow_backend.provider.SmsProvider;
import com.sgagestudio.warm_follow_backend.repository.DeliveryRepository;
import com.sgagestudio.warm_follow_backend.repository.ReminderRecipientRepository;
import com.sgagestudio.warm_follow_backend.repository.ReminderRepository;
import com.sgagestudio.warm_follow_backend.repository.TransactionRepository;
import com.sgagestudio.warm_follow_backend.service.ContactPolicyService;
import com.sgagestudio.warm_follow_backend.service.EmailIdentityService;
import com.sgagestudio.warm_follow_backend.service.LegalTermsService;
import com.sgagestudio.warm_follow_backend.service.TimeService;
import com.sgagestudio.warm_follow_backend.service.UnsubscribeService;
import com.sgagestudio.warm_follow_backend.util.RequestContext;
import com.sgagestudio.warm_follow_backend.util.RequestContextHolder;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ReminderDispatchService {
    private final ReminderRepository reminderRepository;
    private final ReminderRecipientRepository reminderRecipientRepository;
    private final TransactionRepository transactionRepository;
    private final DeliveryRepository deliveryRepository;
    private final EmailProvider emailProvider;
    private final SmsProvider smsProvider;
    private final AuditService auditService;
    private final ContactPolicyService contactPolicyService;
    private final LegalTermsService legalTermsService;
    private final UnsubscribeService unsubscribeService;
    private final TimeService timeService;
    private final EmailIdentityService emailIdentityService;

    public ReminderDispatchService(
            ReminderRepository reminderRepository,
            ReminderRecipientRepository reminderRecipientRepository,
            TransactionRepository transactionRepository,
            DeliveryRepository deliveryRepository,
            EmailProvider emailProvider,
            SmsProvider smsProvider,
            AuditService auditService,
            ContactPolicyService contactPolicyService,
            LegalTermsService legalTermsService,
            UnsubscribeService unsubscribeService,
            TimeService timeService,
            EmailIdentityService emailIdentityService
    ) {
        this.reminderRepository = reminderRepository;
        this.reminderRecipientRepository = reminderRecipientRepository;
        this.transactionRepository = transactionRepository;
        this.deliveryRepository = deliveryRepository;
        this.emailProvider = emailProvider;
        this.smsProvider = smsProvider;
        this.auditService = auditService;
        this.contactPolicyService = contactPolicyService;
        this.legalTermsService = legalTermsService;
        this.unsubscribeService = unsubscribeService;
        this.timeService = timeService;
        this.emailIdentityService = emailIdentityService;
    }

    public void dispatchDueReminders() {
        Instant now = Instant.now();
        List<Reminder> dueReminders = reminderRepository.findByStatusInAndNextRunLessThanEqual(
                List.of(ReminderStatus.active, ReminderStatus.pending),
                now
        );
        for (Reminder reminder : dueReminders) {
            String requestId = "scheduler-" + UUID.randomUUID();
            String workspaceId = reminder.getWorkspaceId() != null ? reminder.getWorkspaceId().toString() : null;
            RequestContextHolder.set(new RequestContext(requestId, null, "scheduler", null, workspaceId));
            try {
                processReminder(reminder);
            } finally {
                RequestContextHolder.clear();
            }
        }
    }

    private void processReminder(Reminder reminder) {
        Transaction transaction = new Transaction();
        transaction.setReminder(reminder);
        transaction.setWorkspaceId(reminder.getWorkspaceId());
        transaction.setStatus(TransactionStatus.running);
        transaction.setRequestId(RequestContextHolder.getRequestId());
        transactionRepository.save(transaction);

        if (!legalTermsService.isAccepted(reminder.getOwnerUserId())) {
            transaction.setStatus(TransactionStatus.failed);
            transaction.setFinishedAt(Instant.now());
            transactionRepository.save(transaction);
            auditService.audit(
                    reminder.getWorkspaceId(),
                    "transaction",
                    transaction.getId().toString(),
                    "scheduler.blocked_terms",
                    null,
                    java.util.Map.of("reason", "LEGAL_TERMS_REQUIRED")
            );
            updateReminderAfterRun(reminder);
            return;
        }

        List<ReminderRecipient> recipients = reminderRecipientRepository.findByReminder_Id(reminder.getId());
        List<Delivery> deliveries = new ArrayList<>();
        boolean anyFailed = false;
        for (ReminderRecipient recipient : recipients) {
            if (reminder.getChannel() == Channel.email || reminder.getChannel() == Channel.both) {
                deliveries.add(sendEmail(reminder, transaction, recipient));
            }
            if (reminder.getChannel() == Channel.sms || reminder.getChannel() == Channel.both) {
                deliveries.add(sendSms(reminder, transaction, recipient));
            }
        }
        for (Delivery delivery : deliveries) {
            if (delivery.getStatus() == DeliveryStatus.failed || delivery.getStatus() == DeliveryStatus.bounced) {
                anyFailed = true;
            }
        }
        deliveryRepository.saveAll(deliveries);

        transaction.setStatus(anyFailed ? TransactionStatus.failed : TransactionStatus.done);
        transaction.setFinishedAt(Instant.now());
        transactionRepository.save(transaction);
        auditService.audit(
                reminder.getWorkspaceId(),
                "transaction",
                transaction.getId().toString(),
                "scheduler.run",
                null,
                java.util.Map.of("status", transaction.getStatus().name())
        );

        updateReminderAfterRun(reminder);
    }

    private Delivery sendEmail(Reminder reminder, Transaction transaction, ReminderRecipient recipient) {
        Delivery delivery = new Delivery();
        delivery.setWorkspaceId(reminder.getWorkspaceId());
        delivery.setTransaction(transaction);
        delivery.setCustomer(recipient.getCustomer());
        delivery.setReminderId(reminder.getId());
        delivery.setChannel(DeliveryChannel.email);
        delivery.setQueuedAt(Instant.now());
        if (!contactPolicyService.isContactable(recipient.getCustomer(), DeliveryChannel.email)) {
            delivery.setStatus(DeliveryStatus.failed);
            delivery.setErrorCode("RECIPIENT_OPTOUT");
            delivery.setErrorMessage("Recipient opted out");
            return delivery;
        }
        try {
            String to = recipient.getCustomer().getEmail();
            if (!StringUtils.hasText(to)) {
                throw new IllegalArgumentException("Missing email");
            }
            delivery.setRecipient(to);
            String body = appendUnsubscribeLink(
                    reminder.getTemplate().getContent(),
                    recipient.getCustomer().getId(),
                    DeliveryChannel.email
            );
            EmailIdentityService.EmailIdentity identity = emailIdentityService.resolve(reminder.getOwnerUserId());
            String messageId = emailProvider.sendEmail(
                    new EmailProvider.EmailRequest(
                            to,
                            reminder.getTemplate().getSubject(),
                            body,
                            java.util.Map.of("reminder_id", reminder.getId().toString()),
                            identity.address(),
                            identity.name()
                    )
            );
            delivery.setProviderMessageId(messageId);
            delivery.setStatus(DeliveryStatus.sent);
            delivery.setSentAt(Instant.now());
        } catch (Exception ex) {
            delivery.setStatus(DeliveryStatus.failed);
            delivery.setErrorCode("SEND_FAILED");
            delivery.setErrorMessage(ex.getMessage());
            delivery.setFailedAt(Instant.now());
        }
        return delivery;
    }

    private Delivery sendSms(Reminder reminder, Transaction transaction, ReminderRecipient recipient) {
        Delivery delivery = new Delivery();
        delivery.setWorkspaceId(reminder.getWorkspaceId());
        delivery.setTransaction(transaction);
        delivery.setCustomer(recipient.getCustomer());
        delivery.setReminderId(reminder.getId());
        delivery.setChannel(DeliveryChannel.sms);
        delivery.setQueuedAt(Instant.now());
        if (!contactPolicyService.isContactable(recipient.getCustomer(), DeliveryChannel.sms)) {
            delivery.setStatus(DeliveryStatus.failed);
            delivery.setErrorCode("RECIPIENT_OPTOUT");
            delivery.setErrorMessage("Recipient opted out");
            return delivery;
        }
        try {
            String to = recipient.getCustomer().getPhone();
            if (!StringUtils.hasText(to)) {
                throw new IllegalArgumentException("Missing phone");
            }
            delivery.setRecipient(to);
            String body = appendUnsubscribeLink(
                    reminder.getTemplate().getContent(),
                    recipient.getCustomer().getId(),
                    DeliveryChannel.sms
            );
            String messageId = smsProvider.sendSms(
                    new SmsProvider.SmsRequest(
                            to,
                            body,
                            java.util.Map.of("reminder_id", reminder.getId().toString())
                    )
            );
            delivery.setProviderMessageId(messageId);
            delivery.setStatus(DeliveryStatus.sent);
            delivery.setSentAt(Instant.now());
        } catch (Exception ex) {
            delivery.setStatus(DeliveryStatus.failed);
            delivery.setErrorCode("SEND_FAILED");
            delivery.setErrorMessage(ex.getMessage());
            delivery.setFailedAt(Instant.now());
        }
        return delivery;
    }

    private String appendUnsubscribeLink(String content, UUID customerId, DeliveryChannel channel) {
        String body = content == null ? "" : content;
        String url = unsubscribeService.buildUrl(customerId, channel);
        if (channel == DeliveryChannel.sms) {
            return body + "\nPara dejar de recibir: " + url;
        }
        return body + "\n\nPara dejar de recibir, visita: " + url;
    }

    private void updateReminderAfterRun(Reminder reminder) {
        if (reminder.getFrequency() == ReminderFrequency.once) {
            reminder.setStatus(ReminderStatus.completed);
            reminder.setNextRun(Instant.now());
            reminderRepository.save(reminder);
            auditService.audit(
                    reminder.getWorkspaceId(),
                    "reminder",
                    reminder.getId().toString(),
                    "scheduler.complete",
                    null,
                    java.util.Map.of("status", reminder.getStatus().name(), "next_run", reminder.getNextRun())
            );
            return;
        }
        Instant nextRun = calculateNextRun(reminder);
        reminder.setStatus(ReminderStatus.active);
        reminder.setNextRun(nextRun);
        reminderRepository.save(reminder);
        auditService.audit(
                reminder.getWorkspaceId(),
                "reminder",
                reminder.getId().toString(),
                "scheduler.reschedule",
                null,
                java.util.Map.of("status", reminder.getStatus().name(), "next_run", reminder.getNextRun())
        );
    }

    private Instant calculateNextRun(Reminder reminder) {
        Instant now = Instant.now();
        LocalDate scheduleDate = reminder.getScheduledDate() != null
                ? reminder.getScheduledDate()
                : timeService.today();
        ZoneId zoneId = timeService.zoneId();
        ZonedDateTime scheduled = ZonedDateTime.of(scheduleDate, reminder.getScheduledTime(), zoneId);
        Instant candidate = scheduled.toInstant();
        while (!candidate.isAfter(now)) {
            switch (reminder.getFrequency()) {
                case daily -> scheduled = scheduled.plusDays(1);
                case weekly -> scheduled = scheduled.plusWeeks(1);
                case monthly -> scheduled = scheduled.plusMonths(1);
                default -> scheduled = scheduled.plusDays(1);
            }
            candidate = scheduled.toInstant();
        }
        return candidate;
    }
}
