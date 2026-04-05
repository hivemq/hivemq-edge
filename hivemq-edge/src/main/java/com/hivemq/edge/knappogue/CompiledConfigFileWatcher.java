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
package com.hivemq.edge.knappogue;

import com.hivemq.configuration.EnvironmentVariables;
import com.hivemq.edge.compiler.lib.model.CompiledConfig;
import com.hivemq.edge.compiler.lib.serialization.CompiledConfigSerializer;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Watches the compiled config JSON file specified by {@code HIVEMQ_COMPILED_CONFIG_PATH} and applies it via
 * {@link CompiledConfigApplier} whenever the file changes.
 *
 * <p>This is the Helm/Kubernetes delivery path: the Knappogue compiler produces {@code compiled-config.json},
 * which is injected into a Kubernetes ConfigMap and mounted into the container. When {@code helm upgrade} updates
 * the ConfigMap, Kubernetes updates the mounted file, and this watcher picks up the change and hot-reloads
 * the adapter and combiner configuration — no pod restart required.
 *
 * <p>The watcher polls on the same interval as the XML config fragment watcher
 * ({@code HIVEMQ_CONFIG_REFRESHINTERVAL}, default 1000 ms). If {@code HIVEMQ_COMPILED_CONFIG_PATH} is not set,
 * the watcher is disabled.
 *
 * <p>On startup, if the file already exists, it is applied immediately before polling begins.
 */
public class CompiledConfigFileWatcher {

    private static final @NotNull Logger log = LoggerFactory.getLogger(CompiledConfigFileWatcher.class);
    private static final long DEFAULT_INTERVAL_MS = 1000L;

    private final @NotNull CompiledConfigApplier applier;
    private final @NotNull CompiledConfigSerializer serializer;
    private final @Nullable Path configPath;
    private final long intervalMs;

    private @Nullable FileTime lastModified;

    public CompiledConfigFileWatcher(
            final @NotNull CompiledConfigApplier applier, final @NotNull CompiledConfigSerializer serializer) {
        this.applier = applier;
        this.serializer = serializer;
        this.configPath = resolveConfigPath();
        this.intervalMs = resolveIntervalMs();
    }

    /**
     * Starts the file watcher. If {@code HIVEMQ_COMPILED_CONFIG_PATH} is not set, this is a no-op.
     * Applies the file immediately on startup if it already exists, then polls on a fixed schedule.
     */
    public void start() {
        if (configPath == null) {
            log.debug("HIVEMQ_COMPILED_CONFIG_PATH not set — compiled config file watcher disabled.");
            return;
        }
        log.info("Watching compiled config file: {} (poll interval: {}ms)", configPath, intervalMs);
        poll();
        final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            final Thread t = new Thread(r, "compiled-config-file-watcher");
            t.setDaemon(true);
            return t;
        });
        var unused = scheduler.scheduleAtFixedRate(this::poll, intervalMs, intervalMs, TimeUnit.MILLISECONDS);
    }

    private void poll() {
        try {
            if (!Files.exists(configPath)) {
                return;
            }
            final FileTime current = Files.getLastModifiedTime(configPath);
            if (current.equals(lastModified)) {
                return;
            }
            lastModified = current;
            final String json = Files.readString(configPath);
            final CompiledConfig compiledConfig = serializer.fromJson(json);
            applier.apply(compiledConfig);
        } catch (final IOException e) {
            log.error("Failed to read compiled config file '{}': {}", configPath, e.getMessage());
        } catch (final Exception e) {
            log.error("Failed to parse or apply compiled config from '{}': {}", configPath, e.getMessage());
            if (log.isDebugEnabled()) {
                log.debug("Original exception:", e);
            }
        }
    }

    private static @Nullable Path resolveConfigPath() {
        final String path = System.getenv(EnvironmentVariables.COMPILED_CONFIG_PATH);
        return (path != null && !path.isBlank()) ? Path.of(path) : null;
    }

    private static long resolveIntervalMs() {
        final String raw = System.getenv(EnvironmentVariables.CONFIG_REFRESH_INTERVAL);
        if (raw != null) {
            try {
                return Long.parseLong(raw);
            } catch (final NumberFormatException e) {
                log.warn(
                        "Invalid value for {}: '{}' — using default {}ms",
                        EnvironmentVariables.CONFIG_REFRESH_INTERVAL,
                        raw,
                        DEFAULT_INTERVAL_MS);
            }
        }
        return DEFAULT_INTERVAL_MS;
    }
}
