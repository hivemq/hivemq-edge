# HiveMQ Edge Bridges

## Table of Contents
1. [What are MQTT Bridges?](#what-are-mqtt-bridges)
2. [Architecture Overview](#architecture-overview)
3. [How Bridges Work](#how-bridges-work)
4. [Message Flow](#message-flow)
5. [Queue Behavior During Disconnection](#queue-behavior-during-disconnection)
6. [QoS 0 Message Handling](#qos-0-message-handling)
7. [Configuration](#configuration)
8. [Key Components](#key-components)

---

## What are MQTT Bridges?

MQTT Bridges in HiveMQ Edge provide a mechanism to forward MQTT messages between different MQTT brokers. A bridge acts as a special MQTT client that:

- Subscribes to topics on the local HiveMQ Edge broker
- Publishes received messages to a remote MQTT broker
- Can subscribe to topics on the remote broker
- Forwards remote messages back to the local broker

Bridges enable hybrid architectures where edge devices publish to a local HiveMQ Edge instance, which then forwards selected messages to a central cloud broker.

### Use Cases
- **Cloud Integration**: Forward edge data to cloud-based MQTT brokers
- **Hierarchical Topologies**: Create multi-tier MQTT infrastructures
- **Data Filtering**: Forward only relevant messages to reduce bandwidth
- **Topic Transformation**: Remap topics between local and remote brokers
- **Bi-directional Communication**: Enable two-way communication between edge and cloud

---

## Architecture Overview

The HiveMQ Edge bridge implementation consists of several key components:

```
┌─────────────────────────────────────────────────────────────┐
│                      HiveMQ Edge                            │
│                                                             │
│  Local Publishers  →  TopicTree  →  ClientQueuePersistence  │
│                                           ↓                 │
│                                   MessageForwarderImpl      │
│                                           ↓                 │
│                                   RemoteMqttForwarder       │
│                                           ↓                 │
│                                   BridgeMqttClient          │
└───────────────────────────────────────────┬─────────────────┘
                                            │
                                            │ MQTT Connection
                                            ↓
                                   ┌────────────────┐
                                   │ Remote Broker  │
                                   └────────────────┘
```

---

## How Bridges Work

### Initialization

1. **Bridge Configuration**: Bridges are configured via XML or REST API
2. **BridgeService**: Manages bridge lifecycle (start, stop, restart)
3. **MQTT Client Creation**: Each bridge creates an MQTT 5 client connection
4. **Subscription Setup**: Forwarders are created for each local subscription
5. **Topic Registration**: Local topics are registered with the topic tree using shared subscriptions

### Runtime Operation

1. **Message Reception**: Local messages matching bridge subscriptions are queued
2. **Queue Persistence**: Messages are stored in ClientQueuePersistence
3. **Polling**: MessageForwarder polls queued messages
4. **Forwarding**: RemoteMqttForwarder processes and forwards messages
5. **Delivery**: Messages are published to the remote broker via BridgeMqttClient

---

## Message Flow

### Local to Remote Flow (Forward Path)

```
1. Local MQTT Client publishes message
   ↓
2. Message matched by TopicTree against bridge subscriptions
   ↓
3. Message added to ClientQueuePersistence (shared subscription queue)
   ↓
4. MessageForwarderImpl polls messages from persistence
   ↓
5. RemoteMqttForwarder receives message via onMessage()
   ↓
6. Message transformations applied:
   - Topic conversion (destination pattern)
   - QoS downgrade (if maxQoS configured)
   - User property additions
   - Loop prevention (hop count)
   ↓
7. Interceptors run (if configured)
   ↓
8. Message sent to remote broker:
   - If connected: Send immediately
   - If disconnected: Add to in-memory buffer
   ↓
9. Acknowledgment handling based on QoS
```

**Key Source Locations:**
- Message queuing: `MessageForwarderImpl.java:118-155`
- Message forwarding: `RemoteMqttForwarder.java:129-202`
- Sending logic: `RemoteMqttForwarder.java:227-255`

### Remote to Local Flow (Reverse Path)

1. Remote broker publishes message
2. BridgeMqttClient receives via RemotePublishConsumer
3. Message converted to internal PUBLISH format
4. Published to local HiveMQ Edge broker
5. Distributed to local subscribers

---

## Queue Behavior During Disconnection

### What Happens When a Bridge Disconnects?

When the bridge connection to the remote broker is lost:

1. **Connection Monitoring**: The `BridgeMqttClient` detects disconnection via MQTT client listeners
2. **Automatic Reconnection**: Exponential backoff reconnection strategy is triggered
   - Initial delay: 1 second
   - Maximum delay: 2 minutes
   - 25% jitter to prevent thundering herd
3. **Message Buffering**: Messages continue to be queued

### Two-Level Queueing System

The bridge implementation uses a **two-tier queuing approach**:

#### Level 1: Persistent Queue (ClientQueuePersistence)
- **Location**: `ClientQueuePersistence` (shared subscription queue)
- **Persistence**: Configurable (in-memory or disk-based)
- **Capacity**: Controlled by global `maxQueuedMessages` setting
- **Behavior**:
  - All messages (QoS 0, 1, 2) are stored here
  - Survives restarts if using persistent storage
  - Subject to queue limits and eviction policies

**Source**: `MessageForwarderImpl.java:118-155`

#### Level 2: In-Memory Buffer (RemoteMqttForwarder)
- **Location**: `ConcurrentLinkedQueue` in `RemoteMqttForwarder`
- **Persistence**: Volatile (in-memory only)
- **Capacity**: Unbounded (memory-limited)
- **Behavior**:
  - Messages added when `remoteMqttClient.isConnected() == false`
  - Acts as immediate buffer before attempting send
  - Lost on restart or bridge reconfiguration

**Source**: `RemoteMqttForwarder.java:72, 232-234`

```java
// RemoteMqttForwarder.java:232-234
if (!remoteMqttClient.isConnected()) {
    queue.add(new BufferedPublishInformation(queueId, originalUniqueId, originalQoS, publish));
    return;
}
```

### Queue Draining on Reconnection

When the bridge reconnects:

1. **Connection Established**: `BridgeMqttClient` fires connected event
2. **Forwarders Notified**: All forwarders receive `drainQueue()` callback
3. **In-Memory Buffer Drained**: Messages in RemoteMqttForwarder's buffer sent first
4. **Persistent Queue Polled**: MessageForwarder continues polling ClientQueuePersistence
5. **Sequential Processing**: Messages sent in order received

**Source**: `RemoteMqttForwarder.java:258-282`

---

## QoS 0 Message Handling

### Do Bridges Enqueue QoS 0 Messages During Disconnection?

**YES, QoS 0 messages ARE enqueued during bridge connectivity failures.**

This is a key design decision in HiveMQ Edge that differs from standard MQTT behavior.

### How QoS 0 Messages are Handled

#### During Normal Operation

1. Local client publishes QoS 0 message
2. Message stored in `ClientQueuePersistence` (same as QoS 1/2)
3. MessageForwarder polls and forwards message
4. RemoteMqttForwarder sends to remote broker
5. No acknowledgment tracking (fire-and-forget)

#### During Disconnection

1. QoS 0 message arrives at bridge
2. **Persisted to ClientQueuePersistence** (Level 1 queue)
3. If RemoteMqttForwarder receives it while disconnected:
   - **Buffered in memory queue** (Level 2 queue)
4. Held until reconnection
5. Sent when connection restored

### Key Differences from QoS 1/2

The primary difference is in **inflight marker handling**, not queuing:

```java
// MessageForwarderImpl.java:178-182
//QoS 0 has no inflight marker
if (qos != QoS.AT_MOST_ONCE) {
    //-- 15665 - > QoS 0 causes republishing
    FutureUtils.addExceptionLogger(queuePersistence.get().removeShared(queueId, uniqueId));
}
```

**What this means:**
- **QoS 0**: No inflight marker set, messages removed from queue after processing attempt
- **QoS 1/2**: Inflight marker set, messages only removed after confirmed delivery

**Implications:**
- QoS 0 messages are queued but not tracked for delivery confirmation
- QoS 0 messages won't be retransmitted if the forwarding attempt fails
- QoS 0 messages can be lost if:
  - Queue reaches capacity (oldest messages evicted)
  - HiveMQ Edge restarts with in-memory persistence
  - Message exceeds max queue time (if configured)

### Persistence Guarantees by QoS Level

| QoS Level | Queued? | Persisted? | Redelivered on Failure? | Survives Restart? |
|-----------|---------|------------|-------------------------|-------------------|
| QoS 0     | ✅ Yes  | ✅ Yes*    | ❌ No                   | ⚠️ Depends**      |
| QoS 1     | ✅ Yes  | ✅ Yes     | ✅ Yes                  | ✅ Yes***         |
| QoS 2     | ✅ Yes  | ✅ Yes     | ✅ Yes                  | ✅ Yes***         |

\* Persisted to ClientQueuePersistence but not tracked with inflight markers
\** Only if persistence mode is file-based and messages haven't been polled yet
\*** If using persistent storage mode

---

## Configuration

### Bridge Configuration Example

```xml
<mqtt-bridge>
    <id>cloud-bridge</id>
    <host>broker.hivemq.cloud</host>
    <port>8883</port>
    <client-id>edge-bridge-client</client-id>
    <clean-start>false</clean-start>
    <keep-alive>60</keep-alive>
    <session-expiry>3600</session-expiry>

    <local-subscriptions>
        <local-subscription>
            <filters>
                <filter>sensor/+/temperature</filter>
            </filters>
            <destination>edge/{sensor.id}/temp</destination>
            <max-qos>1</max-qos>
            <preserve-retain>true</preserve-retain>
        </local-subscription>
    </local-subscriptions>

    <remote-subscriptions>
        <remote-subscription>
            <filters>
                <filter>commands/+</filter>
            </filters>
            <max-qos>1</max-qos>
        </remote-subscription>
    </remote-subscriptions>
</mqtt-bridge>
```

### Key Configuration Parameters

- **id**: Unique identifier for the bridge
- **host/port**: Remote broker connection details
- **clean-start**: Whether to start with clean session
- **session-expiry**: Session expiry interval in seconds
- **local-subscriptions**: Topics to forward from local to remote
  - **max-qos**: Maximum QoS level for forwarded messages (downgrades higher QoS)
  - **destination**: Topic transformation pattern
  - **preserve-retain**: Whether to preserve retain flag
- **remote-subscriptions**: Topics to subscribe on remote broker
- **loop-prevention**: Hop count mechanism to prevent message loops

### Global Queue Settings

Queue behavior is controlled by global MQTT configuration:

```xml
<mqtt>
    <queued-messages>
        <max-queue-size>1000</max-queue-size>
        <strategy>discard</strategy>
    </queued-messages>
</mqtt>
```

---

## Key Components

### BridgeService
**Location**: `com.hivemq.bridge.BridgeService`

**Responsibilities**:
- Bridge lifecycle management (start, stop, restart)
- Bridge configuration synchronization
- Maintains registry of active bridges
- Coordinates with MessageForwarder for cleanup

**Key Methods**:
- `updateBridges()`: Synchronizes bridge configs
- `startBridge()`: Starts a specific bridge
- `stopBridge()`: Stops bridge and optionally clears queues
- `restartBridge()`: Restarts bridge with new config

### BridgeMqttClient
**Location**: `com.hivemq.bridge.mqtt.BridgeMqttClient`

**Responsibilities**:
- MQTT 5 client management for remote connection
- Connection lifecycle (connect, disconnect, reconnect)
- Automatic reconnection with exponential backoff
- Remote subscription management
- Connection status tracking

**Key Features**:
- Exponential backoff: 1s to 2min with 25% jitter
- Loop prevention via hop count tracking
- Event publishing for monitoring
- TLS/WebSocket support

### MessageForwarderImpl
**Location**: `com.hivemq.bridge.MessageForwarderImpl`

**Responsibilities**:
- Coordinates message forwarding across all bridges
- Polls messages from ClientQueuePersistence
- Manages shared subscription registrations
- Handles message acknowledgments
- Controls polling backpressure

**Key Features**:
- Shared subscription mechanism for load distribution
- Batch polling for efficiency
- Inflight count throttling
- Automatic queue cleanup

### RemoteMqttForwarder
**Location**: `com.hivemq.bridge.mqtt.RemoteMqttForwarder`

**Responsibilities**:
- Forwards messages from local to remote broker
- Message transformation (topic, QoS, properties)
- In-memory buffering during disconnection
- Loop prevention enforcement
- Metrics tracking

**Key Features**:
- Topic destination pattern transformation
- QoS downgrade based on maxQoS
- User property manipulation
- Interceptor chain execution
- In-memory queue for immediate buffering

### ClientQueuePersistence
**Location**: `com.hivemq.persistence.clientqueue.ClientQueuePersistence`

**Responsibilities**:
- Persistent storage of queued messages
- Shared subscription queue management
- Message batch retrieval
- Inflight marker tracking
- Queue cleanup and eviction

---

## Summary

**Key Takeaways:**

1. **QoS 0 Messages ARE Queued**: During bridge disconnection, QoS 0 messages are enqueued in ClientQueuePersistence, just like QoS 1/2 messages.

2. **Two-Tier Queueing**: Messages flow through both persistent storage (ClientQueuePersistence) and in-memory buffers (RemoteMqttForwarder).

3. **Different Tracking**: The difference between QoS levels is in delivery tracking (inflight markers), not whether messages are queued.

4. **Best Effort for QoS 0**: While QoS 0 messages are queued, they receive best-effort delivery without acknowledgment tracking or redelivery on failure.

5. **Persistence Mode Matters**: Long-term durability depends on HiveMQ Edge's persistence configuration (in-memory vs file-based).

6. **Automatic Reconnection**: Bridges automatically reconnect with exponential backoff and drain queued messages upon reconnection.

---

## Related Documentation

- HiveMQ Edge User Guide: Bridges
- MQTT 5 Specification: Quality of Service
- HiveMQ Persistence Configuration
- Bridge REST API Reference

---

**Document Version**: 1.0
**Last Updated**: 2025-11-05
**Generated from source analysis of commit**: 91e5fc4fdd7d957379481a980071759148959933