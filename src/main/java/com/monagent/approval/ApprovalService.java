package com.monagent.approval;

import com.monagent.analysis.RecommendationActionType;
import com.monagent.persistence.ApprovalEntity;
import com.monagent.persistence.ApprovalRepository;
import com.monagent.persistence.RecommendationEntity;
import com.monagent.persistence.RecommendationRepository;
import com.monagent.audit.AuditService;
import com.monagent.security.DataEncryptionService;
import com.monagent.web.SelfObservabilityMetrics;
import jakarta.persistence.EntityNotFoundException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ApprovalService {

    private static final Logger log = LoggerFactory.getLogger(ApprovalService.class);

    private final ApprovalRepository approvalRepository;
    private final RecommendationRepository recommendationRepository;
    private final AuditService auditService;
    private final SelfObservabilityMetrics metrics;
    private final ApprovedActionExecutor approvedActionExecutor;
    private final DataEncryptionService dataEncryptionService;

    public ApprovalService(ApprovalRepository approvalRepository,
                           RecommendationRepository recommendationRepository,
                           AuditService auditService,
                           SelfObservabilityMetrics metrics,
                           ApprovedActionExecutor approvedActionExecutor,
                           DataEncryptionService dataEncryptionService) {
        this.approvalRepository = approvalRepository;
        this.recommendationRepository = recommendationRepository;
        this.auditService = auditService;
        this.metrics = metrics;
        this.approvedActionExecutor = approvedActionExecutor;
        this.dataEncryptionService = dataEncryptionService;
    }

    @Transactional
    public ApprovalResponse request(UUID recommendationId, String actor, String reason) {
        log.info("Creating approval request recommendationId={} actor={}", recommendationId, actor);
        RecommendationEntity recommendation = recommendationRepository.findById(recommendationId)
                .orElseThrow(() -> new EntityNotFoundException("Recommendation not found: " + recommendationId));
        ensureAppropriateAction(recommendation);
        ensureNoExistingDecision(recommendationId);

        ApprovalEntity approval = new ApprovalEntity();
        approval.setApprovalId(UUID.randomUUID());
        approval.setRecommendationId(recommendationId);
        approval.setRequestedBy(actor);
        approval.setApprovalStatus(ApprovalStatus.REQUESTED.name());
        approval.setDecisionReason(dataEncryptionService.encrypt(reason));
        approval.setDecidedAt(null);
        approvalRepository.saveAndFlush(approval);
        auditService.record(actor, "APPROVAL_REQUESTED", "recommendation", recommendationId, sanitize(reason));
        metrics.incrementApprovalDecision(ApprovalStatus.REQUESTED.name());
        log.info("Approval requested approvalId={} recommendationId={}", approval.getApprovalId(), recommendationId);
        return toResponse(approval);
    }

    @Transactional
    public ApprovalResponse approve(UUID recommendationId, String actor, String reason) {
        log.info("Approving recommendationId={} actor={}", recommendationId, actor);
        ApprovalEntity approval = loadOpenApproval(recommendationId);
        if (actor.equalsIgnoreCase(approval.getRequestedBy())) {
            throw new IllegalArgumentException("Self-approval is not allowed");
        }
        approval.setApprovedBy(actor);
        approval.setApprovalStatus(ApprovalStatus.APPROVED.name());
        approval.setDecisionReason(dataEncryptionService.encrypt(reason));
        approval.setDecidedAt(Instant.now());
        approvalRepository.saveAndFlush(approval);
        auditService.record(actor, "APPROVAL_APPROVED", "recommendation", recommendationId, sanitize(reason));
        metrics.incrementApprovalDecision(ApprovalStatus.APPROVED.name());
        log.info("Approval approved approvalId={} recommendationId={}", approval.getApprovalId(), recommendationId);
        return toResponse(approval);
    }

    @Transactional
    public ApprovalResponse reject(UUID recommendationId, String actor, String reason) {
        log.info("Rejecting recommendationId={} actor={}", recommendationId, actor);
        ApprovalEntity approval = loadOpenApproval(recommendationId);
        approval.setApprovedBy(actor);
        approval.setApprovalStatus(ApprovalStatus.REJECTED.name());
        approval.setDecisionReason(dataEncryptionService.encrypt(reason));
        approval.setDecidedAt(Instant.now());
        approvalRepository.saveAndFlush(approval);
        auditService.record(actor, "APPROVAL_REJECTED", "recommendation", recommendationId, sanitize(reason));
        metrics.incrementApprovalDecision(ApprovalStatus.REJECTED.name());
        log.info("Approval rejected approvalId={} recommendationId={}", approval.getApprovalId(), recommendationId);
        return toResponse(approval);
    }

    @Transactional
    public ApprovedActionExecutor.ControlledActionResult executeApprovedAction(UUID recommendationId, String actor) {
        ApprovalEntity approval = loadOpenOrDecidedApproval(recommendationId);
        RecommendationEntity recommendation = recommendationRepository.findById(recommendationId)
                .orElseThrow(() -> new EntityNotFoundException("Recommendation not found: " + recommendationId));
        auditService.record(actor, "APPROVED_ACTION_REVALIDATED", "recommendation", recommendationId, "state=" + approval.getApprovalStatus());
        if (!approvedActionExecutor.revalidateTargetState(toRecommendation(recommendation), toResponse(approval))) {
            ApprovedActionExecutor.ControlledActionResult result = new ApprovedActionExecutor.ControlledActionResult(false, "Target state changed before execution");
            auditService.record(actor, "APPROVED_ACTION_BLOCKED", "recommendation", recommendationId, result.message());
            return result;
        }
        return approvedActionExecutor.execute(toRecommendation(recommendation), toResponse(approval), actor);
    }

    @Transactional(readOnly = true)
    public List<ApprovalResponse> list() {
        log.debug("Loading approvals");
        return approvalRepository.findAll().stream().map(this::toResponse).toList();
    }

    private ApprovalEntity loadOpenApproval(UUID recommendationId) {
        return approvalRepository.findAll().stream()
                .filter(item -> recommendationId.equals(item.getRecommendationId()))
                .filter(item -> ApprovalStatus.REQUESTED.name().equals(item.getApprovalStatus()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Open approval not found for recommendation: " + recommendationId));
    }

    private ApprovalEntity loadOpenOrDecidedApproval(UUID recommendationId) {
        return approvalRepository.findAll().stream()
                .filter(item -> recommendationId.equals(item.getRecommendationId()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Approval not found for recommendation: " + recommendationId));
    }

    private void ensureNoExistingDecision(UUID recommendationId) {
        boolean existing = approvalRepository.findAll().stream()
                .anyMatch(item -> recommendationId.equals(item.getRecommendationId())
                        && !ApprovalStatus.REQUESTED.name().equals(item.getApprovalStatus()));
        if (existing) {
            throw new IllegalArgumentException("Duplicate or replayed approval for recommendation: " + recommendationId);
        }
    }

    private void ensureAppropriateAction(RecommendationEntity recommendation) {
        RecommendationActionType actionType = RecommendationActionType.valueOf(recommendation.getActionType());
        if (actionType == RecommendationActionType.NO_OP) {
            throw new IllegalArgumentException("No-op recommendations cannot be approved");
        }
        if (!recommendation.isRequiresApproval()) {
            throw new IllegalArgumentException("Recommendation does not require approval");
        }
    }

    private ApprovalResponse toResponse(ApprovalEntity approval) {
        return new ApprovalResponse(
                approval.getApprovalId(),
                approval.getRecommendationId(),
                approval.getRequestedBy(),
                approval.getApprovedBy(),
                approval.getApprovalStatus(),
                dataEncryptionService.decrypt(approval.getDecisionReason()),
                approval.getDecidedAt(),
                approval.getVersion());
    }

    private String sanitize(String value) {
        if (value == null) {
            return "";
        }
        return value.replaceAll("(?i)(password|secret|token|api[-_ ]?key)=[^\\s,;]+", "$1=[REDACTED]");
    }

    private com.monagent.analysis.Recommendation toRecommendation(RecommendationEntity recommendation) {
        return new com.monagent.analysis.Recommendation(
                recommendation.getRecommendationId(),
                recommendation.getIncidentId(),
                com.monagent.analysis.RecommendationActionType.valueOf(recommendation.getActionType()),
                recommendation.getDescription(),
                com.monagent.analysis.RecommendationRiskLevel.valueOf(recommendation.getRiskLevel()),
                recommendation.isRequiresApproval(),
                recommendation.getStatus(),
                recommendation.getEvidenceSummary(),
                List.of(),
                recommendation.getCreatedAt());
    }
}
