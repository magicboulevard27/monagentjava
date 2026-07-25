package com.monagent.security;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "monagent.security.crypto")
public record SecurityCryptoProperties(
        @NotBlank String encryptionKeyBase64) {
}
