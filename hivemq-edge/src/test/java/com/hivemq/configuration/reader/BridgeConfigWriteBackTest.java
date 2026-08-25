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
package com.hivemq.configuration.reader;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.google.common.io.Files;
import com.hivemq.bridge.config.LocalSubscription;
import com.hivemq.bridge.config.MqttBridge;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

/**
 * What writing {@code config.xml} back out does to a bridge the operator did not touch (EDG-882 QA
 * round 2).
 * <p>
 * {@code sync} rebuilds every bridge entity from the runtime objects, and <b>any</b> REST write of
 * <b>any</b> subsystem triggers a write — creating a protocol adapter rewrites the bridges. So
 * anything the round trip loses is lost from the operator's file, and anything it changes is a
 * configuration change: the reload that follows sees a different bridge, restarts it, and clears the
 * queues of whatever the new configuration cannot match.
 */
public class BridgeConfigWriteBackTest extends AbstractConfigurationTest {

    /** Filters written deliberately out of alphabetical order, with a queue limit. */
    private static final @NotNull String CONFIG = "" + "<hivemq>\n"
            + "<mqtt-bridges>\n"
            + "    <mqtt-bridge>\n"
            + "        <id>edg-882-writeback</id>\n"
            + "        <remote-broker>\n"
            + "            <host>testhost</host>\n"
            + "        </remote-broker>\n"
            + "        <forwarded-topics>\n"
            + "            <forwarded-topic>\n"
            + "                <filters>\n"
            + "                    <mqtt-topic-filter>zone-b/#</mqtt-topic-filter>\n"
            + "                    <mqtt-topic-filter>alarms/#</mqtt-topic-filter>\n"
            + "                </filters>\n"
            + "                <excludes>\n"
            + "                    <mqtt-topic-filter>zone-b/private/#</mqtt-topic-filter>\n"
            + "                    <mqtt-topic-filter>alarms/private/#</mqtt-topic-filter>\n"
            + "                </excludes>\n"
            + "                <destination>{#}</destination>\n"
            + "                <max-qos>1</max-qos>\n"
            + "                <queue-limit>500</queue-limit>\n"
            + "            </forwarded-topic>\n"
            + "        </forwarded-topics>\n"
            + "    </mqtt-bridge>\n"
            + "</mqtt-bridges>"
            + "</hivemq>";

    private @NotNull MqttBridge loadAndWriteBack() throws IOException {
        Files.write(CONFIG.getBytes(UTF_8), xmlFile);
        reader.applyConfig();
        final MqttBridge asRead = bridgeConfiguration.getBridges().get(0);

        // the production write path, not writeConfigToXML: only this one runs the extractors' sync,
        // which is where the bridge entities are rebuilt
        reader.writeConfigWithSync();

        reader.applyConfig();
        final MqttBridge afterRoundTrip = bridgeConfiguration.getBridges().get(0);
        assertThat(afterRoundTrip)
                .as("a write-back the operator did not ask for must not change the bridge")
                .isEqualTo(asRead);
        return afterRoundTrip;
    }

    @Test
    public void whenTheConfigurationIsWrittenBack_thenTheQueueLimitSurvives() throws IOException {
        final MqttBridge bridge = loadAndWriteBack();

        assertThat(bridge.getLocalSubscriptions().get(0).getQueueLimit()).isEqualTo(500L);
    }

    @Test
    public void whenTheConfigurationIsWrittenBack_thenTheOperatorsFilterOrderIsKept() throws IOException {
        loadAndWriteBack();

        final String written = java.nio.file.Files.readString(xmlFile.toPath());
        assertThat(written.indexOf("zone-b/#"))
                .as("the filters were written as zone-b then alarms and must stay that way")
                .isLessThan(written.indexOf("alarms/#"));
        assertThat(written.indexOf("zone-b/private/#")).isLessThan(written.indexOf("alarms/private/#"));
    }

    @Test
    public void whenTheConfigurationIsWrittenBack_thenTheQueueLimitElementIsStillThere() throws IOException {
        loadAndWriteBack();

        assertThat(java.nio.file.Files.readString(xmlFile.toPath())).contains("<queue-limit>500</queue-limit>");
    }

    /**
     * Canonicalisation is for identity, not for the file: the subscription the runtime works with is
     * still sorted, whatever order it was written in.
     */
    @Test
    public void whenFiltersAreOutOfOrder_thenTheRuntimeStillSeesThemCanonically() throws IOException {
        Files.write(CONFIG.getBytes(UTF_8), xmlFile);
        reader.applyConfig();

        final LocalSubscription subscription =
                bridgeConfiguration.getBridges().get(0).getLocalSubscriptions().get(0);

        assertThat(subscription.getFilters()).containsExactly("alarms/#", "zone-b/#");
        assertThat(subscription.getConfiguredFilters()).containsExactly("zone-b/#", "alarms/#");
        assertThat(subscription.getExcludes()).containsExactly("alarms/private/#", "zone-b/private/#");
        assertThat(subscription.getConfiguredExcludes()).containsExactly("zone-b/private/#", "alarms/private/#");
    }

    /**
     * EDG-882 QA round 1: an update expressed as remove-then-add notified the bridge subsystem twice,
     * and the first notification said the bridge was gone from the configuration — which is answered by
     * stopping it and clearing every queue it owns, whatever the second notification then says.
     */
    @Test
    public void whenABridgeIsReplaced_thenTheConsumerNeverSeesItAbsent() throws IOException {
        Files.write(CONFIG.getBytes(UTF_8), xmlFile);
        reader.applyConfig();
        final List<List<String>> notifiedBridgeIds = new ArrayList<>();
        bridgeConfiguration.registerConsumer(bridges ->
                notifiedBridgeIds.add(bridges.stream().map(MqttBridge::getId).toList()));
        notifiedBridgeIds.clear();

        final boolean replaced = bridgeConfiguration.replaceBridge(
                "edg-882-writeback",
                updatedCopy(bridgeConfiguration.getBridges().get(0)));

        assertThat(replaced).isTrue();
        assertThat(notifiedBridgeIds).containsExactly(List.of("edg-882-writeback"));
        assertThat(bridgeConfiguration.getBridges()).hasSize(1);
        assertThat(bridgeConfiguration
                        .getBridges()
                        .get(0)
                        .getLocalSubscriptions()
                        .get(0)
                        .getFilters())
                .containsExactly("changed/#");
    }

    @Test
    public void whenAnUnknownBridgeIsReplaced_thenNothingChanges() throws IOException {
        Files.write(CONFIG.getBytes(UTF_8), xmlFile);
        reader.applyConfig();
        final MqttBridge before = bridgeConfiguration.getBridges().get(0);
        final List<List<String>> notified = new ArrayList<>();
        bridgeConfiguration.registerConsumer(
                bridges -> notified.add(bridges.stream().map(MqttBridge::getId).toList()));
        notified.clear();

        // The body carries the id being replaced: replaceBridge requires the two to agree, because a
        // mismatch turns an update into a remove + add and clears the queues of the bridge it was meant
        // to update (EDG-882 review v02, R2-09).
        final boolean replaced =
                bridgeConfiguration.replaceBridge("no-such-bridge", copyWithId(updatedCopy(before), "no-such-bridge"));

        assertThat(replaced).isFalse();
        assertThat(notified).isEmpty();
        assertThat(bridgeConfiguration.getBridges()).containsExactly(before);
    }

    /** The replacement keeps its position among the other bridges. */
    @Test
    public void whenABridgeIsReplaced_thenTheOrderOfTheOthersIsKept() throws IOException {
        Files.write(CONFIG.getBytes(UTF_8), xmlFile);
        reader.applyConfig();
        final MqttBridge first = bridgeConfiguration.getBridges().get(0);
        bridgeConfiguration.addBridge(copyWithId(first, "edg-882-second"));
        bridgeConfiguration.addBridge(copyWithId(first, "edg-882-third"));

        bridgeConfiguration.replaceBridge("edg-882-second", updatedCopy(copyWithId(first, "edg-882-second")));

        assertThat(bridgeConfiguration.getBridges().stream().map(MqttBridge::getId))
                .containsExactly("edg-882-writeback", "edg-882-second", "edg-882-third");
    }

    /**
     * The id in the body has to be the id being replaced (EDG-882 review v02, R2-09).
     * <p>
     * This method exists to keep a bridge <em>present</em> across an update, because a bridge that
     * disappears from the configuration is stopped with an empty retain list and loses every queue it
     * owns. A body carrying a different id defeats exactly that: the list comes out without the old id
     * and with a new one, which {@code updateBridges} reads as a removal followed by an addition. The
     * REST layer rejects an id change, so today no caller does it — the precondition is here so that the
     * next one cannot either, silently.
     */
    @Test
    public void whenTheBodyCarriesADifferentId_thenTheReplacementIsRefused() throws IOException {
        Files.write(CONFIG.getBytes(UTF_8), xmlFile);
        reader.applyConfig();
        final MqttBridge before = bridgeConfiguration.getBridges().get(0);

        assertThatThrownBy(
                        () -> bridgeConfiguration.replaceBridge(before.getId(), copyWithId(before, "a-different-id")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(before.getId())
                .hasMessageContaining("a-different-id");

        assertThat(bridgeConfiguration.getBridges().stream().map(MqttBridge::getId))
                .as("a refused replacement must not have changed the configuration")
                .containsExactly(before.getId());
    }

    /**
     * A {@code <forwarded-topic>} with no {@code <max-qos>} forwards at QoS <b>0</b>, not at the
     * schema's documented default of 2 (EDG-882 review v02, R2-17).
     * <p>
     * {@code ForwardedTopicEntity.maxQoS} is a bare {@code int} with no initialiser and the element is
     * {@code minOccurs="0"}; the "Default: 2" in {@code config.xsd} is an {@code xs:annotation}, which is
     * documentation and not an XSD {@code default}, so JAXB leaves the field at the Java default and
     * {@code RemoteMqttForwarder.convertQos} then downgrades every forwarded message to fire-and-forget.
     * <p>
     * Pinned here because it is silent: a configuration or a test fixture that omits the element looks
     * like it asks for QoS 2 and delivers at QoS 0, and the only symptom is messages that occasionally
     * are not there. Three fixtures on this branch had exactly that.
     */
    @Test
    public void whenMaxQosIsOmitted_thenTheSubscriptionForwardsAtQosZero() throws IOException {
        Files.write(CONFIG.replace("<max-qos>1</max-qos>", "").getBytes(UTF_8), xmlFile);
        reader.applyConfig();

        assertThat(bridgeConfiguration
                        .getBridges()
                        .get(0)
                        .getLocalSubscriptions()
                        .get(0)
                        .getMaxQoS())
                .as("an absent <max-qos> is QoS 0, whatever the schema documentation says")
                .isZero();
    }

    /** And the value is honoured when it is written, which is what the fixtures must do. */
    @Test
    public void whenMaxQosIsGiven_thenTheSubscriptionForwardsAtThatQos() throws IOException {
        Files.write(CONFIG.getBytes(UTF_8), xmlFile);
        reader.applyConfig();

        assertThat(bridgeConfiguration
                        .getBridges()
                        .get(0)
                        .getLocalSubscriptions()
                        .get(0)
                        .getMaxQoS())
                .isEqualTo(1);
    }

    private static @NotNull MqttBridge updatedCopy(final @NotNull MqttBridge bridge) {
        return builderOf(bridge)
                .withLocalSubscriptions(List.of(new LocalSubscription(List.of("changed/#"), "{#}")))
                .build();
    }

    private static @NotNull MqttBridge copyWithId(final @NotNull MqttBridge bridge, final @NotNull String id) {
        return builderOf(bridge).withId(id).withClientId(id).build();
    }

    private static MqttBridge.@NotNull Builder builderOf(final @NotNull MqttBridge bridge) {
        return new MqttBridge.Builder()
                .withId(bridge.getId())
                .withHost(bridge.getHost())
                .withPort(bridge.getPort())
                .withClientId(bridge.getClientId())
                .withLocalSubscriptions(bridge.getLocalSubscriptions())
                .withRemoteSubscriptions(bridge.getRemoteSubscriptions());
    }
}
