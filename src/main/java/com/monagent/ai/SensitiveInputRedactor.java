package com.monagent.ai;

import org.springframework.stereotype.Component;

@Component
public class SensitiveInputRedactor {

    private final com.monagent.security.RedactionService redactionService;

    public SensitiveInputRedactor(com.monagent.security.RedactionService redactionService) {
        this.redactionService = redactionService;
    }

    public String redact(String value) {
        return redactionService.redact(value);
    }
}
