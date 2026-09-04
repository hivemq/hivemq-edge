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
 * Sort [classes] slowest-first according to [timings], then snake them over [forks].
 *
 * A class with no recorded time is worth 0 and therefore sorts LAST. That is the whole rule -- there is no
 * separate bucket for them. A helper is untimed, sorts to the end, and JUnit finds nothing in it; a NEW test
 * is also untimed and still runs. Sorting by name within an equal time keeps the order stable run to run.
 */
fun arrange(
    classes: List<String>,
    timings: Map<String, Double>,
    forks: Int
): List<String> = snake(classes.sortedWith(compareByDescending<String> { timings[it] ?: 0.0 }.thenBy { it }), forks)

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
