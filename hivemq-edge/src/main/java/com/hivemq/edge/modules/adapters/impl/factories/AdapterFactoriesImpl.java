package com.hivemq.edge.modules.adapters.impl.factories;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hivemq.api.error.ApiException;
import com.hivemq.api.model.core.Payload;
import com.hivemq.api.model.core.PayloadImpl;
import com.hivemq.edge.model.TypeIdentifier;
import com.hivemq.edge.model.TypeIdentifierImpl;
import com.hivemq.edge.modules.adapters.data.DataPointImpl;
import com.hivemq.edge.modules.adapters.factories.AdapterFactories;
import com.hivemq.edge.modules.adapters.factories.AdapterSubscriptionFactory;
import com.hivemq.edge.modules.adapters.factories.ApiExceptionFactory;
import com.hivemq.edge.modules.adapters.factories.DataPointFactory;
import com.hivemq.edge.modules.adapters.factories.EventBuilderFactory;
import com.hivemq.edge.modules.adapters.factories.PayloadFactory;
import com.hivemq.edge.modules.adapters.factories.TypeIdentifierFactory;
import com.hivemq.edge.modules.adapters.factories.UserPropertyFactory;
import com.hivemq.edge.modules.api.events.model.EventBuilder;
import com.hivemq.edge.modules.api.events.model.EventBuilderImpl;
import com.hivemq.edge.modules.config.impl.AdapterSubscriptionImpl;
import com.hivemq.edge.modules.config.impl.UserPropertyImpl;
import com.hivemq.extension.sdk.api.annotations.NotNull;

public class AdapterFactoriesImpl implements AdapterFactories {

    @Override
    public @NotNull AdapterSubscriptionFactory adapterSubscriptionFactory() {
        return AdapterSubscriptionImpl::new;

    }

    @Override
    public @NotNull ApiExceptionFactory apiExceptionFactory(final @NotNull String message) {
        return () -> new ApiException(message);
    }

    @Override
    public @NotNull PayloadFactory payloadFactory() {
        return new PayloadFactory() {
            @Override
            public @NotNull Payload create(
                    final Payload.@NotNull ContentType contentType,
                    final @NotNull String content) {
                return PayloadImpl.from(contentType, content);
            }

            @Override
            public @NotNull Payload create(final @NotNull ObjectMapper mapper, final @NotNull Object data) {
                return PayloadImpl.fromObject(mapper, data);
            }
        };
    }

    @Override
    public @NotNull TypeIdentifierFactory TypeIdentifierFactory() {
        return TypeIdentifierImpl::new;
    }

    @Override
    public @NotNull UserPropertyFactory userPropertyFactory() {
        return UserPropertyImpl::new;
    }

    @Override
    public @NotNull EventBuilderFactory eventBuilderFactory() {
        return (final @NotNull String id, final @NotNull String protocolId) -> {

            final EventBuilder eventBuilder = new EventBuilderImpl()
                    .withTimestamp(System.currentTimeMillis())
                    .withSource(TypeIdentifierImpl.create(TypeIdentifier.TYPE.ADAPTER, id))
                    .withAssociatedObject(TypeIdentifierImpl.create(TypeIdentifier.TYPE.ADAPTER_TYPE, protocolId));
            return eventBuilder;
        };
    }

    @Override
    public @NotNull DataPointFactory dataPointFactory() {
        return DataPointImpl::new;
    }
}