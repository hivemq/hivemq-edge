package com.hivemq.edge.modules.adapters.impl.factories;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hivemq.adapter.sdk.api.events.model.Payload;
import com.hivemq.adapter.sdk.api.factories.AdapterFactories;
import com.hivemq.adapter.sdk.api.factories.AdapterSubscriptionFactory;
import com.hivemq.adapter.sdk.api.factories.DataPointFactory;
import com.hivemq.adapter.sdk.api.factories.PayloadFactory;
import com.hivemq.api.model.core.PayloadImpl;
import com.hivemq.edge.modules.adapters.data.DataPointImpl;
import com.hivemq.edge.modules.config.impl.PollingContextImpl;
import com.hivemq.extension.sdk.api.annotations.NotNull;

public class AdapterFactoriesImpl implements AdapterFactories {

    @Override
    public @NotNull AdapterSubscriptionFactory adapterSubscriptionFactory() {
        return PollingContextImpl::new;
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
    public @NotNull DataPointFactory dataPointFactory() {
        return DataPointImpl::new;
    }
}
