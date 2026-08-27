package com.hivemq.testordering

import java.io.File

/**
 * A class that can be dispatched to a test JVM: its name, and the file it was found in.
 *
 * The FILE is kept because the scan below has it in hand. Looking it up again later -- one lookup per
 * class -- costs 166x the single walk that found them all: measured on the integration suite, 12ms to
 * walk once against 1991ms to resolve 700 classes one at a time, and far worse on a CI agent whose
 * filesystem cache is cold.
 */
data class DispatchableClass(
    val name: String,
    /** The class-output directory this was found under -- the root a FileTree must be based on. */
    val root: File,
    /** Path of the class file relative to [root], which is how Gradle derives the class name. */
    val relativePath: String
)

/**
 * Every non-abstract outer class under [root]. NOT "every test class".
 *
 * This is what Gradle's own scanner does, and reproducing it exactly is the point: the list built here
 * REPLACES that scan, so anything it drops stops running.
 *
 * Deciding what is a test is JUnit's job, and it does it inside the test JVM after the class has been
 * dispatched. Nothing here can do it as well: a class carrying no test annotation may inherit its tests from
 * an abstract base or hold them all in @Nested inner classes, and a class that looks like a test may be
 * @Disabled or filtered out by tag. Every attempt to answer that question here is a chance to silently stop
 * running a real test, so the question is not asked. Everything is dispatched; JUnit filters. Dispatching a
 * class with no tests costs one slot in the forkEvery recycling count and nothing else.
 *
 * Three exclusions, none of them a judgement about test-ness:
 *
 *  - Abstract classes and interfaces. Not "these look like helpers" -- these cannot be instantiated, so
 *    there is no way to run anything in them. JUnit would reject them, and Gradle's scanner skips them for
 *    the same reason. Read from the ACC_ABSTRACT and ACC_INTERFACE bits of the class file.
 *  - Nested and anonymous classes (a `$` in the name). A nested class is never dispatched on its own:
 *    Gradle dispatches the enclosing class and JUnit runs the nested ones inside it, so dispatching it
 *    separately would run those tests twice.
 *  - `module-info`, which is not a class at all.
 */
fun findDispatchableTestClasses(root: File): List<DispatchableClass> =
    root
        .walkTopDown()
        .filter { it.isFile && it.extension == "class" && it.name != "module-info.class" }
        .filter {
            !it
                .relativeTo(root)
                .path
                .contains('$')
        }.filter { isConcrete(it.readBytes()) }
        .map {
            val relative = it.relativeTo(root).path
            DispatchableClass(
                relative.removeSuffix(".class").replace(File.separatorChar, '.'),
                root,
                relative
            )
        }.toList()
        .sortedBy { it.name }

private const val ACC_INTERFACE = 0x0200
private const val ACC_ABSTRACT = 0x0400

/**
 * Whether this class can be instantiated, i.e. is neither abstract nor an interface.
 *
 * A class file that cannot be parsed counts as concrete: the safe direction is to dispatch it and let JUnit
 * find nothing, rather than drop something that might have been a test.
 */
fun isConcrete(bytes: ByteArray): Boolean {
    val flags = accessFlags(bytes) ?: return true
    return (flags and ACC_ABSTRACT) == 0 && (flags and ACC_INTERFACE) == 0
}

/**
 * A class file's `access_flags`, or null if this does not parse as one.
 *
 * The flags sit immediately after the constant pool, whose entries are variable-length and self-describing,
 * so reaching them means stepping through every entry. Long and Double take two pool slots each -- a 1995
 * quirk that every class file reader has to special-case.
 */
fun accessFlags(bytes: ByteArray): Int? {
    if (bytes.size < 10) return null
    // 0xCAFEBABE
    if (bytes[0].toInt() != -54 || bytes[1].toInt() != -2 || bytes[2].toInt() != -70 || bytes[3].toInt() != -66) {
        return null
    }
    val count = readU2(bytes, 8)
    var offset = 10
    var index = 1
    while (index < count) {
        if (offset >= bytes.size) return null
        val tag = bytes[offset].toInt() and 0xFF
        offset += 1
        when (tag) {
            // Utf8: length-prefixed
            1 -> {
                offset += 2 + readU2(bytes, offset)
            }

            // Class, String, MethodType, Module, Package
            7, 8, 16, 19, 20 -> {
                offset += 2
            }

            // MethodHandle
            15 -> {
                offset += 3
            }

            // Integer, Float, Fieldref, Methodref, InterfaceMethodref, NameAndType, Dynamic, InvokeDynamic
            3, 4, 9, 10, 11, 12, 17, 18 -> {
                offset += 4
            }

            // Long, Double: two pool slots
            5, 6 -> {
                offset += 8
                index += 1
            }

            else -> {
                return null
            }
        }
        index += 1
    }
    return if (offset + 2 <= bytes.size) readU2(bytes, offset) else null
}

private fun readU2(
    bytes: ByteArray,
    offset: Int
): Int = ((bytes[offset].toInt() and 0xFF) shl 8) or (bytes[offset + 1].toInt() and 0xFF)
