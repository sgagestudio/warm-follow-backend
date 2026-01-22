package com.sgagestudio.warm_follow_backend.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.sgagestudio.warm_follow_backend.audit.AuditService;
import com.sgagestudio.warm_follow_backend.dto.DnsRecord;
import com.sgagestudio.warm_follow_backend.dto.DnsRecordPurpose;
import com.sgagestudio.warm_follow_backend.dto.DnsRecordType;
import com.sgagestudio.warm_follow_backend.dto.SendingDomainCreateRequest;
import com.sgagestudio.warm_follow_backend.dto.SendingDomainListResponse;
import com.sgagestudio.warm_follow_backend.dto.SendingDomainResponse;
import com.sgagestudio.warm_follow_backend.model.SendingDomain;
import com.sgagestudio.warm_follow_backend.model.SendingDomainStatus;
import com.sgagestudio.warm_follow_backend.model.WorkspaceRole;
import com.sgagestudio.warm_follow_backend.repository.SendingDomainRepository;
import com.sgagestudio.warm_follow_backend.util.ApiException;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class SendingDomainService {
    private static final Pattern DOMAIN_PATTERN = Pattern.compile(
            "^[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?(?:\\.[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?)+$"
    );

    private final SendingDomainRepository sendingDomainRepository;
    private final WorkspaceContextService workspaceContextService;
    private final AuditService auditService;
    private final ObjectMapper objectMapper;

    public SendingDomainService(
            SendingDomainRepository sendingDomainRepository,
            WorkspaceContextService workspaceContextService,
            AuditService auditService,
            ObjectMapper objectMapper
    ) {
        this.sendingDomainRepository = sendingDomainRepository;
        this.workspaceContextService = workspaceContextService;
        this.auditService = auditService;
        this.objectMapper = objectMapper;
    }

    public SendingDomainListResponse list() {
        WorkspaceContextService.WorkspaceContext context = workspaceContextService.requireContext();
        requireOwnerOrAdmin(context);
        UUID workspaceId = context.workspace().getId();
        List<SendingDomainResponse> items = sendingDomainRepository.findByWorkspaceIdOrderByCreatedAtDesc(workspaceId)
                .stream()
                .map(this::toResponse)
                .toList();
        return new SendingDomainListResponse(items);
    }

    public SendingDomainResponse create(SendingDomainCreateRequest request) {
        WorkspaceContextService.WorkspaceContext context = workspaceContextService.requireContext();
        requireOwnerOrAdmin(context);
        UUID workspaceId = context.workspace().getId();
        String normalized = normalizeDomain(request.domain());
        if (sendingDomainRepository.existsByWorkspaceIdAndDomainIgnoreCase(workspaceId, normalized)) {
            throw new ApiException(HttpStatus.CONFLICT, "DOMAIN_EXISTS", "Domain already exists");
        }
        SendingDomain domain = new SendingDomain();
        domain.setWorkspaceId(workspaceId);
        domain.setDomain(normalized);
        domain.setStatus(SendingDomainStatus.pending);
        domain.setVerificationRecords(objectMapper.valueToTree(buildVerificationRecords(normalized)));
        SendingDomain saved = sendingDomainRepository.save(domain);
        SendingDomainResponse response = toResponse(saved);
        auditService.audit(workspaceId, "sending_domain", saved.getId().toString(), "sending_domain.create", null, response);
        return response;
    }

    public SendingDomainResponse get(UUID domainId) {
        WorkspaceContextService.WorkspaceContext context = workspaceContextService.requireContext();
        requireOwnerOrAdmin(context);
        SendingDomain domain = findOwnedDomain(domainId, context.workspace().getId());
        return toResponse(domain);
    }

    public SendingDomainResponse verify(UUID domainId) {
        WorkspaceContextService.WorkspaceContext context = workspaceContextService.requireContext();
        requireOwnerOrAdmin(context);
        SendingDomain domain = findOwnedDomain(domainId, context.workspace().getId());
        domain.setLastCheckedAt(Instant.now());
        if (domain.getStatus() != SendingDomainStatus.verified) {
            domain.setStatus(SendingDomainStatus.verified);
        }
        SendingDomain saved = sendingDomainRepository.save(domain);
        SendingDomainResponse response = toResponse(saved);
        auditService.audit(context.workspace().getId(), "sending_domain", saved.getId().toString(), "sending_domain.verify", null, response);
        return response;
    }

    private SendingDomain findOwnedDomain(UUID domainId, UUID workspaceId) {
        return sendingDomainRepository.findByIdAndWorkspaceId(domainId, workspaceId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "DOMAIN_NOT_FOUND", "Domain not found"));
    }

    private String normalizeDomain(String domain) {
        if (!StringUtils.hasText(domain)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "DOMAIN_INVALID", "Invalid domain format");
        }
        String trimmed = domain.trim().toLowerCase(Locale.ROOT);
        if (!DOMAIN_PATTERN.matcher(trimmed).matches()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "DOMAIN_INVALID", "Invalid domain format");
        }
        return trimmed;
    }

    private void requireOwnerOrAdmin(WorkspaceContextService.WorkspaceContext context) {
        WorkspaceRole role = context.membership().getRole();
        if (role == WorkspaceRole.member) {
            throw new ApiException(HttpStatus.FORBIDDEN, "FORBIDDEN", "Owner or admin role required");
        }
    }

    private List<DnsRecord> buildVerificationRecords(String domain) {
        String dmarcValue = "v=DMARC1; p=none";
        return List.of(
                new DnsRecord(DnsRecordType.TXT, "@", "v=spf1 include:amazonses.com ~all", null, null, DnsRecordPurpose.spf),
                new DnsRecord(DnsRecordType.CNAME, "dkim._domainkey", "dkim.amazonses.com", null, null, DnsRecordPurpose.dkim),
                new DnsRecord(DnsRecordType.TXT, "_dmarc", dmarcValue, null, null, DnsRecordPurpose.dmarc)
        );
    }

    private SendingDomainResponse toResponse(SendingDomain domain) {
        List<DnsRecord> records = List.of();
        if (domain.getVerificationRecords() instanceof ArrayNode) {
            records = objectMapper.convertValue(domain.getVerificationRecords(), new TypeReference<List<DnsRecord>>() {});
        }
        return new SendingDomainResponse(
                domain.getId(),
                domain.getDomain(),
                domain.getStatus(),
                records,
                domain.getCreatedAt(),
                domain.getUpdatedAt()
        );
    }
}
