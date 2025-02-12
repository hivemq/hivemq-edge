package com.hivemq.protocols.northbound;

import com.hivemq.adapter.sdk.api.data.DataPoint;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.function.Consumer;

public interface TagConsumer extends Consumer<List<DataPoint>> {

   @NotNull String getTagName();
}
