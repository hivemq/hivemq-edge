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
import com.hivemq.configuration.service.InternalConfigurations;
import com.hivemq.edge.modules.adapters.model.ProtocolAdapterPollingSampler;
import com.hivemq.edge.modules.api.adapters.ProtocolAdapter;
import com.hivemq.edge.modules.api.adapters.ProtocolAdapterPollingService;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
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
    private final @NotNull ScheduledExecutorService scheduledExecutorService;
    private final @NotNull Map<ProtocolAdapterPollingSampler, MonitoredPollingJob> activePollers =
            new ConcurrentHashMap<>();
    private final @NotNull Set<MonitoredPollingJob> executingJobs
            = Collections.synchronizedSet(new HashSet<>());

    private final @NotNull Object semaphore = new Object();
    private final @NotNull Watchdog watchdog = new Watchdog();

    @Inject
    public ProtocolAdapterPollingServiceImpl(final @NotNull ScheduledExecutorService scheduledExecutorService,
                                             final @NotNull ShutdownHooks shutdownHooks) {
        this.scheduledExecutorService = scheduledExecutorService;

        Thread watchdogThread = new Thread(watchdog, "ProtocolAdapter-Watchdog");
        watchdogThread.setPriority(Thread.MIN_PRIORITY);
        watchdogThread.setDaemon(true);
        watchdogThread.start();

        shutdownHooks.add(new HiveMQShutdownHook() {
            @Override
            public @NotNull String name() {
                return "Protocol Adapter Polling Service ShutDown";
            }

            @Override
            public void run() {
                try {
                    ProtocolAdapterPollingServiceImpl.this.watchdog.running = false;
                    synchronized (semaphore){
                        semaphore.notifyAll();
                    }
                } catch(Exception e){
                    log.warn("Error Encountered Stopping Watchdog", e);
                }
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
        private final AtomicInteger runCount = new AtomicInteger(0);
        private final AtomicInteger watchdogErrorCount = new AtomicInteger(0);
        private final AtomicInteger applicationErrorCount = new AtomicInteger(0);
        private final ProtocolAdapterPollingSampler sampler;
        private volatile long notBefore = 0;
        private volatile long recentExecutionStarted = 0;
        private volatile Thread currentThread;

        public MonitoredPollingJob(final ProtocolAdapterPollingSampler sampler) {
            this.sampler = sampler;
        }

        private void resetErrorStats(){
            applicationErrorCount.set(0);
            watchdogErrorCount.set(0);
            notBefore = 0;
        }

        private boolean hasErrorStats(){
            return notBefore > 0 || applicationErrorCount.get() > 0 || watchdogErrorCount.get() > 0;
        }

        @Override
        public void run() {
            long startedTimeMillis = System.currentTimeMillis();
            try {
                if(notBefore > 0){
                    if(System.currentTimeMillis() < notBefore){
                        //-- We're backing off atm so as not to harass the network
                        return;
                    }
                }
                if(!sampler.isClosed()){
                    final String originalName = Thread.currentThread().getName();
                    if(!executingJobs.contains(this)){
                        try {
                            boolean execute;
                            synchronized (semaphore) {
                                execute = !executingJobs.contains(this);
                                recentExecutionStarted = startedTimeMillis;
                                currentThread = Thread.currentThread();
                                if(execute) {
                                    executingJobs.add(this);
                                    semaphore.notify();
                                }
                            }
                            if(execute) {
                                runCount.incrementAndGet();
                                currentThread.setName(originalName + " " + sampler.getAdapterId());
                                CompletableFuture<?> sampleFuture = sampler.execute();
                                if(sampleFuture != null){
                                    sampleFuture.get();
                                    if (log.isTraceEnabled()) {
                                        log.trace("Sampler {} Successfully Invoked in {}ms",
                                                sampler.getAdapterId(), System.currentTimeMillis() - startedTimeMillis);
                                    }
                                    if(hasErrorStats()){
                                        resetErrorStats();
                                    }
                                } else {
                                    throw new IllegalStateException("Sampler Returned Empty Future, Error Handling");
                                }
                            }
                        }
                        finally {
                            currentThread.setName(originalName);
                            synchronized (semaphore) {
                                executingJobs.remove(this);
                                currentThread = null;
                            }
                        }
                    } else {
                        if(log.isInfoEnabled()){
                            log.info("Determined Sampler {} Was Already Running, Concurrent Access Forbidden", sampler.getAdapterId());
                        }
                    }
                } else {
                    //Sampler was closed externally, ensure we remove it from the engine
                    stopPolling(sampler);
                }
            } catch(Throwable e){
                boolean continuing, notify = true;
                int errorCountTotal;
                if(isInterruptedException(e)){
                    //-- Job was killed by the framework as it took too long
                    //-- Do not call back to the job here (notify) since it will
                    //-- Not respond and we dont want to block other polls
                    errorCountTotal = watchdogErrorCount.incrementAndGet();
                    continuing = errorCountTotal < InternalConfigurations.ADAPTER_RUNTIME_WATCHDOG_TIMEOUT_ERRORS_BEFORE_INTERRUPT.get();
                    if(!continuing){
                        if(log.isInfoEnabled()){
                            log.info("Detected Bad System Process {} In Sampler {} - Terminating Process to Maintain Health ({}ms Runtime)",
                                    errorCountTotal, sampler.getAdapterId(), System.currentTimeMillis() - startedTimeMillis);
                        }
                    } else {
                        if(log.isDebugEnabled()){
                            log.debug("Detected Bad System Process {} In Sampler {} - Interrupted Process to Maintain Health ({}ms Runtime)",
                                    errorCountTotal, sampler.getAdapterId(), System.currentTimeMillis() - startedTimeMillis);
                        }
                    }
                } else {
                    errorCountTotal = applicationErrorCount.incrementAndGet();
                    continuing = errorCountTotal < sampler.getMaxErrorsBeforeRemoval();
                    if(log.isDebugEnabled()){
                        log.debug("Application Error {} In Sampler {} -> {}",
                                errorCountTotal, sampler.getAdapterId(), e.getMessage());
                    }
                }
                try {
                    if(notify){
                        try {
                            sampler.error(e, continuing);
                        } catch(Throwable samplerError){
                            if(log.isInfoEnabled()){
                                log.info("Sampler Encountered Error In Notification", samplerError);
                            }
                        }
                    }
                    if(!continuing) {
                        stopPolling(sampler);
                        //-- rest the error state
                        resetErrorStats();
                    } else {
                        //exp. backoff the network call according to the number of errors
                        long backoff = getBackoff(errorCountTotal,
                                InternalConfigurations.ADAPTER_RUNTIME_MAX_APPLICATION_ERROR_BACKOFF.get(),true);
                        notBefore = System.currentTimeMillis() + backoff;
                    }
                } catch(Throwable t){
                    if(log.isErrorEnabled()){
                        log.error("Framework Error Detected, This Needs Addressing", t);
                    }
                }
            }
        }
    }

    protected static boolean isInterruptedException(@NotNull Throwable t){
        Preconditions.checkNotNull(t);
        do{
          if(t instanceof InterruptedException || t instanceof TimeoutException)
              return true;
          t = t.getCause();
        } while(t != null);
        return false;
    }

    private final class Watchdog implements Runnable {
        volatile boolean running = false;

        @Override
        public void run() {
            running = true;
            while(running){
                try {
                    //create shallow copy then process outside lock
                    Set<MonitoredPollingJob> inProgress;
                    synchronized (semaphore){
                        if(executingJobs.isEmpty()){
                            semaphore.wait();
                        }
                        if(!running) break;
                        //-- notify will release so relock for iterator
                        synchronized (semaphore){
                            inProgress = new HashSet<>(executingJobs);
                        }
                    }
                    Iterator<MonitoredPollingJob> itr = inProgress.iterator();
                    while(itr.hasNext()){
                        MonitoredPollingJob job = itr.next();
                        if((System.currentTimeMillis() - job.recentExecutionStarted) >
                                InternalConfigurations.ADAPTER_RUNTIME_JOB_EXECUTION_TIMEOUT_MILLIS.get()){
                            if(job.currentThread != null){
                                job.currentThread.interrupt();
                            }
                        }
                    }
                    //-- Ensure were not too aggressive
                    Thread.sleep(25);
                }
                catch(Throwable e){
                    log.error("Watchdog Thread was Terminated By Error", e);
                }
            }
        }
    }
}
