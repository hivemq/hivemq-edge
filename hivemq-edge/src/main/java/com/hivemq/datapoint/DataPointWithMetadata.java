package com.hivemq.datapoint;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.hivemq.adapter.sdk.api.data.DataPoint;
import com.hivemq.adapter.sdk.api.datapoint.DataPointBuilder;
import org.jetbrains.annotations.NotNull;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Optional;
import java.util.function.Function;

public class DataPointWithMetadata implements DataPoint {
    private final @NotNull JsonNode jsonNode;

    public DataPointWithMetadata(@NotNull final JsonNode jsonNode) {
        this.jsonNode = jsonNode;
    }

    public static <R> @NotNull DataPointBuilder<R> builder(final @NotNull String tagName, final long timestamp, final @NotNull Function<DataPointBuilder<R>, R> completer) {
        return new DataPointBuilderImpl<>(tagName, timestamp, completer);
    }

    @Override
    public JsonNode getTagValue() {
        return jsonNode.get("value");
    }

    public Long getTimestamp() {
        return jsonNode.get("timestamp").asLong();
    }

    @Override
    public String getTagName() {
        return jsonNode.get("tagName").asText();
    }

    public Optional<JsonNode> getMetadata() {
        return Optional.ofNullable(jsonNode.get("metadata"));
    }

    public Optional<JsonNode> getProtocolTagMetadata() {
        return Optional.ofNullable(jsonNode.get("protocolTagMetadata"));
    }

    public Optional<JsonNode> getProtocolDeviceMetadata() {
        return Optional.ofNullable(jsonNode.get("protocolDeviceMetadata"));
    }

    public Optional<JsonNode> getAdapterDatapointMetadata() {
        return Optional.ofNullable(jsonNode.get("adapterDatapointMetadata"));
    }

    public Optional<JsonNode> getAdapterTagMetadata() {
        return Optional.ofNullable(jsonNode.get("adapterTagMetadata"));
    }

    public Optional<JsonNode> getAdapterDeviceMetadata() {
        return Optional.ofNullable(jsonNode.get("adapterDeviceMetadata"));
    }

    public @NotNull JsonNode getJsonNode() {
        return jsonNode;
    }

    // --- Inner builder classes ---

    public static final class DataPointBuilderImpl<R> implements DataPointBuilder<R> {
        private final @NotNull String tagName;
        private final @NotNull Function<DataPointBuilder<R>, R> completer;
        private final long timestamp;
        private JsonNode value;
        private JsonNode metadata;
        private JsonNode protocolTagMetadata;
        private JsonNode protocolDeviceMetadata;
        private JsonNode adapterDatapointMetadata;
        private JsonNode adapterTagMetadata;
        private JsonNode adapterDeviceMetadata;

        private DataPointBuilderImpl(final @NotNull String tagName, final long timestamp, final @NotNull Function<DataPointBuilder<R>, R> completer) {
            this.tagName = tagName;
            this.timestamp = timestamp;
            this.completer = completer;
        }

        @Override
        public @NotNull DataPointBuilder<R> setValue(final @NotNull String value) {
            this.value = JsonNodeFactory.instance.textNode(value);
            return this;
        }

        @Override
        public @NotNull DataPointBuilder<R> setValue(final int value) {
            this.value = JsonNodeFactory.instance.numberNode(value);
            return this;
        }

        @Override
        public @NotNull DataPointBuilder<R> setValue(final long value) {
            this.value = JsonNodeFactory.instance.numberNode(value);
            return this;
        }

        @Override
        public @NotNull DataPointBuilder<R> setValue(final double value) {
            this.value = JsonNodeFactory.instance.numberNode(value);
            return this;
        }

        @Override
        public @NotNull DataPointBuilder<R> setValue(final float value) {
            this.value = JsonNodeFactory.instance.numberNode(value);
            return this;
        }

        @Override
        public @NotNull DataPointBuilder<R> setValue(final boolean value) {
            this.value = JsonNodeFactory.instance.booleanNode(value);
            return this;
        }

        @Override
        public @NotNull DataPointBuilder<R> setValue(final short value) {
            this.value = JsonNodeFactory.instance.numberNode(value);
            return this;
        }

        @Override
        public @NotNull DataPointBuilder<R> setValue(final @NotNull BigDecimal value) {
            this.value = JsonNodeFactory.instance.numberNode(value);
            return this;
        }

        @Override
        public @NotNull DataPointBuilder<R> setValue(final @NotNull BigInteger value) {
            this.value = JsonNodeFactory.instance.numberNode(value);
            return this;
        }

        @Override
        public @NotNull DataPointBuilder<R> setValue(final byte @NotNull [] value) {
            this.value = JsonNodeFactory.instance.binaryNode(value);
            return this;
        }

        @Override
        public @NotNull DataPointBuilder<R> setNullValue() {
            this.value = JsonNodeFactory.instance.nullNode();
            return this;
        }

        @Override
        public @NotNull ObjectBuilder<DataPointBuilder<R>> valueStart() {
            return new ObjectBuilderImpl<>(node -> {
                this.value = node;
                return this;
            });
        }

        @Override
        public @NotNull ObjectBuilder<DataPointBuilder<R>> metadataStart() {
            return new ObjectBuilderImpl<>(node -> {
                this.metadata = node;
                return this;
            });
        }

        @Override
        public @NotNull ObjectBuilder<DataPointBuilder<R>> protocolTagMetadataStart() {
            return new ObjectBuilderImpl<>(node -> {
                this.protocolTagMetadata = node;
                return this;
            });
        }

        @Override
        public @NotNull ObjectBuilder<DataPointBuilder<R>> protocolDeviceMetadataStart() {
            return new ObjectBuilderImpl<>(node -> {
                this.protocolDeviceMetadata = node;
                return this;
            });
        }

        @Override
        public @NotNull ObjectBuilder<DataPointBuilder<R>> adapterDatapointMetadataStart() {
            return new ObjectBuilderImpl<>(node -> {
                this.adapterDatapointMetadata = node;
                return this;
            });
        }

        @Override
        public @NotNull ObjectBuilder<DataPointBuilder<R>> adapterTagMetadataStart() {
            return new ObjectBuilderImpl<>(node -> {
                this.adapterTagMetadata = node;
                return this;
            });
        }

        public @NotNull ObjectBuilder<DataPointBuilder<R>> adapterDeviceMetadataStart() {
            return new ObjectBuilderImpl<>(node -> {
                this.adapterDeviceMetadata = node;
                return this;
            });
        }

        @Override
        public R finish() {
            return completer.apply(this);
        }

        @NotNull DataPoint build() {
            final ObjectNode root = JsonNodeFactory.instance.objectNode();
            root.put("tagName", tagName);
            root.put("timestamp", timestamp);
            if (value != null) {
                root.set("value", value);
            }
            if (metadata != null) {
                root.set("metadata", metadata);
            }
            if (protocolTagMetadata != null) {
                root.set("protocolTagMetadata", protocolTagMetadata);
            }
            if (protocolDeviceMetadata != null) {
                root.set("protocolDeviceMetadata", protocolDeviceMetadata);
            }
            if (adapterDatapointMetadata != null) {
                root.set("adapterDatapointMetadata", adapterDatapointMetadata);
            }
            if (adapterTagMetadata != null) {
                root.set("adapterTagMetadata", adapterTagMetadata);
            }
            if (adapterDeviceMetadata != null) {
                root.set("adapterDeviceMetadata", adapterDeviceMetadata);
            }
            return new DataPointWithMetadata(root);
        }
    }

    public static final class ObjectBuilderImpl<P> implements DataPointBuilder.ObjectBuilder<P> {
        private final @NotNull ObjectNode node;
        private final @NotNull Function<ObjectNode, P> completer;

        ObjectBuilderImpl(final @NotNull Function<ObjectNode, P> completer) {
            this.node = JsonNodeFactory.instance.objectNode();
            this.completer = completer;
        }

        @Override
        public @NotNull DataPointBuilder.ObjectBuilder<P> add(final @NotNull String key, final @NotNull String value) {
            node.put(key, value);
            return this;
        }

        @Override
        public @NotNull DataPointBuilder.ObjectBuilder<P> add(final @NotNull String key, final int value) {
            node.put(key, value);
            return this;
        }

        @Override
        public @NotNull DataPointBuilder.ObjectBuilder<P> add(final @NotNull String key, final long value) {
            node.put(key, value);
            return this;
        }

        @Override
        public @NotNull DataPointBuilder.ObjectBuilder<P> add(final @NotNull String key, final double value) {
            node.put(key, value);
            return this;
        }

        @Override
        public @NotNull DataPointBuilder.ObjectBuilder<P> add(final @NotNull String key, final float value) {
            node.put(key, value);
            return this;
        }

        @Override
        public @NotNull DataPointBuilder.ObjectBuilder<P> add(final @NotNull String key, final boolean value) {
            node.put(key, value);
            return this;
        }

        @Override
        public @NotNull DataPointBuilder.ObjectBuilder<P> add(final @NotNull String key, final @NotNull BigDecimal value) {
            node.put(key, value);
            return this;
        }

        @Override
        public @NotNull DataPointBuilder.ObjectBuilder<P> add(final @NotNull String key, final @NotNull BigInteger value) {
            node.put(key, value);
            return this;
        }

        @Override
        public @NotNull DataPointBuilder.ObjectBuilder<P> add(final @NotNull String key, final byte @NotNull [] value) {
            node.put(key, value);
            return this;
        }

        @Override
        public @NotNull DataPointBuilder.ObjectBuilder<P> add(final @NotNull String key, final short value) {
            node.put(key, value);
            return this;
        }

        @Override
        public @NotNull DataPointBuilder.ObjectBuilder<P> addNull(final @NotNull String key) {
            node.putNull(key);
            return this;
        }

        @Override
        public @NotNull DataPointBuilder.ObjectBuilder<DataPointBuilder.ObjectBuilder<P>> objectStart(final @NotNull String key) {
            return new ObjectBuilderImpl<>(child -> {
                node.set(key, child);
                return this;
            });
        }

        @Override
        public @NotNull DataPointBuilder.ArrayBuilder<DataPointBuilder.ObjectBuilder<P>> arrayStart(final @NotNull String key) {
            return new ArrayBuilderImpl<>(child -> {
                node.set(key, child);
                return this;
            });
        }

        /**
         * Alias used when this builder was created via {@code valueStart()}, {@code metadataStart()}, etc.
         */
        @Override
        public @NotNull P valueStop() {
            return completer.apply(node);
        }

        /**
         * Alias used when this builder was created via {@code metadataStart()}.
         */
        @Override
        public @NotNull P metadataStop() {
            return completer.apply(node);
        }

        @Override
        public @NotNull P protocolTagMetadataStop() {
            return completer.apply(node);
        }

        @Override
        public @NotNull P protocolDeviceMetadataStop() {
            return completer.apply(node);
        }

        @Override
        public @NotNull P adapterDatapointMetadataStop() {
            return completer.apply(node);
        }

        @Override
        public @NotNull P adapterTagMetadataStop() {
            return completer.apply(node);
        }

        @Override
        public @NotNull P adapterDeviceMetadataStop() {
            return completer.apply(node);
        }

        /**
         * Completes this nested object and returns to the parent builder.
         */
        @Override
        public @NotNull P objectEnd() {
            return completer.apply(node);
        }
    }

    public static final class ArrayBuilderImpl<P> implements DataPointBuilder.ArrayBuilder<P> {
        private final @NotNull ArrayNode node;
        private final @NotNull Function<ArrayNode, P> completer;

        ArrayBuilderImpl(final @NotNull Function<ArrayNode, P> completer) {
            this.node = JsonNodeFactory.instance.arrayNode();
            this.completer = completer;
        }

        @Override
        public @NotNull DataPointBuilder.ArrayBuilder<P> add(final @NotNull String value) {
            node.add(value);
            return this;
        }

        @Override
        public @NotNull DataPointBuilder.ArrayBuilder<P> add(final int value) {
            node.add(value);
            return this;
        }

        @Override
        public @NotNull DataPointBuilder.ArrayBuilder<P> add(final long value) {
            node.add(value);
            return this;
        }

        @Override
        public @NotNull DataPointBuilder.ArrayBuilder<P> add(final double value) {
            node.add(value);
            return this;
        }

        @Override
        public @NotNull DataPointBuilder.ArrayBuilder<P> add(final float value) {
            node.add(value);
            return this;
        }

        @Override
        public @NotNull DataPointBuilder.ArrayBuilder<P> add(final boolean value) {
            node.add(value);
            return this;
        }

        @Override
        public @NotNull DataPointBuilder.ArrayBuilder<P> add(final @NotNull BigDecimal value) {
            node.add(value);
            return this;
        }

        @Override
        public @NotNull DataPointBuilder.ArrayBuilder<P> add(final @NotNull BigInteger value) {
            node.add(value);
            return this;
        }

        @Override
        public @NotNull DataPointBuilder.ArrayBuilder<P> add(final byte @NotNull [] value) {
            node.add(value);
            return this;
        }

        @Override
        public @NotNull DataPointBuilder.ArrayBuilder<P> add(final short value) {
            node.add(value);
            return this;
        }

        @Override
        public @NotNull DataPointBuilder.ArrayBuilder<P> addNull() {
            node.addNull();
            return this;
        }

        @Override
        public @NotNull DataPointBuilder.ObjectBuilder<DataPointBuilder.ArrayBuilder<P>> objectStart() {
            return new ObjectBuilderImpl<>(child -> {
                node.add(child);
                return this;
            });
        }

        /**
         * Completes this array and returns to the parent builder.
         */
        @Override
        public @NotNull P arrayEnd() {
            return completer.apply(node);
        }
    }
}
