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
package com.hivemq.edge.modules.adapters.impl;

import com.google.common.collect.ImmutableList;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.util.List;

public class ProtocolAdapterLoader {

    private static final Logger log = LoggerFactory.getLogger(ProtocolAdapterLoader.class);

    public static @NotNull List<IsolatedExtensionClassloaderHolder> loadModuleJar(final @NotNull File moduleFolder) {
        final ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
        final ImmutableList.Builder<IsolatedExtensionClassloaderHolder> classloaders = ImmutableList.builder();

        final File[] libs = moduleFolder.listFiles();
        if (libs != null) {
            for (final File lib : libs) {
                try {
                    if (lib.getName().endsWith(".jar")) {
                        log.debug("Found module jar {}.", lib.getAbsolutePath());
                        final IsolatedModuleClassloader isolatedModuleClassloader =
                                new IsolatedModuleClassloader(new URL[]{lib.toURI().toURL()}, contextClassLoader);
                        classloaders.add(new IsolatedExtensionClassloaderHolder(isolatedModuleClassloader,
                                lib.toPath()));
                    } else {
                        log.trace("Ignoring non jar file in module folder {}.", lib.getAbsolutePath());
                    }
                } catch (final IOException ioException) {
                    log.warn("Exception while reading module file {}, reason: {}",
                            lib.getAbsolutePath(),
                            ioException.getMessage());
                    log.debug("Original exception", ioException);
                }
            }
        }
        return classloaders.build();
    }

    public static class IsolatedExtensionClassloaderHolder {

        private final @NotNull IsolatedModuleClassloader classloader;
        private final @NotNull Path path;

        public IsolatedExtensionClassloaderHolder(
                final @NotNull IsolatedModuleClassloader classloader, final @NotNull Path path) {
            this.classloader = classloader;
            this.path = path;
        }

        public @NotNull IsolatedModuleClassloader getClassloader() {
            return classloader;
        }

        public @NotNull Path getPath() {
            return path;
        }
    }
}
