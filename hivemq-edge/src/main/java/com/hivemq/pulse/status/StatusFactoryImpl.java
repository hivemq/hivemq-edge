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
package com.hivemq.pulse.status;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.List;
import org.jetbrains.annotations.NotNull;

@Singleton
public class StatusFactoryImpl implements StatusFactory {

    @Inject
    public StatusFactoryImpl() {}

    @Override
    public @NotNull PulseAgentStatus create(
            final @NotNull PulseAgentStatus.Status status,
            final @NotNull List<String> errorMessages) {
        return new PulseAgentStatusImpl(status, errorMessages);
    }
}
