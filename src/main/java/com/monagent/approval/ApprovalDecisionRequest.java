package com.monagent.approval;

import jakarta.validation.constraints.NotBlank;

public record ApprovalDecisionRequest(
        @NotBlank String actor,
        @NotBlank String reason) {
}
