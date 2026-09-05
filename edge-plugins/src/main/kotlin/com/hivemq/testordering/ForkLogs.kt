package com.hivemq.testordering

import java.io.File

/**
 * ONE PLACE THAT ANSWERS "HOW LONG DID THIS CLASS TAKE, AND WHEN".
 *
 * Every consumer of test timings reads this file. It exists because the same four mistakes were made
 * repeatedly, each producing plausible numbers that were wrong, and each caught only by someone noticing
 * a figure that did not smell right:
 *
 *  1. MEASURING FROM THE FIRST ATTEMPT TO THE END OF THE RETRY. A retried class's `<testsuite time=>` in
 *     the JUnit XML spans first-attempt-start to last-attempt-end, INCLUDING the idle gap between them.
 *     One class read 462s against 25s of real work. Reported twice as a "stall" that never happened.
 *  2. COUNTING TEST-METHOD TIME AND CALLING IT CLASS TIME. Summing `<testcase time=>` counts only time
 *     inside test methods, so a class whose cost is a container start measures near zero:
 *     `OidcServiceKeycloakIT` holds its fork 30.9s, spends 2.5s in methods, and was recorded as 1.3s.
 *  3. NESTED CLASSES, IN BOTH DIRECTIONS. Summing every record double-counts, because the outer class's
 *     duration already spans its nested ones (`EtherIpCipOdvaIT`: 176s reported for 88s of work). But
 *     reading the JUnit XML instead loses the outer class entirely -- JUnit writes a file per NESTED
 *     class and none for the enclosing one, so `OpenLdapIT` measured 0.3s for a class that takes 10s,
 *     was scheduled as trivially fast, and ran last where nothing could overlap it.
 *  4. SUMMING ACROSS RUNS. The fork-log directory is never cleared, so it holds months of runs.
 *
 * THE LISTENER'S RECORDS ARE THE AUTHORITY, not the JUnit XML. They are written by
 * `ForkAttributionListener` from JUnit's own `executionStarted`/`executionFinished` callbacks, so a record
 * is wall-clock time for one attempt of one class in one JVM: setup and teardown included (fixing 2), the
 * outer class present as its own record (fixing 3), and a retry appearing as a SEPARATE record in a
 * different JVM rather than one span across the gap (fixing 1). Run separation is handled here (fixing 4).
 *
 * The JUnit XML remains the authority for PASS/FAIL and for test counts. It is not a timing source.
 *
 * ONE PIPELINE, ONE GRAMMAR. The listener prints the same records to standard output AND to these files,
 * so a Jenkins console and a local fork log carry identical lines and every consumer -- this task, and the
 * vault's testrun-records.py / testrun-condense.py / testrun-report.py -- reads the same bytes the same
 * way. Before that, local and CI were read by different code from different artefacts, and the two
 * measured subtly different things; that split is where a month of contradictory timing numbers came from.
 *
 *     $1 kind     $2 name     $3 parent   $4 endMillis    $5 outcome      $6 durationMillis
 *     -------------------------------------------------------------------------------------
 *     JVM         <pid>       <worker>    <nowMillis>     --              --
 *     TESTCLASS   <class>     <pid>       <endMillis>     PASSED|FAILED   <durationMillis>
 *     TEST        <method>    <class>     <endMillis>     PASSED|FAILED   <durationMillis>
 *
 * `$3` always names the ENCLOSING thing -- a test's class, a class's JVM, a JVM's executor -- so the chain
 * is walkable and no fact is written twice. The TEST lines are what make SETUP knowable: a class's own
 * duration minus the sum of its tests is everything it did around them, which is invisible to any reader
 * working from per-method events alone.
 */

/** One class's stay in one test JVM: wall-clock, one attempt, setup and teardown included. */
data class ClassRun(
    val className: String,
    val startMillis: Long,
    val endMillis: Long,
    val jvmPid: String,
    /**
     * Time spent INSIDE this class's test methods, or null when unmeasured.
     *
     * `durationMillis - inTestsMillis` is SETUP: container startup, fixture construction, teardown --
     * everything a class does around its tests. It is the number that explains why a class is expensive,
     * as opposed to merely how expensive it is, and nothing could compute it until the listener began
     * recording each test with its own duration alongside each class.
     */
    val inTestsMillis: Long? = null
) {
    val durationMillis: Long get() = endMillis - startMillis

    /** Setup and teardown: the class's own time minus the time inside its tests. Null when unmeasured. */
    val setupMillis: Long?
        get() = inTestsMillis?.let { inside ->
            // A class shorter than the tests inside it is impossible and means the two numbers came from
            // different runs. Report nothing rather than a figure that cannot be true.
            (durationMillis - inside).takeIf { it >= 0 }
        }
}

/** One TEST record: a single attempt of one test method. */
private data class TestEvent(
    val className: String,
    val methodName: String,
    val endMillis: Long,
    val durationMillis: Long,
    val outcome: String
)

/**
 * One test method's outcome, as the records state it.
 *
 * Kept per (class, method) rather than per line because a RETRY emits a second record for the same
 * method: a method that failed and then passed is FLAKY, not failed, and only the sequence of its
 * attempts distinguishes the two.
 */
data class TestRuns(
    val className: String,
    val methodName: String,
    val outcomes: List<String>
) {
    val skipped: Boolean get() = outcomes.all { it == "SKIPPED" }
    val passed: Boolean get() = outcomes.any { it == "PASSED" }
    val flaky: Boolean get() = passed && outcomes.any { it == "FAILED" }

    /** Failed on every attempt. A skipped test never ran, so it is neither passed nor failed. */
    val failed: Boolean get() = !passed && !skipped
}

/** Every class record of a single test run, already separated from the other runs in the directory. */
data class TestRun(
    val classes: List<ClassRun>,
    /** Every test method of this run, with all of its attempts. Empty for a run with no TEST records. */
    val tests: List<TestRuns> = emptyList()
) {
    /** Classes that ended in failure on their last attempt -- the ones whose timings are unreliable. */
    val failedClasses: List<String>
        get() = tests.filter { it.failed }.map { it.className }.distinct()

    val passedCount: Int get() = tests.count { it.passed && !it.flaky }
    val flakyCount: Int get() = tests.count { it.flaky }
    val failedCount: Int get() = tests.count { it.failed }
    val skippedCount: Int get() = tests.count { it.skipped }

    /** Tests that actually EXECUTED. A skip is counted separately, never as part of the total. */
    val executedCount: Int get() = tests.count { !it.skipped }

    /** Time inside test methods, summed over every attempt. */
    val inTestsMillis: Long get() = classes.sumOf { it.inTestsMillis ?: 0L }

    val startMillis: Long get() = classes.minOf { it.startMillis }
    val endMillis: Long get() = classes.maxOf { it.endMillis }

    /** Wall clock from the first class starting to the last one finishing. */
    val wallClockMillis: Long get() = (endMillis - startMillis).coerceAtLeast(1)

    /** Total fork occupancy: what has to be packed into the forks. Attempts count individually. */
    val workMillis: Long get() = classes.sumOf { it.durationMillis }

    /** Forks busy on average, i.e. how much of the wall clock was spent doing work. */
    val concurrency: Double get() = workMillis.toDouble() / wallClockMillis

    /**
     * The cost the SCHEDULER should plan for, per class: the longest attempt, not the sum.
     *
     * A retried class occupies a fork twice, but the ordering decides where to place ONE dispatch, and a
     * class that failed and was retried is not a class that reliably costs the total of both. The longest
     * attempt is what a fork must be able to absorb.
     *
     * WHY `max` IS RIGHT HERE AND WRONG IN THE JENKINS READER. One fork-log record is one COMPLETE class
     * execution -- `ForkAttributionListener` writes a line when the class finishes, not per test method --
     * so several records for one class mean several attempts, and `max` picks one of them. The Jenkins
     * console log carries one event pair per TEST METHOD instead, so the same rule applied there reports a
     * class at its longest single method: `BridgeReconnectCyclesIT` read 17.1s for 55.2s of occupancy,
     * making a whole CI run look 40% cheaper. That reader sums its segments and excludes only a retry's
     * idle gap; see `CLAUDE/_scripts/jenkins-classtimes.py` and its fixture tests.
     *
     * The rule is the same in both -- charge the fork for the work, never for the gap -- but the unit of
     * input differs, so the code cannot. Check which one you are holding before copying either.
     */
    fun longestPerClass(): Map<String, Double> =
        classes.groupBy { it.className }
            .mapValues { (_, runs) -> runs.maxOf { it.durationMillis } / 1000.0 }
}

/**
 * No class running for this long ends a run.
 *
 * Within a run the forks are essentially never all idle -- Gradle hands the next class over the moment one
 * frees up -- while between runs everything stops to recompile. Measured across several values: 5s recovers
 * full runs at their exact class count where 15s and above merge neighbouring runs.
 */
const val RUN_BOUNDARY_MILLIS = 5_000L

/**
 * Read every class record in [dir], newest run last.
 *
 * NESTED CLASSES ARE DROPPED HERE, once, so no caller has to remember to. The outer class's record already
 * spans them, and a nested class is never dispatched on its own -- Gradle hands out the enclosing class and
 * JUnit runs the nested ones inside it.
 */
fun readRuns(dir: File): List<TestRun> {
    val files = dir.listFiles { f -> f.isFile && f.name.endsWith(".log") }?.toList().orEmpty()
    val all = mutableListOf<ClassRun>()
    // Time inside test methods, summed per class and folded at '$' so a @Nested test counts towards the
    // class that was actually dispatched. Summing STATED durations cannot charge a retry's idle gap.
    // One TEST record. The end time is kept so these can be split into runs alongside the class
    // records, rather than folded in globally and attributed to whichever run asks.
    val testEvents = mutableListOf<TestEvent>()
    files.forEach { file ->
        file.forEachLine { line ->
            if (line.startsWith("#")) return@forEachLine
            val f = line.split(' ')
            if (f.size != 6) return@forEachLine
            when (f[0]) {
                "TEST" -> {
                    val duration = f[5].toLongOrNull() ?: return@forEachLine
                    val end = f[3].toLongOrNull() ?: return@forEachLine
                    // FOLDED AT '$': a @Nested test's record names the nested class, but its time and
                    // its outcome belong to the class that was actually dispatched.
                    testEvents += TestEvent(f[2].substringBefore('$'), f[1], end, duration, f[4])
                }
                "TESTCLASS" -> {
                    val name = f[1]
                    if (name.contains('$')) return@forEachLine
                    val end = f[3].toLongOrNull() ?: return@forEachLine
                    val duration = f[5].toLongOrNull() ?: return@forEachLine
                    // A class SKIPPED WITH ZERO DURATION never ran -- @Disabled on the type -- so it
                    // occupied no JVM and belongs in no timing; it is recorded only so a reader can see it
                    // was part of the run. A class whose tests ABORTED also reads SKIPPED but has a real
                    // duration, because it did start, did hold its JVM, and must be counted. Keying on the
                    // outcome alone would silently drop that occupancy.
                    if (f[4] == "SKIPPED" && duration == 0L) return@forEachLine
                    // The pid comes from the RECORD, not the file name: the two agree today, but a
                    // record carrying its own identity is what lets the same reader work on a Jenkins
                    // console, where there are no per-JVM files at all.
                    all += ClassRun(name, end - duration, end, f[2])
                }
            }
        }
    }
    if (all.isEmpty()) return emptyList()

    // A run boundary is a stretch where NOTHING was running -- not a gap between consecutive starts,
    // which cannot tell a slow class from a pause and once merged three runs into one.
    val sorted = all.sortedBy { it.startMillis }
    val runs = mutableListOf(mutableListOf(sorted.first()))
    var busyUntil = sorted.first().endMillis
    sorted.drop(1).forEach { run ->
        if (run.startMillis > busyUntil + RUN_BOUNDARY_MILLIS) {
            runs += mutableListOf(run)
        } else {
            runs.last() += run
        }
        busyUntil = maxOf(busyUntil, run.endMillis)
    }

    // TESTS ARE ASSIGNED TO A RUN BY WHEN THEY FINISHED, not folded in globally. The directory holds
    // every run ever made -- Gradle never clears it -- so a class name that appears in three runs has
    // three sets of tests, and attributing all of them to one run inflates its in-test time and can
    // make setup come out negative.
    return runs.map { classRuns ->
        val from = classRuns.minOf { it.startMillis }
        val to = classRuns.maxOf { it.endMillis }
        val mine = testEvents.filter { it.endMillis in from..to }
        val inTests = mine.groupBy { it.className }.mapValues { (_, e) -> e.sumOf { it.durationMillis } }
        TestRun(
            classRuns.map { it.copy(inTestsMillis = inTests[it.className]) },
            mine.groupBy { it.className to it.methodName }
                .map { (key, e) -> TestRuns(key.first, key.second, e.map { it.outcome }) }
        )
    }
}

/**
 * The run just finished, or null when the directory holds none.
 *
 * THE NEWEST, simply that -- callers ask right after a run. Choosing the LARGEST instead, to reject a stray
 * single-class invocation that landed afterwards, was tried and is worse: one extra record makes an older
 * run larger, and it then wins outright. That silently reported the previous evening's run as the current
 * one (43:32 against a true 41:46). A stray announces itself -- the caller prints the class count, sees
 * "1 class", and re-runs -- so prefer the failure that is visible.
 */
fun newestRun(dir: File): TestRun? = readRuns(dir).maxByOrNull { it.startMillis }
