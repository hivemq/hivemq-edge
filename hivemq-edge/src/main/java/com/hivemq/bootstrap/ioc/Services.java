package com.hivemq.bootstrap.ioc;

import com.hivemq.bridge.BridgeService;
import com.hivemq.edge.HiveMQCapabilityService;
import com.hivemq.extension.sdk.api.annotations.NotNull;

import javax.inject.Inject;

public class Services {


    private final @NotNull HiveMQCapabilityService capabilityService;
    private final @NotNull BridgeService bridgeService;


    @Inject
    public Services(final @NotNull HiveMQCapabilityService capabilityService,
                    final @NotNull BridgeService bridgeService) {
        this.capabilityService = capabilityService;
        this.bridgeService = bridgeService;
    }

    public @NotNull HiveMQCapabilityService capabilityService() {
        return capabilityService;
    }

    public @NotNull BridgeService bridgeService() {
        return bridgeService;
    }
}
