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
package com.hivemq.context;

import com.google.common.util.concurrent.ListenableFuture;
import com.hivemq.context.model.Result;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.mqtt.handler.publish.PublishReturnCode;

public interface HiveMQEdgeService {

    @NotNull ListenableFuture<Result> apply(@NotNull HiveMQEdgeContext hiveMQEdgeContext);

    @NotNull ListenableFuture<PublishReturnCode> applyAndPublish(@NotNull HiveMQEdgeContext hiveMQEdgeContext);

}


