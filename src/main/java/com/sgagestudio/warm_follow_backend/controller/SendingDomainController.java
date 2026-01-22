package com.sgagestudio.warm_follow_backend.controller;

import com.sgagestudio.warm_follow_backend.dto.SendingDomainCreateRequest;
import com.sgagestudio.warm_follow_backend.dto.SendingDomainListResponse;
import com.sgagestudio.warm_follow_backend.dto.SendingDomainResponse;
import com.sgagestudio.warm_follow_backend.service.SendingDomainService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/sending-domains")
@CrossOrigin(origins = "*")
public class SendingDomainController {
    private final SendingDomainService sendingDomainService;

    public SendingDomainController(SendingDomainService sendingDomainService) {
        this.sendingDomainService = sendingDomainService;
    }

    @GetMapping
    public SendingDomainListResponse list() {
        return sendingDomainService.list();
    }

    @PostMapping
    public SendingDomainResponse create(@Valid @RequestBody SendingDomainCreateRequest request) {
        return sendingDomainService.create(request);
    }

    @GetMapping("/{domainId}")
    public SendingDomainResponse get(@PathVariable UUID domainId) {
        return sendingDomainService.get(domainId);
    }

    @PostMapping("/{domainId}/verify")
    public SendingDomainResponse verify(@PathVariable UUID domainId) {
        return sendingDomainService.verify(domainId);
    }
}
