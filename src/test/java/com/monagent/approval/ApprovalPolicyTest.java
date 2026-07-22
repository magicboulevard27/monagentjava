package com.monagent.approval;

import static org.assertj.core.api.Assertions.assertThat;

import com.monagent.analysis.RecommendationActionType;
import org.junit.jupiter.api.Test;

class ApprovalPolicyTest {

    @Test
    void allowsApproverRolesAndBlocksViewers() {
        ApprovalPolicy policy = new ApprovalPolicy();

        assertThat(policy.canRequest(ApprovalRole.OPERATOR, RecommendationActionType.RESTART_SERVICE)).isTrue();
        assertThat(policy.canDecide(ApprovalRole.APPROVER)).isTrue();
        assertThat(policy.canDecide(ApprovalRole.VIEWER)).isFalse();
    }
}
