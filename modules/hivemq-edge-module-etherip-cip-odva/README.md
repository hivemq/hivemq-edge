# EthernetIP CIP - ODVA flavor

For interacting with Ethernet IP/CIP devices we rely on an open source library which isn't published on Maven-Central:

[EtherIP](https://github.com/ornl-epics/etherip/)

We build the library internal and provide the binary in **libs/etherip-1.0.0.jar**.

In case you want to build the libary yourself simply check out the original project, build the libary and put it into 
the libs folder.

# TL;DR

Allows reading tags using ODVA standard (get attribute single) with logical adressing. _Tag Address_ in the format: "@CC/II/AA", where

* CC - class (numerical value)
* II - instance (numerical value)
* AA - attribute (numerical value)

Supports:
* multiple CIP data types
* reading arrays of data (also BOOL/FLAG)
* batch reading - decoding multiple tags from a single read operation
* hysteresis (dead band) for each tag (setting "0" - publishes every change)
* scheduled updates (useful when used with hysteresis) - publish update at least every X milliseconds

Includes a "COMPOSITE" data type to allow for publishing all tags read at a given address as a single composite object, ie:

```json
{
    "timestamp" : 1756462706440,
    "value" : {
        "flag0" : false,
        "flag1" : true,
        "flags-array" : [ true, true, true, false, false, false, false, false, false, true, false, false, false, false, false, false ],
        "int" : 519,
        "usint array from batch" : [ 10, 7, 2, 65, 66, 67, 69, 1, 0, 0, 0, 255, 255, 255, 255, 6, 66, 246, 230, 102, 5, 65, 66, 67, 68, 69, 0, 0, 0, 0 ]
    },
    "tagName" : "composite"
}
```

# Dependencies
Uses [EtherIP](https://github.com/ornl-epics/etherip/) library and extends it

# Todo

- [x] Hysteresis support + time based trigger (republish if data older than)
  - Per Tag max interval configuration?

- [ ] Add write capability
    - [x] Encoders to use superType Number and Double as param
    - [x] requestSize() requires value - necessary for strings
    - [x] Write Simple (single value, no batch)
    - [x] Write a batched value
      - [x] Writing of multiple tags (underlying byte[] is available, just needs clear() operation)
    - [x] Test write with logical addressing
    - [x] Implement Encoders
      - [ ] BOOLEAN - write single + write multiple (arrays)
  - [ ] Expose Write capability in Edge
  - [ ] Add TagDefinition "direction" (WriteOnly?) - to avoid unnecessary reads
  - [ ] Process incoming payloads (json?)

- [ ] Add support for read_fragmented - reading larger amount data than supported by PLC (ie. >500 bytes) using multiple partial requests using offset and count

- [ ] Run simple requests as MultiRequest?
  - [ ] Consider max request size + expected response size
    - [ ] Require adding size/max size to TagDefinition for SSTRING/STRING for response size assessment

- [ ] Add support for root level COMPOSITE - aggregate data from all tags

- [ ] Add support for read_tag (configured on tag definition level) to support both: read tag + get attribute single
  - [ ] Support symbolic addressing
  - [ ] Support logical addressing (same as used in get_attribute_single)

- [x] Apply hysteresis to both flags (boolean) and strings to allow for forced value updates when hysteresis=0 - consistent behaviour with other data types

- [x] Add a "COMPOSITE" cip data type, that will output all tags at given address with a format {tagname}: {tagvalue}
  - [x] Respects publishing only changed values, composite object will contain ONLY changed ones

- [x] Create ClassInstanceAttribute only once

- [x] CipTag/List<CipTag> based DelegatingAttributeProtocol
  - uses passed tags and ByteBuffer to decode values (based on cip tag) and sends them to a CipTagValueReceiver
  - decoding done using: CipTagDecoder
    - receives buf, delegates to specific decoder based on cipdatatype

- [ ] Implement ULINT (as BigDecimal?)

- [x] Better exception information
  - [x] Decode exceptions
  - [x] Include tag information
  
- [x] List decoding of booleans

- [x] Check UI for null values for batchByteIndex, batchBitIndex. When config reloaded UI reports errors for nullable values

- [x] Implement hysteresis after decoders based on Java Type
  - [x] remove getClazz()

- [x] Reconnects
  - [x] Constructor only simple, fast operations
  - [x] Start should not fail if not able to connect
  - [x] Reconnects to be performed in poll() method when connection related error is detected
  - [x] When connection error detected during poll() trigger reconnect

- [x] Adapter gets stopped/removed after maxErrorLevel is reached
  - Workaround:
    - [x] Set limit at VERY HIGH value (so it's never reached, or -1)
    - [ ] Set internal max backoff time to some reasonable value: currently set statically to 10 minutes
      - InternalConfigurations.ADAPTER_RUNTIME_MAX_APPLICATION_ERROR_BACKOFF.get()

- [x] BOOL Attribute protocol single (checks whole value) & batched (looks at given bit) (For get attribute single - True is represented by SINT 0xFF = -1)

- [x] Delegating AttributeProtocol (delegate value consumer, not require new instance creation)
- [x] Array reads as repeated invocation of AttributeProtocol

- [x] Exception handling

- [x] Add ByteOrder
