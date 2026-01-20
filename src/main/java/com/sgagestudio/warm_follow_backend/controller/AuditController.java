package com.sgagestudio.warm_follow_backend.controller;

import com.sgagestudio.warm_follow_backend.dto.AuditEventResponse;
import com.sgagestudio.warm_follow_backend.dto.PagedResponse;
import com.sgagestudio.warm_follow_backend.model.AuditEvent;
import com.sgagestudio.warm_follow_backend.repository.AuditEventRepository;
import com.sgagestudio.warm_follow_backend.security.SecurityUtils;
import com.sgagestudio.warm_follow_backend.service.TimeService;
import com.sgagestudio.warm_follow_backend.util.ApiException;
import com.sgagestudio.warm_follow_backend.util.OffsetPageRequest;
import jakarta.persistence.criteria.Predicate;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/audit")
@CrossOrigin(origins = "*")
public class AuditController {
    private final AuditEventRepository auditEventRepository;
    private final SecurityUtils securityUtils;
    private final TimeService timeService;

    public AuditController(AuditEventRepository auditEventRepository, SecurityUtils securityUtils, TimeService timeService) {
        this.auditEventRepository = auditEventRepository;
        this.securityUtils = securityUtils;
        this.timeService = timeService;
    }

    @GetMapping("/events")
    public PagedResponse<AuditEventResponse> events(
            @RequestParam(required = false) String entity_type,
            @RequestParam(required = false) String entity_id,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String request_id,
            @RequestParam(required = false) LocalDate from,
            @RequestParam(required = false) LocalDate to,
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(required = false) String cursor
    ) {
        long offset = parseCursor(cursor);
        UUID actorId = securityUtils.requireCurrentUserId();
        Pageable pageable = OffsetPageRequest.of(offset, limit, Sort.by("createdAt").descending());
        Specification<AuditEvent> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("actorUser").get("id"), actorId));
            if (entity_type != null) {
                predicates.add(cb.equal(root.get("entityType"), entity_type));
            }
            if (entity_id != null) {
                predicates.add(cb.equal(root.get("entityId"), entity_id));
            }
            if (action != null) {
                predicates.add(cb.equal(root.get("action"), action));
            }
            if (request_id != null) {
                predicates.add(cb.equal(root.get("requestId"), request_id));
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
        Page<AuditEvent> page = auditEventRepository.findAll(spec, pageable);
        List<AuditEventResponse> items = page.getContent().stream()
                .map(event -> new AuditEventResponse(
                        event.getId(),
                        event.getActorUser() != null ? event.getActorUser().getId() : null,
                        event.getEntityType(),
                        event.getEntityId(),
                        event.getAction(),
                        event.getRequestId(),
                        event.getCreatedAt()
                ))
                .toList();
        String nextCursor = page.hasNext() ? String.valueOf(offset + limit) : null;
        return new PagedResponse<>(items, nextCursor);
    }

    private long parseCursor(String cursor) {
        if (cursor == null || cursor.isBlank()) {
            return 0;
        }
        try {
            return Long.parseLong(cursor);
        } catch (NumberFormatException ex) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "CURSOR_INVALID", "Invalid cursor");
        }
    }
}
