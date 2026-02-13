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
package com.hivemq.combining.vanilla;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import com.hivemq.combining.mapping.DataCombiningTransformationService;
import com.hivemq.combining.model.DataCombining;
import com.hivemq.mqtt.handler.publish.PublishingResult;
import com.hivemq.mqtt.message.publish.PUBLISH;
import com.hivemq.mqtt.message.publish.PUBLISHFactory;
import com.hivemq.mqtt.services.PrePublishProcessorService;
import com.hivemq.util.JsonUtils;
import com.jayway.jsonpath.DocumentContext;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class VanillaDataCombiningTransformationService implements DataCombiningTransformationService {
    private static final Logger LOGGER = LoggerFactory.getLogger(VanillaDataCombiningTransformationService.class);
    private final @NotNull PrePublishProcessorService prePublishProcessorService;

    public VanillaDataCombiningTransformationService(
            final @NotNull PrePublishProcessorService prePublishProcessorService) {
        this.prePublishProcessorService = prePublishProcessorService;
    }

    @Override
    public @NotNull CompletableFuture<Void> applyMappings(
            final @NotNull PUBLISH mergedPublish, final @NotNull DataCombining dataCombining) {
        final Optional<DocumentContext> optionalDocumentContext =
                JsonUtils.toDocumentContext(mergedPublish.getPayload());
        if (optionalDocumentContext.isEmpty()) {
            LOGGER.warn("Merged payload is not valid JSON, cannot apply data combining {}", dataCombining.id());
            return CompletableFuture.completedFuture(null);
        }
        final DocumentContext sourceDocumentContext = optionalDocumentContext.get();
        final ObjectMapper objectMapper = JsonUtils.NO_PRETTY_PRINT_WITH_JAVA_TIME;
        final ObjectNode destinationObjectNode = objectMapper.createObjectNode();
        dataCombining.instructions().stream()
                // Should we skip instructions without a data identifier reference?
                .filter(instruction -> Objects.nonNull(instruction.dataIdentifierReference()))
                .filter(instruction ->
                        Objects.nonNull(instruction.dataIdentifierReference().type()))
                .forEach(instruction -> {
                    final String sourceJsonPath = instruction.toSourceJsonPath();
                    Object value = null;
                    Exception parsingException = null;
                    try {
                        value = sourceDocumentContext.read(sourceJsonPath, Object.class);
                    } catch (final Exception e) {
                        parsingException = e;
                    }
                    if (parsingException != null) {
                        LOGGER.warn("Source json path {} does not exist", sourceJsonPath, parsingException);
                    } else if (value == null) {
                        LOGGER.warn("No data found for source json path {}", sourceJsonPath);
                    }
                    if (value != null) {
                        final String destinationJsonPath = instruction.toDestinationJsonPath();
                        final String[] fieldNames = destinationJsonPath.split("\\.+");
                        if (fieldNames.length == 0) {
                            LOGGER.warn("No destination field name specified in instruction {}", instruction);
                        } else {
                            ObjectNode currentNode = destinationObjectNode;
                            for (int i = 0; i < fieldNames.length - 1; i++) {
                                final String fieldName = fieldNames[i];
                                final ObjectNode childObjectNode;
                                if (currentNode.has(fieldName)) {
                                    final JsonNode childJsonNode = currentNode.get(fieldName);
                                    if (childJsonNode.isObject()) {
                                        childObjectNode = (ObjectNode) childJsonNode;
                                    } else {
                                        childObjectNode = objectMapper.createObjectNode();
                                        currentNode.replace(fieldName, childObjectNode);
                                    }
                                } else {
                                    childObjectNode = objectMapper.createObjectNode();
                                    currentNode.set(fieldName, childObjectNode);
                                }
                                currentNode = childObjectNode;
                            }
                            final String lastFieldName = fieldNames[fieldNames.length - 1];
                            currentNode.replace(lastFieldName, objectMapper.valueToTree(value));
                        }
                    }
                });
        // Let's publish the combined message.
        final PUBLISHFactory.Mqtt5Builder publish = new PUBLISHFactory.Mqtt5Builder()
                .fromPublish(mergedPublish)
                .withTopic(dataCombining.destination().topic())
                .withPayload(destinationObjectNode.toString().getBytes(StandardCharsets.UTF_8));
        final ListenableFuture<PublishingResult> listenableFuture = prePublishProcessorService.publish(
                publish.build(),
                MoreExecutors.newDirectExecutorService(),
                "vanilla-data-combining-" + dataCombining.id());
        final CompletableFuture<Void> returnFuture = new CompletableFuture<>();
        listenableFuture.addListener(
                () -> {
                    try {
                        listenableFuture.get();
                    } catch (final InterruptedException e) {
                        Thread.currentThread().interrupt();
                        returnFuture.completeExceptionally(e);
                    } catch (final ExecutionException e) {
                        returnFuture.completeExceptionally(e);
                    }
                },
                MoreExecutors.directExecutor());
        return returnFuture;
    }

    @Override
    public void removeScriptForDataCombining(final @NotNull DataCombining combining) {
        LOGGER.debug("Removing data combining {}", combining);
    }

    @Override
    public void addScriptForDataCombining(final @NotNull DataCombining combining) {
        LOGGER.debug("Adding data combining {}", combining);
    }
}
