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
package com.hivemq.configuration.entity.adapter;

import com.hivemq.configuration.entity.EntityConverter;
import com.hivemq.mqtt.message.QoS;
import org.jetbrains.annotations.NotNull;

public final class QoSConverter implements EntityConverter<com.hivemq.edge.api.model.QoS, QoS> {
    public static final QoSConverter INSTANCE = new QoSConverter();

    private QoSConverter() {}

    @Override
    public @NotNull QoS toInternalEntity(final @NotNull com.hivemq.edge.api.model.QoS qoS) {
        return switch (qoS) {
            case AT_MOST_ONCE -> QoS.AT_MOST_ONCE;
            case AT_LEAST_ONCE -> QoS.AT_LEAST_ONCE;
            case EXACTLY_ONCE -> QoS.EXACTLY_ONCE;
        };
    }

    @Override
    public @NotNull com.hivemq.edge.api.model.QoS toRestEntity(final @NotNull QoS qoS) {
        return switch (qoS) {
            case AT_MOST_ONCE -> com.hivemq.edge.api.model.QoS.AT_MOST_ONCE;
            case AT_LEAST_ONCE -> com.hivemq.edge.api.model.QoS.AT_LEAST_ONCE;
            case EXACTLY_ONCE -> com.hivemq.edge.api.model.QoS.EXACTLY_ONCE;
        };
    }

    public @NotNull com.hivemq.edge.api.model.QoS toRestEntity(final int intQoS) {
        final QoS qoS = QoS.valueOf(intQoS);
        return toRestEntity(qoS == null ? QoS.AT_MOST_ONCE : qoS);
    }
}
