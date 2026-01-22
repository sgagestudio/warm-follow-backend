package com.sgagestudio.warm_follow_backend.controller;

import com.sgagestudio.warm_follow_backend.dto.OnboardingStateResponse;
import com.sgagestudio.warm_follow_backend.dto.OnboardingUpdateRequest;
import com.sgagestudio.warm_follow_backend.service.OnboardingService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/onboarding")
@CrossOrigin(origins = "*")
public class OnboardingController {
    private final OnboardingService onboardingService;

    public OnboardingController(OnboardingService onboardingService) {
        this.onboardingService = onboardingService;
    }

    @GetMapping
    public OnboardingStateResponse getState() {
        return onboardingService.getState();
    }

    @PatchMapping
    public OnboardingStateResponse update(@Valid @RequestBody OnboardingUpdateRequest request) {
        return onboardingService.update(request);
    }
}
