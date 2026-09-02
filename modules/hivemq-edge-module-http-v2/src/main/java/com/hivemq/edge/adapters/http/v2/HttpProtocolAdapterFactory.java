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
package com.hivemq.edge.adapters.http.v2;

import com.hivemq.adapter.sdk.api.schema.ArraySchema;
import com.hivemq.adapter.sdk.api.schema.ObjectSchema;
import com.hivemq.adapter.sdk.api.schema.ScalarSchema;
import com.hivemq.adapter.sdk.api.schema.ScalarType;
import com.hivemq.adapter.sdk.api.schema.Schema;
import com.hivemq.adapter.sdk.api.v2.ProtocolAdapterInformation;
import com.hivemq.adapter.sdk.api.v2.factories.ProtocolAdapterFactory;
import com.hivemq.adapter.sdk.api.v2.model.ProtocolAdapterInput;
import com.hivemq.adapter.sdk.api.v2.model.ProtocolAdapterOutput;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.jetbrains.annotations.NotNull;

/**
 * The factory for the v2 HTTP adapter type. It is discovered by the module loader through the
 * {@code META-INF/services} declaration and instantiated with its no-argument constructor. It exposes the type
 * identity, constructs a {@link HttpProtocolAdapter} per configured instance, and advertises the reused v1
 * {@link Schema}s the framework validates the instance configuration and projects the node definition against.
 */
public final class HttpProtocolAdapterFactory implements ProtocolAdapterFactory {

    private static final @NotNull Schema ADAPTER_CONFIG_SCHEMA = buildAdapterConfigSchema();
    private static final @NotNull Schema NODE_DEFINITION_SCHEMA = buildNodeDefinitionSchema();

    @Override
    public @NotNull ProtocolAdapterInformation information() {
        return HttpProtocolAdapterInformation.INSTANCE;
    }

    @Override
    public @NotNull HttpProtocolAdapter createAdapter(
            final @NotNull ProtocolAdapterInput input, final @NotNull ProtocolAdapterOutput output) {
        return new HttpProtocolAdapter(input, output);
    }

    @Override
    public @NotNull Schema adapterConfigSchema() {
        return ADAPTER_CONFIG_SCHEMA;
    }

    @Override
    public @NotNull Schema nodeDefinitionSchema() {
        return NODE_DEFINITION_SCHEMA;
    }

    private static @NotNull Schema buildAdapterConfigSchema() {
        final Map<String, Schema> properties = new LinkedHashMap<>();
        properties.put(
                "httpConnectTimeoutSeconds",
                new ScalarSchema(
                        ScalarType.LONG,
                        1,
                        HttpAdapterConfiguration.MAX_TIMEOUT_SECONDS,
                        "HTTP Connection Timeout",
                        "Timeout (in seconds) to allow the underlying HTTP connection to be established.",
                        false,
                        true,
                        false));
        properties.put(
                "allowUntrustedCertificates",
                booleanProperty(
                        "Allow Untrusted Certificates",
                        "Allow the adapter to connect to untrusted TLS sources (for example expired certificates)."));
        properties.put(
                "assertResponseIsJson",
                booleanProperty(
                        "Assert JSON Response?",
                        "Always parse the response body as JSON data, regardless of its Content-Type."));
        properties.put(
                "httpPublishSuccessStatusCodeOnly",
                booleanProperty(
                        "Publish Only On Success Codes",
                        "Only publish data when the HTTP response code is successful (200-299)."));
        return new ObjectSchema(properties, List.of(), false, "HTTP adapter configuration", null, false, true, false);
    }

    private static @NotNull Schema buildNodeDefinitionSchema() {
        final Map<String, Schema> properties = new LinkedHashMap<>();
        properties.put("url", stringProperty("URL", "The URL of the HTTP request you would like to make.", false));
        // The reused v1 Schema has no enum constraint, so the method projects as a plain string; the accepted values
        // are the names of the HttpMethod constants.
        properties.put(
                "httpRequestMethod", stringProperty("Http Method", "The HTTP method (one of GET, POST, PUT).", false));
        properties.put(
                "httpRequestTimeoutSeconds",
                new ScalarSchema(
                        ScalarType.LONG,
                        1,
                        HttpNode.MAX_TIMEOUT_SECONDS,
                        "Http Request Timeout",
                        "Timeout (in seconds) to wait for the HTTP request to complete.",
                        false,
                        true,
                        false));
        properties.put(
                "httpRequestBodyContentType",
                stringProperty(
                        "Http Request Content Type",
                        "The content type of the request body (one of JSON, PLAIN, HTML, XML, YAML).",
                        false));
        properties.put(
                "httpRequestBody",
                stringProperty("Http Request Body", "The body to include in the HTTP request.", true));
        properties.put(
                "httpHeaders",
                new ArraySchema(
                        httpHeaderSchema(),
                        null,
                        null,
                        "HTTP Headers",
                        "HTTP headers to be added to your requests.",
                        false,
                        true,
                        false));
        return new ObjectSchema(properties, List.of("url"), false, "HTTP node definition", null, false, true, false);
    }

    private static @NotNull Schema httpHeaderSchema() {
        final Map<String, Schema> properties = new LinkedHashMap<>();
        properties.put("name", stringProperty("Name", "The name of the HTTP header.", false));
        properties.put("value", stringProperty("Value", "The value of the HTTP header.", false));
        return new ObjectSchema(properties, List.of("name", "value"), false, "HTTP header", null, false, true, false);
    }

    private static @NotNull Schema stringProperty(
            final @NotNull String title, final @NotNull String description, final boolean nullable) {
        return new ScalarSchema(ScalarType.STRING, null, null, title, description, nullable, true, false);
    }

    private static @NotNull Schema booleanProperty(final @NotNull String title, final @NotNull String description) {
        return new ScalarSchema(ScalarType.BOOLEAN, null, null, title, description, false, true, false);
    }
}
