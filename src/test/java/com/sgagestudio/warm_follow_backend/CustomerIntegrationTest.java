package com.sgagestudio.warm_follow_backend;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sgagestudio.warm_follow_backend.dto.CustomerCreateRequest;
import com.sgagestudio.warm_follow_backend.dto.RegisterRequest;
import com.sgagestudio.warm_follow_backend.model.ConsentStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CustomerIntegrationTest extends AbstractIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void createAndListCustomers() throws Exception {
        String accessToken = registerAndGetAccessToken("customer-owner@example.com");

        CustomerCreateRequest request = new CustomerCreateRequest(
                "Alice",
                "Smith",
                "alice@example.com",
                "+11111111",
                ConsentStatus.granted,
                List.of("email"),
                "web",
                "proof-1"
        );
        String body = objectMapper.writeValueAsString(request);
        mockMvc.perform(post("/customers")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());

        mockMvc.perform(get("/customers")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk());
    }

    private String registerAndGetAccessToken(String email) throws Exception {
        RegisterRequest register = new RegisterRequest(email, "Owner", "StrongPass1");
        String registerJson = objectMapper.writeValueAsString(register);
        String response = mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerJson))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        JsonNode node = objectMapper.readTree(response);
        return node.get("access_token").asText();
    }
}
