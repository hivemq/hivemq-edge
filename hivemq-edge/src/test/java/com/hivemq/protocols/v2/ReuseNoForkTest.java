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
package com.hivemq.protocols.v2;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/**
 * The reuse boundary (decision D2) for the framework side: {@code protocols.v2} depends on the reused v1 SDK
 * types and the new {@code api.v2} contracts — it never forks them into a parallel copy. Naming rule N2 governs
 * every new {@code protocols.v2} identifier; the package name carries the version, so no identifier takes a
 * {@code 2} suffix.
 */
class ReuseNoForkTest {

    private static final Path PROTOCOLS_V2_SOURCE_DIRECTORY =
            Path.of("src", "main", "java", "com", "hivemq", "protocols", "v2");

    /**
     * Simple names of reused v1 SDK types and of the {@code api.v2} contracts — a same-named type under
     * {@code protocols.v2} would be a fork.
     */
    private static final Set<String> NO_FORK_TYPE_SIMPLE_NAMES = Set.of(
            // reused v1 SDK subset
            "DataPoint",
            "DataPointBuilder",
            "DataPointFactory",
            "Schema",
            "ScalarSchema",
            "ObjectSchema",
            "ArraySchema",
            "AnySchema",
            "ScalarType",
            "SchemaBuilder",
            "SchemaJsonRepresentation",
            "NodeType",
            "ProtocolAdapterCategory",
            "ProtocolAdapterTag",
            "ProtocolSpecificAdapterConfig",
            // api.v2 contracts (SDK v2)
            "MailboxMessage",
            "MailboxMessagePriority",
            "MailboxSender",
            "Mailbox",
            "DefaultMailbox",
            "MessageHandler",
            "MessageDispatcher",
            "MessageDispatcherHandle",
            "Node",
            "Tag",
            "NodeTagPair",
            "NodeProperty",
            "AccessTriState",
            "AccessFlags",
            "WriteEntry",
            "BrowseFilter",
            "BrowseResultEntry",
            "ErrorScope",
            "VerifyOutcome",
            "ProtocolAdapter",
            "ProtocolAdapterOutput",
            "ProtocolAdapterCapability",
            "ProtocolAdapterInformation",
            "ProtocolAdapterFactory",
            "ProtocolAdapterInput",
            "ProtocolAdapterService");

    /**
     * Architectural shorthands that must never appear in code identifiers (naming rule N2).
     */
    private static final Set<String> FORBIDDEN_ABBREVIATIONS =
            Set.of("Paw", "Pam", "Paf", "Fsm", "Ctx", "Cfg", "Ack", "Mgr");

    @Test
    void protocolsV2_doesNotForkAnyReusedType() throws IOException {
        for (final String typeName : protocolsV2TopLevelTypeNames()) {
            assertThat(NO_FORK_TYPE_SIMPLE_NAMES)
                    .as("protocols.v2 type %s must not fork a reused type", typeName)
                    .doesNotContain(typeName);
        }
    }

    @Test
    void namingRuleN2_noAbbreviationsInTypeNames() throws IOException {
        for (final String typeName : protocolsV2TopLevelTypeNames()) {
            for (final String abbreviation : FORBIDDEN_ABBREVIATIONS) {
                assertThat(typeName)
                        .as(
                                "identifier %s must not contain the abbreviation '%s' (naming rule N2)",
                                typeName, abbreviation)
                        .doesNotContain(abbreviation);
            }
        }
    }

    private static List<String> protocolsV2TopLevelTypeNames() throws IOException {
        assertThat(PROTOCOLS_V2_SOURCE_DIRECTORY)
                .as("the test must run with the project directory as its working directory")
                .exists();
        try (final Stream<Path> paths = Files.walk(PROTOCOLS_V2_SOURCE_DIRECTORY)) {
            return paths.filter(path -> path.getFileName().toString().endsWith(".java"))
                    .map(path -> path.getFileName().toString().replace(".java", ""))
                    .filter(typeName -> !typeName.equals("package-info"))
                    .toList();
        }
    }
}
