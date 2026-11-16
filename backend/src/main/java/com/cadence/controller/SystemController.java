package com.cadence.controller;

import com.cadence.config.TraceIdFilter;
import com.cadence.dto.AuthCheckResponse;
import com.cadence.dto.SystemHealthResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.ZoneId;
import java.time.ZonedDateTime;

/**
 * System diagnostic and telemetry controller.
 */
@RestController
@RequestMapping("/api/v1/system")
public class SystemController {

    private final ZoneId zoneId;
    private final String appName;

    public SystemController(
            ZoneId zoneId,
            @Value("${spring.application.name:cadence-backend}") String appName
    ) {
        this.zoneId = zoneId;
        this.appName = appName;
    }

    @GetMapping("/health")
    public ResponseEntity<SystemHealthResponse> healthCheck(HttpServletRequest request) {
        String traceId = (String) request.getAttribute(TraceIdFilter.MDC_KEY);
        SystemHealthResponse response = new SystemHealthResponse(
                "UP",
                appName,
                "0.1.0-SNAPSHOT",
                ZonedDateTime.now(zoneId),
                zoneId.getId(),
                traceId != null ? traceId : "trace-health-probe"
        );
        return ResponseEntity.ok(response);
    }

    @GetMapping("/auth-check")
    public ResponseEntity<AuthCheckResponse> authCheck(
            Authentication authentication,
            HttpServletRequest request
    ) {
        String traceId = (String) request.getAttribute(TraceIdFilter.MDC_KEY);
        String tokenSource = request.getHeader("X-Cadence-Api-Key") != null ? "HEADER_API_KEY" : "BEARER_TOKEN";
        AuthCheckResponse response = new AuthCheckResponse(
                true,
                authentication != null ? authentication.getName() : "cadence-client",
                tokenSource,
                ZonedDateTime.now(zoneId),
                traceId != null ? traceId : "trace-auth-probe"
        );
        return ResponseEntity.ok(response);
    }
}
