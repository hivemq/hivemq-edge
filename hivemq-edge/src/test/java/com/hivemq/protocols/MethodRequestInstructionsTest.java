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
package com.hivemq.protocols;

import static org.assertj.core.api.Assertions.assertThat;

import com.hivemq.persistence.mappings.fieldmapping.FieldMapping;
import com.hivemq.persistence.mappings.fieldmapping.Instruction;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

/// Which instructions a method mapping's transform is generated from.
///
/// A southbound field mapping is a whitelist: the generated transform starts from an empty object and copies only
/// what an instruction names, so a field no instruction mentions is dropped before the adapter sees it. These pin
/// that a method request's own fields get instructions, that nothing else does, and that a mapping which carries
/// no method calls is left exactly as it was.
class MethodRequestInstructionsTest {

    private static @NotNull FieldMapping userMapping() {
        return new FieldMapping(List.of(new Instruction("$.temperature", "$.value.setpoint", null)));
    }

    private static @NotNull List<String> destinations(final @NotNull FieldMapping mapping) {
        return mapping.getInstructions().stream()
                .map(Instruction::toDestinationJsonPath)
                .toList();
    }

    @Test
    void notAMethodMapping_isLeftExactlyAsItWas() {
        // Every southbound mapping that exists today. The generated transform for them has to stay identical.
        final FieldMapping configured = userMapping();

        final FieldMapping augmented = MethodRequestInstructions.augment(configured, false, false, false);

        assertThat(augmented).isSameAs(configured);
    }

    @Test
    void notAMethodMapping_ignoresTheOtherTwoFlags() {
        // The method-call flag is the precondition. Correlation data on a mapping that is not a method mapping has
        // nothing to correlate, so it must not quietly turn one on.
        final FieldMapping configured = userMapping();

        assertThat(MethodRequestInstructions.augment(configured, false, true, true))
                .isSameAs(configured);
    }

    @Test
    void aMethodMapping_alwaysCarriesTheMethodName() {
        // A fire-and-forget call: method calls on, neither optional field. Without this instruction the request
        // arrives with no method named and cannot be dispatched at all.
        final FieldMapping augmented = MethodRequestInstructions.augment(userMapping(), true, false, false);

        assertThat(destinations(augmented)).containsExactly("value.setpoint", "methodName");
    }

    @Test
    void eachFlagAddsItsOwnField() {
        final FieldMapping all = MethodRequestInstructions.augment(userMapping(), true, true, true);

        assertThat(destinations(all))
                .containsExactly("value.setpoint", "methodName", "correlationData", "responseTopic");
    }

    @Test
    void theFlagsAreIndependent() {
        assertThat(destinations(MethodRequestInstructions.augment(userMapping(), true, true, false)))
                .containsExactly("value.setpoint", "methodName", "correlationData");
        assertThat(destinations(MethodRequestInstructions.augment(userMapping(), true, false, true)))
                .containsExactly("value.setpoint", "methodName", "responseTopic");
    }

    @Test
    void theAddedInstructionsAreIdentityCopies() {
        // Their only job is to stop the whitelist deleting the field: the name on the wire is the name in the
        // request, and no renaming is offered anywhere.
        final FieldMapping augmented = MethodRequestInstructions.augment(userMapping(), true, true, true);

        augmented.getInstructions().stream()
                .filter(instruction -> !instruction.toDestinationJsonPath().startsWith("value"))
                .forEach(instruction -> assertThat(instruction.toSourceJsonPath())
                        .isEqualTo("$." + instruction.toDestinationJsonPath()));
    }

    @Test
    void aUserInstructionWritingTheSameDestinationWins() {
        // Should be unreachable -- these fields are read-only in the schema the editor offers destinations from --
        // but a hand-written configuration or a direct API call can still produce one, and two instructions writing
        // one destination would make the result depend on their order.
        final FieldMapping configured =
                new FieldMapping(List.of(new Instruction("$.theirField", "$.methodName", null)));

        final FieldMapping augmented = MethodRequestInstructions.augment(configured, true, false, false);

        assertThat(augmented.getInstructions()).hasSize(1);
        assertThat(augmented.getInstructions().get(0).toSourceJsonPath()).isEqualTo("$.theirField");
    }

    @Test
    void aConfiguredInstructionCannotDefeatASwitchedOffFlag() {
        // The decisive test. The flags work by omission: a southbound mapping is a whitelist, so switching
        // `responseTopic` off simply adds no instruction and the field is deleted before the adapter sees it.
        // A configured instruction naming the field was therefore enough to keep it -- the whitelist mentioned
        // it either way, the adapter read it, and an administrator who had disabled caller-chosen reply topics
        // still had requests redirected. The stored flag described nothing.
        final FieldMapping configured =
                new FieldMapping(List.of(new Instruction("$.theirField", "$.responseTopic", null)));

        final FieldMapping augmented = MethodRequestInstructions.augment(configured, true, false, false);

        assertThat(destinations(augmented))
                .as("a switched-off field must not survive in the transform")
                .containsExactly("methodName");
    }

    @Test
    void theSameHoldsForCorrelationData() {
        final FieldMapping configured =
                new FieldMapping(List.of(new Instruction("$.theirField", "$.correlationData", null)));

        final FieldMapping augmented = MethodRequestInstructions.augment(configured, true, false, false);

        assertThat(destinations(augmented)).containsExactly("methodName");
    }

    @Test
    void aConfiguredInstructionForASwitchedOnFieldIsKept() {
        // The counterpart. With the flag on, the field belongs in the transform -- and the configured
        // instruction is the one that writes it, since two instructions naming one destination would make the
        // result depend on their order. So the source stays the user's, not an identity copy.
        final FieldMapping configured =
                new FieldMapping(List.of(new Instruction("$.theirField", "$.responseTopic", null)));

        final FieldMapping augmented = MethodRequestInstructions.augment(configured, true, false, true);

        assertThat(destinations(augmented)).containsExactly("responseTopic", "methodName");
        assertThat(augmented.getInstructions().get(0).toSourceJsonPath()).isEqualTo("$.theirField");
    }

    @Test
    void anInstructionNamingMethodNameIsNeverDiscarded() {
        // `methodName` is not reserved: it has no flag to contradict, and it is added whenever the mapping
        // carries method calls -- so an instruction naming it agrees with what Edge would have added anyway.
        // Discarding it would delete the one field without which no request can be dispatched.
        final FieldMapping configured =
                new FieldMapping(List.of(new Instruction("$.theirField", "$.methodName", null)));

        final FieldMapping augmented = MethodRequestInstructions.augment(configured, true, false, false);

        assertThat(destinations(augmented)).containsExactly("methodName");
        assertThat(augmented.getInstructions().get(0).toSourceJsonPath()).isEqualTo("$.theirField");
    }

    @Test
    void anOrdinaryInstructionIsUntouchedWhateverTheFlagsSay() {
        // The reserved destinations are two names, not a general filter: a user's own mapping of a value field
        // must survive with every combination of the flags.
        final FieldMapping augmented = MethodRequestInstructions.augment(userMapping(), true, false, false);

        assertThat(destinations(augmented)).containsExactly("value.setpoint", "methodName");
    }

    @Test
    void aMappingThatCarriesNoMethodCalls_keepsEvenAReservedDestination() {
        // Nothing is reserved when this is not a method mapping. Every southbound mapping that exists today
        // takes this path, and its transform has to stay byte-identical.
        final FieldMapping configured =
                new FieldMapping(List.of(new Instruction("$.theirField", "$.responseTopic", null)));

        final FieldMapping augmented = MethodRequestInstructions.augment(configured, false, false, false);

        assertThat(augmented).isSameAs(configured);
    }

    @Test
    void anEmptyUserMapping_stillGetsTheRequestFields() {
        // A method tag needs no value instruction at all when its methods take no arguments.
        final FieldMapping augmented = MethodRequestInstructions.augment(new FieldMapping(List.of()), true, true, true);

        assertThat(destinations(augmented)).containsExactly("methodName", "correlationData", "responseTopic");
    }
}
