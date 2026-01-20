package com.sgagestudio.warm_follow_backend.provider;

import com.sgagestudio.warm_follow_backend.config.GoogleOAuthProperties;
import com.sgagestudio.warm_follow_backend.util.ApiException;
import java.util.Map;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

@Service
public class GoogleOAuthClientImpl implements GoogleOAuthClient {
    private final GoogleOAuthProperties properties;
    private final RestClient restClient;

    public GoogleOAuthClientImpl(GoogleOAuthProperties properties, RestClient.Builder restClientBuilder) {
        this.properties = properties;
        this.restClient = restClientBuilder.build();
    }

    @Override
    public GoogleProfile exchangeCode(String code, String codeVerifier) {
        if (!properties.isEnabled()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "OAUTH_DISABLED", "Google OAuth disabled");
        }
        if ("mock".equalsIgnoreCase(properties.getMode())) {
            return new GoogleProfile(properties.getMockEmail(), properties.getMockSub());
        }
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("client_id", properties.getClientId());
        form.add("client_secret", properties.getClientSecret());
        form.add("code", code);
        form.add("code_verifier", codeVerifier);
        form.add("redirect_uri", properties.getRedirectUri());
        form.add("grant_type", "authorization_code");

        Map<String, Object> tokenResponse = restClient.post()
                .uri("https://oauth2.googleapis.com/token")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});
        if (tokenResponse == null || !tokenResponse.containsKey("access_token")) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "OAUTH_TOKEN_ERROR", "Unable to exchange code");
        }
        String accessToken = tokenResponse.get("access_token").toString();
        Map<String, Object> userInfo = restClient.get()
                .uri("https://openidconnect.googleapis.com/v1/userinfo")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});
        if (userInfo == null || !userInfo.containsKey("email") || !userInfo.containsKey("sub")) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "OAUTH_PROFILE_ERROR", "Unable to fetch profile");
        }
        return new GoogleProfile(userInfo.get("email").toString(), userInfo.get("sub").toString());
    }
}
