package com.hivemq.testordering

import org.gradle.api.tasks.testing.Test
import org.gradle.api.tasks.testing.TestDescriptor
import org.gradle.api.tasks.testing.TestOutputEvent
import org.gradle.api.tasks.testing.TestOutputListener
import java.util.concurrent.ConcurrentHashMap

/**
 * Keep the test JVMs' `UME-` records, and discard everything else they print (EDG-990, EDG-992).
 *
 * ONE LOG IS THE WHOLE ACCOUNT. Everything a reader needs is in the records themselves -- each names its own
 * absolute time and its own parent, so a class knows its process and a test knows its class -- which means one
 * log is a complete account of a run and nothing has to be reconstructed from how the lines were arranged.
 * That holds for a local run and a CI run alike, and it is what lets one reader serve both.
 *
 * The records go to TWO destinations, and neither is a copy of the other: the console, which is all that comes
 * back from a remote executor, and the task's record file, which is what a local run leaves behind without
 * anyone having to capture anything. See `RecordFile` for why the file is not redundant.
 *
 * The test JVMs' own output is NOT echoed (`showStandardStreams = false`), because it was 96% of a 78 MB build
 * log and none of it was what anyone measured. The `UME-` records are what everything measures, and they come
 * to roughly 1500 lines instead of half a million.
 *
 * On CI this is the only way they come back at all: the tests run on up to 40 remote executors whose disks are
 * discarded when they are released, so per-JVM files never reach Jenkins.
 *
 * `onOutput` is Gradle's supported hook for exactly this -- the same mechanism `showStandardStreams` uses
 * internally -- so it sees output from a remote executor just as it sees output from a local fork.
 *
 * BUFFERED UNTIL A NEWLINE. A process can flush a partial line, so a chunk is not a line: matching on the
 * chunk would silently drop any record that happened to be split. Silent loss is the failure this whole
 * exercise exists to end, so the accumulation is per-destination and only complete lines are considered.
 *
 * APPLIED TO EVERY TEST TASK by the convention plugin, so the unit suite and the integration suite cannot
 * drift apart. They did: the integration suite echoed its records while the unit suite did not, so a unit run
 * left nothing on the console and its analysis had to read the per-JVM files instead -- two inputs for one
 * question, which is the thing this design exists to avoid.
 */
internal fun umeRecordsToConsole(
    task: Test,
    records: RecordFile
) {
    // PER TEST DESCRIPTOR, not one shared buffer. Test JVMs run concurrently -- five locally, up to 40 on
    // CI -- and their chunks arrive interleaved. A single buffer would splice two JVMs' half-lines together
    // and produce records that never existed.
    val pending = ConcurrentHashMap<String, StringBuilder>()
    task.addTestOutputListener(
        object : TestOutputListener {
            override fun onOutput(
                descriptor: TestDescriptor,
                event: TestOutputEvent
            ) {
                val buffer = pending.computeIfAbsent(descriptor.toString()) { StringBuilder() }
                synchronized(buffer) {
                    buffer.append(event.message)
                    var cut = buffer.indexOf("\n")
                    while (cut >= 0) {
                        val line = buffer.substring(0, cut).trimEnd('\r')
                        buffer.delete(0, cut + 1)
                        if (line.startsWith("UME-")) {
                            // TO BOTH, and in this order. The console is what CI keeps; the file is what a
                            // local run keeps. Neither is a copy of the other, because each survives where the
                            // other does not -- see `RecordFile`.
                            task.logger.lifecycle(line)
                            records.append(line)
                        }
                        cut = buffer.indexOf("\n")
                    }
                }
            }
        }
    )
}
