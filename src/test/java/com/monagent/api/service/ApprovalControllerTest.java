package com.monagent.api.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.monagent.approval.ApprovalPolicy;
import com.monagent.approval.ApprovalRole;
import com.monagent.approval.ApprovalService;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ApprovalControllerTest {

    @Test
    void rejectsUnauthorizedDecisions() {
        ApprovalService approvalService = mock(ApprovalService.class);
        ApprovalPolicy policy = new ApprovalPolicy();
        ApprovalController controller = new ApprovalController(approvalService, policy);

        assertThatThrownBy(() -> controller.approve(UUID.randomUUID(),
                new com.monagent.approval.ApprovalDecisionRequest("actor", "reason"),
                ApprovalRole.VIEWER))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not authorized");
    }

    @Test
    void listsApprovals() {
        ApprovalService approvalService = mock(ApprovalService.class);
        ApprovalPolicy policy = new ApprovalPolicy();
        ApprovalController controller = new ApprovalController(approvalService, policy);
        when(approvalService.list()).thenReturn(List.of());

        List<?> approvals = controller.list();

        org.assertj.core.api.Assertions.assertThat(approvals).isEmpty();
    }
}
