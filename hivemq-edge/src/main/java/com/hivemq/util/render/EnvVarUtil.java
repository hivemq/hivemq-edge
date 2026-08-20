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

import com.hivemq.exceptions.UnrecoverableException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
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

    /** Matches a placeholder that is the whole text of an element, capturing the element name. */
    private static final @NotNull Pattern ELEMENT_PLACEHOLDER =
            Pattern.compile("<([A-Za-z_][\\w.:-]*)>\\s*(\\$\\{ENV:(.*?)})\\s*</\\1>");

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
            final var value = getValue(matcher.group(3));
            if (value != null && !value.isEmpty()) {
                placeholders.add(new ElementPlaceholder(matcher.group(1), matcher.group(2), value));
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
     * element into a variable reference nobody asked for. Anything ambiguous is left rendered and
     * reported, which is rare and visible; the alternative is silent and permanent.
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
            if (group.stream().anyMatch(placeholder -> !placeholder.literal().equals(literal))) {
                log.warn(
                        "Two different placeholders fill a '<{}>' element with the same value, so neither can be"
                                + " restored when the configuration file is written; the value will be written out"
                                + " instead.",
                        first.element());
                continue;
            }
            final String element =
                    "<" + first.element() + ">" + escapeXmlText(first.value()) + "</" + first.element() + ">";
            final int inDocument = countOccurrences(result, element);
            if (inDocument == 0) {
                log.warn(
                        "The '<{}>' element that '{}' filled is not in the configuration being written, so the"
                                + " placeholder cannot be restored. Check the file for a value that should have"
                                + " stayed a variable reference.",
                        first.element(),
                        literal);
                continue;
            }
            if (inDocument != group.size()) {
                log.warn(
                        "'{}' fills {} '<{}>' element(s) but the configuration being written has {}, so the"
                                + " placeholder cannot be restored; its value will be written out instead.",
                        literal,
                        group.size(),
                        first.element(),
                        inDocument);
                continue;
            }
            result = result.replace(element, "<" + first.element() + ">" + literal + "</" + first.element() + ">");
        }
        return result;
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

    /** The three characters a marshaller escapes inside element text. */
    private static @NotNull String escapeXmlText(final @NotNull String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
