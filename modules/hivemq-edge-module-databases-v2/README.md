# hivemq-edge-module-databases-v2

The **Databases** protocol adapter ported to **SDK v2** — a production, listable, northbound poll-only adapter with
protocol id **`databases-v2`**. It runs side by side with the v1 `hivemq-edge-module-databases` adapter (protocol id
`databases`); the two share no configuration, factory registry, or REST surface and never interact.

`DatabasesProtocolAdapter` is the SDK-v2 reference for a **fat-jar, connection-pooled, multi-engine,
multi-value-poll** adapter (alongside the stateless `FileProtocolAdapter`, the asynchronous `HttpProtocolAdapter`,
and the synthetic `ChaosProtocolAdapter`): it extends `AbstractProtocolAdapter`, opens a HikariCP pool against
**PostgreSQL, MySQL (via the MariaDB driver), or MS SQL** on connect, and executes each tag's SQL query on its
scheduled poll, publishing the result set as JSON — one array message, or one message per row.

## The fat jar

Unlike the File and HTTP v2 modules, this module's shadow jar **does bundle runtime dependencies**: the PostgreSQL,
MariaDB, and MSSQL JDBC drivers plus the HikariCP pool are shaded into the module jar (with a third-party-license
aggregation and a MariaDB LGPL exclusion, mirroring the v1 module). The module loader isolates each module's
classloader, so `doConnect` explicitly registers the three bundled drivers under the module's own classloader and
points the thread context classloader at it while HikariCP creates the data source.

## The framework enhancement — multi-value polls

The v1 adapter's `spiltLinesInIndividualMessages` (the historical key spelling is preserved) publishes **one MQTT
message per result row** — N values per poll. The v2 polled-read aspect previously ended a poll on its **first**
value, absorbing the rest. To keep this feature exactly, this port aligned the poll value-message with the subscribe
value-message so a poll may emit **0..N** values, carried by three `ProtocolAdapterOutput` methods:

- `dataPoint(node, value)` — one value that **also completes** the poll (the common single-value case, and every
  subscription push). Unchanged for single-value adapters.
- `dataPoints(node, values)` — zero or more values that do **not** complete the poll; call it (repeatedly, to stream
  a cursor per page without materializing it whole) and end the poll with an explicit `pollComplete`.
- `pollComplete(node)` — completes a poll with no (further) value: an empty result set, or the terminator after one
  or more `dataPoints`. A `nodeError` also terminates a poll (a mid-stream failure never hangs the tag).

Internally the completion rides on the value message: `dataPoint` tells `DataPointReceived(…, completesPoll=true)`
and `dataPoints` tells `DataPointReceived(…, completesPoll=false)` per value; both ride the **`DATA`** band with
`PollCompleted`, so within-band FIFO delivers a completion strictly after the values it terminates. The polled read
aspect publishes every value and stays in `WAITING_FOR_POLL_DATAPOINT` until a completing value, a `pollComplete`, or
a `nodeError` schedules the next poll; a subscribed aspect ignores the completion bit. `AbstractProtocolAdapter`'s
`doPollBatch` is just a loop over `doPoll` — it never completes a poll on the adapter's behalf, so every `doPoll`
(synchronous or asynchronous) owns its own terminator.

This adapter uses all three: **array mode** is a single completing `dataPoint(node, allRows)`; **split-lines mode**
reports each row as its own value, draining the rows in `dataPoints(node, page)` pages (see the batch size below) then
`pollComplete(node)` (an empty result set is the bare `pollComplete`); a query failure is a `nodeError`.

### Split-lines batch size

The adapter configuration has a `batchSize` field (integer, **1–1000, default 100**) controlling how many result
rows are drained per `dataPoints` call — a cursor page size. **Each row is always its own data point** (its own
value, hence its own MQTT message); `batchSize` never packs rows into one message. It only bounds how much of the
result set is materialized per output call: a batch size of 1 hands the framework one row at a time, a larger size a
page of that many rows at once, so a large result set drains page by page instead of paying one call per row or
materializing the whole set. `dataPoints(node, values)` takes a **list of per-row data points**, so a page is that
list. The field is only meaningful when a tag sets `spiltLinesInIndividualMessages`; array mode ships every row in one
message regardless.

## Behavior parity with the v1 Databases adapter

Preserved verbatim:

- All three engines (`POSTGRESQL`, `MYSQL` via the MariaDB driver, `MSSQL`) and their per-engine Hikari
  configuration.
- The `java.sql.Types` row-to-JSON mapping (integer/long/decimal/double, everything else as a string).
- Both message-shaping modes: all rows as one array message, or `spiltLinesInIndividualMessages` for one message
  per row.
- The connect-time classloader dance and the connection validation after the pool opens.

### Intentional deltas (the framework now owns these)

1. **No auto-removal after repeated poll errors.** v1's `maxPollingErrorsBeforeRemoval` has no v2 equivalent: a poll
   failure is reported as a per-node error, the framework returns the tag to its poll interval and counts the failure
   with escalating log severity, and the adapter is never auto-removed. The config knob is dropped.
2. **The MQTT payload envelope belongs to the framework.** The adapter emits only the row/array JSON as a reused v1
   `DataPoint`; topic, QoS, timestamp, and user-properties are owned by the framework's northbound mappings. The
   on-wire JSON envelope is therefore the framework's, not identical to v1's.
3. **The poll interval is per-tag** (`TagEntity.pollIntervalMillis`), not v1's single adapter-level
   `pollingIntervalMillis`.
4. **The connection lifecycle is explicit.** v1 opened the pool in `start()` and marked the adapter `STATELESS`
   during polls; v2 maps the pool onto the adapter machine — `doConnect` opens and validates it, `doDisconnect`
   closes it — so the pool lives for the `CONNECTED` lifetime with real connection observability.
5. **`port` is a bounded integer** (1–65535) in the adapter-configuration schema, not v1's identifier-regex-on-a-
   string quirk; the v1 `@JsonPropertyOrder({"url","destination"})` copy-paste leftover is dropped.
6. **Verification** uses the template default (always `Success`) — parity with v1, which never verified a query
   ahead of the poll that runs it. Running `SELECT 1` per node at verification time is a noted extension point.

### v1 defects deliberately corrected (flagged for review)

1. **Connection-timeout doubling.** v1 set `connectionTimeout * 2000` milliseconds — twice the documented seconds
   (30 s became 60 000 ms). The port converts with `* 1000`.
2. **`trustCertificate` was dead.** v1's `getTrustCertificate()` returned `encrypt`, and MSSQL hardcoded
   `trustServerCertificate=true` whenever `encrypt` was on. The port wires the setting: TLS on/off is driven by
   `encrypt`, certificate trust by `trustCertificate`.
3. **Statement/ResultSet leak.** v1 closed only the pooled `Connection`; the port holds the connection, prepared
   statement, and result set in one try-with-resources (and closes the validation connection in `doConnect`).

PostgreSQL continues to ignore `encrypt` exactly as v1 did (no `sslmode` is added — out of scope).

## Excluded southbound path

Databases has no write path at all — not even a dormant one in v1 — so `capabilities()` is an empty set and the
adapter never writes, browses, or subscribes. A southbound `WRITE` capability (executing INSERT/UPDATE statements)
remains a documented extension point.

## Testing

- Unit tests drive the adapter directly (`ManualDispatcher` + a recording output) against a stubbed JDBC layer.
- `DatabasesProtocolAdapterWrapperTest` drives a **real `ProtocolAdapterWrapper`** — with the real polled read-aspect
  machines — against **real PostgreSQL, MySQL, and MSSQL Testcontainers**, deterministically (`FakeClock`; the
  synchronous JDBC poll executes inside the manual drain).
- `DatabasesV2AdapterEndToEndTest` (in `hivemq-edge-test`) boots a real Edge runtime that loads this module's fat jar
  through the standard module loader and proves catalog listability, both message-shaping modes over MQTT, the
  flag-only activation reload, and v1/v2 catalog disjointness against all three real engines.
