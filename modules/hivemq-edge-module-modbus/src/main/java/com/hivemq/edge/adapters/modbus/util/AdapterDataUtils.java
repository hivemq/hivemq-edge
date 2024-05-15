package com.hivemq.edge.adapters.modbus.util;

import com.hivemq.adapter.sdk.api.data.DataPoint;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class AdapterDataUtils {
    public static boolean matches(final @NotNull DataPoint point, final @NotNull List<DataPoint> list) {
        return list.stream().filter(dp -> dp.getTagName().equals(point.getTagName())) // First filter by tagName
                .anyMatch(dp -> dp.getTagValue().equals(point.getTagValue())); // Then check for tagValue
    }

    public static @NotNull List<DataPoint> mergeChangedSamples(
            final @Nullable List<DataPoint> historicalSamples, final @NotNull List<DataPoint> currentSamples) {
        if (historicalSamples == null) {
            return currentSamples;
        }
        List<DataPoint> delta = new ArrayList<>();
        for (int i = 0; i < currentSamples.size(); i++) {
            DataPoint currentSample = currentSamples.get(i);
            // If the current sample does not match any in the historical samples, it has changed
            if (!matches(currentSample, historicalSamples)) {
                historicalSamples.set(i, currentSample);
                delta.add(currentSample);
            }
        }
        return delta;
    }
}
