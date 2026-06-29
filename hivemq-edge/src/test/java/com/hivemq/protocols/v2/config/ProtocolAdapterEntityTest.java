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
package com.hivemq.protocols.v2.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.hivemq.adapter.sdk.api.v2.node.AccessTriState;
import jakarta.xml.bind.ValidationEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

class ProtocolAdapterEntityTest {

    @Test
    void defaults_matchTheV2ConfigContract() {
        final ProtocolAdapterEntity entity = new ProtocolAdapterEntity();

        assertThat(entity.getConfigVersion()).isEqualTo(ProtocolAdapterEntity.DEFAULT_CONFIG_VERSION);
        assertThat(entity.getConfigVersion()).isGreaterThanOrEqualTo(2);
        assertThat(entity.isNorthboundActivated()).isTrue();
        assertThat(entity.isSouthboundActivated()).isTrue();
        assertThat(entity.isSkipVerification()).isFalse();
        assertThat(entity.getWatchdogTimeoutMillis()).isEqualTo(ProtocolAdapterEntity.DEFAULT_WATCHDOG_TIMEOUT_MILLIS);
        assertThat(entity.getCommandTimeoutMillis()).isEqualTo(ProtocolAdapterEntity.DEFAULT_COMMAND_TIMEOUT_MILLIS);
        assertThat(entity.getWatchdogTimeoutMillis()).isGreaterThan(entity.getCommandTimeoutMillis());
    }

    @Test
    void validEntity_hasNoValidationErrors() {
        assertThat(validate(validAdapter())).isEmpty();
    }

    @Test
    void emptyAdapterId_isRejected() {
        final ProtocolAdapterEntity entity = adapter("", "chaos");
        assertThat(messages(entity)).anyMatch(message -> message.contains("adapter-id"));
    }

    @Test
    void invalidProtocolId_isRejected() {
        assertThat(messages(adapter("a", "has space"))).anyMatch(message -> message.contains("protocol-id"));
        assertThat(messages(adapter("a", "is/slashed"))).anyMatch(message -> message.contains("protocol-id"));
        assertThat(validate(adapter("a", "chaos-2.1_x"))).isEmpty();
    }

    @Test
    void duplicateTagNames_areRejected() {
        final ProtocolAdapterEntity entity = validAdapter();
        entity.getTags().add(tag("temperature"));
        entity.getTags().add(tag("temperature"));

        assertThat(entity.getDuplicatedTagNameSet()).containsExactly("temperature");
        assertThat(messages(entity))
                .anyMatch(message -> message.contains("temperature") && message.contains("more than once"));
    }

    @Test
    void northboundMappingToUnknownTag_isRejected() {
        final ProtocolAdapterEntity entity = validAdapter();
        entity.getTags().add(tag("temperature"));
        entity.getNorthboundMappings().add(new NorthboundMappingEntity("does-not-exist", "plant/a/temperature"));

        assertThat(messages(entity))
                .anyMatch(message -> message.contains("northbound") && message.contains("does-not-exist"));
    }

    @Test
    void southboundMappingToUnknownTag_isRejected() {
        final ProtocolAdapterEntity entity = validAdapter();
        entity.getTags().add(tag("setpoint"));
        entity.getSouthboundMappings().add(new SouthboundMappingEntity("plant/a/setpoint", "does-not-exist"));

        assertThat(messages(entity))
                .anyMatch(message -> message.contains("southbound") && message.contains("does-not-exist"));
    }

    @Test
    void mappingToDeclaredTag_isAccepted() {
        final ProtocolAdapterEntity entity = validAdapter();
        entity.getTags().add(tag("temperature"));
        entity.getNorthboundMappings().add(new NorthboundMappingEntity("temperature", "plant/a/temperature"));
        entity.getSouthboundMappings().add(new SouthboundMappingEntity("plant/a/temperature", "temperature"));

        assertThat(validate(entity)).isEmpty();
    }

    // S32: the watchdog must be strictly greater than the PA command timeout.
    @Test
    void watchdogNotGreaterThanCommandTimeout_isRejected() {
        assertThat(messages(withTimeouts(10_000, 10_000)))
                .anyMatch(message ->
                        message.contains("watchdog-timeout-millis") && message.contains("command-timeout-millis"));
        assertThat(messages(withTimeouts(5_000, 10_000)))
                .anyMatch(message -> message.contains("watchdog-timeout-millis"));
        assertThat(validate(withTimeouts(10_001, 10_000))).isEmpty();
    }

    @Test
    void nonPositiveCommandTimeout_isRejected() {
        assertThat(messages(withTimeouts(30_000, 0))).anyMatch(message -> message.contains("command-timeout-millis"));
    }

    @Test
    void invalidRetryPolicy_isRejected() {
        final ProtocolAdapterEntity entity = entity(
                "chaos-1",
                "chaos",
                true,
                false,
                new RetryPolicyEntity(1_000, 0.5, 32_000, 0),
                30_000,
                10_000,
                List.of());
        assertThat(messages(entity))
                .anyMatch(message -> message.contains("retry-policy") && message.contains("factor"));
    }

    // Reload classification inputs (feeds T11): an activation-only edit and a tag-set edit are each isolatable.
    @Test
    void activationOnlyDifference_isIsolatable() {
        final ProtocolAdapterEntity before = entity(
                "chaos-1", "chaos", true, false, new RetryPolicyEntity(), 30_000, 10_000, List.of(tag("temperature")));
        final ProtocolAdapterEntity after = entity(
                "chaos-1", "chaos", false, false, new RetryPolicyEntity(), 30_000, 10_000, List.of(tag("temperature")));

        assertThat(after).isNotEqualTo(before);
        assertThat(after.getTags()).isEqualTo(before.getTags());
        assertThat(after.getAdapterConfiguration()).isEqualTo(before.getAdapterConfiguration());
        assertThat(after.isNorthboundActivated()).isNotEqualTo(before.isNorthboundActivated());
    }

    @Test
    void tagsOnlyDifference_isIsolatable() {
        final ProtocolAdapterEntity before = entity(
                "chaos-1", "chaos", true, false, new RetryPolicyEntity(), 30_000, 10_000, List.of(tag("temperature")));
        final ProtocolAdapterEntity after = entity(
                "chaos-1",
                "chaos",
                true,
                false,
                new RetryPolicyEntity(),
                30_000,
                10_000,
                List.of(tag("temperature"), tag("pressure")));

        assertThat(after).isNotEqualTo(before);
        assertThat(after.isNorthboundActivated()).isEqualTo(before.isNorthboundActivated());
        assertThat(after.isSouthboundActivated()).isEqualTo(before.isSouthboundActivated());
        assertThat(after.getAdapterConfiguration()).isEqualTo(before.getAdapterConfiguration());
        assertThat(after.getTags()).isNotEqualTo(before.getTags());
    }

    private static @NotNull ProtocolAdapterEntity validAdapter() {
        return adapter("chaos-1", "chaos");
    }

    private static @NotNull ProtocolAdapterEntity adapter(
            final @NotNull String adapterId, final @NotNull String protocolId) {
        return entity(adapterId, protocolId, true, false, new RetryPolicyEntity(), 30_000, 10_000, List.of());
    }

    private static @NotNull ProtocolAdapterEntity withTimeouts(final long watchdogMillis, final long commandMillis) {
        return entity(
                "chaos-1", "chaos", true, false, new RetryPolicyEntity(), watchdogMillis, commandMillis, List.of());
    }

    private static @NotNull ProtocolAdapterEntity entity(
            final @NotNull String adapterId,
            final @NotNull String protocolId,
            final boolean northboundActivated,
            final boolean southboundActivated,
            final @NotNull RetryPolicyEntity retryPolicy,
            final long watchdogMillis,
            final long commandMillis,
            final @NotNull List<TagEntity> tags) {
        return new ProtocolAdapterEntity(
                adapterId,
                protocolId,
                2,
                northboundActivated,
                southboundActivated,
                false,
                Map.of(),
                retryPolicy,
                watchdogMillis,
                commandMillis,
                new ArrayList<>(tags),
                new ArrayList<>(),
                new ArrayList<>());
    }

    private static @NotNull TagEntity tag(final @NotNull String name) {
        return new TagEntity(
                name,
                "{\"id\":\"" + name + "\"}",
                true,
                true,
                true,
                false,
                5_000,
                new AccessFlagsEntity(AccessTriState.YES, AccessTriState.YES, AccessTriState.YES, AccessTriState.NO));
    }

    private static @NotNull List<ValidationEvent> validate(final @NotNull ProtocolAdapterEntity entity) {
        final List<ValidationEvent> events = new ArrayList<>();
        entity.validate(events);
        return events;
    }

    private static @NotNull List<String> messages(final @NotNull ProtocolAdapterEntity entity) {
        return validate(entity).stream().map(ValidationEvent::getMessage).toList();
    }
}
