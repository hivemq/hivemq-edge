package com.hivemq.testordering

import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

/** What one test class did in a run: how long it took, and whether anything failed in it. */
data class ClassResult(
    val seconds: Double,
    val failures: Int
)

/**
 * The run's test tally.
 *
 * FLAKY is a test that failed and then passed on a retry, so the same test appears more than once with at
 * least one failure and at least one pass. It is counted separately from [failed] because the build is green:
 * calling it a failure overstates, ignoring it hides a real problem.
 */
data class RunTotals(
    val classes: Int,
    val tests: Int,
    val passed: Int,
    val flaky: Int,
    val failed: Int,
    val skipped: Int,
    val seconds: Double
)

/** Tally the run's tests, distinguishing a genuine failure from one that passed on retry. */
fun readRunTotals(xmlDir: File): RunTotals {
    val files = xmlDir.listFiles { f -> f.isFile && f.name.startsWith("TEST-") && f.extension == "xml" }
        ?: return RunTotals(0, 0, 0, 0, 0, 0, 0.0)

    // Keyed by class+method: a retried test appears once per attempt.
    val attempts = mutableMapOf<Pair<String, String>, MutableList<Boolean>>()
    var skipped = 0
    var seconds = 0.0
    val classes = mutableSetOf<String>()

    val builder = DocumentBuilderFactory.newInstance().newDocumentBuilder()
    files.forEach { file ->
        val doc = runCatching { builder.parse(file) }.getOrNull() ?: return@forEach
        val cases = doc.getElementsByTagName("testcase")
        for (i in 0 until cases.length) {
            val case = cases.item(i)
            val attrs = case.attributes ?: continue
            val className = attrs.getNamedItem("classname")?.nodeValue ?: continue
            val name = attrs.getNamedItem("name")?.nodeValue ?: continue
            classes += className.substringBefore('$')
            seconds += attrs.getNamedItem("time")?.nodeValue?.toDoubleOrNull() ?: 0.0

            var isSkipped = false
            var isFailure = false
            val children = case.childNodes
            for (c in 0 until children.length) {
                when (children.item(c).nodeName) {
                    "skipped" -> isSkipped = true
                    "failure", "error" -> isFailure = true
                }
            }
            if (isSkipped) {
                skipped += 1
            } else {
                attempts.getOrPut(className to name) { mutableListOf() } += isFailure
            }
        }
    }

    var passed = 0
    var flaky = 0
    var failed = 0
    attempts.values.forEach { outcomes ->
        when {
            // Never failed on any attempt.
            outcomes.none { it } -> passed += 1
            // Failed at least once but passed in the end -- green, but worth knowing about.
            outcomes.any { !it } -> flaky += 1
            else -> failed += 1
        }
    }

    return RunTotals(classes.size, attempts.size, passed, flaky, failed, skipped, seconds)
}

/**
 * Per-class results from a run's JUnit XML.
 *
 * THE UNIT IS THE OUTER CLASS. Gradle dispatches an outer class to a test JVM and JUnit runs every @Nested
 * class inside it there, so a nested class is never scheduled independently. Nested entries are folded into
 * their enclosing class.
 *
 * WHICH DURATION. What the ordering needs is how long a class OCCUPIES a fork, because that is what has to be
 * packed. Neither attribute gives that on its own:
 *
 *  - `<testsuite time=>` is the right quantity -- it covers per-class setup and teardown, which for a class
 *    that starts a container is nearly all of its cost. But when a class was RETRIED it spans
 *    first-attempt-start to last-attempt-end, including the idle gap between attempts, and can overstate by an
 *    order of magnitude (one retried class read 462s against 25s of actual work).
 *  - Summing `<testcase time=>` is immune to that gap, but counts only time inside test METHODS. A class whose
 *    work happens in `@BeforeAll`/`@BeforeEach` measures as near zero: `OidcServiceKeycloakIT` spends 25s
 *    starting a Keycloak container and recorded 1.3s, so the ordering put the tenth-slowest class in the suite
 *    last, where nothing was left to overlap it (EDG-987).
 *
 * So: use the suite time, EXCEPT where a retry makes it untrustworthy, and there fall back to the sum.
 *
 * DETECTING A RETRY. Not by repeated `<testcase name=>` -- a parameterised test writes the same name under
 * several methods (`LdapDirectoryDescentIT` has `[1] OpenLDAP` six times and was never retried), and the XML
 * carries no method attribute to separate the two. The usable signal is that a retry only happens after a
 * FAILURE, so a class that recorded one or more failures may have been retried and its suite time may span an
 * idle gap. A class with no failures cannot have been retried, whatever its names look like.
 *
 * That is deliberately conservative in one direction: a class that failed outright, with no retry, also falls
 * back to the sum and is understated. That is the safe error -- it is the number this task used for every
 * class before, and a failing class's timing is disturbed anyway (the task already warns that a failing class
 * stops early and understates its real time).
 */
fun readRunResults(xmlDir: File): Map<String, ClassResult> {
    val results = mutableMapOf<String, ClassResult>()
    val files = xmlDir.listFiles { f -> f.isFile && f.name.startsWith("TEST-") && f.extension == "xml" }
        ?: return emptyMap()

    val builder = DocumentBuilderFactory.newInstance().newDocumentBuilder()
    files.forEach { file ->
        val doc = runCatching { builder.parse(file) }.getOrNull() ?: return@forEach
        val cases = doc.getElementsByTagName("testcase")

        // Per FILE, because the duration we want lives on the file's <testsuite> element.
        var caseSeconds = 0.0
        var failures = 0
        var outerClass: String? = null

        for (i in 0 until cases.length) {
            val case = cases.item(i)
            val attrs = case.attributes ?: continue
            val rawClass = attrs.getNamedItem("classname")?.nodeValue ?: continue
            // Fold nested classes into the enclosing one -- only the outer class is ever dispatched.
            if (outerClass == null) {
                outerClass = rawClass.substringBefore('$')
            }
            caseSeconds += attrs.getNamedItem("time")?.nodeValue?.toDoubleOrNull() ?: 0.0

            val children = case.childNodes
            for (c in 0 until children.length) {
                when (children.item(c).nodeName) {
                    "failure", "error" -> failures += 1
                }
            }
        }

        val outer = outerClass ?: return@forEach
        val suiteSeconds = doc.documentElement
            ?.takeIf { it.nodeName == "testsuite" }
            ?.getAttribute("time")
            ?.toDoubleOrNull()

        // Suite time unless something makes it untrustworthy: a failure (which may have been retried, and a
        // retry's suite time spans the idle gap between attempts), or a suite time BELOW the work it
        // contains, which no honest run produces.
        val seconds = if (failures > 0 || suiteSeconds == null || suiteSeconds < caseSeconds) {
            caseSeconds
        } else {
            suiteSeconds
        }

        val previous = results[outer]
        results[outer] =
            if (previous == null) {
                ClassResult(seconds, failures)
            } else {
                // Nested classes write one file each; their durations add up to the enclosing class's cost.
                ClassResult(previous.seconds + seconds, previous.failures + failures)
            }
    }
    return results
}
