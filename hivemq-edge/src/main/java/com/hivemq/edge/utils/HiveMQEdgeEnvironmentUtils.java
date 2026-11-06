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
package com.hivemq.edge.utils;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author Simon L Johnson
 */
public class HiveMQEdgeEnvironmentUtils {

    /** Used until the saved installation id is loaded, and when usage tracking is off: one per JVM, never the MAC. */
    private static class InstallationTokenHolder {
        static final @NotNull String VALUE = UUID.randomUUID().toString();
    }

    /** The saved installation id ({@link InstallationId}), set once at startup when usage tracking is on. */
    private static volatile @Nullable String installationToken;

    private static class SessionTokenHolder {
        static final @NotNull String VALUE = UUID.randomUUID().toString();
    }

    public static @NotNull String generateInstallationToken() {
        final String token = installationToken;
        return token != null ? token : InstallationTokenHolder.VALUE;
    }

    /**
     * @param token the installation id every phone-home event carries from now on, read from the data folder by
     *     {@link InstallationId#loadOrCreate}
     */
    public static void installationToken(final @NotNull String token) {
        installationToken = token;
    }

    public static @NotNull String getSessionToken() {
        return SessionTokenHolder.VALUE;
    }

    public static @NotNull Map<String, String> generateEnvironmentMap() {

        Map<String, String> map = new HashMap<>();
        map.put("vm", System.getProperty("java.vm.version"));
        map.put("os.arch", System.getProperty("os.arch"));
        map.put("os.name", System.getProperty("os.name"));
        map.put("os.version", System.getProperty("os.version"));
        map.put("processor.count", Runtime.getRuntime().availableProcessors() + "");
        map.put("max.memory", Runtime.getRuntime().maxMemory() + "");
        return map;
    }
}
