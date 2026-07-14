# hivemq-edge-module-file-v2

The **File** protocol adapter ported to **SDK v2** — a production, listable, northbound poll-only adapter with
protocol id **`file-v2`**. It runs side by side with the v1 `hivemq-edge-module-file` adapter (protocol id `file`);
the two share no configuration, factory registry, or REST surface and never interact.

`FileProtocolAdapter` doubles as a compact, real-world SDK-v2 read-only reference (alongside the synthetic
`ChaosProtocolAdapter`): it extends `AbstractProtocolAdapter`, owns no connection (File is stateless), and reads a
file's content on each scheduled poll, decoding it with one of the five content types.

## Behavior parity with the v1 File adapter

Preserved verbatim:

- The five content types (`BINARY`, `TEXT_PLAIN`, `TEXT_JSON`, `TEXT_XML`, `TEXT_CSV`) and their decode rules —
  `BINARY` is Base64-encoded bytes, `TEXT_JSON` is parsed to a `JsonNode`, the rest are UTF-8 strings.
- The **64 KB** file-size cap enforced before the file is read.

### Intentional deltas (the framework now owns these)

1. **No auto-removal after repeated poll errors.** v1's `maxPollingErrorsBeforeRemoval` has no v2 equivalent: a poll
   failure is reported as a per-node error, the framework returns the tag to its poll interval and counts the failure
   with escalating log severity, and the adapter is never auto-removed. The config knob is dropped.
2. **The MQTT payload envelope belongs to the framework.** The adapter emits only the decoded value as a reused v1
   `DataPoint`; topic, QoS, timestamp, and user-properties are owned by the framework's northbound mappings. The
   on-wire JSON envelope is therefore the framework's, not identical to v1's.
3. **The poll interval is per-tag** (`TagEntity.pollIntervalMillis`), not a single adapter-level setting.
4. **Verification** uses the template default (always `Success`) — the File adapter cannot verify a node ahead of
   the poll that reads it.

The File adapter has **no adapter-level configuration**, **no** write, browse, or subscription support (its
`capabilities()` is an empty set), and no southbound path.
