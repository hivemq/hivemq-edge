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
package util;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import org.jetbrains.annotations.NotNull;
import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.engine.support.descriptor.ClassSource;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestIdentifier;

/**
 * Records which test JVM ran which class, one file per JVM.
 * <p>
 * Gradle runs several test JVMs at once and merges their console output into one stream, so the combined log
 * cannot tell you which fork ran what. The JUnit XML does not carry it either -- it has a hostname, not a
 * process. That gap has repeatedly forced guesswork: reconstructing forks from start and end timestamps
 * cannot distinguish "a different fork" from "the same fork after a {@code forkEvery} restart", and produces
 * per-fork class counts that are simply wrong.
 * <p>
 * Each JVM writes {@code build/fork-logs/fork-<pid>.log} with one line per class:
 * {@code <epochMillis> <elapsedMillisInThisJvm> <durationMillis> <className>}. The process id is the only
 * identifier that is both stable within a JVM and distinct across concurrent ones. Note a {@code forkEvery}
 * restart yields a NEW pid and therefore a new file -- so files are JVMs, not Gradle's parallel slots;
 * grouping JVMs into slots is a question of overlapping time ranges, which the timestamps support.
 * <p>
 * Diagnostic only, and deliberately cheap: one line per class, appended, no locking beyond the file handle.
 * Nothing reads it during the build.
 */
public class ForkAttributionListener implements TestExecutionListener {

    private static final @NotNull String OUTPUT_DIR_PROPERTY = "forkLog.dir";
    private static final @NotNull String RUN_ID_PROPERTY = "forkLog.runId";

    private final @NotNull ConcurrentHashMap<String, Long> startedAt = new ConcurrentHashMap<>();
    private final @NotNull AtomicReference<Writer> writer = new AtomicReference<>();
    private final long jvmStart = System.currentTimeMillis();
    private final @NotNull java.util.concurrent.atomic.AtomicInteger sequence =
            new java.util.concurrent.atomic.AtomicInteger();

    @Override
    public void executionStarted(final @NotNull TestIdentifier identifier) {
        className(identifier).ifPresent(name -> startedAt.put(name, System.currentTimeMillis()));
    }

    @Override
    public void executionFinished(final @NotNull TestIdentifier identifier, final @NotNull TestExecutionResult result) {
        className(identifier).ifPresent(name -> {
            final Long start = startedAt.remove(name);
            if (start == null) {
                return;
            }
            final long now = System.currentTimeMillis();
            // seq is the n-th class this JVM was handed. Gradle deals round robin, so within one
            // JVM the classes sit at a constant stride in the dispatch order -- that stride is what
            // identifies the fork slot, and seq makes it readable without inferring it.
            //
            // NESTED CLASSES ARE LOGGED BUT MUST BE IGNORED WHEN AGGREGATING. A class with @Nested
            // inner classes produces a line per nested class AND a line for the enclosing class
            // whose duration already spans all of them, so summing every line double-counts:
            // EtherIpCipOdvaIT reported 176s for 88s of work. A nested class is also never
            // scheduled on its own -- Gradle dispatches the outer class and JUnit runs the nested
            // ones inside it -- so the outer class is the only meaningful unit for timing,
            // distribution and counting. Filter on '$' in the name.
            write(String.format(
                    "%d %d %d %d %s%n", now, start - jvmStart, now - start, sequence.incrementAndGet(), name));
        });
    }

    private @NotNull java.util.Optional<String> className(final @NotNull TestIdentifier identifier) {
        return identifier
                .getSource()
                .filter(source -> source instanceof ClassSource)
                .map(source -> ((ClassSource) source).getClassName());
    }

    private void write(final @NotNull String line) {
        try {
            Writer out = writer.get();
            if (out == null) {
                synchronized (this) {
                    out = writer.get();
                    if (out == null) {
                        final String dir = System.getProperty(OUTPUT_DIR_PROPERTY);
                        if (dir == null) {
                            return; // not enabled for this run
                        }
                        // The build stamps every fork of one test task with the same run id. Without it
                        // the logs of separate runs sit side by side in this directory -- Gradle never
                        // clears it -- and nothing distinguishes them: two back-to-back runs look like
                        // one run twice as long, with a dead stretch in the middle where the build was
                        // recompiling. Timing heuristics cannot separate them, because the idle gap
                        // between runs is the same length as a slow class.
                        final String runId = System.getProperty(RUN_ID_PROPERTY, "unknown");
                        final Path path = Paths.get(dir);
                        Files.createDirectories(path);
                        final long pid = ProcessHandle.current().pid();
                        out = Files.newBufferedWriter(
                                path.resolve("fork-" + runId + "-" + pid + ".log"),
                                StandardCharsets.UTF_8,
                                StandardOpenOption.CREATE,
                                StandardOpenOption.APPEND);
                        // Header: which run this JVM belonged to, when it started, and the worker number
                        // Gradle gave it. The worker number counts JVMs, not slots (a forkEvery restart
                        // gets a new one), so it does NOT identify the fork -- recorded because it is
                        // free and pins this file to a line in Gradle's own output.
                        out.write(String.format(
                                "# runId=%s jvmStart=%d pid=%d gradleWorker=%s%n",
                                runId, jvmStart, pid, System.getProperty("org.gradle.test.worker", "?")));
                        writer.set(out);
                    }
                }
            }
            synchronized (this) {
                out.write(line);
                out.flush();
            }
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
