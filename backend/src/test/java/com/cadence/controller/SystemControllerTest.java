package com.cadence.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class SystemControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("GET /api/v1/system/health is public and returns 200 with UP status")
    void healthCheckPublicAccess() throws Exception {
        mockMvc.perform(get("/api/v1/system/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.service").value("cadence-backend"))
                .andExpect(header().exists("X-Trace-Id"));
    }

    @Test
    @DisplayName("GET /api/v1/system/auth-check without key returns 401 ProblemDetail")
    void authCheckRejectsMissingKey() throws Exception {
        mockMvc.perform(get("/api/v1/system/auth-check"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("Content-Type", "application/problem+json"))
                .andExpect(jsonPath("$.error_code").value("UNAUTHORIZED_API_KEY"))
                .andExpect(header().exists("X-Trace-Id"));
    }

    @Test
    @DisplayName("GET /api/v1/system/auth-check with header X-Cadence-Api-Key returns 200")
    void authCheckAcceptsValidHeaderKey() throws Exception {
        mockMvc.perform(get("/api/v1/system/auth-check")
                        .header("X-Cadence-Api-Key", "cadence-dev-secret-key-change-me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authenticated").value(true))
                .andExpect(jsonPath("$.token_source").value("HEADER_API_KEY"));
    }

    @Test
    @DisplayName("GET /api/v1/system/auth-check with Authorization Bearer returns 200")
    void authCheckAcceptsValidBearerKey() throws Exception {
        mockMvc.perform(get("/api/v1/system/auth-check")
                        .header("Authorization", "Bearer cadence-dev-secret-key-change-me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authenticated").value(true))
                .andExpect(jsonPath("$.token_source").value("BEARER_TOKEN"));
    }

    @Test
    @DisplayName("GET /api/v1/system/auth-check with invalid key returns 401")
    void authCheckRejectsInvalidKey() throws Exception {
        mockMvc.perform(get("/api/v1/system/auth-check")
                        .header("X-Cadence-Api-Key", "invalid-fake-key"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error_code").value("UNAUTHORIZED_API_KEY"));
    }
}
