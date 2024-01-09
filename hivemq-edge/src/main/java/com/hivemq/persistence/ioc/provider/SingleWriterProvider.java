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
package com.hivemq.persistence.ioc.provider;

import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extensions.core.PersistencesService;
import com.hivemq.persistence.InMemorySingleWriter;
import com.hivemq.persistence.SingleWriterService;
import com.hivemq.persistence.InFileSingleWriter;

import javax.inject.Inject;
import javax.inject.Provider;

public class SingleWriterProvider {

    private final @NotNull PersistencesService persistencesService;
    private final @NotNull Provider<InMemorySingleWriter> inMemorySingleWriterProvider;
    private final @NotNull Provider<InFileSingleWriter> singleWriterServiceProvider;

    @Inject
    public SingleWriterProvider(
            final @NotNull PersistencesService persistencesService,
            final @NotNull Provider<InMemorySingleWriter> inMemorySingleWriterProvider,
            final @NotNull Provider<InFileSingleWriter> singleWriterServiceProvider) {
        this.persistencesService = persistencesService;
        this.inMemorySingleWriterProvider = inMemorySingleWriterProvider;
        this.singleWriterServiceProvider = singleWriterServiceProvider;
    }

    public @NotNull SingleWriterService get() {
        if (persistencesService.isFilePersistencesPresent()) {
            return singleWriterServiceProvider.get();
        } else {
           return inMemorySingleWriterProvider.get();
        }
    }
}
