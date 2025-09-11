package com.hivemq.api.model.components;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static java.util.Objects.requireNonNullElse;

public class ConfidentialityAgreement {

    @JsonProperty("enabled")
    @Schema(description = "Whether the confidentiality agreement should be shown prior to login in")
    private final @NotNull Boolean enabled;

    @JsonProperty("content")
    @Schema(description = "The confidentiality agreement")
    private final @Nullable String content;

    public ConfidentialityAgreement() {
        this(false, null);
    }

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public ConfidentialityAgreement(final @Nullable Boolean enabled, final @Nullable String content) {
        this.enabled = requireNonNullElse(enabled, false);
        if (this.enabled && (content == null || content.isEmpty())) {
            throw new IllegalArgumentException("Content cannot be null or empty when enabled");
        }
        this.content = content;
    }

    public boolean getEnabled() {
        return enabled;
    }

    public @Nullable String getContent() {
        return content;
    }
}
