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
package com.hivemq.configuration.reader;

import com.hivemq.configuration.entity.HiveMQConfigEntity;
import com.hivemq.configuration.entity.api.ApiTlsEntity;
import com.hivemq.configuration.entity.bridge.BridgeTlsEntity;
import com.hivemq.configuration.entity.listener.tls.KeystoreEntity;
import com.hivemq.configuration.entity.listener.tls.TruststoreEntity;
import com.hivemq.edge.HiveMQEdgeConstants;
import com.hivemq.util.ThreadFactoryUtil;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributeView;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.function.Function;
import java.util.function.Supplier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.VisibleForTesting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Watches the configuration file for changes and reloads it through {@link ConfigFileReaderWriter}: a
 * scheduled check of the modification times of the file (or the mounted fragment in a container), of
 * the fragments it pulls in and of the keystores it names, the reload under the configuration lock, and
 * the restart decision when a change cannot be applied live (EDG-949).
 * <p>
 * Owns everything the check needs and nothing else: the scheduler, the last-known times, the set of
 * watched fragments -- replaced by the reader on every accepted load -- and the stamp the reader leaves
 * after a write of its own, so the node's own REST write is not re-read as an operator change.
 */
final class ConfigFileWatcher {

    private static final @NotNull Logger log = LoggerFactory.getLogger(ConfigFileWatcher.class);
    private static final long STOP_TIMEOUT_SECONDS = 5;

    static final @NotNull String CONFIG_FRAGMENT_PATH = "/fragment/config";

    private final @NotNull Lock lock;
    private final @NotNull ConfigurationFile configFile;
    private final @NotNull Function<File, ConfigFileReaderWriter.ReloadOutcome> reload;
    private final @NotNull ConcurrentMap<Path, Long> fragmentToModificationTime;
    private final @NotNull AtomicReference<ScheduledExecutorService> executorService;
    /**
     * What the configuration watcher does when the node has to restart to apply a change. The process
     * exit in production; replaced in tests so the decision can be observed instead of ending the JVM.
     */
    // On its own thread: exiting from the tick would leave the tick blocked in Runtime.exit while the JVM
    // runs the shutdown hooks -- one of which is stop(), waiting for the tick to finish -- and with the
    // configuration lock held for the whole shutdown.
    private volatile @NotNull Runnable restartHook =
            () -> new Thread(() -> System.exit(0), "hivemq-edge-config-restart").start();
    /**
     * The modification time of the configuration file as the watcher last knew it. Set by the watcher
     * when it starts and on every reload, and by {@link #writtenByThisNode(Path, long)} after
     * a write of this node's own -- so the watcher does not re-read, re-parse and re-apply a file it has
     * just written itself after every REST change (EDG-949 review). Only consulted when no
     * {@code /fragment/config} exists; in a container the watcher compares the fragment instead.
     */
    private final @NotNull AtomicLong watchedConfigModified = new AtomicLong();

    /**
     * @param lock       the configuration-transition lock; the check and the reload run under it
     * @param configFile the configured configuration file, to tell the node's own write from any other
     * @param reload     loads and applies the file, answering how it went
     */
    ConfigFileWatcher(
            final @NotNull Lock lock,
            final @NotNull ConfigurationFile configFile,
            final @NotNull Function<File, ConfigFileReaderWriter.ReloadOutcome> reload) {
        this.lock = lock;
        this.configFile = configFile;
        this.reload = reload;
        this.fragmentToModificationTime = new ConcurrentHashMap<>();
        this.executorService = new AtomicReference<>();
    }

    @VisibleForTesting
    void setRestartHook(final @NotNull Runnable restartHook) {
        this.restartHook = restartHook;
    }

    /**
     * The fragments the configuration now pulls in, replacing the previous set: a fragment taken out of
     * the configuration and deleted from disk must not stay watched (EDG-949).
     */
    void watchFragments(final @NotNull Map<Path, Long> fragments) {
        fragmentToModificationTime.keySet().retainAll(fragments.keySet());
        fragmentToModificationTime.putAll(fragments);
    }

    /**
     * Records the time a write of this node's own carries onto the configuration file, so the next check
     * does not read it as a change -- only where the check compares that file, see
     * {@link #comparesThisFile(Path)}.
     *
     * @param target       the file that was replaced
     * @param modifiedTime the modification time the replacement carries, read before it was moved
     */
    void writtenByThisNode(final @NotNull Path target, final long modifiedTime) {
        if (modifiedTime >= 0 && comparesThisFile(target)) {
            watchedConfigModified.set(modifiedTime);
        }
    }

    /**
     * Whether the watcher decides "changed" by this file's own modification time. It does so only for the
     * configured configuration file, and only where no {@code /fragment/config} exists: in a container
     * the watcher compares the fragment, which this node never writes, so a stamp taken from
     * config.xml would be a foreign value in that comparison and every REST write would read as a
     * change (EDG-949 review).
     */
    private boolean comparesThisFile(final @NotNull Path written) {
        if (new File(CONFIG_FRAGMENT_PATH).exists()) {
            return false;
        }
        return configFile
                .file()
                .map(configured -> {
                    try {
                        return Files.isSameFile(configured.toPath(), written);
                    } catch (final IOException e) {
                        return false;
                    }
                })
                .orElse(false);
    }

    private static @NotNull Map<Path, Long> findFilesToWatch(final @NotNull HiveMQConfigEntity entity) {
        final Map<Path, Long> paths = new ConcurrentHashMap<>();
        entity.getBridgeConfig().forEach(cfg -> {
            final BridgeTlsEntity tls = cfg.getRemoteBroker().getTls();
            if (tls != null) {
                final KeystoreEntity keyStore = tls.getKeyStore();
                if (keyStore != null) {
                    final Path path = Paths.get(keyStore.getPath());
                    try {
                        paths.put(path, Files.getLastModifiedTime(path).toMillis());
                    } catch (final IOException e) {
                        throw new RuntimeException(e);
                    }
                }
                final TruststoreEntity trustStore = tls.getTrustStore();
                if (trustStore != null) {
                    final Path path = Paths.get(trustStore.getPath());
                    try {
                        paths.put(path, Files.getLastModifiedTime(path).toMillis());
                    } catch (final IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        });
        final ApiTlsEntity tls = entity.getApiConfig().getTls();
        if (tls != null && tls.getKeystoreEntity() != null) {
            final Path path = Paths.get(tls.getKeystoreEntity().getPath());
            try {
                paths.put(path, Files.getLastModifiedTime(path).toMillis());
            } catch (final IOException e) {
                throw new RuntimeException(e);
            }
        }
        return paths;
    }

    void start(
            final @NotNull File configFile,
            final long interval,
            final @NotNull Supplier<HiveMQConfigEntity> entitySupplier) {
        if (executorService.compareAndSet(
                null,
                Executors.newSingleThreadScheduledExecutor(ThreadFactoryUtil.create("hivemq-edge-config-watch-%d")))) {

            final HiveMQConfigEntity entity = entitySupplier.get();
            final Map<Path, Long> fileModificationTimestamps = findFilesToWatch(entity);
            final AtomicLong fileModified = watchedConfigModified;
            try {
                fileModified.set(watchedTimestamp(configFile));
            } catch (final IOException e) {
                throw new RuntimeException("Unable to read last modified time from " + configFile.getAbsolutePath(), e);
            }

            log.info("Rereading config file every {} ms", interval);
            // executorService was just set via compareAndSet, so it cannot be null here
            final ScheduledExecutorService scheduler = Objects.requireNonNull(executorService.get());
            scheduler.scheduleAtFixedRate(
                    () -> {
                        // A scheduled task that throws is never run again, silently: the watcher was lost
                        // to the first unreadable or unparseable file and nothing said so (EDG-949).
                        // Whatever escapes the check is logged and the next tick runs.
                        try {
                            checkMonitoredFilesForChanges(configFile, fileModified, fileModificationTimestamps);
                        } catch (final Throwable t) {
                            log.error(
                                    "The configuration watcher failed to check {}; it will try again in {} ms",
                                    configFile,
                                    interval,
                                    t);
                        }
                    },
                    0,
                    interval,
                    TimeUnit.MILLISECONDS);
            Runtime.getRuntime().addShutdownHook(new Thread(this::stop));
        } else {
            throw new IllegalStateException("Config watch was already started");
        }
    }

    /**
     * Ends the watch and waits, bounded, for a tick in flight: a caller that removes the configuration
     * folder right after stopping -- an embedded node's teardown -- must not race the last check.
     */
    void stop() {
        final ScheduledExecutorService es = executorService.getAndSet(null);
        if (es != null) {
            es.shutdownNow();
            try {
                if (!es.awaitTermination(STOP_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                    log.debug(
                            "The configuration watcher did not finish its last check within {} s",
                            STOP_TIMEOUT_SECONDS);
                }
            } catch (final InterruptedException interrupted) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private void checkMonitoredFilesForChanges(
            final @NotNull File configFile,
            final @NotNull AtomicLong fileModified,
            final @NotNull Map<Path, Long> fileModificationTimestamps)
            throws IOException {
        final boolean isDevMode = "true".equals(System.getProperty(HiveMQEdgeConstants.DEVELOPMENT_MODE));
        if (!isDevMode) {
            final Map<Path, Long> pathsToCheck = new HashMap<>(fragmentToModificationTime);
            pathsToCheck.putAll(fileModificationTimestamps);
            for (final Map.Entry<Path, Long> watched : pathsToCheck.entrySet()) {
                final Path key = watched.getKey();
                if (key.toString().equals(CONFIG_FRAGMENT_PATH)) {
                    continue;
                }
                final long lastModified;
                try {
                    lastModified = Files.getFileAttributeView(
                                    key.toRealPath(LinkOption.NOFOLLOW_LINKS), BasicFileAttributeView.class)
                            .readAttributes()
                            .lastModifiedTime()
                            .toMillis();
                } catch (final NoSuchFileException gone) {
                    // A fragment or keystore that was removed. It is dropped from the watch rather than
                    // ending it: the configuration that referenced it will be re-read when it changes, and
                    // a file the node no longer has is not a reason to stop noticing the ones it does.
                    log.warn("The watched file {} no longer exists; it is no longer watched", key);
                    fragmentToModificationTime.remove(key);
                    fileModificationTimestamps.remove(key);
                    continue;
                } catch (final IOException unreadable) {
                    // Momentary: a network file system, or a tool rewriting the file. Next tick.
                    log.warn("Could not read the modification time of the watched file {}", key, unreadable);
                    continue;
                }
                if (lastModified != watched.getValue()) {
                    log.error("Restarting because a required file was updated: {}", key);
                    restartHook.run();
                    return;
                }
            }
        }

        // Under the configuration lock, which the write path holds from the move to the stamp: read
        // outside it, a tick could observe the moved file before the stamp and reload this node's own
        // write. The compare-and-set covers the same race for a stamp that lands between the two reads.
        lock.lock();
        try {
            final long known = fileModified.get();
            final long modified;
            try {
                modified = watchedTimestamp(configFile);
            } catch (final NoSuchFileException gone) {
                // The configuration file itself is missing: an operator replacing it by remove-and-copy, or
                // a test deleting the folder of a node it stopped. Nothing to compare against, so the applied
                // configuration stays and the file is checked again next tick; it reappears with a time of
                // its own, which the comparison below picks up.
                log.warn(
                        "The configuration file {} does not exist; keeping the applied configuration until it reappears",
                        configFile);
                return;
            }
            // Any change, not only a later time: a configuration restored from a backup -- cp -p,
            // rsync -a, a volume remount -- carries an older time than the broken file it replaces, and a
            // high-water mark would ignore the correction for good (EDG-949 review).
            if (modified == known) {
                return;
            }
            // Recorded before the load: a file that is rejected is not re-parsed on every tick, and the
            // operator's correction moves the time again. Compare-and-set, not set: if this node's own
            // write stamped the field since `known` was read, the time read above is that write's or
            // older, and the next tick compares against the stamp. Setting it back would make the next
            // tick reload the node's own write.
            if (!fileModified.compareAndSet(known, modified)) {
                return;
            }
            switch (reload.apply(configFile)) {
                case APPLIED -> {}
                case REJECTED_INVALID ->
                    // Deliberately not a restart: a node restarted onto a file that cannot be parsed does
                    // not come back. The node keeps the configuration it has, says so, and the watcher
                    // stays alive to pick up the correction -- which is what used to be lost (EDG-949).
                    log.error("Configuration reload rejected because the new configuration is invalid;"
                            + " keeping the previously applied configuration."
                            + " Fix the reported errors above and save the file again.");
                case NEEDS_RESTART -> {
                    if (!isDevMode) {
                        log.error("Restarting because new config can't be hot-reloaded");
                        restartHook.run();
                    } else {
                        log.error("TEST MODE, NOT RESTARTING");
                    }
                }
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * The time the watcher compares: the fragment's where a container mounts one, the configuration
     * file's own otherwise. One source for the seed and for every tick, so the first tick after start
     * does not read as a change.
     */
    private static long watchedTimestamp(final @NotNull File configFile) throws IOException {
        final File fragment = new File(CONFIG_FRAGMENT_PATH);
        if (fragment.exists()) {
            return Files.getLastModifiedTime(fragment.toPath()).toMillis();
        }
        log.warn("No fragment found, checking the full config, only used for testing");
        return Files.getLastModifiedTime(configFile.toPath()).toMillis();
    }
}
