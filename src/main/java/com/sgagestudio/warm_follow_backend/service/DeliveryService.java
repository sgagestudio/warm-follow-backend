package com.sgagestudio.warm_follow_backend.service;

import com.sgagestudio.warm_follow_backend.dto.DeliveryResponse;
import com.sgagestudio.warm_follow_backend.model.Delivery;
import com.sgagestudio.warm_follow_backend.model.Transaction;
import com.sgagestudio.warm_follow_backend.repository.DeliveryRepository;
import com.sgagestudio.warm_follow_backend.repository.TransactionRepository;
import com.sgagestudio.warm_follow_backend.security.SecurityUtils;
import com.sgagestudio.warm_follow_backend.util.ApiException;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class DeliveryService {
    private final DeliveryRepository deliveryRepository;
    private final TransactionRepository transactionRepository;
    private final SecurityUtils securityUtils;
    private final WorkspaceContextService workspaceContextService;

    public DeliveryService(
            DeliveryRepository deliveryRepository,
            TransactionRepository transactionRepository,
            SecurityUtils securityUtils,
            WorkspaceContextService workspaceContextService
    ) {
        this.deliveryRepository = deliveryRepository;
        this.transactionRepository = transactionRepository;
        this.securityUtils = securityUtils;
        this.workspaceContextService = workspaceContextService;
    }

    public List<DeliveryResponse> listByTransaction(UUID transactionId) {
        UUID workspaceId = workspaceContextService.requireContext().workspace().getId();
        Transaction tx = transactionRepository.findByIdAndWorkspaceId(transactionId, workspaceId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "TRANSACTION_NOT_FOUND", "Transaction not found"));
        return deliveryRepository.findByTransaction_Id(tx.getId()).stream()
                .map(this::toResponse)
                .toList();
    }

    public DeliveryResponse getDelivery(UUID deliveryId) {
        UUID workspaceId = workspaceContextService.requireContext().workspace().getId();
        Delivery delivery = deliveryRepository.findByIdAndWorkspaceId(deliveryId, workspaceId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "DELIVERY_NOT_FOUND", "Delivery not found"));
        return toResponse(delivery);
    }

    private DeliveryResponse toResponse(Delivery delivery) {
        return new DeliveryResponse(
                delivery.getId(),
                delivery.getReminderId(),
                delivery.getTransaction().getId(),
                delivery.getChannel(),
                delivery.getProviderMessageId(),
                delivery.getRecipient(),
                delivery.getStatus(),
                delivery.getErrorCode(),
                delivery.getErrorMessage(),
                delivery.getCreatedAt(),
                delivery.getUpdatedAt()
        );
    }
}
