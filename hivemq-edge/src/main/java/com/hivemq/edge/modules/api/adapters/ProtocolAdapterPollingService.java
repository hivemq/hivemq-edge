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
package com.hivemq.edge.modules.api.adapters;

import com.hivemq.edge.modules.adapters.params.ProtocolAdapterPollingInput;
import com.hivemq.edge.modules.adapters.params.ProtocolAdapterPollingOutput;
import com.hivemq.extension.sdk.api.annotations.NotNull;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * The Polling Service allows Protocol Adapters to use a centrally managed and tracked Scheduler
 * which means all processes can be managed via API / UX.
 *
 * @author Simon L Johnson
 */
public interface ProtocolAdapterPollingService {

    ProtocolAdapterPollingOutput schedulePolling(@NotNull ProtocolAdapter adapter,
                                                 @NotNull ProtocolAdapterPollingInput input);
    Optional<ProtocolAdapterPollingOutput> getPollingJob(@NotNull UUID id);

    List<ProtocolAdapterPollingOutput> getActiveProcesses();

    List<ProtocolAdapterPollingOutput> getPollingJobsForAdapter(@NotNull String adapterId);

    int currentErrorCount(@NotNull ProtocolAdapterPollingOutput pollingJob);

    void stopPolling(@NotNull ProtocolAdapterPollingOutput pollingJob);

    void stopPollingForAdapterInstance(@NotNull ProtocolAdapter adapter);

    void stopAllPolling();
}
