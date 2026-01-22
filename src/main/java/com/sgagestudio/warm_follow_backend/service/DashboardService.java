package com.sgagestudio.warm_follow_backend.service;

import com.sgagestudio.warm_follow_backend.dto.DashboardStatsResponse;
import com.sgagestudio.warm_follow_backend.model.ConsentStatus;
import com.sgagestudio.warm_follow_backend.model.Customer;
import com.sgagestudio.warm_follow_backend.model.Delivery;
import com.sgagestudio.warm_follow_backend.model.DeliveryStatus;
import com.sgagestudio.warm_follow_backend.model.Reminder;
import com.sgagestudio.warm_follow_backend.model.ReminderStatus;
import com.sgagestudio.warm_follow_backend.repository.CustomerRepository;
import com.sgagestudio.warm_follow_backend.repository.DeliveryRepository;
import com.sgagestudio.warm_follow_backend.repository.ReminderRepository;
import com.sgagestudio.warm_follow_backend.security.SecurityUtils;
import jakarta.persistence.criteria.Predicate;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

@Service
public class DashboardService {
    private final CustomerRepository customerRepository;
    private final ReminderRepository reminderRepository;
    private final DeliveryRepository deliveryRepository;
    private final SecurityUtils securityUtils;
    private final WorkspaceContextService workspaceContextService;

    public DashboardService(
            CustomerRepository customerRepository,
            ReminderRepository reminderRepository,
            DeliveryRepository deliveryRepository,
            SecurityUtils securityUtils,
            WorkspaceContextService workspaceContextService
    ) {
        this.customerRepository = customerRepository;
        this.reminderRepository = reminderRepository;
        this.deliveryRepository = deliveryRepository;
        this.securityUtils = securityUtils;
        this.workspaceContextService = workspaceContextService;
    }

    public DashboardStatsResponse getStats() {
        UUID workspaceId = workspaceContextService.requireContext().workspace().getId();
        long totalCustomers = customerRepository.count(customerSpec(workspaceId, null, false));
        long consentedCustomers = customerRepository.count(customerSpec(workspaceId, ConsentStatus.granted, false));
        long activeReminders = reminderRepository.count(reminderSpec(workspaceId, ReminderStatus.active));

        Instant weekAgo = Instant.now().minus(7, ChronoUnit.DAYS);
        long sentThisWeek = deliveryRepository.count(deliverySpec(workspaceId, weekAgo, List.of(DeliveryStatus.sent, DeliveryStatus.delivered)));
        long totalDeliveries = deliveryRepository.count(deliverySpec(workspaceId, null, null));
        long delivered = deliveryRepository.count(deliverySpec(workspaceId, null, List.of(DeliveryStatus.delivered)));
        double deliveryRate = totalDeliveries == 0 ? 0.0 : (double) delivered / totalDeliveries;

        return new DashboardStatsResponse(
                totalCustomers,
                activeReminders,
                sentThisWeek,
                deliveryRate,
                consentedCustomers
        );
    }

    private Specification<Customer> customerSpec(UUID workspaceId, ConsentStatus status, boolean includeErased) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("workspaceId"), workspaceId));
            if (!includeErased) {
                predicates.add(cb.isFalse(root.get("erased")));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("consentStatus"), status));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private Specification<Reminder> reminderSpec(UUID workspaceId, ReminderStatus status) {
        return (root, query, cb) -> cb.and(
                cb.equal(root.get("workspaceId"), workspaceId),
                cb.equal(root.get("status"), status)
        );
    }

    private Specification<Delivery> deliverySpec(UUID workspaceId, Instant from, List<DeliveryStatus> statuses) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("workspaceId"), workspaceId));
            if (from != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), from));
            }
            if (statuses != null && !statuses.isEmpty()) {
                predicates.add(root.get("status").in(statuses));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
