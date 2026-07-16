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
package com.hivemq.protocols.v2.southbound;

import static java.nio.charset.StandardCharsets.UTF_8;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.util.concurrent.MoreExecutors;
import com.hivemq.mqtt.message.QoS;
import com.hivemq.mqtt.message.publish.PUBLISH;
import com.hivemq.mqtt.message.publish.PUBLISHFactory;
import com.hivemq.mqtt.services.InternalPublishService;
import com.hivemq.persistence.RetainedMessage;
import com.hivemq.persistence.retained.RetainedMessagePersistence;
import com.hivemq.persistence.util.FutureUtils;
import com.hivemq.protocols.v2.tag.SouthboundWriteOutcome;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The production {@link SouthboundWriteVerdictReporter}: each terminal verdict is published as a <b>retained</b>
 * QoS 1 JSON message on the command topic's {@code /result} sibling — deliberately <b>not</b> the command topic
 * itself, which would feed every verdict straight back into the command queue. The verdict is the correlation
 * reply and the observability record in one:
 * <pre>{@code
 * { "adapterId": "...", "tag": "...", "commandTopic": "...", "commandId": "...",
 *   "outcome": "SUCCEEDED" | "FAILED", "reason": null | "...",
 *   "deduplicated": false, "completedAtMillis": 1234567890123 }
 * }</pre>
 * Because it is retained, the latest verdict per command topic survives a restart — {@link #lastExecutedVerdict()}
 * recovers it from the retained store at construction, and the backlog uses it to recognize a crash-replayed,
 * already-executed command (see {@link SouthboundWriteVerdictReporter}).
 */
final class MqttSouthboundWriteVerdictReporter implements SouthboundWriteVerdictReporter {

    private static final @NotNull Logger log = LoggerFactory.getLogger(MqttSouthboundWriteVerdictReporter.class);

    /** The verdict topic suffix appended to the command topic. */
    static final @NotNull String RESULT_TOPIC_SUFFIX = "/result";

    /** How long construction waits for the retained-verdict recovery before giving up on dedup priming. */
    private static final long RECOVERY_TIMEOUT_MILLIS = 5_000;

    private final @NotNull InternalPublishService internalPublishService;
    private final @NotNull ExecutorService publishExecutor = MoreExecutors.newDirectExecutorService();
    private final @NotNull ObjectMapper objectMapper;
    private final @NotNull String hivemqId;
    private final @NotNull String adapterId;
    private final @NotNull String tagName;
    private final @NotNull String commandTopic;
    private final @NotNull String resultTopic;
    private final @Nullable ExecutedVerdict recovered;

    /**
     * Recovers the last retained verdict (bounded wait; a recovery failure only forfeits dedup priming, loudly).
     *
     * @param internalPublishService     the broker-internal publish path.
     * @param retainedMessagePersistence the retained store the last verdict is recovered from.
     * @param objectMapper               builds and parses the verdict JSON.
     * @param hivemqId                   the publisher identity (the intake's client id).
     * @param adapterId                  the owning adapter's id.
     * @param tagName                    the tag whose commands this reporter covers.
     * @param commandTopic               the mapping's command topic; verdicts go to its {@code /result} sibling.
     */
    MqttSouthboundWriteVerdictReporter(
            final @NotNull InternalPublishService internalPublishService,
            final @NotNull RetainedMessagePersistence retainedMessagePersistence,
            final @NotNull ObjectMapper objectMapper,
            final @NotNull String hivemqId,
            final @NotNull String adapterId,
            final @NotNull String tagName,
            final @NotNull String commandTopic) {
        this.internalPublishService = internalPublishService;
        this.objectMapper = objectMapper;
        this.hivemqId = hivemqId;
        this.adapterId = adapterId;
        this.tagName = tagName;
        this.commandTopic = commandTopic;
        this.resultTopic = commandTopic + RESULT_TOPIC_SUFFIX;
        this.recovered = recover(retainedMessagePersistence);
    }

    @Override
    public void report(
            final @NotNull String commandId,
            final @NotNull SouthboundWriteOutcome outcome,
            final @Nullable String reason,
            final boolean deduplicated,
            final @Nullable String command,
            final byte @Nullable [] correlationData) {
        final ObjectNode verdict = objectMapper.createObjectNode();
        verdict.put("adapterId", adapterId);
        verdict.put("tag", tagName);
        verdict.put("commandTopic", commandTopic);
        verdict.put("commandId", commandId);
        verdict.put("outcome", outcome.name());
        if (reason == null) {
            verdict.putNull("reason");
        } else {
            verdict.put("reason", reason);
        }
        verdict.put("deduplicated", deduplicated);
        // What was executed and the publisher's own correlation key — so a client recognizes its command's reply.
        if (command == null) {
            verdict.putNull("command");
        } else {
            verdict.put("command", command);
        }
        if (correlationData == null) {
            verdict.putNull("correlationData");
        } else {
            verdict.put("correlationData", java.util.Base64.getEncoder().encodeToString(correlationData));
        }
        verdict.put("completedAtMillis", System.currentTimeMillis());
        final byte[] payload = verdict.toString().getBytes(UTF_8);
        final PUBLISH publish = new PUBLISHFactory.Mqtt3Builder()
                .withQoS(QoS.AT_LEAST_ONCE)
                .withOnwardQos(QoS.AT_LEAST_ONCE)
                .withTopic(resultTopic)
                .withRetain(true)
                .withPayload(payload)
                .withHivemqId(hivemqId)
                .build();
        FutureUtils.addExceptionLogger(internalPublishService.publish(publish, publishExecutor, hivemqId));
    }

    @Override
    public @Nullable ExecutedVerdict lastExecutedVerdict() {
        return recovered;
    }

    private @Nullable ExecutedVerdict recover(final @NotNull RetainedMessagePersistence retainedMessagePersistence) {
        try {
            final RetainedMessage retained =
                    retainedMessagePersistence.get(resultTopic).get(RECOVERY_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
            if (retained == null || retained.getMessage() == null) {
                return null;
            }
            final JsonNode verdict = objectMapper.readTree(retained.getMessage());
            final JsonNode commandId = verdict.get("commandId");
            final JsonNode outcome = verdict.get("outcome");
            if (commandId == null || outcome == null) {
                return null;
            }
            return new ExecutedVerdict(commandId.asText(), SouthboundWriteOutcome.valueOf(outcome.asText()));
        } catch (final InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            log.warn(
                    "Interrupted recovering the last southbound verdict for tag '{}' on adapter '{}' — "
                            + "crash-replay dedup is not primed",
                    tagName,
                    adapterId);
            return null;
        } catch (final Exception failure) {
            log.warn(
                    "Could not recover the last southbound verdict for tag '{}' on adapter '{}' — crash-replay "
                            + "dedup is not primed",
                    tagName,
                    adapterId,
                    failure);
            return null;
        }
    }
}
