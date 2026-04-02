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
package com.hivemq.edge.knappogue;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Topic resolution for the Knappogue compiled-config delivery channel.
 *
 * <p>Default topic pattern: {@value DEFAULT_PATTERN}
 *
 * <p>Two placeholders are supported everywhere a topic pattern is accepted:
 * <ul>
 *   <li>{@code {fleetId}} — replaced with the fleet identifier (e.g. {@code acme})</li>
 *   <li>{@code {edgeInstanceId}} — replaced with the Edge instance identifier (e.g. {@code wolery})</li>
 * </ul>
 *
 * <p>Both IDs default to {@code -} so the channel degrades gracefully in single-instance POC setups.
 *
 * <h2>Topic resolution — subscriber side (running Edge instance)</h2>
 * <ol>
 *   <li>{@code HIVEMQ_COMPILED_CONFIG_TOPIC} env var — full pattern override (placeholders replaced)</li>
 *   <li>Default: {@value DEFAULT_PATTERN} with placeholders replaced from env vars</li>
 * </ol>
 *
 * <h2>Topic resolution — push side ({@code edge --compile --push})</h2>
 * <ol>
 *   <li>{@code --topic} CLI option — pattern override (placeholders replaced)</li>
 *   <li>{@code HIVEMQ_COMPILED_CONFIG_TOPIC} env var — pattern override (placeholders replaced)</li>
 *   <li>Default: {@value DEFAULT_PATTERN} with placeholders replaced</li>
 * </ol>
 *
 * <p>In both cases {@code {fleetId}} is resolved from the {@code HIVEMQ_FLEET_ID} env var (or
 * {@code --fleet} CLI arg on the push side), and {@code {edgeInstanceId}} from
 * {@code HIVEMQ_EDGE_INSTANCE_ID} (or {@code --instance} CLI arg on the push side).
 */
public final class KnappogueTopic {

    /** Default MQTT topic pattern for the compiled-config delivery channel. */
    public static final @NotNull String DEFAULT_PATTERN = "HIVEMQ/{fleetId}/EDGE/{edgeInstanceId}/CONFIG/apply";

    /** Env var: overrides the full topic pattern on both subscriber and push sides. Placeholders are replaced. */
    public static final @NotNull String TOPIC_OVERRIDE_ENV_VAR = "HIVEMQ_COMPILED_CONFIG_TOPIC";

    /** Env var: fleet identifier (e.g. {@code acme}). Defaults to {@code -}. */
    public static final @NotNull String FLEET_ID_ENV_VAR = "HIVEMQ_FLEET_ID";

    /** Env var: Edge instance identifier (e.g. {@code wolery}). Defaults to {@code -}. */
    public static final @NotNull String INSTANCE_ID_ENV_VAR = "HIVEMQ_EDGE_INSTANCE_ID";

    private KnappogueTopic() {}

    /**
     * Resolves the subscribe topic for a running Edge instance (reads all env vars).
     *
     * @see KnappogueTopic class-level Javadoc for resolution order
     */
    public static @NotNull String resolveForSubscriber() {
        final String fleetId = envOr(FLEET_ID_ENV_VAR, "-");
        final String instanceId = envOr(INSTANCE_ID_ENV_VAR, "-");
        return resolve(fleetId, instanceId, null);
    }

    /**
     * Resolves the publish topic for a push operation.
     *
     * @param fleetId          fleet ID from {@code --fleet} CLI arg or {@value FLEET_ID_ENV_VAR} env var (default {@code -})
     * @param instanceId       Edge instance ID from {@code --instance} CLI arg or auto-detected (default {@code -})
     * @param cliTopicOverride value of {@code --topic} CLI option, or {@code null} if not provided
     * @see KnappogueTopic class-level Javadoc for resolution order
     */
    public static @NotNull String resolveForPush(
            final @NotNull String fleetId, final @NotNull String instanceId, final @Nullable String cliTopicOverride) {
        return resolve(fleetId, instanceId, cliTopicOverride);
    }

    /**
     * Replaces {@code {fleetId}} and {@code {edgeInstanceId}} placeholders in the given pattern.
     */
    public static @NotNull String replacePlaceholders(
            final @NotNull String pattern, final @NotNull String fleetId, final @NotNull String instanceId) {
        return pattern.replace("{fleetId}", fleetId).replace("{edgeInstanceId}", instanceId);
    }

    /** Returns the env var value, or {@code defaultValue} if it is absent or blank. */
    static @NotNull String envOr(final @NotNull String envVar, final @NotNull String defaultValue) {
        final String v = System.getenv(envVar);
        return (v != null && !v.isBlank()) ? v : defaultValue;
    }

    private static @NotNull String resolve(
            final @NotNull String fleetId, final @NotNull String instanceId, final @Nullable String cliOverride) {
        if (cliOverride != null && !cliOverride.isBlank()) {
            return replacePlaceholders(cliOverride, fleetId, instanceId);
        }
        final String envOverride = System.getenv(TOPIC_OVERRIDE_ENV_VAR);
        if (envOverride != null && !envOverride.isBlank()) {
            return replacePlaceholders(envOverride, fleetId, instanceId);
        }
        return replacePlaceholders(DEFAULT_PATTERN, fleetId, instanceId);
    }
}
