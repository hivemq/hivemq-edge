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
package com.hivemq.edge.adapters.opcua.method;

import com.fasterxml.jackson.databind.JsonNode;
import com.hivemq.edge.adapters.opcua.southbound.JsonToOpcUaValueConverter;
import com.hivemq.edge.adapters.opcua.southbound.JsonToOpcUaValueConverter.ConversionException;
import java.util.ArrayList;
import java.util.List;
import org.eclipse.milo.opcua.stack.core.StatusCodes;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant;
import org.eclipse.milo.opcua.stack.core.types.structured.Argument;
import org.jetbrains.annotations.NotNull;

/// Turns a request's named arguments into the positional `Variant[]` a `Call` takes.
///
/// A request names its arguments; the wire passes them by position. The order in {@link MethodSignature} is that
/// mapping, so this walks the signature rather than the request -- an argument the method declares and the
/// request omits still occupies its position, and one the request carries but the method does not declare is a
/// mistake worth naming rather than silently dropping.
///
/// What each argument's value may be is not decided here. {@link JsonToOpcUaValueConverter} decides that for every
/// southbound route, so an argument of a given DataType accepts exactly what a variable of that DataType does.
public final class MethodArguments {

    private MethodArguments() {}

    /// Why a request could not be turned into a call. Carries the message a caller sees, which names the
    /// offending argument -- an OPC UA server answers the same mistakes with `Bad_InvalidArgument` and no
    /// indication of which argument was wrong.
    public static final class InvalidArgumentException extends RuntimeException {
        private final @NotNull String reason;
        private final long statusCode;

        InvalidArgumentException(final @NotNull String message) {
            this(message, StatusCodes.Bad_InvalidArgument);
        }

        InvalidArgumentException(final @NotNull String message, final long statusCode) {
            super(message);
            this.reason = message;
            this.statusCode = statusCode;
        }

        /// The message, as a non-null value.
        ///
        /// `getMessage()` is declared nullable on `Throwable`, so a caller reporting this to a user has to
        /// defend against a null that cannot occur here. Kept as its own field rather than asserted at each
        /// call site.
        public @NotNull String reason() {
            return reason;
        }

        /// The status code the caller is answered with.
        ///
        /// Carried on the exception because the fault is known here and not at the catch: every argument
        /// problem reaches the adapter through one handler, which cannot tell a missing argument from a
        /// misspelled one without inspecting the message text. `Bad_InvalidArgument` is the default, so a
        /// throw site that does not say otherwise behaves as before.
        public long statusCode() {
            return statusCode;
        }
    }

    /// Builds the input arguments for a call, in wire order, for arguments of builtin types only.
    ///
    /// For a caller with no server at hand. An argument whose type needs the server's DataType hierarchy -- an
    /// enumeration, a structure, a Simple DataType the browse did not resolve -- is refused as one whose type
    /// cannot be resolved.
    public static @NotNull Variant[] forCall(final @NotNull MethodSignature signature, final @NotNull JsonNode value) {
        return forCall(signature, value, JsonToOpcUaValueConverter.builtinOnly());
    }

    /// Builds the input arguments for a call, in wire order.
    ///
    /// @param signature the method as the device describes it.
    /// @param value     the request's `value` object, keyed by argument name.
    /// @param converter the converter for the device the method is called on.
    /// @throws InvalidArgumentException when the request names an argument the method does not declare, omits one
    ///         it does, or carries a value the argument's type cannot hold.
    public static @NotNull Variant[] forCall(
            final @NotNull MethodSignature signature,
            final @NotNull JsonNode value,
            final @NotNull JsonToOpcUaValueConverter converter) {

        rejectUnknownNames(signature, value);

        final List<Argument> declared = signature.inputArguments();
        final Variant[] arguments = new Variant[declared.size()];
        for (int i = 0; i < declared.size(); i++) {
            final Argument argument = declared.get(i);
            final String name = argument.getName();
            // An argument the device did not name cannot be addressed by a request, so nothing can be supplied
            // for it. Passing a null Variant keeps the positions aligned, which is the only thing that matters
            // for the arguments that *are* named.
            if (name == null || name.isEmpty()) {
                arguments[i] = Variant.NULL_VALUE;
                continue;
            }
            final JsonNode supplied = value.get(name);
            if (supplied == null || supplied.isNull()) {
                // The one argument fault OPC-UA names specifically. Every other one -- a name the method does
                // not declare, a value its type cannot hold -- is Bad_InvalidArgument, which is also what a
                // server answers. Reported as its own code because a caller branching on status codes can act
                // on "you left something out" without parsing the message.
                throw new InvalidArgumentException(
                        "method '"
                                + signature.name()
                                + "' requires the argument '"
                                + name
                                + "', which the request does not carry. It declares: "
                                + String.join(", ", signature.inputArgumentNames()),
                        StatusCodes.Bad_ArgumentsMissing);
            }
            arguments[i] = convert(signature, name, argument, supplied, converter);
        }
        return arguments;
    }

    /// Rejects a request naming arguments the method does not declare.
    ///
    /// Done before any conversion so the message names the mistake rather than the symptom. Without it the extra
    /// field is simply ignored, and a caller who misspells `setpoint` gets a `Bad_ArgumentsMissing` about the
    /// argument they thought they had supplied.
    private static void rejectUnknownNames(final @NotNull MethodSignature signature, final @NotNull JsonNode value) {
        final List<String> unknown = new ArrayList<>();
        value.fieldNames().forEachRemaining(name -> {
            if (signature.inputPosition(name) == null) {
                unknown.add(name);
            }
        });
        if (!unknown.isEmpty()) {
            throw new InvalidArgumentException("method '"
                    + signature.name()
                    + "' does not declare the argument(s) "
                    + String.join(", ", unknown)
                    + ". It declares: "
                    + (signature.declaresNoInputs() ? "none" : String.join(", ", signature.inputArgumentNames())));
        }
    }

    /// Converts one argument, and words a refusal with the argument's and the method's names.
    ///
    /// The builtin the browse resolved for the argument is passed along, so a Simple DataType such as `Duration`
    /// -- which needs the server's DataType hierarchy to reduce -- is converted from the answer the browse already
    /// has rather than asking again at call time. An argument the browse reduced to no usable builtin is resolved
    /// by the converter from its DataType: an enumeration reduces to `Variant` and a structure to
    /// `ExtensionObject`, and both are encodable.
    private static @NotNull Variant convert(
            final @NotNull MethodSignature signature,
            final @NotNull String argumentName,
            final @NotNull Argument argument,
            final @NotNull JsonNode supplied,
            final @NotNull JsonToOpcUaValueConverter converter) {

        final String owner = "argument '" + argumentName + "' of method '" + signature.name() + "'";
        final NodeId dataType = argument.getDataType();
        if (dataType == null) {
            throw new InvalidArgumentException(
                    owner + " has a type this version cannot encode: the device declares none");
        }
        try {
            return converter.toVariant(supplied, dataType, argument.getValueRank(), signature.builtinTypeOf(argument));
        } catch (final ConversionException e) {
            throw new InvalidArgumentException(
                    switch (e.fault()) {
                        case UNSUPPORTED ->
                            owner + " has a type this version cannot encode: " + e.expected() + ", because "
                                    + e.reason();
                        case SHAPE -> owner + " " + e.reason();
                        case VALUE ->
                            owner + " expects " + e.expected() + ", and the value supplied is not one: " + e.reason();
                    });
        }
    }
}
