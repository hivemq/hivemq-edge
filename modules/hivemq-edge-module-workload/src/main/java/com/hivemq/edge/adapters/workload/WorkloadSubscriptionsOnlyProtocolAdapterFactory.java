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
package com.hivemq.edge.adapters.workload;

import com.hivemq.adapter.sdk.api.schema.ScalarSchema;
import com.hivemq.adapter.sdk.api.schema.ScalarType;
import com.hivemq.adapter.sdk.api.schema.Schema;
import com.hivemq.adapter.sdk.api.v2.ProtocolAdapterInformation;
import com.hivemq.adapter.sdk.api.v2.factories.ProtocolAdapterFactory;
import com.hivemq.adapter.sdk.api.v2.model.ProtocolAdapterInput;
import com.hivemq.adapter.sdk.api.v2.model.ProtocolAdapterOutput;
import org.jetbrains.annotations.NotNull;

/** The module-loader factory for the capability-less {@code "workload-subonly"} type (SUBSCRIPTIONS only, no WRITE). */
public final class WorkloadSubscriptionsOnlyProtocolAdapterFactory implements ProtocolAdapterFactory {

    private static final @NotNull Schema STRING_SCHEMA =
            new ScalarSchema(ScalarType.STRING, null, null, null, null, false, true, false);

    private final @NotNull ProtocolAdapterInformation information = new WorkloadSubscriptionsOnlyProtocolAdapterInformation();

    @Override
    public @NotNull ProtocolAdapterInformation information() {
        return information;
    }

    @Override
    public @NotNull WorkloadProtocolAdapter createAdapter(
            final @NotNull ProtocolAdapterInput input, final @NotNull ProtocolAdapterOutput output) {
        return new WorkloadProtocolAdapter(
                input.adapterId(), output, WorkloadScenario.parseOrEmpty(input.adapterConfig().getTagValue()));
    }

    @Override
    public @NotNull Schema adapterConfigSchema() {
        return STRING_SCHEMA;
    }

    @Override
    public @NotNull Schema nodeDefinitionSchema() {
        return STRING_SCHEMA;
    }
}
