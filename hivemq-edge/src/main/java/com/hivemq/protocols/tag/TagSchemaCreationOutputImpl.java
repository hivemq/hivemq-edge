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
     * The composed schema as a future, in the northbound (read) direction.
     * <p>
     * Each call composes a fresh document. Composition is a pure function of the {@link DataPointSchema} the
     * adapter finished with, so repeated calls are equal in value; they are deliberately <em>not</em> the same
     * instance, because {@link ObjectNode} is mutable and a shared one would let one caller's edit rewrite what
     * every other caller already holds. Pinned by
     * {@code TagSchemaCreationOutputImplSchemaBuilderTest.test_repeatedCalls_produceEqualButIndependentDocuments}.
     *
     * @deprecated retained for source compatibility with callers outside this repository (the integration tests
     *     in hivemq-edge-test). Use {@link #getSchema(TagSchemaDirection)} instead, which makes the direction
     *     explicit. Note this returns the NORTHBOUND schema, which is what this method always produced. The
     *     southbound DataHub resources in {@code ProtocolAdapterWritingServiceImpl.createDataHubResources} used
     *     to obtain the schema through here and therefore registered the northbound document as the destination
     *     of a write; they now request {@link TagSchemaDirection#SOUTHBOUND} explicitly.
     */
    @Deprecated(forRemoval = true)
    public @NotNull CompletableFuture<ObjectNode> getFuture() {
        return future.thenApply(
                tagSchemas -> SchemaJsonRepresentation.INSTANCE.toJsonSchemaDocument(northboundSchema(tagSchemas)));
    }

    /**
     * Resolves the produced schema, selects the schema for the requested direction, and renders it as a
     * JSON Schema document.
     * <p>
     * Which aspects belong to a northbound vs a southbound shape is this class's concern, because it owns the
     * {@link DataPointSchema}. {@link SchemaJsonRepresentation} is direction-agnostic: it only converts a
     * {@link Schema} to its JSON representation.
     *
     * @param direction NORTHBOUND (read) or SOUTHBOUND (write).
     * @return the JSON schema document for that direction.
     */
    public @NotNull ObjectNode getSchema(final @NotNull TagSchemaDirection direction)
            throws ExecutionException, InterruptedException {
        final DataPointSchema tagSchemas = future.get();
        final Schema schema =
                switch (direction) {
                    case NORTHBOUND -> northboundSchema(tagSchemas);
                    case SOUTHBOUND -> southboundSchema(tagSchemas);
                };
        return SchemaJsonRepresentation.INSTANCE.toJsonSchemaDocument(schema);
    }

    /**
     * The schema for the <em>northbound</em> (read) direction: the envelope ({@code tagName}, {@code timestamp})
     * wrapping {@code value}, plus optional {@code metadata} / {@code context}. Writability is irrelevant here — a
     * read consumer observes the full data shape.
     * <p>
     * Must stay semantically identical to the deprecated
     * {@link SchemaJsonRepresentation#toCompositeSchema(DataPointSchema)}, which adapters built against an older
     * SDK still see; the equality is pinned by {@code TagSchemaCreationOutputImplSchemaBuilderTest}.
     */
    private static @NotNull Schema northboundSchema(final @NotNull DataPointSchema tagSchemas) {
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
     * The schema for the <em>southbound</em> (write) direction: only what a write targets. The non-writable
     * envelope ({@code tagName}, {@code timestamp}, {@code metadata}, {@code context}) is dropped entirely — a write
     * form should never present fields that cannot be written. The {@code value} is the adapter's explicit
     * southbound schema when it provides one (e.g. a condition tag's {@code {eventId, method, comment}});
     * otherwise the plain {@code valueSchema}.
     * <p>
     * <b>Note:</b> read-only fields inside the value are <em>not</em> pruned. A statically-derived write schema
     * cannot express write-validity correctly (an array of read-only items admits only {@code []}; a required
     * read-only member makes the object unsatisfiable). Only the non-writable envelope is stripped here.
     * <p>
     * <b>{@code readOnly} is descriptive metadata, not a safety boundary.</b> It is a JSON Schema annotation, not
     * an assertion: the DataHub validator on the southbound path (networknt, default configuration) does not
     * reject an instance for carrying a read-only field. Consumers use it to decide what to <em>offer</em> as a
     * write destination. Turning it into an enforced rule ({@code readOnly ⇒ ⊥}) is runtime matcher work left to
     * Nevsky; until then nothing in Edge rejects a write on the strength of this flag.
     * <p>
     * Because that rule is coming, the flags this method emits must already be true rather than merely
     * harmless: the document root is marked writable below, and
     * {@code test_southboundRoot_isNeverReadOnly_soEnforcementWouldNotRejectEveryWrite} pins it by running a
     * conforming write through networknt with enforcement switched on.
     * <p>
     * Because {@link com.hivemq.adapter.sdk.api.schema.SchemaBuilder} defaults every node to
     * {@code writable = false}, an adapter supplying an explicit southbound schema must chain {@code .writable()}
     * on the root and on each writable member — otherwise the whole shape renders {@code readOnly} and offers no
     * destinations at all. See {@code TagSchemaCreationOutputImplSchemaBuilderTest} for both cases.
     */
    private static @NotNull Schema southboundSchema(final @NotNull DataPointSchema tagSchemas) {
        final Schema writeValue =
                tagSchemas.southboundSchema() != null ? tagSchemas.southboundSchema() : tagSchemas.valueSchema();

        final var builder = new SchemaBuilder().startObject();
        // No readable/writable annotations here: a property defined via schema() renders the prebuilt schema
        // as-is (SchemaBuilder ignores annotations chained after schema()), so the value's flags are whatever
        // the adapter put on its own schema root — identical to the northbound rendering of the same value,
        // which is what lets a consumer detect "read and write use the same schema" by comparing value shapes.
        builder.property("value").required().schema(writeValue);
        // The wrapper itself is writable, and saying so matters. SchemaBuilder defaults every node to
        // writable = false, which renders readOnly on the document root — on the one document whose entire
        // purpose is to be written. Harmless while readOnly is only an annotation, but the moment it becomes
        // an assertion (the readOnly ⇒ ⊥ rule) a read-only root rejects EVERY southbound write for EVERY tag,
        // arrays or not: networknt's readOnly check fires on the node being present at all, so the root error
        // lands on every instance. Verified against networknt with readOnly enforcement enabled — with the
        // default flag a conforming command produces "$: is a readonly field, it cannot be changed".
        //
        // The northbound wrapper keeps its readOnly, and correctly so: that document describes what is
        // published, and it genuinely cannot be written. The asymmetry is the point, not an oversight.
        return builder.endObject().writable().build();
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
