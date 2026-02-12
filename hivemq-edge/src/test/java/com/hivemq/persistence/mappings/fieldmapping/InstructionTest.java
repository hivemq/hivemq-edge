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
package com.hivemq.persistence.mappings.fieldmapping;

import com.hivemq.combining.model.DataIdentifierReference;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class InstructionTest {

    @Test
    public void toSourceJsonPath_whenTopicFilterWithoutScope_thenNoScopePrefix() {
        final Instruction instruction = new Instruction(
                "$.value",
                "dest.value",
                new DataIdentifierReference("topic/a", DataIdentifierReference.Type.TOPIC_FILTER));
        assertThat(instruction.toSourceJsonPath()).isEqualTo("$['TOPIC_FILTER:topic/a'].value");
    }

    @Test
    public void toSourceJsonPath_whenTagWithoutScope_thenNoScopePrefix() {
        final Instruction instruction = new Instruction(
                "$.value",
                "dest.value",
                new DataIdentifierReference("temperature", DataIdentifierReference.Type.TAG));
        assertThat(instruction.toSourceJsonPath()).isEqualTo("$['TAG:temperature'].value");
    }

    @Test
    public void toSourceJsonPath_whenTagWithScope_thenScopeIsPrefixed() {
        final Instruction instruction = new Instruction(
                "$.value",
                "dest.value",
                new DataIdentifierReference("temperature", DataIdentifierReference.Type.TAG, "adapter1"));
        assertThat(instruction.toSourceJsonPath()).isEqualTo("$['adapter1/TAG:temperature'].value");
    }

    @Test
    public void toSourceJsonPath_whenTagWithScopeAndNestedPath_thenScopeIsPrefixed() {
        final Instruction instruction = new Instruction(
                "$.readings.temperature",
                "dest.temp",
                new DataIdentifierReference("sensor1", DataIdentifierReference.Type.TAG, "opcua-adapter"));
        assertThat(instruction.toSourceJsonPath()).isEqualTo("$['opcua-adapter/TAG:sensor1'].readings.temperature");
    }

    @Test
    public void toSourceJsonPath_whenTagWithScopeAndRootPath_thenScopeIsPrefixed() {
        final Instruction instruction = new Instruction(
                "$",
                "dest.all",
                new DataIdentifierReference("sensor1", DataIdentifierReference.Type.TAG, "adapter1"));
        assertThat(instruction.toSourceJsonPath()).isEqualTo("$['adapter1/TAG:sensor1']");
    }

    @Test
    public void toSourceJsonPath_whenTwoTagsSameNameDifferentScope_thenDifferentPaths() {
        final Instruction instruction1 = new Instruction(
                "$.value",
                "dest.temp1",
                new DataIdentifierReference("temperature", DataIdentifierReference.Type.TAG, "adapter1"));
        final Instruction instruction2 = new Instruction(
                "$.value",
                "dest.temp2",
                new DataIdentifierReference("temperature", DataIdentifierReference.Type.TAG, "adapter2"));

        assertThat(instruction1.toSourceJsonPath()).isEqualTo("$['adapter1/TAG:temperature'].value");
        assertThat(instruction2.toSourceJsonPath()).isEqualTo("$['adapter2/TAG:temperature'].value");
        assertThat(instruction1.toSourceJsonPath()).isNotEqualTo(instruction2.toSourceJsonPath());
    }

    @Test
    public void toSourceJsonPath_whenTagWithDotsInId_thenDotsReplacedWithSlashes() {
        final Instruction instruction = new Instruction(
                "$.value",
                "dest.value",
                new DataIdentifierReference("sensor.temperature", DataIdentifierReference.Type.TAG, "adapter1"));
        assertThat(instruction.toSourceJsonPath()).isEqualTo("$['adapter1/TAG:sensor/temperature'].value");
    }

    @Test
    public void toSourceJsonPath_whenPulseAssetWithoutScope_thenNoScopePrefix() {
        final Instruction instruction = new Instruction(
                "$.value",
                "dest.value",
                new DataIdentifierReference("asset-123", DataIdentifierReference.Type.PULSE_ASSET));
        assertThat(instruction.toSourceJsonPath()).isEqualTo("$['PULSE_ASSET:asset-123'].value");
    }

    @Test
    public void toSourceJsonPath_whenNoDataIdentifierReference_thenReturnSourceFieldName() {
        final Instruction instruction = new Instruction("$.value", "dest.value", null);
        assertThat(instruction.toSourceJsonPath()).isEqualTo("$.value");
    }

    @Test
    public void toDestinationJsonPath_whenFullJsonPath_thenStripsDollarDot() {
        final Instruction instruction = new Instruction(
                "$.value",
                "$.dest.value",
                new DataIdentifierReference("tag1", DataIdentifierReference.Type.TAG));
        assertThat(instruction.toDestinationJsonPath()).isEqualTo("dest.value");
    }

    @Test
    public void toDestinationJsonPath_whenDotNotation_thenReturnsAsIs() {
        final Instruction instruction = new Instruction(
                "$.value",
                "dest.value",
                new DataIdentifierReference("tag1", DataIdentifierReference.Type.TAG));
        assertThat(instruction.toDestinationJsonPath()).isEqualTo("dest.value");
    }
}
