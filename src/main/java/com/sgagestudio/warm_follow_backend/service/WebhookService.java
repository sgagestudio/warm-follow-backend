package com.sgagestudio.warm_follow_backend.service;

import com.sgagestudio.warm_follow_backend.audit.AuditService;
import com.sgagestudio.warm_follow_backend.config.WebhookProperties;
import com.sgagestudio.warm_follow_backend.dto.WebhookStatusRequest;
import com.sgagestudio.warm_follow_backend.model.Delivery;
import com.sgagestudio.warm_follow_backend.model.DeliveryStatus;
import com.sgagestudio.warm_follow_backend.model.Transaction;
import com.sgagestudio.warm_follow_backend.model.TransactionStatus;
import com.sgagestudio.warm_follow_backend.repository.DeliveryRepository;
import com.sgagestudio.warm_follow_backend.repository.TransactionRepository;
import com.sgagestudio.warm_follow_backend.util.ApiException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class WebhookService {
    private final DeliveryRepository deliveryRepository;
    private final TransactionRepository transactionRepository;
    private final WebhookProperties webhookProperties;
    private final AuditService auditService;

    public WebhookService(
            DeliveryRepository deliveryRepository,
            TransactionRepository transactionRepository,
            WebhookProperties webhookProperties,
            AuditService auditService
    ) {
        this.deliveryRepository = deliveryRepository;
        this.transactionRepository = transactionRepository;
        this.webhookProperties = webhookProperties;
        this.auditService = auditService;
    }

    public void handleEmailStatus(WebhookStatusRequest request, String signature) {
        verifySignature(signature, webhookProperties.getEmailSecret(), request);
        updateDeliveryStatus(request);
    }

    public void handleSmsStatus(WebhookStatusRequest request, String signature) {
        verifySignature(signature, webhookProperties.getSmsSecret(), request);
        updateDeliveryStatus(request);
    }

    private void updateDeliveryStatus(WebhookStatusRequest request) {
        Delivery delivery = deliveryRepository.findByProviderMessageId(request.provider_message_id())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "DELIVERY_NOT_FOUND", "Delivery not found"));
        DeliveryStatus beforeStatus = delivery.getStatus();
        delivery.setStatus(request.status());
        delivery.setErrorCode(request.error_code());
        delivery.setErrorMessage(request.error_message());
        deliveryRepository.save(delivery);
        auditService.audit("delivery", delivery.getId().toString(), "webhook.update", beforeStatus, request.status());

        Transaction transaction = delivery.getTransaction();
        updateTransactionStatus(transaction);
    }

    private void updateTransactionStatus(Transaction transaction) {
        List<DeliveryRepository.DeliveryStatusCount> counts = deliveryRepository.countByTransactionId(transaction.getId());
        boolean anyFailed = counts.stream().anyMatch(row -> row.getStatus() == DeliveryStatus.failed);
        boolean anyBounced = counts.stream().anyMatch(row -> row.getStatus() == DeliveryStatus.bounced);
        TransactionStatus newStatus = (anyFailed || anyBounced) ? TransactionStatus.failed : TransactionStatus.done;
        transaction.setStatus(newStatus);
        transaction.setFinishedAt(Instant.now());
        transactionRepository.save(transaction);
        auditService.audit("transaction", transaction.getId().toString(), "transaction.webhook_update", null, newStatus);
    }

    private void verifySignature(String signature, String secret, WebhookStatusRequest request) {
        if (secret == null || secret.isBlank()) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "WEBHOOK_SECRET_MISSING", "Webhook secret not configured");
        }
        if (signature == null || signature.isBlank()) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "WEBHOOK_SIGNATURE_MISSING", "Webhook signature missing");
        }
        String payload = request.provider_message_id() + ":" + request.status().name()
                + ":" + nullToEmpty(request.error_code()) + ":" + nullToEmpty(request.error_message());
        String expected = hmacSha256(secret, payload);
        if (!expected.equals(signature)) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "WEBHOOK_SIGNATURE_INVALID", "Invalid webhook signature");
        }
    }

    private String hmacSha256(String secret, String payload) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] hash = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            return java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
        } catch (Exception ex) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "WEBHOOK_SIGNATURE_ERROR", "Unable to verify signature");
        }
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
