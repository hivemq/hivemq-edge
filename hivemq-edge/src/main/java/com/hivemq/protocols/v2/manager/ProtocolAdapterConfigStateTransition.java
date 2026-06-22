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
package com.hivemq.protocols.v2.manager;

/**
 * The class of difference between an adapter's running configuration and a freshly-loaded one — the
 * <b>gentlest correct transition</b> the manager applies on reload (design §8.2). The classes are ordered from
 * gentlest to most disruptive; {@link ProtocolAdapterConfigDiffUtils} chooses the gentlest one that still brings the running
 * adapter fully in line with the new configuration.
 */
public enum ProtocolAdapterConfigStateTransition {

    /**
     * The new configuration is identical to the running one — nothing to do.
     */
    NO_CHANGE,

    /**
     * Only activation flags changed — the adapter {@code northbound-activated} / {@code southbound-activated}
     * and/or a tag's {@code read-activated} / {@code write-activated}. Applied as one atomic
     * {@link com.hivemq.protocols.v2.wrapper.ProtocolAdapterWrapperCommand.ApplyActivation} to the running
     * wrapper: no reconnect, no re-verification of unaffected aspects (design §8.2, EDG-462).
     */
    ACTIVATION_ONLY,

    /**
     * The tag set changed (tags added, removed, or edited beyond their activation flags) or the mappings changed
     * (so {@code used} is recomputed). Applied as one atomic
     * {@link com.hivemq.protocols.v2.wrapper.ProtocolAdapterWrapperCommand.UpdateTagSet} to the running wrapper,
     * which diffs in place — never reconnects (design §8.2).
     */
    TAGS_ONLY,

    /**
     * Anything else — a change to the protocol id, the adapter configuration, the retry / watchdog / command
     * timeouts, the skip-verification flag, or the config version. The wrapper is stopped, the protocol adapter is
     * discarded, and a fresh pair is created and started (design §8.2). The
     * {@link ProtocolAdapterConfigState} refinement (a credential rotation handled in a disconnect/reconnect
     * window rather than a full recreate) is a documented stub in this project.
     */
    FULL_RECREATE
}
