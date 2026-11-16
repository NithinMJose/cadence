package com.cadence.config;

import com.cadence.dto.ProblemDetailResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Stateless API key and Bearer token authentication filter for /api/v1/** endpoints.
 */
@Component
public class ApiKeyAuthFilter extends OncePerRequestFilter {

    public static final String API_KEY_HEADER = "X-Cadence-Api-Key";
    public static final String AUTH_HEADER = "Authorization";
    public static final String BEARER_PREFIX = "Bearer ";

    private final String expectedApiKey;
    private final ObjectMapper objectMapper;

    public ApiKeyAuthFilter(
            @Value("${cadence.api-key:cadence-dev-secret-key-change-me}") String expectedApiKey,
            ObjectMapper objectMapper
    ) {
        this.expectedApiKey = expectedApiKey;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String path = request.getRequestURI();

        // Allow CORS pre-flight OPTIONS requests, public health endpoints, and actuator without API key
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())
                || path.equals("/api/v1/system/health")
                || path.startsWith("/actuator")) {
            filterChain.doFilter(request, response);
            return;
        }

        String providedKey = extractApiKey(request);
        if (providedKey == null || !providedKey.equals(expectedApiKey)) {
            sendUnauthorizedError(request, response);
            return;
        }

        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                "cadence-client",
                providedKey,
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );
        SecurityContextHolder.getContext().setAuthentication(auth);

        filterChain.doFilter(request, response);
    }

    private String extractApiKey(HttpServletRequest request) {
        String key = request.getHeader(API_KEY_HEADER);
        if (key != null && !key.isBlank()) {
            return key.trim();
        }

        String authHeader = request.getHeader(AUTH_HEADER);
        if (authHeader != null && authHeader.startsWith(BEARER_PREFIX)) {
            return authHeader.substring(BEARER_PREFIX.length()).trim();
        }

        return null;
    }

    private void sendUnauthorizedError(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws IOException {
        String traceId = (String) request.getAttribute(TraceIdFilter.MDC_KEY);
        if (traceId == null) {
            traceId = "trace-unknown";
        }

        ProblemDetailResponse problem = ProblemDetailResponse.of(
                HttpStatus.UNAUTHORIZED.value(),
                "UNAUTHORIZED_API_KEY",
                "Missing or invalid API key credentials.",
                request.getRequestURI(),
                traceId,
                "Provide a valid 'X-Cadence-Api-Key' header or 'Authorization: Bearer <key>'."
        );

        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType("application/problem+json");
        response.setHeader(TraceIdFilter.TRACE_HEADER, traceId);
        objectMapper.writeValue(response.getOutputStream(), problem);
    }
}
