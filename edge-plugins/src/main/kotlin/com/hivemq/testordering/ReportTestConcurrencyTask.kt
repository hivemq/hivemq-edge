package com.hivemq.testordering

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction

/**
 * Report how well the last test run filled its parallel forks, and whether a fresh ordering would help.
 *
 * Two halves:
 *
 *  - **What actually happened.** From the per-JVM fork logs: how many forks were busy at each decile of the
 *    run, the average concurrency, and how much of the wall clock was spent below full occupancy. This is
 *    measured, not modelled.
 *  - **What a different ordering would give.** A simulation, for a range of fork counts, of the snake
 *    arrangement under the COMMITTED ordering versus the ordering THIS RUN implies.
 *
 * The gap between those two simulated columns is the decision: if this run's ordering is meaningfully
 * better, copy the generated file over the committed one and raise a PR. If not, leave it alone.
 *
 * TWO THINGS THE SIMULATION DELIBERATELY DOES NOT DO.
 *
 * The committed file's SECONDS are never added up. They only decide the sort order. Both simulations take
 * every duration from this run, so the ordering is the only variable between them -- which is what makes the
 * two numbers comparable at all. (The seconds stay in the committed file because they are useful to read.)
 *
 * Both simulations cover exactly the same set of classes: everything this run measured. A class the run
 * measured but the committed file has never heard of is unranked, so it sorts last in the committed
 * ordering -- but its time still counts. Dropping it would let the committed ordering quietly benefit from
 * ignoring real work, and the comparison would be rigged.
 */
abstract class ReportTestConcurrencyTask : DefaultTask() {
    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val resultsDir: DirectoryProperty

    /**
     * Per-JVM logs from ForkAttributionListener. Not an @InputDirectory: the suite that has no listener
     * never creates it, and a missing @InputDirectory fails the task rather than skipping the section.
     */
    @get:Internal
    abstract val forkLogsDir: DirectoryProperty

    @get:Optional
    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val committedTimings: RegularFileProperty

    @get:OutputFile
    abstract val runTimings: RegularFileProperty

    @get:Input
    abstract val forkCounts: ListProperty<Int>

    @get:Input
    @get:Optional
    abstract val actualForks: Property<Int>

    /** `-Pmd` emits Markdown -- tables and headings, for pasting into a document. */
    @get:Internal
    val markdown: Boolean get() = project.hasProperty("md")

    init {
        group = "verification"
        description = "Report how well the last test run filled its forks, and whether a fresh ordering would help"
        outputs.upToDateWhen { false }
    }

    @TaskAction
    fun run() {
        val results = readRunResults(resultsDir.get().asFile)
        if (results.isEmpty()) {
            throw GradleException(
                "No JUnit XML found in ${resultsDir.get().asFile}. Run the test suite first -- " +
                    "this reports on a run that has happened."
            )
        }

        val failed = results.filterValues { it.failures > 0 }
        if (failed.isNotEmpty()) {
            logger.warn("")
            logger.warn("WARNING: ${failed.size} class(es) failed in this run.")
            logger.warn("A failing class stops early, so its measured time understates the real one.")
            logger.warn("Treat the numbers below as provisional until the suite is green.")
        }

        // R -- this run. The durations here are the ONLY durations used anywhere below.
        //
        // FROM THE FORK LOGS, NOT THE JUNIT XML. The XML is authoritative for pass/fail (above) and for
        // test counts, and is not a timing source: its per-class time either spans a retry's idle gap or,
        // summed per test method, omits the fixture work that dominates a container-backed class -- and it
        // has no entry at all for a class with @Nested members. See ForkLogs.kt, which documents all four
        // traps and is the single reader every consumer of these timings goes through.
        //
        // Falls back to the XML only when the logs are absent (-PnoForkLogs, or a run predating them), so
        // the task still produces something rather than failing.
        val run = newestRun(forkLogsDir.get().asFile)
        val runtimes = run?.longestPerClass() ?: results.mapValues { it.value.seconds }
        if (run == null) {
            logger.warn("")
            logger.warn("No fork logs found -- falling back to JUnit XML timings, which understate any")
            logger.warn("class whose cost is in its fixture. Re-run without -PnoForkLogs for real numbers.")
        }
        val committedFile = committedTimings.orNull?.asFile
        val committed = committedFile?.let { readTimings(it) } ?: emptyMap()

        // What gets WRITTEN is smoothed against the committed value: a class that got slower is believed at
        // once, one that got faster comes down gradually (see `smooth`). What gets SIMULATED below is this
        // run's raw durations -- the two orderings must be scored against the same clock, or the comparison
        // measures the smoothing rather than the ordering.
        val smoothed = runtimes.mapValues { (name, seconds) -> smooth(committed[name], seconds) }

        writeTimings(
            runTimings.get().asFile,
            smoothed,
            // ONE header, because adopting a new ordering is a plain `cp` of this file over the committed
            // one. So it has to read as the committed file -- that is where someone will find it and wonder.
            // Short by design: the explanation lives in the Edge Lore, not in a data file.
            listOf(
                "This file is used to guide the distribution of test classes to",
                "concurrent JVMs (forks) to minimize total wall clock time.",
                "DO NOT EDIT THIS FILE MANUALLY.",
                "",
                "Versions of this file exist in two different places:",
                "  gradle/  the committed ordering, used by every local test run",
                "  build/   what the last run measured, regenerated by the report below",
                "",
                "After a successful test run use the following command to get a report:",
                "  ./gradlew :hivemq-edge:reportTestConcurrency   (core unit tests)",
                "  ./gradlew reportTestConcurrency                (integration tests)",
                "",
                "If it shows a real gain, adopt this ordering and raise a PR:",
                "  cp build/test-class-timings.csv gradle/test-class-timings.csv",
                "",
                "Columns: 'seconds' orders the classes and is smoothed -- a class that got",
                "slower takes its new time at once, one that got faster moves halfway there,",
                "so a single fast run cannot undo a real cost. 'measured' is what the last run",
                "actually recorded, and is informational. Only the first number is read back.",
                "",
                "https://hivemq.github.io/hivemq-edge-lore/3-Quality-and-Testing/balancing-the-parallel-forks/"
            ),
            measured = runtimes
        )

        reportTotals()
        reportOccupancy()
        reportSimulation(runtimes, committed, committedFile)
    }

    /** What the run consisted of, before any analysis of how it was spread. */
    private fun reportTotals() {
        val t = readRunTotals(resultsDir.get().asFile)
        // Time in test methods is the sum of the individual testcase times. It is LESS than the class time
        // in the occupancy section, which measures how long each class HELD its JVM and so also carries
        // per-class setup and teardown.
        logger.lifecycle("")
        if (markdown) {
            logger.lifecycle("# Test concurrency report")
            logger.lifecycle("")
            logger.lifecycle("## The run")
            logger.lifecycle("")
            logger.lifecycle("| classes | tests | in test methods | passed | flaky | failed | skipped |")
            logger.lifecycle("| --: | --: | --: | --: | --: | --: | --: |")
            logger.lifecycle(
                "| ${t.classes} | ${t.tests} | ${fmt((t.seconds * 1000).toLong())} | " +
                    "${t.passed} | ${t.flaky} | ${t.failed} | ${t.skipped} |"
            )
        } else {
            logger.lifecycle(
                "Test run -- ${t.classes} classes, ${t.tests} tests, " +
                    "${fmt((t.seconds * 1000).toLong())} in test methods"
            )
            logger.lifecycle("  ${t.passed} passed, ${t.flaky} flaky, ${t.failed} failed, ${t.skipped} skipped")
        }
    }

    /**
     * What the run actually did, from the fork logs.
     *
     * Each line is `endEpochMs offsetFromJvmStart durationMs sequence className`, so a class occupied its
     * JVM over [end - duration, end]. Overlapping those intervals gives the number of forks busy at any
     * moment.
     *
     * The window is FIRST CLASS START to LAST CLASS END -- test execution only. Everything before the first
     * class (compilation, Gradle configuration, JVM startup) is not something the ordering can influence,
     * and including it drags the concurrency down by a factor that has nothing to do with the balance.
     *
     * Concurrency is AVERAGED OVER EACH DECILE, not sampled at an instant. Sampling lands between classes
     * often enough to print 0 in the middle of a fully busy run, which reads as a stall that never happened.
     */
    private fun reportOccupancy() {
        val dir = forkLogsDir.orNull?.asFile
        // Reading, run separation, nested-class handling and "which run is this" all live in ForkLogs.kt,
        // so this section and the timings written above cannot disagree about what the run was.
        val allRuns = dir?.let { readRuns(it) }.orEmpty()
        val spans = allRuns.maxByOrNull { it.startMillis }
        if (spans == null) {
            logger.lifecycle("")
            logger.lifecycle("Fork occupancy: no fork logs (the run predates them, or -PnoForkLogs was set)")
            return
        }
        val ignored = allRuns.sumOf { it.classes.size } - spans.classes.size
        val otherRuns = allRuns.size - 1

        val start = spans.startMillis
        val window = spans.wallClockMillis
        val work = spans.workMillis

        // Concurrency is AVERAGED OVER EACH DECILE, not sampled at an instant -- sampling lands between
        // classes often enough to print 0 in the middle of a fully busy run.
        val busy = (0 until 10).map { i ->
            val lo = start + window * i / 10
            val hi = start + window * (i + 1) / 10
            val occupied = spans.classes.sumOf { c ->
                (minOf(c.endMillis, hi) - maxOf(c.startMillis, lo)).coerceAtLeast(0)
            }
            occupied.toDouble() / (hi - lo)
        }
        // The class count is on this line deliberately. The newest burst of activity is taken to be the
        // run being reported on, which is right when this task is invoked after a suite and wrong if
        // something small ran in between -- one class from an IDE, say. Naming the count makes that
        // visible at a glance ("1 class") instead of hiding behind a plausible-looking total.
        val summary = String.format(
            "%d classes: %s of class time in %s of wall clock = **%.1f** effective average concurrency",
            spans.classes.size, fmt(work), fmt(window), work.toDouble() / window
        )

        logger.lifecycle("")
        if (markdown) {
            logger.lifecycle("## Fork occupancy through the test execution")
            logger.lifecycle("")
            logger.lifecycle("| % of run |" + (1..10).joinToString("") { " ${it * 10} |" })
            logger.lifecycle("| -- |" + " --: |".repeat(10))
            logger.lifecycle("| forks busy |" + busy.joinToString("") { String.format(" %.1f |", it) })
            logger.lifecycle("")
            logger.lifecycle(summary)
            if (ignored > 0) {
                logger.lifecycle("")
                logger.lifecycle("*$ignored class record(s) from $otherRuns earlier run(s) in the same directory ignored.*")
            }
        } else {
            logger.lifecycle("Fork occupancy through the test execution")
            if (ignored > 0) {
                logger.lifecycle("  ($ignored class record(s) from $otherRuns earlier run(s) ignored)")
            }
            logger.lifecycle("  % of run   " + (1..10).joinToString("") { String.format("%7d", it * 10) })
            logger.lifecycle("  forks busy " + busy.joinToString("") { String.format("%7.1f", it) })
            logger.lifecycle("")
            logger.lifecycle("  " + summary.replace("**", ""))
        }
    }

    private fun reportSimulation(
        runtimes: Map<String, Double>,
        committed: Map<String, Double>,
        committedFile: java.io.File?
    ) {
        val classes = runtimes.keys.toList()
        val onlyInRun = classes.filter { it !in committed }
        val onlyInCommitted = committed.keys.filter { it !in runtimes }

        val forks = forkCounts.get().sorted().distinct()
        val note = if (committedFile == null || committed.isEmpty()) {
            "No committed timings yet, so there is nothing to compare against."
        } else {
            "Committed file: ${committed.size} entries -- " +
                "${onlyInRun.size} class(es) it does not rank (they sort last), " +
                "${onlyInCommitted.size} it lists that did not run (ignored)."
        }

        logger.lifecycle("")
        if (markdown) {
            logger.lifecycle("## What a different ordering would give")
            logger.lifecycle("")
            logger.lifecycle(note)
            logger.lifecycle("")
            if (committed.isEmpty()) {
                logger.lifecycle("| forks | this run |")
                logger.lifecycle("| --: | --: |")
                forks.forEach { n ->
                    logger.lifecycle("| $n | ${simulate(arrange(classes, runtimes, n), runtimes, n).toLong()}s |")
                }
            } else {
                logger.lifecycle("| forks | committed | this run | gain | |")
                logger.lifecycle("| --: | --: | --: | --: | -- |")
                forks.forEach { n ->
                    val fromRun = simulate(arrange(classes, runtimes, n), runtimes, n)
                    val fromCommitted = simulate(arrange(classes, committed, n), runtimes, n)
                    val marker = if (actualForks.orNull == n) "this machine" else ""
                    logger.lifecycle(
                        "| $n | ${fromCommitted.toLong()}s | ${fromRun.toLong()}s | " +
                            "${(fromCommitted - fromRun).toLong()}s | $marker |"
                    )
                }
            }
            logger.lifecycle("")
            logger.lifecycle("`gain` is what re-ordering would save. Adopt this run's ordering with:")
            logger.lifecycle("")
            logger.lifecycle("```bash")
            logger.lifecycle("cp ${runTimings.get().asFile} ${committedFile ?: "<committed file>"}")
            logger.lifecycle("```")
            logger.lifecycle("")
            logger.lifecycle("then raise a PR -- the diff is a reordered CSV.")
        } else {
            logger.lifecycle("What a different ordering would give")
            logger.lifecycle("  $note")
            logger.lifecycle("")
            logger.lifecycle(
                if (committed.isEmpty()) {
                    String.format("  %6s  %12s", "forks", "this run")
                } else {
                    String.format("  %6s  %12s  %12s  %10s", "forks", "committed", "this run", "gain")
                }
            )
            forks.forEach { n ->
                val fromRun = simulate(arrange(classes, runtimes, n), runtimes, n)
                if (committed.isEmpty()) {
                    logger.lifecycle(String.format("  %6d  %11.0fs", n, fromRun))
                } else {
                    val fromCommitted = simulate(arrange(classes, committed, n), runtimes, n)
                    val marker = if (actualForks.orNull == n) "  <- this machine" else ""
                    logger.lifecycle(
                        String.format(
                            "  %6d  %11.0fs  %11.0fs  %9.0fs%s",
                            n, fromCommitted, fromRun, fromCommitted - fromRun, marker
                        )
                    )
                }
            }
            logger.lifecycle("")
            logger.lifecycle("'gain' is what re-ordering would save. Adopt this run's ordering with:")
            logger.lifecycle("  cp ${runTimings.get().asFile} ${committedFile ?: "<committed file>"}")
            logger.lifecycle("then raise a PR -- the diff is a reordered CSV.")
        }
        logger.lifecycle("")
    }

    /**
     * A duration as `m:ss`, or `s` below a minute.
     *
     * Colon-separated rather than `40m14s`: the report is read as a column of times to compare against
     * each other, and `41:47` versus `43:32` is a subtraction the eye can do where `41m47s` versus
     * `43m32s` is not.
     */
    private fun fmt(millis: Long): String {
        val seconds = millis / 1000
        return if (seconds < 60) "${seconds}s" else "${seconds / 60}:${"%02d".format(seconds % 60)}"
    }
}
