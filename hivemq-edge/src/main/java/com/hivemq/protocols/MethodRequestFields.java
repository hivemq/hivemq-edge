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

import org.jetbrains.annotations.NotNull;

/// The field names a method tag's messages carry beside `value`.
///
/// A request and its response carry these at the message root. Across the border between Edge and an adapter
/// they travel inside one object, named by [#INTERNAL_REQUEST_PARAMETERS] or
/// [#INTERNAL_RESPONSE_PARAMETERS], which sits inside `value`.
///
/// **This file is the twin of `MethodRequestFieldsOpcUa` in the OPC-UA module, and the two are identical
/// except for their name and this paragraph.** An adapter module must not depend on Edge in the Gradle sense
/// -- it compiles against the adapter SDK -- so it cannot reference this one. Keeping the two byte-identical
/// otherwise is what makes a divergence visible in a diff.
///
/// Nothing in the compiler enforces that they agree, and no unit test does either. What catches a divergence
/// today is `OpcUaMethodCallIT`, which publishes a real request and reads the real answer, so a name that
/// stops matching breaks it.
public final class MethodRequestFields {

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
    /// **Deliberately not `methodName`, and that is the whole of the mechanism.** Three things together make a
    /// southbound message a method call: a non-blank `methodName` in the payload, a southbound mapping for that
    /// tag and topic filter with method calls switched on, and a tag whose kind is `METHOD`. Nothing else is
    /// consulted -- in particular nothing looks for this field.
    ///
    /// So a response is **not** a request, by construction: it carries no `methodName`, so the first condition
    /// fails and the message is quarantined on `$invalid/` for naming no method. No rule had to be invented to
    /// get that, and no capability was taken away.
    ///
    /// **What it prevents.** A caller may name any publishable topic as its reply address, and a northbound
    /// mapping's own topic is equally free. Naming a method tag's own command topic -- or another method tag's
    /// -- is a plausible configuration: a self-loop needs only one careless line. Before the rename the answer
    /// was a structurally valid request, so the mapping reading that topic dispatched it, and where the first
    /// method's outputs happened to satisfy the second method's inputs **the device was called**.
    ///
    /// Reported by QA as P22 on EDG-1116, and by Miguel as R5-1 from the other side.
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

    private MethodRequestFields() {}
}
