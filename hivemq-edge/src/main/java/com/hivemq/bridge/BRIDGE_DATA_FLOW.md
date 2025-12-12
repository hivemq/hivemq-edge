
## Bridge Architecture Overview

### Core Components

```
┌─────────────────────────────────────────────────────────────────────────┐
│                              HiveMQ Edge                                │
│┌───────────────────────────────────────────────────────────────────────┐│
││                           BridgeService                               ││
││  ┌──────────────────┐  ┌──────────────────┐  ┌──────────────────────┐ ││
││  │  BridgeMqttClient│  │ MessageForwarder │  │ RemotePublishConsumer│ ││
││  │  (to Remote)     │  │ (Local→Remote)   │  │ (Remote→Local)       │ ││
││  └────────┬─────────┘  └────────┬─────────┘  └──────────┬───────────┘ ││
││           │                     │                       │             ││
││           │    ┌────────────────┴────────────────┐      │             ││
││           │    │      RemoteMqttForwarder        │      │             ││
││           │    │  - drainQueue()                 │      │             ││
││           │    │  - sendPublish()                │      │             ││
││           │    │  - finishProcessing()           │      │             ││
││           │    │  - inflightCounter              │      │             ││
││           │    └────────────────┬────────────────┘      │             ││
││           │                     │                       │             ││
││  ┌────────┴─────────────────────┴───────────────────────┴────────┐    ││
││  │                    Persistence Layer                          │    ││
││  │  - In-Memory Queue (default)                                  │    ││
││  │  - RocksDB Queue (FILE_NATIVE)                                │    ││
││  │  - Inflight markers (packetId tracking)                       │    ││
││  └───────────────────────────────────────────────────────────────┘    ││
│└───────────────────────────────────────────────────────────────────────┘│
└─────────────────────────────────────────────────────────────────────────┘
                                     │
                                     │ MQTT (TCP/TLS/WebSocket)
                                     ▼
                          ┌──────────────────────┐
                          │    Remote Broker     │
                          └──────────────────────┘
```

### Bridge Configuration Options

| Option | Type | Description | Default |
|--------|------|-------------|---------|
| `id` | String | Unique bridge identifier | Required |
| `host` | String | Remote broker hostname | Required |
| `port` | int | Remote broker port | Required |
| `clientId` | String | MQTT client ID for bridge | Required |
| `keepAlive` | int | MQTT keep-alive interval (seconds) | 60 |
| `sessionExpiry` | long | Session expiry interval (seconds) | 3600 |
| `cleanStart` | boolean | Start with clean session | false |
| `username` | String | Authentication username | null |
| `password` | String | Authentication password | null |
| `bridgeTls` | BridgeTls | TLS configuration | null |
| `bridgeWebsocketConfig` | BridgeWebsocketConfig | WebSocket configuration | null |
| `localSubscriptions` | List | Topics to forward Local→Remote | [] |
| `remoteSubscriptions` | List | Topics to pull Remote→Local | [] |
| `loopPreventionEnabled` | boolean | Enable hop count tracking | true |
| `loopPreventionHopCount` | int | Maximum hop count | 1 |
| `persist` | boolean | Persist messages to disk | true |

### LocalSubscription Options

| Option | Type | Description | Default |
|--------|------|-------------|---------|
| `filters` | List<String> | Topic filters to match | Required |
| `destination` | String | Destination topic pattern | null |
| `excludes` | List<String> | Topic patterns to exclude | [] |
| `customUserProperties` | List | User properties to add | [] |
| `preserveRetain` | boolean | Preserve retain flag | false |
| `maxQoS` | int | Maximum QoS level (0, 1, 2) | 2 |
| `queueLimit` | Long | Per-subscription queue limit | null |

### RemoteSubscription Options

| Option | Type | Description | Default |
|--------|------|-------------|---------|
| `filters` | List<String> | Topic filters to subscribe | Required |
| `destination` | String | Local destination topic pattern | null |
| `customUserProperties` | List | User properties to add | [] |
| `preserveRetain` | boolean | Preserve retain flag | false |
| `maxQoS` | int | Maximum QoS level (0, 1, 2) | 2 |

---

## Data Flow Scenarios

### 1. Local-to-Remote Forwarding (Push)

```mermaid
sequenceDiagram
    participant LP as Local Publisher
    participant LE as Local Edge Broker
    participant MF as MessageForwarder
    participant RMF as RemoteMqttForwarder
    participant Q as Queue (Memory/RocksDB)
    participant BMC as BridgeMqttClient
    participant RB as Remote Broker

    LP->>LE: PUBLISH (topic matches filter)
    LE->>MF: onMessage(publish)
    MF->>MF: Check loop prevention (hop count)
    MF->>MF: Apply topic transformation
    MF->>MF: Add custom user properties

    alt Bridge Connected
        MF->>RMF: forward(message)
        RMF->>RMF: inflightCounter.incrementAndGet()
        RMF->>BMC: sendPublish(message)
        BMC->>RB: PUBLISH
        RB-->>BMC: PUBACK (QoS 1) / PUBREC-PUBREL-PUBCOMP (QoS 2)
        BMC-->>RMF: onPublishComplete
        RMF->>RMF: finishProcessing()
        RMF->>RMF: inflightCounter.decrementAndGet()
    else Bridge Disconnected
        MF->>Q: store(message)
        Note over Q: Message persisted with packetId marker
    end
```

### 2. Remote-to-Local Subscription (Pull)

```mermaid
sequenceDiagram
    participant RB as Remote Broker
    participant BMC as BridgeMqttClient
    participant RPC as RemotePublishConsumer
    participant BIH as BridgeInterceptorHandler
    participant LE as Local Edge Broker
    participant LS as Local Subscriber

    Note over BMC,RB: Bridge subscribes on connect
    BMC->>RB: SUBSCRIBE (remote filters)
    RB-->>BMC: SUBACK

    RB->>BMC: PUBLISH (matching topic)
    BMC->>RPC: onPublish(message)
    RPC->>RPC: Apply topic transformation
    RPC->>RPC: Check loop prevention
    RPC->>BIH: intercept(message)
    BIH->>LE: publishToLocal(message)
    LE->>LS: PUBLISH
```

### 3. Reconnection Flow (Normal)

```mermaid
sequenceDiagram
    participant RMF as RemoteMqttForwarder
    participant Q as Queue
    participant BMC as BridgeMqttClient
    participant RB as Remote Broker

    Note over BMC,RB: Connection Lost
    BMC-xRB: Connection broken

    Note over RMF: Messages queued during outage
    loop New messages arrive
        RMF->>Q: store(message)
    end

    Note over BMC,RB: Reconnection
    BMC->>RB: CONNECT
    RB-->>BMC: CONNACK
    BMC->>BMC: onConnected callback

    Note over RMF: Drain queued messages
    RMF->>RMF: drainQueue()
    loop For each queued message
        RMF->>Q: readShared()
        Q-->>RMF: message (with packetId marker)
        RMF->>BMC: sendPublish(message)
        BMC->>RB: PUBLISH
        RB-->>BMC: PUBACK
        RMF->>Q: removeShared(message)
    end
```

### 4. Reconnection with Network Disruption

```mermaid
sequenceDiagram
    participant P as Publisher
    participant RMF as RemoteMqttForwarder
    participant Q as RocksDB Queue
    participant BMC as BridgeMqttClient
    participant NW as Network
    participant RB as Remote Broker

    Note over RMF: Normal operation
    P->>RMF: onMessage()
    RMF->>RMF: inflightCounter++ (now 1)
    RMF->>BMC: sendPublish()

    Note over NW: Network disruption
    BMC->>NW: PUBLISH packet
    NW-xBMC: Connection lost
    Note over BMC: ChannelOutputShutdownException

    Note over RMF: Handle failed publish
    RMF->>RMF: finishProcessing()
    RMF->>RMF: inflightCounter-- (now 0)
    RMF->>Q: Clear packetId markers

    Note over BMC,RB: Reconnection
    BMC->>RB: CONNECT
    RB-->>BMC: CONNACK

    Note over RMF: Drain queued messages
    RMF->>RMF: drainQueue()
    RMF->>Q: readShared()
    Q-->>RMF: message (markers cleared, ready to send)
    RMF->>BMC: sendPublish(message)
    BMC->>RB: PUBLISH
    RB-->>BMC: PUBACK
    RMF->>Q: removeShared(message)

    Note over RMF: Bridge resumes forwarding
```

### 5. TLS Connection Flow

```mermaid
sequenceDiagram
    participant BMC as BridgeMqttClient
    participant TLS as TLS Layer
    participant RB as Remote Broker

    BMC->>TLS: Initialize SSLContext
    Note over TLS: Load truststore (server cert validation)
    Note over TLS: Load keystore (client cert - mutual TLS)

    BMC->>TLS: Connect
    TLS->>RB: ClientHello
    RB-->>TLS: ServerHello + Certificate
    TLS->>TLS: Validate server certificate

    alt Mutual TLS
        RB-->>TLS: CertificateRequest
        TLS->>RB: Client Certificate
        RB->>RB: Validate client certificate
    end

    TLS->>RB: Finished
    RB-->>TLS: Finished
    Note over TLS: TLS Handshake Complete

    BMC->>RB: MQTT CONNECT (encrypted)
    RB-->>BMC: CONNACK (encrypted)
```

### 6. WebSocket Connection Flow

```mermaid
sequenceDiagram
    participant BMC as BridgeMqttClient
    participant WS as WebSocket Layer
    participant RB as Remote Broker

    BMC->>WS: Configure WebSocket
    Note over WS: path: /mqtt (configurable)
    Note over WS: subProtocol: mqtt (configurable)

    WS->>RB: HTTP Upgrade Request
    Note over WS: GET /mqtt HTTP/1.1
    Note over WS: Upgrade: websocket
    Note over WS: Sec-WebSocket-Protocol: mqtt

    RB-->>WS: HTTP 101 Switching Protocols
    Note over WS: WebSocket connection established

    BMC->>RB: MQTT CONNECT (over WebSocket frames)
    RB-->>BMC: CONNACK
```

### 7. Loop Prevention Flow

```mermaid
sequenceDiagram
    participant E1 as Edge 1
    participant B1 as Bridge 1
    participant E2 as Edge 2
    participant B2 as Bridge 2

    Note over E1,E2: Bidirectional bridge setup
    Note over E1,E2: hopCount = 2

    E1->>E1: Local publish (hopCount = 0)
    E1->>B1: Forward to E2
    B1->>B1: Increment hopCount (now 1)
    B1->>E2: PUBLISH with hopCount=1

    E2->>B2: Forward back to E1?
    B2->>B2: Increment hopCount (now 2)
    B2->>E1: PUBLISH with hopCount=2

    E1->>B1: Forward again?
    B1->>B1: Check hopCount (2 >= maxHop 2)
    Note over B1: BLOCKED - loop prevented!
```

---
