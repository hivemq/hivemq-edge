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
package com.hivemq.http.handlers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hivemq.common.shutdown.HiveMQShutdownHook;
import com.hivemq.common.shutdown.ShutdownHooks;
import org.jetbrains.annotations.NotNull;

import java.io.InputStream;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;


/**
 * Allow static consribution of dynamic classloaders to the HTTPD engine,
 * allowing modules to contribute arbitrary resources
 */
public class AlternativeClassloadingStaticFileHandler extends StaticFileHandler {
    protected static final @NotNull Set<ClassLoader> classLoaders = new HashSet<>();

    public AlternativeClassloadingStaticFileHandler(final ObjectMapper mapper, final String resourceRoot, final ShutdownHooks shutdownHooks) {
        super(mapper, resourceRoot);
        shutdownHooks.add(new HiveMQShutdownHook() {
            @Override
            public @NotNull String name() {
                return "AlternativeClassloadingStaticFileHandler - Shutdown";
            }

            @Override
            public void run() {
                classLoaders.clear();
            }
        });
    }

    public static void addClassLoader(final ClassLoader classLoader){
        classLoaders.add(classLoader);
    }

    @Override
    protected InputStream loadClasspathResource(final String resource) {
        InputStream inputStream = null;
        final Iterator<ClassLoader> loaderIterator = classLoaders.iterator();
        while(loaderIterator.hasNext() && inputStream == null){
            final ClassLoader classLoader = loaderIterator.next();
            inputStream = loadClasspathResourceInternal(classLoader, resource);
        }
        return inputStream;
    }

    protected static InputStream loadClasspathResourceInternal(final ClassLoader loader, final String resource) {
        InputStream is = loader.getResourceAsStream(resource);
        if (is == null) {
            is = loader.getResourceAsStream("/" + resource);
        }
        return is;
    }
}
