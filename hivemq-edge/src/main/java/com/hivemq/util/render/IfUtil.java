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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * This is a temporary solution until we can get a proper template engine or a XML-parser based approach in place.
 */
public class IfUtil {
    private static final Logger log = LoggerFactory.getLogger(EnvVarUtil.class);

    private static final @NotNull String FRAGMENT_VAR_PATTERN = "\\$\\{IF:(.*?)}";

    public static final @NotNull List<String> SUPPORTED_ENVS = List.of(
            "HIVEMQ_HTTPS_ENABLED",
            "HIVEMQ_MQTT_ENABLED",
            "HIVEMQ_MQTTS_ENABLED",
            "HIVEMQ_MQTTS_CLIENTAUTH_ENABLED",
            "HIVEMQ_DATAHUB_ENABLED",
            "HIVEMQ_LDAP_SERVER2_ENABLED",
            "HIVEMQ_LDAP_SERVER3_ENABLED",
            "HIVEMQ_LDAP_TLS_TRUSTSTORE_ENABLED",
            "HIVEMQ_LDAP_ENABLED");

    /**
     * Get a Java system property or system environment variable with the specified name.
     * If a variable with the same name exists in both targets the Java system property is returned.
     *
     * @param name the name of the environment variable
     * @return the value of the environment variable with the specified name
     */
    public static @Nullable String getValue(final @NotNull String name) {
        //also check java properties if system variable is not found
        final String systemProperty = System.getProperty(name);
        if (systemProperty != null) {
            return systemProperty;
        }

        return System.getenv(name);
    }

    /**
     * Replaces if markers like '${IF:HIVEMQ_HTTPS_ENABLED}' depending on the existence of an env variable.
     *
     * @param text the text which contains placeholders (or not)
     * @return replacement result containing the actual string
     * @throws UnrecoverableException if a variable used in a placeholder is not set
     */
    public static @NotNull String replaceIfPlaceHolders(final @NotNull String text) {

        final List<String> activations = SUPPORTED_ENVS.stream()
                .map(envName -> Boolean.parseBoolean(getValue(envName)) ? envName : "!" + envName)
                .toList();

        final Matcher matcher = Pattern.compile(FRAGMENT_VAR_PATTERN)
                .matcher(text);

        // Find all tokens
        final List<IfToken> allTokens = new ArrayList<>();
        while (matcher.find()) {
            if (matcher.groupCount() < 1) {
                //this should never happen as we declared 1 groups in the ENV_VAR_PATTERN
                log.warn("Found unexpected if placeholder in config.xml");
                continue;
            }
            allTokens.add(new IfToken(matcher.group(1), matcher.start(), matcher.end()));
        }

        // Match opening and closing tags using a stack
        final Stack<IfToken> stack = new Stack<>();
        final List<IfTokenPair> pairs = new ArrayList<>();

        for (final var token : allTokens) {
            if (!stack.isEmpty() && stack.peek().getContent().equals(token.getContent())) {
                // This is a closing tag - matches the top of the stack
                final var opening = stack.pop();
                pairs.add(new IfTokenPair(opening, token));
            } else {
                // This is an opening tag
                stack.push(token);
            }
        }

        if (!stack.isEmpty()) {
            log.warn("Unmatched IF tags found in config");
        }

        // Create a boolean array to mark which characters to keep
        final var keep = new boolean[text.length()];
        Arrays.fill(keep, true); // Keep all by default

        // Sort pairs by span size descending (outermost first)
        pairs.sort((a, b) -> {
            final var spanA = a.closing.getStop() - a.opening.getStart();
            final var spanB = b.closing.getStop() - b.opening.getStart();
            return Integer.compare(spanB, spanA);
        });

        // Process each pair
        for (final var pair : pairs) {
            // Check if opening tag has been marked for removal (already inside a removed block)
            if (!keep[pair.opening.getStart()]) {
                continue; // Skip, already inside a removed block
            }

            if (activations.contains(pair.opening.getContent())) {
                // Active block - remove only the tags, keep content
                for (int i = pair.opening.getStart(); i < pair.opening.getStop(); i++) {
                    keep[i] = false;
                }
                for (int i = pair.closing.getStart(); i < pair.closing.getStop(); i++) {
                    keep[i] = false;
                }
            } else {
                // Inactive block - remove entire block including tags and content
                for (int i = pair.opening.getStart(); i < pair.closing.getStop(); i++) {
                    keep[i] = false;
                }
            }
        }

        // Build result string
        final var result = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            if (keep[i]) {
                result.append(text.charAt(i));
            }
        }

        return result.toString();
    }

    private static class IfToken{
        private final String content;
        private final int start;
        private final int stop;

        public IfToken(final String content, final int start, final int stop) {
            this.content = content;
            this.start = start;
            this.stop = stop;
        }

        public String getContent() {
            return content;
        }

        public int getStart() {
            return start;
        }

        public int getStop() {
            return stop;
        }

        @Override
        public String toString() {
            return "IfToken{" + "content='" + content + '\'' + ", start=" + start + ", stop=" + stop + '}';
        }
    }

    private static class IfTokenPair {
        private final IfToken opening;
        private final IfToken closing;

        public IfTokenPair(final IfToken opening, final IfToken closing) {
            this.opening = opening;
            this.closing = closing;
        }
    }
}
