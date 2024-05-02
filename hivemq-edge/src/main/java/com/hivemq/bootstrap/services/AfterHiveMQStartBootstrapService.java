package com.hivemq.bootstrap.services;

import com.hivemq.edge.ModulesAndExtensionsService;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.protocols.ProtocolAdapterManager;

public interface AfterHiveMQStartBootstrapService extends CompleteBootstrapService {

    @NotNull ProtocolAdapterManager protocolAdapterManager();

    @NotNull ModulesAndExtensionsService modulesAndExtensionsService();
}
