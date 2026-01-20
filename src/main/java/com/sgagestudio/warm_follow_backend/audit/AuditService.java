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
        AuditEvent event = new AuditEvent();
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
}
