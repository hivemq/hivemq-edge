package com.hivemq.edge.modules.adapters.impl.factories;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hivemq.api.model.core.PayloadImpl;
import com.hivemq.edge.model.TypeIdentifierImpl;
import com.hivemq.edge.modules.adapters.data.DataPointImpl;
import com.hivemq.edge.modules.api.events.model.EventBuilderImpl;
import com.hivemq.edge.modules.config.impl.AdapterSubscriptionImpl;
import com.hivemq.extension.sdk.api.adapters.factories.AdapterFactories;
import com.hivemq.extension.sdk.api.adapters.factories.AdapterSubscriptionFactory;
import com.hivemq.extension.sdk.api.adapters.factories.DataPointFactory;
import com.hivemq.extension.sdk.api.adapters.factories.EventBuilderFactory;
import com.hivemq.extension.sdk.api.adapters.factories.PayloadFactory;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.events.model.Payload;
import com.hivemq.extension.sdk.api.events.model.TypeIdentifier;

public class AdapterFactoriesImpl implements AdapterFactories {

    @Override
    public @NotNull AdapterSubscriptionFactory adapterSubscriptionFactory() {
        return AdapterSubscriptionImpl::new;

    }

    @Override
    public @NotNull PayloadFactory payloadFactory() {
        return new PayloadFactory() {
            @Override
            public @NotNull Payload create(
                    final Payload.@NotNull ContentType contentType, final @NotNull String content) {
                return PayloadImpl.from(contentType, content);
            }

            @Override
            public @NotNull Payload create(final @NotNull ObjectMapper mapper, final @NotNull Object data) {
                return PayloadImpl.fromObject(mapper, data);
            }
        };
    }

    @Override
    public @NotNull EventBuilderFactory eventBuilderFactory() {
        return (final @NotNull String adapterId, final @NotNull String protocolId) -> new EventBuilderImpl().withTimestamp(
                        System.currentTimeMillis())
                .withSource(TypeIdentifierImpl.create(TypeIdentifier.Type.ADAPTER, adapterId))
                .withAssociatedObject(TypeIdentifierImpl.create(TypeIdentifier.Type.ADAPTER_TYPE, protocolId));
    }

    @Override
    public @NotNull DataPointFactory dataPointFactory() {
        return DataPointImpl::new;
    }
}
