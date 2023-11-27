package com.hivemq.edge.impl.capability;

import com.hivemq.api.model.capabilities.Capability;
import com.hivemq.api.model.capabilities.CapabilityList;
import com.hivemq.edge.HiveMQCapabilityService;
import com.hivemq.extension.sdk.api.annotations.NotNull;

import javax.inject.Inject;
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
    public synchronized void addCapability(@NotNull final Capability capability) {
        capabilities.add(capability);
    }
}
