package com.hivemq.edge.adapters.etherip;

import com.hivemq.adapter.sdk.api.data.DataPoint;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * This class is here TEMPORARY, the functionality will be moved into NorthboundMappings
 */
public class PublishChangedDataOnlyHandler {
    private final @NotNull Map<String, List<DataPoint>> lastSamples = new ConcurrentHashMap<>();

    public boolean replaceIfValueIsNew(final @NotNull String tagName, final @NotNull List<DataPoint> newValue) {
        final var computedValue = lastSamples.compute(tagName, (key,value) -> {
            if (value == null) {
                return newValue;
            } else if (value.equals(newValue)) {
                return value;
            } else {
                return newValue;
            }
        });

        return newValue != computedValue;
    }

    public void clear() {
        lastSamples.clear();
    }
}
