package com.hivemq.edge.adapters.modbus.util;

import com.hivemq.edge.modules.adapters.data.ProtocolAdapterDataSample;
import com.hivemq.extension.sdk.api.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class AdapterDataUtils {
    public static boolean matches(final @NotNull ProtocolAdapterDataSample.DataPoint point, final @NotNull List<ProtocolAdapterDataSample.DataPoint> list) {
        return list.stream().filter(dp -> dp.getTagName().equals(point.getTagName())) // First filter by tagName
                .anyMatch(dp -> dp.getTagValue().equals(point.getTagValue())); // Then check for tagValue
    }

    public static List<ProtocolAdapterDataSample.DataPoint> mergeChangedSamples(
            final @NotNull List<ProtocolAdapterDataSample.DataPoint> historicalSamples,
            final @NotNull List<ProtocolAdapterDataSample.DataPoint> currentSamples) {
        List<ProtocolAdapterDataSample.DataPoint> delta = new ArrayList<>();
        for (int i = 0; i < currentSamples.size(); i++) {
            ProtocolAdapterDataSample.DataPoint currentSample = currentSamples.get(i);
            // If the current sample does not match any in the historical samples, it has changed
            if (!matches(currentSample, historicalSamples)) {
                historicalSamples.set(i, currentSample);
                delta.add(currentSample);
            }
        }
        return delta;
    }
}
