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
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import ch.qos.logback.classic.Level;
import com.hivemq.configuration.service.ConfigurationService;
import com.hivemq.edge.HiveMQEdgeConstants;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileTime;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.ConcurrentModificationException;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.slf4j.LoggerFactory;
import util.LogbackCapturingAppender;
import util.TestConfigurationBootstrap;

/**
 * EDG-949: the configuration write path, audited beside EDG-882. Four findings, each pinned by a test
 * that fails without its fix.
 * <ol>
 * <li>The hot-reload watcher survives an unparseable configuration and a watched fragment that
 * disappears, and still restarts for a change that needs it.</li>
 * <li>The rolling backup rotates only over the backups Edge itself wrote, never over an operator's own
 * file that happens to match the name.</li>
 * <li>A refused write consumes no backup slot.</li>
 * <li>The last-write time moves only when the file was written.</li>
 * </ol>
 * Finding 5's five-slot count rides along with the rotation test.
 */
public class ConfigWritePathHardeningTest extends AbstractConfigurationTest {

    private static final long WATCH_INTERVAL_MS = 50;
    private static final long WAIT_SECONDS = 15;

    private @NotNull LogbackCapturingAppender logCapture;
    private @NotNull Path config;
    private final @NotNull AtomicInteger restarts = new AtomicInteger();
    private @NotNull String previousDevMode;

    @BeforeEach
    public void captureTheLogAndObserveRestarts() {
        // The package logger: the lines asserted below come from the reader-writer and from the watcher.
        logCapture = LogbackCapturingAppender.Factory.weaveInto(
                LoggerFactory.getLogger(ConfigFileReaderWriter.class.getPackageName()));
        config = xmlFile.toPath();
        reader.setRestartHook(restarts::incrementAndGet);
        // The fragment and keystore watch only runs outside development mode, and so does the restart
        // decision; other test classes set the property and do not always restore it.
        previousDevMode = System.getProperty(HiveMQEdgeConstants.DEVELOPMENT_MODE, "");
        System.setProperty(HiveMQEdgeConstants.DEVELOPMENT_MODE, "false");
    }

    @AfterEach
    public void stopWatchingAndRestore() {
        reader.stopWatching();
        System.setProperty(HiveMQEdgeConstants.DEVELOPMENT_MODE, previousDevMode);
        LogbackCapturingAppender.Factory.cleanUp();
    }

    private static @NotNull String bridges(final @NotNull String bridgeId) {
        return "<mqtt-bridges>\n    <mqtt-bridge>\n        <id>" + bridgeId + "</id>\n"
                + "        <remote-broker>\n            <host>testhost</host>\n"
                + "        </remote-broker>\n        <forwarded-topics>\n            <forwarded-topic>\n"
                + "                <filters>\n                    <mqtt-topic-filter>plant/#</mqtt-topic-filter>\n"
                + "                </filters>\n                <destination>{#}</destination>\n"
                + "                <max-qos>1</max-qos>\n            </forwarded-topic>\n"
                + "        </forwarded-topics>\n    </mqtt-bridge>\n</mqtt-bridges>";
    }

    private static @NotNull String configWithBridge(final @NotNull String bridgeId) {
        return "<hivemq>\n" + bridges(bridgeId) + "\n</hivemq>";
    }

    private static @NotNull String configWithListener(final int port) {
        return "<hivemq>\n<mqtt-listeners>\n    <tcp-listener>\n        <port>" + port + "</port>\n"
                + "        <bind-address>127.0.0.1</bind-address>\n    </tcp-listener>\n</mqtt-listeners>\n"
                + bridges("edg-949-listener-bridge") + "\n</hivemq>";
    }

    /**
     * The watcher compares modification times with millisecond resolution and reacts to any change, so
     * every rewrite in these tests moves the time by whole seconds to stay clear of the granularity of
     * any file system.
     */
    private void rewrite(final @NotNull Path file, final @NotNull String content, final int secondsLater)
            throws IOException {
        rewrite(file, content, secondsLater, "rw-------");
    }

    /**
     * Written beside the file and moved onto it, so the watcher sees the new content, its permissions and
     * its modification time all at once rather than racing a write-then-touch.
     */
    private void rewrite(
            final @NotNull Path file, final @NotNull String content, final int secondsLater, final @NotNull String mode)
            throws IOException {
        final Path staged = file.resolveSibling(file.getFileName() + ".staged");
        Files.writeString(staged, content);
        Files.setLastModifiedTime(staged, FileTime.fromMillis(System.currentTimeMillis() + secondsLater * 1000L));
        // after the time: a file nobody may touch cannot have its time set either
        if (staged.getFileSystem().supportedFileAttributeViews().contains("posix")) {
            Files.setPosixFilePermissions(staged, PosixFilePermissions.fromString(mode));
        }
        Files.move(staged, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
    }

    // ---- the watch ends with the node

    @Test
    @Timeout(30)
    public void theConfigurationServiceStopsTheWatchWhenTheNodeStops() throws Exception {
        rewrite(config, configWithBridge("edg-949-before-stop"), 0);
        final ConfigurationService service = new TestConfigurationBootstrap().getConfigurationService();
        service.setConfigFileReaderWriter(reader);
        reader.applyConfigAndWatch(WATCH_INTERVAL_MS);
        TimeUnit.MILLISECONDS.sleep(WATCH_INTERVAL_MS * 2);

        service.stopWatchingConfigFile();

        // What the integration suite does after stopping an embedded node: the folder goes away. A watch
        // still ticking would report the missing file every interval, and the suite fails on ERROR lines.
        Files.delete(config);
        TimeUnit.MILLISECONDS.sleep(WATCH_INTERVAL_MS * 6);
        assertThat(logged(Level.WARN)).noneMatch(line -> line.contains("does not exist"));
        assertThat(logged(Level.ERROR)).noneMatch(line -> line.contains("watcher failed to check"));
        // and "stopped" means it can be started again
        rewrite(config, configWithBridge("edg-949-after-stop"), 2);
        reader.applyConfigAndWatch(WATCH_INTERVAL_MS);
    }

    @Test
    public void stoppingTheWatchWithoutAReaderIsANoOp() {
        new TestConfigurationBootstrap().getConfigurationService().stopWatchingConfigFile();
    }

    private @NotNull String runningBridgeId() {
        return Objects.requireNonNull(Objects.requireNonNull(reader.getConfigEntity())
                .getBridgeConfig()
                .get(0)
                .getId());
    }

    /** The watcher thread appends while this reads, so a torn read is retried rather than reported. */
    private @NotNull List<String> logged(final @NotNull Level level) {
        while (true) {
            try {
                return logCapture.getCapturedLogs().stream()
                        .filter(event -> event.getLevel() == level)
                        .map(event -> event.getFormattedMessage())
                        .toList();
            } catch (final ConcurrentModificationException torn) {
                Thread.onSpinWait();
            }
        }
    }

    private static void await(final @NotNull String what, final @NotNull BooleanSupplier condition)
            throws InterruptedException {
        final long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(WAIT_SECONDS);
        while (!condition.getAsBoolean()) {
            if (System.nanoTime() > deadline) {
                throw new AssertionError("timed out waiting for: " + what);
            }
            Thread.sleep(20);
        }
    }

    private void requirePosix() {
        assumeTrue(
                config.getFileSystem().supportedFileAttributeViews().contains("posix"),
                "the refusal is provoked through directory permissions");
        assumeTrue(!"root".equals(System.getProperty("user.name")), "a superuser is not refused by mode bits");
    }

    // ---- finding 1: the watcher ----

    @Test
    @Timeout(60)
    public void whenTheConfigurationBecomesUnparseable_thenTheNodeKeepsItsConfigurationAndTheWatcherLivesOn()
            throws Exception {
        rewrite(config, configWithBridge("edg-949-before"), 0);
        reader.applyConfig();
        reader.applyConfigAndWatch(WATCH_INTERVAL_MS);

        rewrite(config, "<hivemq>\n<mqtt-bridges>\n    <mqtt-bridge>\n        <id>edg-949-broken", 2);

        await("the rejected reload to be reported", () -> logged(Level.ERROR).stream()
                .anyMatch(message -> message.contains("reload rejected")));
        assertThat(runningBridgeId())
                .as("the configuration the node runs on must be the one that parsed")
                .isEqualTo("edg-949-before");
        assertThat(restarts)
                .as("a node restarted onto a file that cannot be parsed does not come back")
                .hasValue(0);

        // the watcher is still alive: the operator's correction is picked up
        rewrite(config, configWithBridge("edg-949-corrected"), 4);
        await("the corrected configuration to be applied", () -> "edg-949-corrected".equals(runningBridgeId()));
        assertThat(restarts).hasValue(0);
    }

    @Test
    @Timeout(60)
    public void whenAWatchedFragmentDisappears_thenTheWatcherLivesOn() throws Exception {
        final Path fragment = temporaryFolder.toPath().resolve("bridges.fragment");
        Files.writeString(fragment, bridges("edg-949-from-fragment"));
        rewrite(config, "<hivemq>\n${FRAGMENT:" + fragment + "}\n</hivemq>", 0);
        reader.applyConfig();
        assertThat(runningBridgeId()).isEqualTo("edg-949-from-fragment");
        reader.applyConfigAndWatch(WATCH_INTERVAL_MS);

        Files.delete(fragment);

        await("the vanished fragment to be reported and dropped", () -> logged(Level.WARN).stream()
                .anyMatch(message -> message.contains("no longer exists")));

        // the watcher is still alive: a configuration that no longer references the fragment is applied
        rewrite(config, configWithBridge("edg-949-without-fragment"), 2);
        await("the new configuration to be applied", () -> "edg-949-without-fragment".equals(runningBridgeId()));
        assertThat(restarts).hasValue(0);
        assertThat(logged(Level.ERROR))
                .as("nothing escaped the periodic task")
                .noneMatch(message -> message.contains("watcher failed"));
    }

    @Test
    @Timeout(60)
    public void whenTheReloadedConfigurationNeedsARestart_thenTheNodeStillRestarts() throws Exception {
        rewrite(config, configWithListener(18831), 0);
        reader.applyConfig();
        reader.applyConfigAndWatch(WATCH_INTERVAL_MS);

        rewrite(config, configWithListener(18832), 2);

        await("the restart decision", () -> restarts.get() > 0);
        assertThat(logged(Level.ERROR)).anyMatch(message -> message.contains("can't be hot-reloaded"));
    }

    /** An unreadable file is reported the way an unparseable one is: rejected, previous configuration kept. */
    @Test
    @Timeout(60)
    public void whenTheConfigurationIsMomentarilyUnreadable_thenTheWatcherTriesAgain() throws Exception {
        requirePosix();
        rewrite(config, configWithBridge("edg-949-before"), 0);
        reader.applyConfig();
        reader.applyConfigAndWatch(WATCH_INTERVAL_MS);

        // a rewrite the node cannot read at the moment it looks
        rewrite(config, configWithBridge("edg-949-after"), 2, "---------");
        try {
            await("the unreadable file to be reported", () -> logged(Level.ERROR).stream()
                    .anyMatch(message -> message.contains("reload rejected")));
        } finally {
            Files.setPosixFilePermissions(config, PosixFilePermissions.fromString("rw-------"));
        }
        assertThat(runningBridgeId()).isEqualTo("edg-949-before");

        // readable again, and changed again: the watcher must still be running to see it
        rewrite(config, configWithBridge("edg-949-readable-again"), 4);
        await("the change after the outage to be applied", () -> "edg-949-readable-again".equals(runningBridgeId()));
        assertThat(restarts).hasValue(0);
    }

    /**
     * A file that is not there at all fails before any parse -- reading its modification time throws --
     * which is what used to end the periodic task for good. An external tool replacing the file by
     * delete-and-recreate produces exactly this window.
     */
    @Test
    @Timeout(60)
    public void whenTheConfigurationFileIsMomentarilyMissing_thenTheWatcherTriesAgain() throws Exception {
        rewrite(config, configWithBridge("edg-949-before"), 0);
        reader.applyConfig();
        reader.applyConfigAndWatch(WATCH_INTERVAL_MS);

        Files.delete(config);

        await("the missing file to be reported", () -> logged(Level.WARN).stream()
                .anyMatch(message -> message.contains("does not exist")));
        // A warning, not an error: the integration suite fails a test on any ERROR line, and an operator
        // replacing the file by remove-and-copy has done nothing wrong.
        assertThat(logged(Level.ERROR)).noneMatch(message -> message.contains("watcher failed to check"));
        assertThat(runningBridgeId()).isEqualTo("edg-949-before");

        rewrite(config, configWithBridge("edg-949-recreated"), 4);
        await("the recreated file to be applied", () -> "edg-949-recreated".equals(runningBridgeId()));
        assertThat(restarts).hasValue(0);
    }

    /**
     * The node's own REST write bumps the file's time. The watcher must not read, parse and re-apply a
     * file it has just written itself: pointless work after every REST change, and every such reload
     * takes the configuration lock first and the extractors' monitors second, the inverse of the REST
     * path (EDG-937 R3-02) -- a window that should not open on its own.
     */
    @Test
    @Timeout(60)
    public void whenTheNodeWritesTheConfigurationItself_thenTheWatcherDoesNotReloadIt() throws Exception {
        rewrite(config, configWithBridge("edg-949-own-write"), 0);
        reader.applyConfig();
        reader.applyConfigAndWatch(WATCH_INTERVAL_MS);
        Thread.sleep(WATCH_INTERVAL_MS * 4);
        final long readsBefore = logged(Level.INFO).stream()
                .filter(m -> m.contains("Reading configuration file"))
                .count();

        final FileTime beforeWrite = Files.getLastModifiedTime(config);
        Thread.sleep(1100); // a second, so the write moves the time on any file system
        reader.writeConfigWithSync();
        assertThat(Files.getLastModifiedTime(config))
                .as("the write must have replaced the file")
                .isNotEqualTo(beforeWrite);
        Thread.sleep(WATCH_INTERVAL_MS * 6);

        final long readsAfter = logged(Level.INFO).stream()
                .filter(m -> m.contains("Reading configuration file"))
                .count();
        assertThat(readsAfter).as("the watcher re-read the node's own write").isEqualTo(readsBefore);
        assertThat(restarts).hasValue(0);

        // and an operator's edit after that is still seen
        rewrite(config, configWithBridge("edg-949-operator-edit"), 4);
        await("the operator's edit to be applied", () -> "edg-949-operator-edit".equals(runningBridgeId()));
    }

    /**
     * A restore from a backup -- cp -p, rsync -a, a volume remount -- carries an older time than the
     * broken file it replaces. A high-water mark on the time would ignore the correction for good.
     */
    @Test
    @Timeout(60)
    public void whenAnOlderFileIsRestoredOverABrokenOne_thenItIsApplied() throws Exception {
        rewrite(config, configWithBridge("edg-949-before"), 0);
        reader.applyConfig();
        reader.applyConfigAndWatch(WATCH_INTERVAL_MS);

        rewrite(config, "<hivemq><mqtt-bridges><mqtt-bridge><id>edg-949-broken", 6);
        await("the rejected reload to be reported", () -> logged(Level.ERROR).stream()
                .anyMatch(message -> message.contains("reload rejected")));
        final FileTime broken = Files.getLastModifiedTime(config);

        rewrite(config, configWithBridge("edg-949-restored"), 0);
        Files.setLastModifiedTime(config, FileTime.fromMillis(broken.toMillis() - 60_000)); // a minute older

        await("the older, restored file to be applied", () -> "edg-949-restored".equals(runningBridgeId()));
        assertThat(restarts).hasValue(0);
    }

    /**
     * An unset variable is the most ordinary reload mistake. It used to fall through to the generic
     * handler, which logged "Exiting HiveMQ Edge" on a node that was not exiting -- and an operator who
     * believed it and restarted would not get the node back.
     */
    @Test
    @Timeout(60)
    public void whenAPlaceholderCannotBeRendered_thenTheReloadIsRejectedAndNothingSaysExiting() throws Exception {
        rewrite(config, configWithBridge("edg-949-before"), 0);
        reader.applyConfig();
        reader.applyConfigAndWatch(WATCH_INTERVAL_MS);

        rewrite(config, configWithBridge("${ENV:EDG949_VARIABLE_THAT_IS_NOT_SET}"), 2);

        await("the rejected reload to be reported", () -> logged(Level.ERROR).stream()
                .anyMatch(message -> message.contains("reload rejected")));
        assertThat(logged(Level.ERROR)).anyMatch(message -> message.contains("could not be rendered"));
        assertThat(logged(Level.ERROR)).noneMatch(message -> message.contains("Exiting"));
        assertThat(runningBridgeId()).isEqualTo("edg-949-before");
        assertThat(restarts).hasValue(0);

        rewrite(config, configWithBridge("edg-949-fixed"), 4);
        await("the correction to be applied", () -> "edg-949-fixed".equals(runningBridgeId()));
    }

    // ---- findings 2 and 5: the rotation ----

    @Test
    public void whenTheBackupsRotate_thenOnlyTheBackupsEdgeWroteAreCandidates() throws Exception {
        final Path archive = config.resolveSibling("config-archive-2024.xml");
        final Path yearArchive = config.resolveSibling("config_2024.xml"); // digits only: still not a slot
        final String archived = "<hivemq><!-- the operator's own copy, older than everything --></hivemq>";
        Files.writeString(archive, archived);
        Files.writeString(yearArchive, archived);
        Files.setLastModifiedTime(archive, FileTime.fromMillis(1_700_000_000_000L)); // 2023
        Files.setLastModifiedTime(yearArchive, FileTime.fromMillis(1_700_000_000_000L));

        for (int write = 1; write <= 8; write++) {
            rewrite(config, configWithBridge("edg-949-write-" + write), write);
            reader.applyConfig();
            reader.writeConfigToXML(xmlFile, true, true);
        }

        assertThat(Files.readString(archive))
                .as("a file Edge did not create was overwritten by the rolling backup")
                .isEqualTo(archived);
        assertThat(Files.getLastModifiedTime(archive).toMillis()).isEqualTo(1_700_000_000_000L);
        assertThat(Files.readString(yearArchive))
                .as("a file named like a slot but outside the slot range was overwritten")
                .isEqualTo(archived);
        for (int slot = 1; slot <= 5; slot++) {
            assertThat(config.resolveSibling("config_" + slot + ".xml"))
                    .as("all five configured slots are used, not four")
                    .exists();
        }
        assertThat(config.resolveSibling("config_6.xml")).doesNotExist();
        assertThat(Files.readString(config)).contains("edg-949-write-8");
    }

    // ---- finding 3: a refused write and the backup ----

    /**
     * The configured path is a link into a directory the node cannot write, which is a mounted
     * configuration. The backup goes beside the link, the replacement beside the real file, so the
     * backup used to be taken and the write refused a moment later -- one slot per refusal.
     */
    @Test
    public void whenTheWriteIsRefused_thenNoBackupSlotIsConsumed() throws Exception {
        requirePosix();
        final Path mounted = Files.createDirectory(temporaryFolder.toPath().resolve("mounted"));
        final Path real = mounted.resolve("real.xml");
        Files.writeString(real, configWithBridge("edg-949-mounted"));
        Files.createSymbolicLink(config, real);
        reader.applyConfig();
        Files.setPosixFilePermissions(mounted, PosixFilePermissions.fromString("r-x------"));
        try {
            for (int refusal = 0; refusal < 3; refusal++) {
                reader.writeConfigWithSync();
            }
        } finally {
            Files.setPosixFilePermissions(mounted, PosixFilePermissions.fromString("rwx------"));
        }

        assertThat(logged(Level.ERROR)).anyMatch(message -> message.contains("was not written to config.xml"));
        try (var listing = Files.list(temporaryFolder.toPath())) {
            assertThat(listing.map(path -> path.getFileName().toString()).toList())
                    .as("a refused write must not rotate the backups")
                    .containsExactlyInAnyOrder("config.xml", "mounted");
        }
        assertThat(Files.readString(real)).contains("edg-949-mounted");
    }

    // ---- finding 4: the last-write time ----

    @Test
    public void whenTheWriteIsRefused_thenTheLastWriteTimeDoesNotMove() throws Exception {
        requirePosix();
        rewrite(config, configWithBridge("edg-949-stamp"), 0);
        reader.applyConfig();
        assertThat(reader.getLastWrite()).isZero();
        Files.setPosixFilePermissions(config, PosixFilePermissions.fromString("r--r--r--"));

        reader.writeConfigWithSync();

        assertThat(reader.getLastWrite())
                .as("the node must not report the configuration as written when it refused to write it")
                .isZero();

        Files.setPosixFilePermissions(config, PosixFilePermissions.fromString("rw-------"));
        final long before = System.currentTimeMillis();
        reader.writeConfigWithSync();
        assertThat(reader.getLastWrite()).isGreaterThanOrEqualTo(before);
    }
}
