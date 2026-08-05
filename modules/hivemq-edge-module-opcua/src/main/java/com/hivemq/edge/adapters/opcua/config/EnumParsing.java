/*
 * Copyright 2023-present HiveMQ GmbH
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
package com.hivemq.edge.adapters.opcua.config;

import java.util.Arrays;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Shared parsing for the certificate-validation config enums.
 *
 * <p>Matching is case-insensitive and tolerates missing underscores ({@code allowlist} ==
 * {@code ALLOW_LIST}), mirroring the leniency the OPC UA adapter config has always had.
 *
 * <p>An unrecognized value is reported at WARN and treated as if the setting had been left out. Two
 * alternatives were rejected:
 *
 * <ul>
 *   <li><b>Throwing.</b> Adapter configurations are converted inside a single stream over every
 *       adapter, and an exception there aborts the whole refresh — one typo in one adapter would
 *       silently stop every other adapter from being reconfigured. The blast radius is far worse than
 *       the problem.
 *   <li><b>Failing silently.</b> The operator would have no way to discover why the setting had no
 *       effect.
 * </ul>
 *
 * <p>Falling back to "unset" is safe in the direction that matters: every default in this model is the
 * strictest value, so a misspelling can only ever produce <em>more</em> validation than was asked for,
 * never less. The connection then fails visibly, next to a WARN naming the offending value.
 */
final class EnumParsing {

    private static final @NotNull Logger log = LoggerFactory.getLogger(EnumParsing.class);

    private EnumParsing() {}

    static <E extends Enum<E>> @Nullable E parse(
            final @NotNull Class<E> type, final E @NotNull [] values, final @Nullable String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        final String trimmed = value.trim();
        final String normalized = trimmed.replace("_", "");
        for (final E candidate : values) {
            if (candidate.name().equalsIgnoreCase(trimmed)
                    || candidate.name().replace("_", "").equalsIgnoreCase(normalized)) {
                return candidate;
            }
        }
        log.warn(
                "OPC UA adapter TLS configuration: '{}' is not a valid {} value and was ignored; the setting falls "
                        + "back to its default, which is the strictest available. Permitted values: {}.",
                trimmed,
                type.getSimpleName(),
                Arrays.stream(values).map(Enum::name).collect(Collectors.joining(", ")));
        return null;
    }
}
