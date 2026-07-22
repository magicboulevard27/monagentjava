package com.monagent.api.service;

import com.monagent.approval.ApprovalDecisionRequest;
import com.monagent.approval.ApprovalPolicy;
import com.monagent.approval.ApprovalResponse;
import com.monagent.approval.ApprovalRole;
import com.monagent.approval.ApprovalService;
import com.monagent.analysis.RecommendationActionType;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class ApprovalController {

    private final ApprovalService approvalService;
    private final ApprovalPolicy approvalPolicy;

    public ApprovalController(ApprovalService approvalService, ApprovalPolicy approvalPolicy) {
        this.approvalService = approvalService;
        this.approvalPolicy = approvalPolicy;
    }

    @PostMapping("/recommendations/{recommendationId}/approve")
    public ApprovalResponse approve(
            @PathVariable UUID recommendationId,
            @Valid @RequestBody ApprovalDecisionRequest request,
            @RequestHeader(name = "X-Actor-Role", defaultValue = "APPROVER") ApprovalRole role) {
        if (!approvalPolicy.canDecide(role)) {
            throw new IllegalArgumentException("Actor is not authorized to approve recommendations");
        }
        return approvalService.approve(recommendationId, request.actor(), request.reason());
    }

    @PostMapping("/recommendations/{recommendationId}/reject")
    public ApprovalResponse reject(
            @PathVariable UUID recommendationId,
            @Valid @RequestBody ApprovalDecisionRequest request,
            @RequestHeader(name = "X-Actor-Role", defaultValue = "APPROVER") ApprovalRole role) {
        if (!approvalPolicy.canDecide(role)) {
            throw new IllegalArgumentException("Actor is not authorized to reject recommendations");
        }
        return approvalService.reject(recommendationId, request.actor(), request.reason());
    }

    @GetMapping("/approvals")
    public List<ApprovalResponse> list() {
        return approvalService.list();
    }

    @PostMapping("/recommendations/{recommendationId}/request")
    public ResponseEntity<ApprovalResponse> request(
            @PathVariable UUID recommendationId,
            @Valid @RequestBody ApprovalDecisionRequest request,
            @RequestHeader(name = "X-Actor-Role", defaultValue = "OPERATOR") ApprovalRole role) {
        if (!approvalPolicy.canRequest(role, RecommendationActionType.RESTART_SERVICE)) {
            throw new IllegalArgumentException("Actor is not authorized to request approvals");
        }
        return ResponseEntity.ok(approvalService.request(recommendationId, request.actor(), request.reason()));
    }
}
