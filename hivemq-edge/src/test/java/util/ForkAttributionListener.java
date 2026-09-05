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
 * Records how long each test class occupied a test JVM, and which JVM that was.
 * <p>
 * <b>This is the authority for class-level timing.</b> Neither of the other two sources can answer it:
 * <ul>
 *   <li>The <b>JUnit XML</b> has no start time on a {@code <testcase>}, and its class-level {@code time}
 *       merges BOTH attempts of a retried class into one span that includes the idle gap between them --
 *       one real example read {@code 462.119} seconds for 25.1 seconds of work.
 *   <li>The <b>console</b> emits one event pair per test METHOD and nothing for the class itself, so
 *       everything a class does outside a test method is invisible there. {@code OpenLdapIT} reads ~0.4s
 *       that way for a class that holds its JVM for 9.5s, almost all of it starting an LDAP container.
 * </ul>
 * A record here is wall clock for ONE ATTEMPT of ONE class in ONE JVM -- setup and teardown included, the
 * outer class present as its own record, and a retry appearing as a second record rather than one span.
 * <p>
 * <b>Written twice, to two places, deliberately.</b>
 * <p>
 * To {@code build/fork-logs/fork-<pid>.log}, one line per class:
 * {@code <endEpochMillis> <startOffsetInThisJvm> <durationMillis> <sequence> <className>}, where
 * {@code sequence} is the n-th class this JVM was handed.
 * <p>
 * And to <b>standard output</b>, as {@code TESTCLASS <class> <PASSED|FAILED> <endMillis> <durationMillis>
 * <pid> <worker>}. The file never reaches CI -- it is written to a remote executor's own disk and discarded
 * when the executor is released -- whereas stdout from a remote executor IS forwarded into the Jenkins
 * console. So the console line is what makes class timing available on CI at all (EDG-990).
 * <p>
 * The process id is the only identifier both stable within a JVM and distinct across concurrent ones. A
 * {@code forkEvery} restart yields a NEW pid: a run of 332 classes over 5 lanes used 15 JVMs, three per lane
 * in sequence. So JVMs are not lanes -- grouping them into lanes is a question of overlapping time ranges,
 * which the timestamps support.
 * <p>
 * Diagnostic only, and deliberately cheap: one line per class, appended, no locking beyond the file handle.
 * Nothing reads it during the build.
 */
public class ForkAttributionListener implements TestExecutionListener {

    private static final @NotNull String OUTPUT_DIR_PROPERTY = "forkLog.dir";

    /**
     * Which JVM this is, and which lane it belongs to. Both are needed, and neither alone suffices.
     *
     * <p>The console merges every JVM's output into one stream, so a record has to carry its own identity or
     * the stream cannot be split back into lanes. The pid identifies the JVM and CHANGES when Gradle restarts
     * one -- {@code forkEvery = 24} means a run of 332 classes over 5 lanes uses 15 JVMs, three per lane in
     * sequence. So a restart shows up as two pids whose lifetimes do not overlap, and concurrent lanes as
     * pids whose lifetimes do. The Gradle worker number rises monotonically across the whole run and pins a
     * record to a line in Gradle's own output.
     */
    private static final long PID = ProcessHandle.current().pid();

    private static final @NotNull String GRADLE_WORKER = System.getProperty("org.gradle.test.worker", "?");

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
            final long duration = now - start;
            final String outcome = result.getStatus() == TestExecutionResult.Status.SUCCESSFUL ? "PASSED" : "FAILED";

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
            write(String.format("%d %d %d %d %s%n", now, start - jvmStart, duration, sequence.incrementAndGet(), name));

            // The SAME record, to stdout, so it survives where the file does not.
            //
            // On CI the file above is written to a remote executor's own disk and thrown away when the
            // executor is released, so class-level timing has never reached a Jenkins log -- forcing
            // readers to reconstruct it from per-method events, which cannot see anything a class does
            // outside a test method. OpenLdapIT read 0.3s that way for a class that occupies its JVM
            // for 9.5s, was scheduled as trivially fast, and ran last where nothing could overlap it.
            // Stdout from a remote executor IS forwarded into the console, so this closes that gap
            // without an artifact-collection step (EDG-990).
            //
            // ONE LINE PER ATTEMPT, and the outcome is what makes a retry recognisable: two records for
            // one class otherwise mean either a retry or two separate invocations, and those are
            // indistinguishable. A FAILED record followed by a PASSED one is a retry.
            //
            // The start time is DERIVED by readers as end - duration rather than printed: two
            // independently written fields can disagree, a derived one cannot.
            System.out.println(
                    String.format("TESTCLASS %s %s %d %d %d %s", name, outcome, now, duration, PID, GRADLE_WORKER));
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
                        // No run id is stamped here. The build deliberately does not pass one: it would
                        // have to differ on every invocation, and a system property is part of a test
                        // task's cache key, so the task could then never be restored from the build
                        // cache. Separating runs is left to the reader of these logs, which can do it
                        // from the timings -- within a run the forks are essentially never all idle at
                        // once, so a stretch with nothing running is a run boundary.
                        final Path path = Paths.get(dir);
                        Files.createDirectories(path);
                        out = Files.newBufferedWriter(
                                path.resolve("fork-" + PID + ".log"),
                                StandardCharsets.UTF_8,
                                StandardOpenOption.CREATE,
                                StandardOpenOption.APPEND);
                        // Header: when this JVM started and the worker number Gradle gave it. The worker
                        // number counts JVMs, not slots (a forkEvery restart gets a new one), so it does
                        // NOT identify the fork -- recorded because it is free and pins this file to a
                        // line in Gradle's own output.
                        out.write(
                                String.format("# jvmStart=%d pid=%d gradleWorker=%s%n", jvmStart, PID, GRADLE_WORKER));
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
