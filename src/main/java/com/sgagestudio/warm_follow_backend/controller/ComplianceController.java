package com.sgagestudio.warm_follow_backend.controller;

import com.sgagestudio.warm_follow_backend.dto.ComplianceAssessmentRequest;
import com.sgagestudio.warm_follow_backend.dto.ComplianceAssessmentResponse;
import com.sgagestudio.warm_follow_backend.dto.ProcessingRecordResponse;
import com.sgagestudio.warm_follow_backend.service.ComplianceService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/compliance")
@CrossOrigin(origins = "*")
public class ComplianceController {
    private final ComplianceService complianceService;

    public ComplianceController(ComplianceService complianceService) {
        this.complianceService = complianceService;
    }

    @GetMapping("/processing-record")
    public ProcessingRecordResponse processingRecord() {
        return complianceService.processingRecord();
    }

    @PostMapping("/data-transfer-assessment")
    public ComplianceAssessmentResponse createAssessment(@Valid @RequestBody ComplianceAssessmentRequest request) {
        return complianceService.createAssessment(request);
    }
}
