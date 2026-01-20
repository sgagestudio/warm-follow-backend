package com.sgagestudio.warm_follow_backend;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sgagestudio.warm_follow_backend.dto.CustomerCreateRequest;
import com.sgagestudio.warm_follow_backend.dto.RegisterRequest;
import com.sgagestudio.warm_follow_backend.dto.ReminderCreateRequest;
import com.sgagestudio.warm_follow_backend.dto.TemplateCreateRequest;
import com.sgagestudio.warm_follow_backend.model.Channel;
import com.sgagestudio.warm_follow_backend.model.ConsentStatus;
import com.sgagestudio.warm_follow_backend.model.ReminderFrequency;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ReminderIntegrationTest extends AbstractIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void createReminder() throws Exception {
        String accessToken = registerAndGetAccessToken("reminder-owner@example.com");

        Long templateId = createTemplate(accessToken);
        UUID customerId = createCustomer(accessToken);

        ReminderCreateRequest reminderRequest = new ReminderCreateRequest(
                templateId,
                List.of(customerId),
                Channel.email,
                ReminderFrequency.once,
                LocalTime.of(9, 0),
                LocalDate.now().plusDays(1)
        );
        String body = objectMapper.writeValueAsString(reminderRequest);
        mockMvc.perform(post("/reminders")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());
    }

    private Long createTemplate(String accessToken) throws Exception {
        TemplateCreateRequest request = new TemplateCreateRequest(
                "Template 1",
                "Subject",
                "Hello {{name}}",
                Channel.email
        );
        String body = objectMapper.writeValueAsString(request);
        String response = mockMvc.perform(post("/templates")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        JsonNode node = objectMapper.readTree(response);
        return node.get("id").asLong();
    }

    private UUID createCustomer(String accessToken) throws Exception {
        CustomerCreateRequest request = new CustomerCreateRequest(
                "Bob",
                "Jones",
                "bob@example.com",
                "+22222222",
                ConsentStatus.granted,
                List.of("email"),
                "web",
                "proof-2"
        );
        String body = objectMapper.writeValueAsString(request);
        String response = mockMvc.perform(post("/customers")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        JsonNode node = objectMapper.readTree(response);
        return UUID.fromString(node.get("id").asText());
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
