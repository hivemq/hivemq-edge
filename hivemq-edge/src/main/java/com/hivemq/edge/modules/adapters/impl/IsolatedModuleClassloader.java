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

import com.google.common.collect.ImmutableSet;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.services.EdgeServices;
import com.hivemq.extension.sdk.api.services.Services;
import com.hivemq.extension.sdk.api.services.builder.Builders;

import java.net.URL;
import java.net.URLClassLoader;

//This is a parent first class loader
public class IsolatedModuleClassloader extends URLClassLoader {

    private static final @NotNull ImmutableSet<String> RESTRICTED_CLASSES = new ImmutableSet.Builder<String>().add(
            Services.class.getCanonicalName(),
            Builders.class.getCanonicalName(),
            EdgeServices.class.getCanonicalName()).build();

    public IsolatedModuleClassloader(final URL @NotNull [] classpath, final @NotNull ClassLoader parent) {
        super(classpath, parent);
    }

    @Override
    protected synchronized @NotNull Class<?> loadClass(final @NotNull String name, final boolean resolve)
            throws ClassNotFoundException {
        // first, check if the class has already been loaded
        Class<?> c = findLoadedClass(name);
        if (c != null) {
            return c;
        }

        if (RESTRICTED_CLASSES.contains(name)) {
            throw new ClassNotFoundException(
                    "HiveMQ Extension SDK services or builders can not be accessed from the module SDK");
        }

        try {
            return super.loadClass(name, resolve);
        } catch (ClassNotFoundException | NoClassDefFoundError e) {
            // load from isolated
            c = findClass(name);

            if (resolve) {
                resolveClass(c);
            }
            return c;
        }
    }
}
