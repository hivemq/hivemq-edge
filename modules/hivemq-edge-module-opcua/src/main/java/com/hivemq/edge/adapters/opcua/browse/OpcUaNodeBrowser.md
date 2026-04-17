# OpcUaNodeBrowser — Two-Phase Streamed Browse

`OpcUaNodeBrowser` discovers every variable node in an OPC UA address space and
streams them as `BrowsedNode` records, complete with resolved attributes and
generated defaults. It does this in two phases: an async recursive **browse**
that collects lightweight references, followed by a lazy **batch-read** that
resolves attributes on demand as the caller consumes the stream.

## The Big Picture

```mermaid
flowchart LR
    subgraph "Phase 1: Discover"
        A[ObjectsFolder] -->|browseAsync| B[Collect DiscoveredVariable]
        B -->|recursive| B
        B --> C[Sort by path]
        C --> D[Deduplicate tag defaults]
    end

    subgraph "Phase 2: Stream"
        D --> E[BatchAttributeSpliterator]
        E -->|readAsync batch of 100| F[BrowsedNode]
        F -->|one at a time| G[HTTP Response]
    end

    style A fill:#f9f,stroke:#333
    style G fill:#9f9,stroke:#333
```

**Why two phases?** Browsing is cheap (just node IDs and browse names), but
attribute reads are expensive (round-trip per batch). Phase 1 materialises only
small `DiscoveredVariable` records (~5 fields). Phase 2 lazily reads attributes
in batches of 100 as the HTTP response stream is consumed — at most one batch
of full `BrowsedNode` objects lives in memory at any time.

## Phase 1: Async Recursive Browse

Starting from `ObjectsFolder` (or a user-specified root), the browser walks the
entire hierarchical reference graph, collecting every `Variable` node it finds.

```mermaid
flowchart TD
    Root["ObjectsFolder (i=85)"] -->|browse| R1["References page 1"]
    R1 -->|continuation point| R2["References page 2"]
    R1 --> Var1["Variable: ns=2;i=1001"]
    R1 --> Folder1["Folder: DataBlocks"]
    R2 --> Var2["Variable: ns=2;i=1002"]
    Folder1 -->|recurse| F1R["References"]
    F1R --> Var3["Variable: ns=2;i=2001"]
    F1R --> Var4["Variable: ns=2;i=2002"]

    Var1 -.->|add| VList["List&lt;DiscoveredVariable&gt;"]
    Var2 -.->|add| VList
    Var3 -.->|add| VList
    Var4 -.->|add| VList

    style Root fill:#f9f,stroke:#333
    style VList fill:#ff9,stroke:#333
```

### Serialised Concurrency

Initial `browseAsync` calls are serialised through a single-permit semaphore.
Continuation-point follow-ups (`browseNextAsync`) **bypass** the semaphore
because they are part of the same logical browse and must be consumed promptly
before the server expires them.

```mermaid
sequenceDiagram
    participant Browser
    participant Semaphore
    participant PLC

    Browser->>Semaphore: acquire (1→0)
    Browser->>PLC: browseAsync(ObjectsFolder)
    PLC-->>Browser: references + continuation point
    Browser->>Semaphore: release (0→1)

    note right of Browser: drain continuation pages (no semaphore)
    Browser->>PLC: browseNextAsync(continuation)
    PLC-->>Browser: more references + continuation
    Browser->>PLC: browseNextAsync(continuation)
    PLC-->>Browser: final references (no continuation)

    note right of Browser: now start child recursive browses
    Browser->>Semaphore: acquire (1→0)
    Browser->>PLC: browseAsync(child folder)
    PLC-->>Browser: references
    Browser->>Semaphore: release (0→1)
```

**Why serialise initial browses?** Resource-constrained PLCs (e.g. Siemens
S7-1500) throttle concurrent browse requests, returning `Good` status with
incomplete references or `BadTooManyOperations`. This caused non-deterministic
node counts across runs. Serialising guarantees every request gets the PLC's
full attention.

**Why do continuation pages bypass the semaphore?** Continuation points are
server-side cursors with a limited lifetime. If follow-up `browseNextAsync`
calls compete with new recursive `browseAsync` calls for the semaphore, the
recursive browses may run first — and by the time the continuation point is
consumed, the PLC has expired it (`Bad_ContinuationPointInvalid`). This was
observed on Siemens S7-1500 at depth ≥ 3. The fix: `drainContinuationPages()`
consumes all pages immediately after the initial browse, then starts child
recursive browses.

### Deduplication

OPC UA address spaces are directed graphs, not trees. A node can be reachable
via multiple paths. A `ConcurrentHashMap`-backed `Set<NodeId>` tracks visited
nodes and deduplicates at two levels:

- **Folder nodes** — `visited.add(browseRoot)` in `browseRecursive` prevents
  re-traversal and infinite cycles.
- **Variable nodes** — `visited.add(nodeId)` in `handleBrowseResult` prevents
  duplicate entries in the result list.

### Status Code Enforcement

Every `BrowseResult` is checked for non-Good status:

```
if (!browseResult.getStatusCode().isGood()) {
    throw UncheckedBrowseException(...)
}
```

Without this, a throttled PLC silently returns zero references with Good-looking
empty results — entire subtrees vanish from the output without any error.

### After Collection

Once all variable references are collected, two post-processing steps run
before the stream is created:

1. **Sort by path** — `variables.sort(comparing(path))`. Since
   `DiscoveredVariable` is small (~5 fields), sorting here is cheap. This
   guarantees the output stream is ordered without needing to materialise the
   full `BrowsedNode` list.

2. **Deduplicate tag name defaults** — The full sanitised path is used as the
   default tag name (`/A/B/C` → `a-b-c`). When multiple nodes share the same
   path (e.g. Prosys simulation instances), a numeric suffix is appended:
   `name`, `name-2`, `name-3`.

## Phase 2: Lazy Batch Attribute Reads

The caller receives a `Stream<BrowsedNode>`. Under the hood, a custom
`BatchAttributeSpliterator` lazily reads attributes in batches of 100.

```mermaid
flowchart TD
    subgraph "Sorted DiscoveredVariable list (N items)"
        V1["[0..99]"]
        V2["[100..199]"]
        V3["[200..N]"]
    end

    subgraph "BatchAttributeSpliterator"
        TA["tryAdvance()"]
    end

    subgraph "OPC UA Server"
        RD["readAsync(batch * 3 attributes)"]
    end

    subgraph "HTTP Response Stream"
        BN1["BrowsedNode 0"]
        BN2["BrowsedNode 1"]
        BNx["..."]
    end

    V1 -->|first tryAdvance| TA
    TA -->|"3 ReadValueIds per node"| RD
    RD -->|"DataType, AccessLevel, Description"| TA
    TA --> BN1
    TA --> BN2
    TA --> BNx
    V2 -.->|"next batch when [0..99] exhausted"| TA
    V3 -.->|"next batch when [100..199] exhausted"| TA

    style TA fill:#9cf,stroke:#333
    style RD fill:#fc9,stroke:#333
```

### How tryAdvance Works

```
tryAdvance(action):
    if currentBatch has remaining items:
        emit next item
        return true

    if no more variables:
        return false

    readNextBatch():
        take next 100 DiscoveredVariables
        build 300 ReadValueIds (3 attributes x 100 nodes)
        readAsync → server returns DataValue[300]
        for each variable:
            resolve DataType via DataTypeTree
            resolve AccessLevel from UByte
            resolve Description from LocalizedText
            build BrowsedNode with pre-computed tag default
        return List<BrowsedNode>

    emit first item from new batch
    return true
```

### Memory Profile

At any point during streaming, memory holds:

| What | Size | Lifetime |
|------|------|----------|
| `List<DiscoveredVariable>` | N x ~5 fields | Entire browse |
| `List<String>` tag defaults | N strings | Entire browse |
| Current `List<BrowsedNode>` batch | up to 100 | Until batch consumed |
| `DataValue[]` from readAsync | 300 values | Single readNextBatch call |

For 583 nodes (Prosys sim server), peak memory is the full DiscoveredVariable
list (small) plus one batch of 100 BrowsedNode records. The previous design
materialised all 583 BrowsedNode records at once — 6x the peak.

## End-to-End Flow

```mermaid
sequenceDiagram
    participant Client as HTTP Client
    participant REST as DeviceTagBrowsingResourceImpl
    participant Browser as OpcUaNodeBrowser
    participant PLC as OPC UA Server

    Client->>REST: POST /device-tags/browse (Accept: text/csv)

    rect rgb(240, 230, 255)
        note right of Browser: Phase 1: Discover
        Browser->>PLC: browseAsync(ObjectsFolder)
        PLC-->>Browser: references
        Browser->>PLC: browseAsync(child1)
        PLC-->>Browser: references
        Browser->>PLC: browseAsync(child2)
        PLC-->>Browser: references + continuation
        Browser->>PLC: browseNextAsync(continuation)
        PLC-->>Browser: more references
        note right of Browser: Sort + deduplicate defaults
    end

    Browser-->>REST: Stream<BrowsedNode>

    rect rgb(230, 255, 230)
        note right of REST: Phase 2: Stream
        REST->>Client: HTTP 200 (chunked transfer)
        REST->>Browser: tryAdvance()
        Browser->>PLC: readAsync(batch 0..99, 3 attrs each)
        PLC-->>Browser: DataValue[300]
        Browser-->>REST: BrowsedNode[0..99]
        REST->>Client: CSV rows 1-100

        REST->>Browser: tryAdvance()
        Browser->>PLC: readAsync(batch 100..199, 3 attrs each)
        PLC-->>Browser: DataValue[300]
        Browser-->>REST: BrowsedNode[100..199]
        REST->>Client: CSV rows 101-200
    end

    REST->>Client: (stream complete)
```

## Hardening Timeline

| Problem | Root Cause | Fix |
|---------|-----------|-----|
| Non-deterministic node counts (32 concurrent) | PLC returns BadTooManyOperations with zero references, silently dropping subtrees | Check `BrowseResult.getStatusCode()`, throw on non-Good |
| Non-deterministic at concurrency 4 | `browseNextAsync` bypassed semaphore, overlapping with `browseAsync` | Route initial browse ops through semaphore, reduce to 1 |
| Duplicate `tag_name_default` (parent-folder only) | `/A/B/Icon` and `/A/C/Icon` both produce `b-icon` | Use full sanitised path: `a-b-icon` vs `a-c-icon` |
| Duplicate defaults (identical paths) | Simulation instances share same browse path | Post-processing: append `-2`, `-3` suffix on collision |
| `Bad_ContinuationPointInvalid` on S7-1500 at depth ≥ 3 | Continuation point follow-ups competed with recursive browses for the semaphore; recursive browses ran first, continuation point expired on the PLC | `drainContinuationPages()` consumes all continuation pages immediately (bypassing semaphore), before starting child recursive browses |
