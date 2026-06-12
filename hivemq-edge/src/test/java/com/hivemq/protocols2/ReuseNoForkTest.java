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
package com.hivemq.protocols2;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/**
 * The reuse boundary (decision D2) for the framework side: {@code protocols2} depends on the reused v1 SDK
 * types and the new {@code api2} contracts — it never forks them into a parallel copy. The naming rules
 * (N1/N2) govern every new {@code protocols2} identifier.
 */
class ReuseNoForkTest {

    private static final Path PROTOCOLS2_SOURCE_DIRECTORY =
            Path.of("src", "main", "java", "com", "hivemq", "protocols2");

    /**
     * Simple names of reused v1 SDK types and of the {@code api2} contracts — a same-named type under
     * {@code protocols2} would be a fork.
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
            // api2 contracts (SDK v2)
            "Message",
            "MessagePriority",
            "MailboxSender",
            "Mailbox",
            "DefaultMailbox",
            "Actor",
            "Dispatcher",
            "ActorHandle",
            "Node",
            "Tag2",
            "NodeTagPair",
            "NodeProperty",
            "AccessTriState",
            "AccessFlags",
            "WriteEntry",
            "BrowseFilter",
            "BrowseResultEntry",
            "ErrorScope",
            "VerifyOutcome",
            "ProtocolAdapter2",
            "ProtocolAdapterCallbacks",
            "ProtocolAdapterCapability2",
            "ProtocolAdapterInformation2",
            "ProtocolAdapterFactory2",
            "ProtocolAdapterInput2",
            "ProtocolAdapterServices2");

    /**
     * Architectural shorthands that must never appear in code identifiers (naming rule N2).
     */
    private static final Set<String> FORBIDDEN_ABBREVIATIONS =
            Set.of("Paw", "Pam", "Paf", "Fsm", "Ctx", "Cfg", "Ack", "Mgr");

    @Test
    void protocols2_doesNotForkAnyReusedType() throws IOException {
        for (final String typeName : protocols2TopLevelTypeNames()) {
            assertThat(NO_FORK_TYPE_SIMPLE_NAMES)
                    .as("protocols2 type %s must not fork a reused type", typeName)
                    .doesNotContain(typeName);
        }
    }

    @Test
    void namingRuleN1_twoIsASuffixNeverAnInfix() throws IOException {
        for (final String typeName : protocols2TopLevelTypeNames()) {
            if (typeName.indexOf('2') >= 0) {
                assertThat(typeName)
                        .as("identifier %s must carry '2' only as a suffix (naming rule N1)", typeName)
                        .matches("[A-Za-z]+2");
            }
        }
    }

    @Test
    void namingRuleN2_noAbbreviationsInTypeNames() throws IOException {
        for (final String typeName : protocols2TopLevelTypeNames()) {
            for (final String abbreviation : FORBIDDEN_ABBREVIATIONS) {
                assertThat(typeName)
                        .as(
                                "identifier %s must not contain the abbreviation '%s' (naming rule N2)",
                                typeName, abbreviation)
                        .doesNotContain(abbreviation);
            }
        }
    }

    private static List<String> protocols2TopLevelTypeNames() throws IOException {
        assertThat(PROTOCOLS2_SOURCE_DIRECTORY)
                .as("the test must run with the project directory as its working directory")
                .exists();
        try (final Stream<Path> paths = Files.walk(PROTOCOLS2_SOURCE_DIRECTORY)) {
            return paths.filter(path -> path.getFileName().toString().endsWith(".java"))
                    .map(path -> path.getFileName().toString().replace(".java", ""))
                    .filter(typeName -> !typeName.equals("package-info"))
                    .toList();
        }
    }
}
