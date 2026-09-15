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

import static com.hivemq.protocols.ProtocolAdapterUtils.createProtocolAdapterMapper;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Every enum on the adapter's configuration surface, swept for one property: an unrecognized value is
 * refused, never resolved to a default.
 *
 * <p>This is a class-of-defect guard rather than a behaviour test. {@code messageSecurityMode} was
 * the one enum whose creator returned {@code null} for a misspelling, which {@link Security} then read
 * as "unset" and resolved through the security policy — so {@code SING} under {@code policy=NONE}
 * connected with message security {@code None}. The individual deserializers are pinned in
 * {@link SecurityDeserializerTest} and {@link TlsChecksParsingTest}; this test exists so that the
 * <em>next</em> enum added to this package cannot reintroduce the same hole unnoticed.
 *
 * <p>The set is discovered by scanning the compiled package, deliberately not listed here: a list
 * would have to be maintained by whoever adds the enum, which is exactly the person the guard is for.
 */
class ConfigEnumValueParsingTest {

    private static final @NotNull ObjectMapper MAPPER = createProtocolAdapterMapper(new ObjectMapper());

    /** Not a value of anything in this package, and not a prefix or suffix of one either. */
    private static final @NotNull String NOT_A_VALUE = "NOT_A_CONFIGURED_VALUE";

    @ParameterizedTest
    @MethodSource("configEnums")
    void anUnrecognizedValueIsRefused(final @NotNull Class<?> type) {
        assertThatThrownBy(() -> MAPPER.convertValue(NOT_A_VALUE, type))
                .as(
                        "%s must refuse an unrecognized value: resolving it to a default silently runs the "
                                + "adapter under settings the operator did not write",
                        type.getSimpleName())
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void theSweepActuallyFoundTheConfigurationEnums() {
        // Without this the parameterized test above passes vacuously if the scan ever stops finding
        // anything - the failure mode of every reflective sweep.
        assertThat(configEnums())
                .as("the configuration package holds the validation preset, the six axes, the security "
                        + "policy and the message security mode")
                .hasSizeGreaterThanOrEqualTo(9);
    }

    static @NotNull List<Class<?>> configEnums() {
        final URL marker = MsgSecurityMode.class.getResource("MsgSecurityMode.class");
        assertThat(marker)
                .as("the compiled configuration package must be locatable")
                .isNotNull();
        assertThat(marker.getProtocol())
                .as("the sweep reads the module's own classes directory")
                .isEqualTo("file");

        final Path packageDir = packageDirOf(marker);
        try (Stream<Path> classFiles = Files.list(packageDir)) {
            return classFiles
                    .map(path -> path.getFileName().toString())
                    .filter(name -> name.endsWith(".class"))
                    .<Class<?>>map(name -> loadClass(name.substring(0, name.length() - ".class".length())))
                    .filter(Class::isEnum)
                    .toList();
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static @NotNull Path packageDirOf(final @NotNull URL marker) {
        try {
            return Path.of(marker.toURI()).getParent();
        } catch (final URISyntaxException e) {
            throw new IllegalStateException(e);
        }
    }

    private static @NotNull Class<?> loadClass(final @NotNull String simpleName) {
        try {
            return Class.forName(MsgSecurityMode.class.getPackageName() + "." + simpleName);
        } catch (final ClassNotFoundException e) {
            throw new IllegalStateException(e);
        }
    }
}
