package com.hivemq.edge.modules.adapters.impl.factories;

import com.hivemq.adapter.sdk.api.factories.AdapterFactories;
import com.hivemq.adapter.sdk.api.factories.DataPointFactory;
import com.hivemq.edge.modules.adapters.data.DataPointImpl;
import com.hivemq.extension.sdk.api.annotations.NotNull;

public class AdapterFactoriesImpl implements AdapterFactories {


    @Override
    public @NotNull DataPointFactory dataPointFactory() {
        return DataPointImpl::new;
    }
}
