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

import static java.util.Objects.requireNonNullElse;

import com.google.common.annotations.VisibleForTesting;
import com.hivemq.exceptions.UnrecoverableException;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

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

    private static final @NotNull Pattern ENV_PLACEHOLDER = Pattern.compile(ENV_VAR_PATTERN);

    /** The token every placeholder starts with, used for the cheap "is there one in here at all" test. */
    private static final @NotNull String ENV_TOKEN = "${ENV:";

    /**
     * What {@link #collectPlaceholders(String)} found: the spans that can be restored, and whether it was
     * able to account for every placeholder in the file.
     *
     * @param placeholders      one entry per element text or attribute value that holds a placeholder
     * @param unaccountedTokens placeholders present in the file that the walk below never saw. Always
     *                          zero for a well-formed configuration; anything else means the file holds a
     *                          placeholder somewhere this cannot reason about, and
     *                          {@link #restorePlaceholders} refuses the write rather than guess whether a
     *                          credential is about to go out with it (EDG-882 review v03, R3-08). Counted
     *                          per variable and never negative -- see {@link #collectPlaceholders}.
     */
    public record CollectedPlaceholders(@NotNull List<ElementPlaceholder> placeholders, int unaccountedTokens) {

        public static final @NotNull CollectedPlaceholders NONE = new CollectedPlaceholders(List.of(), 0);
    }

    /**
     * The {@code ${ENV:...}} placeholders of the file as it was written that
     * {@link #restorePlaceholders} is able to put back.
     * <p>
     * <b>Collected from a parsed document, not from the file's bytes.</b> The earlier version of this
     * matched elements and attributes with a regular expression and stored the source span exactly as the
     * operator typed it. That span is not what the marshaller writes, because the parser normalises text
     * on the way in and the restore compares against post-marshal output: a numeric character reference
     * standing in for an ordinary character, CRLF padding, or a value inside a CDATA section all
     * rendered to something the restore then failed to find, took the "not in the document" branch, and
     * let the credential through. CDATA was worse still — the pattern excluded angle brackets, so it
     * could not match one and nothing was collected at all. Parsing the file the same way the
     * configuration loader is about to parse it makes the collected value equal to the value JAXB will
     * hold, which is the only thing the restore can honestly anchor on (EDG-882 review v03, R3-08).
     * <p>
     * The literal kept is therefore the <em>parsed</em> text with the placeholders still in it, not the
     * original bytes: a character reference comes back as the character it denotes, and a CDATA section
     * comes back as its content. Both mean the same thing to the next parse, and {@link #literalSpan}
     * escapes whatever needs escaping on the way out.
     * <p>
     * Comments are skipped, because the walk only visits element text and attribute values. A
     * commented-out block is not part of the configuration and never reaches the marshalled document, but
     * its placeholders would otherwise make every occurrence look ambiguous — commenting a bridge out is
     * an ordinary thing for an operator to do. Their tokens are counted as seen, so they do not show up
     * as unaccounted either.
     * <p>
     * <b>Accounted for by variable, not by total.</b> The two counts measure different populations: the
     * file's own bytes are what the whole-file render resolves, and the parsed document is what this walk
     * can reach. A placeholder spelled with a character reference — {@code $&#123;ENV:PW}} — exists only
     * in the second, so subtracting one total from the other went <em>negative</em>, and a negative count
     * is not zero: it refused every subsequent write of a configuration that had nothing wrong with it
     * (EDG-882 review v04). Comparing per variable and keeping only the shortfall says what the guard
     * always meant — a placeholder the render resolved and this walk could not find — and it no longer
     * lets two different oddities cancel each other out into a clean bill of health.
     * <p>
     * A token visible only after parsing is not a disclosure risk in the first place: the render works on
     * the file's bytes, so it never resolves one, and the value it stands for never enters the document.
     */
    public static @NotNull CollectedPlaceholders collectPlaceholders(final @NotNull String text) {
        final int tokensInFile = countOccurrences(text, ENV_TOKEN);
        if (tokensInFile == 0) {
            return CollectedPlaceholders.NONE;
        }
        final Map<String, Integer> namedInFile = tokenCounts(text);
        // A token whose braces never close names no variable, so nothing can account for it. It is not
        // rendered either -- the whole-file render matches the same pattern this does -- but it is exactly
        // the shape this guard exists to be suspicious of, so it stays unaccounted rather than ignored.
        final int unnamedInFile = tokensInFile - total(namedInFile);
        final Document document = parseOrNull(text);
        if (document == null) {
            // Every token is unaccounted for, which refuses the next write-back rather than performing
            // one with no restoration at all.
            //
            // Usually this never matters: the content handed here is what the configuration loader is
            // about to unmarshal, so content this cannot parse fails there a moment later with the
            // parser's own message, and the placeholders of a configuration that was never accepted are
            // never used. What it does cover is the case where the two parsers disagree -- this one
            // refuses a DOCTYPE and turns off external access, so a file the loader would accept can
            // still be rejected here. Returning "no placeholders" for that would write every resolved
            // secret to disk in silence, which is the failure this whole mechanism exists to prevent.
            log.error("The configuration could not be parsed while locating its environment-variable"
                    + " placeholders, so writing the configuration back out will be refused until it can be."
                    + " Until then a REST change to any subsystem applies to the running node but cannot be"
                    + " persisted to config.xml.");
            return new CollectedPlaceholders(List.of(), tokensInFile);
        }
        final List<ElementPlaceholder> placeholders = new ArrayList<>();
        final Map<String, Integer> seen = new HashMap<>();
        visitValues(document, (name, value, attribute) -> collect(placeholders, seen, name, value, attribute), seen);
        int unaccounted = unnamedInFile;
        for (final Map.Entry<String, Integer> token : namedInFile.entrySet()) {
            unaccounted += Math.max(0, token.getValue() - seen.getOrDefault(token.getKey(), 0));
        }
        return new CollectedPlaceholders(placeholders, unaccounted);
    }

    /** How many times each variable is named by a placeholder in the given text. */
    private static @NotNull Map<String, Integer> tokenCounts(final @NotNull String text) {
        final Map<String, Integer> counts = new HashMap<>();
        final var matcher = ENV_PLACEHOLDER.matcher(text);
        while (matcher.find()) {
            counts.merge(matcher.group(1), 1, Integer::sum);
        }
        return counts;
    }

    private static int total(final @NotNull Map<String, Integer> counts) {
        int total = 0;
        for (final int count : counts.values()) {
            total += count;
        }
        return total;
    }

    /**
     * Parses the configuration content with external access turned off, or {@code null} when it is not
     * well formed.
     * <p>
     * This runs on a file that is about to be unmarshalled by the loader anyway, so it resolves nothing
     * the loader would not; the restrictions are here because a parser that reaches the network or the
     * file system on someone else's say-so is never wanted, not because this particular content is
     * suspect.
     */
    private static @Nullable Document parseOrNull(final @NotNull String text) {
        try {
            final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            setFeatureQuietly(factory, "http://apache.org/xml/features/disallow-doctype-decl", true);
            setFeatureQuietly(factory, "http://xml.org/sax/features/external-general-entities", false);
            setFeatureQuietly(factory, "http://xml.org/sax/features/external-parameter-entities", false);
            factory.setXIncludeAware(false);
            factory.setExpandEntityReferences(false);
            factory.setNamespaceAware(false);
            final DocumentBuilder builder = factory.newDocumentBuilder();
            builder.setErrorHandler(null);
            return builder.parse(new InputSource(new StringReader(text)));
        } catch (final ParserConfigurationException | SAXException | IOException e) {
            return null;
        }
    }

    private static void setFeatureQuietly(
            final @NotNull DocumentBuilderFactory factory, final @NotNull String feature, final boolean value) {
        try {
            factory.setFeature(feature, value);
        } catch (final ParserConfigurationException ignored) {
            // Not every parser implementation knows every feature name; secure processing is already on.
        }
    }

    /** What one element text or attribute value of a parsed document is, to the two passes that read them. */
    private interface ValueVisitor {

        void visit(@NotNull String name, @NotNull String value, boolean attribute);
    }

    /**
     * Visits every element text and attribute value of a parsed document.
     * <p>
     * Values, never markup. Both readers of a configuration document want the same thing — what the
     * unmarshaller would see — and neither wants element names, which is the whole of
     * {@link #refuseIfACredentialSurvived}'s defect: a document containing {@code <admin-api>} read as a
     * document containing the password {@code admin} (EDG-882 review v04).
     * <p>
     * {@code seenInComments} counts the placeholder tokens held in comments and processing instructions,
     * for the collector, which needs {@code unaccountedTokens} to mean "somewhere this walk cannot reach"
     * rather than "somewhere this walk chose not to restore". Null for callers that only want the values.
     */
    private static void visitValues(
            final @NotNull Node node,
            final @NotNull ValueVisitor visitor,
            final @Nullable Map<String, Integer> seenInComments) {
        if (node.getNodeType() == Node.ELEMENT_NODE) {
            final Element element = (Element) node;
            final NamedNodeMap attributes = element.getAttributes();
            for (int i = 0; i < attributes.getLength(); i++) {
                final Node attribute = attributes.item(i);
                visitor.visit(attribute.getNodeName(), attribute.getNodeValue(), true);
            }
            // Only the direct text children, joined: that is what the element's value is to the
            // unmarshaller, and what the marshaller writes back for it.
            final StringBuilder text = new StringBuilder();
            final NodeList children = element.getChildNodes();
            for (int i = 0; i < children.getLength(); i++) {
                final Node child = children.item(i);
                if (child.getNodeType() == Node.TEXT_NODE || child.getNodeType() == Node.CDATA_SECTION_NODE) {
                    text.append(child.getNodeValue());
                }
            }
            visitor.visit(element.getTagName(), text.toString(), false);
        } else if (seenInComments != null
                && (node.getNodeType() == Node.COMMENT_NODE
                        || node.getNodeType() == Node.PROCESSING_INSTRUCTION_NODE)) {
            // Seen, deliberately not restorable: neither reaches the marshalled document.
            tokenCounts(requireNonNullElse(node.getNodeValue(), ""))
                    .forEach((name, count) -> seenInComments.merge(name, count, Integer::sum));
        }
        final NodeList children = node.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            visitValues(children.item(i), visitor, seenInComments);
        }
    }

    private static void collect(
            final @NotNull List<ElementPlaceholder> placeholders,
            final @NotNull Map<String, Integer> seen,
            final @NotNull String name,
            final @NotNull String literal,
            final boolean attribute) {
        final Map<String, Integer> tokens = tokenCounts(literal);
        if (tokens.isEmpty()) {
            return;
        }
        tokens.forEach((variable, count) -> seen.merge(variable, count, Integer::sum));
        final String value = renderOrNull(literal);
        if (value == null) {
            // A span with an unset variable never reaches the marshalled document at all: the render of
            // the whole file throws on the same variable moments from now, with the message the operator
            // needs. Saying anything here would only precede it and describe it worse.
            return;
        }
        if (value.isEmpty()) {
            // An empty rendered span gives the restore nothing to search for, so the placeholder is lost
            // the next time the configuration is written -- silently, until now. Every other way of losing
            // one is reported; this one was the exception, and it is the only one the operator can still
            // do something about before it happens (EDG-882 review v04).
            log.warn(
                    "The '{}' {} is filled by '{}', which resolves to an empty value. There is nothing in the"
                            + " written configuration for the placeholder to be put back into, so the next"
                            + " configuration write will leave that {} empty and the variable reference will be"
                            + " lost. Set the variable to a value if the reference is meant to survive.",
                    name,
                    attribute ? "attribute" : "element",
                    literal,
                    attribute ? "attribute" : "element");
            return;
        }
        // Accounted for either way: they were found, they are simply not restorable, which is not the same
        // as unseen.
        placeholders.add(new ElementPlaceholder(name, literal, value, attribute));
    }

    /**
     * The variables in the given configuration text whose value cannot be put into it as text.
     * <p>
     * {@link #replaceEnvironmentVariablePlaceholders} splices a value into the file before anything parses
     * it, escaping only what the regular expression needs, so a value carrying {@code <} or {@code &}
     * produces a document that is not well formed and the node stops at a parser error about a line the
     * operator did not write -- a password of {@code p@ss&word} cannot be supplied this way at all, and
     * nothing said so. Splicing is also what makes a fragment able to bring in whole elements, so the
     * behaviour is not changed here; the failure is explained instead, on the path where it happens.
     * <p>
     * Names only. The values are the reason this exists and never go anywhere near a log.
     */
    public static @NotNull List<String> variablesWhoseValueIsNotText(final @NotNull String text) {
        final List<String> names = new ArrayList<>();
        final var matcher = ENV_PLACEHOLDER.matcher(text);
        while (matcher.find()) {
            final String name = matcher.group(1);
            final String value = getValue(name);
            if (value != null && (value.contains("<") || value.contains("&")) && !names.contains(name)) {
                names.add(name);
            }
        }
        return names;
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
        final ParsedOnce parses = new ParsedOnce();
        for (final List<ElementPlaceholder> group : bySpanAndValue.values()) {
            final ElementPlaceholder first = group.get(0);
            final String literal = first.literal();
            final Pattern rendered = renderedSpan(first);
            final int inDocument = countMatches(result, rendered);

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
                // Of the document's values, not of its text: searching the serialised XML answered "the
                // secret is still here" for a password of 'admin' in any file with an <admin-api> element,
                // and refused every write from then on (EDG-882 review v04). Parsing also settles the
                // escaping question the previous version handled by searching for two spellings.
                if (isInTheDocumentAsAValue(parses, result, first.value())) {
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
            result = rendered.matcher(result).replaceAll(literalSpan(first));
        }
        return refuseIfACredentialSurvived(result, placeholders, parses);
    }

    /**
     * Restores the placeholders of a whole configuration file, refusing the write when the collector
     * could not account for every one of them.
     * <p>
     * The branch-by-branch reasoning in {@link #restorePlaceholders(String, List)} can only speak for the
     * spans it was given. A token the collector never saw has no branch at all — and "no branch" was
     * exactly how the character-reference, CRLF and CDATA cases got a credential onto disk before the
     * collector was rewritten. So the count is checked here rather than trusted: if the file held a
     * placeholder the walk did not reach, this cannot say whether the value it stands for is in the
     * document, and it refuses instead of assuming (EDG-882 review v03, R3-08).
     *
     * @throws UnrecoverableException when a credential cannot be kept out of the document being written
     */
    public static @NotNull String restorePlaceholders(
            final @NotNull String renderedXml, final @NotNull CollectedPlaceholders collected) {
        if (collected.unaccountedTokens() != 0) {
            log.error(
                    "The configuration file holds {} '{}...' placeholder(s) that could not be located in its"
                            + " structure, so this cannot tell whether writing the configuration back out would"
                            + " put the value of one on disk. The write has been refused: config.xml is"
                            + " unchanged, and this node keeps running on the configuration it already holds.",
                    collected.unaccountedTokens(),
                    ENV_TOKEN);
            throw new UnrecoverableException(false);
        }
        return restorePlaceholders(renderedXml, collected.placeholders());
    }

    /**
     * The parsed form of the document being restored, kept for as long as the document has not changed.
     * <p>
     * Two questions on the write path are asked of the document's values rather than its text — whether a
     * span the restore could not locate is still in it, and the final sweep for a surviving credential —
     * and each one parsed the whole document again. A configuration whose spans the marshaller normalises
     * therefore paid one parse per span, on every write (EDG-882 review v04). They are the same document
     * and the same parse; only the questions differ.
     * <p>
     * Invalidated by identity, deliberately. Every rewrite in the loop produces a new {@code String}, so
     * {@code !=} is exactly "the document has changed"; two equal instances would only cost a parse that
     * was not needed, never return an answer about a document that no longer exists.
     * <p>
     * What it is not is a cheaper test. Deciding from the text — "the value does not appear anywhere, so
     * skip the parse" — is the shortcut that let a numeric character reference carry a credential past
     * this check once already, and no amount of it being faster makes it true.
     */
    private static final class ParsedOnce {

        private @Nullable String source;
        private @Nullable Document parsed;

        private @Nullable Document of(final @NotNull String document) {
            if (source != document) {
                source = document;
                parsed = parseOrNull(document);
            }
            return parsed;
        }
    }

    /**
     * Whether a value is in the document as data rather than as markup — the question both the
     * unlocatable-span branch and {@link #refuseIfACredentialSurvived} ask of a finished document.
     * <p>
     * A document that cannot be parsed answers yes: this cannot see into it, and the caller's safe
     * reading of "cannot tell" is the same as "it is in there".
     */
    private static boolean isInTheDocumentAsAValue(
            final @NotNull ParsedOnce parses, final @NotNull String document, final @NotNull String value) {
        final Document parsed = parses.of(document);
        if (parsed == null) {
            return true;
        }
        final boolean[] found = {false};
        visitValues(parsed, (name, spanValue, attribute) -> found[0] |= holdsValue(name, spanValue, value), null);
        return found[0];
    }

    /**
     * Whether one element text or attribute value of the finished document carries {@code value} — the
     * broad question, asked when the span the restore was anchored to has gone missing.
     * <p>
     * Equal, or inside a credential-bearing span. A value that <em>is</em> the secret is the secret
     * written out wherever it sits, and when the anchor has vanished there is no telling where the
     * marshaller put it; a value that merely contains it is a disclosure only where the credential
     * belongs, because anywhere else it is the operator's own text with the secret inside it, and
     * refusing on that is how {@code <port>1883</port>} came to reject a password of {@code 1}.
     */
    private static boolean holdsValue(
            final @NotNull String spanName, final @NotNull String spanValue, final @NotNull String value) {
        return !spanValue.isEmpty() && (spanValue.equals(value) || isInACredentialSpan(spanName, spanValue, value));
    }

    /**
     * Whether a credential-bearing span is still holding a credential — the narrow question, and the only
     * shape a restore that believes it succeeded can actually have left behind.
     * <p>
     * The restore replaces a span with its own literal; it cannot move a value into a span it never
     * touched. So a credential appearing in some other element of the finished document is the operator's
     * own text — a username that happens to equal the password, a host name a credential variable resolves
     * to — and refusing the write over it means that configuration can never be persisted again while the
     * coincidence lasts (EDG-882 review v04). What is left is the span where the credential belongs, still
     * holding the value instead of the placeholder, whole or concatenated.
     * <p>
     * The residual is an operator who writes the same credential literally in a second credential element.
     * That one is refused, the message names both spans, and the remedy — stop writing the credential in
     * plain text — is the one worth having.
     */
    private static boolean isInACredentialSpan(
            final @NotNull String spanName, final @NotNull String spanValue, final @NotNull String value) {
        return !spanValue.isEmpty() && isSecretElement(spanName) && spanValue.contains(value);
    }

    /**
     * The last word on whether a credential is going to disk, asked of the finished document rather than
     * of the branch that produced it.
     * <p>
     * Every branch above that can end with a secret still in the document refuses on its own, and each of
     * those refusals is tested. This exists because the branch that gets it wrong is by definition the one
     * nobody thought of: it does not reason about how the document was built, it just looks for the values
     * that were supposed to have been taken out of it. A secret that is still there when the restore
     * believes it is finished is a bug in the restore, and shipping the document anyway would make it a
     * disclosure instead (EDG-882 review v03, R3-08).
     * <p>
     * <b>Asked of the document's values, not of its text.</b> The first version searched the serialised
     * XML with {@code String.contains}, which reads markup as content: a password of {@code admin} was
     * "still present" because the file has an {@code <admin-api>} element, and every configuration write
     * was refused from then on — the node stayed up and silently stopped being able to persist anything.
     * Short values ({@code a}, {@code 1}, {@code true}) made it near-certain. So the finished document is
     * parsed and only element text and attribute values are examined, which is where a credential would
     * have to be to be disclosed (EDG-882 review v04).
     * <p>
     * <b>Of the spans where a credential belongs, not of every value in the file.</b> The restore replaces
     * a span with its own literal and cannot move a value into a span it never touched, so a credential
     * still sitting in {@code <password>} is a bug in the restore and the same string sitting in
     * {@code <username>} or {@code <host>} is the operator's own text. Asking the broad question refused
     * every write for a bridge whose password happened to equal its own host name, which is an ordinary
     * enough coincidence and left that node unable to persist a configuration change ever again (EDG-882
     * review v04). Inside a credential-bearing span the question stays broad — containment, not equality —
     * because {@code <password>prefix-s3cr3t</password>} is the concatenation case and is exactly what a
     * half-done restore looks like.
     * <p>
     * The false positive it can still have is an operator who writes the same credential literally in a
     * second credential element. Refusing that write is the right side of the trade: the remedy is to stop
     * writing the credential in plain text, and the message names both spans.
     */
    private static @NotNull String refuseIfACredentialSurvived(
            final @NotNull String document,
            final @NotNull List<ElementPlaceholder> placeholders,
            final @NotNull ParsedOnce parses) {
        final List<ElementPlaceholder> secrets = placeholders.stream()
                .filter(placeholder -> isSecretElement(placeholder.name()))
                .toList();
        if (secrets.isEmpty()) {
            return document;
        }
        final Document parsed = parses.of(document);
        if (parsed == null) {
            // The document about to be written cannot be read back, so this cannot say whether a credential
            // is in it. Refusing keeps the previous config.xml, which parses.
            log.error("The configuration being written could not be parsed to check that no credential"
                    + " survived in it, so the write has been refused: config.xml is unchanged, and this node"
                    + " keeps running on the configuration it already holds.");
            throw new UnrecoverableException(false);
        }
        visitValues(
                parsed,
                (name, value, attribute) -> {
                    for (final ElementPlaceholder secret : secrets) {
                        if (isInACredentialSpan(name, value, secret.value())) {
                            log.error(
                                    "The value supplied to the '{}' {} through '{}' is still present in the"
                                            + " configuration being written, in the '{}' {}, after its placeholder"
                                            + " was restored -- so writing it would put a credential on disk. The"
                                            + " write has been refused: config.xml is unchanged, and this node keeps"
                                            + " running on the value it already holds. If that value is also written"
                                            + " literally somewhere else in the file, remove it there.",
                                    secret.name(),
                                    secret.attribute() ? "attribute" : "element",
                                    secret.literal(),
                                    name,
                                    attribute ? "attribute" : "element");
                            throw new UnrecoverableException(false);
                        }
                    }
                },
                null);
        return document;
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
            final @NotNull Pattern rendered,
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
        return rendered.matcher(document).replaceAll(poisonedSpan(placeholder));
    }

    /**
     * Whatever the marshaller may have written between an element's name and the {@code >} that ends its
     * start tag: nothing, or attributes.
     * <p>
     * Quoted values are stepped over rather than excluded, so an attribute holding a {@code >} does not
     * end the tag early. Everything outside the quotes must be free of angle brackets, which is what keeps
     * the pattern inside one start tag.
     */
    private static final @NotNull String START_TAG_REST = "(\\s(?:[^<>\"]|\"[^\"]*\")*)?";

    /**
     * The span as the marshaller will have written it: what the restore searches the document for.
     * <p>
     * <b>A pattern rather than a string, because an element may carry attributes.</b> The span was built
     * as {@code <name>value</name>} and compared literally, so an element written as
     * {@code <name attr="x">value</name>} was never found: the restore took the "not in the document"
     * branch, which refuses the write outright when the element holds a credential and writes the value
     * out when it does not. Nothing in today's schema puts an attribute on an element that also holds
     * text -- the adapter and module configurations, which are the only arbitrary XML in the file, are
     * unmarshalled into maps and lose their attributes long before this -- so it is a trap rather than a
     * live defect, and it is one the next attribute added to a text-bearing element would spring silently
     * (EDG-882 review v04). The attributes are matched, kept in a group, and written back unchanged.
     */
    private static @NotNull Pattern renderedSpan(final @NotNull ElementPlaceholder placeholder) {
        if (placeholder.attribute()) {
            return Pattern.compile(
                    Pattern.quote(placeholder.name() + "=\"" + escapeXmlAttribute(placeholder.value()) + "\""));
        }
        return Pattern.compile("<" + Pattern.quote(placeholder.name()) + START_TAG_REST + ">"
                + Pattern.quote(escapeXmlText(placeholder.value()))
                + "</" + Pattern.quote(placeholder.name()) + ">");
    }

    /** How many spans of that shape the document holds. */
    private static int countMatches(final @NotNull String text, final @NotNull Pattern pattern) {
        final var matcher = pattern.matcher(text);
        int count = 0;
        while (matcher.find()) {
            count++;
        }
        return count;
    }

    /**
     * The same span with the operator's placeholders back in it: what the restore writes.
     * <p>
     * Escaped on the way out, because the literal is now the <em>parsed</em> text rather than the
     * original bytes: an operator who wrote an escaped ampersand has a literal holding a bare one, and
     * putting that back raw would produce a config.xml that does not parse on the next start. A
     * placeholder itself carries none of the escaped characters, so it passes through untouched.
     */
    private static @NotNull String literalSpan(final @NotNull ElementPlaceholder placeholder) {
        if (placeholder.attribute()) {
            return Matcher.quoteReplacement(
                    placeholder.name() + "=\"" + escapeXmlAttribute(placeholder.literal()) + "\"");
        }
        // $1 is whatever stood between the element's name and the end of its start tag, put back as it
        // was: the restore replaces the value an element holds, not the element.
        return "<" + placeholder.name() + "$1>"
                + Matcher.quoteReplacement(escapeXmlText(placeholder.literal()))
                + "</" + placeholder.name() + ">";
    }

    /** The same span with {@link #UNRESTORED_SECRET} in place of a credential that cannot be restored. */
    private static @NotNull String poisonedSpan(final @NotNull ElementPlaceholder placeholder) {
        if (placeholder.attribute()) {
            return Matcher.quoteReplacement(placeholder.name() + "=\"" + UNRESTORED_SECRET + "\"");
        }
        return "<" + placeholder.name() + "$1>" + Matcher.quoteReplacement(UNRESTORED_SECRET) + "</"
                + placeholder.name() + ">";
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
