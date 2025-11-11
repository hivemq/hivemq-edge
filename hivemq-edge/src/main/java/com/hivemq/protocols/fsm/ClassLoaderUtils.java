/*
 *  Copyright 2019-present HiveMQ GmbH
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.hivemq.protocols.fsm;

import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

public final class ClassLoaderUtils {
    private ClassLoaderUtils() {
    }

    public static <T> @NotNull T runWithContextLoader(
            final @NotNull ClassLoader contextLoader,
            final @NotNull Supplier<T> wrapperSupplier) {
        final ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(contextLoader);
            return wrapperSupplier.get();
        } finally {
            Thread.currentThread().setContextClassLoader(contextClassLoader);
        }
    }
}
