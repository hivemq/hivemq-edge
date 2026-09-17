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

import static com.hivemq.edge.adapters.opcua.browse.FakeOpcUaServer.serviceFault;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.CompletionException;
import org.eclipse.milo.opcua.stack.core.StatusCodes;
import org.eclipse.milo.opcua.stack.core.UaException;
import org.eclipse.milo.opcua.stack.core.types.builtin.ByteString;
import org.eclipse.milo.opcua.stack.core.types.builtin.StatusCode;
import org.eclipse.milo.opcua.stack.core.types.structured.BrowseResult;
import org.eclipse.milo.opcua.stack.core.types.structured.ReferenceDescription;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** {@link ServiceFaults}: how faults, statuses and continuation points are read. */
class ServiceFaultsTest {

    @ParameterizedTest(name = "status 0x{0}")
    @ValueSource(
            longs = {
                StatusCodes.Bad_TooManyOperations,
                StatusCodes.Bad_ResponseTooLarge,
                StatusCodes.Bad_RequestTooLarge,
                StatusCodes.Bad_EncodingLimitsExceeded,
                StatusCodes.Bad_TcpMessageTooLarge
            })
    void isTooManyOperations_everyTooBigFault(final long status) {
        assertThat(ServiceFaults.isTooManyOperations(serviceFault(status))).isTrue();
        assertThat(ServiceFaults.isTooManyOperations(new UaException(status))).isTrue();
    }

    @ParameterizedTest(name = "status 0x{0}")
    @ValueSource(
            longs = {
                StatusCodes.Bad_NodeIdUnknown,
                StatusCodes.Bad_NoContinuationPoints,
                StatusCodes.Bad_ContinuationPointInvalid,
                StatusCodes.Bad_ConnectionClosed,
                StatusCodes.Bad_Timeout
            })
    void isTooManyOperations_otherFaults_false(final long status) {
        assertThat(ServiceFaults.isTooManyOperations(serviceFault(status))).isFalse();
    }

    @Test
    void isTooManyOperations_seesThroughTheCompletionExceptionOfADependentStage() {
        final UaException fault = serviceFault(StatusCodes.Bad_TooManyOperations);
        assertThat(ServiceFaults.isTooManyOperations(new CompletionException(fault)))
                .isTrue();
        assertThat(ServiceFaults.isTooManyOperations(
                        new CompletionException(serviceFault(StatusCodes.Bad_NodeIdUnknown))))
                .isFalse();
        assertThat(ServiceFaults.isTooManyOperations(new CompletionException((Throwable) null)))
                .isFalse();
    }

    @Test
    void isTooManyOperations_notAUaException_false() {
        assertThat(ServiceFaults.isTooManyOperations(null)).isFalse();
        assertThat(ServiceFaults.isTooManyOperations(new RuntimeException("boom")))
                .isFalse();
        assertThat(ServiceFaults.isTooManyOperations(new CompletionException(new RuntimeException("boom"))))
                .isFalse();
    }

    @Test
    void statusOf_namesTheFault_emptyOtherwise() {
        assertThat(ServiceFaults.statusOf(serviceFault(StatusCodes.Bad_TooManyOperations)))
                .contains("Bad_TooManyOperations");
        assertThat(ServiceFaults.statusOf(new RuntimeException("boom"))).isEmpty();
        assertThat(ServiceFaults.statusOf(null)).isEmpty();
    }

    @Test
    void isNoContinuationPoints() {
        assertThat(ServiceFaults.isNoContinuationPoints(new StatusCode(StatusCodes.Bad_NoContinuationPoints)))
                .isTrue();
        assertThat(ServiceFaults.isNoContinuationPoints(new StatusCode(StatusCodes.Bad_ContinuationPointInvalid)))
                .isFalse();
        assertThat(ServiceFaults.isNoContinuationPoints(StatusCode.GOOD)).isFalse();
        assertThat(ServiceFaults.isNoContinuationPoints(null)).isFalse();
    }

    @Test
    void hasContinuationPoint_onlyForANonEmptyCursor() {
        final ReferenceDescription[] none = new ReferenceDescription[0];
        assertThat(ServiceFaults.hasContinuationPoint(
                        new BrowseResult(StatusCode.GOOD, ByteString.of(new byte[] {1}), none)))
                .isTrue();
        assertThat(ServiceFaults.hasContinuationPoint(new BrowseResult(StatusCode.GOOD, ByteString.NULL_VALUE, none)))
                .isFalse();
        assertThat(ServiceFaults.hasContinuationPoint(
                        new BrowseResult(StatusCode.GOOD, ByteString.of(new byte[0]), none)))
                .isFalse();
        assertThat(ServiceFaults.hasContinuationPoint(new BrowseResult(StatusCode.GOOD, null, none)))
                .isFalse();
    }

    @Test
    void continuationPointsOf_collectsEveryCursorWhateverTheStatus() {
        final ReferenceDescription[] none = new ReferenceDescription[0];
        final ByteString a = ByteString.of(new byte[] {1});
        final ByteString b = ByteString.of(new byte[] {2});
        final BrowseResult[] results = {
            new BrowseResult(StatusCode.GOOD, a, none),
            new BrowseResult(StatusCode.GOOD, ByteString.NULL_VALUE, none),
            null,
            new BrowseResult(new StatusCode(StatusCodes.Bad_NodeIdUnknown), b, none),
        };

        assertThat(ServiceFaults.continuationPointsOf(results)).containsExactly(a, b);
        assertThat(ServiceFaults.continuationPointsOf(null)).isEmpty();
        assertThat(ServiceFaults.continuationPointsOf(new BrowseResult[0])).isEmpty();
    }
}
