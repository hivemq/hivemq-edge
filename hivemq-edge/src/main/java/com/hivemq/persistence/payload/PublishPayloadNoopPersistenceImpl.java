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
package com.hivemq.persistence.payload;

import com.google.common.collect.ImmutableMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

/**
 * @author Daniel Krüger
 */
@Singleton
public class PublishPayloadNoopPersistenceImpl implements PublishPayloadPersistence {

    @Inject
    public PublishPayloadNoopPersistenceImpl() {
    }

    @Override
    public void init() {
    }

    @Override
    public void add(final byte @NotNull [] payload, final long id) {
        //NOOP
    }

    @Override
    public byte @Nullable [] get(final long id) {
        throw new UnsupportedOperationException("With in-memory payloads must not be gotten.");
    }

    @Override
    public void incrementReferenceCounterOnBootstrap(final long payloadId) {
        //NOOP
    }

    @Override
    public void decrementReferenceCounter(final long id) {
        //NOOP
    }

    @Override
    public void closeDB() {
        //NOOP
    }

    @Override
    public @NotNull ImmutableMap<Long, Integer> getReferenceCountersAsMap() {
        throw new UnsupportedOperationException("With in-memory payloads must not be gotten.");
    }
}
