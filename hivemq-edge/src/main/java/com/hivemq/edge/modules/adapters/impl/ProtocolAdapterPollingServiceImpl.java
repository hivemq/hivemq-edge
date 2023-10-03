/*
 * Copyright 2019-present HiveMQ GmbH
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
package com.hivemq.edge.modules.adapters.impl;

import com.google.common.base.Preconditions;
import com.hivemq.common.shutdown.HiveMQShutdownHook;
import com.hivemq.common.shutdown.ShutdownHooks;
import com.hivemq.edge.modules.adapters.params.ProtocolAdapterPollingSampler;
import com.hivemq.edge.modules.api.adapters.ProtocolAdapter;
import com.hivemq.edge.modules.api.adapters.ProtocolAdapterPollingService;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * The polling service provides utility to track and invoke adapter
 * data acquisition attempts. It provides a mechanism to handle
 * errors and back off the attempts until a maximum number of
 * retries occurs, at which point a terminal failure callback
 * is invoked and the instance is removed from the schedule and close
 * is called on the input instance (probably closing the underlying
 * resource according to the implementation).
 *
 * @author Simon L Johnson
 */
public class ProtocolAdapterPollingServiceImpl implements ProtocolAdapterPollingService {

    private static final Logger log = LoggerFactory.getLogger(ProtocolAdapterPollingServiceImpl.class);
    private static long MAX_BACKOFF_MILLIS = 60000 * 10; //-- 10 Mins
    private static long JOB_EXECUTION_CEILING_MILLIS = 60000; //-- 60 Seconds
    private static int MAX_TIMEOUT_ERRORS = 10; //-- Number of consecutive job timeouts (based on above) that will be allowed before job is terminated

    private final @NotNull ScheduledExecutorService scheduledExecutorService;
    private final @NotNull Map<ProtocolAdapterPollingSampler, MonitoredPollingJob> activePollers =
            new ConcurrentHashMap<>();

    @Inject
    public ProtocolAdapterPollingServiceImpl(final @NotNull ScheduledExecutorService scheduledExecutorService,
                                             final @NotNull ShutdownHooks shutdownHooks) {
        this.scheduledExecutorService = scheduledExecutorService;
        shutdownHooks.add(new HiveMQShutdownHook() {
            @Override
            public @NotNull String name() {
                return "Protocol Adapter Polling Service ShutDown";
            }

            @Override
            public void run() {
                if(!scheduledExecutorService.isShutdown()){
                    try {
                        scheduledExecutorService.shutdown();
                        if(!scheduledExecutorService.awaitTermination(10, TimeUnit.SECONDS)){
                            scheduledExecutorService.shutdownNow();
                        }
                    } catch(InterruptedException e){
                        log.warn("Error Encountered Attempting to Shutdown Adapter Polling Service", e);
                    }
                }
            }
        });
    }

    public void schedulePolling(final @NotNull ProtocolAdapter adapter,
                                                           final @NotNull ProtocolAdapterPollingSampler sampler){
        if(log.isTraceEnabled()){
            log.trace("Scheduling Polling For Adapter {}", adapter.getId());
        }
        MonitoredPollingJob internalJob = new MonitoredPollingJob(sampler);
        ScheduledFuture<?> future = scheduledExecutorService.scheduleAtFixedRate(internalJob,
                sampler.getInitialDelay(),
                sampler.getPeriod(),
                sampler.getUnit());
        sampler.setScheduledFuture(future);
        activePollers.put(sampler, internalJob);
    }


    public Optional<ProtocolAdapterPollingSampler> getPollingJob(final @NotNull UUID id){
        Preconditions.checkNotNull(id);
        return activePollers.keySet().stream().filter(p -> p.getId().equals(id)).findAny();
    }

    public List<ProtocolAdapterPollingSampler> getPollingJobsForAdapter(final @NotNull String adapterId){
        Preconditions.checkNotNull(adapterId);
        return activePollers.keySet().stream().
                filter(p -> p.getAdapterId().equals(adapterId)).
                collect(Collectors.toList());
    }

    public void stopPollingForAdapterInstance(final @NotNull ProtocolAdapter adapter){
        Preconditions.checkNotNull(adapter);
        activePollers.keySet().stream().
                filter(p -> p.getAdapterId().equals(adapter.getId())).
                forEach(this::stopPolling);
    }

    public void stopPolling(final @NotNull ProtocolAdapterPollingSampler sampler){
        Preconditions.checkNotNull(sampler);
        if(activePollers.remove(sampler) != null){
            Future<?> future = sampler.getScheduledFuture();
            if(!future.isCancelled()){
                if(log.isInfoEnabled()){
                    log.info("Stopping Polling Job {}", sampler.getReferenceId());
                }
                //-- Cancel the future
                //-- Bad Processes May Block Here.. Consider Forking
                if(future.cancel(true)){
                    if(!sampler.isClosed()){
                        sampler.close();
                    }
                }
            }
        }
    }

    @Override
    public List<ProtocolAdapterPollingSampler> getActiveProcesses() {
        return List.copyOf(activePollers.keySet());
    }

    @Override
    public int currentErrorCount(final ProtocolAdapterPollingSampler pollingJob) {
        return activePollers.get(pollingJob).applicationErrorCount.get();
    }

    public void stopAllPolling(){
        activePollers.keySet().stream().forEach(this::stopPolling);
    }

    private static long getBackoff(int errorCount, long max, boolean addFuzziness){
        //-- This will backoff up to a max of about a day (unless the max provided is less)
        long f = (long) (Math.pow(2, Math.min(errorCount, 20)) * 100);
        if(addFuzziness){
            f += ThreadLocalRandom.current().nextInt(0, errorCount * 100);
        }
        f =  Math.min(f, max);
        return f;
    }

    private class MonitoredPollingJob implements Runnable {
        private final AtomicInteger watchdogErrorCount = new AtomicInteger(0);
        private final AtomicInteger applicationErrorCount = new AtomicInteger(0);
        private final ProtocolAdapterPollingSampler sampler;
        private volatile long notBefore = 0;
        private AtomicBoolean executing = new AtomicBoolean(false);

        public MonitoredPollingJob(final ProtocolAdapterPollingSampler sampler) {
            this.sampler = sampler;
        }

        private void resetErrorStats(){
            applicationErrorCount.set(0);
            watchdogErrorCount.set(0);
            notBefore = 0;
        }

        @Override
        public void run() {
            long startedTimeMillis = System.currentTimeMillis();
            try {
                executing.set(true);
                if(notBefore > 0){
                    if(System.currentTimeMillis() < notBefore){
                        //-- We're backing off atm so as not to harass the network
                        return;
                    }
                }
                if(!sampler.isClosed()){
                    final String originalName = Thread.currentThread().getName();
                    try {
                        Thread.currentThread().setName(originalName + " " + sampler.getReferenceId());
                        sampler.execute().orTimeout(JOB_EXECUTION_CEILING_MILLIS, TimeUnit.MILLISECONDS).get();
                        if(log.isTraceEnabled()){
                            log.trace("Adapter Job Successfully Invoked in {}ms", System.currentTimeMillis() - startedTimeMillis);
                        }
                        resetErrorStats();
                    } finally {
                        Thread.currentThread().setName(originalName);
                    }
                } else {
                    //Sampler was closed externally, ensure we remove it from the engine
                    stopPolling(sampler);
                }
            } catch(Throwable e){
                boolean continuing, notify = true;
                int errorCountTotal;
                //-- Determine if this is the watchdog or the application causing the error
                if(e.getCause() instanceof TimeoutException){
                    //-- Job was killed by the framework as it took too long
                    //-- Do not call back to the job here (notify) since it will
                    //-- Not respond and we dont want to block other polls
                    errorCountTotal = watchdogErrorCount.incrementAndGet();
                    continuing = errorCountTotal < MAX_TIMEOUT_ERRORS;
                    notify = false;
                    if(!continuing){
                        if(log.isInfoEnabled()){
                            log.info("Detected Bad System Process In Adapter Job {} - Terminating Process to Maintain Health ({}ms Runtime)",
                                    sampler.getReferenceId(), System.currentTimeMillis() - startedTimeMillis);
                        }
                    }
                } else {
                    errorCountTotal = applicationErrorCount.incrementAndGet();
                    continuing = errorCountTotal < sampler.getMaxErrorsBeforeRemoval();
                    if(log.isDebugEnabled()){
                        log.debug("Error {} In Adapter Job {} -> {}",
                                errorCountTotal, sampler.getReferenceId(), e.getMessage());
                    }
                }
                if(notify){
                    sampler.error(e, continuing);
                }
                if(!continuing) {
                    stopPolling(sampler);
                    //-- rest the error state
                    resetErrorStats();
                } else {
                    //exp. backoff the network call according to the number of errors
                    long backoff = getBackoff(errorCountTotal, MAX_BACKOFF_MILLIS,true);
                    notBefore = System.currentTimeMillis() + backoff;
                }
            } finally {
                executing.set(false);
            }
        }
    }
}
