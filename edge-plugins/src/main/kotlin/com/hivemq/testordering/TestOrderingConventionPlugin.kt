package com.hivemq.testordering

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.withType

/**
 * Hand Gradle the test classes slowest-first in a snake, so the parallel forks finish together.
 *
 * Gradle deals class *i* to fork *(i mod maxParallelForks)* and never rebalances or steals work, so the
 * dispatch ORDER alone decides how evenly the forks are loaded. In directory order -- alphabetical, i.e.
 * arbitrary with respect to runtime -- the forks drifted apart by 194 s over a ~15 minute suite: the unlucky
 * fork was still running long after the others had gone idle.
 *
 * Slowest-first is the classic fix (long jobs first, short ones to fill the gaps), but on its own it hands
 * fork 0 the largest class of every group of `maxParallelForks` and fork N-1 the smallest, so fork 0 finishes
 * last by construction. Reversing every second group cancels that bias -- hence "snake". Measured over the
 * full integration suite at 5 forks: spread 194 s -> 57 s, speedup 4.38x -> 4.72x of the 5 available. On the
 * unit suite the effect is larger still: without ordering it plateaus at 2.2x however many forks it is
 * given; with it, 5 forks reach 4.04x.
 *
 * THIS REORDERS; IT DOES NOT SELECT. Every instantiable class is dispatched and JUnit decides what to run, as
 * it always did -- so nothing here has to be right about which classes are tests. The committed timings carry
 * no authority over membership either: a class that is not listed is worth 0 seconds and therefore sorts
 * last, which is the correct place for both a helper (JUnit finds nothing in it) and a newly added test (it
 * runs, it just cannot be placed by the balancing until it has been measured).
 *
 * That is what keeps the committed file safe to leave stale. It is an optimisation hint, and the failure mode
 * of a wrong or missing entry is a slightly worse distribution -- never a test that stops running.
 *
 * LOCAL RUNS ONLY, by design -- it is switched off when `CI_RUN` is set. On CI the integration suite is
 * farmed out to remote executors that ignore dispatch order, and the unit suite, while it does use local
 * forks there, has never been on the critical path. This exists to make a developer's own test runs finish
 * sooner; keeping it off CI also keeps a mechanism that decides what gets scanned for tests away from the
 * shared pipeline.
 */
class TestOrderingConventionPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val committed = project.layout.projectDirectory.file("gradle/test-class-timings.csv")

        // A default timeout for every test, so no single test can hang a build indefinitely.
        //
        // EDG-864 put this on the integration suite after a run of builds that hung for hours -- one for
        // 22.4 hours before somebody killed it by hand. EDG-973 moved it here and gave the unit suite one
        // too, which had none at all.
        //
        // WHY IT MATTERS BEYOND THE BUILD THAT HANGS: a test's runtime is written back into Test
        // Distribution's per-class history, which keeps a rolling average (avg = avg * 0.9 + current * 0.1).
        // The plugin sizes its executor request as ceil(total estimate / longest class estimate), so ONE
        // 22-hour observation dragged a class average to ~2h and throttled EVERY later build to 3 executors
        // until tens of clean runs diluted it out. That is what happened on 2026-08-03. A hang is not a
        // one-build problem.
        //
        // EVERY PROJECT SETS ITS OWN VALUE in its gradle.properties, next to a note on why that number:
        // 60s for the unit tests (slowest measured 5.2s), 10m for the integration suite (slowest whole
        // class 67s). Both sit an order of magnitude above anything observed, so a healthy test cannot
        // trip them while a runaway is caught long before it poisons anything. Explicit @Timeout
        // annotations still win, so the deliberately long tests keep their own values.
        //
        // The fallback below is a backstop for a project that forgets, NOT a policy default. It matches the
        // most generous value any suite here uses, so it cannot cut a legitimate test short, while still
        // being far below the hours a hang costs. If you are reading this because a test failed at 10
        // minutes, the fix is to set testTimeout in that project rather than to change this line.
        //
        // Set UNCONDITIONALLY, unlike the ordering below: this can only turn a hang into a named failure,
        // and CI is exactly where the poisoned-history problem bites.
        val timeout = project.findProperty("testTimeout") as String? ?: "10m"

        project.tasks.withType<Test>().configureEach {
            systemProperty("junit.jupiter.execution.timeout.default", timeout)

            // THE RECORDS GO TO TWO PLACES, and neither is redundant.
            //
            // The CONSOLE is what CI keeps: the tests run on up to 40 remote executors whose disks are
            // discarded when they are released, so a Jenkins log is all that comes back. The test JVMs' own
            // output is not echoed (`showStandardStreams = false`), because it was 96% of a 78 MB build log;
            // these lines are re-printed through `onOutput`, which sees a remote executor's output exactly as
            // it sees a local fork's, so a plain `gradlew test` prints roughly 1500 records and nothing else.
            //
            // The FILE is what a local run keeps, so reporting on one needs no capture step -- no `tee` to
            // remember before a run that takes twelve minutes. It is not a copy of the console: the ordering
            // step's UME-PREDICTED records never pass through `onOutput` at all, so Gradle's own stored copy
            // of the test output does not contain them. See `RecordFile`.
            //
            // Applied here rather than in each build file so the suites cannot drift apart. They had: the
            // integration suite echoed its records to the console while the unit suite did not.
            val records = RecordFile(
                project.layout.buildDirectory.file("test-records/$name.log").get().asFile,
                logger
            )
            umeRecordsToConsole(this, records)
            doFirst {
                // FIRST, and unconditionally -- see `RecordFile.begin`. The ordering step below returns early
                // in several cases, so it cannot be what guarantees the file holds a single run.
                records.begin()
                orderTestClasses(this as Test, committed.asFile, records)
            }
            doLast {
                records.end()
            }
        }

        // THERE IS NO REPORTING TASK HERE ANY MORE, deliberately. Reading the records, measuring occupancy,
        // simulating an ordering and writing the timings file all live in the jenkins-report tool, which
        // reads the same records from a Jenkins console log and from a local run alike. A copy inside the
        // build could only ever read the local half, and two implementations of the same rules is exactly how
        // the readers came to disagree -- one reporting 337 classes where the other said 333, neither wrong.
        //
        //   ./gradlew test
        //   ../jenkins-report/bin/edge_report.py build/test-records/test.log \
        //       --timings gradle/test-class-timings.csv
        //
        // The report says what the new ordering would save and writes the file; committing it adopts it.
        // What the build still owns is the ordering itself -- reading the committed file, arranging the
        // classes, and recording what it assumed -- because only the build can act on that.
    }
}

/**
 * Replace the task's scanned class list with the same classes in a balanced order.
 *
 * Fail-soft throughout: no timings file, no class directories, or an unreadable file all leave the order
 * exactly as Gradle would have had it.
 */
internal fun orderTestClasses(
    task: Test,
    timingsFile: java.io.File,
    records: RecordFile
) {
    // LOCAL RUNS ONLY -- this exists to make a developer's own test runs finish sooner. CI_RUN is the same
    // switch the integration suite uses to turn on Develocity Test Distribution, so on CI this stays out of
    // the way entirely.
    //
    // It would not help there anyway. The integration suite is farmed out to remote executors, and that
    // scheduler partitions by its own rolling average of per-class runtimes without ever looking at the
    // order the classes arrive in. The unit suite does run in local forks on CI, but it has never been on
    // the critical path -- it finishes alongside the far longer integration branch.
    //
    // Keeping it off CI also contains the blast radius: this manipulates testClassesDirs, which decides
    // what gets scanned for tests, and a mistake there is the kind that stops tests running rather than
    // failing loudly. Not worth that exposure on the shared pipeline for a benefit that is not there.
    if (System.getenv("CI_RUN") != null) {
        task.logger.info("Test class ordering: CI run, leaving the order alone")
        return
    }

    if (!timingsFile.isFile) {
        task.logger.info("Test class ordering: no ${timingsFile.name}, leaving the order to Gradle")
        return
    }

    val classDirs = task.testClassesDirs.files.filter { it.isDirectory }
    if (classDirs.isEmpty()) {
        return
    }

    val timings = readTimings(timingsFile)
    if (timings.isEmpty()) {
        task.logger.info("Test class ordering: ${timingsFile.name} has no usable entries, order unchanged")
        return
    }

    val classes = classDirs.flatMap { findDispatchableTestClasses(it) }.distinctBy { it.name }
    if (classes.isEmpty()) {
        return
    }

    // The task's own fork count, read at execution time, so the snake always matches what actually runs.
    val forks = task.maxParallelForks.coerceAtLeast(1)
    val byName = classes.associateBy { it.name }
    val ordered = arrange(classes.map { it.name }, timings, forks)

    // A FileCollection of file TREES, one per class, in this order. Gradle's scanner walks the collection
    // in the order its elements were added, so this preserves the arrangement.
    //
    // Each element must be a FileTree rooted at the class DIRECTORY, not a bare file: Gradle derives the
    // class name from the file's path RELATIVE TO the tree's root, so a plain file has nothing to relativise
    // against and is not recognised as a test class at all ("No tests found for given includes").
    //
    // The include pattern is built from the file the scan already found, rather than searching for the
    // class again by name -- a small tidy-up, not a measured win: the whole ordering step completes in
    // well under a second even on the integration suite's ~700 classes.
    var arranged: FileCollection = task.project.files()
    ordered.forEach { name ->
        val entry = byName[name] ?: return@forEach
        arranged += task.project.fileTree(entry.root) { include(entry.relativePath) }
    }
    task.testClassesDirs = arranged

    // testClassesDirs is what gets SCANNED for tests; it is not on the classpath by itself. Narrowing it to
    // individual files would hide every helper and inner class from the test JVM, so add the directories
    // back to the classpath.
    task.classpath += task.project.files(classDirs)

    // ONE RECORD PER DISPATCHED CLASS, carrying the time this schedule assumed for it.
    //
    // WITHOUT THESE the log says what the run cost but not what it expected to cost, and the two together are
    // what make the schedule judgeable: comparing the ordering that was used against the one the measurements
    // argue for needs both halves. Reading the predictions from the timings file instead would tie the
    // analysis to a working copy on somebody's machine -- unreachable from a CI log, and already stale by the
    // time anyone looked, since the file is rewritten whenever a run is adopted.
    //
    // Emitted for EVERY dispatched class, including the ~370 that hold no test at all. Those are the reason a
    // 332-class suite needs 30 processes rather than 15 (see `arrange`), and a reader that saw only the
    // classes which reported a result could never account for the difference.
    //
    // ONE RECORD PER DISPATCHED CLASS, carrying the time this schedule assumed for it.
    //
    // These are half of what makes the schedule judgeable: without them a log says what the run COST but not
    // what it expected to cost, and comparing the two is the entire question. Reading them from the timings
    // file instead would tie the analysis to a working copy on somebody's machine -- unreachable from a CI
    // log, and already stale by the time anyone looked, since that file is rewritten whenever a run is
    // adopted.
    //
    // Emitted for EVERY dispatched class, including the two thirds that hold no test at all. Those are why a
    // 332-class suite needs 30 processes rather than 15 (see `arrange`), and a reader that saw only the
    // classes which reported a result could never account for the difference.
    //
    // To the console at `lifecycle` and to the task's record file, like every other record -- not behind
    // `--info`, or an ordinary `gradlew test` log could not answer the question these exist to answer. The
    // format is the shared six-field grammar; see the "READING THE RECORDS" reference in
    // ForkAttributionListener, which these must stay consistent with.
    val now = System.currentTimeMillis()
    val lines = ordered.map { name ->
        "UME-PREDICTED $name ${task.path} $now -- ${((timings[name] ?: 0.0) * 1000).toLong()}"
    }
    // TO BOTH DESTINATIONS, as every other record goes. These are the ONLY records that do not pass through
    // `onOutput` -- they are written by the build, before any test process exists -- which is exactly why the
    // file has to exist: Gradle's own store of the test output cannot contain them.
    lines.forEach {
        task.logger.lifecycle(it)
        records.append(it)
    }

    val untimed = ordered.count { it !in timings }
    val totalSeconds = ordered.sumOf { timings[it] ?: 0.0 }
    task.logger.lifecycle(
        "Test class ordering: ${ordered.size} classes over $forks forks, " +
            "$untimed without a recorded time (sorted last), " +
            "${"%.0f".format(totalSeconds)}s of measured work, " +
            "ideal ${"%.0f".format(totalSeconds / forks)}s per fork"
    )
}
