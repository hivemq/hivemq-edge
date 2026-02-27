# QoS and Persistence Visual Guide

This document provides visual diagrams illustrating message flow through HiveMQ Edge bridges under various scenarios.

---

## Table of Contents

1. [Normal Operation](#normal-operation)
   - [QoS 0 with persist=true](#qos-0-with-persisttrue-normal-operation)
   - [QoS 1 with persist=true](#qos-1-with-persisttrue-normal-operation)
   - [QoS 2 with persist=true](#qos-2-with-persisttrue-normal-operation)
   - [QoS 1/2 with persist=false (Downgrade)](#qos-12-with-persistfalse-downgrade-scenario)
2. [Persistence Modes](#persistence-modes)
   - [IN_MEMORY Mode](#in_memory-mode)
   - [FILE_NATIVE Mode](#file_native-mode)
3. [Disconnect Scenarios](#disconnect-scenarios)
   - [Two-Tier Buffering](#two-tier-buffering-during-disconnect)
   - [QoS Comparison During Disconnect](#qos-comparison-during-disconnect)
4. [Reconnect Scenarios](#reconnect-scenarios)
   - [Exponential Backoff State Machine](#exponential-backoff-reconnection)
   - [Message Flow on Reconnect](#message-flow-on-reconnect)
5. [Intermittent Network Scenarios](#intermittent-network-scenarios)
   - [Message Lifecycle State Machine](#message-lifecycle-in-intermittent-network)
   - [QoS Behavior Comparison](#qos-behavior-comparison-intermittent-network)
6. [Restart Scenarios](#restart-scenarios)
   - [Data Survival by Mode](#data-survival-on-restart)

---

## Normal Operation

### QoS 0 with persist=true (Normal Operation)

```mermaid
sequenceDiagram
    autonumber
    participant LP as Local Publisher
    participant TT as TopicTree
    participant PD as PublishDistributor
    participant CQP as ClientQueuePersistence<br/>(Tier 1)
    participant MF as MessageForwarder
    participant RMF as RemoteMqttForwarder
    participant RB as Remote Broker

    LP->>TT: Publish QoS 0 message
    TT->>PD: Match bridge subscription
    PD->>CQP: queuePublish(QoS 0)
    Note over CQP: Message queued<br/>NO inflight marker set

    MF->>CQP: readShared() - poll messages
    CQP-->>MF: Return message batch
    MF->>RMF: onMessage(publish)

    RMF->>RB: Publish QoS 0
    Note over RMF,RB: Fire-and-forget<br/>No ACK expected

    RMF->>MF: messageProcessed(QoS 0)
    Note over MF: QoS 0: No inflight marker<br/>to clear
```

**Key Points:**
- Message is queued in ClientQueuePersistence (Tier 1) even for QoS 0
- No inflight marker is set for QoS 0 messages
- Message is removed from queue after processing attempt, regardless of delivery success
- No acknowledgment expected from remote broker
- If send fails, message is lost (no retry mechanism for QoS 0)

---

### QoS 1 with persist=true (Normal Operation)

```mermaid
sequenceDiagram
    autonumber
    participant LP as Local Publisher
    participant TT as TopicTree
    participant PD as PublishDistributor
    participant CQP as ClientQueuePersistence<br/>(Tier 1)
    participant MF as MessageForwarder
    participant RMF as RemoteMqttForwarder
    participant RB as Remote Broker

    LP->>TT: Publish QoS 1 message
    TT->>PD: Match bridge subscription
    PD->>CQP: queuePublish(QoS 1)
    Note over CQP: Message queued<br/>Inflight marker SET

    MF->>CQP: readShared() - poll messages
    CQP-->>MF: Return message (marked inflight)
    MF->>RMF: onMessage(publish)

    RMF->>RB: Publish QoS 1
    RB-->>RMF: PUBACK (acknowledgment)
    Note over RMF: Delivery confirmed

    RMF->>MF: messageProcessed(QoS 1)
    MF->>CQP: removeShared(queueId, uniqueId)
    Note over CQP: Inflight marker CLEARED<br/>Message removed
```

**Key Points:**
- Message is queued with inflight marker set
- Inflight marker prevents message from being re-polled while in flight
- PUBACK from remote broker confirms delivery
- Only after PUBACK is the inflight marker cleared and message removed
- If PUBACK not received, message remains in queue for retry on reconnect

---

### QoS 2 with persist=true (Normal Operation)

```mermaid
sequenceDiagram
    autonumber
    participant LP as Local Publisher
    participant TT as TopicTree
    participant PD as PublishDistributor
    participant CQP as ClientQueuePersistence<br/>(Tier 1)
    participant MF as MessageForwarder
    participant RMF as RemoteMqttForwarder
    participant RB as Remote Broker

    LP->>TT: Publish QoS 2 message
    TT->>PD: Match bridge subscription
    PD->>CQP: queuePublish(QoS 2)
    Note over CQP: Message queued<br/>Inflight marker SET

    MF->>CQP: readShared() - poll messages
    CQP-->>MF: Return message (marked inflight)
    MF->>RMF: onMessage(publish)

    RMF->>RB: PUBLISH QoS 2
    RB-->>RMF: PUBREC (received)
    RMF->>RB: PUBREL (release)
    RB-->>RMF: PUBCOMP (complete)
    Note over RMF: 4-way handshake complete<br/>Exactly-once delivery guaranteed

    RMF->>MF: messageProcessed(QoS 2)
    MF->>CQP: removeShared(queueId, uniqueId)
    Note over CQP: Inflight marker CLEARED<br/>Message removed
```

**Key Points:**
- QoS 2 uses a 4-way handshake: PUBLISH → PUBREC → PUBREL → PUBCOMP
- Ensures exactly-once delivery to remote broker
- Message only removed after PUBCOMP completes the handshake
- If any step fails, message remains in queue for retry
- Most resource-intensive but most reliable delivery guarantee

---

### QoS 1/2 with persist=false (Downgrade Scenario)

```mermaid
sequenceDiagram
    autonumber
    participant LP as Local Publisher
    participant TT as TopicTree
    participant PD as PublishDistributor
    participant CQP as ClientQueuePersistence<br/>(Tier 1)
    participant MF as MessageForwarder
    participant RMF as RemoteMqttForwarder
    participant RB as Remote Broker

    LP->>TT: Publish QoS 1 or QoS 2 message
    TT->>PD: Match bridge subscription

    Note over PD: persist=false detected!
    rect rgb(255, 230, 230)
        PD->>PD: Downgrade QoS to 0
        Note over PD: appliedQoS = 0<br/>(was 1 or 2)
    end

    PD->>CQP: queuePublish(QoS 0)
    Note over CQP: Message queued<br/>NO inflight marker

    MF->>CQP: readShared() - poll messages
    CQP-->>MF: Return message
    MF->>RMF: onMessage(publish, QoS 0)

    RMF->>RB: Publish QoS 0
    Note over RMF,RB: Fire-and-forget<br/>No ACK, no retry

    RMF->>MF: messageProcessed(QoS 0)
    Note over MF: No inflight marker to clear
```

**Key Points:**
- Original QoS (1 or 2) is forcefully downgraded to QoS 0
- This happens in `PublishDistributorImpl.java:242-244`
- Message is still queued (not skipped)
- No inflight marker set (QoS 0 behavior)
- No delivery guarantee - message may be lost if send fails
- Log message warns: "QoS for publishes from local subscriptions will be downgraded to AT_MOST_ONCE"

---

## Persistence Modes

### IN_MEMORY Mode

```mermaid
flowchart TB
    subgraph cluster_inmem ["IN_MEMORY Mode (Default - Free)"]
        direction TB

        MSG[/"Incoming Message"/]

        subgraph storage ["Storage Layer"]
            direction LR
            HASH["HashMap<br/>(bucketed)"]
            LIST["LinkedList<br/>(per client)"]
            HASH --> LIST
        end

        MSG --> storage
        storage --> POLL["MessageForwarder<br/>polls messages"]

        subgraph restart_effect ["On Restart"]
            direction TB
            LOST["ALL DATA LOST"]
            style LOST fill:#ff6b6b,color:white
        end

        storage -.->|"Process stops"| restart_effect
    end

    style cluster_inmem fill:#e8f4fd
```

**Key Points:**
- Default persistence mode, available to all users (free)
- Data stored in HashMap + LinkedList structures in RAM
- Fast performance, low latency
- **All data lost on restart** - no disk persistence
- QoS 1/2 delivery tracking works during runtime
- Suitable for scenarios where restart data loss is acceptable

---

### FILE_NATIVE Mode

```mermaid
flowchart TB
    subgraph cluster_file ["FILE_NATIVE Mode (Licensed)"]
        direction TB

        MSG[/"Incoming Message"/]

        subgraph storage ["Storage Layer"]
            direction TB
            CQP["ClientQueuePersistence"]
            DISK[("Disk Storage")]
            CQP --> DISK
        end

        MSG --> storage
        storage --> POLL["MessageForwarder<br/>polls messages"]

        subgraph restart_effect ["On Restart"]
            direction TB
            QOS0["QoS 0: LOST<br/>(not persisted)"]
            QOS12["QoS 1/2: RESTORED"]
            style QOS0 fill:#ff6b6b,color:white
            style QOS12 fill:#51cf66,color:white
        end

        DISK -.->|"Process stops"| restart_effect
    end

    style cluster_file fill:#fff3e0
```

**Key Points:**
- Requires commercial license
- QoS 1/2 messages persisted to disk
- QoS 0 messages NOT persisted (by MQTT design - fire-and-forget has no persistence guarantee)
- **QoS 1/2 messages survive restarts**
- Slightly higher latency due to disk I/O
- Recommended for production where message durability is critical

---

### Persistence Mode Comparison

```mermaid
flowchart LR
    subgraph legend ["Legend"]
        PRESERVED["PRESERVED"]
        LOST["LOST"]
        style PRESERVED fill:#51cf66,color:white
        style LOST fill:#ff6b6b,color:white
    end

    subgraph before ["Before Restart"]
        direction TB
        Q_MEM["IN_MEMORY Queue<br/>10 QoS 1 messages"]
        Q_FILE["FILE_NATIVE Queue<br/>10 QoS 1 messages"]
        Q_FILE_Q0["FILE_NATIVE Queue<br/>10 QoS 0 messages"]
        BUF["Tier 2 Buffer<br/>5 messages (any QoS)"]
    end

    RESTART(("RESTART"))

    subgraph after ["After Restart"]
        direction TB
        Q_MEM_AFTER["0 messages"]
        Q_FILE_AFTER["10 messages"]
        Q_FILE_Q0_AFTER["0 messages"]
        BUF_AFTER["0 messages"]
        style Q_MEM_AFTER fill:#ff6b6b,color:white
        style Q_FILE_AFTER fill:#51cf66,color:white
        style Q_FILE_Q0_AFTER fill:#ff6b6b,color:white
        style BUF_AFTER fill:#ff6b6b,color:white
    end

    Q_MEM --> RESTART --> Q_MEM_AFTER
    Q_FILE --> RESTART --> Q_FILE_AFTER
    Q_FILE_Q0 --> RESTART --> Q_FILE_Q0_AFTER
    BUF --> RESTART --> BUF_AFTER
```

**Key Points:**
- IN_MEMORY mode: All messages lost on restart
- FILE_NATIVE mode with QoS 1/2: Messages preserved
- FILE_NATIVE mode with QoS 0: Messages lost (not persisted by design)
- Tier 2 in-memory buffer: Always lost on restart (volatile RAM)

---

## Disconnect Scenarios

### Two-Tier Buffering During Disconnect

```mermaid
sequenceDiagram
    autonumber
    participant LP as Local Publisher
    participant CQP as ClientQueuePersistence<br/>(Tier 1)
    participant MF as MessageForwarder
    participant RMF as RemoteMqttForwarder
    participant IMB as In-Memory Buffer<br/>(Tier 2)
    participant RB as Remote Broker

    Note over RB: CONNECTION LOST
    rect rgb(255, 200, 200)
        Note over RMF,RB: Disconnected State
    end

    LP->>CQP: Publish message
    Note over CQP: Message stored in Tier 1

    MF->>CQP: readShared() - poll
    CQP-->>MF: Return message
    MF->>RMF: onMessage(publish)

    RMF->>RMF: isConnected() = false

    rect rgb(255, 243, 224)
        RMF->>IMB: queue.add(BufferedPublishInformation)
        Note over IMB: Message buffered in Tier 2<br/>Waiting for reconnection
    end

    Note over IMB: Messages accumulate<br/>during disconnection

    LP->>CQP: Another message
    MF->>CQP: poll
    MF->>RMF: onMessage
    RMF->>IMB: buffer
```

**Key Points:**
- When remote broker is disconnected, messages are buffered in Tier 2 (in-memory buffer)
- Tier 1 (ClientQueuePersistence) continues receiving new messages
- MessageForwarder continues polling from Tier 1
- RemoteMqttForwarder detects `isConnected() == false` and buffers instead of sending
- Code location: `RemoteMqttForwarder.java:301-308`
- Buffer is unbounded (limited only by available memory)

---

### QoS Comparison During Disconnect

```mermaid
flowchart TB
    subgraph disconnect ["During Disconnection - All QoS Levels"]
        direction TB

        subgraph qos0 ["QoS 0 - persist=true or false"]
            Q0_MSG["Message arrives"] --> Q0_TIER1["Queued in Tier 1"]
            Q0_TIER1 --> Q0_POLL["Polled by MessageForwarder"]
            Q0_POLL --> Q0_CHECK{"Connected?"}
            Q0_CHECK -->|No| Q0_BUFFER["Buffered in Tier 2"]
            Q0_BUFFER --> Q0_WAIT["Wait for reconnect"]
        end

        subgraph qos1 ["QoS 1 - persist=true"]
            Q1_MSG["Message arrives"] --> Q1_TIER1["Queued in Tier 1 + Inflight marker"]
            Q1_TIER1 --> Q1_POLL["Polled by MessageForwarder"]
            Q1_POLL --> Q1_CHECK{"Connected?"}
            Q1_CHECK -->|No| Q1_BUFFER["Buffered in Tier 2"]
            Q1_BUFFER --> Q1_WAIT["Wait for reconnect"]
            Q1_TIER1 -.->|Inflight marker keeps message for retry| Q1_TIER1
        end

        subgraph qos1_false ["QoS 1 - persist=false"]
            Q1F_MSG["Message arrives"] --> Q1F_DOWN["Downgraded to QoS 0"]
            Q1F_DOWN --> Q1F_TIER1["Queued in Tier 1 - NO inflight marker"]
            Q1F_TIER1 --> Q1F_BUFFER["Buffered in Tier 2"]
        end
    end

    style Q0_MSG fill:#74c0fc
    style Q1_MSG fill:#69db7c
    style Q1F_MSG fill:#ffa94d
    style Q1F_DOWN fill:#ff8787
```

**Key Points:**
- ALL QoS levels are buffered during disconnection
- The difference is in inflight marker handling:
  - QoS 0: No marker, removed after attempt
  - QoS 1/2: Marker set, kept for retry
- persist=false forces QoS downgrade BEFORE queueing
- Tier 2 buffer holds messages until reconnection regardless of QoS

---

## Reconnect Scenarios

### Exponential Backoff Reconnection

```mermaid
stateDiagram-v2
    [*] --> Connected

    Connected --> Disconnected: Connection lost

    Disconnected --> Reconnecting: Auto reconnect

    state Reconnecting {
        [*] --> Attempt1
        Attempt1: Wait 1s
        Attempt1 --> Attempt2: Failed
        Attempt2: Wait 2s
        Attempt2 --> Attempt3: Failed
        Attempt3: Wait 4s
        Attempt3 --> Attempt4: Failed
        Attempt4: Wait 8s
        Attempt4 --> AttemptN: Failed
        AttemptN: Wait up to 2min with jitter
        AttemptN --> AttemptN: Retry
    }

    Reconnecting --> Recovered: Success

    state Recovered {
        [*] --> DrainBuffers
        DrainBuffers: Drain Tier 2 buffer
        DrainBuffers --> ResumePoll
        ResumePoll: Resume Tier 1 polling
        ResumePoll --> NormalOp
        NormalOp: Normal operation
    }

    Recovered --> Connected
```

**Key Points:**
- Exponential backoff prevents overwhelming the remote broker
- Initial delay: 1 second
- Doubles each attempt: 1s → 2s → 4s → 8s → ...
- Maximum delay: 2 minutes
- 25% jitter added to prevent thundering herd problem
- On success: immediately drain buffers and resume normal operation

---

### Message Flow on Reconnect

```mermaid
sequenceDiagram
    autonumber
    participant CQP as ClientQueuePersistence<br/>(Tier 1)
    participant MF as MessageForwarder
    participant RMF as RemoteMqttForwarder
    participant IMB as In-Memory Buffer<br/>(Tier 2)
    participant BMC as BridgeMqttClient
    participant RB as Remote Broker

    Note over BMC,RB: Reconnection successful!

    BMC->>RMF: onConnected() callback
    RMF->>RMF: flushBufferedMessages()

    rect rgb(224, 255, 224)
        Note over RMF,IMB: Phase 1: Drain Tier 2 Buffer First
        loop For each buffered message
            RMF->>IMB: poll()
            IMB-->>RMF: BufferedPublishInformation
            RMF->>RB: Publish message
            alt QoS 1/2
                RB-->>RMF: ACK
                RMF->>MF: messageProcessed()
                MF->>CQP: removeShared()
            else QoS 0
                Note over RMF: No ACK expected
            end
        end
    end

    rect rgb(224, 240, 255)
        Note over MF,CQP: Phase 2: Resume Normal Polling
        loop Continue normal operation
            MF->>CQP: readShared()
            CQP-->>MF: Message batch
            MF->>RMF: onMessage()
            RMF->>RB: Publish
        end
    end
```

**Key Points:**
- On reconnect, Tier 2 buffer is drained FIRST
- This ensures message ordering is preserved
- Buffered messages retain their original QoS for ACK handling
- After buffer is drained, normal Tier 1 polling resumes
- QoS 1/2 messages: inflight markers cleared only after ACK
- QoS 0 messages: processed immediately without ACK

---

### Reconnect with QoS 1/2 Retry

```mermaid
sequenceDiagram
    autonumber
    participant CQP as ClientQueuePersistence<br/>(Tier 1)
    participant MF as MessageForwarder
    participant RMF as RemoteMqttForwarder
    participant RB as Remote Broker

    Note over CQP: QoS 1 message in queue<br/>Inflight marker SET<br/>(from before disconnect)

    Note over RMF,RB: Reconnected!

    RMF->>RMF: drainQueue()
    Note over RMF: Reset inflight markers<br/>for messages that weren't ACKed

    MF->>CQP: readShared()
    Note over CQP: Returns message again<br/>(inflight was reset)
    CQP-->>MF: QoS 1 message

    MF->>RMF: onMessage(publish)
    RMF->>RB: Publish QoS 1 (retry)
    RB-->>RMF: PUBACK

    RMF->>MF: messageProcessed(QoS 1)
    MF->>CQP: removeShared()
    Note over CQP: NOW message is removed<br/>Inflight marker cleared
```

**Key Points:**
- QoS 1/2 messages that weren't ACKed before disconnect are retried
- `drainQueue()` resets inflight markers so messages can be re-polled
- This ensures at-least-once (QoS 1) or exactly-once (QoS 2) delivery
- Message only removed from Tier 1 after successful ACK
- Code location: `RemoteMqttForwarder.java:346-379`

---

## Intermittent Network Scenarios

### Message Lifecycle in Intermittent Network

```mermaid
stateDiagram-v2
    [*] --> Received: Message arrives

    Received --> Tier1Queued: Queued in<br/>ClientQueuePersistence

    Tier1Queued --> Polled: MessageForwarder<br/>polls message

    state check_connection <<choice>>
    Polled --> check_connection: Check connection

    check_connection --> Sending: Connected
    check_connection --> Tier2Buffered: Disconnected

    Tier2Buffered --> Sending: Reconnected

    state send_result <<choice>>
    Sending --> send_result: Send attempt

    send_result --> WaitingACK: Sent (QoS 1/2)
    send_result --> Delivered: Sent (QoS 0)
    send_result --> SendFailed: Network error

    state ack_result <<choice>>
    WaitingACK --> ack_result: ACK status

    ack_result --> Delivered: ACK received
    ack_result --> RetryPending: ACK timeout

    RetryPending --> Sending: Reconnected

    state qos_check <<choice>>
    SendFailed --> qos_check: Check QoS

    qos_check --> Lost: QoS 0 (no retry)
    qos_check --> RetryPending: QoS 1/2 (will retry)

    Delivered --> [*]: Success
    Lost --> [*]: Message lost
```

**Key Points:**
- Message lifecycle depends on QoS level and network state
- QoS 0: Single attempt, lost on failure
- QoS 1/2: Retry on failure, wait for ACK
- Intermittent network causes messages to cycle through buffered/sending states
- Only successful ACK (QoS 1/2) or send attempt (QoS 0) ends the lifecycle

---

### QoS Behavior Comparison (Intermittent Network)

```mermaid
flowchart TB
    subgraph qos0_flow ["QoS 0 Behavior (persist=true or persist=false)"]
        direction TB
        Q0_START["Message arrives"] --> Q0_QUEUE["Queued (no marker)"]
        Q0_QUEUE --> Q0_POLL["Polled"]
        Q0_POLL --> Q0_CONN{"Connected?"}
        Q0_CONN -->|"Yes"| Q0_SEND["Send"]
        Q0_CONN -->|"No"| Q0_BUF["Buffer"]
        Q0_BUF -->|"Reconnect"| Q0_SEND
        Q0_SEND --> Q0_RESULT{"Success?"}
        Q0_RESULT -->|"Yes"| Q0_DONE["Done"]
        Q0_RESULT -->|"No"| Q0_LOST["LOST"]
        style Q0_LOST fill:#ff6b6b,color:white
        style Q0_DONE fill:#51cf66,color:white
    end

    subgraph qos1_flow ["QoS 1/2 Behavior (persist=true)"]
        direction TB
        Q1_START["Message arrives"] --> Q1_QUEUE["Queued + Inflight marker"]
        Q1_QUEUE --> Q1_POLL["Polled"]
        Q1_POLL --> Q1_CONN{"Connected?"}
        Q1_CONN -->|"Yes"| Q1_SEND["Send"]
        Q1_CONN -->|"No"| Q1_BUF["Buffer"]
        Q1_BUF -->|"Reconnect"| Q1_SEND
        Q1_SEND --> Q1_ACK{"ACK?"}
        Q1_ACK -->|"Yes"| Q1_CLEAR["Clear marker"]
        Q1_ACK -->|"No/Timeout"| Q1_RETRY["Retry"]
        Q1_RETRY -->|"Reconnect"| Q1_SEND
        Q1_CLEAR --> Q1_DONE["Done"]
        style Q1_DONE fill:#51cf66,color:white
    end

    subgraph qos1_false_flow ["QoS 1/2 Behavior (persist=false)"]
        direction TB
        Q1F_START["Message arrives"] --> Q1F_DOWN["Downgrade to QoS 0"]
        Q1F_DOWN --> Q1F_FLOW["Same as QoS 0 flow"]
        style Q1F_DOWN fill:#ff8787,color:white
    end
```

**Key Points:**
- **QoS 0**: Best-effort delivery, no retry on failure
- **QoS 1/2**: Guaranteed delivery with retry mechanism
- **persist=false**: Forces QoS 0 behavior regardless of original QoS
- Intermittent network affects delivery time but not eventual delivery (for QoS 1/2)
- QoS 0 messages may be lost during network issues

---

### Network Flapping Scenario

```mermaid
sequenceDiagram
    autonumber
    participant MSG as Messages
    participant CQP as Tier 1
    participant IMB as Tier 2
    participant RMF as Forwarder
    participant NET as Network
    participant RB as Remote Broker

    Note over NET: Network UP
    MSG->>CQP: M1 (QoS 1)
    CQP->>RMF: Poll M1
    RMF->>RB: Send M1
    RB-->>RMF: ACK M1

    Note over NET: Network DOWN
    MSG->>CQP: M2 (QoS 1)
    CQP->>RMF: Poll M2
    RMF->>IMB: Buffer M2

    MSG->>CQP: M3 (QoS 1)
    CQP->>RMF: Poll M3
    RMF->>IMB: Buffer M3

    Note over NET: Network UP (briefly)
    RMF->>RB: Send M2
    Note over NET: Network DOWN (before ACK)
    RMF->>IMB: M2 retry pending

    MSG->>CQP: M4 (QoS 1)

    Note over NET: Network UP (stable)
    RMF->>RB: Send M2 (retry)
    RB-->>RMF: ACK M2
    RMF->>RB: Send M3
    RB-->>RMF: ACK M3
    CQP->>RMF: Poll M4
    RMF->>RB: Send M4
    RB-->>RMF: ACK M4
```

**Key Points:**
- Network flapping causes messages to cycle between buffered and sending states
- QoS 1/2 ensures eventual delivery despite network instability
- Message order is preserved (M2 before M3 before M4)
- Each message is tracked independently for ACK status
- Tier 2 buffer grows during network outages

---

## Restart Scenarios

### Data Survival on Restart

```mermaid
flowchart TB
    subgraph before ["State Before Restart"]
        direction LR

        subgraph t1_before ["Tier 1 (ClientQueuePersistence)"]
            T1_MEM["IN_MEMORY<br/>15 messages"]
            T1_FILE["FILE_NATIVE<br/>15 messages"]
        end

        subgraph t2_before ["Tier 2 (In-Memory Buffer)"]
            T2_BUF["Buffer<br/>8 messages"]
        end

        subgraph inflight_before ["Inflight Messages"]
            INF["3 messages<br/>awaiting ACK"]
        end
    end

    RESTART(("PROCESS<br/>RESTART"))

    subgraph after ["State After Restart"]
        direction LR

        subgraph t1_after ["Tier 1 (ClientQueuePersistence)"]
            T1_MEM_A["IN_MEMORY<br/>0 messages"]
            T1_FILE_A["FILE_NATIVE<br/>15 messages*"]
        end

        subgraph t2_after ["Tier 2 (In-Memory Buffer)"]
            T2_BUF_A["Buffer<br/>0 messages"]
        end

        subgraph inflight_after ["Inflight Messages"]
            INF_A["0 messages<br/>(reset)"]
        end

        style T1_MEM_A fill:#ff6b6b,color:white
        style T1_FILE_A fill:#51cf66,color:white
        style T2_BUF_A fill:#ff6b6b,color:white
        style INF_A fill:#ffa94d,color:white
    end

    before --> RESTART --> after

    NOTE["*Only QoS 1/2 messages preserved<br/>QoS 0 messages are not persisted to disk"]
```

**Key Points:**
- **IN_MEMORY mode**: All messages lost
- **FILE_NATIVE mode**: Only QoS 1/2 messages preserved
- **Tier 2 buffer**: Always lost (volatile RAM)
- **Inflight messages**: Reset on restart, will be re-polled and retried
- QoS 0 messages are NEVER persisted to disk (by MQTT design)

---

### Complete Restart Recovery Flow (FILE_NATIVE)

```mermaid
sequenceDiagram
    autonumber
    participant DISK as Disk Storage
    participant CQP as ClientQueuePersistence
    participant MF as MessageForwarder
    participant RMF as RemoteMqttForwarder
    participant RB as Remote Broker

    Note over DISK: Before restart:<br/>10 QoS 1 messages persisted

    rect rgb(255, 200, 200)
        Note over CQP,RMF: RESTART OCCURS
    end

    Note over DISK,CQP: Recovery Phase
    DISK->>CQP: Load persisted messages
    Note over CQP: 10 QoS 1 messages restored<br/>Inflight markers reset

    Note over MF,RB: Resume Operations
    MF->>CQP: readShared()
    CQP-->>MF: Return restored messages

    loop For each restored message
        MF->>RMF: onMessage()
        RMF->>RB: Publish
        RB-->>RMF: ACK
        RMF->>MF: messageProcessed()
        MF->>CQP: removeShared()
    end

    Note over CQP: All restored messages<br/>successfully delivered
```

**Key Points:**
- FILE_NATIVE mode loads persisted QoS 1/2 messages on startup
- Inflight markers are reset, allowing messages to be re-polled
- Normal operation resumes after recovery
- Messages are delivered as if restart never happened
- This is the key advantage of FILE_NATIVE over IN_MEMORY

---

### Restart Impact Summary

```mermaid
flowchart LR
    subgraph config ["Configuration"]
        C1["IN_MEMORY<br/>persist=true"]
        C2["IN_MEMORY<br/>persist=false"]
        C3["FILE_NATIVE<br/>persist=true"]
        C4["FILE_NATIVE<br/>persist=false"]
    end

    subgraph qos ["QoS Levels"]
        Q0["QoS 0"]
        Q1["QoS 1"]
        Q2["QoS 2"]
    end

    subgraph result ["Survives Restart?"]
        YES["YES"]
        NO["NO"]
        style YES fill:#51cf66,color:white
        style NO fill:#ff6b6b,color:white
    end

    C1 --> Q0 --> NO
    C1 --> Q1 --> NO
    C1 --> Q2 --> NO

    C2 --> Q0 --> NO
    C2 --> Q1 --> NO
    C2 --> Q2 --> NO

    C3 --> Q0 --> NO
    C3 --> Q1 --> YES
    C3 --> Q2 --> YES

    C4 --> Q0 --> NO
    C4 --> Q1 --> NO
    C4 --> Q2 --> NO
```

**Key Points:**
- **Only one combination survives restart**: FILE_NATIVE + persist=true + QoS 1 or 2
- persist=false downgrades to QoS 0, which is never persisted
- IN_MEMORY mode never persists to disk
- QoS 0 is never persisted to disk (by MQTT design)
- For restart protection: use FILE_NATIVE license + keep persist=true

---

## Summary Diagram

```mermaid
flowchart TB
    subgraph input ["Message Input"]
        PUB["Local Publisher"]
    end

    subgraph persist_check ["persist Flag Check"]
        PC{"persist?"}
        PC -->|"true"| KEEP["Keep original QoS"]
        PC -->|"false"| DOWN["Downgrade to QoS 0"]
    end

    subgraph tier1 ["Tier 1: ClientQueuePersistence"]
        direction TB
        T1_Q["Queue Message"]
        T1_INF{"QoS 1/2?"}
        T1_INF -->|"Yes"| T1_MARK["Set inflight marker"]
        T1_INF -->|"No"| T1_NOMARK["No marker"]
    end

    subgraph tier2 ["Tier 2: In-Memory Buffer"]
        T2_CHECK{"Connected?"}
        T2_CHECK -->|"No"| T2_BUF["Buffer message"]
        T2_CHECK -->|"Yes"| T2_SEND["Send directly"]
    end

    subgraph delivery ["Delivery"]
        D_SEND["Send to Remote Broker"]
        D_ACK{"ACK received?<br/>(QoS 1/2 only)"}
        D_ACK -->|"Yes"| D_CLEAR["Clear inflight marker"]
        D_ACK -->|"No/Timeout"| D_RETRY["Retry on reconnect"]
        D_QOS0["QoS 0: Fire & forget"]
    end

    PUB --> PC
    KEEP --> T1_Q
    DOWN --> T1_Q
    T1_Q --> T1_INF
    T1_MARK --> T2_CHECK
    T1_NOMARK --> T2_CHECK
    T2_BUF -->|"Reconnect"| T2_SEND
    T2_SEND --> D_SEND
    D_SEND --> D_ACK
    D_SEND --> D_QOS0
    D_RETRY -->|"Reconnect"| D_SEND
```

---

**Document Version**: 1.0
**Last Updated**: 2025-12-17
**Based on codebase analysis**: Verified against HiveMQ Edge source code

**Related Documents**:
- [Q0S.md](Q0S.md) - Technical reference for persistence and QoS behavior
- [BRIDGES.md](hivemq-edge/src/main/java/com/hivemq/bridge/BRIDGES.md) - Bridge architecture documentation
