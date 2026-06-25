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
package com.hivemq.edge.adapters.chaos;

import com.hivemq.adapter.sdk.api.schema.ScalarSchema;
import com.hivemq.adapter.sdk.api.schema.ScalarType;
import com.hivemq.adapter.sdk.api.schema.Schema;
import com.hivemq.adapter.sdk.api.v2.ProtocolAdapterCapability;
import com.hivemq.adapter.sdk.api.v2.ProtocolAdapterInformation;
import com.hivemq.adapter.sdk.api.v2.factories.ProtocolAdapterFactory;
import com.hivemq.adapter.sdk.api.v2.model.ProtocolAdapterInput;
import com.hivemq.adapter.sdk.api.v2.model.ProtocolAdapterOutput;
import java.util.EnumSet;
import java.util.function.Function;
import org.jetbrains.annotations.NotNull;

/**
 * The factory for the {@link ChaosProtocolAdapter} type. Tests add it to the constructor-injected
 * factory set of the {@code ProtocolAdapterFactoryRegistry} (the production set is empty, D8); the wired end-to-end
 * suite (a later task) drives the simulator through the full manager → wrapper → adapter stack.
 * <p>
 * Because the simulator is scripted per command, the factory resolves the {@link ChaosScript} for each instance
 * from its {@link ProtocolAdapterInput} through a supplied function — so the end-to-end suite can hand each
 * configured chaos adapter its own script. {@link #withScript(ChaosScript)} builds a factory that hands the same
 * script to every instance, with the default {@code "chaos"} protocol id and all three capabilities.
 */
public final class ChaosProtocolAdapterFactory implements ProtocolAdapterFactory {

    /**
     * The default {@code protocol-id} the simulator registers under.
     */
    public static final @NotNull String DEFAULT_PROTOCOL_ID = "chaos";

    private static final @NotNull Schema STRING_SCHEMA =
            new ScalarSchema(ScalarType.STRING, null, null, null, null, false, true, false);

    private final @NotNull ProtocolAdapterInformation information;
    private final @NotNull Function<ProtocolAdapterInput, ChaosScript> scriptResolver;

    /**
     * @param information    the type identity and capabilities.
     * @param scriptResolver resolves the script for each created instance from its input.
     */
    public ChaosProtocolAdapterFactory(
            final @NotNull ProtocolAdapterInformation information,
            final @NotNull Function<ProtocolAdapterInput, ChaosScript> scriptResolver) {
        this.information = information;
        this.scriptResolver = scriptResolver;
    }

    /**
     * @param script the script every created instance is given.
     * @return a factory with the default {@code "chaos"} protocol id and all three capabilities that hands the
     *         same script to every instance.
     */
    public static @NotNull ChaosProtocolAdapterFactory withScript(final @NotNull ChaosScript script) {
        return new ChaosProtocolAdapterFactory(
                new ChaosProtocolAdapterInformation(
                        DEFAULT_PROTOCOL_ID, EnumSet.allOf(ProtocolAdapterCapability.class)),
                input -> script);
    }

    @Override
    public @NotNull ProtocolAdapterInformation information() {
        return information;
    }

    @Override
    public @NotNull ChaosProtocolAdapter createAdapter(
            final @NotNull ProtocolAdapterInput input, final @NotNull ProtocolAdapterOutput output) {
        return new ChaosProtocolAdapter(input.adapterId(), output, scriptResolver.apply(input));
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
