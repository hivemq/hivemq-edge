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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.hivemq.api.json.CustomConfigSchemaGenerator;
import com.hivemq.edge.adapters.opcua.OpcUaProtocolAdapterInformation;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;

/**
 * Keeps the frontend's copy of the certificate-validation surface honest by removing the copy.
 *
 * <p>{@code ChakraRJSForm.spec.cy.tsx} renders the OPC UA TLS surface through RJSF and asserts what it
 * submits, which is the only test that covers the P1 where {@code tls.ui:order} hid the whole section.
 * It used to be handed a 138-line TypeScript transcription of the two schemas, maintained by hand. Both
 * backend guards could stay green while that transcription drifted: {@link UiSchemaSurfaceTest} checks
 * the real UI schema against the real data schema, and {@code ConfigSchemaIT} pins the served data
 * schema, but nothing connected either to the frontend's copy. The component test would keep passing
 * against an old, self-consistent copy of a form the product no longer has.
 *
 * <p>So this test generates the artifact the frontend imports. The data-schema half comes from
 * {@link CustomConfigSchemaGenerator}, which is what the API serves; the UI half is the {@code tls}
 * block of the real {@code opcua-adapter-ui-schema.json}. Change a TLS property, a default, a
 * description or a {@code ui:order} and this test writes the new artifact and fails, exactly like a
 * stale committed OpenAPI spec fails {@code openApiSpecCopy} - commit what it wrote and the frontend
 * is rendering the current surface again.
 *
 * <p>Scoped to {@code tls} rather than the whole adapter config because that is the surface the
 * component test drives. {@link OpcUaSpecificAdapterConfig} is the northbound shape; the bidirectional
 * one shares this {@code tls} record, and {@link UiSchemaSurfaceTest} already covers both.
 */
class TlsSchemaArtifactTest {

    private static final @NotNull ObjectMapper MAPPER = new ObjectMapper();

    /** Relative to the repository root, inside the frontend so the fixture is an ordinary import. */
    private static final @NotNull String ARTIFACT =
            "hivemq-edge-frontend/src/__test-utils__/adapters/generated/opc-ua-tls.schema.json";

    @Test
    void theCommittedFrontendArtifactMatchesTheSchemasEdgeServes() throws IOException {
        final Path artifact = repositoryRoot().resolve(ARTIFACT);
        final ObjectNode generated = generateArtifact();
        final JsonNode committed = readCommitted(artifact);

        if (!generated.equals(committed)) {
            // Written rather than merely reported: the fix is mechanical, and making the developer
            // transcribe a JSON diff by hand is how the copy drifted in the first place.
            Files.createDirectories(artifact.getParent());
            Files.writeString(artifact, MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(generated) + "\n");
            fail(("%s was stale and has been rewritten from the current schemas. Review the change and commit it; "
                            + "the frontend's TLS form test renders whatever this file contains.")
                    .formatted(ARTIFACT));
        }

        // Cheap guard against the artifact being generated from something empty: a wrong slice path
        // would otherwise write {} and match a committed {} forever.
        assertThat(generated.at("/schema/properties/tls/properties").size())
                .as("the generated tls slice must carry the TLS properties")
                .isGreaterThan(0);
        assertThat(generated.at("/uiSchema/tls/ui:order").isArray())
                .as("the generated ui slice must carry the tls ui:order")
                .isTrue();
    }

    private static @NotNull ObjectNode generateArtifact() {
        final JsonNode dataSchema =
                new CustomConfigSchemaGenerator().generateJsonSchema(OpcUaSpecificAdapterConfig.class);

        // Wrapped in a root object holding only `tls`, because that is what RJSF is handed: the form
        // under test renders this one section, not the whole adapter.
        final ObjectNode schema = MAPPER.createObjectNode();
        schema.put("type", "object");
        schema.set("properties", MAPPER.createObjectNode().set("tls", dataSchema.at("/properties/tls")));

        final ObjectNode artifact = MAPPER.createObjectNode();
        artifact.set("schema", schema);
        artifact.set("uiSchema", MAPPER.createObjectNode().set("tls", uiSchema().at("/tls")));
        return artifact;
    }

    /** Read the way the API reads it: the file carries a licence header comment before the JSON. */
    private static @NotNull JsonNode uiSchema() {
        try {
            return MAPPER.reader()
                    .withFeatures(JsonParser.Feature.ALLOW_COMMENTS)
                    .readTree(OpcUaProtocolAdapterInformation.INSTANCE.getUiSchema());
        } catch (final Exception e) {
            throw new AssertionError("opcua-adapter-ui-schema.json is not parsable", e);
        }
    }

    private static @Nullable JsonNode readCommitted(final @NotNull Path artifact) throws IOException {
        return Files.exists(artifact) ? MAPPER.readTree(Files.readString(artifact)) : null;
    }

    /**
     * The directory holding both {@code modules} and {@code hivemq-edge-frontend}, found by walking up
     * from the test's working directory rather than by counting {@code ../} - Gradle's working
     * directory is the module project, but that is a default, not a promise.
     */
    private static @NotNull Path repositoryRoot() {
        Path candidate = Path.of("").toAbsolutePath();
        while (candidate != null) {
            if (Files.isDirectory(candidate.resolve("modules"))
                    && Files.isDirectory(candidate.resolve("hivemq-edge-frontend"))) {
                return candidate;
            }
            candidate = candidate.getParent();
        }
        throw new AssertionError(
                "could not locate the repository root above " + Path.of("").toAbsolutePath());
    }
}
