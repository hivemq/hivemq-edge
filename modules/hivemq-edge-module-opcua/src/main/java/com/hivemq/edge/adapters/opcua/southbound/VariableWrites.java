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
package com.hivemq.edge.adapters.opcua.southbound;

import com.fasterxml.jackson.databind.JsonNode;
import com.hivemq.edge.adapters.opcua.southbound.JsonToOpcUaValueConverter.ConversionException;
import org.eclipse.milo.opcua.sdk.client.OpcUaClient;
import org.eclipse.milo.opcua.sdk.core.ValueRanks;
import org.eclipse.milo.opcua.stack.core.UaException;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant;
import org.jetbrains.annotations.NotNull;

/// The value a write to a variable sends.
///
/// What the value-write route adds to the converter is only where the declaration comes from: a method argument
/// carries its DataType in the method's signature, a variable carries it in its own `DataType` attribute.
public final class VariableWrites {

    private VariableWrites() {}

    /// Converts a value for the given variable, as the variable's DataType.
    ///
    /// **The shape is the value's own, not the variable's `ValueRank`.** A value write has always sent whatever
    /// shape the JSON has -- a list as an array, anything else as a scalar -- and left a mismatch for the server
    /// to refuse. Checking the declared rank here would refuse writes that work today against servers whose
    /// `ValueRank` attribute does not match what they accept, so the rank passed is `Any`.
    ///
    /// @throws UaException         when the variable's DataType cannot be read.
    /// @throws ConversionException when the DataType cannot be encoded, or the value does not fit it.
    public static @NotNull Variant valueFor(
            final @NotNull OpcUaClient client, final @NotNull NodeId variable, final @NotNull JsonNode value)
            throws UaException {

        final NodeId dataType =
                client.getAddressSpace().getVariableNode(variable).getDataType();
        if (dataType == null) {
            throw new ConversionException(
                    ConversionException.Fault.UNSUPPORTED,
                    variable.toParseableString(),
                    "the server declares no DataType for the variable");
        }
        return JsonToOpcUaValueConverter.of(client).toVariant(value, dataType, ValueRanks.Any);
    }
}
