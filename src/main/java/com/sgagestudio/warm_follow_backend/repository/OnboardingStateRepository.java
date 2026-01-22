package com.sgagestudio.warm_follow_backend.repository;

import com.sgagestudio.warm_follow_backend.model.OnboardingState;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OnboardingStateRepository extends JpaRepository<OnboardingState, UUID> {
}
