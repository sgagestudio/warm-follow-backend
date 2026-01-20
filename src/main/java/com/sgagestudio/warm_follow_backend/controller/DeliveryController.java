package com.sgagestudio.warm_follow_backend.controller;

import com.sgagestudio.warm_follow_backend.dto.DeliveryResponse;
import com.sgagestudio.warm_follow_backend.service.DeliveryService;
import java.util.UUID;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/deliveries")
@CrossOrigin(origins = "*")
public class DeliveryController {
    private final DeliveryService deliveryService;

    public DeliveryController(DeliveryService deliveryService) {
        this.deliveryService = deliveryService;
    }

    @GetMapping("/{deliveryId}")
    public DeliveryResponse get(@PathVariable UUID deliveryId) {
        return deliveryService.getDelivery(deliveryId);
    }
}
