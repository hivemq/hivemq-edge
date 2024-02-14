package com.hivemq.uns.config.impl;

import com.google.common.base.Preconditions;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.uns.config.NamespaceProfile;
import com.hivemq.uns.config.NamespaceSegment;

import java.util.List;
import java.util.stream.Collectors;

public class NamespaceProfileImpl implements NamespaceProfile {

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
        segments = impl.getSegments().stream().map(NamespaceSegment::of).
                collect(Collectors.toList());
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(final boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    @Override
    public String getDescription() {
        return description;
    }

    public void setDescription(final String description) {
        this.description = description;
    }

    @Override
    public List<NamespaceSegment> getSegments() {
        return segments;
    }
}
