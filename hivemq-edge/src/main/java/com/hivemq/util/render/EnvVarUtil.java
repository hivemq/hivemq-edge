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
import java.util.regex.Matcher;
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
     * One span of the configuration file that contained at least one {@code ${ENV:...}} placeholder, and
     * the text it renders to.
     * <p>
     * {@code name} is the element name, or the attribute name when {@code attribute} is set. {@code
     * literal} is the span exactly as the operator wrote it, placeholders and all; {@code value} is what
     * that span renders to, which is what the marshaller will have written.
     * <p>
     * The span is the <em>whole</em> text of the element or attribute, not the placeholder alone. That is
     * what makes a concatenation restorable: {@code <host>edge-${ENV:SITE}</host>} is one span whose
     * literal is {@code edge-${ENV:SITE}} and whose value is {@code edge-berlin}, and putting the literal
     * back where the value is found needs no knowledge of where inside it the placeholder sat (EDG-882
     * review v03, R3-01).
     */
    public record ElementPlaceholder(
            @NotNull String name,
            @NotNull String literal,
            @NotNull String value,
            boolean attribute) {

        /** An element span, which is what the great majority of placeholders are. */
        public ElementPlaceholder(
                final @NotNull String name, final @NotNull String literal, final @NotNull String value) {
            this(name, literal, value, false);
        }
    }

    /**
     * Matches an element whose text contains at least one placeholder, capturing the name and the entire
     * text between the tags.
     * <p>
     * The whole text, rather than the placeholder alone, so that any whitespace the operator wrote and
     * any literal text they concatenated it with are both carried. The file is rendered before it is
     * parsed, so {@code <password>\n  ${ENV:PW}\n</password>} becomes an element whose text is the value
     * <em>with</em> that padding and the marshaller writes it back the same way; a search string built
     * from the bare value matches nothing, the restore gives up, and the credential goes out (EDG-882
     * review v02, R2-20). Concatenation is the same problem one step further out (R3-01).
     * <p>
     * {@code [^<>]} keeps a match inside one element: without it the text could run across a closing tag
     * and swallow a sibling.
     */
    private static final @NotNull Pattern ELEMENT_PLACEHOLDER =
            Pattern.compile("<([A-Za-z_][\\w.:-]*)>([^<>]*\\$\\{ENV:[^}]*}[^<>]*)</\\1>");

    /**
     * Matches an attribute whose value contains at least one placeholder.
     * <p>
     * Attributes were a documented blind spot until R3-01: a placeholder in one was resolved on the way
     * in and written out resolved, with nothing reported at all. There is nothing about an attribute that
     * makes it harder than an element — it is a named span with a value — so it is collected the same way
     * and restored the same way.
     */
    private static final @NotNull Pattern ATTRIBUTE_PLACEHOLDER =
            Pattern.compile("([A-Za-z_][\\w.:-]*)=\"([^\"]*\\$\\{ENV:[^}]*}[^\"]*)\"");

    private static final @NotNull Pattern XML_COMMENT = Pattern.compile("(?s)<!--.*?-->");

    private static final @NotNull Pattern ENV_PLACEHOLDER = Pattern.compile(ENV_VAR_PATTERN);

    /**
     * The {@code ${ENV:...}} placeholders of the file as it was written that
     * {@link #restorePlaceholders} is able to put back.
     * <p>
     * Every element text and every attribute value that contains a placeholder is collected, whether the
     * placeholder is the whole of it or concatenated with literal text. What is <em>not</em> collectable
     * is a span whose rendered form the marshaller will not reproduce verbatim; {@code restorePlaceholders}
     * decides that when it looks, and refuses to write a credential it cannot put back.
     * <p>
     * Comments are stripped first. A commented-out block is not part of the configuration and never
     * reaches the marshalled document, but its placeholders would otherwise be counted and make every
     * occurrence look ambiguous — commenting a bridge out is an ordinary thing for an operator to do.
     */
    public static @NotNull List<ElementPlaceholder> collectPlaceholders(final @NotNull String text) {
        final String withoutComments = XML_COMMENT.matcher(text).replaceAll("");
        final List<ElementPlaceholder> placeholders = new ArrayList<>();
        collectInto(placeholders, ELEMENT_PLACEHOLDER.matcher(withoutComments), false);
        collectInto(placeholders, ATTRIBUTE_PLACEHOLDER.matcher(withoutComments), true);
        return placeholders;
    }

    private static void collectInto(
            final @NotNull List<ElementPlaceholder> placeholders,
            final @NotNull Matcher matcher,
            final boolean attribute) {
        while (matcher.find()) {
            final String literal = matcher.group(2);
            final String value = renderOrNull(literal);
            // An empty rendered span gives the restore nothing to search for, and a span with an unset
            // variable never reaches the marshalled document at all -- the render of the whole file
            // throws first, which is the behaviour this must not change.
            if (value != null && !value.isEmpty()) {
                placeholders.add(new ElementPlaceholder(matcher.group(1), literal, value, attribute));
            }
        }
    }

    /**
     * Renders one span the way {@link #replaceEnvironmentVariablePlaceholders} renders the whole file, or
     * {@code null} when a variable it uses is unset.
     * <p>
     * Null rather than a throw: the whole-file render runs immediately after collection and throws on the
     * same variable with the message the operator needs. Throwing here would only move that failure
     * earlier and describe it worse.
     */
    private static @Nullable String renderOrNull(final @NotNull String span) {
        final StringBuilder rendered = new StringBuilder();
        final var matcher = ENV_PLACEHOLDER.matcher(span);
        while (matcher.find()) {
            final String replacement = getValue(matcher.group(1));
            if (replacement == null) {
                return null;
            }
            matcher.appendReplacement(rendered, escapeReplacement(replacement));
        }
        matcher.appendTail(rendered);
        return rendered.toString();
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
     * <b>Concatenation and attributes are covered</b> (EDG-882 review v03, R3-01). The unit collected is
     * the whole text of an element or the whole value of an attribute, so
     * {@code <host>edge-${ENV:SITE}</host>} and {@code path="${ENV:DIR}/certs"} are located and restored
     * the same way a bare placeholder is; there is no need to know where inside the span it sat.
     * <p>
     * <b>What remains genuinely undecidable</b> is a span the marshaller does not reproduce verbatim: it
     * normalises a typed field, or the element leaves the configuration. That is the {@code inDocument ==
     * 0} case below, and it no longer returns quietly. If the value is still somewhere in the document
     * and it is a credential, the write is <b>refused</b> rather than completed with the secret in it.
     * <p>
     * A placeholder inside a configuration fragment or an {@code <if>} block is collected, because both
     * are flattened into the text before this runs; the write-back cannot reproduce the fragment
     * structure itself, which is a separate and pre-existing limitation of the round trip.
     *
     * @param placeholders what {@link #collectPlaceholders(String)} found in the file as it was written
     * @throws UnrecoverableException when a credential cannot be kept out of the document being written
     */
    public static @NotNull String restorePlaceholders(
            final @NotNull String renderedXml, final @NotNull List<ElementPlaceholder> placeholders) {
        if (placeholders.isEmpty()) {
            return renderedXml;
        }
        // Grouped by the span the placeholder occupied and the value it renders to. Two placeholders that
        // share both are indistinguishable in the marshalled document; a group whose literal is not
        // unique is left alone rather than guessed at.
        final Map<String, List<ElementPlaceholder>> bySpanAndValue = new LinkedHashMap<>();
        for (final ElementPlaceholder placeholder : placeholders) {
            bySpanAndValue
                    .computeIfAbsent(
                            // NUL, as the original key did -- it cannot occur in an XML name or in text
                            // the parser accepted, so the three parts can never run together ambiguously.
                            // Written as the escape rather than as a raw byte: the byte was in the source
                            // literally, which made file(1) and grep treat this whole file as binary and
                            // silently skip it (EDG-882 review v03, R3-01).
                            placeholder.attribute() + "@" + placeholder.name() + '\0' + placeholder.value(),
                            key -> new ArrayList<>())
                    .add(placeholder);
        }

        var result = renderedXml;
        for (final List<ElementPlaceholder> group : bySpanAndValue.values()) {
            final ElementPlaceholder first = group.get(0);
            final String literal = first.literal();
            final String rendered = renderedSpan(first);
            final int inDocument = countOccurrences(result, rendered);

            if (group.stream().anyMatch(placeholder -> !placeholder.literal().equals(literal))) {
                result = giveUpOn(
                        result,
                        first,
                        rendered,
                        "two different placeholders fill it with the same value, so neither of them can be told"
                                + " from the other");
                continue;
            }
            if (inDocument == 0) {
                // The span is not in the document as this expects it. Either it left the configuration --
                // nothing to restore, and nothing to leak -- or the marshaller wrote it differently,
                // normalising a typed field (TRUE to true, 01883 to 1883) or anything else not predicted
                // here.
                //
                // Telling those two apart is the whole point, because the second is a leak: the value is in
                // the document and the anchor cannot find it to take it out. So ask the one question that
                // separates them, which is whether the value is still there at all. If it is, and it is a
                // credential, refuse the write. Returning a document known to contain a secret the operator
                // chose to keep out of it is not a decision this method gets to make (R3-01).
                // Both forms, because the question is "is the secret in this document", not "is this exact
                // string": a value the marshaller escaped is the same disclosure spelled differently. It is
                // not reachable through ${ENV:...} today -- a value carrying an XML metacharacter is
                // spliced in raw and makes the file unparseable long before this -- but a security check
                // that only looks for one spelling is the kind that stops being true quietly.
                if (result.contains(first.value()) || result.contains(escapeXmlText(first.value()))) {
                    if (isSecretElement(first.name())) {
                        log.error(
                                "The '{}' {} holds a credential supplied through '{}', and that value is in the"
                                        + " configuration being written in a form this cannot locate, so the"
                                        + " placeholder cannot be put back. The write has been refused: config.xml is"
                                        + " unchanged, and this node keeps running on the value it already holds.",
                                first.name(),
                                first.attribute() ? "attribute" : "element",
                                literal);
                        throw new UnrecoverableException(false);
                    }
                    log.error(
                            "The '{}' {} that '{}' filled is in the configuration being written in a form this cannot"
                                    + " locate, so its value is written out and the variable reference is lost; put it"
                                    + " back by hand.",
                            first.name(),
                            first.attribute() ? "attribute" : "element",
                            literal);
                    continue;
                }
                // The value is nowhere in the document, so nothing leaked. Still an error, because the two
                // readings of that are "the element left the configuration", which costs nothing, and "the
                // marshaller normalised it", which silently costs the operator their variable reference --
                // and this cannot tell them apart.
                log.error(
                        "The '{}' {} that '{}' filled is not in the configuration being written, so the placeholder"
                                + " cannot be restored. If it was not removed, check the file for a value that should"
                                + " have stayed a variable reference.",
                        first.name(),
                        first.attribute() ? "attribute" : "element",
                        literal);
                continue;
            }
            if (inDocument != group.size()) {
                result = giveUpOn(
                        result,
                        first,
                        rendered,
                        "'" + literal + "' fills " + group.size() + " of them but the configuration being written"
                                + " has " + inDocument + ", so which ones came from the variable cannot be told");
                continue;
            }
            result = result.replace(rendered, literalSpan(first));
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
            final @NotNull String rendered,
            final @NotNull String reason) {
        if (!isSecretElement(placeholder.name())) {
            log.error(
                    "The '<{}>' placeholder cannot be restored when the configuration file is written, because"
                            + " {}. Its value is written out instead and the variable reference is lost; put it"
                            + " back by hand once the ambiguity is resolved.",
                    placeholder.name(),
                    reason);
            return document;
        }
        log.error(
                "The '<{}>' element holds a credential and its placeholder cannot be restored, because {}."
                        + " Rather than write the credential to config.xml it is written as '{}', which no"
                        + " variable sets: this node keeps running on the value it already holds, and the next"
                        + " start will stop and name that variable. Restore the intended '${ENV:...}'"
                        + " reference in config.xml -- the previous file is in the rolling backup beside it.",
                placeholder.name(),
                reason,
                UNRESTORED_SECRET);
        return document.replace(rendered, poisonedSpan(placeholder));
    }

    /** The span as the marshaller will have written it: what the restore searches the document for. */
    private static @NotNull String renderedSpan(final @NotNull ElementPlaceholder placeholder) {
        return placeholder.attribute()
                ? placeholder.name() + "=\"" + escapeXmlAttribute(placeholder.value()) + "\""
                : "<" + placeholder.name() + ">" + escapeXmlText(placeholder.value()) + "</" + placeholder.name() + ">";
    }

    /** The same span with the operator's placeholders back in it: what the restore writes. */
    private static @NotNull String literalSpan(final @NotNull ElementPlaceholder placeholder) {
        return placeholder.attribute()
                ? placeholder.name() + "=\"" + placeholder.literal() + "\""
                : "<" + placeholder.name() + ">" + placeholder.literal() + "</" + placeholder.name() + ">";
    }

    /** The same span with {@link #UNRESTORED_SECRET} in place of a credential that cannot be restored. */
    private static @NotNull String poisonedSpan(final @NotNull ElementPlaceholder placeholder) {
        return placeholder.attribute()
                ? placeholder.name() + "=\"" + UNRESTORED_SECRET + "\""
                : "<" + placeholder.name() + ">" + UNRESTORED_SECRET + "</" + placeholder.name() + ">";
    }

    /**
     * The characters a marshaller escapes inside an attribute value. Differs from element text by the
     * quote, which would otherwise close the attribute.
     */
    private static @NotNull String escapeXmlAttribute(final @NotNull String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace("\"", "&quot;");
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
