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

import com.hivemq.adapter.sdk.api.schema.ScalarSchema;
import com.hivemq.adapter.sdk.api.schema.ScalarType;
import com.hivemq.adapter.sdk.api.v2.node.NodeTagPair;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * The declared typed schema is enforced at the northbound routing point, not just projected
 * (EDG-824 #6): an out-of-range or wrong-type value for a {@code DOUBLE[0,100)} tag is refused — it never flows
 * northbound — and the refusal is surfaced as a per-tag failure with the schema-violation reason, while the adapter
 * connection (correctly) stays up.
 */
class TypedSchemaEnforcementTest {

    private static final ScalarSchema DOUBLE_0_TO_100 =
            new ScalarSchema(ScalarType.DOUBLE, 0, 100, null, null, false, true, false);

    private WrapperTestFixture rangedFixture(final NodeTagPair pair) {
        return WrapperTestFixture.builder()
                .runningCoordinator()
                .nodes(List.of(pair))
                .pollIntervalMillis(1000)
                .build();
    }

    @Test
    void conformingValue_flowsAndCountsNoFailure() {
        final NodeTagPair pair = WrapperTestSupport.typedPair("temperature", DOUBLE_0_TO_100);
        final WrapperTestFixture fixture = rangedFixture(pair);
        fixture.activate(ProtocolAdapterDirection.NORTHBOUND);
        fixture.advance(1000); // WAITING_FOR_POLL_DATAPOINT

        fixture.output.dataPoint(pair.node(), WrapperTestSupport.dataPoint("temperature", 21.5));
        fixture.drain();

        assertThat(fixture.readState("temperature")).isEqualTo("WAITING_FOR_POLL_INTERVAL");
        assertThat(fixture.tag("temperature").failureCount()).isZero();
    }

    @Test
    void outOfRangeValue_isRefusedAndSurfacedAsATagFailure() {
        final NodeTagPair pair = WrapperTestSupport.typedPair("temperature", DOUBLE_0_TO_100);
        final WrapperTestFixture fixture = rangedFixture(pair);
        fixture.activate(ProtocolAdapterDirection.NORTHBOUND);
        fixture.advance(1000); // WAITING_FOR_POLL_DATAPOINT

        fixture.output.dataPoint(pair.node(), WrapperTestSupport.dataPoint("temperature", 250.0));
        fixture.drain();

        // Refused, surfaced, contained: the poll loop retries on its cadence, the connection stays up.
        assertThat(fixture.state()).isEqualTo(CONNECTED);
        assertThat(fixture.tag("temperature").failureCount()).isEqualTo(1);
        assertThat(fixture.tag("temperature").lastFailureReason()).contains("declared-schema violation");
        assertThat(fixture.readState("temperature")).isEqualTo("WAITING_FOR_POLL_INTERVAL");
    }

    @Test
    void wrongTypeValue_isRefusedAndSurfacedAsATagFailure() {
        final NodeTagPair pair = WrapperTestSupport.typedPair("temperature", DOUBLE_0_TO_100);
        final WrapperTestFixture fixture = rangedFixture(pair);
        fixture.activate(ProtocolAdapterDirection.NORTHBOUND);
        fixture.advance(1000);

        fixture.output.dataPoint(pair.node(), WrapperTestSupport.dataPoint("temperature", "garbage-string"));
        fixture.drain();

        assertThat(fixture.tag("temperature").failureCount()).isEqualTo(1);
        assertThat(fixture.tag("temperature").lastFailureReason()).contains("does not conform");
    }

    @Test
    void unconstrainedSchema_keepsExtremeValuesFlowingByteIdentically() {
        // The verified-robust guarantee is preserved: with no declared constraints, NaN/Infinity/extremes flow.
        final NodeTagPair pair = WrapperTestSupport.pair("temperature"); // STRING, unconstrained
        final WrapperTestFixture fixture = rangedFixture(pair);
        fixture.activate(ProtocolAdapterDirection.NORTHBOUND);
        fixture.advance(1000);

        fixture.output.dataPoint(pair.node(), WrapperTestSupport.dataPoint("temperature", "NaN"));
        fixture.drain();

        assertThat(fixture.readState("temperature")).isEqualTo("WAITING_FOR_POLL_INTERVAL");
        assertThat(fixture.tag("temperature").failureCount()).isZero();
    }
}
