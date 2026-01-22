package com.sgagestudio.warm_follow_backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sgagestudio.warm_follow_backend.audit.AuditService;
import com.sgagestudio.warm_follow_backend.dto.PagedResponse;
import com.sgagestudio.warm_follow_backend.dto.TemplateCreateRequest;
import com.sgagestudio.warm_follow_backend.dto.TemplatePreviewRequest;
import com.sgagestudio.warm_follow_backend.dto.TemplatePreviewResponse;
import com.sgagestudio.warm_follow_backend.dto.TemplateResponse;
import com.sgagestudio.warm_follow_backend.dto.TemplateUpdateRequest;
import com.sgagestudio.warm_follow_backend.model.ReminderStatus;
import com.sgagestudio.warm_follow_backend.model.Template;
import com.sgagestudio.warm_follow_backend.repository.ReminderRepository;
import com.sgagestudio.warm_follow_backend.repository.TemplateRepository;
import com.sgagestudio.warm_follow_backend.security.SecurityUtils;
import com.sgagestudio.warm_follow_backend.util.ApiException;
import com.sgagestudio.warm_follow_backend.util.OffsetPageRequest;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class TemplateService {
    private final TemplateRepository templateRepository;
    private final ReminderRepository reminderRepository;
    private final AuditService auditService;
    private final SecurityUtils securityUtils;
    private final WorkspaceContextService workspaceContextService;
    private final ObjectMapper objectMapper;

    public TemplateService(
            TemplateRepository templateRepository,
            ReminderRepository reminderRepository,
            AuditService auditService,
            SecurityUtils securityUtils,
            WorkspaceContextService workspaceContextService,
            ObjectMapper objectMapper
    ) {
        this.templateRepository = templateRepository;
        this.reminderRepository = reminderRepository;
        this.auditService = auditService;
        this.securityUtils = securityUtils;
        this.workspaceContextService = workspaceContextService;
        this.objectMapper = objectMapper;
    }

    public PagedResponse<TemplateResponse> listTemplates(int limit, long offset, String search) {
        UUID workspaceId = workspaceContextService.requireContext().workspace().getId();
        Pageable pageable = OffsetPageRequest.of(offset, limit, Sort.by("createdAt").descending());
        Specification<Template> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("workspaceId"), workspaceId));
            if (StringUtils.hasText(search)) {
                String term = "%" + search.toLowerCase(Locale.ROOT) + "%";
                predicates.add(cb.like(cb.lower(root.get("name")), term));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        Page<Template> page = templateRepository.findAll(spec, pageable);
        List<TemplateResponse> items = page.getContent().stream().map(this::toResponse).toList();
        String nextCursor = page.hasNext() ? String.valueOf(offset + limit) : null;
        return new PagedResponse<>(items, nextCursor);
    }

    public TemplateResponse create(TemplateCreateRequest request) {
        Template template = new Template();
        UUID ownerId = securityUtils.requireCurrentUserId();
        UUID workspaceId = workspaceContextService.requireContext().workspace().getId();
        template.setOwnerUserId(ownerId);
        template.setWorkspaceId(workspaceId);
        template.setName(request.name());
        template.setSubject(request.subject());
        template.setContent(request.content());
        template.setChannel(request.channel());
        if (request.variables() != null) {
            template.setVariables(objectMapper.valueToTree(request.variables()));
        }
        Template saved = templateRepository.save(template);
        TemplateResponse response = toResponse(saved);
        auditService.audit(workspaceId, "template", saved.getId().toString(), "template.create", null, response);
        return response;
    }

    public TemplateResponse get(Long templateId) {
        Template template = findOwnedTemplate(templateId);
        return toResponse(template);
    }

    public TemplateResponse update(Long templateId, TemplateUpdateRequest request) {
        Template template = findOwnedTemplate(templateId);
        if (hasActiveReminders(templateId)) {
            throw new ApiException(HttpStatus.CONFLICT, "TEMPLATE_IN_USE", "Template has active reminders");
        }
        TemplateResponse before = toResponse(template);
        if (request.name() != null) {
            template.setName(request.name());
        }
        if (request.subject() != null) {
            template.setSubject(request.subject());
        }
        if (request.content() != null) {
            template.setContent(request.content());
        }
        if (request.channel() != null) {
            template.setChannel(request.channel());
        }
        if (request.variables() != null) {
            template.setVariables(objectMapper.valueToTree(request.variables()));
        }
        Template saved = templateRepository.save(template);
        TemplateResponse response = toResponse(saved);
        auditService.audit(template.getWorkspaceId(), "template", saved.getId().toString(), "template.update", before, response);
        return response;
    }

    public void delete(Long templateId) {
        if (hasActiveReminders(templateId)) {
            throw new ApiException(HttpStatus.CONFLICT, "TEMPLATE_IN_USE", "Template has active reminders");
        }
        Template template = findOwnedTemplate(templateId);
        templateRepository.delete(template);
        auditService.audit(template.getWorkspaceId(), "template", template.getId().toString(), "template.delete", null, null);
    }

    public TemplatePreviewResponse preview(TemplatePreviewRequest request) {
        String renderedSubject = null;
        if (request.subject() != null) {
            renderedSubject = renderTemplate(request.subject(), request.variables());
        }
        String renderedContent = renderTemplate(request.content(), request.variables());
        return new TemplatePreviewResponse(renderedSubject, renderedContent);
    }

    private Template findOwnedTemplate(Long templateId) {
        UUID workspaceId = workspaceContextService.requireContext().workspace().getId();
        return templateRepository.findByIdAndWorkspaceId(templateId, workspaceId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "TEMPLATE_NOT_FOUND", "Template not found"));
    }

    private boolean hasActiveReminders(Long templateId) {
        UUID workspaceId = workspaceContextService.requireContext().workspace().getId();
        return reminderRepository.existsByTemplate_IdAndWorkspaceIdAndStatusIn(
                templateId,
                workspaceId,
                List.of(ReminderStatus.active, ReminderStatus.pending)
        );
    }

    private TemplateResponse toResponse(Template template) {
        return new TemplateResponse(
                template.getId(),
                template.getName(),
                template.getSubject(),
                template.getContent(),
                template.getChannel(),
                template.getVariables(),
                template.getVersion(),
                template.getCreatedAt(),
                template.getUpdatedAt()
        );
    }

    private String renderTemplate(String content, Map<String, Object> variables) {
        if (variables == null || variables.isEmpty()) {
            return content;
        }
        String rendered = content;
        for (Map.Entry<String, Object> entry : variables.entrySet()) {
            String key = "{{" + entry.getKey() + "}}";
            rendered = rendered.replace(key, String.valueOf(entry.getValue()));
        }
        return rendered;
    }
}
