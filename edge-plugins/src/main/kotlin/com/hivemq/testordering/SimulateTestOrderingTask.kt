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
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction

/**
 * Compare the committed test ordering against the one this run would produce, and say whether updating pays.
 *
 * Run the suite green, then run this. It reports, for a range of fork counts:
 *
 *   - what the run actually took (the busiest fork's real finish time, from the fork logs if available,
 *     otherwise omitted -- it is context, not part of the comparison)
 *   - what the SNAKE arrangement gives using the ordering implied by the COMMITTED file
 *   - what it gives using the ordering implied by THIS run
 *
 * The gap between the last two is the whole decision: if this run's ordering is meaningfully better, copy
 * the generated file over the committed one and raise a PR. If not, leave it alone.
 *
 * TWO THINGS THIS DELIBERATELY DOES NOT DO.
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
abstract class SimulateTestOrderingTask : DefaultTask() {
    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val resultsDir: DirectoryProperty

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

    init {
        group = "verification"
        description = "Compare the committed test-class ordering against this run's, and report what each would cost"
        outputs.upToDateWhen { false }
    }

    @TaskAction
    fun run() {
        val results = readRunResults(resultsDir.get().asFile)
        if (results.isEmpty()) {
            throw GradleException(
                "No JUnit XML found in ${resultsDir.get().asFile}. Run the test suite first -- " +
                    "this compares orderings using the durations that run measured."
            )
        }

        val failed = results.filterValues { it.failures > 0 }
        if (failed.isNotEmpty()) {
            logger.warn("")
            logger.warn("WARNING: ${failed.size} class(es) had failures in this run:")
            failed.keys.sorted().take(10).forEach { logger.warn("  $it") }
            logger.warn("A failing class stops early, so its measured time understates the real one.")
            logger.warn("Treat the numbers below as provisional until the suite is green.")
        }

        // R -- this run. The durations here are the ONLY durations used anywhere below.
        val runtimes = results.mapValues { it.value.seconds }
        val committedFile = committedTimings.orNull?.asFile
        val committed = committedFile?.let { readTimings(it) } ?: emptyMap()

        writeTimings(
            runTimings.get().asFile,
            runtimes,
            listOf(
                "Measured runtime per test class, slowest first. Seconds.",
                "Generated from this run. Used ONLY to order classes across the parallel forks;",
                "the numbers themselves are never summed or used as an estimate.",
                "Copy over the committed file to adopt this ordering."
            )
        )

        // Both simulations run over the SAME class set -- everything this run measured -- so the ordering is
        // the only difference between them.
        val classes = runtimes.keys.toList()
        val onlyInRun = classes.filter { it !in committed }
        val onlyInCommitted = committed.keys.filter { it !in runtimes }

        logger.lifecycle("")
        logger.lifecycle("Test class ordering -- ${classes.size} classes, ${"%.0f".format(runtimes.values.sum())}s of work")
        if (committedFile == null || committed.isEmpty()) {
            logger.lifecycle("No committed timings yet, so there is nothing to compare against.")
        } else {
            logger.lifecycle(
                "Committed file: ${committed.size} entries -- " +
                    "${onlyInRun.size} class(es) it does not rank (they sort last), " +
                    "${onlyInCommitted.size} it lists that did not run (ignored)"
            )
        }
        logger.lifecycle("")

        val forks = forkCounts.get().sorted().distinct()
        val header = if (committed.isEmpty()) {
            String.format("  %6s  %12s", "forks", "this run")
        } else {
            String.format("  %6s  %12s  %12s  %10s", "forks", "committed", "this run", "gain")
        }
        logger.lifecycle(header)

        forks.forEach { n ->
            val fromRun = simulate(arrange(classes, runtimes, n), runtimes, n)
            if (committed.isEmpty()) {
                logger.lifecycle(String.format("  %6d  %11.0fs", n, fromRun))
            } else {
                val fromCommitted = simulate(arrange(classes, committed, n), runtimes, n)
                val gain = fromCommitted - fromRun
                val marker = if (actualForks.orNull == n) "  <- this machine" else ""
                logger.lifecycle(
                    String.format("  %6d  %11.0fs  %11.0fs  %9.0fs%s", n, fromCommitted, fromRun, gain, marker)
                )
            }
        }

        logger.lifecycle("")
        logger.lifecycle("'gain' is what re-ordering would save. Adopt this run's ordering with:")
        logger.lifecycle("  cp ${runTimings.get().asFile} ${committedFile ?: "<committed file>"}")
        logger.lifecycle("then raise a PR -- the diff is a reordered CSV.")
        logger.lifecycle("")
    }
}
