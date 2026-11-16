package com.cadence.dto.replan;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Size;

/**
 * Request payload to propose a schedule replan based on voice/text input or direct AST.
 */
public record ReplanProposeRequest(
        @Size(max = 1000, message = "User message cannot exceed 1000 characters")
        @JsonProperty("user_message")
        String userMessage,

        @JsonProperty("constraint_ast")
        Object constraintAst,

        @JsonProperty("force_overload_option")
        String forceOverloadOption
) {}
