package com.sgagestudio.warm_follow_backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sgagestudio.warm_follow_backend.audit.AuditService;
import com.sgagestudio.warm_follow_backend.dto.ComplianceAssessmentRequest;
import com.sgagestudio.warm_follow_backend.dto.ComplianceAssessmentResponse;
import com.sgagestudio.warm_follow_backend.dto.ProcessingRecordResponse;
import com.sgagestudio.warm_follow_backend.model.AuditEvent;
import com.sgagestudio.warm_follow_backend.model.ComplianceAssessment;
import com.sgagestudio.warm_follow_backend.repository.AuditEventRepository;
import com.sgagestudio.warm_follow_backend.repository.ComplianceAssessmentRepository;
import com.sgagestudio.warm_follow_backend.security.SecurityUtils;
import jakarta.persistence.criteria.Predicate;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

@Service
public class ComplianceService {
    private final AuditEventRepository auditEventRepository;
    private final ComplianceAssessmentRepository assessmentRepository;
    private final ObjectMapper objectMapper;
    private final AuditService auditService;
    private final SecurityUtils securityUtils;
    private final WorkspaceContextService workspaceContextService;

    public ComplianceService(
            AuditEventRepository auditEventRepository,
            ComplianceAssessmentRepository assessmentRepository,
            ObjectMapper objectMapper,
            AuditService auditService,
            SecurityUtils securityUtils,
            WorkspaceContextService workspaceContextService
    ) {
        this.auditEventRepository = auditEventRepository;
        this.assessmentRepository = assessmentRepository;
        this.objectMapper = objectMapper;
        this.auditService = auditService;
        this.securityUtils = securityUtils;
        this.workspaceContextService = workspaceContextService;
    }

    public ProcessingRecordResponse processingRecord() {
        UUID workspaceId = workspaceContextService.requireContext().workspace().getId();
        Specification<AuditEvent> spec = (root, query, cb) -> {
            List<Predicate> predicates = List.of(cb.equal(root.get("workspaceId"), workspaceId));
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        List<AuditEvent> events = auditEventRepository.findAll(spec);
        Map<String, Long> actions = new HashMap<>();
        Map<String, Long> entities = new HashMap<>();
        for (AuditEvent event : events) {
            actions.merge(event.getAction(), 1L, Long::sum);
            entities.merge(event.getEntityType(), 1L, Long::sum);
        }
        return new ProcessingRecordResponse(Instant.now(), actions, entities);
    }

    public ComplianceAssessmentResponse createAssessment(ComplianceAssessmentRequest request) {
        UUID ownerId = securityUtils.requireCurrentUserId();
        UUID workspaceId = workspaceContextService.requireContext().workspace().getId();
        ComplianceAssessment assessment = new ComplianceAssessment();
        assessment.setOwnerUserId(ownerId);
        assessment.setWorkspaceId(workspaceId);
        assessment.setDetails(objectMapper.valueToTree(request.details()));
        ComplianceAssessment saved = assessmentRepository.save(assessment);
        auditService.audit(workspaceId, "compliance_assessment", saved.getId().toString(), "compliance.assessment.create", null, saved);
        return new ComplianceAssessmentResponse(saved.getId(), saved.getCreatedAt());
    }
}
