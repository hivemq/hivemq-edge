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
package com.hivemq.edge.adapters.opcua.client;

import static com.hivemq.edge.adapters.opcua.client.EdgeOpcUaClient.DEFAULT_OPERATIONS_PER_REQUEST;
import static com.hivemq.edge.adapters.opcua.client.EdgeOpcUaClient.withDefaultBatchSizes;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.Unsigned.uint;

import org.eclipse.milo.opcua.sdk.client.OperationLimits;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UInteger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;

class EdgeOpcUaClientTest {

    @Test
    void zeroReadAndBrowseLimits_becomeTheDefault() {
        final OperationLimits limits = withDefaultBatchSizes(readAndBrowse(uint(0), uint(0)));

        assertThat(limits.maxNodesPerRead()).contains(uint(DEFAULT_OPERATIONS_PER_REQUEST));
        assertThat(limits.maxNodesPerBrowse()).contains(uint(DEFAULT_OPERATIONS_PER_REQUEST));
    }

    @Test
    void absentReadAndBrowseLimits_becomeTheDefault() {
        final OperationLimits limits = withDefaultBatchSizes(readAndBrowse(null, null));

        assertThat(limits.maxNodesPerRead()).contains(uint(DEFAULT_OPERATIONS_PER_REQUEST));
        assertThat(limits.maxNodesPerBrowse()).contains(uint(DEFAULT_OPERATIONS_PER_REQUEST));
    }

    @Test
    void advertisedReadAndBrowseLimits_areKept() {
        final OperationLimits limits = withDefaultBatchSizes(readAndBrowse(uint(33), UInteger.MAX));

        assertThat(limits.maxNodesPerRead()).contains(uint(33));
        assertThat(limits.maxNodesPerBrowse()).contains(UInteger.MAX);
    }

    @Test
    void otherLimits_areKept_includingZeroAndAbsent() {
        final OperationLimits limits = withDefaultBatchSizes(new OperationLimits(
                null, uint(1), uint(0), null, uint(2), uint(3), uint(4), uint(0), uint(5), uint(6), null, uint(7)));

        assertThat(limits.maxNodesPerWrite()).contains(uint(1));
        assertThat(limits.maxNodesPerMethodCall()).contains(uint(0));
        assertThat(limits.maxNodesPerRegisterNodes()).contains(uint(2));
        assertThat(limits.maxNodesPerTranslateBrowsePathsToNodeIds()).contains(uint(3));
        assertThat(limits.maxNodesPerNodeManagement()).contains(uint(4));
        // Milo reads 0 here as unlimited; a batch size would cap monitored-item creation.
        assertThat(limits.maxMonitoredItemsPerCall()).contains(uint(0));
        assertThat(limits.maxNodesPerHistoryReadData()).contains(uint(5));
        assertThat(limits.maxNodesPerHistoryReadEvents()).contains(uint(6));
        assertThat(limits.maxNodesPerHistoryUpdateData()).isEmpty();
        assertThat(limits.maxNodesPerHistoryUpdateEvents()).contains(uint(7));
    }

    private static @NotNull OperationLimits readAndBrowse(
            final @Nullable UInteger maxNodesPerRead, final @Nullable UInteger maxNodesPerBrowse) {
        return new OperationLimits(
                maxNodesPerRead, null, null, maxNodesPerBrowse, null, null, null, null, null, null, null, null);
    }
}
