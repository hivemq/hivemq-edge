package com.hivemq.testordering

import java.io.File

/**
 * Reorder so that every second group of [forks] runs shortest-to-longest instead of longest-to-shortest.
 *
 * With groups [a b c] [d e f] and 3 forks, fork 0 gets a and d -- the two largest. Reversing the second
 * group gives fork 0 a and f, fork 2 c and d, so the per-fork totals converge instead of diverging.
 */
fun snake(
    names: List<String>,
    forks: Int
): List<String> =
    names.chunked(forks).mapIndexed { index, group -> if (index % 2 == 1) group.reversed() else group }.flatten()

/**
 * The weight a KNOWN test gets when its measurement rounds to zero.
 *
 * Any positive value works; it only has to beat the 0.0 of a class nobody has measured. A tenth of a second
 * is also roughly honest -- a test that runs at all costs about this much.
 */
const val KNOWN_TEST_FLOOR = 0.1

/**
 * Sort [classes] slowest-first according to [timings], then snake them over [forks].
 *
 * A class ABSENT from [timings] is worth 0 and sorts LAST. A class PRESENT but measured at 0.0 is worth
 * [KNOWN_TEST_FLOOR] and sorts ahead of it. That distinction is the whole point, and it is not cosmetic.
 *
 * WHY. What gets dispatched is every concrete outer class in the test output directory -- 711 of them here,
 * of which only 337 contain a runnable test. Deciding test-ness up front is deliberately not attempted (see
 * `findDispatchableTestClasses`): a class may inherit its tests from an abstract base or hold them in
 * @Nested members, so guessing risks silently never running a real test. The 374 non-tests are dispatched,
 * JUnit finds nothing in them, and each still consumes one slot of the `forkEvery` recycling count.
 *
 * Those 374 are absent from the timings file, so they weigh 0. Without a floor, the handful of REAL tests
 * whose measurement rounds to 0.0 weigh 0 too, and get shuffled in among them by name -- landing at the very
 * end of a run, behind hundreds of empty classes. Measured: a slot then hits its 24-class limit while a
 * genuine test is still queued, retires its process, and starts a FRESH JVM to run one 0.1-second test.
 * Two such JVMs in one run, each costing more to start than the test it ran.
 *
 * NOTHING IS EXCLUDED. An unmeasured class still sorts last and still runs -- which is exactly how a NEWLY
 * ADDED test is picked up, since it has no entry either. The floor changes the ORDER of known tests, never
 * the membership of the run.
 *
 * Sorting by name within an equal weight keeps the order stable run to run.
 */
fun arrange(
    classes: List<String>,
    timings: Map<String, Double>,
    forks: Int
): List<String> =
    snake(
        classes.sortedWith(
            compareByDescending<String> { timings[it]?.coerceAtLeast(KNOWN_TEST_FLOOR) ?: 0.0 }.thenBy { it }
        ),
        forks
    )

/**
 * Read a `class,seconds[,measured]` CSV, returning the FIRST numeric column -- the smoothed value the
 * ordering is built from. Comments (`#`) and the header are skipped, as are nested-class rows.
 *
 * Any further columns are history, carried for a reader to inspect, and are deliberately not consulted here:
 * one number decides the order. Older files have only the one column and still read correctly.
 *
 * Nested classes may appear in a hand-edited file. They are ignored: a nested class is never dispatched on
 * its own, Gradle dispatches the outer class and JUnit runs the nested ones inside it.
 */
fun readTimings(file: File): Map<String, Double> {
    if (!file.isFile) return emptyMap()
    val timings = mutableMapOf<String, Double>()
    file.forEachLine { rawLine ->
        val line = rawLine.trim()
        if (line.isEmpty() || line.startsWith("#")) return@forEachLine
        val fields = line.split(",")
        if (fields.size < 2 || fields[0].contains('$')) return@forEachLine
        fields[1].trim().toDoubleOrNull()?.let { timings[fields[0].trim()] = it }
    }
    return timings
}

// WRITING THIS FILE IS NOT THE BUILD'S JOB, and neither is simulating an ordering or smoothing a
// measurement into one. Those belong to whatever reads a finished run, and that is the jenkins-report tool,
// which does it for a CI console log and a local `build/fork-logs/` with the same code. The build only ever
// READS this file, to decide an order it is about to act on.
//
// It used to do both, and the duplication was not free: two implementations of the same rules disagreed in
// the same report -- 337 classes on one line and 333 on the next, neither number wrong.
