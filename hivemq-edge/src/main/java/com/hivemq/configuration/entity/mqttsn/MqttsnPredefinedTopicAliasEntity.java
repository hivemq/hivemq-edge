/*
 * Copyright 2019-present HiveMQ GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.hivemq.configuration.entity.mqttsn;

import com.hivemq.extension.sdk.api.annotations.NotNull;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * @author Simon L Johnson
 *
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name = "predefined-topic")
@SuppressWarnings({"FieldMayBeFinal", "FieldCanBeLocal"})
public class MqttsnPredefinedTopicAliasEntity {

    @XmlElement(name = "topicName", required = true)
    private @NotNull String topicName = "";

    @XmlElement(name = "alias", required = true)
    private int alias;

    public int getAlias() {
        return alias;
    }

    public String getTopicName() {
        return topicName;
    }
}
