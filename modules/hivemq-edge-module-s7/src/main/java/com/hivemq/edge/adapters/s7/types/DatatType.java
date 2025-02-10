package com.hivemq.edge.adapters.s7.types;

import com.hivemq.adapter.sdk.api.data.DataPoint;
import com.hivemq.edge.adapters.s7.config.S7DataType;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public interface DatatType<T extends S7DataType> {
    List<DataPoint> read(final @NotNull List<String> addresses);
}
