package com.hivemq.edge.impl.capability;

import com.hivemq.api.model.capabilities.Capability;
import com.hivemq.api.model.capabilities.CapabilityList;
import com.hivemq.edge.HiveMQCapabilityService;
import com.hivemq.extension.sdk.api.annotations.NotNull;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class CapabilityServiceImpl implements HiveMQCapabilityService {

    private final @NotNull Set<Capability> capabilities = new HashSet<>();

    @Inject
    public CapabilityServiceImpl() {
    }

    @Override
    public @NotNull CapabilityList getList() {
        return new CapabilityList(new ArrayList<>(capabilities));
    }

    @Override
    public void addCapability(@NotNull final Capability capability) {
        capabilities.add(capability);
    }
}
