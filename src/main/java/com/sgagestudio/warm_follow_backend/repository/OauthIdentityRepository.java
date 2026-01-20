package com.sgagestudio.warm_follow_backend.repository;

import com.sgagestudio.warm_follow_backend.model.AuthProvider;
import com.sgagestudio.warm_follow_backend.model.OauthIdentity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OauthIdentityRepository extends JpaRepository<OauthIdentity, UUID> {
    Optional<OauthIdentity> findByProviderAndProviderSubject(AuthProvider provider, String providerSubject);

    Optional<OauthIdentity> findByUser_IdAndProvider(UUID userId, AuthProvider provider);
}
