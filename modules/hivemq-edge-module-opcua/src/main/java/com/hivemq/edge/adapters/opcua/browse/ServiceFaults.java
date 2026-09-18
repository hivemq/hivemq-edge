/*
 * Copyright 2023-present HiveMQ GmbH
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
package com.hivemq.edge.adapters.opcua.browse;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletionException;
import org.eclipse.milo.opcua.stack.core.StatusCodes;
import org.eclipse.milo.opcua.stack.core.UaException;
import org.eclipse.milo.opcua.stack.core.types.builtin.ByteString;
import org.eclipse.milo.opcua.stack.core.types.builtin.StatusCode;
import org.eclipse.milo.opcua.stack.core.types.structured.BrowseResult;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** How the browser reads what the server said: service faults, result statuses, continuation points. */
final class ServiceFaults {

    private ServiceFaults() {}

    /**
     * A service fault that says the request was too big for the server in one way or another: too many
     * operations, or a request/response the negotiated message size cannot carry. All of them are answered by
     * sending less per request.
     */
    static boolean isTooManyOperations(final @Nullable Throwable cause) {
        // A dependent stage (a callback on Milo's future) sees the fault wrapped in a CompletionException.
        final Throwable unwrapped =
                cause instanceof CompletionException && cause.getCause() != null ? cause.getCause() : cause;
        if (!(unwrapped instanceof final UaException ua)) {
            return false;
        }
        final long status = ua.getStatusCode().getValue();
        return status == StatusCodes.Bad_TooManyOperations
                || status == StatusCodes.Bad_ResponseTooLarge
                || status == StatusCodes.Bad_RequestTooLarge
                || status == StatusCodes.Bad_EncodingLimitsExceeded
                || status == StatusCodes.Bad_TcpMessageTooLarge;
    }

    /** The status of a Milo fault, for log lines; empty when the cause is not one. */
    static @NotNull String statusOf(final @Nullable Throwable cause) {
        return cause instanceof final UaException ua ? String.valueOf(ua.getStatusCode()) : "";
    }

    static boolean isNoContinuationPoints(final @Nullable StatusCode status) {
        return status != null && status.getValue() == StatusCodes.Bad_NoContinuationPoints;
    }

    static boolean hasContinuationPoint(final @NotNull BrowseResult result) {
        return result.getContinuationPoint() != null
                && result.getContinuationPoint().bytes() != null
                && result.getContinuationPoint().bytes().length > 0;
    }

    /** All continuation points present in {@code results}, whatever each result's status. */
    static @NotNull List<ByteString> continuationPointsOf(final @Nullable BrowseResult[] results) {
        if (results == null) {
            return List.of();
        }
        final List<ByteString> points = new ArrayList<>();
        for (final BrowseResult result : results) {
            if (result != null && hasContinuationPoint(result)) {
                points.add(result.getContinuationPoint());
            }
        }
        return points;
    }
}
