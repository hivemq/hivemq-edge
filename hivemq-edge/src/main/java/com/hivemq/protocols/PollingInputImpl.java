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
package com.hivemq.protocols;

import com.hivemq.adapter.sdk.api.config.PollingContext;
import com.hivemq.adapter.sdk.api.polling.PollingInput;
import com.hivemq.extension.sdk.api.annotations.NotNull;

public class PollingInputImpl<T extends  PollingContext> implements PollingInput<T> {

    private final @NotNull T pollingContext;

    public PollingInputImpl(
            final @NotNull T pollingContext) {
        this.pollingContext = pollingContext;
    }

    @Override
    public @NotNull T getPollingContext() {
        return pollingContext;
    }
}
