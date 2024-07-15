package com.hivemq.uns.config;

import com.google.common.base.Preconditions;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "segment")
@XmlAccessorType(XmlAccessType.NONE)
public class NamespaceSegment {

    @XmlElement(name = "name")
    private @NotNull String name;
    @XmlElement(name = "value")
    private @NotNull String value;
    private @Nullable String description;

    public NamespaceSegment() {
    }

    public NamespaceSegment(final @NotNull String name, final @Nullable String value, @Nullable String description) {
        Preconditions.checkNotNull(name);
        this.name = name;
        this.value = value;
        this.description = description;
    }

    public String getName() {
        return name;
    }

    public void setName(final @NotNull String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(final @Nullable String description) {
        this.description = description;
    }

    public String getValue() {
        return value;
    }

    public void setValue(final String value) {
        this.value = value;
    }

    public static NamespaceSegment of(final String name, final String value, final String description){
        return new NamespaceSegment(name, value, description);
    }

    public static NamespaceSegment of(final String name, final String description){
        return new NamespaceSegment(name, null, description);
    }

    public static NamespaceSegment of(final NamespaceSegment segment){
        return new NamespaceSegment(segment.getName(), segment.getValue(), segment.getDescription());
    }
}
