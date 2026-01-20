package com.sgagestudio.warm_follow_backend.service;

import com.sgagestudio.warm_follow_backend.audit.AuditService;
import com.sgagestudio.warm_follow_backend.dto.ConsentEventResponse;
import com.sgagestudio.warm_follow_backend.dto.ConsentUpdateRequest;
import com.sgagestudio.warm_follow_backend.dto.CustomerCreateRequest;
import com.sgagestudio.warm_follow_backend.dto.CustomerResponse;
import com.sgagestudio.warm_follow_backend.dto.CustomerUpdateRequest;
import com.sgagestudio.warm_follow_backend.dto.GdprRequestResponse;
import com.sgagestudio.warm_follow_backend.dto.PagedResponse;
import com.sgagestudio.warm_follow_backend.model.ConsentStatus;
import com.sgagestudio.warm_follow_backend.model.Customer;
import com.sgagestudio.warm_follow_backend.model.CustomerConsentEvent;
import com.sgagestudio.warm_follow_backend.model.GdprRequest;
import com.sgagestudio.warm_follow_backend.model.GdprRequestStatus;
import com.sgagestudio.warm_follow_backend.model.GdprRequestType;
import com.sgagestudio.warm_follow_backend.model.User;
import com.sgagestudio.warm_follow_backend.repository.CustomerConsentEventRepository;
import com.sgagestudio.warm_follow_backend.repository.CustomerRepository;
import com.sgagestudio.warm_follow_backend.repository.GdprRequestRepository;
import com.sgagestudio.warm_follow_backend.repository.UserRepository;
import com.sgagestudio.warm_follow_backend.security.SecurityUtils;
import com.sgagestudio.warm_follow_backend.util.ApiException;
import com.sgagestudio.warm_follow_backend.util.OffsetPageRequest;
import jakarta.persistence.criteria.Predicate;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class CustomerService {
    private final CustomerRepository customerRepository;
    private final CustomerConsentEventRepository consentEventRepository;
    private final GdprRequestRepository gdprRequestRepository;
    private final UserRepository userRepository;
    private final AuditService auditService;
    private final SecurityUtils securityUtils;
    private final LegalTermsService legalTermsService;
    private final TimeService timeService;

    public CustomerService(
            CustomerRepository customerRepository,
            CustomerConsentEventRepository consentEventRepository,
            GdprRequestRepository gdprRequestRepository,
            UserRepository userRepository,
            AuditService auditService,
            SecurityUtils securityUtils,
            LegalTermsService legalTermsService,
            TimeService timeService
    ) {
        this.customerRepository = customerRepository;
        this.consentEventRepository = consentEventRepository;
        this.gdprRequestRepository = gdprRequestRepository;
        this.userRepository = userRepository;
        this.auditService = auditService;
        this.securityUtils = securityUtils;
        this.legalTermsService = legalTermsService;
        this.timeService = timeService;
    }

    public PagedResponse<CustomerResponse> listCustomers(
            String search,
            ConsentStatus consentStatus,
            LocalDate from,
            LocalDate to,
            int limit,
            long offset
    ) {
        UUID ownerId = securityUtils.requireCurrentUserId();
        Pageable pageable = OffsetPageRequest.of(offset, limit, Sort.by("createdAt").descending());
        Specification<Customer> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("ownerUserId"), ownerId));
            predicates.add(cb.isFalse(root.get("erased")));
            if (consentStatus != null) {
                predicates.add(cb.equal(root.get("consentStatus"), consentStatus));
            }
            if (StringUtils.hasText(search)) {
                String term = "%" + search.toLowerCase(Locale.ROOT) + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("firstName")), term),
                        cb.like(cb.lower(root.get("lastName")), term),
                        cb.like(cb.lower(root.get("email")), term),
                        cb.like(cb.lower(root.get("phone")), term)
                ));
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
        Page<Customer> page = customerRepository.findAll(spec, pageable);
        List<CustomerResponse> items = page.getContent().stream().map(this::toResponse).toList();
        String nextCursor = page.hasNext() ? String.valueOf(offset + limit) : null;
        return new PagedResponse<>(items, nextCursor);
    }

    public CustomerResponse create(CustomerCreateRequest request) {
        UUID ownerId = securityUtils.requireCurrentUserId();
        legalTermsService.requireAccepted(ownerId);
        Customer customer = new Customer();
        customer.setOwnerUserId(ownerId);
        customer.setFirstName(request.first_name());
        customer.setLastName(request.last_name());
        customer.setEmail(request.email());
        customer.setPhone(request.phone());
        customer.setConsentStatus(request.consent_status());
        customer.setConsentSource(request.consent_source());
        customer.setConsentProofRef(request.consent_proof_ref());
        customer.setConsentChannels(normalizeChannels(request.consent_channels()));
        if (request.consent_status() == ConsentStatus.granted) {
            customer.setConsentDate(LocalDate.now());
        }
        Customer saved = customerRepository.save(customer);

        CustomerConsentEvent event = new CustomerConsentEvent();
        event.setCustomer(saved);
        event.setStatus(saved.getConsentStatus());
        event.setChannels(saved.getConsentChannels());
        event.setSource(saved.getConsentSource());
        event.setProofRef(saved.getConsentProofRef());
        consentEventRepository.save(event);

        CustomerResponse response = toResponse(saved);
        auditService.audit("customer", saved.getId().toString(), "customer.create", null, response);
        return response;
    }

    public CustomerResponse get(UUID customerId) {
        Customer customer = findOwnedCustomer(customerId);
        return toResponse(customer);
    }

    public CustomerResponse update(UUID customerId, CustomerUpdateRequest request) {
        Customer customer = findOwnedCustomer(customerId);
        CustomerResponse before = toResponse(customer);
        if (request.first_name() != null) {
            customer.setFirstName(request.first_name());
        }
        if (request.last_name() != null) {
            customer.setLastName(request.last_name());
        }
        if (request.email() != null) {
            customer.setEmail(request.email());
        }
        if (request.phone() != null) {
            customer.setPhone(request.phone());
        }
        if (request.consent_status() != null) {
            customer.setConsentStatus(request.consent_status());
            if (request.consent_status() == ConsentStatus.granted) {
                customer.setConsentDate(LocalDate.now());
            }
        }
        if (request.consent_source() != null) {
            customer.setConsentSource(request.consent_source());
        }
        if (request.consent_channels() != null) {
            customer.setConsentChannels(normalizeChannels(request.consent_channels()));
        }
        if (request.consent_proof_ref() != null) {
            customer.setConsentProofRef(request.consent_proof_ref());
        }
        Customer saved = customerRepository.save(customer);
        CustomerResponse response = toResponse(saved);
        auditService.audit("customer", saved.getId().toString(), "customer.update", before, response);
        return response;
    }

    public void delete(UUID customerId) {
        if (!securityUtils.isAdmin()) {
            throw new ApiException(HttpStatus.FORBIDDEN, "FORBIDDEN", "Admin role required");
        }
        Customer customer = findOwnedCustomer(customerId);
        customerRepository.delete(customer);
        auditService.audit("customer", customer.getId().toString(), "customer.delete", null, null);
    }

    public CustomerResponse updateConsent(UUID customerId, ConsentUpdateRequest request) {
        Customer customer = findOwnedCustomer(customerId);
        CustomerResponse before = toResponse(customer);
        customer.setConsentStatus(request.status());
        customer.setConsentSource(request.source());
        customer.setConsentProofRef(request.proof_ref());
        customer.setConsentChannels(normalizeChannels(request.channels()));
        if (request.status() == ConsentStatus.granted) {
            customer.setConsentDate(LocalDate.now());
        }
        Customer saved = customerRepository.save(customer);

        CustomerConsentEvent event = new CustomerConsentEvent();
        event.setCustomer(saved);
        event.setStatus(request.status());
        event.setChannels(saved.getConsentChannels());
        event.setSource(request.source());
        event.setProofRef(request.proof_ref());
        consentEventRepository.save(event);

        CustomerResponse response = toResponse(saved);
        auditService.audit("customer", saved.getId().toString(), "customer.consent_update", before, response);
        return response;
    }

    public List<ConsentEventResponse> consentHistory(UUID customerId) {
        Customer customer = findOwnedCustomer(customerId);
        return consentEventRepository.findByCustomer_IdOrderByCreatedAtDesc(customer.getId())
                .stream()
                .map(event -> new ConsentEventResponse(
                        event.getId(),
                        event.getStatus(),
                        java.util.Arrays.asList(event.getChannels()),
                        event.getProofRef(),
                        event.getSource(),
                        event.getCreatedAt()
                ))
                .toList();
    }

    public GdprRequestResponse exportCustomer(UUID customerId) {
        Customer customer = findOwnedCustomer(customerId);
        User actor = userRepository.findById(securityUtils.requireCurrentUserId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "User not found"));
        GdprRequest request = new GdprRequest();
        request.setCustomer(customer);
        request.setRequestedBy(actor);
        request.setType(GdprRequestType.export);
        request.setStatus(GdprRequestStatus.queued);
        GdprRequest saved = gdprRequestRepository.save(request);
        auditService.audit("gdpr_request", saved.getId().toString(), "gdpr.export", null,
                new GdprRequestResponse(saved.getId(), saved.getStatus()));
        return new GdprRequestResponse(saved.getId(), saved.getStatus());
    }

    public GdprRequestResponse eraseCustomer(UUID customerId) {
        Customer customer = findOwnedCustomer(customerId);
        CustomerResponse before = toResponse(customer);
        User actor = userRepository.findById(securityUtils.requireCurrentUserId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "User not found"));

        customer.setFirstName(null);
        customer.setLastName(null);
        customer.setEmail(null);
        customer.setPhone(null);
        customer.setErased(true);
        customer.setErasedAt(Instant.now());
        customer.setConsentStatus(ConsentStatus.revoked);
        customer.setConsentDate(LocalDate.now());
        customer.setConsentChannels(new String[] {});
        customer.setConsentSource("gdpr");
        customer.setConsentProofRef(null);
        customerRepository.save(customer);

        CustomerConsentEvent event = new CustomerConsentEvent();
        event.setCustomer(customer);
        event.setStatus(ConsentStatus.revoked);
        event.setChannels(new String[] {});
        event.setSource("gdpr");
        consentEventRepository.save(event);

        GdprRequest request = new GdprRequest();
        request.setCustomer(customer);
        request.setRequestedBy(actor);
        request.setType(GdprRequestType.erase);
        request.setStatus(GdprRequestStatus.done);
        request.setCompletedAt(Instant.now());
        GdprRequest saved = gdprRequestRepository.save(request);
        CustomerResponse after = toResponse(customer);
        auditService.audit("customer", customer.getId().toString(), "gdpr.erase", before, after);
        auditService.audit("gdpr_request", saved.getId().toString(), "gdpr.erase.request", null,
                new GdprRequestResponse(saved.getId(), saved.getStatus()));
        return new GdprRequestResponse(saved.getId(), saved.getStatus());
    }

    private Customer findOwnedCustomer(UUID customerId) {
        return customerRepository.findByIdAndOwnerUserId(customerId, securityUtils.requireCurrentUserId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "CUSTOMER_NOT_FOUND", "Customer not found"));
    }

    private String[] normalizeChannels(List<String> channels) {
        if (channels == null || channels.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "CONSENT_CHANNELS_REQUIRED", "Consent channels required");
        }
        List<String> normalized = channels.stream()
                .filter(StringUtils::hasText)
                .map(value -> value.toLowerCase(Locale.ROOT))
                .distinct()
                .toList();
        if (normalized.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "CONSENT_CHANNELS_REQUIRED", "Consent channels required");
        }
        for (String channel : normalized) {
            if (!channel.equals("email") && !channel.equals("sms")) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "CONSENT_CHANNEL_INVALID", "Invalid consent channel");
            }
        }
        return normalized.toArray(String[]::new);
    }

    private CustomerResponse toResponse(Customer customer) {
        return new CustomerResponse(
                customer.getId(),
                customer.getFirstName(),
                customer.getLastName(),
                customer.getEmail(),
                customer.getPhone(),
                customer.getConsentStatus(),
                customer.getConsentDate(),
                customer.getConsentSource(),
                customer.getConsentChannels() == null ? List.of() : java.util.Arrays.asList(customer.getConsentChannels()),
                customer.getConsentProofRef(),
                customer.isErased(),
                customer.getCreatedAt(),
                customer.getUpdatedAt()
        );
    }
}
