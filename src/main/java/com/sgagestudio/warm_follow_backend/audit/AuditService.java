package com.sgagestudio.warm_follow_backend.audit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sgagestudio.warm_follow_backend.model.AuditEvent;
import com.sgagestudio.warm_follow_backend.model.User;
import com.sgagestudio.warm_follow_backend.repository.AuditEventRepository;
import com.sgagestudio.warm_follow_backend.repository.UserRepository;
import com.sgagestudio.warm_follow_backend.security.AuthenticatedUser;
import com.sgagestudio.warm_follow_backend.security.SecurityUtils;
import com.sgagestudio.warm_follow_backend.util.RequestContext;
import com.sgagestudio.warm_follow_backend.util.RequestContextHolder;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class AuditService {
    private final AuditEventRepository auditEventRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;
    private final SecurityUtils securityUtils;

    public AuditService(
            AuditEventRepository auditEventRepository,
            UserRepository userRepository,
            ObjectMapper objectMapper,
            SecurityUtils securityUtils
    ) {
        this.auditEventRepository = auditEventRepository;
        this.userRepository = userRepository;
        this.objectMapper = objectMapper;
        this.securityUtils = securityUtils;
    }

    public void audit(String entityType, String entityId, String action, Object before, Object after) {
        auditInternal(null, entityType, entityId, action, before, after);
    }

    public void audit(UUID workspaceId, String entityType, String entityId, String action, Object before, Object after) {
        auditInternal(workspaceId, entityType, entityId, action, before, after);
    }

    private void auditInternal(UUID workspaceId, String entityType, String entityId, String action, Object before, Object after) {
        AuditEvent event = new AuditEvent();
        event.setWorkspaceId(resolveWorkspaceId(workspaceId));
        event.setEntityType(entityType);
        event.setEntityId(entityId);
        event.setAction(action);
        if (before != null) {
            event.setBefore(objectMapper.valueToTree(before));
        }
        if (after != null) {
            event.setAfter(objectMapper.valueToTree(after));
        }
        event.setRequestId(RequestContextHolder.getRequestId());
        Optional<RequestContext> context = RequestContextHolder.get();
        context.ifPresent(ctx -> {
            event.setIp(ctx.ip());
            event.setUserAgent(ctx.userAgent());
        });

        AuthenticatedUser authenticatedUser = securityUtils.getAuthenticatedUserOrNull();
        if (authenticatedUser != null) {
            UUID userId = authenticatedUser.userId();
            userRepository.findById(userId).ifPresent(event::setActorUser);
        }
        auditEventRepository.save(event);
    }

    private UUID resolveWorkspaceId(UUID workspaceId) {
        if (workspaceId != null) {
            return workspaceId;
        }
        String header = RequestContextHolder.get().map(RequestContext::workspaceId).orElse(null);
        if (StringUtils.hasText(header)) {
            try {
                return UUID.fromString(header);
            } catch (IllegalArgumentException ex) {
                throw new IllegalStateException("Invalid workspace id in request context");
            }
        }
        AuthenticatedUser authenticatedUser = securityUtils.getAuthenticatedUserOrNull();
        if (authenticatedUser != null && authenticatedUser.workspaceId() != null) {
            return authenticatedUser.workspaceId();
        }
        throw new IllegalStateException("Workspace context not available for audit event");
    }
}
