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
    void duplicateSouthboundTopic_isRejected() {
        // Two mappings on one topic would share one durable command queue between two tags (and the second
        // backlog's wakeup would replace the first's) — one command topic feeds exactly one tag.
        final ProtocolAdapterEntity entity = validAdapter();
        entity.getTags().add(tag("setpoint"));
        entity.getTags().add(tag("ramp-rate"));
        entity.getSouthboundMappings().add(new SouthboundMappingEntity("plant/a/cmd", "setpoint"));
        entity.getSouthboundMappings().add(new SouthboundMappingEntity("plant/a/cmd", "ramp-rate"));

        assertThat(messages(entity))
                .anyMatch(message -> message.contains("duplicate topic") && message.contains("plant/a/cmd"));
    }

    @Test
    void southboundMappingOnUnwritableTag_isRejected() {
        // The mapping's durable queue would accept commands no delivery can ever drain (the write window never
        // opens for a tag whose access model forbids writing) — a contradiction refused at load, not a paused tag.
        final ProtocolAdapterEntity entity = validAdapter();
        entity.getTags()
                .add(new TagEntity(
                        "setpoint",
                        "{\"id\":\"setpoint\"}",
                        true,
                        true,
                        true,
                        false,
                        5_000,
                        new AccessFlagsEntity(
                                AccessTriState.YES, AccessTriState.NO, AccessTriState.YES, AccessTriState.NO)));
        entity.getSouthboundMappings().add(new SouthboundMappingEntity("plant/a/cmd", "setpoint"));

        assertThat(messages(entity))
                .anyMatch(message -> message.contains("not writable") && message.contains("setpoint"));
    }

    @Test
    void southboundMappingOnWriteDeactivatedTag_isAccepted() {
        // write-activated=false is the PAUSED form: the tag CAN be written, delivery is merely suspended, and
        // queued commands drain when it is re-activated. Only an access model that forbids writing is fatal.
        final ProtocolAdapterEntity entity = validAdapter();
        entity.getTags()
                .add(new TagEntity(
                        "setpoint",
                        "{\"id\":\"setpoint\"}",
                        true,
                        false,
                        true,
                        false,
                        5_000,
                        new AccessFlagsEntity(
                                AccessTriState.YES, AccessTriState.YES, AccessTriState.YES, AccessTriState.NO)));
        entity.getSouthboundMappings().add(new SouthboundMappingEntity("plant/a/cmd", "setpoint"));

        assertThat(validate(entity)).isEmpty();
    }

    @Test
    void mappingToDeclaredTag_isAccepted() {
        final ProtocolAdapterEntity entity = validAdapter();
        entity.getTags().add(tag("temperature"));
        entity.getNorthboundMappings().add(new NorthboundMappingEntity("temperature", "plant/a/temperature"));
        entity.getSouthboundMappings().add(new SouthboundMappingEntity("plant/a/temperature", "temperature"));

        assertThat(validate(entity)).isEmpty();
    }

    // EDG-824 #12: a topic the broker cannot deliver to is a config error, not a silent GREEN data drop.
    @Test
    void northboundMappingWithWildcardTopic_isRejected() {
        assertThat(northboundTopicMessages("plant/#")).anyMatch(message -> message.contains("not a valid topic"));
        assertThat(northboundTopicMessages("plant/+/temperature"))
                .anyMatch(message -> message.contains("not a valid topic"));
        assertThat(northboundTopicMessages("plant/temp#erature"))
                .anyMatch(message -> message.contains("not a valid topic"));
        assertThat(northboundTopicMessages("plant/temp\0erature"))
                .anyMatch(message -> message.contains("not a valid topic"));
    }

    // EDG-824 #13: the '$' namespace is broker-owned; an adapter must not be able to publish into $SYS.
    @Test
    void northboundMappingIntoTheDollarNamespace_isRejected() {
        assertThat(northboundTopicMessages("$SYS/edge/injected"))
                .anyMatch(message -> message.contains("reserved '$' namespace"));
        assertThat(northboundTopicMessages("$share/group/plant"))
                .anyMatch(message -> message.contains("reserved '$' namespace"));
    }

    // EDG-824 #12: MQTT cannot carry a topic beyond 65535 UTF-8 bytes.
    @Test
    void northboundMappingWithOverlongTopic_isRejected() {
        final String overlong = "plant/" + "x".repeat(NorthboundMappingEntity.MAX_TOPIC_LENGTH_BYTES);
        assertThat(northboundTopicMessages(overlong)).anyMatch(message -> message.contains("maximum length"));
        // Byte semantics, not char count: multi-byte characters trip the limit below 65535 chars.
        final String multiByte = "plant/" + "é".repeat(33_000); // 2 bytes each -> 66006 bytes
        assertThat(northboundTopicMessages(multiByte)).anyMatch(message -> message.contains("maximum length"));
        final String longestLegal = "x".repeat(NorthboundMappingEntity.MAX_TOPIC_LENGTH_BYTES);
        assertThat(northboundTopicMessages(longestLegal)).isEmpty();
    }

    @Test
    void southboundMappingTopicFilter_allowsWildcardsButRejectsIllegalFiltersAndDollar() {
        assertThat(southboundTopicMessages("plant/+/setpoint")).isEmpty();
        assertThat(southboundTopicMessages("plant/#")).isEmpty();
        assertThat(southboundTopicMessages("plant/#/setpoint"))
                .anyMatch(message -> message.contains("not a valid topic filter"));
        assertThat(southboundTopicMessages("plant/set+point"))
                .anyMatch(message -> message.contains("not a valid topic filter"));
        assertThat(southboundTopicMessages("$SYS/edge/commands"))
                .anyMatch(message -> message.contains("reserved '$' namespace"));
    }

    // EDG-824 #11 (Sam round 2, finding 7): the share name of a v2 southbound subscription belongs to Edge — the
    // runtime derives it from the adapter id and uses it as the marker that keeps the durable command queue alive
    // while its consumer is detached. An operator-supplied group has nowhere to go, and accepting one was silently
    // destructive: the literal '$share/...' text became the subscription filter, so it matched only a topic of that
    // literal name while the adapter reported GREEN. Refusing the configuration is the honest failure.
    @Test
    void southboundMappingWithASharedSubscriptionFilter_isRejectedWithTheShareNameReason() {
        assertThat(southboundTopicMessages("$share/group/plant/+/setpoint"))
                .anyMatch(message -> message.contains("Edge owns the share name"));
        assertThat(southboundTopicMessages("$share/group/plant/setpoint"))
                .anyMatch(message -> message.contains("shared-subscription filter"));
        // the degenerate forms are refused too, rather than falling through to a confusing '$' namespace message
        assertThat(southboundTopicMessages("$share")).anyMatch(message -> message.contains("shared-subscription"));
        assertThat(southboundTopicMessages("$share/")).anyMatch(message -> message.contains("shared-subscription"));
        // a shared filter smuggling the broker namespace inside it is refused for the share reason as well
        assertThat(southboundTopicMessages("$share/group/$SYS/#"))
                .anyMatch(message -> message.contains("shared-subscription"));
        // the plain filter an operator should have written instead is accepted
        assertThat(southboundTopicMessages("plant/+/setpoint")).isEmpty();
    }

    // EDG-824 #11: two tags on one topic is legal but never silent — a WARNING event carries the collision.
    @Test
    void collidingNorthboundTopics_raiseAWarningNotAnError() {
        final ProtocolAdapterEntity entity = validAdapter();
        entity.getTags().add(tag("temperature"));
        entity.getTags().add(tag("pressure"));
        entity.getNorthboundMappings().add(new NorthboundMappingEntity("temperature", "plant/a/data"));
        entity.getNorthboundMappings().add(new NorthboundMappingEntity("pressure", "plant/a/data"));

        assertThat(entity.getCollidingNorthboundTopics())
                .containsOnlyKeys("plant/a/data")
                .satisfies(collisions ->
                        assertThat(collisions.get("plant/a/data")).containsExactly("temperature", "pressure"));
        final List<ValidationEvent> events = validate(entity);
        assertThat(events)
                .anyMatch(event -> event.getSeverity() == ValidationEvent.WARNING
                        && event.getMessage().contains("plant/a/data")
                        && event.getMessage().contains("interleave"));
        // strictly a warning: no ERROR/FATAL_ERROR raised for the collision
        assertThat(events)
                .noneMatch(event -> event.getSeverity() == ValidationEvent.FATAL_ERROR
                        || event.getSeverity() == ValidationEvent.ERROR);
    }

    @Test
    void distinctNorthboundTopics_produceNoCollisionWarning() {
        final ProtocolAdapterEntity entity = validAdapter();
        entity.getTags().add(tag("temperature"));
        entity.getTags().add(tag("pressure"));
        entity.getNorthboundMappings().add(new NorthboundMappingEntity("temperature", "plant/a/temperature"));
        entity.getNorthboundMappings().add(new NorthboundMappingEntity("pressure", "plant/a/pressure"));

        assertThat(entity.getCollidingNorthboundTopics()).isEmpty();
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

    // Review on #1635: the interval becomes an absolute deadline (now + interval) on the actor's timer queue, so an
    // unbounded value wraps into a deadline in the past and the slowest configurable cadence polls on every tick.
    // Bounding it here is the fix; the actor's saturation is defence in depth, not the guarantee.
    @Test
    void pollIntervalBeyondTheMaximum_isRejected() {
        assertThat(messages(withPollInterval(Long.MAX_VALUE)))
                .anyMatch(message -> message.contains("poll-interval-millis") && message.contains("exceeds"));
        assertThat(messages(withPollInterval(TagEntity.MAXIMUM_POLL_INTERVAL_MILLIS + 1)))
                .anyMatch(message -> message.contains("poll-interval-millis") && message.contains("exceeds"));
        // the boundary itself is legal, and no configured cadence within it can overflow a wall clock
        assertThat(validate(withPollInterval(TagEntity.MAXIMUM_POLL_INTERVAL_MILLIS)))
                .isEmpty();
    }

    @Test
    void nonPositivePollInterval_isRejected() {
        assertThat(messages(withPollInterval(0)))
                .anyMatch(message -> message.contains("poll-interval-millis") && message.contains("positive"));
        assertThat(messages(withPollInterval(-1)))
                .anyMatch(message -> message.contains("poll-interval-millis") && message.contains("positive"));
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

    // EDG-824 #14: the access model is enforced at runtime, so an activated aspect whose <access> flags permit no
    // capability silently never operates. That has to be a deliberate choice rather than a typo, so the
    // contradiction is surfaced — as a WARNING, because the configuration is legal, just probably not intended.
    // The rules live on TagEntity (they read one tag only) but take the adapter id, which the tag cannot know: two
    // adapters may declare the same tag name, and a warning that cannot be traced to one of them is not actionable.

    @Test
    void aReadActivatedTagWhoseAccessPermitsNoReadTransport_warnsAndNamesItsAdapter() {
        final ProtocolAdapterEntity entity = adapter("plc-1", "chaos");
        // pollable tag, but the access model forbids polling — nothing can ever read it
        entity.getTags()
                .add(tagWithAccess(
                        "temperature",
                        true,
                        false,
                        new AccessFlagsEntity(
                                AccessTriState.YES, AccessTriState.YES, AccessTriState.NO, AccessTriState.NO)));

        assertThat(messages(entity))
                .anyMatch(message -> message.contains("adapter [plc-1]")
                        && message.contains("tag [temperature]")
                        && message.contains("the tag will never be read"));
        assertThat(validate(entity))
                .filteredOn(event -> event.getMessage().contains("never be read"))
                .allMatch(event -> event.getSeverity() == ValidationEvent.WARNING);
    }

    @Test
    void aWriteActivatedTagWhoseAccessForbidsWriting_warnsAndNamesItsAdapter() {
        final ProtocolAdapterEntity entity = adapter("plc-2", "chaos");
        entity.getTags()
                .add(tagWithAccess(
                        "setpoint",
                        true,
                        false,
                        new AccessFlagsEntity(
                                AccessTriState.YES, AccessTriState.NO, AccessTriState.YES, AccessTriState.NO)));

        assertThat(messages(entity))
                .anyMatch(message -> message.contains("adapter [plc-2]")
                        && message.contains("tag [setpoint]")
                        && message.contains("the tag will never be written"));
    }

    @Test
    void aTagWhoseAccessPermitsItsActivatedAspects_raisesNoContradictionWarning() {
        final ProtocolAdapterEntity entity = adapter("plc-3", "chaos");
        entity.getTags()
                .add(tagWithAccess(
                        "temperature",
                        true,
                        false,
                        new AccessFlagsEntity(
                                AccessTriState.YES, AccessTriState.YES, AccessTriState.YES, AccessTriState.NO)));

        assertThat(messages(entity)).noneMatch(message -> message.contains("will never be"));
    }

    @Test
    void anOmittedAccessElement_isUnrestrictedAndNeverContradictsAnything() {
        // The semantic decision this rests on (EDG-824 #14): an omitted <access> means unrestricted, NOT deny-all.
        // A deny-all default would make this warning fire for every tag in every configuration that never mentions
        // access — which is most of them.
        final ProtocolAdapterEntity entity = adapter("plc-4", "chaos");
        // AccessFlagsEntity.unrestricted() is exactly what TagEntity's field initializer leaves behind when the
        // <access> element is absent, so this is the shape a configuration that never mentions access really has.
        entity.getTags().add(tagWithAccess("temperature", true, false, AccessFlagsEntity.unrestricted()));

        assertThat(messages(entity)).noneMatch(message -> message.contains("will never be"));
    }

    private static @NotNull TagEntity tagWithAccess(
            final @NotNull String name,
            final boolean pollable,
            final boolean subscribable,
            final @NotNull AccessFlagsEntity access) {
        return new TagEntity(name, "{\"id\":\"" + name + "\"}", true, true, pollable, subscribable, 5_000, access);
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

    private static @NotNull ProtocolAdapterEntity withPollInterval(final long pollIntervalMillis) {
        final ProtocolAdapterEntity entity = validAdapter();
        entity.getTags()
                .add(new TagEntity(
                        "temperature",
                        "{\"id\":\"temperature\"}",
                        true,
                        true,
                        true,
                        false,
                        pollIntervalMillis,
                        new AccessFlagsEntity(
                                AccessTriState.YES, AccessTriState.YES, AccessTriState.YES, AccessTriState.NO)));
        return entity;
    }

    private static @NotNull List<String> northboundTopicMessages(final @NotNull String topic) {
        final ProtocolAdapterEntity entity = validAdapter();
        entity.getTags().add(tag("temperature"));
        entity.getNorthboundMappings().add(new NorthboundMappingEntity("temperature", topic));
        return messages(entity);
    }

    private static @NotNull List<String> southboundTopicMessages(final @NotNull String topic) {
        final ProtocolAdapterEntity entity = validAdapter();
        entity.getTags().add(tag("setpoint"));
        entity.getSouthboundMappings().add(new SouthboundMappingEntity(topic, "setpoint"));
        return messages(entity);
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
