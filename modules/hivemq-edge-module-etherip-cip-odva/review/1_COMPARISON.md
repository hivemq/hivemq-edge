# EtherNet/IP Adapters — Comparison Overview

There are two EtherNet/IP adapter modules under `hivemq-edge/modules`:

- `hivemq-edge-module-etherip`
- `hivemq-edge-module-etherip-cip-odva`

Both talk to EtherNet/IP / CIP devices and both build on the same underlying
open-source library (`etherip-1.0.0.jar` from ornl-epics). But they represent
**two fundamentally different generations / addressing philosophies**.

| | **hivemq-edge-module-etherip** | **hivemq-edge-module-etherip-cip-odva** |
|---|---|---|
| Protocol ID | `eip` (legacy: `ethernet-ip`) | `etheripCipOdva` |
| Display name | "Ethernet IP Protocol Adapter" | "EthernetIP/CIP/ODVA Protocol Adapter" |
| Package | `...adapters.etherip` | `...adapters.etherip_cip_odva` |
| Capabilities | `READ` only | `READ`, `WRITE`, `COMBINE` |
| Maturity | Stable, simple, production | Newer, feature-rich, parts still WIP (per README TODOs) |

---

## 1. Addressing model — the core difference

- **etherip**: Uses **symbolic tag addressing** — the `address` is the named tag
  in the PLC (e.g. a ControlLogix tag name). It calls `client.readTags(tagAddresses)`
  and relies on the device's tag database. Targeted at Rockwell / Allen-Bradley
  ControlLogix & CompactLogix.

- **etherip-cip-odva**: Uses **ODVA logical addressing** via *Get Attribute Single*.
  Addresses follow the pattern `@{class}/{instance}/{attribute}`
  (regex-enforced: `@[0-9]+/[0-9]+/[0-9]+`, e.g. `@4/100/1`). It builds a CIP
  `LogicalAddressPath` and calls `client.getAttributeSingle(...)`. This is
  vendor-neutral — any PLC supporting the ODVA standard, not just Rockwell.

---

## 2. The forked library

- **etherip** uses the `etherip-1.0.0.jar` binary as-is (`etherip.EtherNetIP`).
- **etherip-cip-odva** ships its own **extended/forked classes in the `etherip`
  package** inside its own source tree, layered on top of the jar:
  - `EthernetIPWithODVA` (subclass adding `getAttributeSingle`)
  - `types/LogicalAddressPath`, `CNClassPath`, `CNService`
  - `protocol/MRChipWriteAnyProtocol`, `BaseDecodingAttributeProtocol`,
    `BaseEncodingAttributeProtocol`

  This is how it adds ODVA logical-addressing and write support the original
  library lacks.

---

## 3. Data types & decoding architecture

- **etherip**: A flat `EipDataType` enum (BOOL, DINT, INT, LINT, LREAL, LTIME,
  REAL, SINT, STRING, TIME, UDINT, UINT, ULINT, USINT). Conversion is handled by
  a small `model/` package (`EtherIpValueFactory` + a handful of `EtherIp*` value
  types). Type resolution is essentially driven by what the library returns.

- **etherip-cip-odva**: A `CipDataType` enum plus a full **pluggable
  decoder/encoder framework**:
  - `decoder/` — one class per type (`BOOLDecoder`, `DINTDecoder`, `LREALDecoder`,
    `SSTRINGDecoder`, `ListOfDecoder`, …) behind a `CipTagDecoder` interface
    registered in `CipTagDecoders`.
  - `encoder/` — symmetric set for writing (`BOOLEncoder`, `STRINGEncoder`, …).
  - Adds **SSTRING vs STRING** distinction and a special **`COMPOSITE`** artificial type.
  - Configurable **byte order** (`BIG_ENDIAN`/`LITTLE_ENDIAN`) — etherip has no
    byte-order option.

---

## 4. Tag configuration richness

- **etherip** `EipTagDefinition`: just `address` + `dataType`.

- **etherip-cip-odva** `CipTagDefinition`: much richer — `address`,
  `numberOfElements` (array reads, 1–1500), `dataType`, `hysteresis` (dead-band),
  `minUpdateIntervalMs` (scheduled re-publish), `batchByteIndex`, `batchBitIndex`
  (decode multiple tags / individual bits out of a single read). Supports
  **batch reading** (decode many tags from one read) and **array reads** including
  BOOL/flag arrays.

---

## 5. Feature set (ODVA-only capabilities)

The ODVA module adds substantial machinery absent from etherip:

- **Write / southbound support** (`WritingProtocolAdapter`, `CipWritePayload`,
  encoders, `MRChipWriteAnyProtocol`) — though `write()` is still stubbed
  ("write to happen here") and the README marks "Expose Write capability in Edge"
  as not-done.
- **COMPOSITE tags** — aggregates all tags at one address into a single JSON
  object (`composite/` package: `CompositeValues`, `DefaultCompositeValues`,
  `NoopCompositeValues`).
- **Hysteresis** package — per-Java-type dead-band checkers
  (`Double/Float/Integer/Long/Short/Byte/List/EqualsHysteresisChecker`).
- **Schema generation** — `createTagSchema` builds JSON schemas (scalar +
  composite); `TagSchemaMapper`, `TagGroups`/`TagGroup`.
- **Dedicated exception hierarchy** — `OdvaException`, `OdvaDecodeException`,
  `OdvaEncodeException`, `ExceptionProcessor`, `ExceptionUtils`.
- **Stats/diagnostics** — `StatsTracker` (with Guava `Stopwatch`), `DataPointStore`,
  JUL→SLF4J bridge (`JULtoSLF4JEnabler`).

---

## 6. Connection lifecycle / robustness

- **etherip**: Simple. `start()` connects once; if it fails, the adapter fails to
  start. `poll()` fails if the client is null — **no in-poll reconnection**. Uses a
  `volatile EtherNetIP` field and a basic `PublishChangedDataOnlyHandler`.

- **etherip-cip-odva**: Far more resilient. `start()` **succeeds even if the
  connection fails** (validates tags, then retries later). `poll()` **detects
  disconnection and reconnects in-line**, inspects exception messages against known
  `DISCONNECT_REASONS` ("Connection reset by peer", "Broken pipe", "Timeout",
  `UnRegisterSession`) to decide whether to drop/reconnect, checks runtime status
  mid-poll, and uses `AtomicReference` for thread-safe client/sample swapping.
  Polls per **TagGroup** rather than one flat list.

---

## 7. Dependencies

- **etherip-cip-odva** additionally pulls in **Guava** (`33.4.0-jre`) and
  **jul-to-slf4j**; etherip needs neither.
- etherip-cip-odva's build also skips the shadow runtime variant
  (`withVariantsFromConfiguration ... skip()`).

---

## Summary

`hivemq-edge-module-etherip` is the **original, lean, read-only symbolic-tag
adapter** specifically for Rockwell/Allen-Bradley PLCs.

`hivemq-edge-module-etherip-cip-odva` is a **much larger, vendor-neutral rewrite**
built around ODVA logical addressing (`@class/instance/attribute`), with its own
extended copy of the etherip library, a pluggable decoder/encoder framework,
batch/array reads, hysteresis, scheduled updates, composite tags, JSON schema
generation, robust reconnection, and the scaffolding for write (southbound)
support — some of which is still in progress per its README TODO list.
