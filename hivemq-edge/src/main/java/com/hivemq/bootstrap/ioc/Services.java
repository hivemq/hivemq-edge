package com.hivemq.bootstrap.ioc;

import com.hivemq.edge.HiveMQCapabilityService;
import com.hivemq.extension.sdk.api.annotations.NotNull;

import javax.inject.Inject;

public class Services {


    private final @NotNull HiveMQCapabilityService capabilityService;

    @Inject
    public Services(final @NotNull HiveMQCapabilityService capabilityService) {
        this.capabilityService = capabilityService;
    }

    public @NotNull HiveMQCapabilityService capabilityService() {
        return capabilityService;
    }
}
