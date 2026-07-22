CREATE TABLE monitored_services (
    service_id UUID PRIMARY KEY,
    service_name VARCHAR(200) NOT NULL,
    environment VARCHAR(50) NOT NULL,
    owner_team VARCHAR(200) NOT NULL,
    health_url VARCHAR(500) NOT NULL,
    prometheus_job_name VARCHAR(200),
    log_index_pattern VARCHAR(200),
    trace_service_name VARCHAR(200),
    kubernetes_namespace VARCHAR(200),
    kubernetes_workload_name VARCHAR(200),
    alert_channels TEXT NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_monitored_services_environment CHECK (environment IN ('development', 'testing', 'staging', 'production'))
);

CREATE INDEX idx_monitored_services_environment ON monitored_services(environment);
CREATE INDEX idx_monitored_services_enabled ON monitored_services(enabled);

CREATE TABLE monitoring_signals (
    signal_id UUID PRIMARY KEY,
    service_id UUID NOT NULL REFERENCES monitored_services(service_id) ON DELETE CASCADE,
    source_type VARCHAR(50) NOT NULL,
    signal_name VARCHAR(200) NOT NULL,
    signal_value VARCHAR(500) NOT NULL,
    unit VARCHAR(50),
    status VARCHAR(50),
    severity VARCHAR(50),
    collected_at TIMESTAMPTZ NOT NULL,
    raw_reference JSONB
);

CREATE INDEX idx_monitoring_signals_service_id ON monitoring_signals(service_id);
CREATE INDEX idx_monitoring_signals_collected_at ON monitoring_signals(collected_at);
CREATE INDEX idx_monitoring_signals_source_type ON monitoring_signals(source_type);

CREATE TABLE anomaly_outcomes (
    anomaly_id UUID PRIMARY KEY,
    service_id UUID NOT NULL REFERENCES monitored_services(service_id) ON DELETE CASCADE,
    signal_id UUID REFERENCES monitoring_signals(signal_id) ON DELETE SET NULL,
    metric_name VARCHAR(200) NOT NULL,
    threshold_value NUMERIC(19, 4),
    observed_value NUMERIC(19, 4),
    comparator VARCHAR(20) NOT NULL,
    severity VARCHAR(50) NOT NULL,
    outcome_status VARCHAR(50) NOT NULL,
    evaluation_window_minutes INTEGER NOT NULL,
    minimum_sample_size INTEGER NOT NULL,
    detected_at TIMESTAMPTZ NOT NULL,
    cooldown_until TIMESTAMPTZ,
    supporting_references JSONB
);

CREATE INDEX idx_anomaly_outcomes_service_id ON anomaly_outcomes(service_id);
CREATE INDEX idx_anomaly_outcomes_metric_name ON anomaly_outcomes(metric_name);
CREATE INDEX idx_anomaly_outcomes_detected_at ON anomaly_outcomes(detected_at);

CREATE TABLE incidents (
    incident_id UUID PRIMARY KEY,
    title VARCHAR(300) NOT NULL,
    severity VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL,
    affected_services JSONB NOT NULL,
    start_time TIMESTAMPTZ NOT NULL,
    detected_at TIMESTAMPTZ NOT NULL,
    resolved_at TIMESTAMPTZ,
    likely_root_cause TEXT,
    confidence VARCHAR(50),
    summary TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_incidents_severity ON incidents(severity);
CREATE INDEX idx_incidents_status ON incidents(status);
CREATE INDEX idx_incidents_detected_at ON incidents(detected_at);

CREATE TABLE incident_evidence (
    evidence_id UUID PRIMARY KEY,
    incident_id UUID NOT NULL REFERENCES incidents(incident_id) ON DELETE CASCADE,
    source_type VARCHAR(50) NOT NULL,
    service_name VARCHAR(200),
    evidence_type VARCHAR(100) NOT NULL,
    description TEXT NOT NULL,
    observed_at TIMESTAMPTZ NOT NULL,
    reference_id VARCHAR(200),
    redacted_payload JSONB
);

CREATE INDEX idx_incident_evidence_incident_id ON incident_evidence(incident_id);
CREATE INDEX idx_incident_evidence_observed_at ON incident_evidence(observed_at);

CREATE TABLE recommendations (
    recommendation_id UUID PRIMARY KEY,
    incident_id UUID NOT NULL REFERENCES incidents(incident_id) ON DELETE CASCADE,
    action_type VARCHAR(100) NOT NULL,
    description TEXT NOT NULL,
    risk_level VARCHAR(50) NOT NULL,
    requires_approval BOOLEAN NOT NULL DEFAULT FALSE,
    status VARCHAR(50) NOT NULL,
    evidence_summary TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_recommendations_incident_id ON recommendations(incident_id);
CREATE INDEX idx_recommendations_status ON recommendations(status);

CREATE TABLE approvals (
    approval_id UUID PRIMARY KEY,
    recommendation_id UUID NOT NULL REFERENCES recommendations(recommendation_id) ON DELETE CASCADE,
    requested_by VARCHAR(200) NOT NULL,
    approved_by VARCHAR(200),
    approval_status VARCHAR(50) NOT NULL,
    decision_reason TEXT,
    decided_at TIMESTAMPTZ,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX idx_approvals_recommendation_id ON approvals(recommendation_id);
CREATE INDEX idx_approvals_status ON approvals(approval_status);

CREATE TABLE audit_logs (
    audit_id UUID PRIMARY KEY,
    actor VARCHAR(200) NOT NULL,
    action VARCHAR(200) NOT NULL,
    entity_type VARCHAR(100) NOT NULL,
    entity_id UUID NOT NULL,
    event_payload JSONB,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_audit_logs_actor ON audit_logs(actor);
CREATE INDEX idx_audit_logs_created_at ON audit_logs(created_at);
