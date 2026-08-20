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
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
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
     * Every {@code ${ENV:...}} placeholder in the given text, mapped to the value it renders to.
     * <p>
     * Taken from the file as it was written, before rendering, so that
     * {@link #restorePlaceholders(String, Map)} can put the placeholders back when the configuration is
     * written out again.
     */
    public static @NotNull Map<String, String> collectPlaceholders(final @NotNull String text) {
        final Map<String, String> placeholders = new LinkedHashMap<>();
        final var matcher = Pattern.compile(ENV_VAR_PATTERN).matcher(text);
        while (matcher.find()) {
            final var value = getValue(matcher.group(1));
            if (value != null && !value.isEmpty()) {
                placeholders.put(matcher.group(), value);
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
     * <b>Anchored and count-checked, because the reverse direction is ambiguous by nature.</b> A value
     * is only put back where it is the <em>entire</em> text of an element, and only when the document
     * contains exactly as many such elements as the original file had occurrences of that placeholder.
     * Anything else — a value that also appears somewhere the operator wrote literally, or two variables
     * that happen to resolve to the same string — is left rendered and reported, because rewriting an
     * element into a variable reference the operator did not ask for would be its own defect. That case
     * is rare and visible; the alternative is silent and permanent.
     *
     * @param placeholders the mapping from {@link #collectPlaceholders(String)}, taken from the file as
     *     it was written
     * @param originalText the file as it was written, used to count the placeholder's occurrences
     */
    public static @NotNull String restorePlaceholders(
            final @NotNull String renderedXml,
            final @NotNull Map<String, String> placeholders,
            final @NotNull String originalText) {
        if (placeholders.isEmpty()) {
            return renderedXml;
        }
        final Set<String> ambiguousValues = new HashSet<>();
        final Set<String> seenValues = new HashSet<>();
        for (final String value : placeholders.values()) {
            if (!seenValues.add(value)) {
                ambiguousValues.add(value);
            }
        }

        var result = renderedXml;
        for (final var placeholder : placeholders.entrySet()) {
            final String literal = placeholder.getKey();
            final String value = placeholder.getValue();
            if (ambiguousValues.contains(value)) {
                log.warn(
                        "Two placeholders in the configuration file resolve to the same value, so '{}' cannot be"
                                + " restored when the file is written; its value will be written out instead.",
                        literal);
                continue;
            }
            final String element = ">" + escapeXmlText(value) + "<";
            final int inDocument = countOccurrences(result, element);
            if (inDocument == 0) {
                continue;
            }
            if (inDocument != countOccurrences(originalText, literal)) {
                log.warn(
                        "The value of '{}' also appears in the configuration where it was not written as a"
                                + " placeholder, so the placeholder cannot be restored when the file is written;"
                                + " its value will be written out instead.",
                        literal);
                continue;
            }
            result = result.replace(element, ">" + literal + "<");
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
