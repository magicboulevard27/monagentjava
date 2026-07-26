package com.monagent.api.service;

import com.monagent.audit.AuditEvent;
import com.monagent.audit.AuditGovernanceService;
import java.time.Instant;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/audit")
public class AuditController {

    private final AuditGovernanceService auditGovernanceService;

    public AuditController(AuditGovernanceService auditGovernanceService) {
        this.auditGovernanceService = auditGovernanceService;
    }

    @GetMapping("/export")
    public List<AuditEvent> export(@RequestParam(defaultValue = "1970-01-01T00:00:00Z") Instant since) {
        return auditGovernanceService.exportSince(since);
    }

    @GetMapping("/integrity")
    public AuditIntegrityResponse integrity() {
        return new AuditIntegrityResponse(auditGovernanceService.verifyIntegrity(), auditGovernanceService.retentionCutoff());
    }

    public record AuditIntegrityResponse(boolean integrityValid, Instant retentionCutoff) {
    }
}
