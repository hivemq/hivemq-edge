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

import org.jetbrains.annotations.NotNull;

/**
 * The verification-lifecycle actions every tag aspect exposes — the seam the <b>shared pre-operating
 * transition rows</b> act through. Because both the read aspect ({@link TagAspectRead}) and the write aspect
 * ({@link TagAspectWrite}) move through the same five pre-operating states (verifying, retrying, permanently
 * failed), those rows are built <b>once</b> by {@link TagAspectPreOperatingTransitions} and parameterized by each
 * variant's state constants and its role-specific "verified" entry — engine reuse without a shared state enum.
 * <p>
 * Every method runs on the wrapper's single dispatch thread.
 */
interface TagAspectVerifying {

    /**
     * Verification succeeded: perform the role-specific kickoff (a read aspect schedules its first poll or
     * requests its subscription; a write aspect simply rests ready for writes) and return the state in which the
     * aspect begins operating.
     *
     * @return the operating-entry state for this aspect variant.
     */
    @NotNull
    TagAspectState enterVerified();

    /**
     * (Re-)verify the node through the shared verification authority — the action the verification-retry row runs.
     */
    void requestVerification();

    /**
     * A transient verification failure: count it and schedule a verification retry on the actor's timer queue.
     *
     * @param reason a human-readable description of the failure.
     */
    void onTransientVerificationFailure(@NotNull String reason);

    /**
     * A permanent verification failure: count it; the aspect is suspended until a user-commanded tag retry
     *.
     *
     * @param reason a human-readable description of the failure.
     */
    void onPermanentVerificationFailure(@NotNull String reason);
}
