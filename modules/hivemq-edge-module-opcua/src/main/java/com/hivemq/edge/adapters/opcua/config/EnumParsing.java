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

/**
 * Shared parsing for the security-relevant config enums: the certificate-validation preset, the six
 * validation axes, and the message security mode.
 *
 * <p>Matching is case-insensitive and tolerates missing underscores ({@code allowlist} ==
 * {@code ALLOW_LIST}), mirroring the leniency the OPC UA adapter config has always had. Blank or
 * absent input yields {@code null}, meaning "unset": the setting resolves to its default at read time.
 *
 * <p><b>An unrecognized non-blank value is a configuration error and is rejected.</b> The alternative
 * — reporting it and treating the setting as unset — was tried and withdrawn: unset does not resolve
 * to the strictest value everywhere. The preset door defaults to {@code STANDARD}, which checks
 * neither hostname nor key usage, so a typo in {@code tlsChecks=ALL} would have silently run the
 * adapter under <em>weaker</em> validation than the operator asked for. {@code messageSecurityMode} is
 * the same trap one block over: unset means "let the security policy decide", so {@code SING} under
 * {@code policy=NONE} would have connected with message security {@code None} where the correctly
 * spelled {@code SIGN} matches no endpoint at all — a typo turning a refused connection into an
 * unsigned one. A misspelled security setting must stop the adapter, not adjust it.
 *
 * <p>Throwing here fails the conversion of this adapter's configuration. That is contained:
 * {@code ProtocolAdapterManager} converts each adapter's configuration in isolation, so the rejection
 * names this adapter, leaves a running instance unchanged, and every other adapter refreshes as usual
 * — which is also what an invalid enum value did before these creators existed, minus the isolation
 * and the actionable message.
 */
final class EnumParsing {

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
        throw new IllegalArgumentException(
                ("OPC UA adapter configuration: '%s' is not a valid %s value. Permitted values: %s "
                                + "(case-insensitive, underscores optional). The adapter configuration has been "
                                + "rejected rather than started with weaker security than was written; correct the "
                                + "value and reload.")
                        .formatted(
                                trimmed,
                                type.getSimpleName(),
                                Arrays.stream(values).map(Enum::name).collect(Collectors.joining(", "))));
    }
}
