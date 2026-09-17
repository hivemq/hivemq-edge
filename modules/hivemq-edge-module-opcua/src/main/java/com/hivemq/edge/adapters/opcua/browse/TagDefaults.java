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
package com.hivemq.edge.adapters.opcua.browse;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.jetbrains.annotations.NotNull;

/**
 * The defaults a browsed variable is offered with: a tag name and the two topics, all derived from the node's
 * browse path. Pure string functions; the only state is the collision suffix of {@link #deduplicate}.
 */
final class TagDefaults {

    private TagDefaults() {}

    /**
     * Tag name for a path: every segment sanitised and joined with dashes, so the full path guarantees
     * uniqueness — {@code /Aliases/FindAlias/InputArguments} → {@code aliases-findalias-inputarguments}.
     */
    static @NotNull String tagName(final @NotNull String path) {
        final String stripped = path.startsWith("/") ? path.substring(1) : path;
        if (stripped.isEmpty()) {
            return "";
        }
        final String[] segments = stripped.split("/", -1);
        final StringBuilder sb = new StringBuilder();
        for (int i = 0; i < segments.length; i++) {
            if (i > 0) {
                sb.append('-');
            }
            sb.append(sanitize(segments[i]));
        }
        return sb.toString();
    }

    /** One tag name per variable, in the order given, with collisions suffixed {@code -2}, {@code -3}, ... */
    static @NotNull List<String> tagNames(final @NotNull List<DiscoveredVariable> variables) {
        final List<String> baseDefaults = new ArrayList<>(variables.size());
        for (final DiscoveredVariable var : variables) {
            baseDefaults.add(tagName(var.path()));
        }
        return deduplicate(baseDefaults);
    }

    /**
     * Makes a list of defaults unique in order: the first occurrence keeps its name, later ones get a numeric
     * suffix — {@code name}, {@code name-2}, {@code name-3}. Nodes sharing a browse path (Prosys simulation
     * instances) are the reason this exists.
     */
    static @NotNull List<String> deduplicate(final @NotNull List<String> baseDefaults) {
        final List<String> result = new ArrayList<>(baseDefaults.size());
        final Map<String, Integer> seen = new HashMap<>();
        for (final String base : baseDefaults) {
            final int count = seen.merge(base, 1, Integer::sum);
            result.add(count == 1 ? base : base + "-" + count);
        }
        return result;
    }

    static @NotNull String northboundTopic(final @NotNull String adapterId, final @NotNull String path) {
        return adapterId + "/" + sanitizePath(path);
    }

    static @NotNull String southboundTopic(final @NotNull String adapterId, final @NotNull String path) {
        return adapterId + "/write/" + sanitizePath(path);
    }

    /**
     * Produces a kebab-case-safe identifier: lowercases, replaces runs of non-alphanumeric
     * characters with a single dash, strips leading/trailing dashes. Single-pass character
     * walk — no regex allocation or intermediate strings. Equivalent to the three-regex
     * formulation previously used, but measurably faster in the Phase 2 hot path.
     */
    static @NotNull String sanitize(final @NotNull String input) {
        final int len = input.length();
        final StringBuilder sb = new StringBuilder(len);
        boolean lastWasDash = false;
        for (int i = 0; i < len; i++) {
            final char lower = Character.toLowerCase(input.charAt(i));
            if ((lower >= 'a' && lower <= 'z') || (lower >= '0' && lower <= '9')) {
                sb.append(lower);
                lastWasDash = false;
            } else if (!lastWasDash && sb.length() > 0) {
                // Collapse runs of non-alphanumeric characters and drop leading dashes.
                sb.append('-');
                lastWasDash = true;
            }
        }
        // Strip trailing dash.
        if (sb.length() > 0 && sb.charAt(sb.length() - 1) == '-') {
            sb.setLength(sb.length() - 1);
        }
        return sb.toString();
    }

    /** Every segment of the path sanitised, joined with slashes, leading slash dropped. */
    static @NotNull String sanitizePath(final @NotNull String path) {
        final String stripped = path.startsWith("/") ? path.substring(1) : path;
        if (stripped.isEmpty()) {
            return "";
        }
        final String[] segments = stripped.split("/", -1);
        final StringBuilder sb = new StringBuilder();
        for (int i = 0; i < segments.length; i++) {
            if (i > 0) {
                sb.append('/');
            }
            sb.append(sanitize(segments[i]));
        }
        return sb.toString();
    }
}
