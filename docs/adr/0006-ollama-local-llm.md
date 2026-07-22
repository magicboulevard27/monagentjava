# ADR-0006: Use Ollama with an approved local model

## Status

Accepted

## Context

The requirements and design allow a local LLM provider and emphasize redaction, constrained prompts, and deterministic handling when the model is unavailable.

## Decision

Use Ollama as the initial LLM provider and standardize on a local instruction-tuned model approved for the MVP.

## Approved Model

`llama3.1:8b-instruct`

## Consequences

- The first implementation can run without depending on a remote AI service.
- Prompt and output handling must assume local inference latency and occasional unavailability.
- Model selection may be revised later if quality, latency, or memory pressure warrants it.

