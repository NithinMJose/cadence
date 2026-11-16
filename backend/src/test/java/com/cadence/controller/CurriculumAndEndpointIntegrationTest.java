package com.cadence.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class CurriculumAndEndpointIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private static final String API_KEY = "cadence-dev-secret-key-change-me";

    @Test
    @DisplayName("Verify seed curriculum, fetch weekly schedule, and review card flow")
    void testEndToEndCurriculumFlow() throws Exception {
        // 1. Seed demo curriculum
        mockMvc.perform(post("/api/v1/demo/seed")
                        .header("X-Cadence-Api-Key", API_KEY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"));

        // 2. Fetch weekly schedule
        mockMvc.perform(get("/api/v1/schedule/week")
                        .header("X-Cadence-Api-Key", API_KEY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total_blocks").isNumber())
                .andExpect(jsonPath("$.blocks").isArray());

        // 3. Fetch due cards
        mockMvc.perform(get("/api/v1/cards/due")
                        .header("X-Cadence-Api-Key", API_KEY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }
}
