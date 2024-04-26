package com.hivemq.edge.modules.adapters.impl.factories;

import com.hivemq.api.error.ApiException;
import com.hivemq.api.model.core.PayloadImpl;
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
        return PayloadImpl::new;
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
        return EventBuilderImpl::new;
    }

    @Override
    public @NotNull DataPointFactory dataPointFactory() {
        return DataPointImpl::new;
    }
}
