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
package com.hivemq.protocols.v2.wrapper;

import static com.hivemq.protocols.v2.wrapper.ProtocolAdapterWrapperState.CONNECTED;
import static org.assertj.core.api.Assertions.assertThat;

import com.hivemq.adapter.sdk.api.v2.node.NodeTagPair;
import com.hivemq.protocols.v2.view.TagStatus;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

/**
 * The tags-only reconfigure transition on a live adapter (EDG-824 #2, the reconfigure wedge). A
 * {@code UpdateTagSet} rebuilds every tag aspect; the rebuilt aspects must be re-coupled to the adapter's live
 * connection phase and re-verify <b>in place</b> — a stably-CONNECTED adapter never reconnects on a tags-only
 * change, so without the phase replay every rebuilt tag would park in {@code WAITING_FOR_ADAPTER_READY} forever
 * and fold to an {@code ERROR} status while the adapter shows GREEN.
 */
class TagSetReconfigureTest {

    private static final long POLL_INTERVAL_MILLIS = 1_000L;

    private static @NotNull WrapperTestFixture fixtureWith(final @NotNull NodeTagPair pair) {
        return WrapperTestFixture.builder()
                .runningCoordinator()
                .nodes(List.of(pair))
                .pollIntervalMillis(POLL_INTERVAL_MILLIS)
                .build();
    }

    private static long count(final @NotNull List<String> commands, final @NotNull String command) {
        return commands.stream().filter(command::equals).count();
    }

    /**
     * Let the queued batches reach the adapter: one tick dispatches the verify batch, the next dispatches the
     * add-subscription batch that the successful verify queued.
     */
    private static void dispatchBatches(final @NotNull WrapperTestFixture fixture) {
        fixture.advance(100);
        fixture.advance(100);
    }

    /** Drive a subscribable tag to SUBSCRIBED by pushing its first value — that is what confirms a subscription. */
    private static void confirmSubscription(
            final @NotNull WrapperTestFixture fixture, final @NotNull NodeTagPair pair) {
        fixture.output.dataPoint(
                pair.node(), WrapperTestSupport.dataPoint(pair.tag().name(), "1"));
        fixture.drain();
    }

    private static @NotNull ProtocolAdapterWrapperCommand.UpdateTagSet tagSet(final @NotNull NodeTagPair... pairs) {
        final Map<String, TagAspectActivationPreference> activation = new LinkedHashMap<>();
        final Set<String> names = new LinkedHashSet<>();
        for (final NodeTagPair pair : pairs) {
            activation.put(pair.tag().name(), TagAspectActivationPreference.defaults());
            names.add(pair.tag().name());
        }
        return new ProtocolAdapterWrapperCommand.UpdateTagSet(
                List.of(pairs), activation, names, Set.of(), Set.of(), POLL_INTERVAL_MILLIS);
    }

    @Test
    void tagsOnlyReload_onAConnectedAdapter_reVerifiesInPlaceAndResumesProducing() {
        final NodeTagPair temperature = WrapperTestSupport.pair("temperature");
        final NodeTagPair pressure = WrapperTestSupport.pair("pressure");
        final WrapperTestFixture fixture = fixtureWith(temperature);

        fixture.activate(ProtocolAdapterDirection.NORTHBOUND);
        assertThat(fixture.state()).isEqualTo(CONNECTED);
        assertThat(fixture.readState("temperature")).isEqualTo("WAITING_FOR_POLL_INTERVAL");
        final long connectsBefore = count(fixture.commands(), "connect");

        // The reconfigure: add a second tag — a tags-only change on a stably-CONNECTED adapter.
        fixture.send(new ProtocolAdapterWrapperCommand.UpdateTagSet(
                List.of(temperature, pressure),
                Map.of(
                        "temperature", TagAspectActivationPreference.defaults(),
                        "pressure", TagAspectActivationPreference.defaults()),
                Set.of("temperature", "pressure"),
                Set.of(),
                Set.of(),
                POLL_INTERVAL_MILLIS));

        // Never reconnects...
        assertThat(fixture.state()).isEqualTo(CONNECTED);
        assertThat(count(fixture.commands(), "connect")).isEqualTo(connectsBefore);
        // ...and never parks: the rebuilt aspects re-verified in place and resumed operating.
        assertThat(fixture.readState("temperature")).isNotEqualTo("WAITING_FOR_ADAPTER_READY");
        assertThat(fixture.readState("pressure")).isNotEqualTo("WAITING_FOR_ADAPTER_READY");
        assertThat(fixture.tagStatus("temperature")).isEqualTo(TagStatus.NORTHBOUND_ONLY);
        assertThat(fixture.tagStatus("pressure")).isEqualTo(TagStatus.NORTHBOUND_ONLY);

        // The surviving tag and the added tag both actually produce again: the next cadence polls both nodes and
        // the returned values are accepted back into the poll loop.
        fixture.advance(POLL_INTERVAL_MILLIS + 100); // cadence + one tick to dispatch the batch
        assertThat(count(fixture.commands(), "pollBatch")).isGreaterThanOrEqualTo(1);
        assertThat(fixture.readState("temperature")).isEqualTo("WAITING_FOR_POLL_DATAPOINT");
        assertThat(fixture.readState("pressure")).isEqualTo("WAITING_FOR_POLL_DATAPOINT");
        fixture.output.dataPoint(temperature.node(), WrapperTestSupport.dataPoint("temperature", "21"));
        fixture.output.dataPoint(pressure.node(), WrapperTestSupport.dataPoint("pressure", "1.1"));
        fixture.drain();
        assertThat(fixture.readState("temperature")).isEqualTo("WAITING_FOR_POLL_INTERVAL");
        assertThat(fixture.readState("pressure")).isEqualTo("WAITING_FOR_POLL_INTERVAL");
        assertThat(fixture.tag("temperature").failureCount()).isZero();
        assertThat(fixture.tag("pressure").failureCount()).isZero();
    }

    @Test
    void pollIntervalOnlyReload_appliesTheNewCadence() {
        final NodeTagPair temperature = WrapperTestSupport.pair("temperature");
        final WrapperTestFixture fixture = fixtureWith(temperature);
        fixture.activate(ProtocolAdapterDirection.NORTHBOUND);
        assertThat(fixture.readState("temperature")).isEqualTo("WAITING_FOR_POLL_INTERVAL");

        // The reconfigure: same tag set, five-times-faster cadence.
        fixture.send(new ProtocolAdapterWrapperCommand.UpdateTagSet(
                List.of(temperature),
                Map.of("temperature", TagAspectActivationPreference.defaults()),
                Set.of("temperature"),
                Set.of(),
                Set.of(),
                POLL_INTERVAL_MILLIS / 5));

        // The first poll after the reload runs at the NEW 200 ms cadence: it fires well before the stale
        // 1000 ms cadence would have. (One poll only — the mock adapter does not answer polls by itself.)
        final long pollsBefore = count(fixture.commands(), "pollBatch");
        fixture.advance(2 * (POLL_INTERVAL_MILLIS / 5)); // two new cadences — well under the stale interval
        assertThat(count(fixture.commands(), "pollBatch")).isGreaterThan(pollsBefore);
    }

    @Test
    void tagsOnlyReload_whileStopped_staysParkedAndCouplesOnTheNextConnect() {
        final NodeTagPair temperature = WrapperTestSupport.pair("temperature");
        final NodeTagPair pressure = WrapperTestSupport.pair("pressure");
        final WrapperTestFixture fixture = fixtureWith(temperature);

        // No direction active: the adapter is at rest, the replayed phase is DISCONNECTED — nothing changes.
        fixture.send(new ProtocolAdapterWrapperCommand.UpdateTagSet(
                List.of(temperature, pressure),
                Map.of(
                        "temperature", TagAspectActivationPreference.defaults(),
                        "pressure", TagAspectActivationPreference.defaults()),
                Set.of("temperature", "pressure"),
                Set.of(),
                Set.of(),
                POLL_INTERVAL_MILLIS));
        assertThat(fixture.readState("temperature")).isEqualTo("DEACTIVATED");

        // The next connect cycle couples the rebuilt aspects normally.
        fixture.activate(ProtocolAdapterDirection.NORTHBOUND);
        assertThat(fixture.state()).isEqualTo(CONNECTED);
        assertThat(fixture.tagStatus("temperature")).isEqualTo(TagStatus.NORTHBOUND_ONLY);
        assertThat(fixture.tagStatus("pressure")).isEqualTo(TagStatus.NORTHBOUND_ONLY);
    }

    // ── the SUBSCRIBED sibling of the reload path (QA #18, Sam CR2 test-gap 2) ──────────────────────────────────
    // Every reload test above is poll-based. The subscription path is the one QA saw fail on the epic build: an
    // added subscribable tag was silently ignored — "no VERIFY, no ADDSUB, no delivery, no error, s3 appears zero
    // times anywhere" — and a later removal then issued REMOVESUB for every tag with no re-subscribe, killing push
    // delivery permanently while the adapter stayed up and green. These pin the correct behaviour per NODE, which
    // is the only granularity at which "the command was never issued for s3" can be stated.

    @Test
    void subscribedTagsOnlyReload_addingATag_verifiesAndSubscribesTheAddedNode() {
        final NodeTagPair s1 = WrapperTestSupport.subscribablePair("s1");
        final NodeTagPair s2 = WrapperTestSupport.subscribablePair("s2");
        final NodeTagPair s3 = WrapperTestSupport.subscribablePair("s3");
        final WrapperTestFixture fixture = WrapperTestFixture.builder()
                .runningCoordinator()
                .nodes(List.of(s1, s2))
                .pollIntervalMillis(POLL_INTERVAL_MILLIS)
                .build();
        fixture.activate(ProtocolAdapterDirection.NORTHBOUND);
        dispatchBatches(fixture);
        confirmSubscription(fixture, s1);
        confirmSubscription(fixture, s2);
        assertThat(fixture.readState("s1")).isEqualTo("SUBSCRIBED");
        assertThat(fixture.readState("s2")).isEqualTo("SUBSCRIBED");

        fixture.send(tagSet(s1, s2, s3));
        dispatchBatches(fixture);

        // The added tag is not silently dropped: it is verified and subscribed by name.
        assertThat(fixture.adapter.verifiedNodes).contains("s3");
        assertThat(fixture.adapter.subscriptionAdds).contains("s3");
        // ...and it actually delivers, which is the only proof that matters.
        confirmSubscription(fixture, s3);
        assertThat(fixture.readState("s3")).isEqualTo("SUBSCRIBED");
        assertThat(fixture.tagStatus("s3")).isEqualTo(TagStatus.NORTHBOUND_ONLY);
        assertThat(fixture.tag("s3").failureCount()).isZero();
    }

    @Test
    void subscribedTagsOnlyReload_removingATag_leavesEverySurvivorSubscribedAndDelivering() {
        final NodeTagPair s1 = WrapperTestSupport.subscribablePair("s1");
        final NodeTagPair s2 = WrapperTestSupport.subscribablePair("s2");
        final NodeTagPair s3 = WrapperTestSupport.subscribablePair("s3");
        final WrapperTestFixture fixture = WrapperTestFixture.builder()
                .runningCoordinator()
                .nodes(List.of(s1, s2, s3))
                .pollIntervalMillis(POLL_INTERVAL_MILLIS)
                .build();
        fixture.activate(ProtocolAdapterDirection.NORTHBOUND);
        dispatchBatches(fixture);
        confirmSubscription(fixture, s1);
        confirmSubscription(fixture, s2);
        confirmSubscription(fixture, s3);

        fixture.send(tagSet(s1, s2)); // drop s3
        dispatchBatches(fixture);

        // the removed node's subscription is released...
        assertThat(fixture.adapter.subscriptionRemovals).contains("s3");
        // ...and the survivors are re-established, not left dead — the QA #18 failure was REMOVESUB for everything
        // followed by no re-VERIFY and no re-ADDSUB ever again.
        assertThat(fixture.adapter.subscriptionAdds).contains("s1", "s2");
        confirmSubscription(fixture, s1);
        confirmSubscription(fixture, s2);
        assertThat(fixture.readState("s1")).isEqualTo("SUBSCRIBED");
        assertThat(fixture.readState("s2")).isEqualTo("SUBSCRIBED");
        assertThat(fixture.tagStatus("s1")).isEqualTo(TagStatus.NORTHBOUND_ONLY);
        assertThat(fixture.tagStatus("s2")).isEqualTo(TagStatus.NORTHBOUND_ONLY);
        // the dropped tag is gone from the published set entirely, not lingering as a zombie
        assertThat(fixture.snapshot().tags()).noneMatch(tag -> tag.tagName().equals("s3"));
    }

    @Test
    void subscribedTagSetChurn_settlesBackToTheBaselineAndKeepsDelivering() {
        // Estefania's tripwire shape: alternate {s1,s2} ↔ {s1,s2,s3} repeatedly and end on the baseline. The
        // shadow set must return to two live subscriptions and both must still push — no ratchet, no silent death.
        final NodeTagPair s1 = WrapperTestSupport.subscribablePair("s1");
        final NodeTagPair s2 = WrapperTestSupport.subscribablePair("s2");
        final NodeTagPair s3 = WrapperTestSupport.subscribablePair("s3");
        final WrapperTestFixture fixture = WrapperTestFixture.builder()
                .runningCoordinator()
                .nodes(List.of(s1, s2))
                .pollIntervalMillis(POLL_INTERVAL_MILLIS)
                .build();
        fixture.activate(ProtocolAdapterDirection.NORTHBOUND);
        dispatchBatches(fixture);
        confirmSubscription(fixture, s1);
        confirmSubscription(fixture, s2);

        for (int cycle = 0; cycle < 6; cycle++) {
            fixture.send(tagSet(s1, s2, s3));
            dispatchBatches(fixture);
            confirmSubscription(fixture, s3);
            fixture.send(tagSet(s1, s2)); // back to the baseline
            dispatchBatches(fixture);
            confirmSubscription(fixture, s1);
            confirmSubscription(fixture, s2);
        }

        assertThat(fixture.snapshot().tags()).hasSize(2);
        assertThat(fixture.readState("s1")).isEqualTo("SUBSCRIBED");
        assertThat(fixture.readState("s2")).isEqualTo("SUBSCRIBED");
        assertThat(fixture.tagStatus("s1")).isEqualTo(TagStatus.NORTHBOUND_ONLY);
        assertThat(fixture.tagStatus("s2")).isEqualTo(TagStatus.NORTHBOUND_ONLY);
        assertThat(fixture.state()).isEqualTo(CONNECTED);
    }
}
