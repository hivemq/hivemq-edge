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

import com.hivemq.adapter.sdk.api.schema.AnySchema;
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
 * The factory for the {@link ChaosProtocolAdapter} type. It comes in two forms for the two ways the simulator is
 * driven:
 * <ul>
 * <li>The <b>no-argument</b> constructor is the form the module loader instantiates when a build bundles the chaos
 * module: it advertises the default {@code "chaos"} protocol id and all three capabilities, and resolves each
 * instance's {@link ChaosScript} from {@link ChaosControl} by its {@code adapter-id} — so a test that boots a real
 * Edge runtime registers a script per adapter there before start. It also captures each instance's node references
 * for node-keyed event injection.</li>
 * <li>The <b>explicit</b> constructor (and {@link #withScript(ChaosScript)}) resolves the script through a supplied
 * function, for the deterministic harness that constructs the factory directly.</li>
 * </ul>
 */
public final class ChaosProtocolAdapterFactory implements ProtocolAdapterFactory {

    /**
     * The default {@code protocol-id} the simulator registers under.
     */
    public static final @NotNull String DEFAULT_PROTOCOL_ID = "chaos";

    private static final @NotNull Schema STRING_SCHEMA =
            new ScalarSchema(ScalarType.STRING, null, null, null, null, false, true, false);

    // The chaos adapter's behavior is driven by its script, not its configuration; it accepts any configuration.
    private static final @NotNull Schema ANY_SCHEMA = new AnySchema(null, null, false, true, false);

    private final @NotNull ProtocolAdapterInformation information;
    private final @NotNull Function<ProtocolAdapterInput, ChaosScript> scriptResolver;

    /**
     * The form the module loader instantiates: the default {@code "chaos"} protocol id and all three capabilities,
     * each instance's script resolved from {@link ChaosControl} by {@code adapter-id} and its node references captured
     * there for node-keyed injection.
     */
    public ChaosProtocolAdapterFactory() {
        this(
                new ChaosProtocolAdapterInformation(
                        DEFAULT_PROTOCOL_ID, EnumSet.allOf(ProtocolAdapterCapability.class)),
                input -> {
                    ChaosControl.captureNodes(input);
                    return ChaosControl.scriptFor(input.adapterId());
                });
    }

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
        return ANY_SCHEMA;
    }

    @Override
    public @NotNull Schema nodeDefinitionSchema() {
        return STRING_SCHEMA;
    }
}
