package com.sgagestudio.warm_follow_backend.repository;

import com.sgagestudio.warm_follow_backend.model.ComplianceAssessment;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ComplianceAssessmentRepository extends JpaRepository<ComplianceAssessment, UUID> {
}
