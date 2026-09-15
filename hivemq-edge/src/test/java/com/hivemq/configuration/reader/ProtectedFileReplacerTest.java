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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.hivemq.configuration.reader.ProtectedFileReplacer.PreservedAttributes;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.concurrent.atomic.AtomicReference;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * The contract of {@link ProtectedFileReplacer} on its own: a file is replaced by content written beside
 * it and moved onto it, carrying the protections of the file it replaces, and nothing is left behind or
 * touched when any step fails. The permission and ACL matrices live in {@code ConfigWritePermissionsTest}
 * and {@code ConfigWriteAclPermissionsTest}; this pins the sequence.
 */
public class ProtectedFileReplacerTest {

    @TempDir
    public File temporaryFolder;

    private @NotNull Path target;

    @BeforeEach
    public void setUp() throws IOException {
        target = temporaryFolder.toPath().resolve("target.xml");
        Files.writeString(target, "before");
        assumeTrue(target.getFileSystem().supportedFileAttributeViews().contains("posix"), "POSIX modes are asserted");
    }

    private static @NotNull Path partialOf(final @NotNull Path target) {
        return target.resolveSibling(target.getFileName() + ".partial");
    }

    @Test
    public void replacesTheContentAndCarriesTheMode() throws IOException {
        Files.setPosixFilePermissions(target, PosixFilePermissions.fromString("rw-------"));

        ProtectedFileReplacer.replaceCarryingProtections(
                target,
                ProtectedFileReplacer.preservedAttributesOf(target),
                partial -> Files.writeString(partial, "after"));

        assertThat(Files.readString(target)).isEqualTo("after");
        assertThat(Files.getPosixFilePermissions(target)).isEqualTo(PosixFilePermissions.fromString("rw-------"));
        assertThat(partialOf(target)).doesNotExist();
    }

    @Test
    public void theHookRunsOnceTheContentIsCompleteAndBeforeTheMove() throws IOException {
        final AtomicReference<String> partialAtHook = new AtomicReference<>();
        final AtomicReference<String> targetAtHook = new AtomicReference<>();

        ProtectedFileReplacer.replaceCarryingProtections(
                target,
                ProtectedFileReplacer.preservedAttributesOf(target),
                partial -> Files.writeString(partial, "after"),
                () -> {
                    partialAtHook.set(Files.readString(partialOf(target)));
                    targetAtHook.set(Files.readString(target));
                });

        assertThat(partialAtHook)
                .as("the replacement is complete when the hook runs")
                .hasValue("after");
        assertThat(targetAtHook)
                .as("the target has not been touched when the hook runs")
                .hasValue("before");
        assertThat(Files.readString(target)).isEqualTo("after");
    }

    @Test
    public void aFailingHookLeavesTheTargetUntouchedAndNoWorkingFile() {
        assertThatThrownBy(() -> ProtectedFileReplacer.replaceCarryingProtections(
                        target,
                        ProtectedFileReplacer.preservedAttributesOf(target),
                        partial -> Files.writeString(partial, "after"),
                        () -> {
                            throw new IOException("no slot");
                        }))
                .isInstanceOf(IOException.class)
                .hasMessage("no slot");
        assertThat(target).hasContent("before");
        assertThat(partialOf(target)).doesNotExist();
    }

    @Test
    public void aFailingWriterLeavesTheTargetUntouchedAndNoWorkingFile() {
        assertThatThrownBy(() -> ProtectedFileReplacer.replaceCarryingProtections(
                        target, ProtectedFileReplacer.preservedAttributesOf(target), partial -> {
                            Files.writeString(partial, "half");
                            throw new IOException("disk full");
                        }))
                .isInstanceOf(IOException.class)
                .hasMessage("disk full");
        assertThat(target).hasContent("before");
        assertThat(partialOf(target)).doesNotExist();
    }

    @Test
    public void aMissingTargetIsCreated() throws IOException {
        final Path absent = temporaryFolder.toPath().resolve("absent.xml");
        final PreservedAttributes none = ProtectedFileReplacer.preservedAttributesOf(absent);
        assertThat(none).isEqualTo(PreservedAttributes.NONE);

        ProtectedFileReplacer.replaceCarryingProtections(absent, none, partial -> Files.writeString(partial, "new"));

        assertThat(absent).hasContent("new");
        assertThat(partialOf(absent)).doesNotExist();
    }

    @Test
    public void aCopyCarriesContentModeAndModificationTime() throws IOException {
        Files.setPosixFilePermissions(target, PosixFilePermissions.fromString("rw-r-----"));
        Files.setLastModifiedTime(target, FileTime.fromMillis(1_700_000_000_000L));
        final Path copy = temporaryFolder.toPath().resolve("target_1.xml");

        ProtectedFileReplacer.copyCarryingProtections(target, copy);

        assertThat(copy).hasContent("before");
        assertThat(Files.getPosixFilePermissions(copy)).isEqualTo(PosixFilePermissions.fromString("rw-r-----"));
        assertThat(Files.getLastModifiedTime(copy).toMillis()).isEqualTo(1_700_000_000_000L);
        assertThat(partialOf(copy)).doesNotExist();
    }

    @Test
    public void aCopyOntoTheSourceItselfIsRefused() throws IOException {
        final Path link = temporaryFolder.toPath().resolve("same.xml");
        Files.createSymbolicLink(link, target);

        assertThatThrownBy(() -> ProtectedFileReplacer.copyCarryingProtections(target, link))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("configuration file itself");
        assertThat(target).hasContent("before");
    }
}
