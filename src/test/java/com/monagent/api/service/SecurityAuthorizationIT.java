package com.monagent.api.service;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.monagent.approval.ApprovalPolicy;
import com.monagent.approval.ApprovalService;
import com.monagent.approval.ApprovedActionExecutor;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import static org.mockito.Mockito.when;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityAuthorizationIT {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private IncidentQueryService incidentQueryService;

    @MockBean
    private ApprovalService approvalService;

    @MockBean
    private ApprovalPolicy approvalPolicy;

    @MockBean
    private ApprovedActionExecutor approvedActionExecutor;

    @Test
    void allowsViewerAccessToIncidentApis() throws Exception {
        when(incidentQueryService.list(null, null, null, null, "detectedAt", "desc")).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/incidents").with(httpBasic("viewer", "viewer")))
                .andExpect(status().isOk());
    }

    @Test
    void rejectsAnonymousAccessToProtectedApi() throws Exception {
        mockMvc.perform(get("/api/v1/incidents"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rejectsViewerAccessToApprovalApis() throws Exception {
        mockMvc.perform(get("/api/v1/approvals").with(httpBasic("viewer", "viewer")))
                .andExpect(status().isForbidden());
    }

    @Test
    void allowsOperatorAccessToApprovalApis() throws Exception {
        when(approvalService.list()).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/approvals").with(httpBasic("operator", "operator")))
                .andExpect(status().isOk());
    }
}
