package com.hivemq.testordering

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.register
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
        // Same name as the committed file: the directory says which is which, and adopting an ordering is
        // then a cp between two paths that differ only in their directory.
        val generated = project.layout.buildDirectory.file("test-class-timings.csv")

        project.tasks.withType<Test>().configureEach {
            doFirst {
                orderTestClasses(this as Test, committed.asFile)
            }
        }

        project.tasks.register<ReportTestConcurrencyTask>("reportTestConcurrency") {
            resultsDir.set(project.layout.buildDirectory.dir("test-results/test"))
            forkLogsDir.set(project.layout.buildDirectory.dir("fork-logs"))
            if (committed.asFile.isFile) {
                committedTimings.set(committed)
            }
            runTimings.set(generated)
            // A spread around the plausible range, plus whatever this machine would actually use.
            forkCounts.set(listOf(2, 5, 10, 20))
            val local = project.tasks.withType(Test::class.java).firstOrNull()?.maxParallelForks
            if (local != null) {
                actualForks.set(local)
            }
        }
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
    timingsFile: java.io.File
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

    val untimed = ordered.count { it !in timings }
    val totalSeconds = ordered.sumOf { timings[it] ?: 0.0 }
    task.logger.lifecycle(
        "Test class ordering: ${ordered.size} classes over $forks forks, " +
            "$untimed without a recorded time (sorted last), " +
            "${"%.0f".format(totalSeconds)}s of measured work, " +
            "ideal ${"%.0f".format(totalSeconds / forks)}s per fork"
    )
}
