package com.hivemq.datapoint;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.hivemq.adapter.sdk.api.data.DataPoint;
import com.hivemq.adapter.sdk.api.datapoint.DataPointBuilder;
import com.hivemq.adapter.sdk.api.tag.Tag;
import org.jetbrains.annotations.NotNull;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Optional;
import java.util.function.Function;

public class DataPointWithMetadata implements DataPoint {
    private final @NotNull ObjectNode jsonNode;

    public DataPointWithMetadata(final @NotNull ObjectNode jsonNode) {
        this.jsonNode = jsonNode;
    }

    public static <R> @NotNull DataPointBuilder<R> builder(
            final @NotNull Tag tag,
            final long timestamp,
            final @NotNull Function<DataPointBuilder<R>, R> completer) {
        final ObjectNode root = JsonNodeFactory.instance.objectNode();
        root.put("tagName", tag.getName());
        root.put("timestamp", timestamp);
        return new DataPointBuilderImpl<>(root, completer);
    }

    @Override
    public @NotNull JsonNode getTagValue() {
        return jsonNode.get("value");
    }

    public Long getTimestamp() {
        return jsonNode.get("timestamp").asLong();
    }

    @Override
    public @NotNull String getTagName() {
        return jsonNode.get("tagName").asText();
    }

    public Optional<JsonNode> getMetadata() {
        return Optional.ofNullable(jsonNode.get("metadata"));
    }

    public Optional<JsonNode> getContext() {
        return Optional.ofNullable(jsonNode.get("context"));
    }

    public @NotNull ObjectNode getJsonNode() {
        return jsonNode;
    }

    // --- Inner builder classes (shadow-node pattern) ---

    public static final class DataPointBuilderImpl<R> implements DataPointBuilder<R> {
        private final @NotNull ObjectNode root;
        private final @NotNull Function<DataPointBuilder<R>, R> completer;

        DataPointBuilderImpl(
                final @NotNull ObjectNode root,
                final @NotNull Function<DataPointBuilder<R>, R> completer) {
            this.root = root;
            this.completer = completer;
        }

        @Override
        public @NotNull DataPointBuilder<R> value(final boolean value) {
            root.put("value", value);
            return this;
        }

        @Override
        public @NotNull DataPointBuilder<R> value(final byte value) {
            root.put("value", value);
            return this;
        }

        @Override
        public @NotNull DataPointBuilder<R> value(final short value) {
            root.put("value", value);
            return this;
        }

        @Override
        public @NotNull DataPointBuilder<R> value(final int value) {
            root.put("value", value);
            return this;
        }

        @Override
        public @NotNull DataPointBuilder<R> value(final long value) {
            root.put("value", value);
            return this;
        }

        @Override
        public @NotNull DataPointBuilder<R> value(final float value) {
            root.put("value", value);
            return this;
        }

        @Override
        public @NotNull DataPointBuilder<R> value(final double value) {
            root.put("value", value);
            return this;
        }

        @Override
        public @NotNull DataPointBuilder<R> value(final @NotNull String value) {
            root.put("value", value);
            return this;
        }

        @Override
        public @NotNull DataPointBuilder<R> value(final byte @NotNull [] value) {
            root.put("value", value);
            return this;
        }

        @Override
        public @NotNull DataPointBuilder<R> value(final @NotNull BigDecimal value) {
            root.put("value", value);
            return this;
        }

        @Override
        public @NotNull DataPointBuilder<R> value(final @NotNull BigInteger value) {
            root.set("value", JsonNodeFactory.instance.numberNode(value));
            return this;
        }

        @Override
        public @NotNull DataPointBuilder<R> value(final @NotNull JsonNode value) {
            root.set("value", value);
            return this;
        }

        @Override
        public @NotNull DataPointBuilder<R> valueNull() {
            root.putNull("value");
            return this;
        }

        @Override
        public @NotNull ObjectBuilder<DataPointBuilder<R>> startObjectValue() {
            final ObjectNode child = JsonNodeFactory.instance.objectNode();
            root.set("value", child);
            return new ObjectBuilderImpl<>(this, child);
        }

        @Override
        public @NotNull ArrayBuilder<DataPointBuilder<R>> startArrayValue() {
            final ArrayNode child = JsonNodeFactory.instance.arrayNode();
            root.set("value", child);
            return new ArrayBuilderImpl<>(this, child);
        }

        @Override
        public @NotNull ObjectBuilder<DataPointBuilder<R>> startObjectMetadata() {
            final ObjectNode child = JsonNodeFactory.instance.objectNode();
            root.set("metadata", child);
            return new ObjectBuilderImpl<>(this, child);
        }

        @Override
        public @NotNull ObjectBuilder<DataPointBuilder<R>> startObjectContext() {
            final ObjectNode child = JsonNodeFactory.instance.objectNode();
            root.set("context", child);
            return new ObjectBuilderImpl<>(this, child);
        }

        @Override
        public R endDataPoint() {
            return completer.apply(this);
        }

        public @NotNull DataPointWithMetadata build() {
            return new DataPointWithMetadata(root);
        }
    }

    public static final class ObjectBuilderImpl<P> implements DataPointBuilder.ObjectBuilder<P> {
        private final @NotNull P parent;
        private final @NotNull ObjectNode node;

        ObjectBuilderImpl(final @NotNull P parent, final @NotNull ObjectNode node) {
            this.parent = parent;
            this.node = node;
        }

        @Override
        public @NotNull DataPointBuilder.ObjectBuilder<P> put(final @NotNull String key, final boolean value) {
            node.put(key, value);
            return this;
        }

        @Override
        public @NotNull DataPointBuilder.ObjectBuilder<P> put(final @NotNull String key, final byte value) {
            node.put(key, value);
            return this;
        }

        @Override
        public @NotNull DataPointBuilder.ObjectBuilder<P> put(final @NotNull String key, final short value) {
            node.put(key, value);
            return this;
        }

        @Override
        public @NotNull DataPointBuilder.ObjectBuilder<P> put(final @NotNull String key, final int value) {
            node.put(key, value);
            return this;
        }

        @Override
        public @NotNull DataPointBuilder.ObjectBuilder<P> put(final @NotNull String key, final long value) {
            node.put(key, value);
            return this;
        }

        @Override
        public @NotNull DataPointBuilder.ObjectBuilder<P> put(final @NotNull String key, final float value) {
            node.put(key, value);
            return this;
        }

        @Override
        public @NotNull DataPointBuilder.ObjectBuilder<P> put(final @NotNull String key, final double value) {
            node.put(key, value);
            return this;
        }

        @Override
        public @NotNull DataPointBuilder.ObjectBuilder<P> put(final @NotNull String key, final @NotNull String value) {
            node.put(key, value);
            return this;
        }

        @Override
        public @NotNull DataPointBuilder.ObjectBuilder<P> put(final @NotNull String key, final byte @NotNull [] value) {
            node.put(key, value);
            return this;
        }

        @Override
        public @NotNull DataPointBuilder.ObjectBuilder<P> put(final @NotNull String key, final @NotNull BigDecimal value) {
            node.put(key, value);
            return this;
        }

        @Override
        public @NotNull DataPointBuilder.ObjectBuilder<P> put(final @NotNull String key, final @NotNull BigInteger value) {
            node.set(key, JsonNodeFactory.instance.numberNode(value));
            return this;
        }

        @Override
        public @NotNull DataPointBuilder.ObjectBuilder<P> put(final @NotNull String key, final @NotNull JsonNode value) {
            node.set(key, value);
            return this;
        }

        @Override
        public @NotNull DataPointBuilder.ObjectBuilder<P> putNull(final @NotNull String key) {
            node.putNull(key);
            return this;
        }

        @Override
        public @NotNull DataPointBuilder.ObjectBuilder<DataPointBuilder.ObjectBuilder<P>> startObject(final @NotNull String key) {
            final ObjectNode child = JsonNodeFactory.instance.objectNode();
            node.set(key, child);
            return new ObjectBuilderImpl<>(this, child);
        }

        @Override
        public @NotNull DataPointBuilder.ArrayBuilder<DataPointBuilder.ObjectBuilder<P>> startArray(final @NotNull String key) {
            final ArrayNode child = JsonNodeFactory.instance.arrayNode();
            node.set(key, child);
            return new ArrayBuilderImpl<>(this, child);
        }

        @Override
        public @NotNull P endObject() {
            return parent;
        }
    }

    public static final class ArrayBuilderImpl<P> implements DataPointBuilder.ArrayBuilder<P> {
        private final @NotNull P parent;
        private final @NotNull ArrayNode node;

        ArrayBuilderImpl(final @NotNull P parent, final @NotNull ArrayNode node) {
            this.parent = parent;
            this.node = node;
        }

        @Override
        public @NotNull DataPointBuilder.ArrayBuilder<P> add(final boolean value) {
            node.add(value);
            return this;
        }

        @Override
        public @NotNull DataPointBuilder.ArrayBuilder<P> add(final byte value) {
            node.add(value);
            return this;
        }

        @Override
        public @NotNull DataPointBuilder.ArrayBuilder<P> add(final short value) {
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
        public @NotNull DataPointBuilder.ArrayBuilder<P> add(final float value) {
            node.add(value);
            return this;
        }

        @Override
        public @NotNull DataPointBuilder.ArrayBuilder<P> add(final double value) {
            node.add(value);
            return this;
        }

        @Override
        public @NotNull DataPointBuilder.ArrayBuilder<P> add(final @NotNull String value) {
            node.add(value);
            return this;
        }

        @Override
        public @NotNull DataPointBuilder.ArrayBuilder<P> add(final byte @NotNull [] value) {
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
            node.add(JsonNodeFactory.instance.numberNode(value));
            return this;
        }

        @Override
        public @NotNull DataPointBuilder.ArrayBuilder<P> add(final @NotNull JsonNode value) {
            node.add(value);
            return this;
        }

        @Override
        public @NotNull DataPointBuilder.ArrayBuilder<P> addNull() {
            node.addNull();
            return this;
        }

        @Override
        public @NotNull DataPointBuilder.ObjectBuilder<DataPointBuilder.ArrayBuilder<P>> startObject() {
            final ObjectNode child = JsonNodeFactory.instance.objectNode();
            node.add(child);
            return new ObjectBuilderImpl<>(this, child);
        }

        @Override
        public @NotNull DataPointBuilder.ArrayBuilder<DataPointBuilder.ArrayBuilder<P>> startArray() {
            final ArrayNode child = JsonNodeFactory.instance.arrayNode();
            node.add(child);
            return new ArrayBuilderImpl<>(this, child);
        }

        @Override
        public @NotNull P endArray() {
            return parent;
        }
    }

    @Override
    public String toString() {
        return "DataPointWithMetadata{" + "jsonNode=" + jsonNode + '}';
    }
}
