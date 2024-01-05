package com.hivemq.edge;

import com.hivemq.api.model.capabilities.Capability;
import com.hivemq.api.model.capabilities.CapabilityList;
import com.hivemq.extension.sdk.api.annotations.NotNull;

public interface HiveMQCapabilityService {

    @NotNull CapabilityList getList();

    void addCapability(@NotNull Capability capability);

}
