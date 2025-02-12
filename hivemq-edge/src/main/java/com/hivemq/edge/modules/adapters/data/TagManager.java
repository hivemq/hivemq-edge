package com.hivemq.edge.modules.adapters.data;

import com.hivemq.adapter.sdk.api.data.DataPoint;
import com.hivemq.protocols.northbound.TagConsumer;
import org.jetbrains.annotations.NotNull;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@Singleton
public class TagManager {

    @Inject
    public TagManager() {
    }

    private final @NotNull ConcurrentHashMap<String, List<TagConsumer>> consumers =
            new ConcurrentHashMap<>();

    public void feed(final @NotNull String tagName, final @NotNull List<DataPoint> dataPoints) {
        consumers.get(tagName).forEach(c -> c.accept(dataPoints));
    }


    public void addConsumer(final @NotNull String tagName, final @NotNull TagConsumer consumer) {
        consumers.compute(tagName, (tag, current) -> {
            if (current != null) {
                current.add(consumer);
                return current;
            } else {
                final List<TagConsumer> consumers = new ArrayList<>();
                consumers.add(consumer);
                return consumers;
            }
        });
    }


    public void removeConsumer(final @NotNull TagConsumer consumer) {
        consumers.computeIfPresent(consumer.getTagName(), (tag, current) -> {
            current.remove(consumer);
            return current;
        });
    }

    // TODO remove consumer


}
