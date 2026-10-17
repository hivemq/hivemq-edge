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

import org.jetbrains.annotations.NotNull;

/// The field names a method tag's messages carry beside `value`.
///
/// A request and its response carry these at the message root. Across the border between Edge and an adapter
/// they travel inside one object, named by [#INTERNAL_REQUEST_PARAMETERS] or
/// [#INTERNAL_RESPONSE_PARAMETERS], which sits inside `value`.
///
/// **This file is the twin of `com.hivemq.protocols.MethodRequestFields` in Edge, and the two are identical
/// except for their name and this paragraph.** An adapter module must not depend on Edge in the Gradle sense
/// -- it compiles against the adapter SDK -- so it cannot reference that one. Keeping the two byte-identical
/// otherwise is what makes a divergence visible in a diff.
///
/// Nothing in the compiler enforces that they agree, and no unit test does either. What catches a divergence
/// today is `OpcUaMethodCallIT`, which publishes a real request and reads the real answer, so a name that
/// stops matching breaks it.
public final class MethodRequestFieldsOpcUa {

    /// The payload aspect holding what the device names: a request's arguments, a response's outputs. It is
    /// also what the parameter object below sits inside, across the adapter border.
    public static final @NotNull String VALUE = "value";

    /// The object inside `value` that carries a request's parameters across the adapter border.
    ///
    /// Edge writes it immediately before handing the payload to the adapter, which removes it immediately
    /// after, so it appears in no published message and in no schema. Named `INTERNAL_` because nothing
    /// outside those two steps may read it.
    ///
    /// The suffix is 64 random bits, frozen into the source. These two names decide behaviour on their own:
    /// finding one of them is what makes Edge treat a payload as a method call's parameters, and that is
    /// deliberate, because a name that decides cannot be confused across adapters the way a flag carried
    /// beside the data could. The price is that anything able to put a field into a payload -- a device
    /// answering a read, a caller publishing a message -- could otherwise reach this machinery by choosing
    /// the same name. The random suffix makes that collision impossible to hit by accident and impractical
    /// to hit on purpose, while leaving the readable prefix for whoever meets the field in a debug dump.
    ///
    /// The suffix encodes 8 random bytes, each as an uppercase letter followed by a digit: the letter is
    /// `A` plus the byte divided by ten, the digit is the byte modulo ten. Byte 0 reads `A0`, byte 255
    /// reads `Z5`, and the pairs `Z6` to `Z9` never occur, so the 16 characters decode back to the bytes
    /// `4f 43 b3 10 b4 d7 94 b2` without ambiguity.
    public static final @NotNull String INTERNAL_REQUEST_PARAMETERS = "INTERNAL_REQUEST_PARAMETERS_H9G7R9B6S0V5O8R8";

    /// The object inside `value` that carries a response's parameters back across the adapter border.
    ///
    /// Carries the same random suffix as [#INTERNAL_REQUEST_PARAMETERS], because the two are one
    /// mechanism and a reader should see that at a glance.
    public static final @NotNull String INTERNAL_RESPONSE_PARAMETERS = "INTERNAL_RESPONSE_PARAMETERS_H9G7R9B6S0V5O8R8";

    /// Request only: the method to call.
    public static final @NotNull String METHOD_NAME = "methodName";

    /// Response only: the method an answer belongs to. Read as "response from *methodName*".
    ///
    /// **Deliberately not `methodName`.** A response that carried that name would be a structurally valid
    /// request, so a southbound method mapping reading the answer's topic would dispatch it -- and where the
    /// answered method's outputs satisfied the second method's inputs, the device would be called. Carrying a
    /// different name means a response fails the first of the three conditions that make a message a method
    /// call, and is quarantined for naming no method.
    ///
    /// Mirrors `MethodRequestFields.RESPONSE_METHOD_NAME` in Edge, which carries the full reasoning. The two
    /// are duplicated because an adapter module must not depend on Edge.
    public static final @NotNull String RESPONSE_METHOD_NAME = "responseMethodName";

    /// A token echoed from a request to its response, carrying an encoding prefix while inside Edge.
    public static final @NotNull String CORRELATION_DATA = "correlationData";

    /// Request only: where to publish the answer, instead of the northbound mapping's own topic.
    public static final @NotNull String RESPONSE_TOPIC = "responseTopic";

    /// Response only: the OPC UA status code by name, present only when the call failed.
    public static final @NotNull String ERROR_CODE = "errorCode";

    /// Response only: the device's own detail about a failure, when it gave any.
    public static final @NotNull String STATUS_INFO = "statusInfo";

    /// A datapoint's aspect holding the protocol's own namespace. Not a method field, and not where a
    /// method's parameters travel -- an earlier design carried them here.
    public static final @NotNull String CONTEXT = "context";

    private MethodRequestFieldsOpcUa() {}
}
