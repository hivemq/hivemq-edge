# hivemq-edge-module-http-v2

The **HTTP(s)** protocol adapter ported to **SDK v2** — a production, listable, northbound poll-only adapter with
protocol id **`http-v2`**. It runs side by side with the v1 `hivemq-edge-module-http` adapter (protocol id `http`);
the two share no configuration, factory registry, or REST surface and never interact.

`HttpProtocolAdapter` is a real-world SDK-v2 reference for connection-owning, asynchronous, TLS-capable read adapters:
it extends `AbstractProtocolAdapter`, owns the JDK `java.net.http.HttpClient` across `doConnect`/`doDisconnect`
(there is no third-party HTTP dependency), and fires per-tag requests concurrently.

## Behavior parity with the v1 HTTP adapter

Preserved verbatim:

- Per-tag request shaping: GET/POST/PUT, custom headers, per-tag request body and content type, the
  `HiveMQ-Edge; <version>` User-Agent, and redirect `NORMAL`.
- Response handling: success is HTTP 200–299; a `Content-Type` of `application/json` (or `assertResponseIsJson`)
  parses the body to a `JsonNode`, otherwise the body is emitted as a `data:<contentType>;base64,…` string.
- Success-status filtering (`httpPublishSuccessStatusCodeOnly`) and the optional trust-all TLS context
  (`allowUntrustedCertificates`, ported verbatim and gated behind the flag, which defaults to `false`).
- **Per-cycle parallelism (C8).** `doPoll` issues `HttpClient.sendAsync` and returns immediately; the completion
  handler reports the result through the thread-safe `output` façade from the client's completion thread, so a hung
  endpoint (bounded by its per-request timeout) never wedges the dispatch thread.

### Intentional deltas (the framework now owns these)

1. **No auto-removal after repeated poll errors.** v1's `maxPollingErrorsBeforeRemoval` has no v2 equivalent: a poll
   failure (a non-success status under success-only publishing, a timeout, an I/O or parse error) is reported as a
   per-node error and the framework counts it with escalating log severity; the adapter is never auto-removed. The
   config knob is dropped.
2. **The MQTT payload envelope belongs to the framework** — the adapter emits only the decoded response value; topic,
   QoS, timestamp, and user-properties are owned by the framework's northbound mappings.
3. **The poll interval is per-tag** (`TagEntity.pollIntervalMillis`), not a single adapter-level setting.
4. **Non-success responses.** Under `httpPublishSuccessStatusCodeOnly` a non-success status is mapped to a per-node
   error so the tag reflects the failure; with success-only publishing off, the response body is emitted as a data
   point.

### Excluded: the dormant southbound path (extension point)

The v1 module ships a dormant, unused southbound write configuration (`MqttToHttpMapping` / `WritingContext` /
`HttpPayload`). Under strict parity it is **not** carried into v2 — the adapter's `capabilities()` is an empty set
(no write, browse, or subscription). Lighting up southbound writes is a documented future extension point.

The adapter-level configuration carries the four response-handling settings (`httpConnectTimeoutSeconds`,
`allowUntrustedCertificates`, `assertResponseIsJson`, `httpPublishSuccessStatusCodeOnly`).
