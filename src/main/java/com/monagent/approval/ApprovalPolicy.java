package com.monagent.approval;

import com.monagent.analysis.RecommendationActionType;
import java.util.EnumSet;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class ApprovalPolicy {

    private static final Set<ApprovalRole> APPROVER_ROLES = EnumSet.of(ApprovalRole.APPROVER, ApprovalRole.ADMINISTRATOR);

    public boolean canRequest(ApprovalRole role, RecommendationActionType actionType) {
        return role == ApprovalRole.OPERATOR || role == ApprovalRole.APPROVER || role == ApprovalRole.ADMINISTRATOR;
    }

    public boolean canDecide(ApprovalRole role) {
        return APPROVER_ROLES.contains(role);
    }
}
