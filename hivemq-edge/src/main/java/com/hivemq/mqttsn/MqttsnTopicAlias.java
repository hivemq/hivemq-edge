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
package com.hivemq.mqttsn;

import com.hivemq.extension.sdk.api.annotations.NotNull;

import java.util.Objects;

/**
 * Represents either one of the 2 types of TopicAlias within the MQTTSN subsystem.
 * @author Simon L Johnson
 */
public class MqttsnTopicAlias {

    public enum TYPE {
        NORMAL(0x00),
        PREDEFINED(0x01),
        SHORT(0x10),
        FULL(0x11);

        int topicAliasTypeId;

        TYPE(int topicAliasTypeId){
            this.topicAliasTypeId = topicAliasTypeId;
        }

        public int getTopicAliasTypeId(){
            return topicAliasTypeId;
        }
    }

    private @NotNull String topicName;
    private int alias;
    private TYPE type;

    public MqttsnTopicAlias(@NotNull final String topicName, final int alias, @NotNull final TYPE type) {
        this.topicName = topicName;
        this.alias = alias;
        this.type = type;
    }

    public MqttsnTopicAlias(@NotNull final String topicName, @NotNull final TYPE type) {
        this.topicName = topicName;
        this.type = type;
    }

    public int getAlias() {
        return alias;
    }

    public String getTopicName() {
        return topicName;
    }

    public TYPE getType() {
        return type;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MqttsnTopicAlias that = (MqttsnTopicAlias) o;
        return topicName.equals(that.topicName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(topicName);
    }

    @Override
    public String toString() {
        return "MqttsnTopicAlias{" +
                "topicName='" + topicName + '\'' +
                ", alias=" + alias +
                ", type=" + type +
                '}';
    }
}
