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
package com.hivemq.datagov;

import com.google.common.util.concurrent.ListenableFuture;
import com.hivemq.api.mqtt.PublishReturnCode;
import com.hivemq.datagov.model.DataGovernanceResult;
import com.hivemq.mqtt.handler.publish.PublishingResult;
import org.jetbrains.annotations.NotNull;

public interface DataGovernanceService {

    @NotNull ListenableFuture<DataGovernanceResult> apply(@NotNull DataGovernanceContext governanceContext);

    @NotNull ListenableFuture<PublishingResult> applyAndPublish(@NotNull DataGovernanceContext governanceContext);

}


