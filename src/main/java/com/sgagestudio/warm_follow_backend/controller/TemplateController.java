package com.sgagestudio.warm_follow_backend.controller;

import com.sgagestudio.warm_follow_backend.dto.PagedResponse;
import com.sgagestudio.warm_follow_backend.dto.StatusResponse;
import com.sgagestudio.warm_follow_backend.dto.TemplateCreateRequest;
import com.sgagestudio.warm_follow_backend.dto.TemplatePreviewRequest;
import com.sgagestudio.warm_follow_backend.dto.TemplatePreviewResponse;
import com.sgagestudio.warm_follow_backend.dto.TemplateResponse;
import com.sgagestudio.warm_follow_backend.dto.TemplateUpdateRequest;
import com.sgagestudio.warm_follow_backend.service.TemplateService;
import com.sgagestudio.warm_follow_backend.util.ApiException;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/templates")
@CrossOrigin(origins = "*")
public class TemplateController {
    private final TemplateService templateService;

    public TemplateController(TemplateService templateService) {
        this.templateService = templateService;
    }

    @GetMapping
    public PagedResponse<TemplateResponse> list(
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(required = false) String cursor,
            @RequestParam(required = false) String search
    ) {
        long offset = parseCursor(cursor);
        return templateService.listTemplates(limit, offset, search);
    }

    @PostMapping
    public TemplateResponse create(@Valid @RequestBody TemplateCreateRequest request) {
        return templateService.create(request);
    }

    @GetMapping("/{templateId}")
    public TemplateResponse get(@PathVariable Long templateId) {
        return templateService.get(templateId);
    }

    @PatchMapping("/{templateId}")
    public TemplateResponse update(@PathVariable Long templateId, @RequestBody TemplateUpdateRequest request) {
        return templateService.update(templateId, request);
    }

    @DeleteMapping("/{templateId}")
    public StatusResponse delete(@PathVariable Long templateId) {
        templateService.delete(templateId);
        return new StatusResponse("ok");
    }

    @PostMapping("/preview")
    public TemplatePreviewResponse preview(@Valid @RequestBody TemplatePreviewRequest request) {
        return templateService.preview(request);
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
