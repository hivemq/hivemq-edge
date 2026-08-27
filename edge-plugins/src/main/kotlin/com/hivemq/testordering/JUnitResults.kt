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
 * Durations are summed from the individual `<testcase time=>` values, never taken from the `<testsuite time=>`
 * attribute. That attribute spans first-attempt-start to last-attempt-end, so for a class that was retried it
 * includes the idle gap between attempts and can overstate the work by an order of magnitude.
 */
fun readRunResults(xmlDir: File): Map<String, ClassResult> {
    val results = mutableMapOf<String, ClassResult>()
    val files = xmlDir.listFiles { f -> f.isFile && f.name.startsWith("TEST-") && f.extension == "xml" }
        ?: return emptyMap()

    val builder = DocumentBuilderFactory.newInstance().newDocumentBuilder()
    files.forEach { file ->
        val doc = runCatching { builder.parse(file) }.getOrNull() ?: return@forEach
        val cases = doc.getElementsByTagName("testcase")
        for (i in 0 until cases.length) {
            val case = cases.item(i)
            val attrs = case.attributes ?: continue
            val rawClass = attrs.getNamedItem("classname")?.nodeValue ?: continue
            // Fold nested classes into the enclosing one -- only the outer class is ever dispatched.
            val outer = rawClass.substringBefore('$')
            val seconds = attrs.getNamedItem("time")?.nodeValue?.toDoubleOrNull() ?: 0.0

            var failures = 0
            val children = case.childNodes
            for (c in 0 until children.length) {
                when (children.item(c).nodeName) {
                    "failure", "error" -> failures += 1
                }
            }

            val previous = results[outer]
            results[outer] =
                if (previous == null) {
                    ClassResult(seconds, failures)
                } else {
                    ClassResult(previous.seconds + seconds, previous.failures + failures)
                }
        }
    }
    return results
}
