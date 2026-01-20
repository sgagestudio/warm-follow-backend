package com.sgagestudio.warm_follow_backend.controller;

import com.sgagestudio.warm_follow_backend.dto.StatusResponse;
import com.sgagestudio.warm_follow_backend.service.UnsubscribeService;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/unsubscribe")
@CrossOrigin(origins = "*")
public class UnsubscribeController {
    private final UnsubscribeService unsubscribeService;

    public UnsubscribeController(UnsubscribeService unsubscribeService) {
        this.unsubscribeService = unsubscribeService;
    }

    @GetMapping("/{token}")
    public StatusResponse unsubscribe(@PathVariable String token) {
        unsubscribeService.unsubscribe(token);
        return new StatusResponse("ok");
    }
}
