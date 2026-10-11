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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.eclipse.milo.opcua.sdk.client.AddressSpace;
import org.eclipse.milo.opcua.sdk.client.OpcUaClient;
import org.eclipse.milo.opcua.stack.core.NodeIds;
import org.eclipse.milo.opcua.stack.core.StatusCodes;
import org.eclipse.milo.opcua.stack.core.UaException;
import org.eclipse.milo.opcua.stack.core.types.builtin.ExpandedNodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.LocalizedText;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.QualifiedName;
import org.eclipse.milo.opcua.stack.core.types.enumerated.NodeClass;
import org.eclipse.milo.opcua.stack.core.types.structured.ReferenceDescription;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

/// What a tag gets when reading a method's argument list fails.
///
/// **EDG-1087 M5.** A read that fails says nothing about the tag: a timeout, an access refusal, a
/// `Bad_TooManyOperations` from a busy server. It used to answer `Rejected`, which {@link MethodCache} keeps for
/// the session -- so one timed-out read made a tag unusable until the session was replaced, and an independent
/// client could call the method meanwhile.
///
/// What settles it is an inconsistency rather than a doctrine about retrying. The browse of the object one
/// level up already answers a transport failure with `Unverifiable`, which the cache drops. Same device, same
/// class of fault, and the two differed only in which code path happened to meet it.
///
/// **No promise about when a retry happens.** The cache does not keep this outcome, so a later request finding
/// no entry asks again; a reconnect clears everything regardless. Neither is a guarantee, and the reference
/// document deliberately states none.
class FailedSignatureReadTest {

    private static final @NotNull NodeId OBJECT = new NodeId(2, "Boiler1");

    private static @NotNull ReferenceDescription methodReference() {
        return new ReferenceDescription(
                NodeIds.HasComponent,
                true,
                ExpandedNodeId.parse("nsu=urn:test;s=Boiler1.Square"),
                new QualifiedName(2, "Square"),
                LocalizedText.english("Square"),
                NodeClass.Method,
                NodeIds.BaseObjectType.expanded());
    }

    /// A client whose node resolution fails the way a busy or unreachable server makes it fail.
    private static @NotNull OpcUaClient clientFailingWith(final @NotNull UaException failure) {
        final AddressSpace addressSpace = mock(AddressSpace.class);
        when(addressSpace.toNodeIdAsync(any(ExpandedNodeId.class))).thenReturn(CompletableFuture.failedFuture(failure));

        final OpcUaClient client = mock(OpcUaClient.class);
        when(client.getAddressSpace()).thenReturn(addressSpace);
        return client;
    }

    @Test
    void aTimedOutRead_isUnverifiableSoTheNextRequestAsksAgain() {
        final OpcUaClient client = clientFailingWith(new UaException(StatusCodes.Bad_Timeout, "timed out"));

        final MethodVerifier.Result result = MethodVerifier.readSignatures(
                        client, List.of(methodReference()), OBJECT, false, OBJECT, "boiler")
                .join();

        assertThat(result)
                .as("a read that failed is not a verdict about the tag")
                .isInstanceOf(MethodVerifier.Result.Unverifiable.class);
    }

    @Test
    void aServerRefusingForLoad_isAlsoUnverifiable() {
        // Bad_TooManyOperations is the one the reviewer named: a busy server declining work, which says nothing
        // about whether the tag is correct and is precisely the condition that clears on its own.
        final OpcUaClient client =
                clientFailingWith(new UaException(StatusCodes.Bad_TooManyOperations, "too many operations"));

        final MethodVerifier.Result result = MethodVerifier.readSignatures(
                        client, List.of(methodReference()), OBJECT, false, OBJECT, "boiler")
                .join();

        assertThat(result).isInstanceOf(MethodVerifier.Result.Unverifiable.class);
    }

    @Test
    void theReasonNamesTheTagAndTheCause() {
        // An operator has to be able to tell an access refusal from a server that went away, which is the whole
        // value of carrying the cause rather than a fixed sentence.
        final OpcUaClient client =
                clientFailingWith(new UaException(StatusCodes.Bad_UserAccessDenied, "access denied"));

        final MethodVerifier.Result result = MethodVerifier.readSignatures(
                        client, List.of(methodReference()), OBJECT, false, OBJECT, "boiler")
                .join();

        assertThat(((MethodVerifier.Result.Unverifiable) result).reason())
                .contains("boiler")
                .contains("access denied");
    }

    @Test
    void theCacheDoesNotKeepIt() {
        // The half that matters to a caller: the classification only changes anything because the cache treats
        // the two outcomes differently. Asserted together so neither can be changed without the other.
        final MethodCache cache = new MethodCache();
        final OpcUaClient client = clientFailingWith(new UaException(StatusCodes.Bad_Timeout, "timed out"));

        final MethodVerifier.Result first = cache.get(
                        "boiler",
                        () -> MethodVerifier.readSignatures(
                                client, List.of(methodReference()), OBJECT, false, OBJECT, "boiler"))
                .join();
        assertThat(first).isInstanceOf(MethodVerifier.Result.Unverifiable.class);

        // A second request, after the first settled, must reach the read rather than the cache.
        final boolean readAgain[] = {false};
        cache.get("boiler", () -> {
                    readAgain[0] = true;
                    return CompletableFuture.completedFuture(
                            (MethodVerifier.Result) MethodVerifier.Result.Verified.of(List.of()));
                })
                .join();

        assertThat(readAgain[0])
                .as("the tag must not stay unusable for the session because of one failed read")
                .isTrue();
    }
}
