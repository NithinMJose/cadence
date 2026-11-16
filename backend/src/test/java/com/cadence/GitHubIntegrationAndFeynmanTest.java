package com.cadence;

import com.cadence.client.AiServiceClient;
import com.cadence.model.*;
import com.cadence.repository.TimeBlockRepository;
import com.cadence.repository.TopicCardRepository;
import com.cadence.service.CurriculumSeederService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class GitHubIntegrationAndFeynmanTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TopicCardRepository cardRepository;

    @Autowired
    private TimeBlockRepository blockRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ZoneId zoneId;

    @MockitoBean
    private AiServiceClient aiServiceClient;

    private static final String API_KEY = "cadence-dev-secret-key-change-me";

    @BeforeEach
    void setup() {
        cardRepository.deleteAll();
        blockRepository.deleteAll();
    }

    @Test
    void testAnalyzeRawDiffGeneratesCardAndWarmupBrief() throws Exception {
        // Mock AI Service commit autopsy response
        String autopsyJson = """
        {
          "session_warmup_brief": [
            "1. Refactored FSRS spaced repetition equations",
            "2. Note stability decay boundary",
            "3. Run integration tests"
          ],
          "emergency_floor_microquiz": "What happens when stability is 0?",
          "flashcards": [
            {
              "title": "FSRS Decay Factor",
              "topic_tag": "spaced_repetition",
              "summary": "Decay factor governs stability decay rate over time",
              "emergency_floor_prompt": "Recall decay exponent",
              "rubric_checkpoints": ["point 1", "point 2", "point 3", "point 4"]
            }
          ]
        }
        """;
        Mockito.when(aiServiceClient.callCommitAutopsy(any(), anyString()))
                .thenReturn(objectMapper.readTree(autopsyJson));

        // Create an upcoming scheduled block to receive warmup brief
        TimeBlockEntity block = new TimeBlockEntity();
        block.setId(UUID.randomUUID());
        block.setUserId(CurriculumSeederService.DEMO_USER_ID);
        block.setTitle("Evening Deep Work");
        block.setTopicTag("upskill");
        block.setStartTime(ZonedDateTime.now(zoneId).plusHours(2));
        block.setEndTime(ZonedDateTime.now(zoneId).plusHours(3));
        block.setStatus(BlockStatus.SCHEDULED);
        block.setDeviceType(DeviceType.LAPTOP_DEEP);
        block.setPlannedDurationMin(60);
        block.setCreatedAt(ZonedDateTime.now(zoneId));
        block.setUpdatedAt(ZonedDateTime.now(zoneId));
        blockRepository.save(block);

        String rawDiffPayload = """
        {
          "rawDiff": "diff --git a/fsrs.py b/fsrs.py\\n+DECAY = -0.5",
          "commitMessage": "feat(fsrs): add decay factor",
          "repoName": "cadence"
        }
        """;

        mockMvc.perform(post("/api/v1/integrations/github/analyze-raw-diff")
                        .header("X-Cadence-Api-Key", API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(rawDiffPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cardsGenerated").value(1))
                .andExpect(jsonPath("$.warmupBrief.length()").value(3))
                .andExpect(jsonPath("$.emergencyFloorQuiz").value("What happens when stability is 0?"));

        // Verify card persisted in DB
        assertThat(cardRepository.findAll()).hasSize(1);
        TopicCardEntity savedCard = cardRepository.findAll().get(0);
        assertThat(savedCard.getTitle()).isEqualTo("FSRS Decay Factor");

        // Verify block received warmup brief
        TimeBlockEntity updatedBlock = blockRepository.findById(block.getId()).orElseThrow();
        assertThat(updatedBlock.getWarmupBriefJson()).contains("Refactored FSRS spaced repetition equations");
        assertThat(updatedBlock.getCardId()).isEqualTo(savedCard.getId());
    }

    @Test
    void testFeynmanGradeEvaluationUpdatesFsrsState() throws Exception {
        TopicCardEntity card = new TopicCardEntity();
        card.setId(UUID.randomUUID());
        card.setUserId(CurriculumSeederService.DEMO_USER_ID);
        card.setTitle("KV Cache Scaling");
        card.setTopicTag("ai_eng");
        card.setConceptSummary("KV cache dynamics in LLMs");
        card.setEmergencyFloorPrompt("Recall FP16 byte size");
        card.setRubricJson("[\"Key per layer\", \"Linear scaling\", \"Saves recomputation\", \"FP16 2 bytes\"]");
        card.setState(CardState.NEW);
        card.setCreatedAt(ZonedDateTime.now(zoneId));
        card.setUpdatedAt(ZonedDateTime.now(zoneId));
        cardRepository.save(card);

        // Mock AI Service Feynman grading response
        String feynmanJson = """
        {
          "fsrs_grade": 4,
          "matched_checkpoints": ["Key per layer", "Linear scaling", "Saves recomputation", "FP16 2 bytes"],
          "missing_checkpoints": [],
          "feedback": "Flawless explanation!",
          "_debug_trace": {"trace_id": "trace-mock-test"}
        }
        """;
        Mockito.when(aiServiceClient.callFeynmanGrade(any(), anyString()))
                .thenReturn(objectMapper.readTree(feynmanJson));

        String requestBody = """
        {
          "transcript": "KV cache saves keys and values per layer to avoid recomputation, scaling linearly in FP16 format."
        }
        """;

        mockMvc.perform(post("/api/v1/cards/" + card.getId() + "/feynman-grade")
                        .header("X-Cadence-Api-Key", API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fsrsGrade").value(4))
                .andExpect(jsonPath("$.matchedCheckpoints.length()").value(4))
                .andExpect(jsonPath("$.feedback").value("Flawless explanation!"))
                .andExpect(jsonPath("$.stability").isNumber())
                .andExpect(jsonPath("$.scheduledDays").isNumber());

        TopicCardEntity updated = cardRepository.findById(card.getId()).orElseThrow();
        assertThat(updated.getReps()).isEqualTo(1);
        assertThat(updated.getStability()).isGreaterThan(0.0);
        assertThat(updated.getLastReviewAt()).isNotNull();
    }
}
