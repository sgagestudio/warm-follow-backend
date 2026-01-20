package com.sgagestudio.warm_follow_backend.controller;

import com.sgagestudio.warm_follow_backend.dto.SettingsResponse;
import com.sgagestudio.warm_follow_backend.dto.SettingsUpdateRequest;
import com.sgagestudio.warm_follow_backend.service.SettingsService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/settings")
@CrossOrigin(origins = "*")
public class SettingsController {
    private final SettingsService settingsService;

    public SettingsController(SettingsService settingsService) {
        this.settingsService = settingsService;
    }

    @GetMapping
    public SettingsResponse get() {
        return settingsService.getSettings();
    }

    @PatchMapping
    public SettingsResponse update(@RequestBody SettingsUpdateRequest request) {
        return settingsService.update(request);
    }
}
