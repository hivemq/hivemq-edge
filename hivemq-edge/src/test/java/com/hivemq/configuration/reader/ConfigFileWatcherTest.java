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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ch.qos.logback.classic.Level;
import com.hivemq.configuration.entity.HiveMQConfigEntity;
import com.hivemq.configuration.entity.bridge.BridgeTlsEntity;
import com.hivemq.configuration.entity.bridge.MqttBridgeEntity;
import com.hivemq.configuration.entity.bridge.RemoteBrokerEntity;
import com.hivemq.configuration.entity.listener.tls.KeystoreEntity;
import com.hivemq.configuration.reader.ConfigFileReaderWriter.ReloadOutcome;
import com.hivemq.edge.HiveMQEdgeConstants;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileTime;
import java.util.ConcurrentModificationException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.BooleanSupplier;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.LoggerFactory;
import util.LogbackCapturingAppender;

/**
 * {@link ConfigFileWatcher} on its own, with a recording reload in place of the reader-writer: what
 * counts as a change, what each reload outcome does, what the node's own write does not do, how watched
 * fragments and keystores are handled, and that no failure ends the watch.
 */
public class ConfigFileWatcherTest {

    private static final long INTERVAL_MS = 50;

    @TempDir
    public File temporaryFolder;

    private @NotNull Path config;
    private final @NotNull ReentrantLock lock = new ReentrantLock();
    private final @NotNull AtomicInteger reloads = new AtomicInteger();
    private final @NotNull AtomicInteger restarts = new AtomicInteger();
    private final @NotNull AtomicReference<ReloadOutcome> outcome = new AtomicReference<>(ReloadOutcome.APPLIED);
    private final @NotNull AtomicReference<RuntimeException> reloadFailure = new AtomicReference<>();
    private @NotNull ConfigFileWatcher watcher;
    private @NotNull LogbackCapturingAppender logCapture;
    private @NotNull String previousDevMode;

    @BeforeEach
    public void setUp() throws IOException {
        config = temporaryFolder.toPath().resolve("config.xml");
        rewrite(config, "v0", 0);
        watcher = new ConfigFileWatcher(lock, new ConfigurationFile(config.toFile()), file -> {
            final RuntimeException failure = reloadFailure.get();
            if (failure != null) {
                throw failure;
            }
            reloads.incrementAndGet();
            return outcome.get();
        });
        watcher.setRestartHook(restarts::incrementAndGet);
        logCapture = LogbackCapturingAppender.Factory.weaveInto(LoggerFactory.getLogger(ConfigFileWatcher.class));
        previousDevMode = System.getProperty(HiveMQEdgeConstants.DEVELOPMENT_MODE, "");
        System.setProperty(HiveMQEdgeConstants.DEVELOPMENT_MODE, "false");
    }

    @AfterEach
    public void tearDown() {
        watcher.stop();
        System.setProperty(HiveMQEdgeConstants.DEVELOPMENT_MODE, previousDevMode);
        LogbackCapturingAppender.Factory.cleanUp();
    }

    private void start() {
        watcher.start(config.toFile(), INTERVAL_MS, HiveMQConfigEntity::new);
    }

    /** Staged and moved, so the watcher sees content and time at once; whole seconds clear any granularity. */
    private static void rewrite(final @NotNull Path file, final @NotNull String content, final int secondsLater)
            throws IOException {
        final Path staged = file.resolveSibling(file.getFileName() + ".staged");
        Files.writeString(staged, content);
        Files.setLastModifiedTime(staged, FileTime.fromMillis(System.currentTimeMillis() + secondsLater * 1000L));
        Files.move(staged, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
    }

    private static void await(final @NotNull String what, final @NotNull BooleanSupplier condition)
            throws InterruptedException {
        final long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(15);
        while (!condition.getAsBoolean()) {
            if (System.nanoTime() > deadline) {
                throw new AssertionError("timed out waiting for: " + what);
            }
            Thread.sleep(10);
        }
    }

    private static void ticks(final int count) throws InterruptedException {
        Thread.sleep(INTERVAL_MS * count);
    }

    private long logged(final @NotNull Level level, final @NotNull String fragment) {
        while (true) {
            try {
                return logCapture.getCapturedLogs().stream()
                        .filter(event -> event.getLevel() == level)
                        .filter(event -> event.getFormattedMessage().contains(fragment))
                        .count();
            } catch (final ConcurrentModificationException torn) {
                Thread.onSpinWait();
            }
        }
    }

    // ---- what counts as a change

    @Test
    @Timeout(30)
    public void anUnchangedFileIsNeverReloaded() throws Exception {
        start();
        ticks(6);
        assertThat(reloads).hasValue(0);
        assertThat(restarts).hasValue(0);
    }

    @Test
    @Timeout(30)
    public void aChangedFileIsReloadedExactlyOnce() throws Exception {
        start();
        rewrite(config, "v1", 2);
        await("the reload", () -> reloads.get() == 1);
        ticks(6);
        assertThat(reloads).hasValue(1);
    }

    @Test
    @Timeout(30)
    public void anOlderTimeIsAChangeToo() throws Exception {
        start();
        rewrite(config, "restored", -60);
        await("the reload of the older file", () -> reloads.get() == 1);
    }

    // ---- what each outcome does

    @Test
    @Timeout(30)
    public void aRejectedReloadIsReportedAndTheWatchGoesOn() throws Exception {
        outcome.set(ReloadOutcome.REJECTED_INVALID);
        start();
        rewrite(config, "broken", 2);
        await("the rejection to be reported", () -> logged(Level.ERROR, "reload rejected") == 1);
        assertThat(restarts).hasValue(0);

        outcome.set(ReloadOutcome.APPLIED);
        rewrite(config, "fixed", 4);
        await("the correction to be reloaded", () -> reloads.get() == 2);
    }

    @Test
    @Timeout(30)
    public void aReloadThatNeedsARestartRestarts() throws Exception {
        outcome.set(ReloadOutcome.NEEDS_RESTART);
        start();
        rewrite(config, "listener changed", 2);
        await("the restart decision", () -> restarts.get() == 1);
        assertThat(logged(Level.ERROR, "can't be hot-reloaded")).isEqualTo(1);
    }

    @Test
    @Timeout(30)
    public void inDevelopmentModeARestartIsOnlyLogged() throws Exception {
        System.setProperty(HiveMQEdgeConstants.DEVELOPMENT_MODE, "true");
        outcome.set(ReloadOutcome.NEEDS_RESTART);
        start();
        rewrite(config, "listener changed", 2);
        await("the reload", () -> reloads.get() == 1);
        await("the test-mode line", () -> logged(Level.ERROR, "TEST MODE") == 1);
        assertThat(restarts).hasValue(0);
    }

    // ---- the node's own write

    @Test
    @Timeout(30)
    public void aWriteStampedAsTheNodesOwnIsNotReloaded() throws Exception {
        start();
        final long stamped = System.currentTimeMillis() + 2000;
        lock.lock();
        try {
            rewrite(config, "own write", 2);
            Files.setLastModifiedTime(config, FileTime.fromMillis(stamped));
            watcher.writtenByThisNode(config, stamped);
        } finally {
            lock.unlock();
        }
        ticks(6);
        assertThat(reloads).as("the node's own write must not be re-read").hasValue(0);

        rewrite(config, "operator edit", 4);
        await("an operator edit after it is still seen", () -> reloads.get() == 1);
    }

    @Test
    @Timeout(30)
    public void aStampForAnotherFileIsIgnored() throws Exception {
        start();
        final Path other = temporaryFolder.toPath().resolve("other.xml");
        Files.writeString(other, "x");
        rewrite(config, "v1", 2);
        watcher.writtenByThisNode(other, Files.getLastModifiedTime(config).toMillis());
        await("the change is still reloaded", () -> reloads.get() == 1);
    }

    @Test
    @Timeout(30)
    public void aNegativeStampIsIgnored() throws Exception {
        start();
        rewrite(config, "v1", 2);
        watcher.writtenByThisNode(config, -1);
        await("the change is still reloaded", () -> reloads.get() == 1);
    }

    // ---- fragments and keystores

    @Test
    @Timeout(30)
    public void aChangedFragmentRestarts() throws Exception {
        final Path fragment = temporaryFolder.toPath().resolve("bridges.fragment");
        rewrite(fragment, "f0", 0);
        watcher.watchFragments(
                Map.of(fragment, Files.getLastModifiedTime(fragment).toMillis()));
        start();

        rewrite(fragment, "f1", 2);

        await("the restart for a changed fragment", () -> restarts.get() == 1);
        assertThat(logged(Level.ERROR, "required file was updated")).isEqualTo(1);
        assertThat(reloads).hasValue(0);
    }

    @Test
    @Timeout(30)
    public void aVanishedFragmentIsReportedOnceAndDropped() throws Exception {
        final Path fragment = temporaryFolder.toPath().resolve("bridges.fragment");
        rewrite(fragment, "f0", 0);
        watcher.watchFragments(
                Map.of(fragment, Files.getLastModifiedTime(fragment).toMillis()));
        start();

        Files.delete(fragment);

        await("the vanished fragment to be reported", () -> logged(Level.WARN, "no longer exists") == 1);
        ticks(10);
        assertThat(logged(Level.WARN, "no longer exists"))
                .as("reported once, not every tick")
                .isEqualTo(1);
        assertThat(restarts).hasValue(0);
        assertThat(logged(Level.ERROR, "watcher failed")).isZero();
    }

    @Test
    @Timeout(30)
    public void replacingTheWatchedFragmentsDropsTheOldOnes() throws Exception {
        final Path old = temporaryFolder.toPath().resolve("old.fragment");
        rewrite(old, "o", 0);
        watcher.watchFragments(Map.of(old, Files.getLastModifiedTime(old).toMillis()));
        watcher.watchFragments(Map.of()); // the next accepted configuration references no fragment
        start();

        rewrite(old, "changed", 2);

        ticks(6);
        assertThat(restarts)
                .as("a fragment no longer referenced must not be watched")
                .hasValue(0);
    }

    @Test
    @Timeout(30)
    public void aChangedKeystoreRestarts() throws Exception {
        final Path keystore = temporaryFolder.toPath().resolve("bridge.p12");
        rewrite(keystore, "k0", 0);
        final HiveMQConfigEntity entity = new HiveMQConfigEntity();
        final KeystoreEntity keystoreEntity = new KeystoreEntity();
        keystoreEntity.setPath(keystore.toString());
        final BridgeTlsEntity tls = new BridgeTlsEntity();
        tls.setKeyStore(keystoreEntity);
        final RemoteBrokerEntity remote = new RemoteBrokerEntity();
        remote.setTls(tls);
        final MqttBridgeEntity bridge = new MqttBridgeEntity();
        bridge.setRemoteBroker(remote);
        entity.getBridgeConfig().addAll(List.of(bridge));
        watcher.start(config.toFile(), INTERVAL_MS, () -> entity);

        rewrite(keystore, "k1", 2);

        await("the restart for a changed keystore", () -> restarts.get() == 1);
    }

    @Test
    @Timeout(30)
    public void aVanishedKeystoreIsReportedOnceAndDropped() throws Exception {
        final Path keystore = temporaryFolder.toPath().resolve("bridge.p12");
        rewrite(keystore, "k0", 0);
        final HiveMQConfigEntity entity = new HiveMQConfigEntity();
        final KeystoreEntity keystoreEntity = new KeystoreEntity();
        keystoreEntity.setPath(keystore.toString());
        final BridgeTlsEntity tls = new BridgeTlsEntity();
        tls.setKeyStore(keystoreEntity);
        final RemoteBrokerEntity remote = new RemoteBrokerEntity();
        remote.setTls(tls);
        final MqttBridgeEntity bridge = new MqttBridgeEntity();
        bridge.setRemoteBroker(remote);
        entity.getBridgeConfig().addAll(List.of(bridge));
        watcher.start(config.toFile(), INTERVAL_MS, () -> entity);

        Files.delete(keystore);

        await("the vanished keystore to be reported", () -> logged(Level.WARN, "no longer exists") == 1);
        ticks(10);
        assertThat(logged(Level.WARN, "no longer exists"))
                .as("reported once, not every tick")
                .isEqualTo(1);
        assertThat(restarts).hasValue(0);
    }

    // ---- nothing ends the watch

    @Test
    @Timeout(30)
    public void aReloadThatThrowsDoesNotEndTheWatch() throws Exception {
        reloadFailure.set(new IllegalStateException("simulated"));
        start();
        rewrite(config, "v1", 2);
        await("the failed check to be reported", () -> logged(Level.ERROR, "watcher failed to check") >= 1);

        reloadFailure.set(null);
        rewrite(config, "v2", 4);
        await("the next change to be reloaded", () -> reloads.get() == 1);
    }

    @Test
    @Timeout(30)
    public void aMissingConfigurationFileIsAWarningAndTheWatchGoesOn() throws Exception {
        start();
        Files.delete(config);
        await("the missing file to be reported", () -> logged(Level.WARN, "does not exist") >= 1);
        // Not an error: the file is missing, the watcher is fine (CI on EDG-949: an embedded node's
        // teardown deletes the folder, and the integration suite fails on any ERROR line).
        assertThat(logged(Level.ERROR, "watcher failed to check")).isZero();

        rewrite(config, "recreated", 4);
        await("the recreated file to be reloaded", () -> reloads.get() == 1);
        assertThat(logged(Level.ERROR, "watcher failed to check")).isZero();
    }

    @Test
    @Timeout(30)
    public void aStoppedWatchDoesNotNoticeTheFolderGoingAway() throws Exception {
        start();
        ticks(2);
        watcher.stop();
        final long warningsAtStop = logged(Level.WARN, "does not exist");
        Files.delete(config);
        ticks(6);
        assertThat(logged(Level.WARN, "does not exist")).isEqualTo(warningsAtStop);
        assertThat(logged(Level.ERROR, "watcher failed to check")).isZero();
        assertThat(reloads.get()).isZero();
    }

    // ---- the lock and the lifecycle

    @Test
    @Timeout(30)
    public void theCheckWaitsForTheConfigurationLock() throws Exception {
        start();
        final CountDownLatch holding = new CountDownLatch(1);
        final CountDownLatch release = new CountDownLatch(1);
        final Thread holder = new Thread(() -> {
            lock.lock();
            try {
                holding.countDown();
                release.await();
            } catch (final InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                lock.unlock();
            }
        });
        holder.setDaemon(true);
        holder.start();
        assertThat(holding.await(5, TimeUnit.SECONDS)).isTrue();

        rewrite(config, "v1", 2);
        ticks(6);
        assertThat(reloads)
                .as("no reload while another transition holds the lock")
                .hasValue(0);

        release.countDown();
        await("the reload once the lock is free", () -> reloads.get() == 1);
        holder.join(5000);
    }

    @Test
    public void startingTwiceIsRefused() {
        start();
        assertThatThrownBy(this::start).isInstanceOf(IllegalStateException.class);
    }

    @Test
    @Timeout(30)
    public void aStoppedWatchCanBeStartedAgain() throws Exception {
        start();
        watcher.stop();
        rewrite(config, "while stopped", 2);
        ticks(6);
        assertThat(reloads).as("a stopped watch reloads nothing").hasValue(0);

        start(); // seeds from the file as it is now: what changed while stopped is the new baseline
        ticks(4);
        assertThat(reloads)
                .as("a restarted watch does not replay what it missed")
                .hasValue(0);
        rewrite(config, "after the restart", 4);
        await("a change after the restart is seen", () -> reloads.get() == 1);
    }
}
