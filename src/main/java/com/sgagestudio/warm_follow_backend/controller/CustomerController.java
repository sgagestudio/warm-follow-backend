package com.sgagestudio.warm_follow_backend.controller;

import com.sgagestudio.warm_follow_backend.dto.ConsentEventResponse;
import com.sgagestudio.warm_follow_backend.dto.ConsentUpdateRequest;
import com.sgagestudio.warm_follow_backend.dto.CustomerCreateRequest;
import com.sgagestudio.warm_follow_backend.dto.CustomerResponse;
import com.sgagestudio.warm_follow_backend.dto.CustomerUpdateRequest;
import com.sgagestudio.warm_follow_backend.dto.GdprRequestResponse;
import com.sgagestudio.warm_follow_backend.dto.PagedResponse;
import com.sgagestudio.warm_follow_backend.dto.StatusResponse;
import com.sgagestudio.warm_follow_backend.model.ConsentStatus;
import com.sgagestudio.warm_follow_backend.service.CustomerService;
import com.sgagestudio.warm_follow_backend.util.ApiException;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/customers")
@CrossOrigin(origins = "*")
public class CustomerController {
    private final CustomerService customerService;

    public CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }

    @GetMapping
    public PagedResponse<CustomerResponse> list(
            @RequestParam(required = false) String search,
            @RequestParam(required = false, name = "consent_status") ConsentStatus consentStatus,
            @RequestParam(required = false) LocalDate from,
            @RequestParam(required = false) LocalDate to,
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(required = false) String cursor
    ) {
        long offset = parseCursor(cursor);
        return customerService.listCustomers(search, consentStatus, from, to, limit, offset);
    }

    @PostMapping
    public CustomerResponse create(@Valid @RequestBody CustomerCreateRequest request) {
        return customerService.create(request);
    }

    @GetMapping("/{customerId}")
    public CustomerResponse get(@PathVariable UUID customerId) {
        return customerService.get(customerId);
    }

    @PatchMapping("/{customerId}")
    public CustomerResponse update(@PathVariable UUID customerId, @RequestBody CustomerUpdateRequest request) {
        return customerService.update(customerId, request);
    }

    @DeleteMapping("/{customerId}")
    public StatusResponse delete(@PathVariable UUID customerId) {
        customerService.delete(customerId);
        return new StatusResponse("ok");
    }

    @PostMapping("/{customerId}/consent")
    public CustomerResponse updateConsent(@PathVariable UUID customerId, @Valid @RequestBody ConsentUpdateRequest request) {
        return customerService.updateConsent(customerId, request);
    }

    @GetMapping("/{customerId}/consent-history")
    public List<ConsentEventResponse> consentHistory(@PathVariable UUID customerId) {
        return customerService.consentHistory(customerId);
    }

    @PostMapping("/{customerId}/export")
    public GdprRequestResponse export(@PathVariable UUID customerId) {
        return customerService.exportCustomer(customerId);
    }

    @PostMapping("/{customerId}/erase")
    public GdprRequestResponse erase(@PathVariable UUID customerId) {
        return customerService.eraseCustomer(customerId);
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
