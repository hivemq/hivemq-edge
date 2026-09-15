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

import java.util.concurrent.ConcurrentHashMap;
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
 * <b>ONE GRAMMAR, ONE DESTINATION: THE CONSOLE.</b> Every record goes to standard output and nowhere else,
 * and one build log is a complete account of one run. That works because a record states its own absolute
 * time and its own parent, so nothing has to be inferred from where a line appeared or which file it was in.
 * Gradle forwards a remote executor's stdout into the build log exactly as it forwards a local fork's, so the
 * SAME log serves a 40-executor CI run and a five-fork local one, read by ONE reader.
 * <p>
 * These records were once written to a per-JVM file as well, on the reasoning that a local console scrolls
 * past unrecorded. It bought nothing and cost correctness: the directory was never cleared between runs -- a
 * run id in the path would have made the test task uncacheable -- so reading it meant separating one run out
 * of an accumulated pile, and getting that subtly wrong folded one run's tests into another, reading 9590
 * tests for a 4795-test suite. A build log has no such problem: it IS one run.
 * <p>
 * Four kinds of line share the grammar:
 * <pre>
 *   $1 kind        $2 name    $3 parent  $4 endMillis  $5 outcome              $6 durationMillis
 *   --------------------------------------------------------------------------------------------
 *   UME-JVM        &lt;pid&gt;      &lt;worker&gt;   &lt;nowMillis&gt;   --                      --
 *   UME-TESTCLASS  &lt;class&gt;    &lt;pid&gt;      &lt;endMillis&gt;   PASSED|FAILED|SKIPPED   &lt;durationMillis&gt;
 *   UME-TEST       &lt;method&gt;   &lt;class&gt;    &lt;endMillis&gt;   PASSED|FAILED|SKIPPED   &lt;durationMillis&gt;
 *   UME-PREDICTED  &lt;class&gt;    &lt;task&gt;     &lt;nowMillis&gt;   --                      &lt;predictedMillis&gt;
 * </pre>
 * Every line is {@code kind name parent when [outcome duration]}, so one split on whitespace reads all four
 * and {@code $3} always names the enclosing thing: a test's class, a class's JVM, a JVM's executor. That is
 * what makes the chain walkable -- a test line does not repeat the JVM because its class already carries it.
 * A JVM line has no outcome and no duration, and simply stops after {@code $4} rather than padding.
 * <p>
 * <b>{@code UME-PREDICTED} IS NOT WRITTEN HERE.</b> It comes from the Gradle ordering step
 * ({@code TestOrderingConventionPlugin}), which runs before any test JVM exists, and it states the time the
 * schedule ASSUMED for a class rather than anything measured. It shares the grammar so that one reader
 * covers the whole log, and it is listed here because this is where the grammar is defined -- but a change
 * to its shape belongs in the plugin. It is emitted for every DISPATCHED class, including the roughly two
 * thirds that contain no test at all and therefore never produce a {@code UME-TESTCLASS} record; those are
 * why the suite needs about twice as many processes as its class count suggests, and a reader that saw only
 * the classes reporting a result could never account for the difference.
 * <p>
 * <b>WHY THE {@code UME-} PREFIX.</b> These lines are selected out of a build log by pattern, and the words
 * {@code TEST} and {@code JVM} are far too common to anchor on: {@code TEST} is a prefix of
 * {@code TESTCLASS}, a substring of most class names in the suite, and a word any product log line might
 * shout. A distinctive prefix makes the pattern {@code ^UME-} and removes the ambiguity for every reader,
 * including a person grepping by hand. It matters more than it looks: the console filter keeps ONLY matching
 * lines and discards half a million others, so a pattern that could match product output would silently pull
 * noise into the records. (Named for the Ume, the river through Umea that carried the log drives.)
 *
 * <h2>READING THE RECORDS -- the reference every consumer follows</h2>
 *
 * THIS SECTION IS THE SINGLE SOURCE FOR HOW THESE RECORDS MUST BE INTERPRETED. It is written next to the
 * emitter because that is the one place guaranteed to be found by anyone changing the record format; the
 * reader that applies these rules is {@code records.py} in {@code jenkins-report}, and it implements them
 * once, for every consumer. Each rule below was got wrong at least once and produced numbers that looked
 * entirely plausible, which is why they are written down rather than left to be re-derived.
 *
 * <h3>The records form a tree, and {@code $3} is the edge</h3>
 *
 * A test's parent is its class; a class's parent is its JVM; a JVM's parent is the Gradle worker. Nothing is
 * repeated -- a TEST line does not name the JVM, because its class already does. So attributing a test to a
 * JVM means two hops, and a reader that wants per-JVM test counts must walk the chain rather than expect a
 * field. This is also why the records can be read from a Jenkins console where forty machines interleave
 * their output: every line carries enough identity to be reattached to its own branch.
 *
 * <h3>1. Nested classes -- the trap that cuts both ways</h3>
 *
 * A class with {@code @Nested} members emits a record for EACH nested class AND one for the enclosing class
 * whose duration ALREADY SPANS them. Both mistakes have been made here:
 * <ul>
 *   <li>SUMMING every class record double-counts. {@code EtherIpCipOdvaIT} read 176s for 88s of work.
 *   <li>Reading only the nested ones loses the outer class entirely, and with it everything the class did
 *       around its nested members.
 * </ul>
 * The rule: for TIMING, COUNTING and SCHEDULING, only the OUTER class exists -- filter on {@code '$'} in the
 * name. A nested class is never dispatched on its own; Gradle hands out the enclosing class and JUnit runs
 * the nested ones inside it. But a nested class's TEST records still belong to the run: fold them into the
 * enclosing class by truncating at {@code '$'}, or the outer class reports zero tests.
 *
 * <h3>2. Retries -- one record per ATTEMPT, and the gap is not work</h3>
 *
 * The retry plugin re-runs a failed class, so a retried class emits TWO class records, usually in different
 * JVMs and separated by an idle gap. Three consequences:
 * <ul>
 *   <li>For SCHEDULING, take the LONGEST attempt, never the sum. The ordering places one dispatch, and a
 *       class that failed and was retried does not reliably cost both. (This is what
 *       {@code Run.measured()} does.)
 *   <li>For OCCUPANCY, both attempts count -- the JVMs really were busy twice.
 *   <li>NEVER measure from first start to last end. That spans the idle gap between attempts and counts
 *       time when nothing was running: one class read 462s against 25s of real work, and was twice
 *       reported as a stall that never happened.
 * </ul>
 * A retry is recognisable because a FAILED record for a class is followed by another record for the same
 * class. Without the outcome field the two are indistinguishable from one class dispatched twice.
 * <p>
 * A CLASS RECORD REPORTS ITS TESTS' OUTCOME, not the JUnit container's. The two differ exactly where it
 * matters: a container whose test failed still finishes {@code SUCCESSFUL}, so writing the container's own
 * result -- as this listener first did -- yields a class record that can never be {@code FAILED}, and the
 * paragraph above becomes untrue. The container's result still decides the rest, and is what reports a class
 * that failed in {@code @BeforeAll}, where no test ran to fail on its own.
 *
 * <h3>3. A repeated method name is EITHER a parameterised case OR a retry</h3>
 *
 * A {@code @ParameterizedTest} emits one TEST record per case, all under the SAME method name -- ten of them
 * for one adapter schema test. A retried test also repeats its name. The outcomes tell them apart: the retry
 * plugin only re-runs what FAILED, so attempts with no failure among them are independent cases and each
 * counts as a test; anything else is one test that was retried and counts once. Collapsing every repeat into
 * one test undercounted a real CI build by 57 of 1182.
 *
 * <h3>4. Skipped, and the two very different things it means</h3>
 *
 * {@code SKIPPED} covers two situations that must not be treated alike, and the DURATION separates them:
 * <ul>
 *   <li>{@code SKIPPED} with duration 0 -- {@code @Disabled} on the type. JUnit never started the class, so
 *       it occupied no JVM. It must be EXCLUDED from timing and from concurrency, and its record exists
 *       only so a reader can see it was part of the run. A tally taken from the records will therefore
 *       report fewer skips than the build tool does, because the tests inside such a class never produce
 *       records at all -- JUnit fires its skip callback on the CONTAINER.
 *   <li>{@code SKIPPED} with a real duration -- a test that ABORTED, typically a failed assumption. It DID
 *       run and DID hold its JVM, so it must be counted in occupancy. Recording an abort as FAILED, which
 *       an earlier version did, turned six such tests into six hard failures in a green suite.
 * </ul>
 *
 * <h3>5. What may update the timings file</h3>
 *
 * The timings file drives the schedule for the NEXT run, so it must record what a class COSTS when it works
 * -- not what it cost while going wrong. Only a class whose record reads PASSED is a sound measurement:
 * <ul>
 *   <li>A FAILED class usually stops early, so its duration understates the real cost. Feeding that in makes
 *       the scheduler believe a class is cheap, place it late, and pay for it on the critical path.
 *   <li>A class that ABORTED ran only part of its tests, for the same reason.
 *   <li>A class SKIPPED at zero cost measures nothing at all.
 * </ul>
 * A class absent from the file is not excluded from running -- it simply sorts last, which is exactly how a
 * newly added test is picked up. But a class PRESENT in the file gets a small positive floor, so that a real
 * test measured at 0.0s still sorts ahead of the several hundred dispatched classes that contain no test.
 *
 * <h3>6. Setup is a subtraction, and it can only be done here</h3>
 *
 * A class's own duration minus the SUM of its tests' durations is everything it did outside test methods:
 * container startup, fixture construction, teardown. Summing STATED durations cannot mis-handle a retry's
 * idle gap, which is what reconstructing from intervals repeatedly got wrong. A negative result is
 * impossible and means the two numbers came from different runs -- report nothing rather than a figure that
 * cannot be true. This subtraction is the reason the TEST records exist at all: no other source sees what a
 * class does around its tests, and one class read 0.3s that way for a class holding its JVM for 9.5s.
 *
 * <h3>7. Runs accumulate; a file or a log may hold several</h3>
 *
 * Nothing clears the per-JVM files, and one Gradle daemon log holds many builds. Runs are separated by a
 * stretch where NOTHING was running -- within a run the JVMs are essentially never all idle at once. Five
 * seconds is the agreed boundary. Deliberately no run id is stamped on the records: it would have to differ
 * on every invocation, and a system property is part of a test task's cache key, so the task could never be
 * restored from the build cache.
 * <p>
 * Stdout from a remote executor IS forwarded into the Jenkins console, so these lines are what make class
 * timing available on CI at all (EDG-990). The pid ties the three kinds together.
 * <p>
 * ONE CONSUMER READS THESE RECORDS, and it reads nothing else: the {@code jenkins-report} tool, which takes
 * a build log from either environment and applies the rules below in one
 * place. The build used to carry a second reader of its own, and the two disagreed inside a single report --
 * 337 classes on one line and 333 on the next, neither number wrong, because they counted different things
 * and neither said so. An older positional record was written here as well, and went the same way: two
 * formats for one fact is how the readers drifted apart in the first place.
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
 * Diagnostic only, and deliberately cheap: one line per class and one per test, printed and forgotten.
 * Nothing reads them during the build.
 */
public class ForkAttributionListener implements TestExecutionListener {

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

    /**
     * Classes with at least one failing test in this JVM, so the class record can say so.
     *
     * <p><b>A CONTAINER'S OWN RESULT IS NOT ITS TESTS' RESULT.</b> JUnit reports a class as
     * {@code SUCCESSFUL} whenever the class itself completed -- a failing test reports its own failure and
     * does not fail its container. So writing {@code outcome(result)} on the class line, as this listener
     * did, produced a record that is never {@code FAILED}: measured across 102 build logs, 4033 class
     * records, not one of them failed, against test records that failed as expected.
     *
     * <p>That silently disabled two rules downstream. A retry is meant to be recognisable as a FAILED record
     * followed by a PASSED one; with no FAILED record it is indistinguishable from one class dispatched
     * twice. And the scheduler is meant to take a class's longest PASSING attempt -- with every attempt
     * marked passing it takes the longest attempt outright, which for a flaky class is the FAILING one,
     * because failing slowly is what made it longest. {@code MySQLLifecycleIT} was proposed to the schedule
     * at its 76.7s failure rather than its 30.2s pass.
     *
     * <p>Keyed on the class name and removed when that class's record is written, so the map holds only
     * classes currently running in this JVM -- at most one per thread.
     */
    private final @NotNull ConcurrentHashMap<String, Boolean> failedTests = new ConcurrentHashMap<>();

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
        // A JVM HANDED ONLY EMPTY CLASSES emits nothing else at all, so this is the only evidence it
        // existed -- and those are two thirds of the dispatched classes, which is why a suite needs far
        // more processes than its class count suggests. Without this record the count is inexplicable.
        //
        // Fields follow the shared grammar: name is the pid, PARENT is the Gradle worker -- the lane
        // this JVM was started for -- and then the time. There is no outcome and no duration, so the
        // line stops at $4 rather than padding to six.
        emit(String.format("UME-JVM %d %s %d", PID, GRADLE_WORKER, jvmStart));
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
                    "UME-TEST %s %s %d SKIPPED 0",
                    key.substring(split + 1), key.substring(0, split), System.currentTimeMillis()));
        });
        // A whole class can be skipped too -- @Disabled on the type. It occupies no JVM, so it gets no
        // TESTCLASS record and correctly does not appear in any timing; this line is what lets a reader
        // still see that it was part of the run rather than silently missing.
        className(identifier)
                .ifPresent(name ->
                        emit(String.format("UME-TESTCLASS %s %d %d SKIPPED 0", name, PID, System.currentTimeMillis())));
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
            final String methodOutcome = outcome(result);

            // REMEMBERED FOR THE CLASS LINE, which is written a few lines below in this same callback --
            // methods finish before their container does. This is the only point at which a failure is
            // visible: the container's own result will not carry it. See `failedTests`.
            if ("FAILED".equals(methodOutcome)) {
                failedTests.put(key.substring(0, split), Boolean.TRUE);
            }

            emit(String.format(
                    "UME-TEST %s %s %d %s %d",
                    key.substring(split + 1), key.substring(0, split), now, methodOutcome, now - start));
        });
        className(identifier).ifPresent(name -> {
            final Long start = startedAt.remove(name);
            if (start == null) {
                return;
            }
            final long now = System.currentTimeMillis();
            final long duration = now - start;

            // A CLASS FAILS WHEN ONE OF ITS TESTS DOES, which its own result does not say -- see
            // `failedTests`. The container's result still decides everything else: it is what reports a
            // class that failed in @BeforeAll, where no test ever ran to record a failure of its own.
            final String outcome = failedTests.remove(name) != null ? "FAILED" : outcome(result);

            // NESTED CLASSES ARE LOGGED BUT MUST BE IGNORED WHEN AGGREGATING. A class with @Nested
            // inner classes produces a line per nested class AND a line for the enclosing class
            // whose duration already spans all of them, so summing every line double-counts:
            // EtherIpCipOdvaIT reported 176s for 88s of work. A nested class is also never
            // scheduled on its own -- Gradle dispatches the outer class and JUnit runs the nested
            // ones inside it -- so the outer class is the only meaningful unit for timing,
            // distribution and counting. Filter on '$' in the name.
            //
            // CLASS-LEVEL TIMING EXISTS NOWHERE ELSE. Before this record it had to be reconstructed from
            // per-method events, which cannot see anything a class does outside a test method: OpenLdapIT
            // read 0.3s that way for a class that occupies its JVM for 9.5s, was scheduled as trivially
            // fast, and ran last where nothing could overlap it. Stdout from a remote executor IS
            // forwarded into the console, so one line here closes that gap on CI and locally alike, with
            // no artifact collection anywhere (EDG-990).
            //
            // ONE LINE PER ATTEMPT, and the outcome is what makes a retry recognisable: two records for
            // one class otherwise mean either a retry or two separate invocations, and those are
            // indistinguishable. A FAILED record followed by a PASSED one is a retry.
            //
            // THE OUTCOME IS THE TESTS', NOT THE CONTAINER'S. Reporting `outcome(result)` here made that
            // sentence false in every log written so far -- a container whose test failed is still
            // SUCCESSFUL, so no class record ever failed and no retry was recognisable by outcome.
            //
            // The start time is DERIVED by readers as end - duration rather than printed: two
            // independently written fields can disagree, a derived one cannot.
            //
            // The PARENT is the pid alone. The Gradle worker number was printed here too and is now
            // dropped: the JVM line already pairs this pid with its worker, so repeating it made the
            // same fact writable from two places, which is how they come to disagree.
            //
            // TO BOTH PLACES, as with TEST above: the fork file, which is all a local run leaves, and
            // stdout, which is all a remote executor leaves. One grammar, so one reader serves both.
            emit(String.format("UME-TESTCLASS %s %d %d %s %d", name, PID, now, outcome, duration));
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
     * One record, to standard output. That is the only destination, and it is enough.
     * <p>
     * THE CONSOLE IS THE RECORD. Every record states its own absolute time and its own parent, so a class
     * knows its process and a test knows its class -- which means the console alone is a complete account of
     * a run, with nothing to reconstruct from what a reader knows about how the lines were arranged. Gradle
     * forwards a remote executor's stdout into the build log exactly as it forwards a local fork's, so the
     * same log serves a 40-executor CI run and a five-fork local one.
     * <p>
     * These records USED to be written to a per-JVM file as well, on the reasoning that a local console
     * scrolls past unrecorded. That turned out to buy nothing and cost real correctness: the directory was
     * never cleared between runs, so reading it needed the run separated out of an accumulated pile, and
     * getting that subtly wrong folded one run's tests into another -- a 4795-test suite read 9590. The
     * console has no such problem, because a build log IS one run.
     */
    private void emit(final @NotNull String line) {
        System.out.println(line);
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
}
