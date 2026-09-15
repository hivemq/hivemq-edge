package com.hivemq.testordering

import org.gradle.api.logging.Logger
import java.io.File
import java.io.Writer

/**
 * The file a test task writes its `UME-` records to, so a local run needs no capture step (EDG-992).
 *
 * WHY A FILE AT ALL, when EDG-990 established that the console is the record. Because two different things
 * emit records, on two different channels, and only one of them reaches anything Gradle keeps:
 *
 *   - `ForkAttributionListener` runs INSIDE each test process and writes to its standard output. Gradle
 *     captures that, and `onOutput` hands it to us.
 *   - The ordering step runs in the BUILD, before any test process exists, and writes `UME-PREDICTED` through
 *     the Gradle logger. That never passes `onOutput`, because it is not test output.
 *
 * So Gradle's own `build/test-results/test/binary/output-events.bin` holds the first kind and none of the
 * second -- measured: 0 of 678 predictions. The only local artefact holding both is the Gradle daemon log,
 * which accumulates every build that daemon served and cannot be matched to a run from outside Gradle.
 *
 * Without this file, reporting on a local run means remembering `./gradlew test | tee somewhere` BEFORE a run
 * that takes twelve minutes. With it, the file is simply there afterwards.
 *
 * THE PREDICTIONS ONLY EVER EXIST LOCALLY, which is what makes this the case that matters rather than a
 * fallback: the ordering step is switched off when `CI_RUN` is set, so no Jenkins log has ever carried one.
 * The comparison between the ordering that ran and the ordering the run argues for is a local-only feature.
 *
 * ONE FILE PER TEST TASK, at `build/test-records/<task>.log`, holding EXACTLY ONE RUN. That last part is the
 * whole difference from the per-process files this replaces: those accumulated across runs, so reading them
 * meant separating one run out of a pile, and getting that subtly wrong folded one run's tests into another --
 * a 4795-test suite read 9590. A file truncated at the start of each run cannot have that bug.
 *
 * CI is untouched. The console remains the only thing that leaves a remote executor, and a Jenkins log is read
 * exactly as before.
 */
internal class RecordFile(
    private val file: File,
    private val logger: Logger
) {

    /**
     * The open handle, created on the first write rather than up front.
     *
     * LAZY, because a test task that is UP-TO-DATE or restored from the cache never runs and never writes a
     * record. Opening eagerly would leave an empty file behind and make "the file exists" a lie about whether
     * anything ran.
     */
    private var writer: Writer? = null

    /**
     * Start this run's file, discarding whatever the last run left.
     *
     * CALLED UNCONDITIONALLY at the start of the task, and NOT from the ordering step. That step returns early
     * on CI, when there is no timings file, and whenever it decides to leave the order alone -- so if it owned
     * the truncation, a stale run would still be sitting in the file, to be read as part of this one. That is
     * exactly the defect the per-process files had.
     */
    fun begin() {
        failSoft {
            file.parentFile?.mkdirs()
            file.writeText("")
        }
    }

    /**
     * Append one record. Safe to call from several test JVMs' output threads at once.
     *
     * SYNCHRONIZED ON THIS, because `onOutput` is called concurrently -- five threads locally, up to forty on
     * CI. Two interleaved writes would splice half-lines together and produce records that never existed,
     * which is the same failure the per-descriptor line buffering upstream exists to prevent.
     */
    @Synchronized
    fun append(line: String) {
        failSoft {
            val out = writer ?: file.bufferedWriter().also { writer = it }
            out.write(line)
            out.write("\n")
            // FLUSHED PER RECORD. A build that is killed part-way -- which is how a hanging suite usually
            // ends -- should still leave the records of everything that finished, because that is precisely
            // the run someone wants to look at.
            out.flush()
        }
    }

    /** Close the handle at the end of the task. */
    @Synchronized
    fun end() {
        failSoft { writer?.close() }
        writer = null
    }

    /**
     * Run a file operation, reporting a failure rather than propagating it.
     *
     * FAIL-SOFT THROUGHOUT. These records are a diagnostic; a build must never fail because a diagnostic file
     * could not be written. Losing the file costs the local report and nothing else -- on CI the console copy
     * is what matters and is unaffected.
     *
     * Reported at `info` rather than swallowed: silence about a file that should exist is how someone spends
     * an afternoon wondering why their report is empty.
     */
    private inline fun failSoft(block: () -> Unit) {
        runCatching(block).onFailure {
            logger.info("Test records: could not write ${file.path} (${it.message})")
        }
    }
}
