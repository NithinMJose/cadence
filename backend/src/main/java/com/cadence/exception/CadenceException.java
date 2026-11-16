package com.cadence.exception;

import org.springframework.http.HttpStatus;

/**
 * Base application domain exception mapping to RFC 7807 ProblemDetail attributes.
 */
public class CadenceException extends RuntimeException {

    private final HttpStatus status;
    private final String errorCode;
    private final String remediationHint;

    public CadenceException(HttpStatus status, String errorCode, String message, String remediationHint) {
        super(message);
        this.status = status;
        this.errorCode = errorCode;
        this.remediationHint = remediationHint;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public String getRemediationHint() {
        return remediationHint;
    }
}
