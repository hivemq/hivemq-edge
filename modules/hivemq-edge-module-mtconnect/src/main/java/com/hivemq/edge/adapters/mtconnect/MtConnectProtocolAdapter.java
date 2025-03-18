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
import com.hivemq.adapter.sdk.api.exceptions.ProtocolAdapterException;
import com.hivemq.adapter.sdk.api.factories.AdapterFactories;
import com.hivemq.adapter.sdk.api.factories.DataPointFactory;
import com.hivemq.adapter.sdk.api.model.ProtocolAdapterInput;
import com.hivemq.adapter.sdk.api.model.ProtocolAdapterStartInput;
import com.hivemq.adapter.sdk.api.model.ProtocolAdapterStartOutput;
import com.hivemq.adapter.sdk.api.model.ProtocolAdapterStopInput;
import com.hivemq.adapter.sdk.api.model.ProtocolAdapterStopOutput;
import com.hivemq.adapter.sdk.api.polling.batch.BatchPollingInput;
import com.hivemq.adapter.sdk.api.polling.batch.BatchPollingOutput;
import com.hivemq.adapter.sdk.api.polling.batch.BatchPollingProtocolAdapter;
import com.hivemq.adapter.sdk.api.state.ProtocolAdapterState;
import com.hivemq.adapter.sdk.api.tag.Tag;
import com.hivemq.edge.adapters.mtconnect.config.MtConnectAdapterConfig;
import com.hivemq.edge.adapters.mtconnect.config.tag.MtConnectAdapterTag;
import com.hivemq.edge.adapters.mtconnect.config.tag.MtConnectAdapterTagDefinition;
import com.hivemq.edge.adapters.mtconnect.models.MtConnectData;
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
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;


public class MtConnectProtocolAdapter implements BatchPollingProtocolAdapter {
    public static final @NotNull String NODE_SCHEMA_LOCATION = "schemaLocation";
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

    protected final @NotNull List<MtConnectAdapterTag> tags;
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
        this.tags = input.getTags().stream().map(tag -> (MtConnectAdapterTag) tag).toList();
        this.version = input.getVersion();
    }

    private static boolean isStatusCodeSuccessful(final int statusCode) {
        // https://developer.mozilla.org/en-US/docs/Web/HTTP/Status
        return statusCode >= 200 && statusCode < 300;
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
    public void poll(final @NotNull BatchPollingInput pollingInput, final @NotNull BatchPollingOutput pollingOutput) {
        if (httpClient == null) {
            pollingOutput.fail(new ProtocolAdapterException(),
                    "No response was created, because the HTTP client is null.");
        } else if (tags.isEmpty()) {
            pollingOutput.fail(new ProtocolAdapterException(), "No response was created, tags are empty.");
        } else {
            final List<CompletableFuture<MtConnectData>> pollingFutures = tags.stream().map(this::pollXml).toList();
            CompletableFuture.allOf(pollingFutures.toArray(new CompletableFuture[]{}))
                    .whenComplete((result, throwable) -> {
                        if (throwable != null) {
                            pollingOutput.fail(throwable, "Error while polling tags.");
                        } else {
                            final List<MtConnectData> dataList = pollingFutures.stream().map(future -> {
                                try {
                                    return future.get();
                                } catch (Exception e) {
                                    return null;
                                }
                            }).toList();
                            if (dataList.isEmpty()) {
                                protocolAdapterState.setConnectionStatus(ProtocolAdapterState.ConnectionStatus.ERROR);
                                pollingOutput.fail("Polled empty list of tags.");
                            } else if (dataList.stream().anyMatch(Objects::isNull)) {
                                protocolAdapterState.setConnectionStatus(ProtocolAdapterState.ConnectionStatus.ERROR);
                                pollingOutput.fail("At least one completed future failed while polling tags.");
                            } else {
                                final Optional<MtConnectData> optionalFirstFailedData =
                                        dataList.stream().filter(data -> !data.isSuccessful()).findFirst();
                                optionalFirstFailedData.ifPresentOrElse(data -> {
                                    protocolAdapterState.setConnectionStatus(ProtocolAdapterState.ConnectionStatus.ERROR);
                                    if (data.getErrorMessage() == null) {
                                        data.setErrorMessage("Error while polling tag [" + data.getTagName() + "]");
                                    }
                                    if (data.getCause() == null) {
                                        pollingOutput.fail(data.getErrorMessage());
                                    } else {
                                        pollingOutput.fail(data.getCause(), data.getErrorMessage());
                                    }
                                }, () -> {
                                    final DataPointFactory dataPointFactory = adapterFactories.dataPointFactory();
                                    dataList.stream()
                                            .map(data -> dataPointFactory.createJsonDataPoint(data.getTagName(),
                                                    Objects.requireNonNull(data.getJsonString())))
                                            .forEach(pollingOutput::addDataPoint);
                                    protocolAdapterState.setConnectionStatus(ProtocolAdapterState.ConnectionStatus.STATELESS);
                                    pollingOutput.finish();
                                });
                            }
                        }
                    });
        }
    }

    protected @NotNull CompletableFuture<MtConnectData> pollXml(final @NotNull MtConnectAdapterTag tag) {
        final MtConnectAdapterTagDefinition definition = tag.getDefinition();
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
        return Objects.requireNonNull(httpClient)
                .sendAsync(httpRequest, HttpResponse.BodyHandlers.ofString())
                .thenApply(httpResponse -> processHttpResponse(httpResponse, tag));
    }

    protected @NotNull MtConnectData processHttpResponse(
            final @NotNull HttpResponse<String> httpResponse,
            final @NotNull Tag tag) {
        final MtConnectAdapterTagDefinition definition = (MtConnectAdapterTagDefinition) tag.getDefinition();
        final MtConnectData mtConnectData = new MtConnectData(definition.getUrl(),
                isStatusCodeSuccessful(httpResponse.statusCode()),
                tag.getName());
        if (mtConnectData.isSuccessful()) {
            // Let's make sure the response body is XML.
            final Optional<String> optionalContentType = httpResponse.headers().firstValue(HEADER_CONTENT_TYPE);
            if (optionalContentType.map(value -> CONTENT_TYPE_TEXT_XML.equals(value) ||
                    CONTENT_TYPE_APPLICATION_XML.equals(value)).orElse(false)) {
                try {
                    mtConnectData.setJsonString(processXml(httpResponse.body(), definition));
                } catch (final Exception e) {
                    mtConnectData.setSuccessful(false);
                    mtConnectData.setCause(e);
                    mtConnectData.setErrorMessage(e.getMessage());
                    if (LOGGER.isDebugEnabled()) {
                        LOGGER.debug(e.getMessage(), e);
                    }
                }
            } else {
                mtConnectData.setSuccessful(false);
                mtConnectData.setErrorMessage("Content type [" +
                        optionalContentType.orElse("") +
                        "] is not supported.");
            }
        }
        return mtConnectData;
    }

    protected @NotNull String processXml(
            final @NotNull String body,
            final @NotNull MtConnectAdapterTagDefinition definition)
            throws JsonProcessingException, XMLParseException, JAXBException {
        final ObjectMapper objectMapper =
                definition.isIncludeNull() ? OBJECT_MAPPER_INCLUDE_NULL : OBJECT_MAPPER_EXCLUDE_NULL;
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
                    return objectMapper.writeValueAsString(element.getValue());
                } catch (final Exception e) {
                    throw new XMLParseException(e, "Incoming XML message failed to conform " + schemaLocation);
                }
            }
        }
        final @NotNull JsonNode rootNode = XML_MAPPER.readTree(body);
        final @Nullable JsonNode jsonNodeSchemaLocation = rootNode.get(NODE_SCHEMA_LOCATION);
        if (jsonNodeSchemaLocation == null) {
            throw new XMLParseException("Attribute schemaLocation is not found");
        }
        return objectMapper.writeValueAsString(rootNode);
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
