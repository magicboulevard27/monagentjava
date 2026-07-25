package com.monagent.approval;

import com.monagent.analysis.Recommendation;
import com.monagent.analysis.RecommendationActionType;
import com.monagent.audit.AuditService;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class ApprovedActionExecutor {

    private final ApprovedActionExecutionProperties properties;
    private final AuditService auditService;

    public ApprovedActionExecutor(ApprovedActionExecutionProperties properties, AuditService auditService) {
        this.properties = properties;
        this.auditService = auditService;
    }

    public ControlledActionResult execute(Recommendation recommendation, ApprovalResponse approval, String actor) {
        auditService.record(actor, "APPROVED_ACTION_ATTEMPTED", "recommendation", recommendation.recommendationId(),
                "actionType=" + recommendation.actionType());
        if (!isSafe(recommendation.actionType())) {
            ControlledActionResult result = new ControlledActionResult(false, "Action is not a safe automated action");
            auditService.record(actor, "APPROVED_ACTION_BLOCKED", "recommendation", recommendation.recommendationId(), result.message());
            return result;
        }
        if (isProductionWriteAction(recommendation.actionType()) && !Boolean.TRUE.equals(properties.allowProductionWriteActions())) {
            ControlledActionResult result = new ControlledActionResult(false, "Production write actions are disabled");
            auditService.record(actor, "APPROVED_ACTION_BLOCKED", "recommendation", recommendation.recommendationId(), result.message());
            return result;
        }
        ControlledActionResult result = new ControlledActionResult(true, "Executed safely");
        auditService.record(actor, "APPROVED_ACTION_RESULT", "recommendation", recommendation.recommendationId(), result.message());
        return result;
    }

    public boolean revalidateTargetState(Recommendation recommendation, ApprovalResponse approval) {
        return approval != null
                && recommendation != null
                && recommendation.recommendationId().equals(approval.recommendationId())
                && ApprovalStatus.APPROVED.name().equals(approval.approvalStatus());
    }

    private boolean isSafe(RecommendationActionType actionType) {
        return switch (actionType) {
            case RESTART_SERVICE, SCALE_UP, SCALE_DATABASE_POOL, CHECK_DEPENDENCY, REVIEW_CONFIGURATION, INVESTIGATE_KAFKA_LAG -> true;
            case ROLLBACK_DEPLOYMENT, ADJUST_RESOURCE_LIMITS -> false;
            case NO_OP -> false;
        };
    }

    private boolean isProductionWriteAction(RecommendationActionType actionType) {
        return actionType == RecommendationActionType.RESTART_SERVICE
                || actionType == RecommendationActionType.SCALE_UP
                || actionType == RecommendationActionType.SCALE_DATABASE_POOL
                || actionType == RecommendationActionType.ROLLBACK_DEPLOYMENT;
    }

    public record ControlledActionResult(boolean executed, String message) {
    }
}
