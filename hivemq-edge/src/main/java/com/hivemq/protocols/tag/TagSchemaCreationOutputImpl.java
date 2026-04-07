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
package com.hivemq.protocols.tag;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.hivemq.adapter.sdk.api.schema.TagSchemaCreationOutput;
import com.hivemq.adapter.sdk.api.schema.impl.SchemaJsonRepresentation;
import com.hivemq.exceptions.StackLessProtocolAdapterException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class TagSchemaCreationOutputImpl implements TagSchemaCreationOutput {

    private volatile @Nullable String message = null;
    private volatile @NotNull Status status = Status.SUCCESS;
    private final @NotNull CompletableFuture<ObjectNode> future =
            new CompletableFuture<ObjectNode>().orTimeout(30, TimeUnit.SECONDS);

    public @Nullable String getMessage() {
        return message;
    }

    public @NotNull CompletableFuture<ObjectNode> getFuture() {
        return future;
    }

    @Override
    public void finish(@NotNull final DataPointSchema schema) {
        final SchemaJsonRepresentation instance = SchemaJsonRepresentation.INSTANCE;
        future.complete(instance.toCompositeSchema(schema));
    }

    @Override
    public void finish(final @NotNull JsonNode schema) {
        if (schema instanceof final ObjectNode objectNode) {
            finish(new DataPointSchema(SchemaJsonRepresentation.INSTANCE.fromJsonSchema(objectNode), null, null));
        } else {
            status = Status.UNSPECIFIED_FAILURE;
            future.completeExceptionally(
                    new StackLessProtocolAdapterException("The provided json schema is not an object node."));
        }
    }

    @Override
    public void notSupported() {
        status = Status.NOT_SUPPORTED;
        future.completeExceptionally(
                new UnsupportedOperationException("The adapter does not support the creation of json impl for tags."));
    }

    @Override
    public void adapterNotStarted() {
        status = Status.ADAPTER_NOT_STARTED;
        future.completeExceptionally(new IllegalStateException("The adapter was not started yet."));
    }

    @Override
    public void fail(final @NotNull Throwable t, final @Nullable String errorMessage) {
        status = Status.UNSPECIFIED_FAILURE;
        message = errorMessage;
        future.completeExceptionally(t);
    }

    @Override
    public void fail(final @NotNull String errorMessage) {
        status = Status.UNSPECIFIED_FAILURE;
        message = errorMessage;
        future.completeExceptionally(new StackLessProtocolAdapterException("Json impl creation for tag failed."));
    }

    @Override
    public void tagNotFound(final @NotNull String errorMessage) {
        status = Status.TAG_NOT_FOUND;
        future.completeExceptionally(new StackLessProtocolAdapterException(errorMessage));
    }

    public @NotNull Status getStatus() {
        return status;
    }

    public enum Status {
        SUCCESS,
        NOT_SUPPORTED,
        ADAPTER_NOT_STARTED,
        TAG_NOT_FOUND,
        UNSPECIFIED_FAILURE
    }
}
