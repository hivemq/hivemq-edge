package com.hivemq.edge.adapters.s7.types;

import com.github.xingshuangs.iot.protocol.s7.service.S7PLC;
import com.hivemq.adapter.sdk.api.data.DataPoint;
import com.hivemq.adapter.sdk.api.factories.DataPointFactory;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class BoolType<Bool> {
    private final @NotNull DataPointFactory dataPointFactory;
    private final @NotNull S7PLC client;

    public BoolType(@NotNull final DataPointFactory dataPointFactory, @NotNull final S7PLC client) {
        this.dataPointFactory = dataPointFactory;
        this.client = client;
    }

    public List<DataPoint> read(final @NotNull List<String> addresses) {
        return combine(dataPointFactory, addresses, client.readBoolean(addresses));
    }

    public static List<DataPoint> combine(final @NotNull DataPointFactory dataPointFactory, final @NotNull List<String> addresses, final @NotNull  List<?> values) {
        return IntStream
                .range(0, addresses.size())
                .mapToObj(i -> dataPointFactory.create(addresses.get(i), values.get(i)))
                .collect(Collectors.toList());
    }
}
