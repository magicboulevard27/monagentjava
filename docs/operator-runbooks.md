# Operator Runbooks

## Source Failures

- Check the source endpoint and credentials.
- Verify timeouts, retries, and circuit breakers.
- Confirm the collector continues with partial results.

## LLM Failures

- Check the Ollama endpoint and model name.
- Verify memory and CPU headroom.
- Expect deterministic fallback analysis when the model is unavailable.

## Database Failures

- Check connectivity, credentials, and migrations first.
- Restore the database before restarting the application if data is missing or corrupt.
- Verify `/actuator/health` and a representative API call after recovery.

## Notification Failures

- Check downstream channel credentials and rate limits.
- Confirm the approval and notification paths are still accepting work.
- Re-run the alert or notification flow after the channel recovers.
