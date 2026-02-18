
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
package com.hivemq.configuration.entity.combining;

import com.hivemq.combining.model.DataIdentifierReference;
import com.hivemq.configuration.entity.EntityConverter;
import org.jetbrains.annotations.NotNull;

public final class DataIdentifierReferenceTypeConverter
        implements EntityConverter<
        com.hivemq.edge.api.model.DataIdentifierReference.TypeEnum, DataIdentifierReference.Type> {
    public static final DataIdentifierReferenceTypeConverter INSTANCE = new DataIdentifierReferenceTypeConverter();

    private DataIdentifierReferenceTypeConverter() {}

    @Override
    public @NotNull DataIdentifierReference.Type toInternalEntity(
            final @NotNull com.hivemq.edge.api.model.DataIdentifierReference.TypeEnum entity) {
        return switch (entity) {
            case PULSE_ASSET -> DataIdentifierReference.Type.PULSE_ASSET;
            case TAG -> DataIdentifierReference.Type.TAG;
            case TOPIC_FILTER -> DataIdentifierReference.Type.TOPIC_FILTER;
        };
    }

    @Override
    public @NotNull com.hivemq.edge.api.model.DataIdentifierReference.TypeEnum toRestEntity(
            final @NotNull DataIdentifierReference.Type entity) {
        return switch (entity) {
            case PULSE_ASSET -> com.hivemq.edge.api.model.DataIdentifierReference.TypeEnum.PULSE_ASSET;
            case TAG -> com.hivemq.edge.api.model.DataIdentifierReference.TypeEnum.TAG;
            case TOPIC_FILTER -> com.hivemq.edge.api.model.DataIdentifierReference.TypeEnum.TOPIC_FILTER;
        };
    }
}
