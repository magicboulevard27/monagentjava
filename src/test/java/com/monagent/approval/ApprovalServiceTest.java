package com.monagent.approval;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.monagent.analysis.RecommendationActionType;
import com.monagent.persistence.ApprovalEntity;
import com.monagent.persistence.ApprovalRepository;
import com.monagent.persistence.RecommendationEntity;
import com.monagent.persistence.RecommendationRepository;
import com.monagent.audit.AuditService;
import com.monagent.web.SelfObservabilityMetrics;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ApprovalServiceTest {

    @Test
    void requestsApprovalsForRequiringRecommendations() {
        ApprovalRepository approvalRepository = mock(ApprovalRepository.class);
        RecommendationRepository recommendationRepository = mock(RecommendationRepository.class);
        AuditService auditService = mock(AuditService.class);
        ApprovalService service = new ApprovalService(
                approvalRepository,
                recommendationRepository,
                auditService,
                new SelfObservabilityMetrics(new io.micrometer.core.instrument.simple.SimpleMeterRegistry()));

        UUID recommendationId = UUID.randomUUID();
        RecommendationEntity recommendation = recommendation(recommendationId, RecommendationActionType.RESTART_SERVICE, true);
        when(recommendationRepository.findById(recommendationId)).thenReturn(Optional.of(recommendation));
        when(approvalRepository.findAll()).thenReturn(List.of());

        ApprovalResponse response = service.request(recommendationId, "operator-a", "needs approval");

        assertThat(response.recommendationId()).isEqualTo(recommendationId);
        assertThat(response.approvalStatus()).isEqualTo(ApprovalStatus.REQUESTED.name());
        verify(approvalRepository).saveAndFlush(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void blocksSelfApproval() {
        ApprovalRepository approvalRepository = mock(ApprovalRepository.class);
        RecommendationRepository recommendationRepository = mock(RecommendationRepository.class);
        AuditService auditService = mock(AuditService.class);
        ApprovalService service = new ApprovalService(
                approvalRepository,
                recommendationRepository,
                auditService,
                new SelfObservabilityMetrics(new io.micrometer.core.instrument.simple.SimpleMeterRegistry()));

        UUID recommendationId = UUID.randomUUID();
        ApprovalEntity approval = new ApprovalEntity();
        approval.setApprovalId(UUID.randomUUID());
        approval.setRecommendationId(recommendationId);
        approval.setRequestedBy("approver-a");
        approval.setApprovalStatus(ApprovalStatus.REQUESTED.name());
        when(approvalRepository.findAll()).thenReturn(List.of(approval));

        assertThatThrownBy(() -> service.approve(recommendationId, "approver-a", "approved"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Self-approval");
    }

    @Test
    void rejectsDuplicateApprovalDecisions() {
        ApprovalRepository approvalRepository = mock(ApprovalRepository.class);
        RecommendationRepository recommendationRepository = mock(RecommendationRepository.class);
        AuditService auditService = mock(AuditService.class);
        ApprovalService service = new ApprovalService(
                approvalRepository,
                recommendationRepository,
                auditService,
                new SelfObservabilityMetrics(new io.micrometer.core.instrument.simple.SimpleMeterRegistry()));

        UUID recommendationId = UUID.randomUUID();
        RecommendationEntity recommendation = recommendation(recommendationId, RecommendationActionType.ROLLBACK_DEPLOYMENT, true);
        ApprovalEntity approval = new ApprovalEntity();
        approval.setApprovalId(UUID.randomUUID());
        approval.setRecommendationId(recommendationId);
        approval.setRequestedBy("operator-a");
        approval.setApprovalStatus(ApprovalStatus.APPROVED.name());
        when(recommendationRepository.findById(recommendationId)).thenReturn(Optional.of(recommendation));
        when(approvalRepository.findAll()).thenReturn(List.of(approval));

        assertThatThrownBy(() -> service.request(recommendationId, "operator-b", "again"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Duplicate or replayed approval");
    }

    private RecommendationEntity recommendation(UUID id, RecommendationActionType actionType, boolean requiresApproval) {
        RecommendationEntity recommendation = new RecommendationEntity();
        recommendation.setRecommendationId(id);
        recommendation.setIncidentId(UUID.randomUUID());
        recommendation.setActionType(actionType.name());
        recommendation.setDescription("desc");
        recommendation.setRiskLevel("HIGH");
        recommendation.setRequiresApproval(requiresApproval);
        recommendation.setStatus("PENDING");
        recommendation.setEvidenceSummary("evidence");
        recommendation.setCreatedAt(Instant.now());
        return recommendation;
    }
}
