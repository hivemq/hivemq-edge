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
import org.junit.platform.engine.support.descriptor.MethodSource;
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
 * <b>ONE GRAMMAR, WRITTEN TO TWO PLACES.</b> Every record goes both to standard output and to
 * {@code build/fork-logs/fork-<pid>.log}, in identical words, because each destination survives where the
 * other does not: on CI the file is written to a remote executor's own disk and discarded when the executor
 * is released, while locally the console scrolls past unrecorded and the file is what gets collected.
 * Writing the same line to both is what lets ONE reader serve both environments -- and a local run needs no
 * capture step, because the records are on disk when it finishes.
 * <p>
 * Three kinds of line share the grammar:
 * <pre>
 *   $1 kind   $2 name    $3 parent   $4 endMillis   $5 outcome        $6 durationMillis
 *   ---------------------------------------------------------------------------------------
 *   JVM       &lt;pid&gt;      &lt;worker&gt;    &lt;nowMillis&gt;    --                --
 *   TESTCLASS &lt;class&gt;    &lt;pid&gt;       &lt;endMillis&gt;    PASSED|FAILED     &lt;durationMillis&gt;
 *   TEST      &lt;method&gt;   &lt;class&gt;     &lt;endMillis&gt;    PASSED|FAILED     &lt;durationMillis&gt;
 * </pre>
 * Every line is {@code kind name parent when [outcome duration]}, so one split on whitespace reads all three
 * and {@code $3} always names the enclosing thing: a test's class, a class's JVM, a JVM's executor. That is
 * what makes the chain walkable -- a TEST line does not repeat the JVM because its class already carries it.
 * A JVM has no outcome and no duration, and simply stops after {@code $4} rather than padding.
 * <p>
 * Stdout from a remote executor IS forwarded into the Jenkins console, so these lines are what make class
 * timing available on CI at all (EDG-990). The pid ties the three kinds together.
 * <p>
 * Every consumer reads these records and nothing else: the Gradle {@code reportTestConcurrency} task via
 * {@code ForkLogs.kt}, and the vault's {@code testrun-records.py} / {@code testrun-condense.py} /
 * {@code testrun-report.py}. An older positional line used to be written here as well and was removed once
 * the last reader moved over -- two formats for one fact is how the readers drifted apart in the first place.
 * <p>
 * The TEST line duplicates what Gradle's own {@code SomeIT > someTest() PASSED} events already say, and is
 * printed anyway because those events carry NO TIMESTAMP OF THEIR OWN. On Jenkins that is invisible, because
 * Jenkins stamps every console line as it arrives; on a local run the same log yields nothing, so the two
 * environments needed two different readers. Emitting the time in the text makes one log format, read one
 * way, everywhere. The cost is about 2,300 lines on a console log that already runs to 520,000.
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

    @Override
    public void testPlanExecutionStarted(final @NotNull org.junit.platform.launcher.TestPlan testPlan) {
        // WHEN THIS JVM CAME UP, printed once per JVM before it runs anything.
        //
        // The TESTCLASS records below say when each CLASS started, which is not the same thing: the
        // difference between this line and the first class in the same JVM is the JVM's own startup,
        // and `forkEvery` pays that repeatedly -- a 332-class run over 5 lanes uses 15 JVMs, so it is
        // paid 15 times. Roughly 20 seconds each by the note in the build file, but that figure has
        // never been measured on CI because nothing recorded it there.
        //
        // The same value goes into the file header as `jvmStart`; this is that header's console twin,
        // for the same reason as the TESTCLASS line -- the file never leaves a remote executor.
        //
        // Fields follow the shared grammar: name is the pid, PARENT is the Gradle worker -- the lane
        // this JVM was started for -- and then the time. There is no outcome and no duration, so the
        // line stops at $4 rather than padding to six.
        emit(String.format("JVM %d %s %d", PID, GRADLE_WORKER, jvmStart));
    }

    /**
     * A test JUnit never ran -- {@code @Disabled}, or an {@code assumeTrue} that did not hold.
     * <p>
     * Recorded because otherwise a skip is INVISIBLE: {@code executionStarted} never fires for it, so
     * without this the records could not say how many tests were skipped and that one number had to come
     * from the JUnit XML -- which meant two sources in one report, disagreeing about what they counted.
     * <p>
     * Duration is always 0. A skipped test consumes no time, so the field is present for the grammar's
     * sake rather than to carry information; the outcome is what matters.
     */
    @Override
    public void executionSkipped(final @NotNull TestIdentifier identifier, final @NotNull String reason) {
        methodKey(identifier).ifPresent(key -> {
            final int split = key.indexOf(' ');
            emit(String.format(
                    "TEST %s %s %d SKIPPED 0",
                    key.substring(split + 1), key.substring(0, split), System.currentTimeMillis()));
        });
        // A whole class can be skipped too -- @Disabled on the type. It occupies no JVM, so it gets no
        // TESTCLASS record and correctly does not appear in any timing; this line is what lets a reader
        // still see that it was part of the run rather than silently missing.
        className(identifier)
                .ifPresent(name ->
                        emit(String.format("TESTCLASS %s %d %d SKIPPED 0", name, PID, System.currentTimeMillis())));
    }

    @Override
    public void executionStarted(final @NotNull TestIdentifier identifier) {
        final long now = System.currentTimeMillis();
        className(identifier).ifPresent(name -> startedAt.put(name, now));
        // A METHOD IS KEYED ON CLASS AND METHOD TOGETHER, never the method name alone: two classes in
        // one JVM routinely share a method name, and a parameterised method repeats its own name once
        // per case. The identifier's unique id would also serve, but it is verbose and not printable
        // as a single field.
        methodKey(identifier).ifPresent(key -> startedAt.put(key, now));
    }

    @Override
    public void executionFinished(final @NotNull TestIdentifier identifier, final @NotNull TestExecutionResult result) {
        methodKey(identifier).ifPresent(key -> {
            final Long start = startedAt.remove(key);
            if (start == null) {
                return;
            }
            final long now = System.currentTimeMillis();
            final int split = key.indexOf(' ');

            // ONE LINE PER TEST METHOD, carrying its own absolute time.
            //
            // Gradle already prints `SomeIT > someTest() PASSED` for every test, so this looks
            // redundant -- and on CI it nearly is, because Jenkins stamps every console line as it
            // arrives. A LOCAL Gradle run has no such stamp, so the identical log yields no times at
            // all and local and CI needed two different readers, measuring two different things. That
            // divergence is what produced a string of contradictory numbers. Putting the time IN the
            // text removes the difference between the two environments rather than compensating for it.
            //
            // The parent is the class that DECLARES the method, so a @Nested test names the nested
            // class -- matching the TESTCLASS line emitted for that same nested class, and letting a
            // reader roll methods up to whichever level it wants.
            //
            // WRITTEN TO BOTH PLACES, in the SAME words. The file is what a local run keeps -- the
            // console scrolls past and nobody captures it -- while on CI only the console survives.
            // Emitting one identical line to each means neither environment needs a capture step and
            // neither needs its own reader.
            emit(String.format(
                    "TEST %s %s %d %s %d",
                    key.substring(split + 1), key.substring(0, split), now, outcome(result), now - start));
        });
        className(identifier).ifPresent(name -> {
            final Long start = startedAt.remove(name);
            if (start == null) {
                return;
            }
            final long now = System.currentTimeMillis();
            final long duration = now - start;
            final String outcome = outcome(result);

            // NESTED CLASSES ARE LOGGED BUT MUST BE IGNORED WHEN AGGREGATING. A class with @Nested
            // inner classes produces a line per nested class AND a line for the enclosing class
            // whose duration already spans all of them, so summing every line double-counts:
            // EtherIpCipOdvaIT reported 176s for 88s of work. A nested class is also never
            // scheduled on its own -- Gradle dispatches the outer class and JUnit runs the nested
            // ones inside it -- so the outer class is the only meaningful unit for timing,
            // distribution and counting. Filter on '$' in the name.
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
            //
            // The PARENT is the pid alone. The Gradle worker number was printed here too and is now
            // dropped: the JVM line already pairs this pid with its worker, so repeating it made the
            // same fact writable from two places, which is how they come to disagree.
            //
            // TO BOTH PLACES, as with TEST above. The positional line written just before stays as it
            // is because ForkLogs.kt and testrun-condense.py parse it; this adds the same fact in the
            // shared grammar, so a fork file and a Jenkins console can be read by ONE reader.
            //
            // The two lines describe the same class execution, which is safe only because BOTH
            // existing parsers require a NUMERIC first field -- `f[0].toLongOrNull() ?: return` in
            // ForkLogs.kt, `int(parts[0])` under a try/except in testrun-condense.py -- so a line
            // beginning `TESTCLASS` is skipped rather than counted a second time. Verified in both
            // before adding this; a parser without that guard would double every class.
            emit(String.format("TESTCLASS %s %d %d %s %d", name, PID, now, outcome, duration));
        });
    }

    private static @NotNull String outcome(final @NotNull TestExecutionResult result) {
        // THREE OUTCOMES, NOT TWO. JUnit distinguishes SUCCESSFUL, ABORTED and FAILED, and collapsing the
        // last two reports a test that never really ran as a failure: the TLS 1.1 tests abort on a failed
        // assumption when the JDK has the protocol disabled, and were reported as six hard failures in a
        // green suite. An abort is JUnit's "skipped at runtime" -- the JUnit XML files it under <skipped>
        // -- so it is recorded as SKIPPED here, matching what a skipped test means everywhere else.
        switch (result.getStatus()) {
            case SUCCESSFUL:
                return "PASSED";
            case ABORTED:
                return "SKIPPED";
            default:
                return "FAILED";
        }
    }

    /**
     * One record, to BOTH the console and the fork-log file, in identical words.
     * <p>
     * Each destination survives where the other does not. On CI the file is written to a remote executor's
     * own disk and discarded when the executor is released, so only the console comes back. Locally the
     * console scrolls past unrecorded -- nothing captures it, and requiring a capture step would be a new
     * demand on whoever runs the suite -- while the file sits in {@code build/} where the existing
     * collection script already picks it up.
     * <p>
     * Writing the same line to both is what makes ONE reader enough for both environments. It also means a
     * local run needs no change in how it is invoked: run the tests as always, and the records are on disk.
     */
    private void emit(final @NotNull String line) {
        System.out.println(line);
        write(line + System.lineSeparator());
    }

    private @NotNull java.util.Optional<String> className(final @NotNull TestIdentifier identifier) {
        return identifier
                .getSource()
                .filter(source -> source instanceof ClassSource)
                .map(source -> ((ClassSource) source).getClassName());
    }

    /**
     * {@code "<declaringClass> <methodName>"} for a test method, empty for anything else.
     * <p>
     * Two values in one string because they are needed together in both places that use them -- as a map key
     * that cannot collide across classes, and as the two name fields of the TEST line.
     * <p>
     * Class and method are read from the {@link MethodSource} rather than from the display name. A display
     * name CONTAINS SPACES for a parameterised or repeated test -- {@code method(QoS) > [1] QoS_is_Absent} --
     * which would break the field positions of every line after it.
     */
    private @NotNull java.util.Optional<String> methodKey(final @NotNull TestIdentifier identifier) {
        return identifier
                .getSource()
                .filter(source -> source instanceof MethodSource)
                .map(source -> (MethodSource) source)
                .map(source -> source.getClassName() + " " + source.getMethodName());
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
