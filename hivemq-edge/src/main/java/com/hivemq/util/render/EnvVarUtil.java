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
package com.hivemq.util.render;

import com.google.common.annotations.VisibleForTesting;
import com.hivemq.exceptions.UnrecoverableException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Util for handling system environment variables
 *
 * @author Christoph Schäbel
 */
public class EnvVarUtil {

    private static final Logger log = LoggerFactory.getLogger(EnvVarUtil.class);

    private static final @NotNull String ENV_VAR_PATTERN = "\\$\\{ENV:(.*?)}";

    /**
     * Get a Java system property or system environment variable with the specified name.
     * If a variable with the same name exists in both targets the Java system property is returned.
     *
     * @param name the name of the environment variable
     * @return the value of the environment variable with the specified name
     */
    public static @Nullable String getValue(final @NotNull String name) {
        // also check java properties if system variable is not found
        final var systemProperty = System.getProperty(name);
        if (systemProperty != null) {
            return systemProperty;
        }

        return System.getenv(name);
    }

    /**
     * Replaces placeholders like '${ENV:VAR_NAME}' with the according environment variables.
     *
     * @param text the text which contains placeholders (or not)
     * @return the text with all the placeholders replaced
     * @throws UnrecoverableException if a variable used in a placeholder is not set
     */
    public static @NotNull String replaceEnvironmentVariablePlaceholders(final @NotNull String text) {
        final var resultString = new StringBuilder();

        final var matcher = Pattern.compile(ENV_VAR_PATTERN).matcher(text);

        while (matcher.find()) {

            if (matcher.groupCount() < 1) {
                // this should never happen as we declared 1 groups in the ENV_VAR_PATTERN
                log.warn("Found unexpected environment variable placeholder in config.xml");
                matcher.appendReplacement(resultString, "");
                continue;
            }

            final var varName = matcher.group(1);

            final var replacement = getValue(varName);

            if (replacement == null) {
                log.error("Environment Variable {} for HiveMQ config.xml is not set.", varName);
                throw new UnrecoverableException(false);
            }

            // sets replacement for this match
            matcher.appendReplacement(resultString, escapeReplacement(replacement));
        }

        // adds everything except the replacements to the string buffer
        matcher.appendTail(resultString);

        return resultString.toString();
    }

    private static @NotNull String escapeReplacement(final @NotNull String replacement) {
        return replacement.replace("\\", "\\\\").replace("$", "\\$");
    }

    /**
     * A {@code ${ENV:...}} placeholder that is the entire text of one element, and the value it renders
     * to: the element's name, the placeholder as written, and that value.
     */
    public record ElementPlaceholder(
            @NotNull String element,
            @NotNull String literal,
            @NotNull String value) {}

    /**
     * Matches a placeholder that is the whole text of an element, capturing the element name and any
     * whitespace the operator wrote around it.
     * <p>
     * The padding is captured rather than skipped because the element's text is what the document ends up
     * holding: the file is rendered before it is parsed, so {@code <password>\n  ${ENV:PW}\n</password>}
     * becomes an element whose text is the value <em>with</em> that padding, and the marshaller writes it
     * back the same way. A search string built from the bare value then matches nothing, the restore gives
     * up, and the credential is written out — which is what this looked like before
     * {@code whenThePlaceholderIsWrittenWithSurroundingWhitespace_thenItIsStillRestored} was written
     * (EDG-882 review v02, R2-20).
     */
    private static final @NotNull Pattern ELEMENT_PLACEHOLDER =
            Pattern.compile("<([A-Za-z_][\\w.:-]*)>(\\s*)(\\$\\{ENV:(.*?)})(\\s*)</\\1>");

    private static final @NotNull Pattern XML_COMMENT = Pattern.compile("(?s)<!--.*?-->");

    /**
     * The {@code ${ENV:...}} placeholders of the file as it was written that
     * {@link #restorePlaceholders} is able to put back.
     * <p>
     * Only placeholders that are an element's entire text are collected, because those are the only ones
     * that can be located again in a document the marshaller rebuilt from the configuration model: a
     * placeholder in an attribute, or one concatenated with other text, has no anchor to return to. Such
     * a placeholder is reported by {@code restorePlaceholders} rather than silently resolved.
     * <p>
     * Comments are stripped first. A commented-out block is not part of the configuration and never
     * reaches the marshalled document, but its placeholders would otherwise be counted and make every
     * occurrence look ambiguous — commenting a bridge out is an ordinary thing for an operator to do.
     */
    public static @NotNull List<ElementPlaceholder> collectPlaceholders(final @NotNull String text) {
        final String withoutComments = XML_COMMENT.matcher(text).replaceAll("");
        final List<ElementPlaceholder> placeholders = new ArrayList<>();
        final var matcher = ELEMENT_PLACEHOLDER.matcher(withoutComments);
        while (matcher.find()) {
            final var value = getValue(matcher.group(4));
            if (value != null && !value.isEmpty()) {
                // Both sides carry the operator's padding, so the search string is what the marshaller
                // wrote and the replacement is what they typed, character for character.
                final String leading = matcher.group(2);
                final String trailing = matcher.group(5);
                placeholders.add(new ElementPlaceholder(
                        matcher.group(1), leading + matcher.group(3) + trailing, leading + value + trailing));
            }
        }
        return placeholders;
    }

    /**
     * Puts {@code ${ENV:...}} placeholders back into a rendered document before it is written to disk.
     * <p>
     * Rendering happens once, on the whole file, before it is parsed, so the configuration Edge holds in
     * memory contains the <em>values</em>. Writing that back out — which any REST change to any subsystem
     * does — replaced the operator's placeholders with what they resolved to, so a bridge password
     * supplied through an environment variable ended up in {@code config.xml} in plain text, and the
     * indirection the operator chose was gone for good (EDG-882 QA round 2).
     * <p>
     * <b>Anchored to the element, and count-checked, because the reverse direction is ambiguous by
     * nature.</b> A placeholder is only put back into the same element name it came from, and only when
     * the document holds exactly as many such elements as the original file did. Matching on the value
     * alone was not enough: a variable that resolves to a string the operator also wrote somewhere else
     * — {@code ${ENV:NODE_NAME}} rendering to a bridge's own id, say — would have rewritten that other
     * element into a variable reference nobody asked for.
     * <p>
     * <b>What happens when it cannot decide</b> is {@link #giveUpOn}: an error naming the element and the
     * reason, and, for an element that holds a credential, {@link #UNRESTORED_SECRET} written in place of
     * the value so that no secret reaches the disk unannounced (EDG-882 review v02, R2-04). It used to be
     * a warning and the value.
     * <p>
     * <b>Known blind spots</b>, all of them cases {@link #collectPlaceholders} never sees, so they are
     * resolved on the way in and stay resolved on the way out with nothing reported at all:
     * <ul>
     *   <li>a placeholder in an <em>attribute</em> rather than in element text;</li>
     *   <li>a placeholder <em>concatenated</em> with other text, as in
     *       {@code <host>edge-${ENV:SITE}</host>} — there is no anchor to return it to;</li>
     *   <li>a placeholder inside a <em>configuration fragment</em> or an {@code <if>} block, which are
     *       flattened into the document before this runs.</li>
     * </ul>
     * A credential written through any of these is materialised into {@code config.xml} exactly as it was
     * before this method existed. Closing them means restoring at the level the operator wrote at rather
     * than at the element, which is a larger change than this one.
     *
     * @param placeholders what {@link #collectPlaceholders(String)} found in the file as it was written
     */
    public static @NotNull String restorePlaceholders(
            final @NotNull String renderedXml, final @NotNull List<ElementPlaceholder> placeholders) {
        if (placeholders.isEmpty()) {
            return renderedXml;
        }
        // Grouped by the element the placeholder occupied and the value it renders to. Two placeholders
        // that share both are indistinguishable in the marshalled document; a group whose literal is not
        // unique is left alone rather than guessed at.
        final Map<String, List<ElementPlaceholder>> byElementAndValue = new LinkedHashMap<>();
        for (final ElementPlaceholder placeholder : placeholders) {
            byElementAndValue
                    .computeIfAbsent(placeholder.element() + ' ' + placeholder.value(), key -> new ArrayList<>())
                    .add(placeholder);
        }

        var result = renderedXml;
        for (final List<ElementPlaceholder> group : byElementAndValue.values()) {
            final ElementPlaceholder first = group.get(0);
            final String literal = first.literal();
            final String element =
                    "<" + first.element() + ">" + escapeXmlText(first.value()) + "</" + first.element() + ">";
            final int inDocument = countOccurrences(result, element);

            if (group.stream().anyMatch(placeholder -> !placeholder.literal().equals(literal))) {
                result = giveUpOn(
                        result,
                        first,
                        element,
                        "two different placeholders fill it with the same value, so neither of them can be told"
                                + " from the other");
                continue;
            }
            if (inDocument == 0) {
                // Nothing to replace: the element the placeholder filled is not in the document as this
                // expects it. Either it left the configuration, or the marshaller wrote its text
                // differently -- normalising a typed field (TRUE -> true, 01883 -> 1883), or anything else
                // that makes the element's text not what was collected.
                //
                // This branch can leak: if the element is still there under a text this did not predict,
                // the value is on disk and nothing here can find it to take it out. That is why the
                // collection side has to record the text exactly as the document will hold it -- the
                // whitespace case (R2-20) was precisely this, and it was a credential written in plain.
                log.error(
                        "The '<{}>' element that '{}' filled is not in the configuration being written, so the"
                                + " placeholder cannot be restored. If the element was not removed, check the file"
                                + " for a value that should have stayed a variable reference.",
                        first.element(),
                        literal);
                continue;
            }
            if (inDocument != group.size()) {
                result = giveUpOn(
                        result,
                        first,
                        element,
                        "'" + literal + "' fills " + group.size() + " of them but the configuration being written"
                                + " has " + inDocument + ", so which ones came from the variable cannot be told");
                continue;
            }
            result = result.replace(element, "<" + first.element() + ">" + literal + "</" + first.element() + ">");
        }
        return result;
    }

    /**
     * What is written in place of a secret whose placeholder could not be restored.
     * <p>
     * Not the value, which is the whole point; not an empty element either, because some secret-bearing
     * elements are {@code nonEmptyString} in the schema and an empty one would make the file unparseable
     * on the next start — a worse failure than the one being avoided, and one that names the schema
     * rather than the cause. This is a placeholder for a variable nobody sets, so the next start stops
     * with {@code Environment Variable EDGE_UNRESTORED_SECRET for HiveMQ config.xml is not set} and the
     * operator is told exactly where to look, while the node they are running now carries on with the
     * secret it already holds in memory.
     */
    @VisibleForTesting
    static final @NotNull String UNRESTORED_SECRET = "${ENV:EDGE_UNRESTORED_SECRET}";

    /**
     * Element names whose text is a credential. Matched as a suffix so that the adapter configurations —
     * {@code xs:any} in the schema, so their element names are not knowable here — are covered by the
     * same rule as {@code <password>}, {@code <client-secret>}, {@code <private-key-password>} and
     * {@code <truststore-password>}.
     */
    private static final @NotNull List<String> SECRET_ELEMENT_SUFFIXES =
            List.of("password", "secret", "passphrase", "token", "credentials");

    private static boolean isSecretElement(final @NotNull String element) {
        final String name = element.toLowerCase(Locale.ROOT);
        return SECRET_ELEMENT_SUFFIXES.stream().anyMatch(name::endsWith);
    }

    /**
     * Reports a placeholder that cannot be put back, and keeps its value off the disk when that value is
     * a credential.
     * <p>
     * The restore is anchored to an element name and count-checked, which is sound when the counts line
     * up and ambiguous when they do not; there is no reading of an ambiguous case that is right, because
     * an element holding the value and an element holding a literal that happens to equal it are the same
     * bytes by the time the document is marshalled. So the ambiguous case is decided by which mistake can
     * be undone. Writing a credential out cannot be: it is on disk, usually under version control, and
     * rotating it is the only remedy. Replacing one the operator had written literally can be — the write
     * takes a rolling backup of {@code config.xml} first, and the element is named in the error below.
     * <p>
     * A non-secret takes the other side of that trade and keeps its value: losing the indirection is
     * permanent and worth an error, but poisoning a {@code <port>} would stop a node from starting over
     * something that is not a disclosure.
     */
    private static @NotNull String giveUpOn(
            final @NotNull String document,
            final @NotNull ElementPlaceholder placeholder,
            final @NotNull String element,
            final @NotNull String reason) {
        if (!isSecretElement(placeholder.element())) {
            log.error(
                    "The '<{}>' placeholder cannot be restored when the configuration file is written, because"
                            + " {}. Its value is written out instead and the variable reference is lost; put it"
                            + " back by hand once the ambiguity is resolved.",
                    placeholder.element(),
                    reason);
            return document;
        }
        log.error(
                "The '<{}>' element holds a credential and its placeholder cannot be restored, because {}."
                        + " Rather than write the credential to config.xml it is written as '{}', which no"
                        + " variable sets: this node keeps running on the value it already holds, and the next"
                        + " start will stop and name that variable. Restore the intended '${ENV:...}'"
                        + " reference in config.xml -- the previous file is in the rolling backup beside it.",
                placeholder.element(),
                reason,
                UNRESTORED_SECRET);
        return document.replace(
                element, "<" + placeholder.element() + ">" + UNRESTORED_SECRET + "</" + placeholder.element() + ">");
    }

    private static int countOccurrences(final @NotNull String text, final @NotNull String needle) {
        int count = 0;
        int index = text.indexOf(needle);
        while (index >= 0) {
            count++;
            index = text.indexOf(needle, index + needle.length());
        }
        return count;
    }

    /**
     * The three characters a marshaller escapes inside element text.
     * <p>
     * Defensive only, and unreachable through {@code ${ENV:...}}:
     * {@link #replaceEnvironmentVariablePlaceholders} splices a value into the document as raw text
     * before it is parsed, escaping only the regex metacharacters, so a value carrying one of these
     * makes the file malformed and the configuration fails to read. Kept because the search string has
     * to be what the marshaller wrote for any value that reaches here by another route.
     */
    private static @NotNull String escapeXmlText(final @NotNull String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
