package com.cadence.exception;

import com.cadence.config.TraceIdFilter;
import com.cadence.dto.ProblemDetailResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Controller advice ensuring all unhandled exceptions conform to RFC 7807 ProblemDetail.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(CadenceException.class)
    public ResponseEntity<ProblemDetailResponse> handleCadenceException(
            CadenceException ex,
            HttpServletRequest request
    ) {
        String traceId = resolveTraceId(request);
        ProblemDetailResponse problem = ProblemDetailResponse.of(
                ex.getStatus().value(),
                ex.getErrorCode(),
                ex.getMessage(),
                request.getRequestURI(),
                traceId,
                ex.getRemediationHint()
        );
        return ResponseEntity.status(ex.getStatus()).body(problem);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetailResponse> handleGeneralException(
            Exception ex,
            HttpServletRequest request
    ) {
        String traceId = resolveTraceId(request);
        log.error("Unhandled exception on request {}: {}", request.getRequestURI(), ex.getMessage(), ex);
        ProblemDetailResponse problem = ProblemDetailResponse.of(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "INTERNAL_SERVER_ERROR",
                ex.getMessage() != null ? ex.getMessage() : "An unexpected server error occurred.",
                request.getRequestURI(),
                traceId,
                "Check backend service logs correlating with trace_id: " + traceId
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(problem);
    }

    private String resolveTraceId(HttpServletRequest request) {
        String traceId = (String) request.getAttribute(TraceIdFilter.MDC_KEY);
        return traceId != null ? traceId : "trace-unknown";
    }
}
