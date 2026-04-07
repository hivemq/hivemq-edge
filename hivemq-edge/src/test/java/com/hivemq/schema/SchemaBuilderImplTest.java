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
package com.hivemq.schema;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.hivemq.adapter.sdk.api.schema.AnySchema;
import com.hivemq.adapter.sdk.api.schema.ArraySchema;
import com.hivemq.adapter.sdk.api.schema.ObjectSchema;
import com.hivemq.adapter.sdk.api.schema.ScalarSchema;
import com.hivemq.adapter.sdk.api.schema.ScalarType;
import com.hivemq.adapter.sdk.api.schema.Schema;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class SchemaBuilderImplTest {

    // ── Any ──────────────────────────────────────────────────────────────────

    @Test
    void test_any_buildsAnySchema() {
        final Schema schema = new com.hivemq.adapter.sdk.api.schema.impl.SchemaBuilderImpl()
                .any()
                .build();

        assertThat(schema).isInstanceOf(AnySchema.class);
        assertThat(schema.nullable()).isFalse();
        assertThat(schema.readable()).isTrue();
        assertThat(schema.writable()).isFalse();
        assertThat(schema.title()).isNull();
        assertThat(schema.description()).isNull();
    }

    @Test
    void test_any_nullable() {
        final Schema schema = new com.hivemq.adapter.sdk.api.schema.impl.SchemaBuilderImpl()
                .any()
                .nullable()
                .build();

        assertThat(schema).isInstanceOf(AnySchema.class);
        assertThat(schema.nullable()).isTrue();
    }

    // ── Scalar ───────────────────────────────────────────────────────────────

    @Test
    void test_scalar_long() {
        final Schema schema = new com.hivemq.adapter.sdk.api.schema.impl.SchemaBuilderImpl()
                .scalar(ScalarType.LONG)
                .build();

        assertThat(schema).isInstanceOf(ScalarSchema.class);
        final var s = (ScalarSchema) schema;
        assertThat(s.type()).isEqualTo(ScalarType.LONG);
        assertThat(s.nullable()).isFalse();
    }

    @Test
    void test_scalar_allTypes() {
        for (final ScalarType type : ScalarType.values()) {
            final Schema schema = new com.hivemq.adapter.sdk.api.schema.impl.SchemaBuilderImpl()
                    .scalar(type)
                    .build();
            assertThat(schema).isInstanceOf(ScalarSchema.class);
            assertThat(((ScalarSchema) schema).type()).isEqualTo(type);
        }
    }

    @Test
    void test_scalar_withAnnotations() {
        final Schema schema = new com.hivemq.adapter.sdk.api.schema.impl.SchemaBuilderImpl()
                .scalar(ScalarType.LONG)
                .title("Motor Speed")
                .description("Rotational speed of the main motor shaft")
                .readable(true)
                .writable(false)
                .build();

        assertThat(schema).isInstanceOf(ScalarSchema.class);
        assertThat(schema.title()).isEqualTo("Motor Speed");
        assertThat(schema.description()).isEqualTo("Rotational speed of the main motor shaft");
        assertThat(schema.readable()).isTrue();
        assertThat(schema.writable()).isFalse();
    }

    @Test
    void test_scalar_nullable() {
        final Schema schema = new com.hivemq.adapter.sdk.api.schema.impl.SchemaBuilderImpl()
                .scalar(ScalarType.LONG)
                .nullable()
                .build();

        final var s = (ScalarSchema) schema;
        assertThat(s.nullable()).isTrue();
    }

    @Test
    void test_scalar_writeOnly() {
        final Schema schema = new com.hivemq.adapter.sdk.api.schema.impl.SchemaBuilderImpl()
                .scalar(ScalarType.LONG)
                .readable(false)
                .writable(true)
                .build();

        assertThat(schema.readable()).isFalse();
        assertThat(schema.writable()).isTrue();
    }

    // ── Object ───────────────────────────────────────────────────────────────

    @Test
    void test_object_simple() {
        final Schema schema = new com.hivemq.adapter.sdk.api.schema.impl.SchemaBuilderImpl()
                .startObject()
                .property("rpm")
                .required()
                .scalar(ScalarType.LONG)
                .title("Motor Speed")
                .property("label")
                .scalar(ScalarType.STRING)
                .nullable()
                .endObject()
                .build();

        assertThat(schema).isInstanceOf(ObjectSchema.class);
        final var o = (ObjectSchema) schema;
        assertThat(o.properties()).hasSize(2);
        assertThat(o.required()).containsExactly("rpm");
        assertThat(o.additionalProperties()).isTrue();

        final var rpm = (ScalarSchema) o.properties().get("rpm");
        assertThat(rpm.type()).isEqualTo(ScalarType.LONG);
        assertThat(rpm.title()).isEqualTo("Motor Speed");
        assertThat(rpm.nullable()).isFalse();

        final var label = (ScalarSchema) o.properties().get("label");
        assertThat(label.type()).isEqualTo(ScalarType.STRING);
        assertThat(label.nullable()).isTrue();
    }

    @Test
    void test_object_nestedObject() {
        final Schema schema = new com.hivemq.adapter.sdk.api.schema.impl.SchemaBuilderImpl()
                .startObject()
                .property("bearing")
                .startObject()
                .property("temperature")
                .required()
                .scalar(ScalarType.DOUBLE)
                .property("vibration")
                .required()
                .scalar(ScalarType.DOUBLE)
                .additionalProperties(false)
                .endObject()
                .endObject()
                .build();

        final var root = (ObjectSchema) schema;
        final var bearing = (ObjectSchema) root.properties().get("bearing");
        assertThat(bearing.properties()).hasSize(2);
        assertThat(bearing.required()).containsExactly("temperature", "vibration");
        assertThat(bearing.additionalProperties()).isFalse();
    }

    @Test
    void test_object_nullable() {
        final Schema schema = new com.hivemq.adapter.sdk.api.schema.impl.SchemaBuilderImpl()
                .startObject()
                .property("x")
                .scalar(ScalarType.LONG)
                .endObject()
                .nullable()
                .build();

        assertThat(schema).isInstanceOf(ObjectSchema.class);
        assertThat(schema.nullable()).isTrue();
    }

    @Test
    void test_object_annotationsOnObject() {
        final Schema schema = new com.hivemq.adapter.sdk.api.schema.impl.SchemaBuilderImpl()
                .startObject()
                .property("x")
                .scalar(ScalarType.LONG)
                .endObject()
                .title("Root")
                .description("The root object")
                .build();

        assertThat(schema.title()).isEqualTo("Root");
        assertThat(schema.description()).isEqualTo("The root object");
    }

    @Test
    void test_object_propertyAny() {
        final Schema schema = new com.hivemq.adapter.sdk.api.schema.impl.SchemaBuilderImpl()
                .startObject()
                .property("extra")
                .any()
                .endObject()
                .build();

        final var o = (ObjectSchema) schema;
        assertThat(o.properties().get("extra")).isInstanceOf(AnySchema.class);
    }

    // ── Array ────────────────────────────────────────────────────────────────

    @Test
    void test_array_uniformLong() {
        final Schema schema = new com.hivemq.adapter.sdk.api.schema.impl.SchemaBuilderImpl()
                .startArray()
                .scalar(ScalarType.LONG)
                .endArray()
                .build();

        assertThat(schema).isInstanceOf(ArraySchema.class);
        final var a = (ArraySchema) schema;
        assertThat(a.items()).isInstanceOf(ScalarSchema.class);
        assertThat(((ScalarSchema) a.items()).type()).isEqualTo(ScalarType.LONG);
        assertThat(a.minContains()).isNull();
        assertThat(a.maxContains()).isNull();
    }

    @Test
    void test_array_withBounds() {
        final Schema schema = new com.hivemq.adapter.sdk.api.schema.impl.SchemaBuilderImpl()
                .startArray()
                .scalar(ScalarType.STRING)
                .nullable()
                .minContains(1)
                .maxContains(20)
                .endArray()
                .build();

        final var a = (ArraySchema) schema;
        assertThat(a.minContains()).isEqualTo(1);
        assertThat(a.maxContains()).isEqualTo(20);

        final var items = (ScalarSchema) a.items();
        assertThat(items.type()).isEqualTo(ScalarType.STRING);
        assertThat(items.nullable()).isTrue();
    }

    @Test
    void test_array_ofObjects() {
        final Schema schema = new com.hivemq.adapter.sdk.api.schema.impl.SchemaBuilderImpl()
                .startArray()
                .startObject()
                .property("code")
                .required()
                .scalar(ScalarType.LONG)
                .property("message")
                .scalar(ScalarType.STRING)
                .endObject()
                .endArray()
                .build();

        final var a = (ArraySchema) schema;
        assertThat(a.items()).isInstanceOf(ObjectSchema.class);
        final var item = (ObjectSchema) a.items();
        assertThat(item.properties()).hasSize(2);
        assertThat(item.required()).containsExactly("code");
    }

    @Test
    void test_array_ofArrays() {
        final Schema schema = new com.hivemq.adapter.sdk.api.schema.impl.SchemaBuilderImpl()
                .startArray()
                .startArray()
                .scalar(ScalarType.DOUBLE)
                .endArray()
                .endArray()
                .build();

        final var outer = (ArraySchema) schema;
        assertThat(outer.items()).isInstanceOf(ArraySchema.class);
        final var inner = (ArraySchema) outer.items();
        assertThat(inner.items()).isInstanceOf(ScalarSchema.class);
        assertThat(((ScalarSchema) inner.items()).type()).isEqualTo(ScalarType.DOUBLE);
    }

    @Test
    void test_array_unconstrained() {
        final Schema schema = new com.hivemq.adapter.sdk.api.schema.impl.SchemaBuilderImpl()
                .startArray()
                .endArray()
                .build();

        final var a = (ArraySchema) schema;
        assertThat(a.items()).isInstanceOf(AnySchema.class);
    }

    @Test
    void test_array_nullable() {
        final Schema schema = new com.hivemq.adapter.sdk.api.schema.impl.SchemaBuilderImpl()
                .startArray()
                .scalar(ScalarType.LONG)
                .endArray()
                .nullable()
                .build();

        assertThat(schema.nullable()).isTrue();
    }

    @Test
    void test_array_itemAnnotationsVsArrayAnnotations() {
        final Schema schema = new com.hivemq.adapter.sdk.api.schema.impl.SchemaBuilderImpl()
                .startObject()
                .property("readings")
                .startArray()
                .scalar(ScalarType.DOUBLE)
                .title("Sensor reading")
                .nullable()
                .endArray()
                .title("Readings")
                .nullable()
                .endObject()
                .build();

        final var root = (ObjectSchema) schema;
        final var readings = (ArraySchema) root.properties().get("readings");
        assertThat(readings.title()).isEqualTo("Readings");
        assertThat(readings.nullable()).isTrue();

        final var items = (ScalarSchema) readings.items();
        assertThat(items.title()).isEqualTo("Sensor reading");
        assertThat(items.nullable()).isTrue();
    }

    // ── Composing from subschemas ────────────────────────────────────────────

    @Test
    void test_schema_composesFromSubschemas() {
        final Schema valueSchema = new com.hivemq.adapter.sdk.api.schema.impl.SchemaBuilderImpl()
                .scalar(ScalarType.DOUBLE)
                .build();

        final Schema metadataSchema = new com.hivemq.adapter.sdk.api.schema.impl.SchemaBuilderImpl()
                .startObject()
                .property("status")
                .scalar(ScalarType.STRING)
                .property("serverTimestamp")
                .scalar(ScalarType.LONG)
                .title("Server Timestamp")
                .readable(true)
                .writable(false)
                .endObject()
                .build();

        final Schema dpSchema = new com.hivemq.adapter.sdk.api.schema.impl.SchemaBuilderImpl()
                .startObject()
                .property("value")
                .schema(valueSchema)
                .property("metadata")
                .schema(metadataSchema)
                .endObject()
                .build();

        final var root = (ObjectSchema) dpSchema;
        assertThat(root.properties()).hasSize(2);

        // impl() reuses the same object
        assertThat(root.properties().get("value")).isSameAs(valueSchema);
        assertThat(root.properties().get("metadata")).isSameAs(metadataSchema);
    }

    // ── Local-variables style ────────────────────────────────────────────────

    @Test
    void test_localVariablesStyle() {
        final var rootBuilder = new com.hivemq.adapter.sdk.api.schema.impl.SchemaBuilderImpl();
        final var root = rootBuilder.startObject();
        root.property("rpm").required().scalar(ScalarType.LONG).title("Motor Speed");
        final var bearing = root.property("bearing").startObject();
        bearing.property("temperature").required().scalar(ScalarType.DOUBLE);
        bearing.property("vibration").required().scalar(ScalarType.DOUBLE);
        bearing.additionalProperties(false);

        final Schema schema = rootBuilder.build();

        final var o = (ObjectSchema) schema;
        assertThat(o.properties()).hasSize(2);
        assertThat(o.required()).containsExactly("rpm");

        final var b = (ObjectSchema) o.properties().get("bearing");
        assertThat(b.required()).containsExactly("temperature", "vibration");
        assertThat(b.additionalProperties()).isFalse();
    }

    // ── Annotations before structure ─────────────────────────────────────────

    @Test
    void test_annotationsBeforeStructure() {
        final Schema schema = new com.hivemq.adapter.sdk.api.schema.impl.SchemaBuilderImpl()
                .title("RPM")
                .description("desc")
                .nullable()
                .scalar(ScalarType.LONG)
                .build();

        assertThat(schema).isInstanceOf(ScalarSchema.class);
        assertThat(schema.title()).isEqualTo("RPM");
        assertThat(schema.description()).isEqualTo("desc");
        assertThat(schema.nullable()).isTrue();
    }

    // ── Error cases ──────────────────────────────────────────────────────────

    @Test
    void test_doubleStructure_throwsException() {
        final var builder = new com.hivemq.adapter.sdk.api.schema.impl.SchemaBuilderImpl();
        builder.scalar(ScalarType.LONG);

        assertThatThrownBy(() -> builder.scalar(ScalarType.STRING))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("structure already defined");
    }

    @Test
    void test_doubleStructure_scalarThenObject_throwsException() {
        final var builder = new com.hivemq.adapter.sdk.api.schema.impl.SchemaBuilderImpl();
        builder.scalar(ScalarType.LONG);

        assertThatThrownBy(builder::startObject).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void test_noStructure_throwsException() {
        final var builder = new com.hivemq.adapter.sdk.api.schema.impl.SchemaBuilderImpl();

        assertThatThrownBy(builder::build)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("no structure defined");
    }

    // ── Callback on build ────────────────────────────────────────────────────

    @Test
    void test_callback_calledOnBuild() {
        final AtomicReference<Schema> captured = new AtomicReference<>();
        final var builder = new com.hivemq.adapter.sdk.api.schema.impl.SchemaBuilderImpl(captured::set);

        final Schema schema = builder.scalar(ScalarType.LONG).build();

        assertThat(captured.get()).isSameAs(schema);
    }

    @Test
    void test_noCallback_noError() {
        final Schema schema = new com.hivemq.adapter.sdk.api.schema.impl.SchemaBuilderImpl()
                .scalar(ScalarType.LONG)
                .build();
        assertThat(schema).isNotNull();
    }

    // ── Build caching ────────────────────────────────────────────────────────

    @Test
    void test_build_returnsSameInstance() {
        final var builder = new com.hivemq.adapter.sdk.api.schema.impl.SchemaBuilderImpl();
        builder.scalar(ScalarType.LONG);

        final Schema first = builder.build();
        final Schema second = builder.build();

        assertThat(first).isSameAs(second);
    }

    // ── Complex document example from spec ───────────────────────────────────

    @Test
    void test_fullDocumentExample() {
        final Schema schema = new com.hivemq.adapter.sdk.api.schema.impl.SchemaBuilderImpl()
                .startObject()
                .property("rpm")
                .required()
                .scalar(ScalarType.LONG)
                .title("Motor Speed")
                .description("RPM")
                .readable(true)
                .writable(false)
                .property("label")
                .scalar(ScalarType.STRING)
                .nullable()
                .property("bearing")
                .startObject()
                .property("temperature")
                .required()
                .scalar(ScalarType.DOUBLE)
                .property("vibration")
                .required()
                .scalar(ScalarType.DOUBLE)
                .additionalProperties(false)
                .endObject()
                .property("extra")
                .any()
                .endObject()
                .build();

        final var root = (ObjectSchema) schema;
        assertThat(root.properties()).hasSize(4);
        assertThat(root.required()).containsExactly("rpm");

        final var rpm = (ScalarSchema) root.properties().get("rpm");
        assertThat(rpm.type()).isEqualTo(ScalarType.LONG);
        assertThat(rpm.title()).isEqualTo("Motor Speed");
        assertThat(rpm.description()).isEqualTo("RPM");
        assertThat(rpm.readable()).isTrue();
        assertThat(rpm.writable()).isFalse();

        final var label = (ScalarSchema) root.properties().get("label");
        assertThat(label.nullable()).isTrue();

        final var bearing = (ObjectSchema) root.properties().get("bearing");
        assertThat(bearing.required()).containsExactly("temperature", "vibration");
        assertThat(bearing.additionalProperties()).isFalse();

        assertThat(root.properties().get("extra")).isInstanceOf(AnySchema.class);
    }

    // ── Required toggle ──────────────────────────────────────────────────────

    @Test
    void test_required_canBeUnset() {
        final Schema schema = new com.hivemq.adapter.sdk.api.schema.impl.SchemaBuilderImpl()
                .startObject()
                .property("a")
                .required(true)
                .scalar(ScalarType.LONG)
                .property("b")
                .required(true)
                .required(false)
                .scalar(ScalarType.LONG)
                .endObject()
                .build();

        final var o = (ObjectSchema) schema;
        assertThat(o.required()).containsExactly("a");
    }

    // ── Nullable with boolean parameter ──────────────────────────────────────

    @Test
    void test_nullable_false_explicitlySet() {
        final Schema schema = new com.hivemq.adapter.sdk.api.schema.impl.SchemaBuilderImpl()
                .scalar(ScalarType.LONG)
                .nullable(false)
                .build();

        assertThat(schema.nullable()).isFalse();
    }

    // ── Minimum / Maximum ────────────────────────────────────────────────────

    @Test
    void test_scalar_withMinMax_long() {
        final Schema schema = new com.hivemq.adapter.sdk.api.schema.impl.SchemaBuilderImpl()
                .scalar(ScalarType.LONG)
                .minimum(-128)
                .maximum(127)
                .build();

        final var s = (ScalarSchema) schema;
        assertThat(s.minimum()).isEqualTo(-128L);
        assertThat(s.maximum()).isEqualTo(127L);
    }

    @Test
    void test_scalar_withMinMax_double() {
        final Schema schema = new com.hivemq.adapter.sdk.api.schema.impl.SchemaBuilderImpl()
                .scalar(ScalarType.DOUBLE)
                .minimum(-3.4e38)
                .maximum(3.4e38)
                .build();

        final var s = (ScalarSchema) schema;
        assertThat(s.minimum()).isEqualTo(-3.4e38);
        assertThat(s.maximum()).isEqualTo(3.4e38);
    }

    @Test
    void test_scalar_noMinMax_defaultsToNull() {
        final Schema schema = new com.hivemq.adapter.sdk.api.schema.impl.SchemaBuilderImpl()
                .scalar(ScalarType.LONG)
                .build();

        final var s = (ScalarSchema) schema;
        assertThat(s.minimum()).isNull();
        assertThat(s.maximum()).isNull();
    }

    @Test
    void test_scalar_minMaxOnProperty() {
        final Schema schema = new com.hivemq.adapter.sdk.api.schema.impl.SchemaBuilderImpl()
                .startObject()
                .property("temp")
                .scalar(ScalarType.LONG)
                .minimum(0)
                .maximum(4095)
                .endObject()
                .build();

        final var o = (ObjectSchema) schema;
        final var temp = (ScalarSchema) o.properties().get("temp");
        assertThat(temp.minimum()).isEqualTo(0L);
        assertThat(temp.maximum()).isEqualTo(4095L);
    }

    @Test
    void test_scalar_minMaxOnArrayItem() {
        final Schema schema = new com.hivemq.adapter.sdk.api.schema.impl.SchemaBuilderImpl()
                .startArray()
                .scalar(ScalarType.DOUBLE)
                .minimum(-100.0)
                .maximum(100.0)
                .endArray()
                .build();

        final var a = (ArraySchema) schema;
        final var items = (ScalarSchema) a.items();
        assertThat(items.minimum()).isEqualTo(-100.0);
        assertThat(items.maximum()).isEqualTo(100.0);
    }
}
