package com.hivemq.protocols;

import com.hivemq.adapter.sdk.api.config.PollingContext;
import com.hivemq.adapter.sdk.api.config.ProtocolSpecificAdapterConfig;
import com.hivemq.adapter.sdk.api.factories.AdapterFactories;
import com.hivemq.adapter.sdk.api.model.ProtocolAdapterInput;
import com.hivemq.adapter.sdk.api.services.ModuleServices;
import com.hivemq.adapter.sdk.api.services.ProtocolAdapterMetricsService;
import com.hivemq.adapter.sdk.api.state.ProtocolAdapterState;
import com.hivemq.adapter.sdk.api.tag.Tag;
import com.hivemq.edge.modules.adapters.impl.factories.AdapterFactoriesImpl;
import com.hivemq.persistence.mappings.NorthboundMapping;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.stream.Collectors;

public class ProtocolAdapterInputImpl<T extends ProtocolSpecificAdapterConfig> implements ProtocolAdapterInput<T> {
    public static final AdapterFactoriesImpl ADAPTER_FACTORIES = new AdapterFactoriesImpl();
    private final String adapterId;
    private final @NotNull T configObject;
    private final @NotNull String version;
    private final @NotNull ProtocolAdapterState protocolAdapterState;
    private final @NotNull ModuleServices moduleServices;
    private final @NotNull ProtocolAdapterMetricsService protocolAdapterMetricsService;
    private final @NotNull List<Tag> tags;
    private final @NotNull List<PollingContext> pollingContexts;

    public ProtocolAdapterInputImpl(
            final @NotNull String adapterId,
            final @NotNull T configObject,
            final @NotNull List<Tag> tags,
            final @NotNull List<NorthboundMapping> northboundMappings,
            final @NotNull String version,
            final @NotNull ProtocolAdapterState protocolAdapterState,
            final @NotNull ModuleServices moduleServices,
            final @NotNull ProtocolAdapterMetricsService protocolAdapterMetricsService) {
        this.adapterId = adapterId;
        this.configObject = configObject;
        this.version = version;
        this.protocolAdapterState = protocolAdapterState;
        this.moduleServices = moduleServices;
        this.protocolAdapterMetricsService = protocolAdapterMetricsService;
        this.tags = tags;
        this.pollingContexts =
                northboundMappings.stream().map(PollingContextWrapper::from).collect(Collectors.toList());
    }

    @Override
    public @NotNull String getAdapterId() {
        return adapterId;
    }

    @Override
    public @NotNull T getConfig() {
        return configObject;
    }

    @Override
    public @NotNull String getVersion() {
        return version;
    }

    @Override
    public @NotNull ProtocolAdapterState getProtocolAdapterState() {
        return protocolAdapterState;
    }

    @Override
    public @NotNull ModuleServices moduleServices() {
        return moduleServices;
    }

    @Override
    public @NotNull AdapterFactories adapterFactories() {
        return ADAPTER_FACTORIES;
    }

    @Override
    public @NotNull ProtocolAdapterMetricsService getProtocolAdapterMetricsHelper() {
        return protocolAdapterMetricsService;
    }

    @Override
    public @NotNull List<Tag> getTags() {
        return tags;
    }

    @Override
    public @NotNull List<PollingContext> getPollingContexts() {
        return pollingContexts;
    }
}
