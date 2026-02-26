# Bridge Persistence and QoS Behavior

This document explains how the `persist` flag and persistence modes affect message handling in HiveMQ Edge bridges.

---

## Table of Contents

1. [Understanding MQTT Quality of Service (QoS)](#understanding-mqtt-quality-of-service-qos)
2. [The `persist` Flag](#the-persist-flag)
3. [Two-Tier Queuing Architecture](#two-tier-queuing-architecture)
4. [Behavior by persist Flag and QoS Level](#behavior-by-persist-flag-and-qos-level)
5. [Persistence Modes](#persistence-modes)
6. [What Survives Disconnection vs Restart](#what-survives-disconnection-vs-restart)
7. [Key Points Summary](#key-points-summary)
8. [Code References](#code-references)

---

## Understanding MQTT Quality of Service (QoS)

MQTT defines three levels of Quality of Service (QoS) that determine the delivery guarantees for messages:

### QoS 0 - At Most Once (Fire-and-Forget)

| Aspect | Description |
|--------|-------------|
| **Delivery guarantee** | Message delivered at most once, may be lost |
| **Acknowledgment** | None |
| **Use case** | Telemetry data where occasional loss is acceptable |
| **Performance** | Fastest, lowest overhead |

The sender publishes the message and immediately forgets about it. No confirmation is expected or tracked.

### QoS 1 - At Least Once

| Aspect | Description |
|--------|-------------|
| **Delivery guarantee** | Message delivered at least once, may be duplicated |
| **Acknowledgment** | PUBACK from receiver |
| **Use case** | Important data where duplicates can be handled |
| **Performance** | Moderate overhead |

The sender keeps the message until it receives a PUBACK acknowledgment. If no acknowledgment arrives, the message is resent.

### QoS 2 - Exactly Once

| Aspect | Description |
|--------|-------------|
| **Delivery guarantee** | Message delivered exactly once |
| **Acknowledgment** | 4-way handshake (PUBREC → PUBREL → PUBCOMP) |
| **Use case** | Critical data where duplicates are unacceptable (e.g., billing) |
| **Performance** | Highest overhead |

A 4-way handshake ensures the message is delivered exactly once, with no duplicates and no loss.

---

## The `persist` Flag

The `persist` flag on a bridge configuration **controls QoS downgrading**. It determines whether messages maintain their original QoS level or are downgraded to QoS 0.

### When persist=true (default)

- **QoS levels are preserved**: QoS 0 stays 0, QoS 1 stays 1, QoS 2 stays 2
- Messages are queued with full delivery tracking for QoS 1/2
- Inflight markers ensure reliable delivery with retry capability
- Messages can be persisted to disk (with FILE_NATIVE mode)

### When persist=false

- **QoS 1 and QoS 2 messages are downgraded to QoS 0**
- All messages become fire-and-forget
- No delivery tracking or retry capability
- Messages are not persisted to disk

### Why It's Called "persist"

The flag controls **persistence guarantees**:
- Whether messages maintain QoS levels that enable disk persistence
- Whether messages have inflight markers for redelivery tracking
- Whether delivery acknowledgments are tracked

Messages are always queued internally regardless of this flag. The difference is in the delivery guarantees applied to those messages.

### Code Evidence

```java
// PublishDistributorImpl.java:242-244
if (!customBridgeLimitations.persist) {
    // if the bridge has the persist flag disabled, we reduce the QoS of the messages to 0,
    // so they are not stored in the file persistence in case.
    appliedQoS = 0;
}
```

After this check, `queuePublish()` is always called, regardless of the persist flag value.

---

## Two-Tier Queuing Architecture

HiveMQ Edge bridges use a two-tier queuing system to handle messages:

### Tier 1: ClientQueuePersistence

| Property | Description |
|----------|-------------|
| **Location** | `ClientQueuePersistence` (shared subscription queue) |
| **Storage** | Configurable: IN_MEMORY (HashMap) or FILE_NATIVE (disk) |
| **Capacity** | Controlled by global `maxQueuedMessages` setting |
| **QoS Handling** | QoS 1/2: inflight markers set; QoS 0: no markers |

- All messages (QoS 0, 1, 2) are stored here
- With FILE_NATIVE mode, QoS 1/2 messages survive restarts
- QoS 0 messages are queued but not tracked for redelivery

### Tier 2: In-Memory Buffer (RemoteMqttForwarder)

| Property | Description |
|----------|-------------|
| **Location** | `ConcurrentLinkedQueue` in `RemoteMqttForwarder` |
| **Storage** | Volatile (RAM only) |
| **Capacity** | Unbounded (limited by available memory) |
| **Purpose** | Immediate buffering when remote broker is disconnected |

- Messages added here when the remote broker connection is down
- Drained first upon reconnection to preserve message order
- Always lost on restart (even with FILE_NATIVE mode for Tier 1)

---

## Behavior by persist Flag and QoS Level

### Complete Behavior Matrix

| persist | Original QoS | Applied QoS | Queued | Inflight Tracked | Redelivered on Failure | Persisted to Disk |
|---------|--------------|-------------|--------|------------------|------------------------|-------------------|
| true    | 0            | 0           | Yes    | No               | No                     | No*               |
| true    | 1            | 1           | Yes    | Yes              | Yes                    | Yes**             |
| true    | 2            | 2           | Yes    | Yes              | Yes                    | Yes**             |
| false   | 0            | 0           | Yes    | No               | No                     | No                |
| false   | 1            | **0**       | Yes    | No               | No                     | No                |
| false   | 2            | **0**       | Yes    | No               | No                     | No                |

\* QoS 0 messages are queued in ClientQueuePersistence but not persisted to disk (standard MQTT behavior)
\** Only with FILE_NATIVE persistence mode; IN_MEMORY mode loses all data on restart

### Inflight Marker Behavior

The key difference between QoS levels is **inflight marker handling**:

```java
// MessageForwarderImpl.java:290-297
public void messageProcessed(final QoS qos, ...) {
    // QoS 0 has no inflight marker
    if (qos != QoS.AT_MOST_ONCE) {
        // Only QoS 1/2 messages have inflight markers to clear
        qPersistence.get().removeShared(queueId, uniqueId);
    }
}
```

**QoS 0**: Message removed from queue after processing attempt, regardless of delivery success
**QoS 1/2**: Message only removed after confirmed delivery (ACK received from remote broker)

---

## Persistence Modes

HiveMQ Edge supports two persistence modes, configured globally:

### IN_MEMORY (Default - Free)

| Aspect | Behavior |
|--------|----------|
| **Storage** | HashMap + LinkedList structures in RAM |
| **Performance** | Fast, low latency |
| **Durability** | All data lost on restart |
| **License** | Free, available to all users |

### FILE_NATIVE (Licensed)

| Aspect | Behavior |
|--------|----------|
| **Storage** | Disk-based persistence |
| **Performance** | Slightly higher latency due to disk I/O |
| **Durability** | QoS 1/2 messages survive restart |
| **License** | Requires commercial license |

### Configuration

```xml
<persistence>
    <mode>in-memory</mode>  <!-- or "file-native" -->
</persistence>
```

---

## What Survives Disconnection vs Restart

### Temporary Disconnection (Remote Broker Unavailable)

| Scenario | Messages Buffered? | Sent on Reconnect? | Notes |
|----------|--------------------|--------------------|-------|
| persist=true, QoS 0 | Yes (both tiers) | Yes | Fire-and-forget, no retry on failure |
| persist=true, QoS 1/2 | Yes (both tiers) | Yes | With ACK tracking and retry on failure |
| persist=false, any QoS | Yes (both tiers) | Yes | All sent as QoS 0, no retry on failure |

During temporary disconnection, **all messages are buffered and sent on reconnect**, regardless of persist flag or QoS level. The difference is in delivery guarantees after reconnection.

### Restart (HiveMQ Edge Process Stops and Starts)

| Scenario | Messages Preserved? | Notes |
|----------|---------------------|-------|
| IN_MEMORY mode (any config) | **No** | All queued messages lost |
| FILE_NATIVE + persist=true, QoS 0 | **No** | QoS 0 not persisted to disk by design |
| FILE_NATIVE + persist=true, QoS 1/2 | **Yes** | Full persistence and recovery |
| FILE_NATIVE + persist=false | **No** | QoS downgraded to 0, not persisted |

### Summary Table

| Event | persist=true + IN_MEMORY | persist=true + FILE_NATIVE | persist=false (any mode) |
|-------|--------------------------|----------------------------|--------------------------|
| **Temporary Disconnect** | Buffered, sent on reconnect | Buffered, sent on reconnect | Buffered, sent as QoS 0 |
| **Restart** | **LOST** | QoS 1/2: **PRESERVED**, QoS 0: LOST | **LOST** |

---

## Key Points Summary

1. **`persist` controls QoS downgrading**
   - `persist=true`: QoS levels preserved (default)
   - `persist=false`: All messages downgraded to QoS 0

2. **Two-tier queuing handles all messages**
   - Tier 1: ClientQueuePersistence (configurable storage)
   - Tier 2: In-memory buffer (for disconnection periods)

3. **During disconnection: messages are buffered and sent on reconnect**
   - All QoS levels are buffered during disconnection
   - Difference is in delivery tracking after reconnection

4. **On restart: only FILE_NATIVE + persist=true + QoS 1/2 survives**
   - IN_MEMORY mode: everything lost
   - FILE_NATIVE + persist=false: everything lost (QoS downgraded to 0)
   - FILE_NATIVE + persist=true + QoS 0: lost (QoS 0 not disk-persisted by design)

5. **QoS 0 is never upgraded**
   - QoS 0 always stays QoS 0
   - Only QoS 1/2 can be downgraded (by persist=false or maxQoS config)

6. **Free users get full QoS 1/2 support**
   - IN_MEMORY mode supports QoS 1/2 with full delivery tracking
   - Only difference from FILE_NATIVE: no restart protection

---

## Code References

| Behavior | File | Lines |
|----------|------|-------|
| persist flag QoS downgrade | `PublishDistributorImpl.java` | 242-244 |
| Always calls queuePublish() | `PublishDistributorImpl.java` | 247-253 |
| Queue add operation | `PublishDistributorImpl.java` | 268 |
| Inflight marker handling | `MessageForwarderImpl.java` | 290-297 |
| In-memory buffer on disconnect | `RemoteMqttForwarder.java` | 301-308 |
| Buffer drain on reconnect | `RemoteMqttForwarder.java` | 346-379 |
| persist flag log message | `BridgeExtractor.java` | 72-76 |
| Persistence mode enum | `PersistenceMode.java` | enum definition |

---

**Document Version**: 2.1
**Last Updated**: 2025-12-17
**Based on codebase analysis**: Verified against HiveMQ Edge source code
