# Authoring a v2 Protocol Adapter

This guide shows how to implement a protocol adapter against **SDK v2**
(`com.hivemq.adapter.sdk.api.v2.*`), the contract that drives the v2
protocol-adapter framework. `ChaosProtocolAdapter` in this module is the worked,
end-to-end SDK-v2 reference — read it alongside this guide.

> SDK v2 runs **side by side** with the legacy v1 framework; the two never
> interact. v2 adapters are declared in the `<v2>` config
> section (the adapter list nests under `<v2><protocol-adapters>`) and served by
> the `/api/v2/...` REST surface. Configuration is
> produced by external tooling — Edge only reads it.

## 1. The mental model

- **The PA is pure mechanism.** It executes commands (`connect`, `pollBatch`,
  `writeBatch`, …) and reports events (`connected()`, `dataPoint(...)`,
  `error(...)`) through the `ProtocolAdapterOutput` tell-façade. It owns **no**
  retry, backoff, scheduling, or reconnect logic — the wrapper (PAW) owns all
  policy.
- **It is an actor.** Each adapter has a single dispatch thread fed one message
  at a time from a mailbox. You never touch shared state off that thread; you
  report results by calling the thread-safe `output` callbacks from any thread.
- **Values cross the boundary as the reused v1 `DataPoint`.** Correlation is by
  `Node` reference — there is no lookup map.

## 2. The minimum you implement

Extend `AbstractProtocolAdapter` (it gives you the mailbox, the `tell`-ing
command methods, and the `receive` switch for free) and implement the abstract
`do*` methods on your protocol library:

```java
public final class MyAdapter extends AbstractProtocolAdapter {

    public MyAdapter(final ProtocolAdapterInput input, final ProtocolAdapterOutput output) {
        super(input, output);
    }

    @Override protected void doStart()      { /* acquire resources */ output.started(); }
    @Override protected void doStop()        { /* release resources */ output.stopped(); }
    @Override protected void doConnect()     { /* open the connection */ output.connected(); }
    @Override protected void doDisconnect()  { /* close it */ output.disconnected(); }

    @Override protected void doPoll(final Node node) {
        final Object value = readFromDevice(node);
        // Build the value with the REUSED v1 DataPointFactory (never new your own DataPoint type):
        output.dataPoint(node, dataPointFactory.create(node.nodeId(), value));
    }

    @Override protected void doAddSubscription(final Node node) { /* subscribe; push via output.dataPoint(...) */ }
    @Override protected void doWrite(final Node node, final DataPoint value) { /* write; output.writeResult(node, true, null) */ }
}
```

You may also override the batch and discovery defaults:

- `doVerifyNode(node)` — connect-time agreement check. Return
  `output.verifyResult(node, new VerifyOutcome.Success())`,
  `…TransientFailure(reason)` (the framework retries on a timer), or
  `…PermanentFailure(reason)` (the aspect suspends until a REST tag retry).
- `doBrowse(filter)` — defaults to an empty result. For large address spaces,
  implement it **asynchronously inside the PA** so polling continues between
  round-trips, and report `output.browseResult(entries)`.
- `doPollBatch` / `doVerifyBatch` / `doWriteBatch` — the defaults fan out to the
  single-node methods; override when the protocol has a native batch call.

**Threading rule.** `do*` methods run on your dispatch thread and *may block*
(a blocking `connect()` is normal); queued commands simply wait, and the PAW's
watchdogs bound the damage. A `Stop`/`Disconnect` (a `CONTROL`-band command)
jumps ahead of queued batch work, but nothing preempts an in-flight `do*`.

## 3. The factory trio

```java
public final class MyAdapterFactory implements ProtocolAdapterFactory {
    @Override public ProtocolAdapterInformation information()  { return new MyAdapterInformation(); }
    @Override public ProtocolAdapter createAdapter(ProtocolAdapterInput in, ProtocolAdapterOutput out) {
        return new MyAdapter(in, out);                 // synchronous and cheap: no I/O, no connection
    }
    @Override public Schema adapterConfigSchema()  { return /* reused v1 Schema */; }
    @Override public Schema nodeDefinitionSchema() { return /* reused v1 Schema */; }
}
```

`ProtocolAdapterInformation` is the **single** home of `capabilities()`
(`SUBSCRIPTIONS` / `WRITE` / `BROWSE`), the category/search tags (reused v1
enums), and `nodeClass()` — the class your `node-string` JSON deserializes into.
`currentConfigVersion()` returns `>= 2` for a v2 type.

### Node serialization

The framework deserializes each tag's `node-string` into your `nodeClass()`
with Jackson, so your `Node` subclass must round-trip:
`nodeString()` produces the JSON, and the class must be Jackson-deserializable
from it (a public field + no-arg constructor, or a `@JsonCreator`).
`ChaosNode` is intentionally minimal — see the note in `ProtocolAdapterV2EndToEndTest`,
which supplies a deserializer because `ChaosNode` is immutable with no creator.

## 4. Capabilities, registration, and visibility

A `ProtocolAdapterFactory` is placed into the constructor-injected
`ProtocolAdapterFactoryRegistry` as **listable** or **hidden**:

- **listable** types appear in `GET /api/v2/.../types` (the frontend catalog);
- **hidden** types are resolvable by `protocol-id` (a configured instance runs
  like any other) but are excluded from `/types`.

`ChaosProtocolAdapter` ships as a **hidden** type, injected only by
`hivemq-edge-test`. The production registry is empty (no demo adapter ships).

## 5. Status the REST surface derives

Both are pure folds of the immutable snapshots the wrapper publishes — you do
not compute them.

**Adapter machine → color** (`/adapters/{id}/status`):

| Machine state | Color |
|---|---|
| `STOPPED` | grey |
| `WAITING_FOR_STARTED` / `…_CONNECTED` / `…_VERIFICATION` | yellow (connecting) |
| `CONNECTED` | green |
| `WAITING_FOR_CONNECTION_RETRY` | amber (retrying) |
| `WAITING_FOR_DISCONNECTED*` / `WAITING_FOR_STOPPED` | yellow (stopping) |
| `ERROR` | red |

**Tag status** (combination-aware, `/adapters/{id}/tags/{tag}/status`):

| Status | When |
|---|---|
| `NORTHBOUND_AND_SOUTHBOUND` | both aspects active and operating |
| `NORTHBOUND_ONLY` | read active and operating; write deactivated |
| `SOUTHBOUND_ONLY` | write active and ready; read deactivated |
| `DEACTIVATED` | every aspect deactivated |
| `ERROR` | any aspect permanently failed, or active-but-not-yet-operating |

An aspect runs **iff** its direction is activated **and** the aspect is
activated **and** the tag is *used* (referenced by a mapping) — the
three-condition rule. Direction activation is a live REST goal (never
persisted); the per-aspect `read-activated`/`write-activated` preferences are
configuration, applied at startup and on reload without reconnecting.

## 6. Worked reference

`ChaosProtocolAdapter` (+ `ChaosProtocolAdapterFactory`,
`ChaosProtocolAdapterInformation`, `ChaosNode`, `ChaosDataPoint`) is a complete
SDK-v2 adapter. `ProtocolAdapterV2EndToEndTest` (in `hivemq-edge-test`) shows it loaded
from configuration, connected, polled, retried, and reloaded through the wired
runtime; the deterministic scenario matrix in this module's `src/test` drives
the same adapter on a `FakeClock` for exhaustive state-machine coverage.
