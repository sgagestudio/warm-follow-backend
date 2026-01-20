package com.sgagestudio.warm_follow_backend.service;

import com.sgagestudio.warm_follow_backend.dto.PagedResponse;
import com.sgagestudio.warm_follow_backend.dto.TransactionResponse;
import com.sgagestudio.warm_follow_backend.model.Transaction;
import com.sgagestudio.warm_follow_backend.model.TransactionStatus;
import com.sgagestudio.warm_follow_backend.repository.DeliveryRepository;
import com.sgagestudio.warm_follow_backend.repository.TransactionRepository;
import com.sgagestudio.warm_follow_backend.security.SecurityUtils;
import com.sgagestudio.warm_follow_backend.util.ApiException;
import com.sgagestudio.warm_follow_backend.util.OffsetPageRequest;
import jakarta.persistence.criteria.Predicate;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class TransactionService {
    private final TransactionRepository transactionRepository;
    private final DeliveryRepository deliveryRepository;
    private final SecurityUtils securityUtils;
    private final TimeService timeService;

    public TransactionService(
            TransactionRepository transactionRepository,
            DeliveryRepository deliveryRepository,
            SecurityUtils securityUtils,
            TimeService timeService
    ) {
        this.transactionRepository = transactionRepository;
        this.deliveryRepository = deliveryRepository;
        this.securityUtils = securityUtils;
        this.timeService = timeService;
    }

    public PagedResponse<TransactionResponse> listTransactions(
            UUID reminderId,
            UUID customerId,
            TransactionStatus status,
            LocalDate from,
            LocalDate to,
            int limit,
            long offset
    ) {
        UUID ownerId = securityUtils.requireCurrentUserId();
        List<UUID> transactionIdsFilter;
        if (customerId != null) {
            transactionIdsFilter = deliveryRepository.findDistinctTransactionIdsByCustomerId(customerId);
            if (transactionIdsFilter.isEmpty()) {
                return new PagedResponse<>(List.of(), null);
            }
        } else {
            transactionIdsFilter = null;
        }
        Pageable pageable = OffsetPageRequest.of(offset, limit, Sort.by("createdAt").descending());
        Specification<Transaction> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("reminder").get("ownerUserId"), ownerId));
            if (reminderId != null) {
                predicates.add(cb.equal(root.get("reminder").get("id"), reminderId));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (from != null) {
                Instant start = timeService.startOfDay(from);
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), start));
            }
            if (to != null) {
                Instant end = timeService.startOfNextDay(to);
                predicates.add(cb.lessThan(root.get("createdAt"), end));
            }
            if (transactionIdsFilter != null) {
                predicates.add(root.get("id").in(transactionIdsFilter));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        Page<Transaction> page = transactionRepository.findAll(spec, pageable);
        Map<UUID, Map<String, Long>> counts = loadCounts(page.getContent());
        List<TransactionResponse> items = page.getContent().stream()
                .map(tx -> toResponse(tx, counts.getOrDefault(tx.getId(), Map.of())))
                .toList();
        String nextCursor = page.hasNext() ? String.valueOf(offset + limit) : null;
        return new PagedResponse<>(items, nextCursor);
    }

    public TransactionResponse getTransaction(UUID transactionId) {
        Transaction tx = transactionRepository.findByIdAndReminder_OwnerUserId(transactionId, securityUtils.requireCurrentUserId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "TRANSACTION_NOT_FOUND", "Transaction not found"));
        Map<UUID, Map<String, Long>> counts = loadCounts(List.of(tx));
        return toResponse(tx, counts.getOrDefault(tx.getId(), Map.of()));
    }

    private Map<UUID, Map<String, Long>> loadCounts(List<Transaction> transactions) {
        if (transactions.isEmpty()) {
            return Map.of();
        }
        List<UUID> ids = transactions.stream().map(Transaction::getId).toList();
        List<DeliveryRepository.DeliveryStatusCountByTransaction> rows = deliveryRepository.countByTransactionIds(ids);
        Map<UUID, Map<String, Long>> counts = new HashMap<>();
        for (DeliveryRepository.DeliveryStatusCountByTransaction row : rows) {
            counts.computeIfAbsent(row.getTransactionId(), id -> new HashMap<>())
                    .put(row.getStatus().name(), row.getCount());
        }
        return counts;
    }

    private TransactionResponse toResponse(Transaction tx, Map<String, Long> counts) {
        return new TransactionResponse(
                tx.getId(),
                tx.getReminder().getId(),
                tx.getStatus(),
                tx.getRequestId(),
                tx.getIdempotencyKey(),
                tx.getStartedAt(),
                tx.getFinishedAt(),
                tx.getCreatedAt(),
                counts
        );
    }
}
