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
 * How much of the distance to a lower measurement is given up each time this file is adopted.
 *
 * A HALF, so a fall lands on the midpoint: `(committed + measured) / 2`. The unit here is an ADOPTION, not a
 * test run -- this file changes only when someone runs the report and commits the result, which is a handful
 * of times a month, not several times a day. A gentler decay would leave a class that genuinely got faster
 * mis-scheduled for months of wall-clock time. (Gradle's own test distribution re-measures continuously and
 * can afford to move in much smaller steps; this file cannot.)
 *
 * A class dropping from 30s to 2s then reads 16.0, 9.0, 5.5, 3.8, 2.9 -- settled within about five adoptions.
 */
const val DECAY = 0.5

/**
 * Fold a fresh measurement into the value the schedule uses: a rise is taken in full, a fall moves [DECAY] of
 * the way -- with DECAY at 0.5, to the midpoint between the committed value and this run's.
 *
 * ASYMMETRIC ON PURPOSE. Underestimating a class is expensive and overestimating it is nearly free. A class
 * scheduled too late runs when nothing is left to overlap it, so its whole duration lands on the critical
 * path; a class scheduled too early merely runs alongside others. Halving a rise as well would take four
 * adoptions -- months, at this file's cadence -- before a class that got slower was scheduled as slow, and
 * that is the direction where being wrong costs something.
 *
 * [previous] is null for a class never seen before, which then simply takes its measured time.
 *
 * This is not what fixed EDG-987 -- that was a measurement reading the wrong attribute, and no amount of
 * smoothing repairs a number that is wrong every time. It guards the different case of a class whose cost
 * genuinely varies between runs, where the schedule should plan for the bad case.
 *
 * WHAT MAY BE FED IN IS NOT DECIDED HERE. Only a class that PASSED is a sound measurement -- see rule 5 of
 * the "READING THE RECORDS" reference in `ForkAttributionListener`, enforced by `longestPerClass()`. This
 * function smooths whatever it is given and asks no questions about it.
 */
fun smooth(
    previous: Double?,
    measured: Double
): Double =
    when {
        previous == null -> measured
        measured >= previous -> measured
        else -> previous - (previous - measured) * DECAY
    }

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

/**
 * Write a `class,seconds,measured` CSV, slowest first.
 *
 * [timings] is the smoothed value the ordering is built from -- the first column, and the only one anything
 * reads back. [measured] is what the run actually recorded for that class, written alongside so a reader can
 * see why the two differ (a class whose smoothed value is far above its measurement is one on the way down
 * from a slower run). A class absent from [measured] simply has an empty second field.
 */
fun writeTimings(
    file: File,
    timings: Map<String, Double>,
    header: List<String>,
    measured: Map<String, Double> = emptyMap()
) {
    file.parentFile?.mkdirs()
    file.bufferedWriter().use { out ->
        // An empty header entry is a blank comment line -- written as a bare "#" so it carries no
        // trailing whitespace, which some editors and pre-commit hooks strip on sight.
        header.forEach { out.write(if (it.isEmpty()) "#\n" else "# $it\n") }
        out.write("class,seconds,measured\n")
        timings.entries
            .sortedWith(compareByDescending<Map.Entry<String, Double>> { it.value }.thenBy { it.key })
            .forEach { entry ->
                val raw = measured[entry.key]?.let { "%.1f".format(it) } ?: ""
                out.write("${entry.key},${"%.1f".format(entry.value)},$raw\n")
            }
    }
}

/**
 * The finish time of the busiest fork, given a dispatch order.
 *
 * Models what Gradle actually does: class *i* goes to fork *(i mod forks)*, assigned up front, with no
 * rebalancing and no work stealing. So the busiest fork sets the finish time. JVM recycling is not modelled
 * -- it costs roughly the same whatever the order, so it would shift every number by the same amount.
 */
fun simulate(
    ordered: List<String>,
    runtimes: Map<String, Double>,
    forks: Int
): Double {
    val busy = DoubleArray(forks)
    ordered.forEachIndexed { index, name -> busy[index % forks] += runtimes[name] ?: 0.0 }
    return busy.max()
}
