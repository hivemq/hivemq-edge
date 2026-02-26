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
package com.hivemq.combining.model;

import static org.assertj.core.api.Assertions.assertThat;

import com.hivemq.configuration.entity.combining.DataIdentifierReferenceEntity;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class DataIdentifierReferenceTest {

    // --- Constructor tests ---

    @Test
    void twoArgConstructor_scopeIsNull() {
        final DataIdentifierReference ref = new DataIdentifierReference("tag1", DataIdentifierReference.Type.TAG);
        assertThat(ref.id()).isEqualTo("tag1");
        assertThat(ref.type()).isEqualTo(DataIdentifierReference.Type.TAG);
        assertThat(ref.scope()).isNull();
    }

    @Test
    void threeArgConstructor_scopeIsSet() {
        final DataIdentifierReference ref =
                new DataIdentifierReference("tag1", DataIdentifierReference.Type.TAG, "adapter-1");
        assertThat(ref.id()).isEqualTo("tag1");
        assertThat(ref.type()).isEqualTo(DataIdentifierReference.Type.TAG);
        assertThat(ref.scope()).isEqualTo("adapter-1");
    }

    @Test
    void threeArgConstructor_scopeExplicitlyNull() {
        final DataIdentifierReference ref =
                new DataIdentifierReference("filter/+", DataIdentifierReference.Type.TOPIC_FILTER, null);
        assertThat(ref.scope()).isNull();
    }

    // --- isScopeValid tests ---

    @Test
    void isScopeValid_tagWithScope_true() {
        final DataIdentifierReference ref =
                new DataIdentifierReference("tag1", DataIdentifierReference.Type.TAG, "adapter-1");
        assertThat(ref.isScopeValid()).isTrue();
    }

    @Test
    void isScopeValid_tagWithNullScope_false() {
        final DataIdentifierReference ref = new DataIdentifierReference("tag1", DataIdentifierReference.Type.TAG, null);
        assertThat(ref.isScopeValid()).isFalse();
    }

    @Test
    void isScopeValid_tagWithEmptyScope_false() {
        final DataIdentifierReference ref = new DataIdentifierReference("tag1", DataIdentifierReference.Type.TAG, "");
        assertThat(ref.isScopeValid()).isFalse();
    }

    @Test
    void isScopeValid_tagWithBlankScope_false() {
        final DataIdentifierReference ref =
                new DataIdentifierReference("tag1", DataIdentifierReference.Type.TAG, "   ");
        assertThat(ref.isScopeValid()).isFalse();
    }

    @Test
    void isScopeValid_topicFilterWithNullScope_true() {
        final DataIdentifierReference ref =
                new DataIdentifierReference("filter/+", DataIdentifierReference.Type.TOPIC_FILTER, null);
        assertThat(ref.isScopeValid()).isTrue();
    }

    @Test
    void isScopeValid_topicFilterWithScope_false() {
        final DataIdentifierReference ref =
                new DataIdentifierReference("filter/+", DataIdentifierReference.Type.TOPIC_FILTER, "adapter-1");
        assertThat(ref.isScopeValid()).isFalse();
    }

    @Test
    void isScopeValid_pulseAssetWithNullScope_true() {
        final DataIdentifierReference ref =
                new DataIdentifierReference("asset-1", DataIdentifierReference.Type.PULSE_ASSET, null);
        assertThat(ref.isScopeValid()).isTrue();
    }

    @Test
    void isScopeValid_pulseAssetWithScope_false() {
        final DataIdentifierReference ref =
                new DataIdentifierReference("asset-1", DataIdentifierReference.Type.PULSE_ASSET, "adapter-1");
        assertThat(ref.isScopeValid()).isFalse();
    }

    @ParameterizedTest
    @EnumSource(
            value = DataIdentifierReference.Type.class,
            names = {"TOPIC_FILTER", "PULSE_ASSET"})
    void isScopeValid_nonTagTypesWithNullScope_true(final @NotNull DataIdentifierReference.Type type) {
        final DataIdentifierReference ref = new DataIdentifierReference("id", type, null);
        assertThat(ref.isScopeValid()).isTrue();
    }

    @ParameterizedTest
    @EnumSource(
            value = DataIdentifierReference.Type.class,
            names = {"TOPIC_FILTER", "PULSE_ASSET"})
    void isScopeValid_nonTagTypesWithScope_false(final @NotNull DataIdentifierReference.Type type) {
        final DataIdentifierReference ref = new DataIdentifierReference("id", type, "some-adapter");
        assertThat(ref.isScopeValid()).isFalse();
    }

    // --- isIdEmpty tests ---

    @Test
    void isIdEmpty_nullId_true() {
        final DataIdentifierReference ref =
                new DataIdentifierReference(null, DataIdentifierReference.Type.TAG, "adapter-1");
        assertThat(ref.isIdEmpty()).isTrue();
    }

    @Test
    void isIdEmpty_emptyId_true() {
        final DataIdentifierReference ref =
                new DataIdentifierReference("", DataIdentifierReference.Type.TAG, "adapter-1");
        assertThat(ref.isIdEmpty()).isTrue();
    }

    @Test
    void isIdEmpty_blankId_true() {
        final DataIdentifierReference ref =
                new DataIdentifierReference("   ", DataIdentifierReference.Type.TAG, "adapter-1");
        assertThat(ref.isIdEmpty()).isTrue();
    }

    @Test
    void isIdEmpty_nonBlankId_false() {
        final DataIdentifierReference ref =
                new DataIdentifierReference("tag1", DataIdentifierReference.Type.TAG, "adapter-1");
        assertThat(ref.isIdEmpty()).isFalse();
    }

    // --- equals / hashCode tests ---

    @Test
    void equals_sameFieldsIncludingScope() {
        final DataIdentifierReference a =
                new DataIdentifierReference("tag1", DataIdentifierReference.Type.TAG, "adapter-1");
        final DataIdentifierReference b =
                new DataIdentifierReference("tag1", DataIdentifierReference.Type.TAG, "adapter-1");
        assertThat(a).isEqualTo(b);
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
    }

    @Test
    void equals_differentScope_notEqual() {
        final DataIdentifierReference a =
                new DataIdentifierReference("tag1", DataIdentifierReference.Type.TAG, "adapter-1");
        final DataIdentifierReference b =
                new DataIdentifierReference("tag1", DataIdentifierReference.Type.TAG, "adapter-2");
        assertThat(a).isNotEqualTo(b);
    }

    @Test
    void equals_nullScopeVsNonNullScope_notEqual() {
        final DataIdentifierReference a = new DataIdentifierReference("tag1", DataIdentifierReference.Type.TAG, null);
        final DataIdentifierReference b =
                new DataIdentifierReference("tag1", DataIdentifierReference.Type.TAG, "adapter-1");
        assertThat(a).isNotEqualTo(b);
    }

    @Test
    void equals_bothNullScope_equal() {
        final DataIdentifierReference a =
                new DataIdentifierReference("filter/+", DataIdentifierReference.Type.TOPIC_FILTER, null);
        final DataIdentifierReference b =
                new DataIdentifierReference("filter/+", DataIdentifierReference.Type.TOPIC_FILTER, null);
        assertThat(a).isEqualTo(b);
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
    }

    @Test
    void equals_twoArgConstructorVsThreeArgNullScope_equal() {
        final DataIdentifierReference a =
                new DataIdentifierReference("filter/+", DataIdentifierReference.Type.TOPIC_FILTER);
        final DataIdentifierReference b =
                new DataIdentifierReference("filter/+", DataIdentifierReference.Type.TOPIC_FILTER, null);
        assertThat(a).isEqualTo(b);
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
    }

    // --- Persistence round-trip tests ---

    @Test
    void persistenceRoundTrip_tagWithScope() {
        final DataIdentifierReference original =
                new DataIdentifierReference("tag1", DataIdentifierReference.Type.TAG, "adapter-1");
        final DataIdentifierReferenceEntity entity = original.toPersistence();

        assertThat(entity.getId()).isEqualTo("tag1");
        assertThat(entity.getType()).isEqualTo(DataIdentifierReference.Type.TAG);
        assertThat(entity.getScope()).isEqualTo("adapter-1");

        final DataIdentifierReference restored = DataIdentifierReference.fromPersistence(entity);
        assertThat(restored).isEqualTo(original);
    }

    @Test
    void persistenceRoundTrip_topicFilterWithNullScope() {
        final DataIdentifierReference original =
                new DataIdentifierReference("filter/+", DataIdentifierReference.Type.TOPIC_FILTER, null);
        final DataIdentifierReferenceEntity entity = original.toPersistence();

        assertThat(entity.getId()).isEqualTo("filter/+");
        assertThat(entity.getType()).isEqualTo(DataIdentifierReference.Type.TOPIC_FILTER);
        assertThat(entity.getScope()).isNull();

        final DataIdentifierReference restored = DataIdentifierReference.fromPersistence(entity);
        assertThat(restored).isEqualTo(original);
    }

    @Test
    void persistenceRoundTrip_pulseAssetWithNullScope() {
        final DataIdentifierReference original =
                new DataIdentifierReference("asset-1", DataIdentifierReference.Type.PULSE_ASSET, null);
        final DataIdentifierReferenceEntity entity = original.toPersistence();

        assertThat(entity.getScope()).isNull();

        final DataIdentifierReference restored = DataIdentifierReference.fromPersistence(entity);
        assertThat(restored).isEqualTo(original);
    }

    @Test
    void persistenceRoundTrip_twoArgConstructor_scopeRemainsNull() {
        final DataIdentifierReference original = new DataIdentifierReference("tag1", DataIdentifierReference.Type.TAG);
        final DataIdentifierReferenceEntity entity = original.toPersistence();

        assertThat(entity.getScope()).isNull();

        final DataIdentifierReference restored = DataIdentifierReference.fromPersistence(entity);
        assertThat(restored).isEqualTo(original);
        assertThat(restored.scope()).isNull();
    }

    // --- API model round-trip tests ---

    @Test
    void apiRoundTrip_tagWithScope() {
        final DataIdentifierReference original =
                new DataIdentifierReference("tag1", DataIdentifierReference.Type.TAG, "adapter-1");
        final com.hivemq.edge.api.model.DataIdentifierReference apiModel = original.to();

        assertThat(apiModel.getId()).isEqualTo("tag1");
        assertThat(apiModel.getType()).isEqualTo(com.hivemq.edge.api.model.DataIdentifierReference.TypeEnum.TAG);
        assertThat(apiModel.getScope()).isEqualTo("adapter-1");

        final DataIdentifierReference restored = DataIdentifierReference.from(apiModel);
        assertThat(restored).isEqualTo(original);
    }

    @Test
    void apiRoundTrip_topicFilterWithNullScope() {
        final DataIdentifierReference original =
                new DataIdentifierReference("filter/+", DataIdentifierReference.Type.TOPIC_FILTER, null);
        final com.hivemq.edge.api.model.DataIdentifierReference apiModel = original.to();

        assertThat(apiModel.getScope()).isNull();

        final DataIdentifierReference restored = DataIdentifierReference.from(apiModel);
        assertThat(restored).isEqualTo(original);
    }

    @Test
    void apiRoundTrip_pulseAssetWithNullScope() {
        final DataIdentifierReference original =
                new DataIdentifierReference("asset-1", DataIdentifierReference.Type.PULSE_ASSET, null);
        final com.hivemq.edge.api.model.DataIdentifierReference apiModel = original.to();

        assertThat(apiModel.getScope()).isNull();

        final DataIdentifierReference restored = DataIdentifierReference.from(apiModel);
        assertThat(restored).isEqualTo(original);
    }

    @Test
    void from_nullApiModel_returnsNull() {
        assertThat(DataIdentifierReference.from(null)).isNull();
    }

    // --- Type enum conversion tests ---

    @Test
    void typeFrom_tag() {
        assertThat(DataIdentifierReference.Type.from(com.hivemq.edge.api.model.DataIdentifierReference.TypeEnum.TAG))
                .isEqualTo(DataIdentifierReference.Type.TAG);
    }

    @Test
    void typeFrom_topicFilter() {
        assertThat(DataIdentifierReference.Type.from(
                        com.hivemq.edge.api.model.DataIdentifierReference.TypeEnum.TOPIC_FILTER))
                .isEqualTo(DataIdentifierReference.Type.TOPIC_FILTER);
    }

    @Test
    void typeFrom_pulseAsset() {
        assertThat(DataIdentifierReference.Type.from(
                        com.hivemq.edge.api.model.DataIdentifierReference.TypeEnum.PULSE_ASSET))
                .isEqualTo(DataIdentifierReference.Type.PULSE_ASSET);
    }

    @Test
    void typeTo_tag() {
        assertThat(DataIdentifierReference.Type.TAG.to())
                .isEqualTo(com.hivemq.edge.api.model.DataIdentifierReference.TypeEnum.TAG);
    }

    @Test
    void typeTo_topicFilter() {
        assertThat(DataIdentifierReference.Type.TOPIC_FILTER.to())
                .isEqualTo(com.hivemq.edge.api.model.DataIdentifierReference.TypeEnum.TOPIC_FILTER);
    }

    @Test
    void typeTo_pulseAsset() {
        assertThat(DataIdentifierReference.Type.PULSE_ASSET.to())
                .isEqualTo(com.hivemq.edge.api.model.DataIdentifierReference.TypeEnum.PULSE_ASSET);
    }

    @ParameterizedTest
    @EnumSource(DataIdentifierReference.Type.class)
    void typeRoundTrip_allValues(final DataIdentifierReference.Type type) {
        assertThat(DataIdentifierReference.Type.from(type.to())).isEqualTo(type);
    }

    // --- toFullyQualifiedName tests ---

    @Test
    void toFullyQualifiedName_topicFilterWithoutScope() {
        final DataIdentifierReference ref =
                new DataIdentifierReference("topic/a", DataIdentifierReference.Type.TOPIC_FILTER);
        assertThat(ref.toFullyQualifiedName()).isEqualTo("TOPIC_FILTER:topic/a");
    }

    @Test
    void toFullyQualifiedName_topicFilterWithDots_dotsReplacedWithSlashes() {
        final DataIdentifierReference ref =
                new DataIdentifierReference("topic.a.b", DataIdentifierReference.Type.TOPIC_FILTER);
        assertThat(ref.toFullyQualifiedName()).isEqualTo("TOPIC_FILTER:topic/a/b");
    }

    @Test
    void toFullyQualifiedName_tagWithoutScope() {
        final DataIdentifierReference ref =
                new DataIdentifierReference("temperature", DataIdentifierReference.Type.TAG);
        assertThat(ref.toFullyQualifiedName()).isEqualTo("TAG:temperature");
    }

    @Test
    void toFullyQualifiedName_tagWithScope() {
        final DataIdentifierReference ref =
                new DataIdentifierReference("temperature", DataIdentifierReference.Type.TAG, "adapter1");
        assertThat(ref.toFullyQualifiedName()).isEqualTo("adapter1/TAG:temperature");
    }

    @Test
    void toFullyQualifiedName_tagWithScopeAndDots() {
        final DataIdentifierReference ref =
                new DataIdentifierReference("sensor.temperature", DataIdentifierReference.Type.TAG, "my-adapter");
        assertThat(ref.toFullyQualifiedName()).isEqualTo("my-adapter/TAG:sensor/temperature");
    }

    @Test
    void toFullyQualifiedName_pulseAssetWithoutScope() {
        final DataIdentifierReference ref =
                new DataIdentifierReference("asset-123", DataIdentifierReference.Type.PULSE_ASSET);
        assertThat(ref.toFullyQualifiedName()).isEqualTo("PULSE_ASSET:asset-123");
    }

    @Test
    void toFullyQualifiedName_twoTagsSameNameDifferentScope_differentKeys() {
        final DataIdentifierReference ref1 =
                new DataIdentifierReference("temperature", DataIdentifierReference.Type.TAG, "adapter1");
        final DataIdentifierReference ref2 =
                new DataIdentifierReference("temperature", DataIdentifierReference.Type.TAG, "adapter2");

        assertThat(ref1.toFullyQualifiedName()).isEqualTo("adapter1/TAG:temperature");
        assertThat(ref2.toFullyQualifiedName()).isEqualTo("adapter2/TAG:temperature");
        assertThat(ref1.toFullyQualifiedName()).isNotEqualTo(ref2.toFullyQualifiedName());
    }
}
