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
import com.hivemq.adapter.sdk.api.schema.ScalarType;
import com.hivemq.adapter.sdk.api.schema.Schema;
import com.hivemq.adapter.sdk.api.schema.SchemaBuilder;
import com.hivemq.adapter.sdk.api.schema.SchemaJsonRepresentation;
import com.hivemq.adapter.sdk.api.schema.TagSchemaCreationOutput;
import com.hivemq.exceptions.StackLessProtocolAdapterException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class TagSchemaCreationOutputImpl implements TagSchemaCreationOutput {

    /**
     * The direction of the tag schema requested by a consumer.
     */
    public enum Direction {
        /** Northbound / read: the full data shape — {@code tagName}, {@code timestamp}, {@code value}, and any
         * {@code metadata} / {@code context}. */
        READ,
        /** Southbound / write: only what can be written — just the {@code value}, with the non-writable
         * {@code tagName} / {@code timestamp} / {@code metadata} / {@code context} dropped. */
        WRITE
    }

    private volatile @Nullable String message = null;
    private volatile @NotNull Status status = Status.SUCCESS;

    // The adapter provides the raw DataPointSchema; the direction (read/write) is chosen at the consuming edge,
    // so the future carries the schema itself rather than a pre-composed JSON node.
    private final @NotNull CompletableFuture<DataPointSchema> future =
            new CompletableFuture<DataPointSchema>().orTimeout(30, TimeUnit.SECONDS);

    public @Nullable String getMessage() {
        return message;
    }

    /**
     * Resolves the produced schema, selects the schema for the requested direction, and renders it as a
     * JSON Schema document.
     * <p>
     * Which aspects belong to a read vs a write shape is this class's concern, because it owns the
     * {@link DataPointSchema}. {@link SchemaJsonRepresentation} is direction-agnostic: it only converts a
     * {@link Schema} to its JSON representation.
     *
     * @param direction READ (northbound) or WRITE (southbound).
     * @return the JSON schema document for that direction.
     */
    public @NotNull ObjectNode getSchema(final @NotNull Direction direction)
            throws ExecutionException, InterruptedException {
        final DataPointSchema tagSchemas = future.get();
        final Schema schema =
                switch (direction) {
                    case READ -> readSchema(tagSchemas);
                    case WRITE -> writeSchema(tagSchemas);
                };
        return SchemaJsonRepresentation.INSTANCE.toJsonSchemaDocument(schema);
    }

    /**
     * The schema for the <em>read</em> (northbound) direction: the envelope ({@code tagName}, {@code timestamp})
     * wrapping {@code value}, plus optional {@code metadata} / {@code context}. Writability is irrelevant here — a
     * read consumer observes the full data shape.
     */
    private static @NotNull Schema readSchema(final @NotNull DataPointSchema tagSchemas) {
        final var builder = new SchemaBuilder().startObject();

        builder.property("tagName").scalar(ScalarType.STRING).readable().writable(false);
        builder.property("timestamp").scalar(ScalarType.LONG).readable().writable(false);
        builder.property("value")
                .required()
                .schema(tagSchemas.valueSchema())
                .readable()
                .writable();

        if (tagSchemas.metadataSchema() != null) {
            builder.property("metadata")
                    .schema(tagSchemas.metadataSchema())
                    .readable()
                    .writable(false);
        }
        if (tagSchemas.context() != null) {
            builder.property("context").schema(tagSchemas.context()).readable().writable(false);
        }

        return builder.endObject().build();
    }

    /**
     * The schema for the <em>write</em> (southbound) direction: only what can be written. The non-writable
     * envelope ({@code tagName}, {@code timestamp}, {@code metadata}, {@code context}) is dropped entirely — a write
     * form should never present fields that cannot be written. The {@code value} is the adapter's explicit
     * {@link DataPointSchema#writeSchema() writeSchema} when it provides one (e.g. a condition tag's
     * {@code {eventId, method, comment}}); otherwise the plain {@code valueSchema}.
     * <p>
     * <b>Note:</b> read-only fields are <em>not</em> pruned. A statically-derived write schema cannot express
     * write-validity correctly (an array of read-only items admits only {@code []}; a required read-only member
     * makes the object unsatisfiable) — that is a runtime concern (the {@code readOnly ⇒ ⊥} matcher rule) left to
     * Nevsky. Only the non-writable envelope is stripped here.
     */
    private static @NotNull Schema writeSchema(final @NotNull DataPointSchema tagSchemas) {
        final Schema writeValue =
                tagSchemas.writeSchema() != null ? tagSchemas.writeSchema() : tagSchemas.valueSchema();

        final var builder = new SchemaBuilder().startObject();
        builder.property("value").required().schema(writeValue).readable(false).writable();
        return builder.endObject().build();
    }

    @Override
    public void finish(@NotNull final DataPointSchema schema) {
        future.complete(schema);
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
        future.completeExceptionally(new StackLessProtocolAdapterException("Json schema creation for tag failed."));
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
