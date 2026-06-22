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
 * How disruptively one piece of adapter configuration must be applied — the layer that would refine a
 * {@link ProtocolAdapterConfigStateTransition#FULL_RECREATE} into a gentler reconnect window where the protocol allows it (design §8.2).
 * <p>
 * This enum is the documented seam the primary design reserves; the manager treats every {@code FULL_RECREATE} as a
 * stop / discard / recreate today (the credential-rotation-via-disconnect-window optimization and the full
 * per-field dependency handling stay a documented stub, design §8.2). It is defined here so that refinement can be
 * added later without touching the transition classification or the manager's reconcile loop.
 */
public enum ProtocolAdapterConfigState {

    /**
     * Baked into the adapter instance at construction (for example the protocol id or the adapter configuration) —
     * a change requires a full recreate.
     */
    FACTORY_BAKED_IN,

    /**
     * Changeable while connected by cycling the connection (for example a credential rotation) — applicable in a
     * disconnect/reconnect window rather than a full recreate. Reserved; not yet exploited.
     */
    HOT_AT_DISCONNECT_WINDOW,

    /**
     * Changeable only while the adapter is stopped. Reserved; not yet exploited.
     */
    HOT_WHILE_STOPPED,

    /**
     * Changeable at runtime with no connection impact (for example activation flags or the tag set) — already
     * handled by {@link ProtocolAdapterConfigStateTransition#ACTIVATION_ONLY} / {@link ProtocolAdapterConfigStateTransition#TAGS_ONLY}.
     */
    TRULY_LIVE
}
