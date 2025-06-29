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
package com.hivemq.edge.impl.capability;

import com.hivemq.api.model.capabilities.Capability;
import com.hivemq.api.model.capabilities.CapabilityList;
import com.hivemq.edge.HiveMQCapabilityService;
import org.jetbrains.annotations.NotNull;

import jakarta.inject.Inject;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

/**
 * Via this Service components can add capabilities to Edge, which can be retrieved via REST API.
 * The methods are synchronized as they are invoked rarely (Good enough).
 */
public class CapabilityServiceImpl implements HiveMQCapabilityService {

    private final @NotNull Set<Capability> capabilities = new HashSet<>();

    @Inject
    public CapabilityServiceImpl() {
    }

    @Override
    public @NotNull synchronized CapabilityList getList() {
        return new CapabilityList(new ArrayList<>(capabilities));
    }

    @Override
    public synchronized void addCapability(final @NotNull Capability capability) {
        capabilities.add(capability);
    }
}
