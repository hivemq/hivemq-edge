/*
 *  Copyright 2019-present HiveMQ GmbH
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.hivemq.configuration.entity.adapter;

import com.hivemq.adapter.sdk.api.config.MessageHandlingOptions;
import com.hivemq.configuration.entity.EntityConverter;
import com.hivemq.edge.api.model.NorthboundMappingOwner;
import org.jetbrains.annotations.NotNull;

public final class NorthboundMappingOwnerConverter
        implements EntityConverter<NorthboundMappingOwner, NorthboundMappingEntity> {
    public static final NorthboundMappingOwnerConverter INSTANCE = new NorthboundMappingOwnerConverter();

    private NorthboundMappingOwnerConverter() {
    }

    @Override
    public @NotNull NorthboundMappingEntity toInternalEntity(final @NotNull NorthboundMappingOwner entity) {
        return new NorthboundMappingEntity(entity.getTagName(),
                entity.getTopic(),
                QoSConverter.INSTANCE.toInternalEntity(entity.getMaxQoS()).getQosNumber(),
                MessageHandlingOptions.MQTTMessagePerTag,
                entity.getIncludeTagNames(),
                entity.getIncludeTimestamp(),
                MqttUserPropertyEntityConverter.INSTANCE.toInternalEntities(entity.getUserProperties()),
                entity.getMessageExpiryInterval());
    }

    public @NotNull NorthboundMappingOwner toRestEntity(
            final @NotNull NorthboundMappingEntity entity,
            final @NotNull String adapterId) {
        return toRestEntity(entity).adapterId(adapterId);
    }

    @Override
    public @NotNull NorthboundMappingOwner toRestEntity(final @NotNull NorthboundMappingEntity entity) {
        return NorthboundMappingOwner.builder()
                .tagName(entity.getTagName())
                .topic(entity.getTopic())
                .includeTagNames(entity.isIncludeTagNames())
                .includeTimestamp(entity.isIncludeTimestamp())
                .maxQoS(QoSConverter.INSTANCE.toRestEntity(entity.getMaxQoS()))
                .messageExpiryInterval(entity.getMessageExpiryInterval())
                .userProperties(MqttUserPropertyEntityConverter.INSTANCE.toRestEntities(entity.getUserProperties()))
                .build();
    }
}
