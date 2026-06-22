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
 * The manager's own immutable health summary (design §8.3): adapter counts folded from the registry's snapshots on
 * each {@link ProtocolAdapterManagerMessage.ProtocolAdapterManagerTick}, published into an
 * {@code AtomicReference} the manager exposes. A pure function of the registry's snapshots — readers (the REST
 * surface, a later task) read it without locking.
 *
 * @param totalAdapters         the number of registered adapters.
 * @param connectedAdapters     adapters whose machine is {@code CONNECTED} (healthy and serving).
 * @param errorAdapters         adapters whose machine is {@code ERROR}.
 * @param stoppedAdapters       adapters whose machine is {@code STOPPED}.
 * @param transitioningAdapters adapters in any waiting state (connecting, retrying, or stopping).
 * @param lastUpdatedAtMillis   the tick time this summary was folded, in milliseconds.
 */
public record ProtocolAdapterManagerSnapshot(
        int totalAdapters,
        int connectedAdapters,
        int errorAdapters,
        int stoppedAdapters,
        int transitioningAdapters,
        long lastUpdatedAtMillis) {

    /**
     * @return an empty summary — no adapters registered yet.
     */
    public static ProtocolAdapterManagerSnapshot empty() {
        return new ProtocolAdapterManagerSnapshot(0, 0, 0, 0, 0, 0L);
    }
}
