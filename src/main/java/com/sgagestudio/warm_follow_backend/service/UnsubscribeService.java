package com.sgagestudio.warm_follow_backend.service;

import com.sgagestudio.warm_follow_backend.audit.AuditService;
import com.sgagestudio.warm_follow_backend.config.UnsubscribeProperties;
import com.sgagestudio.warm_follow_backend.model.ConsentStatus;
import com.sgagestudio.warm_follow_backend.model.Customer;
import com.sgagestudio.warm_follow_backend.model.CustomerConsentEvent;
import com.sgagestudio.warm_follow_backend.model.DeliveryChannel;
import com.sgagestudio.warm_follow_backend.repository.CustomerConsentEventRepository;
import com.sgagestudio.warm_follow_backend.repository.CustomerRepository;
import com.sgagestudio.warm_follow_backend.util.ApiException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Base64;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class UnsubscribeService {
    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final String TOKEN_SEPARATOR = "\\.";
    private static final String PAYLOAD_SEPARATOR = "\\|";

    private final UnsubscribeProperties properties;
    private final CustomerRepository customerRepository;
    private final CustomerConsentEventRepository consentEventRepository;
    private final AuditService auditService;

    public UnsubscribeService(
            UnsubscribeProperties properties,
            CustomerRepository customerRepository,
            CustomerConsentEventRepository consentEventRepository,
            AuditService auditService
    ) {
        this.properties = properties;
        this.customerRepository = customerRepository;
        this.consentEventRepository = consentEventRepository;
        this.auditService = auditService;
    }

    public String buildUrl(UUID customerId, DeliveryChannel channel) {
        String baseUrl = properties.getNormalizedBaseUrl();
        if (!StringUtils.hasText(baseUrl)) {
            throw new IllegalStateException("Unsubscribe base URL not configured");
        }
        String token = generateToken(customerId, channel);
        return baseUrl + "/unsubscribe/" + token;
    }

    public void unsubscribe(String token) {
        Payload payload = parseAndVerify(token);
        Customer customer = customerRepository.findById(payload.customerId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "CUSTOMER_NOT_FOUND", "Customer not found"));
        if (customer.getConsentStatus() == ConsentStatus.revoked) {
            return;
        }
        customer.setConsentStatus(ConsentStatus.revoked);
        customer.setConsentDate(LocalDate.now());
        customer.setConsentSource("unsubscribe");
        customer.setConsentChannels(new String[] {});
        customer.setConsentProofRef("unsubscribe:" + payload.channel().name().toLowerCase(Locale.ROOT));
        customerRepository.save(customer);

        CustomerConsentEvent event = new CustomerConsentEvent();
        event.setCustomer(customer);
        event.setStatus(ConsentStatus.revoked);
        event.setChannels(new String[] {payload.channel().name().toLowerCase(Locale.ROOT)});
        event.setSource("unsubscribe");
        consentEventRepository.save(event);

        auditService.audit(
                "customer",
                customer.getId().toString(),
                "customer.unsubscribe",
                null,
                Map.of("channel", payload.channel().name().toLowerCase(Locale.ROOT))
        );
    }

    private String generateToken(UUID customerId, DeliveryChannel channel) {
        String nonce = UUID.randomUUID().toString().replace("-", "");
        long issuedAt = Instant.now().getEpochSecond();
        String payload = customerId + "|" + channel.name().toLowerCase(Locale.ROOT) + "|" + issuedAt + "|" + nonce;
        String payloadEncoded = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(payload.getBytes(StandardCharsets.UTF_8));
        String signature = sign(payload);
        return payloadEncoded + "." + signature;
    }

    private Payload parseAndVerify(String token) {
        if (!StringUtils.hasText(token)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "UNSUBSCRIBE_TOKEN_INVALID", "Invalid unsubscribe token");
        }
        String[] parts = token.split(TOKEN_SEPARATOR);
        if (parts.length != 2) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "UNSUBSCRIBE_TOKEN_INVALID", "Invalid unsubscribe token");
        }
        String payloadEncoded = parts[0];
        String signature = parts[1];
        String payload;
        try {
            payload = new String(Base64.getUrlDecoder().decode(payloadEncoded), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException ex) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "UNSUBSCRIBE_TOKEN_INVALID", "Invalid unsubscribe token");
        }
        if (!verify(payload, signature)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "UNSUBSCRIBE_TOKEN_INVALID", "Invalid unsubscribe token");
        }
        String[] fields = payload.split(PAYLOAD_SEPARATOR);
        if (fields.length < 4) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "UNSUBSCRIBE_TOKEN_INVALID", "Invalid unsubscribe token");
        }
        UUID customerId;
        DeliveryChannel channel;
        try {
            customerId = UUID.fromString(fields[0]);
            channel = DeliveryChannel.valueOf(fields[1].toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "UNSUBSCRIBE_TOKEN_INVALID", "Invalid unsubscribe token");
        }
        long issuedAt;
        try {
            issuedAt = Long.parseLong(fields[2]);
        } catch (NumberFormatException ex) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "UNSUBSCRIBE_TOKEN_INVALID", "Invalid unsubscribe token");
        }
        return new Payload(customerId, channel, issuedAt, fields[3]);
    }

    private String sign(String payload) {
        String secret = properties.getSecret();
        if (!StringUtils.hasText(secret)) {
            throw new IllegalStateException("Unsubscribe secret not configured");
        }
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM));
            byte[] signature = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(signature);
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to sign unsubscribe token", ex);
        }
    }

    private boolean verify(String payload, String signature) {
        byte[] expected = Base64.getUrlDecoder().decode(sign(payload));
        byte[] actual;
        try {
            actual = Base64.getUrlDecoder().decode(signature);
        } catch (IllegalArgumentException ex) {
            return false;
        }
        return MessageDigest.isEqual(expected, actual);
    }

    private record Payload(UUID customerId, DeliveryChannel channel, long issuedAt, String nonce) {
    }
}
