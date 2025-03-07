/*
 * Copyright 2023-present HiveMQ GmbH
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package com.hivemq.edge.adapters.mtconnect;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.hivemq.adapter.sdk.api.ProtocolAdapterInformation;
import com.hivemq.adapter.sdk.api.config.PollingContext;
import com.hivemq.adapter.sdk.api.data.DataPoint;
import com.hivemq.adapter.sdk.api.exceptions.ProtocolAdapterException;
import com.hivemq.adapter.sdk.api.factories.AdapterFactories;
import com.hivemq.adapter.sdk.api.model.ProtocolAdapterInput;
import com.hivemq.adapter.sdk.api.model.ProtocolAdapterStartInput;
import com.hivemq.adapter.sdk.api.model.ProtocolAdapterStartOutput;
import com.hivemq.adapter.sdk.api.model.ProtocolAdapterStopInput;
import com.hivemq.adapter.sdk.api.model.ProtocolAdapterStopOutput;
import com.hivemq.adapter.sdk.api.polling.PollingInput;
import com.hivemq.adapter.sdk.api.polling.PollingOutput;
import com.hivemq.adapter.sdk.api.polling.PollingProtocolAdapter;
import com.hivemq.adapter.sdk.api.state.ProtocolAdapterState;
import com.hivemq.adapter.sdk.api.tag.Tag;
import com.hivemq.edge.adapters.mtconnect.config.MtConnectAdapterConfig;
import com.hivemq.edge.adapters.mtconnect.config.tag.MtConnectAdapterTagDefinition;
import com.hivemq.edge.adapters.mtconnect.schemas.MtConnectSchema;
import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Unmarshaller;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.modelmbean.XMLParseException;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509ExtendedTrustManager;
import java.io.StringReader;
import java.net.Socket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.hivemq.adapter.sdk.api.state.ProtocolAdapterState.ConnectionStatus.ERROR;
import static com.hivemq.adapter.sdk.api.state.ProtocolAdapterState.ConnectionStatus.STATELESS;


public class MtConnectProtocolAdapter implements PollingProtocolAdapter {
    public static final @NotNull String NODE_SCHEMA_LOCATION = "schemaLocation";
    private static final @NotNull String DATA = "data";
    // https://www.ietf.org/rfc/rfc2376.txt
    private static final @NotNull String CONTENT_TYPE_APPLICATION_XML = "application/xml";
    // https://www.ietf.org/rfc/rfc2376.txt
    private static final @NotNull String CONTENT_TYPE_TEXT_XML = "text/xml";
    private static final @NotNull Logger LOGGER = LoggerFactory.getLogger(MtConnectProtocolAdapter.class);
    private static final @NotNull String USER_AGENT_HEADER = "User-Agent";
    private static final @NotNull String HEADER_CONTENT_TYPE = "Content-Type";
    private static final @NotNull ObjectMapper OBJECT_MAPPER_INCLUDE_NULL = new ObjectMapper();
    private static final @NotNull ObjectMapper OBJECT_MAPPER_EXCLUDE_NULL = new ObjectMapper();
    private static final @NotNull XmlMapper XML_MAPPER = new XmlMapper();

    static {
        OBJECT_MAPPER_EXCLUDE_NULL.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    }

    protected final @NotNull Map<String, Tag> tagMap;
    protected final @NotNull MtConnectAdapterConfig adapterConfig;
    protected final @NotNull ProtocolAdapterInformation adapterInformation;
    protected final @NotNull ProtocolAdapterState protocolAdapterState;
    private final @NotNull String adapterId;
    private final @NotNull String version;
    private final @NotNull AdapterFactories adapterFactories;
    protected volatile @Nullable HttpClient httpClient = null;

    public MtConnectProtocolAdapter(
            final @NotNull ProtocolAdapterInformation adapterInformation,
            final @NotNull ProtocolAdapterInput<MtConnectAdapterConfig> input) {
        this.adapterId = input.getAdapterId();
        this.adapterInformation = adapterInformation;
        this.adapterConfig = input.getConfig();
        this.adapterFactories = input.adapterFactories();
        this.protocolAdapterState = input.getProtocolAdapterState();
        this.tagMap = input.getTags().stream().collect(Collectors.toMap(Tag::getName, Function.identity()));
        this.version = input.getVersion();
    }

    private static boolean isSuccessfulResponse(final @NotNull HttpResponse<?> httpResponse) {
        // https://developer.mozilla.org/en-US/docs/Web/HTTP/Status
        return httpResponse.statusCode() >= 200 && httpResponse.statusCode() < 300;
    }

    protected @NotNull BiConsumer<HttpResponse<String>, Throwable> processHttpResponse(
            final @NotNull PollingOutput pollingOutput,
            final @NotNull MtConnectAdapterTagDefinition definition) {
        return (httpResponse, throwable) -> {
            if (throwable == null) {
                if (isSuccessfulResponse(httpResponse)) {
                    // Let's make sure the response body is XML.
                    if (httpResponse.headers()
                            .firstValue(HEADER_CONTENT_TYPE)
                            .map(value -> CONTENT_TYPE_TEXT_XML.equals(value) ||
                                    CONTENT_TYPE_APPLICATION_XML.equals(value))
                            .orElse(false)) {
                        try {
                            pollingOutput.addDataPoint(processXml(httpResponse.body(), definition));
                        } catch (final Exception e) {
                            throwable = e;
                        }
                    } else {
                        throwable = new RuntimeException("Response is not XML");
                    }
                    protocolAdapterState.setConnectionStatus(STATELESS);
                } else {
                    protocolAdapterState.setConnectionStatus(ERROR);
                }
            }
            if (throwable == null) {
                pollingOutput.finish();
            } else {
                pollingOutput.fail(throwable, null);
            }
        };
    }

    protected @NotNull DataPoint processXml(
            final @NotNull String body,
            final @NotNull MtConnectAdapterTagDefinition definition)
            throws JsonProcessingException, XMLParseException, JAXBException {
        final ObjectMapper objectMapper =
                definition.isIncludeNull() ? OBJECT_MAPPER_INCLUDE_NULL : OBJECT_MAPPER_EXCLUDE_NULL;
        @Nullable String jsonString = null;
        // There are some custom schemas not supported by this module.
        // Enable the schema validation will cause those messages fail the validation.
        if (definition.isEnableSchemaValidation()) {
            final @Nullable String schemaLocation = MtConnectSchema.extractSchemaLocation(body);
            final @Nullable MtConnectSchema schema = MtConnectSchema.of(schemaLocation);
            if (schema == null) {
                throw new XMLParseException("Schema " + schemaLocation + " is not support");
            }
            // The unmarshal call brings additional performance overhead.
            final @Nullable Unmarshaller unmarshaller = schema.getUnmarshaller();
            if (unmarshaller == null) {
                throw new XMLParseException("Schema " + schemaLocation + " is to be supported");
            } else {
                try (StringReader stringReader = new StringReader(body)) {
                    final JAXBElement<?> element = (JAXBElement<?>) unmarshaller.unmarshal(stringReader);
                    jsonString = objectMapper.writeValueAsString(element.getValue());
                } catch (final Exception e) {
                    throw new XMLParseException(e, "Incoming XML message failed to conform " + schemaLocation);
                }
            }
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("Publishing data {} version {}.{}",
                        schema.getType().getRootNodeName(),
                        schema.getMajorVersion(),
                        schema.getMinorVersion());
            }
        }
        if (jsonString == null) {
            final @NotNull JsonNode rootNode = XML_MAPPER.readTree(body);
            final @Nullable JsonNode jsonNodeSchemaLocation = rootNode.get(NODE_SCHEMA_LOCATION);
            if (jsonNodeSchemaLocation == null) {
                throw new XMLParseException("Attribute schemaLocation is not found");
            }
            jsonString = objectMapper.writeValueAsString(rootNode);
        }
        return adapterFactories.dataPointFactory().create(DATA, jsonString);
    }

    @Override
    public @NotNull String getId() {
        return adapterId;
    }

    @Override
    public void start(
            final @NotNull ProtocolAdapterStartInput input,
            final @NotNull ProtocolAdapterStartOutput output) {
        try {
            protocolAdapterState.setConnectionStatus(ProtocolAdapterState.ConnectionStatus.STATELESS);
            if (httpClient == null) {
                final HttpClient.Builder builder = HttpClient.newBuilder();
                builder.version(HttpClient.Version.HTTP_1_1)
                        .followRedirects(HttpClient.Redirect.NORMAL)
                        .connectTimeout(Duration.ofSeconds(adapterConfig.getHttpConnectTimeoutSeconds()));
                if (adapterConfig.isAllowUntrustedCertificates()) {
                    builder.sslContext(createTrustAllContext());
                }
                httpClient = builder.build();
            }
            output.startedSuccessfully();
        } catch (final Exception e) {
            output.failStart(e, null);
        }
    }

    @Override
    public void stop(
            final @NotNull ProtocolAdapterStopInput protocolAdapterStopInput,
            final @NotNull ProtocolAdapterStopOutput protocolAdapterStopOutput) {
        httpClient = null;
        protocolAdapterStopOutput.stoppedSuccessfully();
    }

    @Override
    public void poll(final @NotNull PollingInput pollingInput, final @NotNull PollingOutput pollingOutput) {
        if (httpClient == null) {
            pollingOutput.fail(new ProtocolAdapterException(),
                    "No response was created, because the HTTP client is null.");
            return;
        }
        final PollingContext pollingContext = pollingInput.getPollingContext();
        final String tagName = pollingContext.getTagName();
        Optional.ofNullable(tagMap.get(tagName))
                .ifPresentOrElse(tag -> pollXml(pollingOutput, tag, pollingContext),
                        () -> pollFail(pollingOutput, tagName));
    }

    protected void pollXml(
            final @NotNull PollingOutput pollingOutput,
            final @NotNull Tag tag,
            final @NotNull PollingContext pollingContext) {
        final MtConnectAdapterTagDefinition definition = (MtConnectAdapterTagDefinition) tag.getDefinition();
        final HttpRequest.Builder builder = HttpRequest.newBuilder();
        final String url = definition.getUrl();
        builder.uri(URI.create(url));
        builder.timeout(Duration.ofSeconds(definition.getHttpConnectTimeoutSeconds()));
        builder.setHeader(USER_AGENT_HEADER, String.format("HiveMQ-Edge; %s", version));
        definition.getHttpHeaders()
                .forEach(adapterHttpHeader -> builder.setHeader(adapterHttpHeader.getName(),
                        adapterHttpHeader.getValue()));
        builder.GET();
        final HttpRequest httpRequest = builder.build();
        final CompletableFuture<HttpResponse<String>> responseFuture =
                Objects.requireNonNull(httpClient).sendAsync(httpRequest, HttpResponse.BodyHandlers.ofString());
        responseFuture.whenComplete(processHttpResponse(pollingOutput, definition));
    }

    protected void pollFail(final @NotNull PollingOutput pollingOutput, final @NotNull String tagName) {
        pollingOutput.fail("Polling for protocol adapter failed because the used tag '" +
                tagName +
                "' was not found. For the polling to work the tag must be created via REST API or the UI.");
    }

    @Override
    public @NotNull ProtocolAdapterInformation getProtocolAdapterInformation() {
        return adapterInformation;
    }

    @Override
    public int getPollingIntervalMillis() {
        return adapterConfig.getPollingIntervalMillis();
    }

    @Override
    public int getMaxPollingErrorsBeforeRemoval() {
        return adapterConfig.getMaxPollingErrorsBeforeRemoval();
    }

    protected @NotNull SSLContext createTrustAllContext() {
        try {
            final SSLContext sslContext = SSLContext.getInstance("TLS");
            final X509ExtendedTrustManager trustManager = new X509ExtendedTrustManager() {
                @Override
                public void checkClientTrusted(
                        final X509Certificate @NotNull [] x509Certificates,
                        final @NotNull String s) {
                }

                @Override
                public void checkServerTrusted(
                        final X509Certificate @NotNull [] x509Certificates,
                        final @NotNull String s) {
                }

                @Override
                public X509Certificate @NotNull [] getAcceptedIssuers() {
                    return new X509Certificate[0];
                }

                @Override
                public void checkClientTrusted(
                        final X509Certificate @NotNull [] x509Certificates,
                        final @NotNull String s,
                        final @NotNull Socket socket) {
                }

                @Override
                public void checkServerTrusted(
                        final X509Certificate @NotNull [] x509Certificates,
                        final @NotNull String s,
                        final @NotNull Socket socket) {
                }

                @Override
                public void checkClientTrusted(
                        final X509Certificate @NotNull [] x509Certificates,
                        final @NotNull String s,
                        final @NotNull SSLEngine sslEngine) {
                }

                @Override
                public void checkServerTrusted(
                        final X509Certificate @NotNull [] x509Certificates,
                        final @NotNull String s,
                        final @NotNull SSLEngine sslEngine) {
                }
            };
            sslContext.init(null, new TrustManager[]{trustManager}, new SecureRandom());
            return sslContext;
        } catch (final NoSuchAlgorithmException | KeyManagementException e) {
            throw new RuntimeException(e);
        }
    }
}
