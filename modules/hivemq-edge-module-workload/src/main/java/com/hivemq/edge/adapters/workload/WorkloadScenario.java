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
package com.hivemq.edge.adapters.workload;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The parsed, config-declared scenario the {@link WorkloadProtocolAdapter} plays autonomously on the real wall clock.
 * It unifies two testing axes on one QA-owned vehicle:
 * <ol>
 *   <li><b>Realistic streaming</b> — a per-node {@link Wave} data stream plus a wall-clock {@link Event} fault timeline
 *       (disconnect/reconnect/fault/recover). Config-once at boot; the adapter self-drives from there.</li>
 *   <li><b>Surgical fault injection</b> — a per-node {@link Fault} (verification and poll outcomes) plus a top-level
 *       {@code connect} behavior, giving the same deterministic, per-tag control the older chaos adapter offered, so
 *       state-machine transitions can be driven precisely by config.</li>
 * </ol>
 * Rides in the adapter's {@code <adapter-configuration>} as a JSON string under a {@code scenario} key, e.g.
 * <pre>{@code
 * <adapter-configuration><scenario>{
 *   "connect": "succeed",
 *   "tags": {
 *     "temperature": {"wave":"sine","periodMs":10000,"amplitude":50,"offset":50,"noise":2},
 *     "bad":         {"verify":"permanent","reason":"wrong type"},
 *     "flapping":    {"verify":"transient-then-success","transientCount":2},
 *     "*":           {"wave":"constant","value":42}
 *   },
 *   "timeline": [
 *     {"atMs":30000,"action":"disconnect"},
 *     {"atMs":45000,"action":"reconnect"},
 *     {"atMs":60000,"action":"fault","tag":"temperature"}
 *   ]
 * }</scenario></adapter-configuration>
 * }</pre>
 * Waves: {@code sine} (offset + amplitude·sin(2π·t/periodMs) + noise), {@code ramp} (offset + slopePerSec·t + noise),
 * {@code constant} (value + noise). Verify: {@code success} (default) | {@code permanent} | {@code transient} |
 * {@code transient-then-success} (fail {@code transientCount} times, then pass) | {@code no-response}. Poll:
 * {@code value} (default — emit the wave) | {@code error} | {@code no-response}. Connect: {@code succeed} (default) |
 * {@code fail} | {@code no-response} (hung) | {@code drop} (connect then immediately drop). Any parse failure → empty.
 */
public final class WorkloadScenario {

    private static final @NotNull Logger log = LoggerFactory.getLogger(WorkloadScenario.class);

    private static final @NotNull ObjectMapper MAPPER = new ObjectMapper();

    // Known values per enum-like field. An unknown value silently falls back to a well-behaved default — which turns a
    // config typo into a healthy device and a hanging test with no diagnostic. Each is WARNed once at parse time.
    private static final @NotNull Set<String> KNOWN_WAVES = Set.of("sine", "ramp", "constant", "counter");
    private static final @NotNull Set<String> KNOWN_VERIFY = Set.of(
            "success", "permanent", "transient", "transient-then-success", "success-then-permanent", "no-response");
    private static final @NotNull Set<String> KNOWN_POLL =
            Set.of("value", "error", "no-response", "garbage", "literal", "double");
    private static final @NotNull Set<String> KNOWN_WRITE = Set.of("success", "fail", "no-response");
    private static final @NotNull Set<String> KNOWN_CONNECT = Set.of("succeed", "fail", "no-response", "drop");
    private static final @NotNull Set<String> KNOWN_MISBEHAVE = Set.of(
            "",
            "start-no-ack",
            "stop-no-ack",
            "disconnect-no-ack",
            "double-start",
            "spurious-events",
            "verify-partial",
            "throw-in-poll",
            "fake-node-flood");
    private static final @NotNull Set<String> KNOWN_ACTIONS =
            Set.of("disconnect", "reconnect", "fault", "recover", "mute", "unmute");

    /** A per-node data stream. */
    public record Wave(
            @NotNull String kind,
            double periodMs,
            double amplitude,
            double offset,
            double slopePerSec,
            double constant,
            double noise) {}

    /** A per-node deterministic fault behavior (verification, poll, and write outcomes). */
    public record Fault(
            @NotNull String verify,
            @NotNull String poll,
            @NotNull String write,
            @NotNull String reason,
            int transientCount) {
        static @NotNull Fault healthy() {
            return new Fault("success", "value", "success", "", 1);
        }
    }

    /** A wall-clock scheduled behavior change. */
    public record Event(
            long atMs, @NotNull String action, @NotNull String tag) {}

    private final @NotNull Map<String, Wave> waves; // nodeId → wave;  "*" is the default
    private final @NotNull Map<String, Fault> faults; // nodeId → fault; "*" is the default
    private final @NotNull String connect;
    private final @NotNull String misbehave; // adversarial: a contract-violating adapter behavior ("" = well-behaved)
    private final @NotNull String controlDir; // deterministic test control channel dir ("" = disabled)
    private final @NotNull List<Event> timeline;

    private WorkloadScenario(
            final @NotNull Map<String, Wave> waves,
            final @NotNull Map<String, Fault> faults,
            final @NotNull String connect,
            final @NotNull String misbehave,
            final @NotNull String controlDir,
            final @NotNull List<Event> timeline) {
        this.waves = waves;
        this.faults = faults;
        this.connect = connect;
        this.misbehave = misbehave;
        this.controlDir = controlDir;
        this.timeline = timeline;
    }

    /** @return the absolute control-channel directory for deterministic test orchestration, or "" if disabled. */
    public @NotNull String controlDir() {
        return controlDir;
    }

    public static @NotNull WorkloadScenario parseOrEmpty(final @Nullable Object configValue) {
        final String json = extractJson(configValue);
        if (json == null || json.isBlank()) {
            return new WorkloadScenario(new HashMap<>(), new HashMap<>(), "succeed", "", "", List.of());
        }
        try {
            return parse(MAPPER.readTree(json));
        } catch (final Exception e) {
            // The empty-scenario fallback keeps the adapter runnable, but silence here turned a JSON typo into a
            // healthy default device with NO control channel and NO journal — a hanging test with zero diagnostic.
            // WL_SCENARIO_PARSE_FAILED mirrors WL_BUILD: together they prove "right jar" AND "right config".
            log.warn("WL_SCENARIO_PARSE_FAILED falling back to the empty scenario: {} — json: {}", e, json);
            return new WorkloadScenario(new HashMap<>(), new HashMap<>(), "succeed", "", "", List.of());
        }
    }

    /** @return the adversarial contract-violating behavior to inject, or "" for a well-behaved adapter. */
    public @NotNull String misbehave() {
        return misbehave;
    }

    private static @Nullable String extractJson(final @Nullable Object cv) {
        if (cv instanceof final Map<?, ?> m) {
            final Object s = m.get("scenario");
            return s == null ? null : String.valueOf(s);
        }
        return cv == null ? null : String.valueOf(cv);
    }

    private static @NotNull WorkloadScenario parse(final @NotNull JsonNode root) {
        final Map<String, Wave> waves = new HashMap<>();
        final Map<String, Fault> faults = new HashMap<>();
        final JsonNode tags = root.path("tags");
        if (tags.isObject()) {
            final Iterator<Map.Entry<String, JsonNode>> it = tags.fields();
            while (it.hasNext()) {
                final Map.Entry<String, JsonNode> e = it.next();
                waves.put(e.getKey(), wave(e.getValue()));
                faults.put(e.getKey(), fault(e.getValue()));
            }
        }
        final String connect = warnIfUnknown("connect", root.path("connect").asText("succeed"), KNOWN_CONNECT);
        final String misbehave =
                warnIfUnknown("misbehave", root.path("misbehave").asText(""), KNOWN_MISBEHAVE);
        final String controlDir = root.path("controlDir").asText("");
        final List<Event> tl = new ArrayList<>();
        final JsonNode timeline = root.path("timeline");
        if (timeline.isArray()) {
            for (final JsonNode ev : timeline) {
                tl.add(new Event(
                        ev.path("atMs").asLong(0),
                        warnIfUnknown("timeline.action", ev.path("action").asText(""), KNOWN_ACTIONS),
                        ev.path("tag").asText("")));
            }
        }
        return new WorkloadScenario(waves, faults, connect, misbehave, controlDir, tl);
    }

    /** Pass the value through unchanged (defaulting behavior is unaffected) but WARN when it is not a known one. */
    private static @NotNull String warnIfUnknown(
            final @NotNull String field, final @NotNull String value, final @NotNull Set<String> known) {
        if (!known.contains(value)) {
            log.warn(
                    "WL_SCENARIO unknown {} value '{}' (known: {}) — falls back to the default behavior",
                    field,
                    value,
                    known);
        }
        return value;
    }

    private static @NotNull Wave wave(final @NotNull JsonNode n) {
        return new Wave(
                warnIfUnknown("wave", n.path("wave").asText("constant"), KNOWN_WAVES),
                n.path("periodMs").asDouble(10_000),
                n.path("amplitude").asDouble(1),
                n.path("offset").asDouble(0),
                n.path("slopePerSec").asDouble(1),
                n.path("value").asDouble(0),
                n.path("noise").asDouble(0));
    }

    private static @NotNull Fault fault(final @NotNull JsonNode n) {
        return new Fault(
                warnIfUnknown("verify", n.path("verify").asText("success"), KNOWN_VERIFY),
                warnIfUnknown("poll", n.path("poll").asText("value"), KNOWN_POLL),
                warnIfUnknown("write", n.path("write").asText("success"), KNOWN_WRITE),
                n.path("reason").asText(""),
                n.path("transientCount").asInt(1));
    }

    public @NotNull String connect() {
        return connect;
    }

    /** @return the wave kind for {@code nodeId} (specific match, else {@code "*"}, else {@code "constant"}). */
    public @NotNull String waveKind(final @NotNull String nodeId) {
        final Wave w = waves.getOrDefault(nodeId, waves.get("*"));
        return w == null ? "constant" : w.kind();
    }

    public @NotNull List<Event> timeline() {
        return timeline;
    }

    /** @return the fault behavior for {@code nodeId} (specific match, else {@code "*"}, else healthy). */
    public @NotNull Fault faultFor(final @NotNull String nodeId) {
        final Fault f = faults.getOrDefault(nodeId, faults.get("*"));
        return f == null ? Fault.healthy() : f;
    }

    /**
     * @return the stream value for {@code nodeId} at {@code elapsedMs} since start; {@code rnd} supplies noise.
     */
    public double valueFor(final @NotNull String nodeId, final long elapsedMs, final @NotNull Random rnd) {
        final Wave w = waves.getOrDefault(nodeId, waves.get("*"));
        if (w == null) {
            return 0;
        }
        final double n = w.noise() > 0 ? rnd.nextGaussian() * w.noise() : 0;
        return switch (w.kind()) {
            case "sine" -> w.offset() + w.amplitude() * Math.sin(2 * Math.PI * elapsedMs / w.periodMs()) + n;
            case "ramp" -> w.offset() + w.slopePerSec() * (elapsedMs / 1000.0) + n;
            default -> w.constant() + n;
        };
    }
}
