/*
 * Copyright 2019-present HiveMQ GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.hivemq.configuration.reader;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.AclEntry;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * EDG-882 — the configuration write path with more than one thread in it.
 * <p>
 * Two claims are worth holding down rather than assuming. The first is that {@link AclComparison} is a pure
 * function: it decides whether a file full of credentials may be written, it is reached from whatever thread
 * a REST request landed on, and the moment anyone gives it a cache or a reusable buffer it stops being safe
 * to call that way. Nothing about its current shape needs protecting — which is exactly why the property
 * should be a test rather than a reading of the source.
 * <p>
 * The second is that two configuration writes in flight at once do not interfere. Within a node they cannot:
 * {@code writeConfigToXML} holds a lock for the whole render-and-replace, and the rolling backup is taken
 * inside it. But the replacement itself is a static helper that works through a file named beside its target,
 * so what has to be true is that <em>different</em> targets in one directory never touch each other's
 * working file — which is the case that is not serialised by anything, and the case a backup and a
 * configuration write in the same directory actually are.
 */
public class ConfigWriteConcurrencyTest {

    private static final int THREADS = 16;

    @TempDir
    private @NotNull Path directory;

    // ------------------------------------------------------------------ the comparison is a pure function

    /**
     * Every pair judged on every thread at once, against the answers the same pairs give on one thread.
     * <p>
     * The pairs are the assorted lists crossed with themselves, so the work is identical on every thread and
     * a shared mutable structure would have to survive being written by all of them. A start gate keeps the
     * threads from finishing before the last one has begun, which is what makes the overlap real rather than
     * incidental.
     */
    @Test
    public void theComparisonGivesTheSameAnswersUnderEveryThreadAtOnce() throws Exception {
        final List<List<AclEntry>> lists = AclComparisonFixture.assortedLists();
        final boolean[] sequential = judgeAll(lists);
        final CountDownLatch ready = new CountDownLatch(THREADS);
        final CountDownLatch go = new CountDownLatch(1);

        try (final ExecutorService threads = Executors.newFixedThreadPool(THREADS)) {
            final List<Future<boolean[]>> answers = new ArrayList<>(THREADS);
            for (int thread = 0; thread < THREADS; thread++) {
                answers.add(threads.submit((Callable<boolean[]>) () -> {
                    ready.countDown();
                    if (!go.await(30, TimeUnit.SECONDS)) {
                        throw new IllegalStateException("the start gate never opened");
                    }
                    boolean[] answer = null;
                    for (int repeat = 0; repeat < 200; repeat++) {
                        answer = judgeAll(lists);
                    }
                    return answer;
                }));
            }
            assertTrue(ready.await(30, TimeUnit.SECONDS), "not every thread started");
            go.countDown();
            for (final Future<boolean[]> answer : answers) {
                assertArrayEquals(
                        sequential,
                        answer.get(60, TimeUnit.SECONDS),
                        "the comparison answered differently on a thread of its own");
            }
        }
    }

    private static boolean[] judgeAll(final @NotNull List<List<AclEntry>> lists) {
        final boolean[] answers = new boolean[lists.size() * lists.size()];
        int position = 0;
        for (final List<AclEntry> candidate : lists) {
            for (final List<AclEntry> reference : lists) {
                answers[position++] = AclComparison.grantsNoMoreThan(candidate, reference);
            }
        }
        return answers;
    }

    // ------------------------------------------------------- concurrent replacements do not cross-talk

    /**
     * Sixteen files in one directory, each replaced from its own thread at the same moment. Each has to end
     * up holding its own content and nobody else's, and the directory has to be left with nothing but the
     * files themselves — a working file surviving a replacement is a copy of a configuration sitting in a
     * directory under a name nothing will ever clean up.
     */
    @Test
    public void concurrentReplacementsOfDifferentFilesDoNotInterfere() throws Exception {
        final List<Path> targets = new ArrayList<>(THREADS);
        for (int index = 0; index < THREADS; index++) {
            final Path target = directory.resolve("config-" + index + ".xml");
            Files.writeString(target, "<before-" + index + "/>", StandardCharsets.UTF_8);
            targets.add(target);
        }
        final CountDownLatch ready = new CountDownLatch(THREADS);
        final CountDownLatch go = new CountDownLatch(1);

        try (final ExecutorService threads = Executors.newVirtualThreadPerTaskExecutor()) {
            final List<Future<?>> replacements = new ArrayList<>(THREADS);
            for (int index = 0; index < THREADS; index++) {
                final Path target = targets.get(index);
                final String content = "<after-" + index + "/>";
                replacements.add(threads.submit(() -> {
                    ready.countDown();
                    if (!go.await(30, TimeUnit.SECONDS)) {
                        throw new IllegalStateException("the start gate never opened");
                    }
                    ConfigFileReaderWriter.replaceCarryingProtections(
                            target,
                            ConfigFileReaderWriter.preservedAttributesOf(target),
                            partial -> Files.writeString(partial, content, StandardCharsets.UTF_8));
                    return null;
                }));
            }
            assertTrue(ready.await(30, TimeUnit.SECONDS), "not every thread started");
            go.countDown();
            for (final Future<?> replacement : replacements) {
                replacement.get(60, TimeUnit.SECONDS);
            }
        }

        for (int index = 0; index < THREADS; index++) {
            assertEquals(
                    "<after-" + index + "/>",
                    Files.readString(targets.get(index), StandardCharsets.UTF_8),
                    "a replacement landed in the wrong file, or was overwritten by another one");
        }
        assertNoWorkingFilesLeftBehind();
    }

    /**
     * The same directory, the same instant, but every thread replacing the file with the content it already
     * has — the shape a configuration write and its rolling backup make when both are in flight. Whatever
     * interleaving happens, no file may be left truncated or half written, because the content is complete
     * on disk before anything is moved onto anything.
     */
    @Test
    public void aReplacementNeverLeavesAFileHalfWritten() throws Exception {
        final Path target = directory.resolve("config.xml");
        final String content = "<configuration>" + "x".repeat(64 * 1024) + "</configuration>";
        Files.writeString(target, content, StandardCharsets.UTF_8);

        try (final ExecutorService threads = Executors.newVirtualThreadPerTaskExecutor()) {
            final List<Future<?>> work = new ArrayList<>();
            for (int index = 0; index < THREADS; index++) {
                // Every thread reads the file while one of them replaces it. Reading a file that is being
                // replaced by a move sees either the old content or the new one, never a prefix of either.
                work.add(threads.submit(() -> {
                    for (int repeat = 0; repeat < 20; repeat++) {
                        assertEquals(content, Files.readString(target, StandardCharsets.UTF_8));
                    }
                    return null;
                }));
            }
            work.add(threads.submit(() -> {
                for (int repeat = 0; repeat < 20; repeat++) {
                    ConfigFileReaderWriter.replaceCarryingProtections(
                            target,
                            ConfigFileReaderWriter.preservedAttributesOf(target),
                            partial -> Files.writeString(partial, content, StandardCharsets.UTF_8));
                }
                return null;
            }));
            for (final Future<?> item : work) {
                item.get(120, TimeUnit.SECONDS);
            }
        }

        assertEquals(content, Files.readString(target, StandardCharsets.UTF_8));
        assertNoWorkingFilesLeftBehind();
    }

    private void assertNoWorkingFilesLeftBehind() throws IOException {
        try (final Stream<Path> entries = Files.list(directory)) {
            final List<String> leftBehind = entries.map(
                            path -> path.getFileName().toString())
                    .filter(name -> name.endsWith(".partial"))
                    .toList();
            assertTrue(leftBehind.isEmpty(), "working files left in the configuration directory: " + leftBehind);
        }
    }
}
