package com.cadence.controller;

import com.cadence.config.TraceIdFilter;
import com.cadence.service.CurriculumSeederService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Endpoint to seed the 30-day curriculum for applied AI, DSA, and Spring/Kotlin craft.
 */
@RestController
@RequestMapping("/api/v1/demo")
public class DemoSeederController {

    private final CurriculumSeederService seederService;

    public DemoSeederController(CurriculumSeederService seederService) {
        this.seederService = seederService;
    }

    @PostMapping("/seed")
    public ResponseEntity<Map<String, Object>> seedDemoData(HttpServletRequest request) {
        String traceId = (String) request.getAttribute(TraceIdFilter.MDC_KEY);
        seederService.seedCurriculum();

        return ResponseEntity.ok(Map.of(
                "status", "SUCCESS",
                "message", "30-day curriculum seeded with Applied AI, DSA, and Spring/Kotlin topics.",
                "user_id", CurriculumSeederService.DEMO_USER_ID.toString(),
                "trace_id", traceId != null ? traceId : "trace-demo-seed"
        ));
    }
}
