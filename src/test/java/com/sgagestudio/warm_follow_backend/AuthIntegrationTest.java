package com.sgagestudio.warm_follow_backend;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sgagestudio.warm_follow_backend.dto.LoginRequest;
import com.sgagestudio.warm_follow_backend.dto.RegisterRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthIntegrationTest extends AbstractIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void registerAndLogin() throws Exception {
        RegisterRequest register = new RegisterRequest("user@example.com", "Test User", "Test Workspace", "StrongPass1");
        String registerJson = objectMapper.writeValueAsString(register);
        String registerResponse = mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerJson))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        JsonNode registerNode = objectMapper.readTree(registerResponse);
        String refreshToken = registerNode.get("refresh_token").asText();

        LoginRequest login = new LoginRequest("user@example.com", "StrongPass1");
        String loginJson = objectMapper.writeValueAsString(login);
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson))
                .andExpect(status().isOk());

        String refreshJson = objectMapper.writeValueAsString(new com.sgagestudio.warm_follow_backend.dto.RefreshRequest(refreshToken));
        mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(refreshJson))
                .andExpect(status().isOk());
    }
}
