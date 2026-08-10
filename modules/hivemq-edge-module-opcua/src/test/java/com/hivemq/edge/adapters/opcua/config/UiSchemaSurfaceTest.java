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

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hivemq.api.json.CustomConfigSchemaGenerator;
import com.hivemq.edge.adapters.opcua.OpcUaProtocolAdapterInformation;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * {@code opcua-adapter-ui-schema.json} checked against the data schema it decorates.
 *
 * <p>Nothing had ever parsed this file in a test. That is how the P1 shipped: {@code tls.ui:order}
 * listed four of the six TLS properties and carried no {@code "*"} wildcard, and React JSON Schema
 * Form refuses to render a whole object when its {@code ui:order} neither names a property nor admits
 * it through a wildcard — so the entire TLS section disappeared from the adapter form. The generated
 * data schema was correct throughout, which is why {@link TlsSchemaSurfaceTest} saw nothing wrong.
 *
 * <p>The rules below are RJSF's, and they are checked over every ordered object in the file rather
 * than over {@code tls} alone: the defect is a property being added to a record while the hand-written
 * order is left behind, and that can happen anywhere. A hard-coded expected order would have to be
 * updated by the same person who forgot the order in the first place.
 *
 * <p>What this cannot cover is what the form actually submits. That is
 * {@code ChakraRJSForm.spec.cy.tsx} in {@code hivemq-edge-frontend}, which renders this same schema
 * pair through RJSF and asserts the submitted data.
 */
class UiSchemaSurfaceTest {

    private static final @NotNull ObjectMapper MAPPER = new ObjectMapper();

    private static final @NotNull String WILDCARD = "*";

    @Test
    void theUiSchemaParsesAtAll() {
        // Not a formality. ProtocolAdapterApiUtils.getUiSchemaForAdapter falls back to a generic
        // DEFAULT_SCHEMA on a parse failure and only logs a warning, so a stray comma here would
        // replace the entire adapter form with the default one and nothing would fail.
        assertThat(OpcUaProtocolAdapterInformation.INSTANCE.getUiSchema())
                .as("the UI schema resource must be on the classpath")
                .isNotNull();
        assertThat(uiSchema().isObject()).isTrue();
    }

    @ParameterizedTest
    @ValueSource(classes = {OpcUaSpecificAdapterConfig.class, BidirectionalOpcUaSpecificAdapterConfig.class})
    void everyOrderedObjectAccountsForEveryPropertyBeneathIt(final @NotNull Class<?> configClass) {
        // The P1, generalized. Both config classes are checked because the API serves the northbound
        // schema or the bidirectional one depending on whether writing is enabled, and the same
        // ui:order decorates both.
        final List<String> violations = new ArrayList<>();
        walk(uiSchema(), dataSchema(configClass), "", violations);

        assertThat(violations)
                .as("ui:order must cover every property the form will render, in %s", configClass.getSimpleName())
                .isEmpty();
    }

    @ParameterizedTest
    @ValueSource(classes = {OpcUaSpecificAdapterConfig.class, BidirectionalOpcUaSpecificAdapterConfig.class})
    void everyEnumLabelListMatchesItsEnumInLengthAndOrder(final @NotNull Class<?> configClass) {
        // ui:enumNames is positional: RJSF pairs label i with enum value i and does not check names.
        // Add a preset without adding its label and every label below it describes the wrong value -
        // silently, in the operator's dropdown, on the setting that decides certificate validation.
        final List<String> violations = new ArrayList<>();
        walkEnumNames(uiSchema(), dataSchema(configClass), "", violations);

        assertThat(violations)
                .as("ui:enumNames must track the enum, in %s", configClass.getSimpleName())
                .isEmpty();
    }

    private static void walk(
            final @NotNull JsonNode uiNode,
            final @NotNull JsonNode schemaNode,
            final @NotNull String path,
            final @NotNull List<String> violations) {

        final JsonNode order = uiNode.get("ui:order");
        if (order != null && order.isArray()) {
            final List<String> ordered = textValuesOf(order);
            final List<String> properties = propertyNamesOf(schemaNode);
            final long wildcards = ordered.stream().filter(WILDCARD::equals).count();
            if (wildcards > 1) {
                // RJSF rejects more than one wildcard outright.
                violations.add("%s ui:order has %d '*' entries; at most one is allowed".formatted(at(path), wildcards));
            } else if (wildcards == 0) {
                final List<String> missing = properties.stream()
                        .filter(property -> !ordered.contains(property))
                        .toList();
                if (!missing.isEmpty()) {
                    violations.add(("%s ui:order omits %s and has no '*' wildcard, so the form will not render "
                                    + "this object at all")
                            .formatted(at(path), missing));
                }
            }

            // The other direction. RJSF drops a name that is not in the schema without complaining, so
            // this cannot break the form - but the name is a property that was renamed or removed with
            // the order left behind, and the order being out of date is the whole defect class here.
            // `truststore` carried an `enabled` entry this way, from a record that never had the field.
            final List<String> extraneous = ordered.stream()
                    .filter(entry -> !WILDCARD.equals(entry))
                    .filter(entry -> !properties.contains(entry))
                    .toList();
            if (!extraneous.isEmpty()) {
                violations.add("%s ui:order names %s, which the schema does not have".formatted(at(path), extraneous));
            }
        }

        for (final Map.Entry<String, JsonNode> child : childObjectsOf(uiNode)) {
            walk(
                    child.getValue(),
                    schemaNode.at("/properties/" + child.getKey()),
                    childPath(path, child.getKey()),
                    violations);
        }
    }

    private static void walkEnumNames(
            final @NotNull JsonNode uiNode,
            final @NotNull JsonNode schemaNode,
            final @NotNull String path,
            final @NotNull List<String> violations) {

        final JsonNode labels = uiNode.get("ui:enumNames");
        if (labels != null && labels.isArray()) {
            final List<String> values = textValuesOf(schemaNode.path("enum"));
            final List<String> labelTexts = textValuesOf(labels);
            if (values.isEmpty()) {
                violations.add("%s declares ui:enumNames but the data schema has no enum there".formatted(at(path)));
            } else if (values.size() != labelTexts.size()) {
                violations.add("%s has %d ui:enumNames for %d enum values %s"
                        .formatted(at(path), labelTexts.size(), values.size(), values));
            } else {
                for (int i = 0; i < values.size(); i++) {
                    // The labels are written as "VALUE: explanation", which is what makes the pairing
                    // checkable at all - keep them that way.
                    if (!labelTexts.get(i).startsWith(values.get(i))) {
                        violations.add("%s ui:enumNames[%d] is '%s' but enum[%d] is '%s'"
                                .formatted(at(path), i, labelTexts.get(i), i, values.get(i)));
                    }
                }
            }
        }

        for (final Map.Entry<String, JsonNode> child : childObjectsOf(uiNode)) {
            walkEnumNames(
                    child.getValue(),
                    schemaNode.at("/properties/" + child.getKey()),
                    childPath(path, child.getKey()),
                    violations);
        }
    }

    /** The children that are themselves objects, skipping the {@code ui:} directives. */
    private static @NotNull List<Map.Entry<String, JsonNode>> childObjectsOf(final @NotNull JsonNode uiNode) {
        final List<Map.Entry<String, JsonNode>> children = new ArrayList<>();
        for (final Iterator<Map.Entry<String, JsonNode>> it =
                        uiNode.properties().iterator();
                it.hasNext(); ) {
            final Map.Entry<String, JsonNode> entry = it.next();
            if (!entry.getKey().startsWith("ui:") && entry.getValue().isObject()) {
                children.add(entry);
            }
        }
        return children;
    }

    private static @NotNull List<String> propertyNamesOf(final @NotNull JsonNode schemaNode) {
        final List<String> names = new ArrayList<>();
        schemaNode.path("properties").properties().forEach(entry -> names.add(entry.getKey()));
        return names;
    }

    private static @NotNull List<String> textValuesOf(final @NotNull JsonNode array) {
        final List<String> values = new ArrayList<>();
        array.forEach(element -> values.add(element.asText()));
        return values;
    }

    private static @NotNull String childPath(final @NotNull String path, final @NotNull String child) {
        return path.isEmpty() ? child : path + "." + child;
    }

    private static @NotNull String at(final @NotNull String path) {
        return path.isEmpty() ? "<root>" : path;
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

    private static @NotNull JsonNode dataSchema(final @NotNull Class<?> configClass) {
        return new CustomConfigSchemaGenerator().generateJsonSchema(configClass);
    }
}
