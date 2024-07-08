package com.hivemq.edge.modules.adapters.simulation;

import com.hivemq.extension.sdk.api.annotations.NotNull;

import java.util.concurrent.TimeUnit;

public class TimeWaiter {
    public static @NotNull TimeWaiter INSTANCE = new TimeWaiter();

    public void sleep(int millis) throws InterruptedException {
        TimeUnit.MILLISECONDS.sleep(millis);
    }
}
