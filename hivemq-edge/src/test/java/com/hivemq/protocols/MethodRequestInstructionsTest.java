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
/// what an instruction names, so anything no instruction mentions is dropped before the adapter sees it. A method
/// request's `context` -- which method to call, the caller's token, the address for the answer -- therefore needs
/// an instruction naming it, or it never arrives.
///
/// **One instruction, not three.** An earlier shape added an identity copy per field (`$.methodName` to
/// `$.methodName`, and the same for the other two), which made the mapping's flags work *by omission*: switching
/// a flag off simply added no instruction. That had two consequences this shape does not: a user-configured
/// instruction naming the field was enough to defeat the switch, and three destinations at the message root
/// could collide with configured ones. Now the context is assembled before the message reaches the policy, the
/// flags are read there, and one instruction carries the result.
class MethodRequestInstructionsTest {

    private static @NotNull FieldMapping userMapping() {
        return new FieldMapping(List.of(new Instruction("$.temperature", "$.value.setpoint", null)));
    }

    private static @NotNull List<String> sources(final @NotNull FieldMapping mapping) {
        return mapping.getInstructions().stream()
                .map(Instruction::sourceFieldName)
                .toList();
    }

    private static @NotNull List<String> destinations(final @NotNull FieldMapping mapping) {
        return mapping.getInstructions().stream()
                .map(Instruction::toDestinationJsonPath)
                .toList();
    }

    @Test
    void notAMethodMapping_isLeftExactlyAsItWas() {
        // Every southbound mapping that exists today. The generated transform for them has to stay identical,
        // which is what makes this change invisible to a value write.
        final FieldMapping configured = userMapping();

        final FieldMapping augmented = MethodRequestInstructions.augment(configured, false);

        assertThat(augmented.getInstructions()).isEqualTo(configured.getInstructions());
    }

    @Test
    void aMethodMapping_getsExactlyOneInstructionAdded() {
        // Sourced from the user property Edge assembles the context into, destined for `context` beside `value`.
        // The source carries the '@.' marker that tells the generated script to look in the user properties
        // rather than walk the payload.
        final FieldMapping augmented = MethodRequestInstructions.augment(userMapping(), true);

        assertThat(augmented.getInstructions()).hasSize(2);
        assertThat(destinations(augmented)).containsExactly("value.setpoint", "context");
        assertThat(sources(augmented))
                .containsExactly("$.temperature", "@." + MethodRequestInstructions.CONTEXT_USER_PROPERTY);
    }

    @Test
    void aConfiguredInstructionAlreadyWritingContext_isLeftAloneRatherThanDuplicated() {
        // Two instructions writing one destination would make the result depend on their order. Not reachable
        // from the mapping editor, which declares the field read-only -- but a configuration file written by
        // hand is not filtered that way.
        final FieldMapping configured = new FieldMapping(List.of(new Instruction("$.mine", "$.context", null)));

        final FieldMapping augmented = MethodRequestInstructions.augment(configured, true);

        assertThat(augmented.getInstructions()).isEqualTo(configured.getInstructions());
        assertThat(destinations(augmented)).containsExactly("context");
    }
}
