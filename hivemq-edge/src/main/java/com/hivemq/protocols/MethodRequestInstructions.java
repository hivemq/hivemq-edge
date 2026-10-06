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

import com.hivemq.persistence.mappings.fieldmapping.FieldMapping;
import com.hivemq.persistence.mappings.fieldmapping.Instruction;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.jetbrains.annotations.NotNull;

/// Adds the field-mapping instructions a method request needs to survive the southbound transform.
///
/// ## Why anything is needed
///
/// A southbound field mapping is a **whitelist**. The generated transform starts from an empty object and copies
/// across only what an instruction names, so a field no instruction mentions is dropped before the adapter sees
/// it. The default mapping is a single `value -> value` copy, which is why a method request's `methodName` would
/// otherwise never arrive -- and a request that cannot name its method cannot be dispatched at all.
///
/// ## Why they are synthetic
///
/// These are not instructions a user writes. The mapping editor offers the writable leaves of a tag's southbound
/// schema as destinations, and the three request fields are declared read-only there precisely so they are not
/// offered: a user wiring an arbitrary field into `correlationData` is the general mapping generator this design
/// avoids. So the instructions are derived from the mapping's own flags and added on the way to the script
/// generator, where they are invisible to the editor and absent from the persisted configuration.
///
/// Identity copies, `$.methodName -> $.methodName`: their only job is to stop the whitelist deleting the field.
public final class MethodRequestInstructions {

    private MethodRequestInstructions() {}

    /// The field naming which method to call. Added whenever the mapping carries method calls.
    static final @NotNull String METHOD_NAME = "methodName";

    /// The caller's token, echoed on the response.
    static final @NotNull String CORRELATION_DATA = "correlationData";

    /// The address the caller wants the response on.
    static final @NotNull String RESPONSE_TOPIC = "responseTopic";

    /// The field mapping to hand the script generator, given the mapping's own.
    ///
    /// Returns the original untouched when the mapping carries no method calls, which is every southbound mapping
    /// that exists today -- so the generated transform for them is byte-identical to what it was.
    ///
    /// @param configured the instructions the user configured.
    /// @param methodCalls whether this mapping carries method calls at all.
    /// @param correlationData whether requests carry a correlation token.
    /// @param responseTopic whether requests may name their own response topic.
    public static @NotNull FieldMapping augment(
            final @NotNull FieldMapping configured,
            final boolean methodCalls,
            final boolean correlationData,
            final boolean responseTopic) {

        if (!methodCalls) {
            return configured;
        }

        // Which fields to carry. `methodName` is unconditional once this is a method mapping: a request without it
        // cannot be dispatched, so a flag for it would only offer a broken configuration.
        final Set<String> fields = new LinkedHashSet<>();
        fields.add(METHOD_NAME);
        if (correlationData) {
            fields.add(CORRELATION_DATA);
        }
        if (responseTopic) {
            fields.add(RESPONSE_TOPIC);
        }

        // A user instruction already naming one of these destinations wins. It should not be reachable -- the
        // fields are read-only in the schema the editor offers -- but a hand-written configuration or a direct API
        // call can still produce one, and two instructions writing the same destination would make the result
        // depend on their order.
        final Set<String> alreadyWritten = new LinkedHashSet<>();
        configured.getInstructions().forEach(instruction -> alreadyWritten.add(instruction.toDestinationJsonPath()));

        final List<Instruction> augmented = new ArrayList<>(configured.getInstructions());
        fields.stream()
                .filter(field -> !alreadyWritten.contains(field))
                .forEach(field -> augmented.add(new Instruction("$." + field, "$." + field, null)));

        return new FieldMapping(augmented);
    }
}
