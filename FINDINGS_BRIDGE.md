# Bridge Message Forwarding Stall - Root Cause Analysis

## Summary

The `RemoteMqttForwarder` can enter a permanent stall state after connection failures where messages continuously arrive on the source topic but are never forwarded, even after the bridge reports being connected.

## Symptoms

- Bridge logs show successful reconnection ("Bridge connected")
- No more message forwarding occurs after reconnection
- No warnings or errors are logged - complete silence
- New messages keep arriving but are not processed

## Reproduction Scenario

The issue occurs during rapid connect/disconnect cycles, typically caused by:
- Network instability
- Remote broker restarts
- TCP connection issues (broken pipe, channel output shutdown)

Example log pattern:
```
Bridge disconnected: Server closed connection without DISCONNECT
Bridge connected
WARN Unable to forward message... ChannelOutputShutdownException
Bridge disconnected
Bridge connected
WARN Unable to forward message...
Bridge disconnected
Bridge connected    <-- Final reconnect, then SILENCE
```

## Root Cause

The stall is caused by a combination of two bugs in the message forwarding pipeline:

### Bug 1: Inflight Counter Imbalance in `drainQueue()`

**Location:** `RemoteMqttForwarder.java:320-360`

When `drainQueue()` sends buffered messages, it does NOT increment `inflightCounter`, but when those messages complete (success or failure), `finishProcessing()` DOES decrement it.

```java
// drainQueue() - NO inflightCounter.incrementAndGet() here!
publishResult.whenComplete((result, throwable) -> {
    // ...
    finishProcessing(...);  // This decrements inflightCounter!
});
```

Compare with `onMessage()` which correctly increments before processing:
```java
public void onMessage(...) {
    inflightCounter.incrementAndGet();  // Correct!
    // ... eventually calls finishProcessing() which decrements
}
```

This causes `inflightCounter` to go negative after draining buffered messages.

### Bug 2: Inflight Markers Not Cleared on Publish Failure

**Location:** `RemoteMqttForwarder.java:303-316` and persistence layer

When messages are read from the persistence queue via `readShared()`, they get an **inflight marker** set (packet ID assigned). This marker indicates the message is being processed.

When a publish fails:
1. `handlePublishError()` logs the warning
2. `finishProcessing()` is called
3. `afterForwardCallback.afterMessage()` → `messageProcessed()` → `removeShared()`

The problem: `removeShared()` tries to **remove** the message from persistence, but if the publish failed, the message should be **retried**, not removed. The inflight marker should be cleared so the message can be re-read.

**Location of inflight marker skip:** `ClientQueueMemoryLocalPersistence.java:307-309`
```java
if (publishWithRetained.getPacketIdentifier() != NO_PACKET_ID) {
    //already inflight
    continue;  // Message is SKIPPED!
}
```

### Bug 3: Missing `checkBuffers()` Trigger After Reconnection

**Location:** `BridgeMqttClient.java:393-412`

The connected listener calls `drainQueue()` but does NOT trigger `checkBuffers()`:
```java
builder.addConnectedListener(context -> {
    connected.set(true);
    forwarders.forEach(MqttForwarder::drainQueue);  // Drains in-memory buffer
    // Missing: No trigger to poll from persistence queue!
});
```

If the in-memory buffer is empty (already drained in previous failed attempts), nothing triggers polling of new messages from persistence.

## The Deadlock Sequence

1. Connection drops during message forwarding
2. In-flight publishes fail with `ChannelOutputShutdownException`
3. Messages in persistence queue retain their inflight markers
4. Rapid reconnect/disconnect cycles occur
5. Each `drainQueue()` call decrements `inflightCounter` without incrementing
6. After final successful reconnect:
   - In-memory buffer (`queue`) is empty
   - `drainQueue()` returns immediately without triggering `checkBuffers()`
   - Persistence queue has messages but they all have inflight markers
   - `readShared()` skips all messages (inflight markers set)
   - Returns null/empty → `notEmptyQueues.remove(queueId)`
   - No mechanism triggers `checkBuffers()` again
   - **Permanent stall**

## Affected Files

- `hivemq-edge/src/main/java/com/hivemq/bridge/mqtt/RemoteMqttForwarder.java`
- `hivemq-edge/src/main/java/com/hivemq/bridge/mqtt/BridgeMqttClient.java`
- `hivemq-edge/src/main/java/com/hivemq/bridge/MessageForwarderImpl.java`
- `hivemq-edge/src/main/java/com/hivemq/persistence/local/memory/ClientQueueMemoryLocalPersistence.java`

## Recommended Fixes

### Fix 1: Balance `inflightCounter` in `drainQueue()`

Add `inflightCounter.incrementAndGet()` when sending messages in `drainQueue()`:

```java
// In drainQueue(), before sending each buffered message:
inflightCounter.incrementAndGet();
final CompletableFuture<Mqtt5PublishResult> publishResult =
        remoteMqttClient.getMqtt5Client().publish(convertPublishForClient(buffered.publish));
```

### Fix 2: Clear Inflight Markers on Publish Failure

When a publish fails, call `removeInFlightMarker()` instead of (or in addition to) `removeShared()`:

```java
// In the publish failure path:
if (throwable != null) {
    handlePublishError(publish, throwable);
    // Clear inflight marker so message can be retried
    resetInflightMarkerCallback.afterMessage(queueId, uniqueId);
}
```

### Fix 3: Trigger `checkBuffers()` After Reconnection

In the connected listener, after draining queues, trigger a buffer check:

```java
builder.addConnectedListener(context -> {
    connected.set(true);
    forwarders.forEach(MqttForwarder::drainQueue);
    // Trigger polling from persistence queue
    messageForwarder.checkBuffers();  // Need to pass reference or use event
});
```

### Fix 4: Consider Adding Connection State Check Before Publish

In `sendPublishToRemote()`, re-check connection state after acquiring the lock:

```java
private synchronized void sendPublishToRemote(...) {
    if (!remoteMqttClient.isConnected()) {
        queue.add(new BufferedPublishInformation(...));
        // Also need to handle inflightCounter here!
        finishProcessing(originalQoS, originalUniqueId, queueId);  // Or similar
        return;
    }
    // ...
}
```

## Testing Recommendations

1. Use Toxiproxy or similar to simulate network failures during message forwarding
2. Test rapid connect/disconnect cycles (multiple within 1-2 seconds)
3. Verify message flow resumes after network stabilizes
4. Monitor `inflightCounter` values (add metrics/logging)
5. Verify inflight markers are properly cleared after failures