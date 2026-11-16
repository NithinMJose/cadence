package com.cadence.client;

import com.cadence.exception.CadenceException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;

/**
 * HTTP client communicating with Python FastAPI AI & Solver Service (Port 8000).
 */
@Component
public class AiServiceClient {

    private static final Logger log = LoggerFactory.getLogger(AiServiceClient.class);

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public AiServiceClient(
            @Value("${cadence.ai-service.url:http://localhost:8000}") String baseUrl,
            ObjectMapper objectMapper
    ) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);
        factory.setReadTimeout(30000);
        this.restClient = RestClient.builder()
                .requestFactory(factory)
                .baseUrl(baseUrl)
                .build();
        this.objectMapper = objectMapper;
    }

    /**
     * Executes the end-to-end AI Replan pipeline on the Python FastAPI service.
     */
    public JsonNode callReplanPipeline(Map<String, Object> payload, String traceId) {
        try {
            log.info("Dispatching replan request to AI Service with trace_id={}", traceId);
            return restClient.post()
                    .uri("/api/v1/ai/replan")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("X-Trace-Id", traceId != null ? traceId : "trace-backend")
                    .body(payload)
                    .retrieve()
                    .body(JsonNode.class);
        } catch (Exception ex) {
            log.error("AI Service call failed for trace_id={}: {}", traceId, ex.getMessage());
            throw new CadenceException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "AI_SERVICE_UNAVAILABLE",
                    "Failed to communicate with AI & Solver service: " + ex.getMessage(),
                    "Verify the Python ai-service is running on port 8000."
            );
        }
    }
}
