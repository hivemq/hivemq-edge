package com.hivemq.bootstrap.ioc;

import com.hivemq.bridge.BridgeService;
import com.hivemq.edge.HiveMQCapabilityService;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.persistence.SingleWriterService;

import javax.inject.Inject;

public class Services {


    private final @NotNull HiveMQCapabilityService capabilityService;
    private final @NotNull BridgeService bridgeService;
    private final @NotNull SingleWriterService singleWriterService;

    @Inject
    public Services(final @NotNull HiveMQCapabilityService capabilityService,
                    final @NotNull BridgeService bridgeService, final @NotNull SingleWriterService singleWriterService) {
        this.capabilityService = capabilityService;
        this.bridgeService = bridgeService;
        this.singleWriterService = singleWriterService;
    }

    public @NotNull HiveMQCapabilityService capabilityService() {
        return capabilityService;
    }

    public @NotNull BridgeService bridgeService() {
        return bridgeService;
    }

    public @NotNull SingleWriterService singleWriterService() {
        return singleWriterService;
    }
}
