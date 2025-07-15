// ...existing code...

import java.util.concurrent.atomic.AtomicReference;

// ...existing code...

    private final AtomicBoolean stateChangeOngoing = new AtomicBoolean(false);
    private final AtomicReference<CompletableFuture<Boolean>> startFutureRef = new AtomicReference<>(null);
    private final AtomicReference<CompletableFuture<Boolean>> stopFutureRef = new AtomicReference<>(null);

    // ...existing code...

    public @NotNull CompletableFuture<Boolean> startAsync(
            final boolean writingEnabled,
            final @NotNull ModuleServices moduleServices) {
        CompletableFuture<Boolean> currentFuture = startFutureRef.get();
        if (currentFuture != null && !currentFuture.isDone()) {
            log.warn("Start already in progress for adapter with id '{}', returning existing future", getId());
            return currentFuture;
        }
        if(stateChangeOngoing.compareAndSet(false, true)) {
            initStartAttempt();
            final ProtocolAdapterStartOutputImpl output = new ProtocolAdapterStartOutputImpl();
            final ProtocolAdapterStartInputImpl input = new ProtocolAdapterStartInputImpl(moduleServices);

            CompletableFuture<Boolean> startFuture = CompletableFuture
                    .supplyAsync(() -> {
                        adapter.start(input, output);
                        return output.getStartFuture();
                    })
                    .thenCompose(Function.identity())
                    .handle((ignored, t) -> {
                        if(t == null) {
                            createAndSubscribeTagConsumer();
                            startPolling(protocolAdapterPollingService, input.moduleServices().eventService());
                            return startWriting(writingEnabled, protocolAdapterWritingService)
                                    .thenApply(v -> {
                                        log.info("Successfully started adapter with id {}", adapter.getId());
                                        setRuntimeStatus(ProtocolAdapterState.RuntimeStatus.STARTED);
                                        return true;
                                    });
                        } else {
                            log.error("Error starting protocol adapter", t);
                            stopPolling(protocolAdapterPollingService);
                            return stopWriting(protocolAdapterWritingService)
                                    .thenApply(v -> {
                                        log.error("Error starting adapter with id {}", adapter.getId(), t);
                                        setRuntimeStatus(ProtocolAdapterState.RuntimeStatus.STOPPED);
                                        return false;
                                    });
                        }
                    })
                    .thenCompose(Function.identity())
                    .whenComplete((ignored, throwable) -> {
                        stateChangeOngoing.set(false);
                        startFutureRef.set(null);
                    });
            startFutureRef.set(startFuture);
            return startFuture;
        } else {
            log.warn("State change for adapter with id '{}' is already ongoing, returning existing start future", getId());
            currentFuture = startFutureRef.get();
            if (currentFuture != null) {
                return currentFuture;
            }
        }
        return CompletableFuture.completedFuture(true);
    }

    public @NotNull CompletableFuture<Boolean> stopAsync(final boolean destroy) {
        CompletableFuture<Boolean> currentFuture = stopFutureRef.get();
        if (currentFuture != null && !currentFuture.isDone()) {
            log.warn("Stop already in progress for adapter with id '{}', returning existing future", getId());
            return currentFuture;
        }
        if(stateChangeOngoing.compareAndSet(false, true)) {
            consumers.forEach(tagManager::removeConsumer);
            final ProtocolAdapterStopInputImpl input = new ProtocolAdapterStopInputImpl();
            final ProtocolAdapterStopOutputImpl output = new ProtocolAdapterStopOutputImpl();

            CompletableFuture<Boolean> stopFuture = CompletableFuture
                    .supplyAsync(() -> {
                        stopPolling(protocolAdapterPollingService);
                        return stopWriting(protocolAdapterWritingService);
                    })
                    .thenCompose(Function.identity())
                    .handle((stopped, t) -> {
                        adapter.stop(input, output);
                        return output.getOutputFuture();
                    })
                    .thenCompose(Function.identity())
                    .handle((ignored, throwable) -> {
                        stateChangeOngoing.set(false);
                        stopFutureRef.set(null);
                        if(destroy) {
                            log.info("Destroying adapter with id '{}'", getId());
                            adapter.destroy();
                        }
                        if(throwable == null) {
                            setRuntimeStatus(ProtocolAdapterState.RuntimeStatus.STOPPED);
                            log.info("Stopped adapter with id {}", adapter.getId());
                            return true;
                        } else {
                            log.error("Error stopping adapter with id {}", adapter.getId(), throwable);
                            return false;
                        }
                    });
            stopFutureRef.set(stopFuture);
            return stopFuture;
        } else {
            log.warn("State change for adapter with id '{}' is already ongoing, returning existing stop future", getId());
            currentFuture = stopFutureRef.get();
            if (currentFuture != null) {
                return currentFuture;
            }
        }
        return CompletableFuture.completedFuture(true);
    }

// ...existing code...
