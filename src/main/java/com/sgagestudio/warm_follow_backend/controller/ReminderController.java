package com.sgagestudio.warm_follow_backend.controller;

import com.sgagestudio.warm_follow_backend.dto.PagedResponse;
import com.sgagestudio.warm_follow_backend.dto.ReminderCreateRequest;
import com.sgagestudio.warm_follow_backend.dto.ReminderResponse;
import com.sgagestudio.warm_follow_backend.dto.ReminderUpdateRequest;
import com.sgagestudio.warm_follow_backend.model.Channel;
import com.sgagestudio.warm_follow_backend.model.ReminderStatus;
import com.sgagestudio.warm_follow_backend.service.ReminderService;
import com.sgagestudio.warm_follow_backend.util.ApiException;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/reminders")
@CrossOrigin(origins = "*")
public class ReminderController {
    private final ReminderService reminderService;

    public ReminderController(ReminderService reminderService) {
        this.reminderService = reminderService;
    }

    @GetMapping
    public PagedResponse<ReminderResponse> list(
            @RequestParam(required = false) ReminderStatus status,
            @RequestParam(required = false) Channel channel,
            @RequestParam(required = false) LocalDate from,
            @RequestParam(required = false) LocalDate to,
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(required = false) String cursor
    ) {
        long offset = parseCursor(cursor);
        return reminderService.listReminders(status, channel, from, to, limit, offset);
    }

    @PostMapping
    public ReminderResponse create(@Valid @RequestBody ReminderCreateRequest request) {
        return reminderService.create(request);
    }

    @GetMapping("/{reminderId}")
    public ReminderResponse get(@PathVariable UUID reminderId) {
        return reminderService.get(reminderId);
    }

    @PatchMapping("/{reminderId}")
    public ReminderResponse update(@PathVariable UUID reminderId, @RequestBody ReminderUpdateRequest request) {
        return reminderService.update(reminderId, request);
    }

    @PostMapping("/{reminderId}/cancel")
    public ReminderResponse cancel(@PathVariable UUID reminderId) {
        return reminderService.cancel(reminderId);
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
