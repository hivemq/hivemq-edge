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
package com.hivemq.protocols.v2.manager;

import static com.hivemq.protocols.v2.manager.ProtocolAdapterManagerTestSupport.adapter;
import static com.hivemq.protocols.v2.manager.ProtocolAdapterManagerTestSupport.tag;
import static org.assertj.core.api.Assertions.assertThat;

import com.hivemq.protocols.v2.config.ProtocolAdapterEntity;
import com.hivemq.protocols.v2.config.RetryPolicyEntity;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * The config-difference classifier (design §8.2): every transition class, the layering that lets the more
 * disruptive change win, and the adapter-direction predicate the manager uses to compose a tags-only transition.
 */
class ProtocolAdapterConfigDiffUtilsTest {

    @Test
    void identicalConfigurations_areNoChange() {
        final ProtocolAdapterEntity running =
                adapter("a").northboundMapping("temperature", "t/a").build();
        final ProtocolAdapterEntity updated =
                adapter("a").northboundMapping("temperature", "t/a").build();

        assertThat(ProtocolAdapterConfigDiffUtils.classify(running, updated))
                .isEqualTo(ProtocolAdapterConfigStateTransition.NO_CHANGE);
    }

    @Test
    void flippedAdapterDirection_isActivationOnly() {
        final ProtocolAdapterEntity running =
                adapter("a").southboundActivated(false).build();
        final ProtocolAdapterEntity updated =
                adapter("a").southboundActivated(true).build();

        assertThat(ProtocolAdapterConfigDiffUtils.classify(running, updated))
                .isEqualTo(ProtocolAdapterConfigStateTransition.ACTIVATION_ONLY);
    }

    @Test
    void flippedTagAspectActivation_isActivationOnly() {
        final ProtocolAdapterEntity running = adapter("a")
                .tags(tag("temperature").readActivated(true).build())
                .build();
        final ProtocolAdapterEntity updated = adapter("a")
                .tags(tag("temperature").readActivated(false).build())
                .build();

        assertThat(ProtocolAdapterConfigDiffUtils.classify(running, updated))
                .isEqualTo(ProtocolAdapterConfigStateTransition.ACTIVATION_ONLY);
    }

    @Test
    void addedTag_isTagsOnly() {
        final ProtocolAdapterEntity running =
                adapter("a").tags(tag("temperature").build()).build();
        final ProtocolAdapterEntity updated = adapter("a")
                .tags(tag("temperature").build(), tag("pressure").build())
                .build();

        assertThat(ProtocolAdapterConfigDiffUtils.classify(running, updated))
                .isEqualTo(ProtocolAdapterConfigStateTransition.TAGS_ONLY);
    }

    @Test
    void changedNodeString_isTagsOnly() {
        final ProtocolAdapterEntity running = adapter("a")
                .tags(tag("temperature").nodeString("{\"identifier\":\"x\"}").build())
                .build();
        final ProtocolAdapterEntity updated = adapter("a")
                .tags(tag("temperature").nodeString("{\"identifier\":\"y\"}").build())
                .build();

        assertThat(ProtocolAdapterConfigDiffUtils.classify(running, updated))
                .isEqualTo(ProtocolAdapterConfigStateTransition.TAGS_ONLY);
    }

    @Test
    void addedMappingThatFlipsUsed_isTagsOnly() {
        // S16: a northbound mapping added in config flips the tag's readUsed — a tags-only transition (UpdateTagSet
        // recomputes used), never a reconnect.
        final ProtocolAdapterEntity running = adapter("a").build();
        final ProtocolAdapterEntity updated =
                adapter("a").northboundMapping("temperature", "t/a").build();

        assertThat(ProtocolAdapterConfigDiffUtils.classify(running, updated))
                .isEqualTo(ProtocolAdapterConfigStateTransition.TAGS_ONLY);
    }

    @Test
    void changedProtocolId_isFullRecreate() {
        final ProtocolAdapterEntity running = adapter("a").protocolId("test").build();
        final ProtocolAdapterEntity updated = adapter("a").protocolId("other").build();

        assertThat(ProtocolAdapterConfigDiffUtils.classify(running, updated))
                .isEqualTo(ProtocolAdapterConfigStateTransition.FULL_RECREATE);
    }

    @Test
    void changedAdapterConfiguration_isFullRecreate() {
        final ProtocolAdapterEntity running =
                adapter("a").adapterConfiguration(Map.of("host", "a")).build();
        final ProtocolAdapterEntity updated =
                adapter("a").adapterConfiguration(Map.of("host", "b")).build();

        assertThat(ProtocolAdapterConfigDiffUtils.classify(running, updated))
                .isEqualTo(ProtocolAdapterConfigStateTransition.FULL_RECREATE);
    }

    @Test
    void changedRetryPolicy_isFullRecreate() {
        final ProtocolAdapterEntity running = adapter("a")
                .retryPolicy(new RetryPolicyEntity(1000, 1.41, 32000, 3))
                .build();
        final ProtocolAdapterEntity updated = adapter("a")
                .retryPolicy(new RetryPolicyEntity(2000, 1.41, 32000, 3))
                .build();

        assertThat(ProtocolAdapterConfigDiffUtils.classify(running, updated))
                .isEqualTo(ProtocolAdapterConfigStateTransition.FULL_RECREATE);
    }

    @Test
    void changedWatchdogTimeout_isFullRecreate() {
        final ProtocolAdapterEntity running =
                adapter("a").watchdogTimeoutMillis(30000).build();
        final ProtocolAdapterEntity updated =
                adapter("a").watchdogTimeoutMillis(20000).build();

        assertThat(ProtocolAdapterConfigDiffUtils.classify(running, updated))
                .isEqualTo(ProtocolAdapterConfigStateTransition.FULL_RECREATE);
    }

    @Test
    void changedCommandTimeout_isFullRecreate() {
        final ProtocolAdapterEntity running =
                adapter("a").commandTimeoutMillis(10000).build();
        final ProtocolAdapterEntity updated =
                adapter("a").commandTimeoutMillis(5000).build();

        assertThat(ProtocolAdapterConfigDiffUtils.classify(running, updated))
                .isEqualTo(ProtocolAdapterConfigStateTransition.FULL_RECREATE);
    }

    @Test
    void changedSkipVerification_isFullRecreate() {
        final ProtocolAdapterEntity running =
                adapter("a").skipVerification(false).build();
        final ProtocolAdapterEntity updated =
                adapter("a").skipVerification(true).build();

        assertThat(ProtocolAdapterConfigDiffUtils.classify(running, updated))
                .isEqualTo(ProtocolAdapterConfigStateTransition.FULL_RECREATE);
    }

    @Test
    void changedConfigVersion_isFullRecreate() {
        final ProtocolAdapterEntity running = adapter("a").configVersion(2).build();
        final ProtocolAdapterEntity updated = adapter("a").configVersion(3).build();

        assertThat(ProtocolAdapterConfigDiffUtils.classify(running, updated))
                .isEqualTo(ProtocolAdapterConfigStateTransition.FULL_RECREATE);
    }

    @Test
    void connectionCriticalChange_winsOverActivationChange() {
        final ProtocolAdapterEntity running =
                adapter("a").protocolId("test").southboundActivated(false).build();
        final ProtocolAdapterEntity updated =
                adapter("a").protocolId("other").southboundActivated(true).build();

        assertThat(ProtocolAdapterConfigDiffUtils.classify(running, updated))
                .isEqualTo(ProtocolAdapterConfigStateTransition.FULL_RECREATE);
    }

    @Test
    void tagSetChange_winsOverActivationChange() {
        final ProtocolAdapterEntity running = adapter("a")
                .southboundActivated(false)
                .tags(tag("temperature").build())
                .build();
        final ProtocolAdapterEntity updated = adapter("a")
                .southboundActivated(true)
                .tags(tag("temperature").build(), tag("pressure").build())
                .build();

        assertThat(ProtocolAdapterConfigDiffUtils.classify(running, updated))
                .isEqualTo(ProtocolAdapterConfigStateTransition.TAGS_ONLY);
    }

    @Test
    void adapterDirectionChanged_tracksTheDirectionFlags() {
        final ProtocolAdapterEntity running = adapter("a")
                .northboundActivated(true)
                .southboundActivated(false)
                .build();
        final ProtocolAdapterEntity sameDirections = adapter("a")
                .northboundActivated(true)
                .southboundActivated(false)
                .build();
        final ProtocolAdapterEntity flipped =
                adapter("a").northboundActivated(true).southboundActivated(true).build();

        assertThat(ProtocolAdapterConfigDiffUtils.adapterDirectionChanged(running, sameDirections))
                .isFalse();
        assertThat(ProtocolAdapterConfigDiffUtils.adapterDirectionChanged(running, flipped))
                .isTrue();
    }
}
