package com.hivemq.configuration.entity;

import com.hivemq.extension.sdk.api.annotations.NotNull;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * @author Christoph Schäbel
 */
@XmlRootElement(name = "option")
@XmlAccessorType(XmlAccessType.NONE)
@SuppressWarnings({"FieldMayBeFinal", "FieldCanBeLocal"})
public class OptionEntity {

    @XmlElement(name = "key", required = true)
    private @NotNull String key = "";

    @XmlElement(name = "value", required = true)
    private @NotNull String value = "";

    public @NotNull String getKey() {
        return key;
    }

    public @NotNull String getValue() {
        return value;
    }
}
