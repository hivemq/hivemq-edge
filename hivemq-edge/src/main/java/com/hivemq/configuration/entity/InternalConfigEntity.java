package com.hivemq.configuration.entity;

import com.google.common.collect.Lists;
import com.hivemq.extension.sdk.api.annotations.NotNull;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.List;

/**
 * @author Christoph Schäbel
 */
@XmlRootElement(name = "internal")
@XmlAccessorType(XmlAccessType.NONE)
@SuppressWarnings({"FieldMayBeFinal", "FieldCanBeLocal"})
public class InternalConfigEntity {

    @XmlElements({@XmlElement(name = "option", type = OptionEntity.class)})
    private @NotNull List<OptionEntity> options = Lists.newArrayList();

    public @NotNull List<OptionEntity> getOptions() {
        return options;
    }
}
