package com.sgagestudio.warm_follow_backend.controller;

import com.sgagestudio.warm_follow_backend.dto.PrivacySettingsResponse;
import com.sgagestudio.warm_follow_backend.dto.PrivacySettingsUpdateRequest;
import com.sgagestudio.warm_follow_backend.service.PrivacySettingsService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/privacy")
@CrossOrigin(origins = "*")
public class PrivacyController {
    private final PrivacySettingsService privacySettingsService;

    public PrivacyController(PrivacySettingsService privacySettingsService) {
        this.privacySettingsService = privacySettingsService;
    }

    @GetMapping("/settings")
    public PrivacySettingsResponse getSettings() {
        return privacySettingsService.getSettings();
    }

    @PatchMapping("/settings")
    public PrivacySettingsResponse updateSettings(@RequestBody PrivacySettingsUpdateRequest request) {
        return privacySettingsService.update(request);
    }
}
