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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.eclipse.milo.opcua.stack.core.StatusCodes;
import org.eclipse.milo.opcua.stack.core.types.builtin.StatusCode;
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant;
import org.eclipse.milo.opcua.stack.core.types.structured.CallMethodResult;
import org.eclipse.milo.opcua.stack.core.types.structured.CallResponse;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/// What a `Call` answered, in the shape a response message needs.
///
/// **Why this exists rather than reusing the condition path.** `ConditionUpdateWriter.statusOf` reduces a
/// `CallResponse` to a single `StatusCode` and discards `outputArguments` and `inputArgumentResults` -- which is
/// right for a condition, whose methods return nothing and whose only question is "did it work". A method call
/// exists to carry those two back, so it needs its own extraction.
///
/// **Two failure detail shapes, not one.** OPC UA reports a failed call's detail in unrelated forms: a
/// `StatusCode` per input argument, positionally, and whatever the method itself returned. Both are kept, and
/// the positional one is translated back to the names the request used -- the caller never saw the positions.
public record MethodCallResult(
        /// The call's overall status. `Good` and the other success codes mean the method ran.
        @NotNull StatusCode statusCode,
        /// The output arguments by name, empty when the method returns none or the call failed.
        @NotNull Map<String, Variant> outputArguments,
        /// Per-input-argument status by argument name, for the arguments the server reported one for. Empty when
        /// it reported none, which is the common case even on failure.
        @NotNull Map<String, StatusCode> argumentStatuses) {

    public MethodCallResult {
        outputArguments = Map.copyOf(outputArguments);
        argumentStatuses = Map.copyOf(argumentStatuses);
    }

    /// Whether the method ran.
    ///
    /// **`Uncertain` counts as failure here**, which is the opposite of how a *read* treats it. An uncertain read
    /// still carries a value worth publishing with a caveat; an uncertain call has no result to publish, and the
    /// device's own explanation is in the output arguments. So the caller is told the call failed and given that
    /// explanation, rather than a value it cannot trust.
    public boolean succeeded() {
        return statusCode.isGood();
    }

    /// The status code's name, as a response reports it -- `Bad_MethodInvalid` rather than `0x80750000`. The name
    /// is stable across servers, searchable in the specification, and readable in a log.
    public @NotNull String statusName() {
        return StatusCodes.lookup(statusCode.value())
                .map(names -> names[0])
                .orElseGet(() -> String.format("0x%08X", statusCode.value()));
    }

    /// Extracts the one result of a single-method `Call`.
    ///
    /// @param response   what the server answered.
    /// @param signature  the method called, for translating argument positions back to names.
    public static @NotNull MethodCallResult of(
            final @NotNull CallResponse response, final @NotNull MethodSignature signature) {

        final CallMethodResult[] results = response.getResults();
        if (results == null || results.length == 0) {
            // The service succeeded and answered about no method. Nothing distinguishes which part failed, so
            // this is reported as a bad call rather than guessed at.
            return new MethodCallResult(new StatusCode(StatusCodes.Bad_UnexpectedError), Map.of(), Map.of());
        }
        final CallMethodResult result = results[0];

        return new MethodCallResult(
                result.getStatusCode() == null
                        ? new StatusCode(StatusCodes.Bad_UnexpectedError)
                        : result.getStatusCode(),
                namedOutputs(result.getOutputArguments(), signature),
                namedArgumentStatuses(result.getInputArgumentResults(), signature));
    }

    /// Keys the output arguments by the names the method declares for them.
    ///
    /// A server returning more values than it declared outputs is trusted about the values and not about the
    /// count: the extra ones have no name to be addressed by, so they are dropped rather than invented names for.
    private static @NotNull Map<String, Variant> namedOutputs(
            final Variant @Nullable [] outputs, final @NotNull MethodSignature signature) {
        if (outputs == null || outputs.length == 0) {
            return Map.of();
        }
        final List<String> names = signature.outputArgumentNames();
        final Map<String, Variant> named = new LinkedHashMap<>();
        for (int i = 0; i < outputs.length && i < names.size(); i++) {
            final String name = names.get(i);
            if (name != null && !name.isEmpty()) {
                named.put(name, outputs[i]);
            }
        }
        return named;
    }

    /// Translates the positional per-argument statuses back to argument names.
    ///
    /// This is the translation the caller cannot do: it sent arguments by name and never saw their positions.
    /// Good statuses are kept rather than filtered -- "setpoint was rejected, ramp was fine" tells an operator
    /// more than "setpoint was rejected" alone, because it says the rest were looked at.
    private static @NotNull Map<String, StatusCode> namedArgumentStatuses(
            final StatusCode @Nullable [] statuses, final @NotNull MethodSignature signature) {
        if (statuses == null || statuses.length == 0) {
            return Map.of();
        }
        final List<String> names = signature.inputArgumentNames();
        final Map<String, StatusCode> named = new LinkedHashMap<>();
        for (int i = 0; i < statuses.length && i < names.size(); i++) {
            final String name = names.get(i);
            if (name != null && !name.isEmpty() && statuses[i] != null) {
                named.put(name, statuses[i]);
            }
        }
        return named;
    }
}
