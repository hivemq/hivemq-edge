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
 * Read a `class,seconds` CSV. Comments (`#`) and the header are skipped, as are nested-class rows.
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

/** Write [timings] as a `class,seconds` CSV, slowest first. */
fun writeTimings(
    file: File,
    timings: Map<String, Double>,
    header: List<String>
) {
    file.parentFile?.mkdirs()
    file.bufferedWriter().use { out ->
        // An empty header entry is a blank comment line -- written as a bare "#" so it carries no
        // trailing whitespace, which some editors and pre-commit hooks strip on sight.
        header.forEach { out.write(if (it.isEmpty()) "#\n" else "# $it\n") }
        out.write("class,seconds\n")
        timings.entries
            .sortedWith(compareByDescending<Map.Entry<String, Double>> { it.value }.thenBy { it.key })
            .forEach { out.write("${it.key},${"%.1f".format(it.value)}\n") }
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
