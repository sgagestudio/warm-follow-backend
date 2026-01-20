package com.sgagestudio.warm_follow_backend.repository;

import com.sgagestudio.warm_follow_backend.model.GdprRequest;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GdprRequestRepository extends JpaRepository<GdprRequest, UUID> {
}
