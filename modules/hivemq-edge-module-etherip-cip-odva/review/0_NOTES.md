# EtherNet/IP

The protocol stack

- ODVA is the standards organization (Open DeviceNet Vendors Association) that owns and publishes CIP.
- CIP (Common Industrial Protocol) is the application layer: an object model (everything is a Class / Instance / Attribute) plus a set of services (Get_Attribute_Single, Read Tag, Write Tag, …).
- EtherNet/IP = "EtherNet/Industrial Protocol" = CIP encapsulated over standard Ethernet/TCP/IP (TCP 44818 for explicit messaging, UDP for implicit I/O).

## old ethernet/ip protocola adapter vs new odva one

### Addressing
| Descr           | etherip (old)                                                         | etherip-cip-odva (new)                             |
|-----------------|-----------------------------------------------------------------------|----------------------------------------------------|
| CIP service     | Read Tag (Rockwell symbolic service)                                  | Get_Attribute_Single (generic CIP service)         |
| Addressing      | Symbolic — tag name (MyTag.Member)                                    | Logical — @class/instance/attribute (@4/100/1)     |
| Code            | client.readTags("tagName")                                            | client.getAttributeSingle(logicalAddressPath, …)   |
| Who supports it | Rockwell/Allen-Bradley ControlLogix & CompactLogix (vendor extension) | Any CIP-conformant device (the ODVA-standard way)  |

The key point: symbolic Read-Tag is the Rockwell/Allen-Bradley flavor of CIP — it's a vendor extension that relies on the PLC having a tag database you can address by name. 
ODVA is the generic, standards-compliant path where data lives in the object model. You fetch it by numeric class/instance/attribute.

**We should probably support both schemes!**

### Composite tags
The ODVA based adapter supports reading multiple tags at once.
This works by reading one contiguous block of memory and parsing it on the client side.
The device does not ensure the datatypes, one broken tag breaks the read.

#### Read limitations
- **Batching only within a single address.** Tags are grouped by `@class/instance/attribute`. Each address = one `Get_Attribute_Single` round-trip, issued sequentially in the poll loop. No CIP Multiple Service Packet bundling distinct addresses (README TODO). Poll latency scales linearly with the number of distinct addresses.
- **Offsets are hand-configured and unvalidated.** `batchByteIndex` / `batchBitIndex` are entered manually per tag; the adapter does not parse the device's structure/EDS. Wrong index → silently wrong value. `batchByteIndex` defaults to 0, so multiple tags left at the default all decode the same bytes. No check for overlapping ranges or gaps.
- **One bad tag poisons its group's read.** Bounds are only checked at decode time (`assertAvailableBuffer`); an overrun throws `OdvaDecodeException` which aborts the remaining tags in that address group for that poll. Other groups are unaffected.
- **No fragmentation → hard packet-size ceiling.** Each read is a single un-fragmented `Get_Attribute_Single`, bounded by the device's max response size (~500 bytes). `numberOfElements` allows up to 1500 in config but the adapter does not enforce the device limit — large arrays just fail at the device.
- **Single, adapter-global byte order.** No per-tag endianness.
- **Grouping fixed at start** (`registerTagsIfEmpty`, latched via AtomicBoolean) — changing the tag set requires a restart.
- **Composite is address-scoped** — only aggregates tags at the same address. No root-level composite across addresses (README TODO).

### Writing back

**Status: NOT wired up.** The adapter's `write()` is a stub — it logs the payload and calls `writingOutput.finish()` (reports SUCCESS) but never invokes the encoders and never sends anything. `WriteTest` is `@Disabled("Work in progress")`, `EthernetIPWithODVA.write(...)` / the MultiRequest path are commented out, README "Expose Write capability in Edge" is unchecked. **Today a write silently discards the value while reporting success.** Everything below describes the *intended* design the encoder framework + forked lib already sketch.

#### Mirror of the read path
- `CipTagEncoders.encode` iterates a group's tags, seeks to `startPosition + batchByteIndex`, writes each value into a shared `ByteBuffer` — inverse of `CipTagDecoders.decode`.
- CIP service counterpart: reads use `Get_Attribute_Single (0x0E)`, writes use **`Set_Attribute_Single (0x10)`** (`EthernetIPWithODVA.setAttributeSingle`).
- Same address-grouping: one attribute = one request.

#### Contiguous block write — the model and the hazard
- `CipTagEncodingAttributeProtocol.writeToBuffer` zero-fills the whole block (`clearBuffer(totalRequestSize)`), then writes each tag at its `batchByteIndex`. `getRequestSize = max(batchByteIndex + valueSize)`. Result: one contiguous attribute payload sent as a single `Set_Attribute_Single`.
- **No read-modify-write.** Bytes not covered by a supplied tag are written as 0. Writing one field of a packed attribute without supplying the others **zeroes the rest of the attribute**. To safely update a block you must supply every field.
- **Single address only** — a contiguous block = one attribute. Cross-address writes would be multiple separate `Set_Attribute_Single` calls (MultiRequest unimplemented).
- **API mismatch:** the Edge write entry point delivers one tag at a time (`CipWritePayload` wraps a single `JsonNode`), while the block encoder expects all sibling values together → batch/block write isn't naturally expressible through the current write API without gathering siblings or read-modify-write.
- **BOOL writes (single + array) are unimplemented** (README).

#### Failure semantics (whole vs partial)
- **Encoding (pre-send):** `CipTagEncoders.encode` throws `OdvaEncodeException` on the first bad tag; buffer is built before transmission → nothing sent, clean all-or-nothing.
- **Single block write (one `Set_Attribute_Single`):** one request for one attribute — device commits the whole attribute or returns a CIP error; no partial-byte commit within a single Set_Attribute_Single. Atomic per attribute.
- **Across multiple addresses:** each address is an independent request with no spanning transaction → **partial failure**; already-written attributes stay written, no rollback of earlier groups.
- **Currently:** write reports success while doing nothing (silent data loss).


