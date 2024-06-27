package com.hivemq.uns.config.impl;

import com.google.common.base.Preconditions;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.uns.config.NamespaceProfile;
import com.hivemq.uns.config.NamespaceSegment;

import javax.validation.constraints.Null;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class NamespaceProfileImpl implements NamespaceProfile {

    private @NotNull boolean prefixAllTopics;
    private @NotNull boolean enabled;
    private @NotNull String name;
    private @Nullable String description;
    private final List<NamespaceSegment> segments;

    public NamespaceProfileImpl(final @NotNull String name, final @Nullable String description, final List<NamespaceSegment> segments) {
        Preconditions.checkNotNull(name);
        this.name = name;
        this.description = description;
        this.segments = segments;
    }

    public NamespaceProfileImpl(NamespaceProfile impl) {
        setName(impl.getName());
        setDescription(impl.getDescription());
        setEnabled(impl.getEnabled());
        setPrefixAllTopics(impl.getPrefixAllTopics());
        segments = impl.getSegments().stream().map(NamespaceSegment::of).
                collect(Collectors.toList());
    }

    public @NotNull boolean getPrefixAllTopics() {
        return prefixAllTopics;
    }

    public void setPrefixAllTopics(final @NotNull boolean prefixAllTopics) {
        this.prefixAllTopics = prefixAllTopics;
    }

    public @NotNull boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(final @NotNull boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public @NotNull String getName() {
        return name;
    }

    public void setName(final @NotNull String name) {
        this.name = name;
    }

    @Override
    public @Nullable String getDescription() {
        return description;
    }

    public void setDescription(final @Nullable String description) {
        this.description = description;
    }

    @Override
    public @NotNull List<NamespaceSegment> getSegments() {
        return segments;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        NamespaceProfileImpl that = (NamespaceProfileImpl) o;
        return Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }
}
