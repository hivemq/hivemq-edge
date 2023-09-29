package com.hivemq.edge.adapters.plc4x.impl;

import com.hivemq.edge.adapters.plc4x.model.Plc4xAdapterConfig;
import com.hivemq.extension.sdk.api.annotations.NotNull;

/**
 * @author Simon L Johnson
 */
public interface Plc4xConnectionQueryStringProvider<T extends Plc4xAdapterConfig> {

    String getConnectionQueryString(@NotNull final T plc4xAdapterConfig);

}
