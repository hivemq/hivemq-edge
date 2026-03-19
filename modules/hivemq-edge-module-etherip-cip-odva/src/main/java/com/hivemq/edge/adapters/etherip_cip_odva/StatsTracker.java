/*
 * Copyright 2023-present HiveMQ GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.hivemq.edge.adapters.etherip_cip_odva;

import com.google.common.base.Stopwatch;
import com.google.common.math.Stats;
import com.google.common.math.StatsAccumulator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.LoggerFactory;

public class StatsTracker {
    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(StatsTracker.class);

    private final Map<String, StatsAccumulator> rttStats = new ConcurrentHashMap<>();
    private final Stopwatch statsTimes = Stopwatch.createUnstarted();
    private final @NotNull String adapterId;

    public StatsTracker(@NotNull final String adapterId) {
        this.adapterId = adapterId;

        if (isLoggingEnabled()) {
            statsTimes.start();
        }
    }

    private boolean isLoggingEnabled() {
        return LOGGER.isDebugEnabled();
    }

    private void calculateStats(String tagAddress, Stopwatch rtt) {
        if (isLoggingEnabled()) {
            String adapterIdAndTag = adapterId + ":" + tagAddress;

            rttStats.computeIfAbsent(adapterIdAndTag, (k) -> new StatsAccumulator())
                    .add(rtt.elapsed(TimeUnit.MICROSECONDS));

            long elapsed = statsTimes.elapsed(TimeUnit.MILLISECONDS);
            if (elapsed >= 10000 || !statsTimes.isRunning()) { // count check?
                statsTimes.reset().start();

                showStats(rttStats, elapsed);
                rttStats.clear();
            }
        }
    }

    private void showStats(final Map<String, StatsAccumulator> rttStatsMap, long elapsed) {
        if (isLoggingEnabled()) {
            rttStatsMap.forEach((id, stats) -> {
                Stats snapshot = stats.snapshot();

                LOGGER.debug(String.format(
                        "[%s] RTT Stats (period=%,dms) [us] calculated_avg=%,.0fms, count=%d, min=%,.0f, mean=%,.0f, variance=%,.0f, max=%,.0f",
                        id,
                        elapsed,
                        (float) elapsed / snapshot.count(),
                        snapshot.count(),
                        snapshot.min(),
                        snapshot.mean(),
                        snapshot.count() > 1 ? snapshot.sampleStandardDeviation() : 0,
                        snapshot.max()));
            });
        }
    }

    public @Nullable Stopwatch start() {
        return isLoggingEnabled() ? Stopwatch.createStarted() : null;
    }

    public void stop(final @NotNull Supplier<String> tagAddressSupplier, final Stopwatch rttTimer) {
        if (isLoggingEnabled() && rttTimer != null) {
            rttTimer.stop();
            calculateStats(tagAddressSupplier.get(), rttTimer);
        }
    }
}
