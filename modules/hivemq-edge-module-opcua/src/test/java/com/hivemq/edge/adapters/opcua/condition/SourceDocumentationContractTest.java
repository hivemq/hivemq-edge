/*
 * Copyright 2023-present HiveMQ GmbH
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
package com.hivemq.edge.adapters.opcua.condition;

import static org.assertj.core.api.Assertions.assertThat;

import com.hivemq.edge.adapters.opcua.config.tag.OpcuaTagKind;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Source comments that enumerate something the code owns, checked against the code.
 * <p>
 * Review-04 finding 6, and the third review running in which prose in this module described an older design.
 * The pattern is always the same: a comment restates a list — the condition methods, the command's fields,
 * the tag kinds — the list grows, and only one of the two copies is updated. Nothing catches it, because a
 * stale comment compiles.
 * <p>
 * The durable fix for most of these was to stop restating the list and point at the enum instead, and where
 * that was done there is nothing left here to check. Three could not be reduced that way — the shape line,
 * the kind summary and the argument categories each have to be readable as prose at the point where someone
 * is deciding what to send — so those are checked against the code that owns them.
 * <p>
 * <b>What is deliberately not covered.</b> The finding also had a structural half: a {@code @link} to a type
 * that no longer exists, and a Javadoc block orphaned between two methods. The second is caught here, because
 * two adjacent comment blocks are a two-line regex with no false positives. The first is not: deciding
 * whether a reference resolves means resolving it, and a hand-rolled approximation of that would fail on
 * inherited members, imports and nested types long before it caught anything. Doclint does it properly and is
 * the right tool if this recurs; it is not wired in, so a dead link is currently invisible to the build.
 * <p>
 * Reading the source file is the only way to observe a comment — Javadoc is not retained in the class file,
 * so there is no reflective route to it. The paths are resolved from the module root rather than hardcoded,
 * and a file that cannot be found fails rather than skips: a documentation test that quietly stops running
 * is worse than none, because the thing it guards looks guarded.
 */
class SourceDocumentationContractTest {

    private static final @NotNull Path MAIN = Path.of("src/main/java/com/hivemq/edge/adapters/opcua");

    @Test
    void theDocumentedCommandShapeListsEveryFieldTheSchemaBuilds() throws IOException {
        // The exact defect: writeSchema() gained a selectedResponse property and its Javadoc kept saying
        // "{method, eventId?, comment?, duration?}". A caller reading the comment to find out what they may
        // send would conclude RESPOND cannot be expressed.
        // The full signature, not the bare name: an earlier version matched "writeSchema()" and found a
        // {@link #writeSchema()} in a different method's comment a hundred lines above the declaration.
        final String javadoc = javadocPreceding(
                MAIN.resolve("condition/ConditionSchemas.java"), "public static @NotNull Schema writeSchema()");

        // The braced literal, not the whole comment. The defect was the shape line saying
        // "{method, eventId?, comment?, duration?}" while the prose below it went on to explain
        // selectedResponse -- so a test that only asked whether the field is mentioned anywhere would have
        // read that as documented. The line a caller copies is the one that has to be complete.
        final String shape = shapeLiteralIn(javadoc);

        assertThat(shape)
                .as("the shape line has to name every property the method below it builds")
                .contains(ConditionUpdate.FIELD_METHOD)
                .contains(ConditionUpdate.FIELD_EVENT_ID)
                .contains(ConditionUpdate.FIELD_COMMENT)
                .contains(ConditionUpdate.FIELD_DURATION)
                .contains(ConditionUpdate.FIELD_SELECTED_RESPONSE);
    }

    @Test
    void theKindSummaryListsEveryKind() throws IOException {
        // REFRESH was an enum constant with its own Javadoc, absent from the class-level list of kinds
        // directly above it -- so the summary a reader meets first said there were three.
        final String javadoc = javadocPreceding(
                Path.of("src/main/java/com/hivemq/edge/adapters/opcua/config/tag/OpcuaTagKind.java"),
                "public enum OpcuaTagKind");

        for (final OpcuaTagKind kind : OpcuaTagKind.values()) {
            assertThat(javadoc)
                    .as("the kind summary omits %s, so a reader meets a shorter list than the enum", kind)
                    .contains(kind.name());
        }
    }

    @Test
    void theArgumentCategoriesAreAllDescribedWhereTheCommandIsParsed() throws IOException {
        // "three take (EventId, Comment), ten take no arguments at all, and TimedShelve takes a duration"
        // was true until RESPOND was added, and then quietly was not. The counts are gone now; what remains
        // is the four shapes, and this fails if a fifth is added without a word about it.
        final String javadoc =
                javadocPreceding(MAIN.resolve("condition/ConditionUpdate.java"), "public record ConditionUpdate");

        assertThat(ConditionUpdate.Method.Arguments.values())
                .as("a new argument shape means a new sentence here, not just a new enum constant")
                .hasSize(4);
        assertThat(javadoc)
                .contains("EventId", "Comment")
                .contains("Duration")
                .contains("SelectedResponse")
                .contains("none at all");
    }

    @Test
    void noDeclarationIsPrecededByTwoJavadocBlocks() throws IOException {
        // The other half of the finding: OpcUaProtocolAdapter carried two Javadoc blocks back to back, so the
        // first -- "Requests a state transition on a condition" -- documented requestRefresh() while
        // writeConditionUpdate() had none. Both compile, and the wrong one is what a reader and the generated
        // API documentation get.
        //
        // Cheap to detect and free of false positives: nobody writes two adjacent Javadoc blocks on purpose,
        // and a comment that is genuinely detached belongs in a /* */ anyway. This is not a substitute for
        // doclint -- a {@link} to a type that no longer exists is still invisible to the build, and
        // reimplementing reference resolution here would be worse than leaving it uncovered.
        for (final Path source : mainSources()) {
            assertThat(withoutStrings(Files.readString(source, StandardCharsets.UTF_8)))
                    .as(
                            "%s has a Javadoc block immediately followed by another, so the first is attached to "
                                    + "nothing",
                            source.getFileName())
                    .doesNotContainPattern("\\*/\\s*+/\\*\\*");
        }
    }

    @Test
    void aSourceFileThatMovedIsAFailureRatherThanASkip(final @TempDir @NotNull Path elsewhere) {
        // The property that makes the three above worth having. A path-based test that answers "nothing to
        // check" when the file is renamed reports success for a file it never opened.
        assertThat(javadocOrNull(elsewhere.resolve("Gone.java"), "anything")).isNull();
    }

    // ── helpers ─────────────────────────────────────────────────────────────────────────────────────

    /** Every main source file of the module. */
    private static @NotNull List<Path> mainSources() throws IOException {
        try (var files = Files.walk(moduleRoot().resolve(MAIN))) {
            final List<Path> sources =
                    files.filter(path -> path.toString().endsWith(".java")).toList();
            assertThat(sources)
                    .as("no sources found under %s -- the module layout moved", MAIN)
                    .isNotEmpty();
            return sources;
        }
    }

    /**
     * The file with string and character literals blanked out, so a sequence that only <em>looks</em> like a
     * comment boundary inside one cannot be mistaken for the real thing.
     */
    private static @NotNull String withoutStrings(final @NotNull String source) {
        return source.replaceAll("\"([^\"\\\\\n]|\\\\.)*\"", "\"\"").replaceAll("'([^'\\\\\n]|\\\\.)'", "' '");
    }

    /**
     * The braced literal a comment uses to state a command's shape — the one line a caller copies.
     * <p>
     * Described rather than quoted: writing the tag out nested inside a {@code @code} tag is what the
     * compiler was warning about, and a Javadoc example of malformed Javadoc is not worth the warning.
     * <p>
     * Whitespace is collapsed because the literal may be wrapped across lines, and a line break inside it is
     * a formatting decision rather than a change to what it says.
     */
    private static @NotNull String shapeLiteralIn(final @NotNull String javadoc) {
        final int open = javadoc.indexOf("{@code {");
        assertThat(open).as("no {@code {...}} shape line in this comment").isNotNegative();
        final int close = javadoc.indexOf('}', javadoc.indexOf('{', open + "{@code ".length()));
        assertThat(close).as("the shape line is not closed").isNotNegative();
        return javadoc.substring(open, close + 1).replaceAll("\\s*\\*\\s*", " ");
    }

    /**
     * The Javadoc block immediately above a declaration.
     * <p>
     * Located by the declaration rather than by line number so that editing the file above it does not
     * silently move the test's target onto a different comment.
     */
    private static @NotNull String javadocPreceding(final @NotNull Path source, final @NotNull String declaration)
            throws IOException {

        final String javadoc = javadocOrNull(source, declaration);
        assertThat(javadoc)
                .as(
                        "no Javadoc found above '%s' in %s -- the declaration was renamed, or the comment removed",
                        declaration, source)
                .isNotNull();
        return javadoc;
    }

    private static @org.jetbrains.annotations.Nullable String javadocOrNull(
            final @NotNull Path source, final @NotNull String declaration) {

        final String text;
        try {
            text = Files.readString(moduleRoot().resolve(source), StandardCharsets.UTF_8);
        } catch (final IOException e) {
            return null;
        }
        final int declarationAt = text.indexOf(declaration);
        if (declarationAt < 0) {
            return null;
        }
        final int end = text.lastIndexOf("*/", declarationAt);
        if (end < 0) {
            return null;
        }
        final int start = text.lastIndexOf("/**", end);
        return start < 0 ? null : text.substring(start, end);
    }

    /**
     * The module directory, whether the test was launched from it or from the composite root.
     * <p>
     * Gradle runs tests with the project directory as the working directory, but a run from an IDE or from
     * the composite may not, and a documentation test that depends on how it was started is one that passes
     * for the wrong reason somewhere.
     */
    private static @NotNull Path moduleRoot() {
        Path candidate = Path.of("").toAbsolutePath();
        for (int up = 0; up < 6 && candidate != null; up++) {
            if (Files.isDirectory(candidate.resolve(MAIN))) {
                return candidate;
            }
            candidate = candidate.getParent();
        }
        return Path.of("").toAbsolutePath();
    }
}
