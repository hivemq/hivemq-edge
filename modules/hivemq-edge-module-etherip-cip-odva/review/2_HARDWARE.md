# Allen-Bradley CompactLogix 1769-L32E — Known ODVA / EtherNet-IP Problems

> Reference notes for testing ODVA (EtherNet/IP + CIP) drivers against the embedded
> EtherNet/IP port of the CompactLogix 1769-L32E (CompactLogix 5332E).
> Catalog number is **1769-L32E** (not 1796). Early-2000s controller; behavior varies by firmware.

---

## 1. Connection capacity is very small and easily exhausted

The single biggest gotcha for driver testing.

- Embedded port supports a **maximum of 32 CIP connections**:
    - up to **32 bridged** (any mix of implicit + explicit connections)
    - up to **20 end-node** connections
- A produced/implicit connection can be consumed by a **maximum of 32 controllers**.
- Ignore third-party claims of 16 TCP/64 CIP or 128 CIP — the Rockwell figure (32 CIP) is authoritative. Successor 5370 controllers jump to ~120 TCP / 256 CIP.

**Consequences:**
- Exceeding the budget → controller refuses the `Forward_Open`. Classic symptom is CIP general status **0x0204 (connection timeout / connection failure)**, logged at the connection level, not as a controller fault.
- Idle engineering workstations, RSLinx/OPC clients, and HMIs each consume connections, so the real available budget during a test is often well under 32 — audit what is connected before blaming the driver.
- The embedded **web server TCP does not count** toward the connection maximum, so HTTP traffic won't explain a shortfall.

---

## 2. No Large Forward Open — capped at ~500-byte connections

- The embedded port does **not** support Large Forward Open. Connected message payload is limited to the classic **500 bytes**.
- The "Large Connection" feature (up to ~4000 bytes) requires **firmware v20+ AND** a newer rack Ethernet bridge (EN2x / EN3x / EN5.x). The embedded L32E port does not qualify.
- Devices that don't meet the requirements **silently fall back to 500 bytes**.

**Test impact:** If the driver advertises/requests a large connection in the `Forward_Open`, expect rejection or silent down-negotiation. Cover the small-connection path and fragmented transfers for anything > 500 bytes.

---

## 3. No Socket Object / no raw TCP-UDP sockets

- There is **no CIP Socket Object** on this controller.
- It handles EtherNet/IP implicit (Class 1) and explicit messaging, but **cannot open user-defined TCP/UDP sockets**.
- Socket services / instructions (`CreateSocket`, `DeleteAllSockets`, etc.) are **unsupported** — this requires a 5370/5380-class controller.

**Test impact:** Probing the Socket Object class will return service-not-supported.

---

## 4. CIP routing / path quirk — virtual backplane, CPU at slot 0

Older "virtual backplane" addressing that differs from 5370+ and frequently breaks drivers validated only against newer hardware.

- **CPU = virtual backplane slot 0**, Ethernet daughtercard = slot 1.
- Final hop of the CIP path is **`1, 0`**.
- On modern 5370-series controllers the final hop is **`2, <ip address>`** instead.

**Test impact:** Drivers that hardcode or auto-derive the newer port-2 routing will fail to connect to the L32E with a path/connection error.

---

## 5. Firmware anomalies that can masquerade as driver bugs

Documented anomalies in the L32E/L35E firmware line — know these so you don't chase phantom driver defects. (Most corrected in later firmware; pin and record your revision.)

| Anomaly | Effect | Notes |
|---|---|---|
| **IOT data-integrity on CompactBus** | Data integrity anomalies on the 1769 CompactBus, occurring for one RPI when using the `IOT` instruction | Corrected in later firmware |
| **Task-overlap fault not logged** | Minor fault (Type 6 Task Overlap, Code 4) **not logged** when 1769 I/O module RPIs are set incorrectly | Can mask timing problems in stress tests |
| **CompactBus browse hang** | Anomalous state when browsing the CompactBus or through a 1769-SDN; recovery requires a specific reset procedure | — |
| **Memory loss over time (L35E rev 15)** | Available memory degrades over ~a week of uptime | Relevant for long soak tests |
| **Online-edit major fault** | Non-recoverable major fault when doing online edits with a large quantity of HMI tags on scan | — |

- The rev 20.011 / 20.013 line was the last firmware family for these controllers.
- Behavior shifts meaningfully across revisions — **pin your test rig to a known late firmware and record it**.

---

## 6. Other operational kinks

- **BOOTP that won't respond:** Controller sends no BOOTP requests if a static IP is already stored or the port isn't in BOOTP/DHCP mode — no request packets appear at the server regardless of switch or direct connection. Reliable recovery: set IP over the serial (Channel 0) port first.
- **Single 10/100 port, no DLR, no second port:** Flat single-interface device. No device-level ring or redundancy behavior to test.
- **Can't go online over Ethernet despite ping/RSLinx visibility:** Recurring field issue where the device is pingable and visible in Who Active but won't open a programming session — usually a PC NIC/driver state problem, not the controller. Isolate it so it isn't logged against the driver.

---

## Open question for narrowing the test plan

Which side of the ODVA stack is the driver on?

- **Scanner / originator** opening connections *to* the L32E, or
- **Adapter / target** that the L32E originates *to*?

This determines the specific `Forward_Open` parameters, connection types (Class 1 vs Class 3 vs UCMM), and error codes worth building into conformance test cases.

---

## Sources

- Rockwell **ENET-AP001** — EtherNet/IP Performance Application Guide (connection capacities)
- Rockwell **1769-RN019 / 1769-RN021** — CompactLogix Controllers Release Notes, Rev 19 & 20 (anomalies)
- Rockwell **ENET-AT002** — EtherNet/IP Sockets Application Technique
- Kepware *Allen-Bradley ControlLogix Ethernet Driver Help* (connection-size requirements)
- Field reports: PLCtalk.net / PLCS.net forum threads (0x0204, sockets, virtual-backplane path)
