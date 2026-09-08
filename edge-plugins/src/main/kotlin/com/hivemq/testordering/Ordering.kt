package com.hivemq.testordering

import java.io.File

/**
 * Reorder so that every second group of [forks] runs shortest-to-longest instead of longest-to-shortest.
 *
 * With groups [a b c] [d e f] and 3 forks, fork 0 gets a and d -- the two largest. Reversing the second
 * group gives fork 0 a and f, fork 2 c and d, so the per-fork totals converge instead of diverging.
 *
 * SUPERSEDED by [balance], which packs the forks directly instead of approximating. Kept because it is the
 * baseline every measurement of the new scheme is quoted against, and because it needs no timings at all --
 * it only reverses alternate groups of an already-sorted list.
 */
fun snake(
    names: List<String>,
    forks: Int
): List<String> =
    names.chunked(forks).mapIndexed { index, group -> if (index % 2 == 1) group.reversed() else group }.flatten()

/**
 * Pack [names] into [forks] lanes by longest-processing-time, then interleave the lanes back into a dispatch
 * list that round-robin dispatch reconstructs exactly.
 *
 * THE IDEA. Gradle deals class *i* to fork *(i mod forks)* and never rebalances, so a dispatch list IS an
 * assignment -- the only question is which one. Rather than approximate a good assignment by permuting a
 * sorted list (see [snake]), build the assignment we actually want and then write it down in the order that
 * recreates it: lane 0's first class, lane 1's first, ... then everyone's second, and so on. Reading the
 * lanes off column by column is what makes `i mod forks` land each class back in the lane it was placed in.
 *
 * THE PACKING is classic LPT: walk the classes longest-first, and put each one on whichever lane is currently
 * cheapest. That is the standard greedy scheduler, guaranteed within 4/3 of optimal, and in practice far
 * closer on a distribution like this one.
 *
 * THE CAPACITY CAP is what makes the interleave sound. Lanes are packed to at most `ceil(n / forks)` classes,
 * so the lanes form a rectangle with at most one short column, and reading down the columns visits each lane
 * once per row. Without the cap, LPT would happily give one lane 40 tiny classes and another 3 big ones, the
 * rows would be ragged, and `i mod forks` would no longer rebuild the lanes at all -- the interleave would
 * silently scramble the very assignment it is meant to preserve.
 *
 * The cap does bind, and often: with 711 dispatched classes over 36 forks it blocks the cheapest lane on
 * about half the assignments. That is harmless here because it only starts binding once the remaining classes
 * weigh nothing -- the ~380 dispatched classes that hold no test at all (see [arrange]). It is a real
 * constraint, though, not a formality: it trades a little packing freedom for an assignment that survives
 * dispatch.
 *
 * MEASURED against [snake] on the committed timings, as makespan over the resulting lanes:
 *
 *     forks   snake      balance
 *         5   +0.9%       +0.0%
 *         8   +4.4%       +0.0%
 *        26  +31.0%       +0.8%
 *        36  +48.6%       +0.4%
 *
 * (percentages over the ideal of total-work / forks). The two are equivalent at low fork counts, which is
 * where the snake was tuned; the gap opens as forks grow, because reversing alternate groups cannot fix an
 * imbalance that spans more than two groups.
 *
 * Ties break on lane index so the result is deterministic run to run.
 */
fun balance(
    names: List<String>,
    weights: (String) -> Double,
    forks: Int
): List<String> {
    if (forks <= 1 || names.isEmpty()) return names
    val capacity = (names.size + forks - 1) / forks
    val lanes = List(forks) { mutableListOf<String>() }
    val loads = DoubleArray(forks)
    names.forEach { name ->
        val target = (0 until forks)
            .filter { lanes[it].size < capacity }
            .minByOrNull { loads[it] }
            ?: return@forEach
        lanes[target].add(name)
        loads[target] += weights(name)
    }
    // Column by column, so `i mod forks` puts each class back on the lane it was packed onto.
    return (0 until capacity).flatMap { row -> lanes.mapNotNull { it.getOrNull(row) } }
}

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
): List<String> {
    val weight = { name: String -> timings[name]?.coerceAtLeast(KNOWN_TEST_FLOOR) ?: 0.0 }
    val sorted = classes.sortedWith(compareByDescending(weight).thenBy { it })
    return balance(sorted, weight, forks)
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

// WRITING THIS FILE IS NOT THE BUILD'S JOB, and neither is simulating an ordering or smoothing a
// measurement into one. Those belong to whatever reads a finished run, and that is the jenkins-report tool,
// which does it for a Jenkins console log and a local run with the same code. The build only ever
// READS this file, to decide an order it is about to act on.
//
// It used to do both, and the duplication was not free: two implementations of the same rules disagreed in
// the same report -- 337 classes on one line and 333 on the next, neither number wrong.
