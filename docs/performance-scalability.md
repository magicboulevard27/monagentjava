# Performance and Scalability Baseline

## Capacity Targets

- Expected monitored services in the initial deployment: 25 to 50.
- Expected signal volume per minute at that scale: 250 to 1,500 normalized signals.
- Expected incident volume per day: 0 to 20 incidents, with spikes during deployments.
- Retention baseline:
  - Monitoring signals: 30 days online, then archive or purge.
  - Incident evidence: 90 days online, then archive or purge.
  - Audit records: retain according to compliance policy, with a minimum of 1 year unless a stricter policy is adopted.

## Runtime Tuning

- Collector dispatch interval: 60 seconds by default.
- Health, logs, traces, Kubernetes, and Prometheus collectors each run on independent scheduled paths.
- Collector backlog is exposed as a metric so queue pressure can be observed before saturation.
- Signal persistence uses batched writes with Hibernate batch settings enabled.
- The async collector executor is bounded by worker-thread and queue-capacity configuration.

## Throughput Guidance

- Health checks should complete well under the 60-second scheduling interval.
- Alert and recommendation generation should normally remain under 60 seconds after the triggering signal is persisted.
- Incident summaries should remain under 2 minutes for the expected initial deployment size.
- Duplicate work should be prevented by enabled-service filtering and by keeping scheduled jobs idempotent.

## Scaling Triggers

- Increase worker threads and queue capacity when backlog grows faster than the collector interval.
- Add database capacity or pool tuning when save latency rises during peak collection windows.
- Split collectors into separate workers only if one source begins to dominate CPU, memory, or queue depth.
- Partition or archive signals and evidence when table growth starts impacting query latency.

## Verification Approach

- Use repeatable load tests against a representative service count and signal mix before production rollout.
- Track collection latency, backlog depth, persistence latency, and incident generation time during those tests.
- Revisit batching, connection pools, and executor sizing after the first production baseline is established.
