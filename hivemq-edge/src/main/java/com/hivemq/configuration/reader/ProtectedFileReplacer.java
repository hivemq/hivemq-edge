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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.AclEntry;
import java.nio.file.attribute.AclEntryPermission;
import java.nio.file.attribute.AclEntryType;
import java.nio.file.attribute.AclFileAttributeView;
import java.nio.file.attribute.FileOwnerAttributeView;
import java.nio.file.attribute.GroupPrincipal;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFileAttributes;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.nio.file.attribute.UserPrincipal;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.VisibleForTesting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Replaces a file on disk with content written beside it, carrying the protections of the file it
 * replaces -- owner, group, mode and access-control list -- and never wider than those while it holds
 * the content. Pure file-system code: it knows nothing about configuration, and everything here was the
 * subject of EDG-882 review rounds v03 to v05.
 * <p>
 * Moved out of {@link ConfigFileReaderWriter} verbatim; that class is the only production caller, through
 * {@link #replaceCarryingProtections(Path, PreservedAttributes, ContentWriter, BeforeMove)} for the
 * configuration file and {@link #copyCarryingProtections(Path, Path)} for its rolling backup.
 */
final class ProtectedFileReplacer {

    private static final Logger log = LoggerFactory.getLogger(ProtectedFileReplacer.class);

    private ProtectedFileReplacer() {}

    /** The bytes of one replacement, written into the narrow file that has just been created for them. */
    @FunctionalInterface
    @VisibleForTesting
    interface ContentWriter {

        void writeTo(@NotNull Path partial) throws IOException;
    }

    /**
     * Replaces a file with content written beside it, under the protections it is given, and never wider
     * than those while it holds the content.
     * <p>
     * <b>Written beside the target and moved onto it, rather than opened and written in place.</b>
     * Rendering the document first closed the "schema validation fails half way" case; opening the real
     * file still truncates it, so a crash, a full disk or a killed process between open and close left a
     * config.xml cut off mid-element. There is a backup, but nothing restores it on its own and the node
     * does not start (EDG-882 review v02, R2-08). Same directory on purpose: ATOMIC_MOVE is only
     * guaranteed within a file store, and the temporary file has to be one the configuration watcher will
     * not try to parse.
     * <p>
     * <b>The protections are settled before a single byte is written, not after.</b> The content holds
     * bridge passwords, keystore and truststore passwords and adapter credentials. A file created by
     * {@code FileWriter} takes this process's umask, so on a 022 umask the first version of this change
     * created a world-readable 0644 file, populated it with every secret in the configuration, closed it,
     * and only then narrowed it to the mode of the file it was about to replace. Any local principal
     * reading the directory during that window got the lot -- on every REST write to any subsystem. That
     * window did not exist before this branch: writing in place reused config.xml's own inode and
     * therefore its own mode, so the secrets never touched a wider file. It is a disclosure this change
     * would have introduced, which is why it fails closed rather than best-effort (EDG-882 review v03,
     * R3-07).
     * <p>
     * <b>One sequence, both files.</b> The rolling backup goes through this too. It is a copy of the same
     * document, made by the same write, and it used to be produced by a plain file copy that carried the
     * mode and left the group to the process -- so a 0640 config.xml owned by one group was backed up
     * into a 0640 file readable by another (EDG-882 review v04). Two paths that must not differ are one
     * path.
     */
    @VisibleForTesting
    static void replaceCarryingProtections(
            final @NotNull Path target,
            final @NotNull PreservedAttributes preserved,
            final @NotNull ContentWriter content)
            throws IOException {
        replaceCarryingProtections(target, preserved, content, () -> {});
    }

    /** Work that must happen once the replacement is complete and proven, and just before it lands. */
    @FunctionalInterface
    @VisibleForTesting
    interface BeforeMove {

        void run() throws IOException;
    }

    static void replaceCarryingProtections(
            final @NotNull Path target,
            final @NotNull PreservedAttributes preserved,
            final @NotNull ContentWriter content,
            final @NotNull BeforeMove beforeMove)
            throws IOException {
        final Path partial = target.resolveSibling(target.getFileName() + ".partial");
        try {
            createPartialFile(partial, preserved);
            content.writeTo(partial);
            // Widened to the target's protections only once the content is on disk, so the file is never
            // wider than its eventual self while it is being filled. Never wider than the target either:
            // a protection this node cannot set -- the owner, or a group it is not in -- narrows the
            // replacement instead of widening it, and anything else aborts, because moving a file whose
            // protections could not be reproduced would open a deliberately restricted config.xml while
            // the original on disk is still valid and still correct.
            applyPreservedAttributes(partial, preserved);
            beforeMove.run();
            try {
                Files.move(partial, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (final AtomicMoveNotSupportedException atomicNotSupported) {
                // Some network and container file systems cannot do it. A non-atomic replace is still
                // better than truncating the original and writing into it, because the content being moved
                // is already complete on disk.
                log.debug("Atomic replace of {} is not supported here, falling back", target);
                Files.move(partial, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            // The outcome of the replace is what the caller must see. A failure to remove the working file
            // -- normally already moved away -- must neither turn a successful write into a reported
            // failure nor hide the real cause of a failed one (EDG-949).
            try {
                Files.deleteIfExists(partial);
            } catch (final IOException cleanup) {
                log.warn("Could not remove the working file {} left beside the configuration", partial, cleanup);
            }
        }
    }

    /**
     * Copies the configuration file to its rolling backup, under the protections of the file it copies.
     * <p>
     * <b>The source's protections, not the destination's.</b> The backup holds the source's content, so it
     * is the source that decides who may read it; the file being overwritten is a previous backup whose
     * protections are of no interest, and on the first rotation there is no destination at all -- which is
     * how the plain copy this replaces ended up handing the backup to the process's own group.
     *
     * @throws IOException when the backup cannot be given the protections of the file it copies, which
     *         aborts the write that asked for it: a backup of a configuration file is a second copy of
     *         every credential in it.
     */
    static void copyCarryingProtections(final @NotNull Path source, final @NotNull Path configured) throws IOException {
        final Path destination = Files.exists(configured) ? configured.toRealPath() : configured;
        if (Files.exists(destination) && Files.isSameFile(source, destination)) {
            // The rotation picked the configuration file itself, which would copy it onto itself through a
            // temporary file. The copy this replaces refused that outright and so does this.
            throw new IOException("The rolling backup of " + source + " resolves to the configuration file"
                    + " itself, so taking it would overwrite the configuration being backed up");
        }
        final PreservedAttributes preserved = preservedAttributesOf(source);
        replaceCarryingProtections(destination, preserved, partial -> {
            try (final InputStream in = Files.newInputStream(source);
                    final OutputStream out = Files.newOutputStream(
                            partial, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING)) {
                in.transferTo(out);
            }
            // Carried because the rotation picks the backup to overwrite by modification time.
            Files.setLastModifiedTime(partial, Files.getLastModifiedTime(source));
        });
    }

    /**
     * Owner read/write and nothing else: what the replacement is created as, before it is written.
     * <p>
     * Unmodifiable because it is shared static state handed to callers that have no reason to expect a
     * mutable set — an {@code EnumSet} on its own would let one of them widen it for the whole process.
     */
    static final @NotNull Set<PosixFilePermission> OWNER_ONLY =
            Collections.unmodifiableSet(EnumSet.of(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE));

    /**
     * Everything a mode can grant to the file's group: what comes off it when the replacement could not be
     * given the group of the file it replaces, so that the mode never grants to this node's own group what
     * it was meant to grant to that one.
     */
    private static final @NotNull Set<PosixFilePermission> GROUP_PERMISSIONS = Collections.unmodifiableSet(EnumSet.of(
            PosixFilePermission.GROUP_READ, PosixFilePermission.GROUP_WRITE, PosixFilePermission.GROUP_EXECUTE));

    /**
     * Everything about the file being replaced that decides who can read it.
     * <p>
     * The mode alone is not that. {@code 0640} names a group without naming <em>which</em> group, and a
     * freshly created file takes its group from the creating process or, on macOS and on any directory
     * with the setgid bit, from the directory — so reproducing only the mode can hand group-read of a
     * file full of credentials to a completely different set of principals than the file it replaced had
     * (EDG-882 review v03, R3-09). Owner, group and any ACL are therefore carried with it.
     *
     * @param permissions the POSIX mode, or {@code null} on a store with no POSIX view
     * @param owner       the owning principal, or {@code null} when the store exposes none
     * @param group       the owning group, or {@code null} on a store with no POSIX view
     * @param acl         the access-control list -- empty when the file has one that grants nobody
     *                    anything, which is a protection in its own right -- or {@code null} on a store
     *                    that has no ACL view
     */
    @VisibleForTesting
    record PreservedAttributes(
            @Nullable Set<PosixFilePermission> permissions,
            @Nullable UserPrincipal owner,
            @Nullable GroupPrincipal group,
            @Nullable List<AclEntry> acl) {

        /** Nothing to reproduce: the configuration file is being created for the first time. */
        static final @NotNull PreservedAttributes NONE = new PreservedAttributes(null, null, null, null);

        boolean nothingToReproduce() {
            return permissions == null && owner == null && group == null && acl == null;
        }

        /** The same protections, minus a claim to have reproduced the owner. */
        @NotNull
        PreservedAttributes withoutOwner() {
            return new PreservedAttributes(permissions, null, group, acl);
        }

        /**
         * The same protections, minus a claim to have reproduced the group <em>and</em> minus everything
         * the mode would have granted through it.
         * <p>
         * The two are one protection. {@code 0640} says "the group may read" without saying which group,
         * so dropping the group while keeping the mode is precisely R3-09: group-read of a file full of
         * credentials, granted to whichever group this node happens to belong to instead of the one the
         * operator chose. Dropping both leaves the replacement granting the group nothing, which is
         * narrower than the file it replaces rather than wider.
         * <p>
         * Only the group's own bits come off. The owner's are this node's either way, and what the mode
         * grants to others is granted to the same others on the file being replaced, so removing those
         * would take away access the operator deliberately gave.
         */
        @NotNull
        PreservedAttributes withoutGroupAccess() {
            if (permissions == null) {
                return new PreservedAttributes(null, owner, null, acl);
            }
            // Built empty and added to, rather than EnumSet.copyOf, which throws on an empty collection
            // that is not itself an EnumSet -- and the mode of a file nobody may read is exactly that.
            final EnumSet<PosixFilePermission> withoutTheGroups = EnumSet.noneOf(PosixFilePermission.class);
            withoutTheGroups.addAll(permissions);
            withoutTheGroups.removeAll(GROUP_PERMISSIONS);
            return new PreservedAttributes(Collections.unmodifiableSet(withoutTheGroups), owner, null, acl);
        }
    }

    /** Whether a mode grants the file's group anything at all. */
    private static boolean grantsAnythingToGroup(final @Nullable Set<PosixFilePermission> permissions) {
        return permissions != null && !Collections.disjoint(permissions, GROUP_PERMISSIONS);
    }

    /**
     * Reads the protections of the file about to be replaced, or {@link PreservedAttributes#NONE} when
     * there are none to reproduce.
     * <p>
     * <b>"Nothing to preserve" and "could not find out" are different answers and only the first one is
     * safe.</b> The previous version returned "nothing" for both, so an {@code IOException} or a
     * {@code SecurityException} on a file that exists and is protected ended with the replacement taking
     * the process umask — the write proceeded and the protection was silently dropped (R3-09). Only a
     * genuinely absent file, or a store that positively does not support a view, is "nothing"; a failure
     * to read one that should be there aborts the write before the partial file is even created.
     *
     * @throws IOException when the file exists but its protections cannot be determined
     */
    @VisibleForTesting
    static @NotNull PreservedAttributes preservedAttributesOf(final @NotNull Path path) throws IOException {
        Set<PosixFilePermission> permissions = null;
        UserPrincipal owner = null;
        GroupPrincipal group = null;
        List<AclEntry> acl = null;

        final PosixFileAttributeView posixView = Files.getFileAttributeView(path, PosixFileAttributeView.class);
        if (posixView != null) {
            try {
                final PosixFileAttributes attributes = posixView.readAttributes();
                permissions = attributes.permissions();
                owner = attributes.owner();
                group = attributes.group();
            } catch (final NoSuchFileException absent) {
                return PreservedAttributes.NONE;
            } catch (final IOException | SecurityException e) {
                throw new IOException(
                        "Could not read the permissions of the configuration file being replaced, so a"
                                + " replacement carrying the same protections cannot be produced; the existing"
                                + " file has been left untouched",
                        e);
            }
        }

        final AclFileAttributeView aclView = Files.getFileAttributeView(path, AclFileAttributeView.class);
        if (aclView != null) {
            try {
                // An empty list is a value, not an absence. An access-control list that grants nobody
                // anything is exactly what is on a file only its owner may read, and the previous version
                // discarded it as "nothing to carry" -- which left the replacement with whatever the
                // containing directory's inheritance produced, on the one file where that matters most
                // (EDG-882 review v04). Reproducing it costs nothing on stores that answer empty for every
                // file, because applyPreservedAttributes only calls setAcl when the replacement's own list
                // differs.
                acl = List.copyOf(aclView.getAcl());
            } catch (final NoSuchFileException absent) {
                return PreservedAttributes.NONE;
            } catch (final IOException | SecurityException e) {
                throw new IOException(
                        "Could not read the access-control list of the configuration file being replaced, so a"
                                + " replacement carrying the same protections cannot be produced; the existing"
                                + " file has been left untouched",
                        e);
            }
        }

        // Through the owner view rather than the POSIX one, because a store can have an owner without
        // having a mode: on an ACL-only store the owner was previously read only as a side effect of the
        // ACL block, and never restored at all (EDG-882 review v04).
        final FileOwnerAttributeView ownerView = Files.getFileAttributeView(path, FileOwnerAttributeView.class);
        if (owner == null && ownerView != null) {
            try {
                owner = ownerView.getOwner();
            } catch (final NoSuchFileException absent) {
                return PreservedAttributes.NONE;
            } catch (final IOException | SecurityException e) {
                throw new IOException(
                        "Could not read the owner of the configuration file being replaced, so a replacement"
                                + " carrying the same protections cannot be produced; the existing file has been"
                                + " left untouched",
                        e);
            }
        }

        if (posixView == null && aclView == null && ownerView == null) {
            // A store that exposes neither view has no protections a replacement could get wrong.
            if (!Files.exists(path)) {
                return PreservedAttributes.NONE;
            }
            log.debug("{} is on a file store with no POSIX, ACL or owner view; nothing to reproduce", path);
            return PreservedAttributes.NONE;
        }
        return new PreservedAttributes(permissions, owner, group, acl);
    }

    /**
     * Creates the file the configuration is rendered into, narrow from the moment it exists.
     * <p>
     * When there is anything to reproduce, the file is created owner-read/write and widened to the
     * target's protections after it has been written — the permissions are an attribute of the
     * {@code createFile} call rather than a {@code chmod} after the fact, so there is no instant at which
     * the file exists and is readable by anyone else. With nothing to reproduce, it is created normally
     * and takes the umask, which is what a first-time configuration file would have had anyway.
     * <p>
     * On a store with no mode at all — Windows and any other ACL-only file system — there is no such
     * attribute to create it with, and the previous version simply created the file with whatever the
     * containing directory's inheritance grants and wrote every credential in the configuration into it
     * (EDG-882 review v04). It is instead narrowed to its own owner, in a file that is still empty,
     * before any caller can write a byte of the configuration into it; the narrowing is proved before
     * this returns, so a store that accepts the call and does something else does not get the secrets
     * either.
     * <p>
     * That narrowing is not the target's own list: an access-control list naming a principal this
     * process is not would either lock the writer out of the file it just created, or — where the target
     * is readable by others — leave the replacement open to them for the whole write, which is the defect
     * itself. The window that remains holds an empty file.
     * <p>
     * A partial file left behind by a killed process is deleted rather than reused: reopening it would
     * inherit whatever mode <em>it</em> was left with.
     */
    @VisibleForTesting
    static void createPartialFile(final @NotNull Path partial, final @NotNull PreservedAttributes preserved)
            throws IOException {
        Files.deleteIfExists(partial);
        if (preserved.permissions() != null) {
            Files.createFile(partial, PosixFilePermissions.asFileAttribute(OWNER_ONLY));
        } else {
            // Nothing to reproduce, or a store that has no mode. The file takes the store's own default,
            // exactly as a first-time configuration file would -- and is narrowed below when the store
            // expresses protection as a list instead.
            Files.createFile(partial);
        }
        if (preserved.acl() != null) {
            narrowToItsOwner(partial, preserved.acl());
        }
    }

    /**
     * Replaces the inherited access-control list of the just-created, still-empty replacement with one
     * that grants its own owner everything and everyone else nothing, and refuses to go on unless the
     * result is narrow enough to write the configuration into.
     * <p>
     * <b>Narrow enough, not identical.</b> The first version demanded that the list read back be equal to
     * the list set, which is a claim about how a file store represents an access-control list rather than
     * about who can read the file. A store that reorders entries without changing what they decide,
     * normalises a permission mask, or hands back an inherited entry alongside the one just written would
     * have failed that comparison and refused every configuration write — on the only kind of store this
     * code path exists for, and in a way nothing off that platform can reproduce (EDG-882 review v04).
     * {@link AclComparison} answers what the lists grant instead, over every token that could be presented
     * to them, so a store may return the narrowing in whatever shape it likes and is held only to what it
     * decided.
     * <p>
     * <b>Two acceptable outcomes, in order of preference.</b> Owner-only, when the store took the list it
     * was given; otherwise no wider than the file about to be replaced, which the replacement is entitled
     * to be because that is exactly what it is about to become. The second is not a weakening: a Windows
     * directory that propagates entries to what is created inside it — {@code ProgramData} and most
     * installation trees do — hands every new file an inherited list, and {@code setAcl} cannot mark a
     * DACL protected through this API, so demanding owner-only would refuse every configuration write on
     * an ordinary node. That is review v04's finding 2.1 in the other direction, and it is a fault, not a
     * safeguard.
     */
    private static void narrowToItsOwner(final @NotNull Path partial, final @NotNull List<AclEntry> targetAcl)
            throws IOException {
        final AclFileAttributeView view = Files.getFileAttributeView(partial, AclFileAttributeView.class);
        if (view == null) {
            throw new IOException("The replacement for the configuration file cannot be given an access-control"
                    + " list on this file store, so it cannot be written without disclosing the configuration;"
                    + " the existing file has been left untouched");
        }
        final List<AclEntry> ownerOnly = List.of(AclEntry.newBuilder()
                .setType(AclEntryType.ALLOW)
                .setPrincipal(view.getOwner())
                .setPermissions(EnumSet.allOf(AclEntryPermission.class))
                .build());
        try {
            view.setAcl(ownerOnly);
        } catch (final UnsupportedOperationException | SecurityException | IOException e) {
            throw new IOException(
                    "The replacement for the configuration file could not be restricted to its owner before"
                            + " being written; the existing file has been left untouched",
                    e);
        }
        final List<AclEntry> actual = view.getAcl();
        if (AclComparison.grantsNoMoreThan(actual, ownerOnly)) {
            return;
        }
        if (AclComparison.grantsNoMoreThan(actual, targetAcl)) {
            // Not owner-only, but no wider than the file it is about to replace -- which is the property
            // that matters: the replacement is never readable by anyone the configuration it replaces is
            // not, at any point while it holds the configuration. Worth saying out loud, because it means
            // the store did not do what it was asked.
            log.warn(
                    "The replacement for the configuration file could not be restricted to its owner: {} keeps"
                            + " an access-control list this node did not set. It is no wider than the"
                            + " configuration file being replaced, so the configuration is written into it"
                            + " anyway.",
                    partial);
            return;
        }
        throw new IOException("The replacement for the configuration file is readable by principals the"
                + " configuration file being replaced is not, so the configuration cannot be written into it;"
                + " the existing file has been left untouched");
    }

    /**
     * Gives the written replacement exactly the protections of the file it is about to replace, and
     * proves it before the move.
     * <p>
     * Order matters: owner and group are set while the file is still owner-only, then the ACL, then the
     * mode last. Setting the mode first would open a window in which the file is already group- or
     * world-readable but still owned by the wrong principals.
     * <p>
     * <b>What is guaranteed is that the replacement is never more accessible than the file it replaces —
     * not that every protection is reproduced exactly.</b> Both extremes are defects and both have been
     * shipped here. Carrying what you can and moving it either way is R3-07 and R3-09 between them: a
     * failure left a file full of credentials with the umask's mode and the creating process's group,
     * permanently, announced at debug level. Refusing the replacement outright is the other, and it is not
     * free — it is the <em>write</em> that is refused, so the configuration the operator just asked for is
     * not persisted, and the node goes on running correctly until a restart brings back the older file
     * without it. Where a protection cannot be reproduced, this narrows instead: the replacement is given
     * less than the file it replaces rather than more, and the write goes on.
     * <p>
     * Two protections are not this node's to set, and each narrows in its own way.
     * <ul>
     * <li><b>The owner.</b> Changing it is privileged everywhere this runs, so refusing on it meant a
     * configuration installed by one account and served by another could never be written at all
     * (EDG-882 review v04). It falls back to this node's own account, which discloses nothing: the account
     * that just rendered the document already holds every credential in it.</li>
     * <li><b>The group.</b> Setting it requires membership of it, so the ordinary container case — a
     * {@code config.xml} installed under one group, Edge running under another — failed here and stopped
     * the node persisting anything at all, found in QA on this branch. Keeping the mode while the group
     * falls back to this process's own is exactly R3-09's disclosure, so the group's permissions come off
     * the mode with it: the replacement grants the group nothing rather than granting it to the wrong
     * principals. Nobody gains, someone may lose, and that is said out loud.</li>
     * </ul>
     * <p>
     * The mode and the access-control list keep refusing. Neither can fail the way those two do — both are
     * set on a file this process created and owns, which is all either call asks for — so a failure there
     * is the store saying something is wrong, not a privilege this node was never going to have.
     * <p>
     * What is applied is taken from what has been proved rather than from what was asked for, so a
     * protection that could not be reproduced cannot be granted back through another one. A file this node
     * may not write in the first place never gets here: {@code writeConfigToXML} turns that away before
     * anything is written.
     */
    @VisibleForTesting
    static void applyPreservedAttributes(final @NotNull Path partial, final @NotNull PreservedAttributes preserved)
            throws IOException {
        if (preserved.nothingToReproduce()) {
            return;
        }
        // What the verification below is entitled to insist on, and what the mode at the end is taken
        // from: everything, less whatever turned out not to be this node's to set.
        PreservedAttributes proven = preserved;
        try {
            // Owner through the owner view, not the POSIX one: an ACL-only store has an owner and no
            // mode, and reading it through the POSIX view meant it was never restored there at all
            // (EDG-882 review v04).
            //
            // Only when they differ, here and below. Changing owner is a privileged operation on every
            // platform this runs on, and changing group requires membership; asking for a change that is
            // already true would fail for no reason on the ordinary path, where the node replaces a file
            // it owns.
            if (preserved.owner() != null) {
                final FileOwnerAttributeView partialOwner =
                        Files.getFileAttributeView(partial, FileOwnerAttributeView.class);
                if (partialOwner == null) {
                    throw new IOException("The replacement cannot carry an owner on this file store");
                }
                if (!preserved.owner().equals(partialOwner.getOwner())) {
                    try {
                        partialOwner.setOwner(preserved.owner());
                    } catch (final UnsupportedOperationException | SecurityException | IOException notPermitted) {
                        // The one protection that is not the node's to reproduce. Changing a file's owner
                        // is privileged on every platform this runs on, so a configuration installed by
                        // one account and served by another -- root-owned config.xml, service user running
                        // Edge -- made every write fail, and the node silently stopped persisting anything
                        // (EDG-882 review v04). The mode, the group and any access-control list are still
                        // reproduced exactly or the write is refused, and those are what decide who else
                        // can read the file; the owner it falls back to is the account that just rendered
                        // the configuration and therefore already holds every credential in it.
                        //
                        // The store's message, not the throwable: this is handled, and a stack trace on
                        // every write reads as a failure to anything scanning the log -- Pulse's system
                        // tests fail a test on any "Exception" line, and did, on every config write.
                        log.warn(
                                "The replacement for {} could not be given the owner of the file it replaces"
                                        + " ('{}'), so it is owned by this node's own account instead. Its mode,"
                                        + " group and access-control list are unchanged, so no one else gains"
                                        + " access -- but if the owner matters here, set it back and check what"
                                        + " installed the file. The store said: {}",
                                partial,
                                preserved.owner().getName(),
                                notPermitted.getMessage());
                        proven = proven.withoutOwner();
                    }
                }
            }
            if (preserved.group() != null) {
                final PosixFileAttributeView partialPosix =
                        Files.getFileAttributeView(partial, PosixFileAttributeView.class);
                if (partialPosix == null) {
                    throw new IOException("The replacement cannot carry a group on this file store");
                }
                if (!preserved.group().equals(partialPosix.readAttributes().group())) {
                    try {
                        partialPosix.setGroup(preserved.group());
                    } catch (final UnsupportedOperationException | SecurityException | IOException notPermitted) {
                        // Setting a file's group requires membership of it, so the ordinary container case
                        // -- config.xml installed under one group, Edge running under another -- refused
                        // the whole write here. The node then went on running correctly while persisting
                        // nothing at all, and every REST change since the last restart was lost on the
                        // next one (found in QA on this branch).
                        //
                        // Dropping the group takes the mode's group permissions with it. Keeping them
                        // would grant group-read of every credential in the configuration to whichever
                        // group this node belongs to, which is R3-09 in the one place it still could
                        // happen. The verification below is told not to insist on either.
                        proven = proven.withoutGroupAccess();
                        if (grantsAnythingToGroup(preserved.permissions())) {
                            log.warn(
                                    "The replacement for {} could not be given the group of the file it replaces"
                                            + " ('{}'), so the permissions that mode grants to the group have been"
                                            + " removed from it rather than handed to this node's own group. The"
                                            + " configuration is still written and nobody gains access, but"
                                            + " principals that could read it through that group no longer can."
                                            + " Add this node's account to that group, or give the file a group it"
                                            + " is already in, to keep it readable. The store said: {}",
                                    partial,
                                    preserved.group().getName(),
                                    notPermitted.getMessage());
                        } else {
                            // The mode grants the group nothing, so which group it names decides nobody's
                            // access and dropping it costs nothing. Not worth a warning on every write.
                            log.debug(
                                    "The replacement for {} could not be given the group of the file it replaces"
                                            + " ('{}'). That file's mode grants its group nothing, so no principal"
                                            + " gains or loses access. The store said: {}",
                                    partial,
                                    preserved.group().getName(),
                                    notPermitted.getMessage());
                        }
                    }
                }
            }
            if (preserved.acl() != null) {
                final AclFileAttributeView partialAcl = Files.getFileAttributeView(partial, AclFileAttributeView.class);
                if (partialAcl == null) {
                    throw new IOException("The replacement cannot carry an access-control list on this file store");
                }
                if (!preserved.acl().equals(partialAcl.getAcl())) {
                    partialAcl.setAcl(preserved.acl());
                }
            }
            // From what was proved, not from what was asked for: if the group could not be reproduced, the
            // permissions it would have had are no longer in here to grant.
            if (proven.permissions() != null) {
                Files.setPosixFilePermissions(partial, proven.permissions());
            }
        } catch (final UnsupportedOperationException | SecurityException | IOException e) {
            throw new IOException(
                    "Could not reproduce the protections of the configuration file being replaced; "
                            + "the existing file has been left untouched",
                    e);
        }
        verifyPreservedAttributes(partial, proven);
    }

    /**
     * Re-reads what was just applied and refuses the replacement if any of it did not take.
     * <p>
     * Every setter above can succeed on a store that then quietly reports something else back —
     * a mapped volume that ignores ownership, a mode masked by a mount option. The whole point of this
     * code is that the replacement is no more readable than the file it replaces, and that is a claim
     * worth checking rather than assuming, because the cost of it being wrong is every credential in the
     * configuration.
     */
    @VisibleForTesting
    static void verifyPreservedAttributes(final @NotNull Path partial, final @NotNull PreservedAttributes preserved)
            throws IOException {
        // Every attribute that was carried, whichever view expresses it. The previous version read the
        // POSIX view and returned when there was none, so on an ACL-only store it verified nothing at all
        // (EDG-882 review v04). The access-control list is checked here rather than where it is applied
        // because the mode is set after it: on a store that has both, a chmod can rewrite the mask of the
        // list that was just applied, and the file's protection is whatever survives that.
        final PosixFileAttributeView posixView = Files.getFileAttributeView(partial, PosixFileAttributeView.class);
        if (posixView != null) {
            final PosixFileAttributes actual = posixView.readAttributes();
            if (preserved.permissions() != null && !preserved.permissions().equals(actual.permissions())) {
                throw new IOException("The replacement's permissions are "
                        + PosixFilePermissions.toString(actual.permissions())
                        + " but the configuration file being replaced has "
                        + PosixFilePermissions.toString(preserved.permissions())
                        + "; the existing file has been left untouched");
            }
            if (preserved.group() != null && !preserved.group().equals(actual.group())) {
                throw new IOException(
                        "The replacement's group is '" + actual.group().getName() + "' but the"
                                + " configuration file being replaced has group '"
                                + preserved.group().getName()
                                + "'; the existing file has been left untouched");
            }
        }
        if (preserved.owner() != null) {
            final FileOwnerAttributeView ownerView = Files.getFileAttributeView(partial, FileOwnerAttributeView.class);
            final UserPrincipal actualOwner = ownerView == null ? null : ownerView.getOwner();
            if (!preserved.owner().equals(actualOwner)) {
                throw new IOException(
                        "The replacement is owned by '" + (actualOwner == null ? "nobody" : actualOwner.getName())
                                + "' but the configuration file being replaced is owned by '"
                                + preserved.owner().getName()
                                + "'; the existing file has been left untouched");
            }
        }
        if (preserved.acl() != null) {
            final AclFileAttributeView aclView = Files.getFileAttributeView(partial, AclFileAttributeView.class);
            final List<AclEntry> actualAcl = aclView == null ? null : aclView.getAcl();
            if (actualAcl == null || !AclComparison.grantsNoMoreThan(actualAcl, preserved.acl())) {
                throw new IOException("The replacement's access-control list grants access the configuration file being"
                        + " replaced does not: it is " + actualAcl + " where that file has "
                        + preserved.acl() + "; the existing file has been left untouched");
            }
            if (!AclComparison.grantsNoMoreThan(preserved.acl(), actualAcl)) {
                // Narrower than the file it replaces. Nobody gains anything, so this is not the disclosure
                // the check is here for -- but someone who could read the configuration no longer can, and
                // that is not something to find out later.
                log.warn(
                        "The replacement for the configuration file has a narrower access-control list than the"
                                + " file it replaces: {} where that file has {}. Nobody gains access, but"
                                + " principals that could read the configuration may no longer be able to.",
                        actualAcl,
                        preserved.acl());
            }
        }
    }
}
