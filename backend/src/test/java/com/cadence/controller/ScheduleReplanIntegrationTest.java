package com.cadence.controller;

import com.cadence.client.AiServiceClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.ZonedDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class ScheduleReplanIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AiServiceClient aiServiceClient;

    private static final String API_KEY = "cadence-dev-secret-key-change-me";

    @Test
    @DisplayName("Verify two-phase replan propose and confirm workflow")
    void testProposeAndConfirmReplanFlow() throws Exception {
        // 1. Seed demo curriculum
        mockMvc.perform(post("/api/v1/demo/seed")
                        .header("X-Cadence-Api-Key", API_KEY))
                .andExpect(status().isOk());

        UUID taskId = UUID.randomUUID();
        ZonedDateTime newStart = ZonedDateTime.now().plusDays(1);
        ZonedDateTime newEnd = newStart.plusMinutes(45);

        String mockAiResponseJson = """
        {
            "status": "OPTIMAL",
            "ast_summary": "Blocked Friday night for team dinner",
            "scheduled_blocks": [
                {
                    "task_id": "%s",
                    "title": "System Design: Cache Stampede",
                    "topic_tag": "system_design",
                    "start_time": "%s",
                    "end_time": "%s",
                    "device_venue": "LAPTOP_DEEP",
                    "is_emergency_floor": false,
                    "was_deferred": false,
                    "duration_min": 45
                }
            ],
            "diff_items": [
                {
                    "task_id": "%s",
                    "title": "System Design: Cache Stampede",
                    "old_start": null,
                    "old_end": null,
                    "new_start": "%s",
                    "new_end": "%s",
                    "action": "MOVED",
                    "causal_reason": "Moved to optimize focus window"
                }
            ],
            "tradeoff_cards": [],
            "total_scheduled_minutes": 45,
            "total_slack_minutes": 0,
            "churn_score": 1,
            "_debug_trace": {
                "trace_id": "test-trace",
                "solver_status": "OPTIMAL",
                "solver_wall_time_ms": 12.5
            }
        }
        """.formatted(taskId, newStart, newEnd, taskId, newStart, newEnd);

        Mockito.when(aiServiceClient.callReplanPipeline(any(), any()))
                .thenReturn(objectMapper.readTree(mockAiResponseJson));

        // 2. Propose replan
        String requestJson = """
        {
            "user_message": "Block Friday night from 7pm to 11pm for party"
        }
        """;

        MvcResult proposeResult = mockMvc.perform(post("/api/v1/schedule/replan/propose")
                        .header("X-Cadence-Api-Key", API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.proposal_id").isNotEmpty())
                .andExpect(jsonPath("$.diff_items").isArray())
                .andExpect(jsonPath("$._debug_trace.solver_status").value("OPTIMAL"))
                .andReturn();

        String proposalIdStr = objectMapper.readTree(proposeResult.getResponse().getContentAsString())
                .path("proposal_id").asText();
        UUID proposalId = UUID.fromString(proposalIdStr);

        // 3. Confirm replan (Phase 2A)
        mockMvc.perform(post("/api/v1/schedule/replan/" + proposalId + "/confirm")
                        .header("X-Cadence-Api-Key", API_KEY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.updated_blocks_count").value(1))
                .andExpect(jsonPath("$.confirmed_at").isNotEmpty());

        // 4. Confirming again must fail with 409 CONFLICT
        mockMvc.perform(post("/api/v1/schedule/replan/" + proposalId + "/confirm")
                        .header("X-Cadence-Api-Key", API_KEY))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error_code").value("INVALID_PROPOSAL_STATUS"));
    }

    @Test
    @DisplayName("Verify replan propose and reject workflow")
    void testProposeAndRejectReplanFlow() throws Exception {
        mockMvc.perform(post("/api/v1/demo/seed")
                        .header("X-Cadence-Api-Key", API_KEY))
                .andExpect(status().isOk());

        String mockAiResponseJson = """
        {
            "status": "OPTIMAL",
            "ast_summary": "Replan test",
            "scheduled_blocks": [],
            "diff_items": [],
            "tradeoff_cards": [],
            "total_scheduled_minutes": 0,
            "total_slack_minutes": 0,
            "churn_score": 0,
            "_debug_trace": { "trace_id": "trace-reject" }
        }
        """;
        Mockito.when(aiServiceClient.callReplanPipeline(any(), any()))
                .thenReturn(objectMapper.readTree(mockAiResponseJson));

        // 1. Propose
        MvcResult proposeResult = mockMvc.perform(post("/api/v1/schedule/replan/propose")
                        .header("X-Cadence-Api-Key", API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"user_message\":\"Adjust study time\"}"))
                .andExpect(status().isOk())
                .andReturn();

        String proposalIdStr = objectMapper.readTree(proposeResult.getResponse().getContentAsString())
                .path("proposal_id").asText();

        // 2. Reject
        mockMvc.perform(post("/api/v1/schedule/replan/" + proposalIdStr + "/reject")
                        .header("X-Cadence-Api-Key", API_KEY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"));

        // 3. Confirming rejected proposal must fail with 409 CONFLICT
        mockMvc.perform(post("/api/v1/schedule/replan/" + proposalIdStr + "/confirm")
                        .header("X-Cadence-Api-Key", API_KEY))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error_code").value("INVALID_PROPOSAL_STATUS"));
    }
}
