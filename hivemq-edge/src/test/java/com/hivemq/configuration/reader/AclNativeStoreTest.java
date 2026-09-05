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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.AclEntry;
import java.nio.file.attribute.AclEntryPermission;
import java.nio.file.attribute.AclEntryType;
import java.nio.file.attribute.AclFileAttributeView;
import java.util.EnumSet;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * EDG-882 / EDG-947 — the ACL write path against a file store that actually has access-control lists.
 * <p>
 * <b>This class does not run on the machines that develop Edge, and it does not run in CI.</b>
 * {@code AclFileAttributeView} is provided by the Windows file system provider and by the Solaris one for
 * ZFS; on Linux and macOS {@code Files.getFileAttributeView(path, AclFileAttributeView.class)} returns
 * {@code null}, and every workflow in this repository runs on {@code ubuntu-latest}. So it skips, and it is
 * written to be found and to pass or fail honestly on the day a Windows job exists — which is EDG-947, and
 * is the difference between this path being tested and being modelled.
 * <p>
 * Everything else about the comparison is decided without a store at all, in {@link AclComparisonTest} and
 * {@link AclComparisonOracleTest}. What is left over, and only reachable here, is what a real store
 * <em>returns</em>: whether it hands back the list it was given, whether it maps a permission mask into a
 * different one, whether inheritance from the containing directory survives a {@code setAcl}, and therefore
 * whether the narrowing the write path asks for is achievable at all. Review v04's finding 2.1 was a rule
 * that would have refused every configuration write on exactly that behaviour, and nothing off Windows could
 * have reproduced it.
 * <p>
 * The assertions are consequently about what the store <em>decided</em>, never about the shape it returned.
 * A store is free to reorder, split, merge or annotate; it is not free to change who can read a file full of
 * credentials.
 */
public class AclNativeStoreTest {

    @TempDir
    private @NotNull Path directory;

    private @NotNull Path target;

    @BeforeEach
    public void requireAnAclStore() throws IOException {
        target = directory.resolve("config.xml");
        Files.writeString(target, "<hivemq/>", StandardCharsets.UTF_8);
        assumeTrue(
                Files.getFileAttributeView(target, AclFileAttributeView.class) != null,
                "this file store has no access-control lists, so there is no ACL write path to exercise");
    }

    private static @NotNull List<AclEntry> aclOf(final @NotNull Path path) throws IOException {
        final AclFileAttributeView view = Files.getFileAttributeView(path, AclFileAttributeView.class);
        assertNotNull(view, "the store lost its ACL view between the assumption and here");
        return List.copyOf(view.getAcl());
    }

    /**
     * The round trip the whole comparison exists to tolerate: whatever the store does to a list on the way
     * in and out, it must not change what the list grants. A failure here is the store telling us that
     * {@code setAcl} does not mean what the caller thinks, and it is the only way to find that out.
     */
    @Test
    public void aListSurvivesBeingWrittenAndReadBack() throws IOException {
        final AclFileAttributeView view = Files.getFileAttributeView(target, AclFileAttributeView.class);
        assertNotNull(view);
        final List<AclEntry> ownerOnly = List.of(AclEntry.newBuilder()
                .setType(AclEntryType.ALLOW)
                .setPrincipal(view.getOwner())
                .setPermissions(EnumSet.allOf(AclEntryPermission.class))
                .build());

        view.setAcl(ownerOnly);
        final List<AclEntry> readBack = aclOf(target);

        assertTrue(
                AclComparison.grantsNoMoreThan(readBack, ownerOnly),
                () -> "the store returned a list granting more than the one it was given: " + readBack);
    }

    /**
     * The property {@code createPartialFile} promises: the file the configuration is about to be rendered
     * into is never readable by anyone the file it replaces is not, from the moment it exists.
     * <p>
     * On this store that is done by writing an owner-only list rather than by a creation mode, and it is
     * proved before the caller may write a byte — so if the store keeps an inherited entry the replacement
     * is refused outright. Either outcome is correct and both are asserted: what may not happen is a partial
     * file that exists, is wider than the target, and is handed back to be filled with credentials.
     */
    @Test
    public void theReplacementIsNeverWiderThanTheFileItReplaces() throws IOException {
        final Path partial = directory.resolve("config.xml.partial");
        final List<AclEntry> targetAcl = aclOf(target);

        try {
            ConfigFileReaderWriter.createPartialFile(partial, ConfigFileReaderWriter.preservedAttributesOf(target));
        } catch (final IOException refused) {
            assertTrue(
                    Files.notExists(partial) || aclOf(partial).isEmpty(),
                    "the narrowing was refused but a partial file was left behind anyway");
            return;
        }
        final List<AclEntry> partialAcl = aclOf(partial);
        assertTrue(
                AclComparison.grantsNoMoreThan(partialAcl, targetAcl),
                () -> "the replacement is readable by principals the configuration file is not: " + partialAcl
                        + " where the configuration has " + targetAcl);
    }

    /**
     * And the whole replacement, end to end: the file that lands carries what the file it replaced granted,
     * in both directions. Narrower would lock out an operator who could read the configuration before;
     * wider is the disclosure everything here is for.
     */
    @Test
    public void aReplacedFileGrantsWhatTheFileItReplacedGranted() throws IOException {
        final List<AclEntry> before = aclOf(target);

        ConfigFileReaderWriter.replaceCarryingProtections(
                target,
                ConfigFileReaderWriter.preservedAttributesOf(target),
                partial -> Files.writeString(partial, "<hivemq><replaced/></hivemq>", StandardCharsets.UTF_8));

        final List<AclEntry> after = aclOf(target);
        assertEquals("<hivemq><replaced/></hivemq>", Files.readString(target, StandardCharsets.UTF_8));
        assertTrue(
                AclComparison.grantsNoMoreThan(after, before),
                () -> "the replacement grants more than the file it replaced: " + after + " where it had " + before);
        assertTrue(
                AclComparison.grantsNoMoreThan(before, after),
                () -> "the replacement grants less than the file it replaced: " + after + " where it had " + before);
    }
}
