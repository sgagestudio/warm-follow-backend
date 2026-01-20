package com.sgagestudio.warm_follow_backend.controller;

import com.sgagestudio.warm_follow_backend.dto.StatusResponse;
import com.sgagestudio.warm_follow_backend.dto.WebhookStatusRequest;
import com.sgagestudio.warm_follow_backend.service.WebhookService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/webhooks")
@CrossOrigin(origins = "*")
public class WebhookController {
    private final WebhookService webhookService;

    public WebhookController(WebhookService webhookService) {
        this.webhookService = webhookService;
    }

    @PostMapping("/email/status")
    public StatusResponse emailStatus(
            @Valid @RequestBody WebhookStatusRequest request,
            @RequestHeader(name = "X-Signature", required = false) String signature
    ) {
        webhookService.handleEmailStatus(request, signature);
        return new StatusResponse("ok");
    }

    @PostMapping("/sms/status")
    public StatusResponse smsStatus(
            @Valid @RequestBody WebhookStatusRequest request,
            @RequestHeader(name = "X-Signature", required = false) String signature
    ) {
        webhookService.handleSmsStatus(request, signature);
        return new StatusResponse("ok");
    }
}
