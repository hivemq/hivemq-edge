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
package com.hivemq.protocols.v2.tag;

import com.hivemq.protocols.v2.fsm.FSMTransitionTable;
import org.jetbrains.annotations.NotNull;

/**
 * The five shared pre-operating transition rows (design §7.2), built <b>once</b> and reused by every aspect
 * variant — the polled and subscribed read tables ({@link TagAspectReadTransitions}) and the write table
 * ({@link TagAspectWriteTransitions}). The rows are parameterized by the variant's pre-operating state constants
 * and by the role-specific "verified" entry reached through {@link TagAspectVerifying#enterVerified()}, so engine
 * reuse is achieved without a shared state enum (the {@code FSM} still has exactly one state type per variant).
 * <p>
 * The rows act through the {@link TagAspectVerifying} context — the small contract both aspect classes implement —
 * so a {@code ContextType} of {@link TagAspectRead} or {@link TagAspectWrite} both satisfy it. Goal and
 * adapter-readiness changes never reach these rows; they are applied directly by the aspect (design §7.1, §7.2).
 */
final class TagAspectPreOperatingTransitions {

    private TagAspectPreOperatingTransitions() {}

    /**
     * Add the five shared pre-operating rows to {@code builder}, parameterized by the variant's "verified" target —
     * the state the aspect begins operating in. Success enters that state through
     * {@link TagAspectVerifying#enterVerified()}, which performs the role-specific kickoff.
     *
     * @param builder                     the table builder to add the rows to.
     * @param waitingForVerification       the variant's verifying state.
     * @param waitingForVerificationRetry  the variant's verification-retry state.
     * @param errorPermanent               the variant's permanent-verification-failure state.
     * @param <ContextType>               the aspect context the rows act through.
     */
    static <ContextType extends TagAspectVerifying> void addPreOperatingRows(
            final @NotNull FSMTransitionTable.Builder<TagAspectState, TagAspectEvent, ContextType> builder,
            final @NotNull TagAspectState waitingForVerification,
            final @NotNull TagAspectState waitingForVerificationRetry,
            final @NotNull TagAspectState errorPermanent) {
        builder.on(waitingForVerification, TagAspectEvent.VerifySucceeded.class)
                .then((current, event, aspect) -> aspect.enterVerified());
        builder.on(waitingForVerification, TagAspectEvent.VerifyTransientlyFailed.class)
                .then((current, event, aspect) -> {
                    aspect.onTransientVerificationFailure(reasonOf(event));
                    return waitingForVerificationRetry;
                });
        builder.on(waitingForVerification, TagAspectEvent.VerifyPermanentlyFailed.class)
                .then((current, event, aspect) -> {
                    aspect.onPermanentVerificationFailure(reasonOf(event));
                    return errorPermanent;
                });
        builder.on(waitingForVerificationRetry, TagAspectEvent.VerificationRetryElapsed.class)
                .then((current, event, aspect) -> {
                    aspect.requestVerification();
                    return waitingForVerification;
                });
    }

    /**
     * @param event a failure-carrying event.
     * @return the event's failure reason, or a generic fallback for an event that carries none.
     */
    static @NotNull String reasonOf(final @NotNull TagAspectEvent event) {
        if (event instanceof final TagAspectEvent.VerifyTransientlyFailed transientlyFailed) {
            return transientlyFailed.reason();
        }
        if (event instanceof final TagAspectEvent.VerifyPermanentlyFailed permanent) {
            return permanent.reason();
        }
        if (event instanceof final TagAspectEvent.NodeFailed failed) {
            return failed.reason();
        }
        if (event instanceof final TagAspectEvent.WriteFailed writeFailed) {
            return writeFailed.reason();
        }
        return "failure";
    }
}
