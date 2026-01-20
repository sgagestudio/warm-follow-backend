package com.sgagestudio.warm_follow_backend.controller;

import com.sgagestudio.warm_follow_backend.dto.DeliveryResponse;
import com.sgagestudio.warm_follow_backend.dto.PagedResponse;
import com.sgagestudio.warm_follow_backend.dto.TransactionResponse;
import com.sgagestudio.warm_follow_backend.model.TransactionStatus;
import com.sgagestudio.warm_follow_backend.service.DeliveryService;
import com.sgagestudio.warm_follow_backend.service.TransactionService;
import com.sgagestudio.warm_follow_backend.util.ApiException;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/transactions")
@CrossOrigin(origins = "*")
public class TransactionController {
    private final TransactionService transactionService;
    private final DeliveryService deliveryService;

    public TransactionController(TransactionService transactionService, DeliveryService deliveryService) {
        this.transactionService = transactionService;
        this.deliveryService = deliveryService;
    }

    @GetMapping
    public PagedResponse<TransactionResponse> list(
            @RequestParam(required = false) UUID reminder_id,
            @RequestParam(required = false) UUID customer_id,
            @RequestParam(required = false) TransactionStatus status,
            @RequestParam(required = false) LocalDate from,
            @RequestParam(required = false) LocalDate to,
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(required = false) String cursor
    ) {
        long offset = parseCursor(cursor);
        return transactionService.listTransactions(reminder_id, customer_id, status, from, to, limit, offset);
    }

    @GetMapping("/{transactionId}")
    public TransactionResponse get(@PathVariable UUID transactionId) {
        return transactionService.getTransaction(transactionId);
    }

    @GetMapping("/{transactionId}/deliveries")
    public List<DeliveryResponse> deliveries(@PathVariable UUID transactionId) {
        return deliveryService.listByTransaction(transactionId);
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
