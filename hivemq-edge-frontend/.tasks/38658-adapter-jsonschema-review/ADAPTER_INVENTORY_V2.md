# Protocol Adapter Inventory (Backend & Frontend)

**Task:** 38658 - Adapter JSON Schema Review  
**Date:** December 16, 2025  
**Version:** 2.1

---

## Source Code Context

Inventory based on the following repository states:

| Repository                    | Branch | Commit                                     | Date       |
| ----------------------------- | ------ | ------------------------------------------ | ---------- |
| `hivemq-edge`                 | master | `eabcb94278d8e5b66a2daea7b491a3ea76751d99` | 2025-12-09 |
| `hivemq-edge-module-bacnetip` | master | `b3458e0d5eee7fab3dddbc6cc1730e8727eb3027` | 2025-11-18 |

**Note:** Re-run inventory if commits have changed significantly.

---

## Overview

This document provides a comprehensive inventory of all protocol adapters, showing both backend definitions and frontend mock coverage.

---

## Complete Adapter List

### Backend Modules Discovered

| ID           | Module Location                                   | Status   |
| ------------ | ------------------------------------------------- | -------- |
| `ads`        | `modules/hivemq-edge-module-plc4x`                | ✅ Found |
| `databases`  | `modules/hivemq-edge-module-databases`            | ✅ Found |
| `eip`        | `modules/hivemq-edge-module-etherip`              | ✅ Found |
| `file`       | `modules/hivemq-edge-module-file`                 | ✅ Found |
| `http`       | `modules/hivemq-edge-module-http`                 | ✅ Found |
| `modbus`     | `modules/hivemq-edge-module-modbus`               | ✅ Found |
| `mtconnect`  | `modules/hivemq-edge-module-mtconnect`            | ✅ Found |
| `opcua`      | `modules/hivemq-edge-module-opcua`                | ✅ Found |
| `s7`         | `modules/hivemq-edge-module-plc4x`                | ✅ Found |
| `simulation` | `hivemq-edge/src/.../modules/adapters/simulation` | ✅ Found |
| `bacnetip`   | `hivemq-edge-module-bacnetip` (External repo)     | ✅ Found |

---

## Detailed Adapter Information

### 1. ADS Adapter

| Property         | Backend                      | Frontend Mock                    |
| ---------------- | ---------------------------- | -------------------------------- |
| **ID**           | `ads`                        | `ads`                            |
| **Protocol**     | ADS                          | ADS                              |
| **Name**         | ADS Protocol Adapter         | ADS Protocol Adapter             |
| **Module**       | `hivemq-edge-module-plc4x`   | `__test-utils__/adapters/ads.ts` |
| **Category**     | INDUSTRIAL                   | Industrial                       |
| **Capabilities** | READ                         | READ                             |
| **UI Schema**    | `ads-adapter-ui-schema.json` | Defined in mock                  |

#### Backend Files

- Config: `modules/hivemq-edge-module-plc4x/src/main/java/com/hivemq/edge/adapters/plc4x/types/ads/config/`
- UI Schema: `modules/hivemq-edge-module-plc4x/src/main/resources/ads-adapter-ui-schema.json`

#### Frontend Files

- Protocol Mock: `src/__test-utils__/adapters/ads.ts`
- Exports: `MOCK_PROTOCOL_ADS`, `MOCK_ADAPTER_ADS`, `MOCK_SCHEMA_ADS`

#### Mock Completeness

| Component        | Status |
| ---------------- | ------ |
| Protocol         | ✅     |
| Adapter Instance | ✅     |
| Config Schema    | ✅     |
| UI Schema        | ✅     |
| Tag Schema       | ✅     |

---

### 2. Databases Adapter

| Property         | Backend                            | Frontend Mock                          |
| ---------------- | ---------------------------------- | -------------------------------------- |
| **ID**           | `databases`                        | `databases`                            |
| **Protocol**     | Databases                          | Databases                              |
| **Name**         | Databases Protocol Adapter         | Databases Protocol Adapter             |
| **Module**       | `hivemq-edge-module-databases`     | `__test-utils__/adapters/databases.ts` |
| **Category**     | CONNECTIVITY                       | Connectivity                           |
| **Capabilities** | READ                               | READ                                   |
| **UI Schema**    | `databases-adapter-ui-schema.json` | Defined in mock                        |

#### Backend Files

- Config: `modules/hivemq-edge-module-databases/src/main/java/com/hivemq/edge/adapters/databases/config/DatabasesAdapterConfig.java`
- UI Schema: `modules/hivemq-edge-module-databases/src/main/resources/databases-adapter-ui-schema.json`

#### Frontend Files

- Protocol Mock: `src/__test-utils__/adapters/databases.ts`
- Exports: `MOCK_PROTOCOL_DATABASES`

#### Mock Completeness

| Component        | Status     |
| ---------------- | ---------- |
| Protocol         | ✅         |
| Adapter Instance | ❌ Missing |
| Config Schema    | ✅         |
| UI Schema        | ✅         |
| Tag Schema       | ❌ Missing |

#### Known Issues

- **Backend Bug:** `port` field has invalid `stringMinLength`, `stringMaxLength`, `stringPattern` on Integer type

---

### 3. EIP (Ethernet/IP) Adapter

| Property         | Backend                      | Frontend Mock                    |
| ---------------- | ---------------------------- | -------------------------------- |
| **ID**           | `eip`                        | `eip`                            |
| **Protocol**     | Ethernet/IP CIP              | Ethernet/IP CIP                  |
| **Name**         | Ethernet IP Protocol Adapter | Ethernet IP Protocol Adapter     |
| **Module**       | `hivemq-edge-module-etherip` | `__test-utils__/adapters/eip.ts` |
| **Category**     | INDUSTRIAL                   | Industrial                       |
| **Capabilities** | READ                         | READ                             |
| **UI Schema**    | `eip-adapter-ui-schema.json` | Defined in mock                  |

#### Backend Files

- Config: `modules/hivemq-edge-module-etherip/src/main/java/com/hivemq/edge/adapters/etherip/config/EipSpecificAdapterConfig.java`
- UI Schema: `modules/hivemq-edge-module-etherip/src/main/resources/eip-adapter-ui-schema.json`

#### Frontend Files

- Protocol Mock: `src/__test-utils__/adapters/eip.ts`
- Exports: `MOCK_PROTOCOL_EIP`, `MOCK_ADAPTER_EIP`, `MOCK_SCHEMA_EIP`

#### Mock Completeness

| Component        | Status |
| ---------------- | ------ |
| Protocol         | ✅     |
| Adapter Instance | ✅     |
| Config Schema    | ✅     |
| UI Schema        | ✅     |
| Tag Schema       | ✅     |

---

### 4. File Adapter

| Property         | Backend                       | Frontend Mock                     |
| ---------------- | ----------------------------- | --------------------------------- |
| **ID**           | `file`                        | `file`                            |
| **Protocol**     | File Protocol                 | File Protocol                     |
| **Name**         | File Adapter                  | File Adapter                      |
| **Module**       | `hivemq-edge-module-file`     | `__test-utils__/adapters/file.ts` |
| **Category**     | CONNECTIVITY                  | Connectivity                      |
| **Capabilities** | READ                          | READ                              |
| **UI Schema**    | `file-adapter-ui-schema.json` | Defined in mock                   |

#### Backend Files

- Config: `modules/hivemq-edge-module-file/src/main/java/com/hivemq/edge/adapters/file/config/FileSpecificAdapterConfig.java`
- Tag Definition: `modules/hivemq-edge-module-file/src/main/java/com/hivemq/edge/adapters/file/tag/FileTagDefinition.java`
- UI Schema: `modules/hivemq-edge-module-file/src/main/resources/file-adapter-ui-schema.json`

#### Frontend Files

- Protocol Mock: `src/__test-utils__/adapters/file.ts`
- Exports: `MOCK_PROTOCOL_FILE`, `MOCK_ADAPTER_FILE`, `MOCK_SCHEMA_FILE`

#### Mock Completeness

| Component        | Status                          |
| ---------------- | ------------------------------- |
| Protocol         | ✅                              |
| Adapter Instance | ✅                              |
| Config Schema    | ✅                              |
| UI Schema        | ✅                              |
| Tag Schema       | ❌ **WRONG** - Uses HTTP schema |

#### Known Issues

- **Critical:** `MOCK_SCHEMA_FILE` has HTTP adapter fields instead of File fields
- **Critical:** `protocolId: 'http'` should be `'file'`
- Backend has: `filePath`, `contentType`
- Frontend has: `httpHeaders`, `httpRequestBody`, `url` (WRONG)

---

### 5. HTTP Adapter

| Property         | Backend                          | Frontend Mock                     |
| ---------------- | -------------------------------- | --------------------------------- |
| **ID**           | `http`                           | `http`                            |
| **Protocol**     | HTTP(s) over TCP                 | HTTP(s) over TCP                  |
| **Name**         | HTTP(s) to MQTT Protocol Adapter | HTTP(s) to MQTT Protocol Adapter  |
| **Module**       | `hivemq-edge-module-http`        | `__test-utils__/adapters/http.ts` |
| **Category**     | CONNECTIVITY                     | Connectivity                      |
| **Capabilities** | READ                             | READ                              |
| **UI Schema**    | `http-adapter-ui-schema.json`    | Defined in mock                   |

#### Backend Files

- Config: `modules/hivemq-edge-module-http/src/main/java/com/hivemq/edge/adapters/http/config/HttpSpecificAdapterConfig.java`
- UI Schema: `modules/hivemq-edge-module-http/src/main/resources/http-adapter-ui-schema.json`

#### Frontend Files

- Protocol Mock: `src/__test-utils__/adapters/http.ts`
- Exports: `MOCK_PROTOCOL_HTTP`, `MOCK_ADAPTER_HTTP`, `MOCK_SCHEMA_HTTP`

#### Mock Completeness

| Component        | Status |
| ---------------- | ------ |
| Protocol         | ✅     |
| Adapter Instance | ✅     |
| Config Schema    | ✅     |
| UI Schema        | ✅     |
| Tag Schema       | ✅     |

---

### 6. Modbus Adapter

| Property         | Backend                         | Frontend Mock                       |
| ---------------- | ------------------------------- | ----------------------------------- |
| **ID**           | `modbus`                        | `modbus`                            |
| **Protocol**     | Modbus TCP                      | Modbus TCP                          |
| **Name**         | Modbus Protocol Adapter         | Modbus Protocol Adapter             |
| **Module**       | `hivemq-edge-module-modbus`     | `__test-utils__/adapters/modbus.ts` |
| **Category**     | INDUSTRIAL                      | Industrial                          |
| **Capabilities** | DISCOVER, READ                  | DISCOVER, READ                      |
| **UI Schema**    | `modbus-adapter-ui-schema.json` | Defined in mock                     |

#### Backend Files

- Config: `modules/hivemq-edge-module-modbus/src/main/java/com/hivemq/edge/adapters/modbus/config/ModbusSpecificAdapterConfig.java`
- UI Schema: `modules/hivemq-edge-module-modbus/src/main/resources/modbus-adapter-ui-schema.json`

#### Frontend Files

- Protocol Mock: `src/__test-utils__/adapters/modbus.ts`
- Exports: `MOCK_PROTOCOL_MODBUS`, `MOCK_ADAPTER_MODBUS`, `MOCK_SCHEMA_MODBUS`

#### Mock Completeness

| Component        | Status      |
| ---------------- | ----------- |
| Protocol         | ✅          |
| Adapter Instance | ✅          |
| Config Schema    | ✅          |
| UI Schema        | ⚠️ Mismatch |
| Tag Schema       | ✅          |

#### Known Issues

- **UI Schema Mismatch:** Backend has `id: { "ui:disabled": true }`, frontend has `'ui:disabled': false`

---

### 7. MTConnect Adapter

| Property         | Backend                            | Frontend Mock                          |
| ---------------- | ---------------------------------- | -------------------------------------- |
| **ID**           | `mtconnect`                        | `mtconnect`                            |
| **Protocol**     | MTConnect                          | MTConnect                              |
| **Name**         | MTConnect Protocol Adapter         | MTConnect Protocol Adapter             |
| **Module**       | `hivemq-edge-module-mtconnect`     | `__test-utils__/adapters/mtconnect.ts` |
| **Category**     | INDUSTRIAL                         | Industrial                             |
| **Capabilities** | READ                               | READ                                   |
| **UI Schema**    | `mtconnect-adapter-ui-schema.json` | Defined in mock                        |

#### Backend Files

- Config: `modules/hivemq-edge-module-mtconnect/src/main/java/com/hivemq/edge/adapters/mtconnect/config/MtConnectAdapterConfig.java`
- UI Schema: `modules/hivemq-edge-module-mtconnect/build/resources/main/mtconnect-adapter-ui-schema.json`

#### Frontend Files

- Protocol Mock: `src/__test-utils__/adapters/mtconnect.ts`
- Exports: `MOCK_PROTOCOL_MTCONNECT`

#### Mock Completeness

| Component        | Status                         |
| ---------------- | ------------------------------ |
| Protocol         | ✅                             |
| Adapter Instance | ❌ Missing                     |
| Config Schema    | ✅                             |
| UI Schema        | ✅ (Minimal - matches backend) |
| Tag Schema       | ❌ Missing                     |

#### Notes

- Backend UI schema is intentionally minimal (only `id` in coreFields tab)
- Frontend mock correctly reflects this minimal schema

---

### 8. OPC-UA Adapter

| Property         | Backend                        | Frontend Mock                       |
| ---------------- | ------------------------------ | ----------------------------------- |
| **ID**           | `opcua`                        | `opcua`                             |
| **Protocol**     | OPC UA                         | OPC UA                              |
| **Name**         | OPC UA Protocol Adapter        | OPC UA Protocol Adapter             |
| **Module**       | `hivemq-edge-module-opcua`     | `__test-utils__/adapters/opc-ua.ts` |
| **Category**     | INDUSTRIAL                     | Industrial                          |
| **Capabilities** | READ, WRITE, DISCOVER, COMBINE | DISCOVER, WRITE, READ, COMBINE      |
| **UI Schema**    | `opcua-adapter-ui-schema.json` | Defined in mock                     |

#### Backend Files

- Config: `modules/hivemq-edge-module-opcua/src/main/java/com/hivemq/edge/adapters/opcua/config/`
- Information: `modules/hivemq-edge-module-opcua/src/main/java/com/hivemq/edge/adapters/opcua/OpcUaProtocolAdapterInformation.java`

#### Frontend Files

- Protocol Mock: `src/__test-utils__/adapters/opc-ua.ts`
- Exports: `MOCK_PROTOCOL_OPC_UA`, `MOCK_ADAPTER_OPC_UA`, `MOCK_SCHEMA_OPC_UA`
- Legacy Mock: `src/api/hooks/useProtocolAdapters/__handlers__/index.ts` (different structure)

#### Mock Completeness

| Component        | Status |
| ---------------- | ------ |
| Protocol         | ✅     |
| Adapter Instance | ✅     |
| Config Schema    | ✅     |
| UI Schema        | ✅     |
| Tag Schema       | ✅     |

---

### 9. S7 Adapter

| Property         | Backend                     | Frontend Mock                   |
| ---------------- | --------------------------- | ------------------------------- |
| **ID**           | `s7`                        | `s7`                            |
| **Protocol**     | S7                          | S7                              |
| **Name**         | S7 Protocol Adapter         | S7 Protocol Adapter             |
| **Module**       | `hivemq-edge-module-plc4x`  | `__test-utils__/adapters/s7.ts` |
| **Category**     | INDUSTRIAL                  | Industrial                      |
| **Capabilities** | READ                        | READ                            |
| **UI Schema**    | `s7-adapter-ui-schema.json` | Defined in mock                 |

#### Backend Files

- Config: `modules/hivemq-edge-module-plc4x/src/main/java/com/hivemq/edge/adapters/plc4x/types/siemens/config/S7SpecificAdapterConfig.java`
- UI Schema: `modules/hivemq-edge-module-plc4x/src/main/resources/s7-adapter-ui-schema.json`

#### Frontend Files

- Protocol Mock: `src/__test-utils__/adapters/s7.ts`
- Exports: `MOCK_PROTOCOL_S7`, `MOCK_ADAPTER_S7`, `MOCK_SCHEMA_S7`

#### Mock Completeness

| Component        | Status |
| ---------------- | ------ |
| Protocol         | ✅     |
| Adapter Instance | ✅     |
| Config Schema    | ✅     |
| UI Schema        | ✅     |
| Tag Schema       | ✅     |

---

### 10. Simulation Adapter

| Property         | Backend                     | Frontend Mock                           |
| ---------------- | --------------------------- | --------------------------------------- |
| **ID**           | `simulation`                | `simulation`                            |
| **Protocol**     | Simulation                  | Simulation                              |
| **Name**         | Simulated Edge Device       | Simulated Edge Device                   |
| **Module**       | `hivemq-edge` (core)        | `__test-utils__/adapters/simulation.ts` |
| **Category**     | SIMULATION                  | Simulation                              |
| **Capabilities** | READ                        | READ                                    |
| **UI Schema**    | N/A (embedded or generated) | Defined in mock                         |

#### Backend Files

- Location: `hivemq-edge/src/main/java/com/hivemq/edge/modules/adapters/simulation/`
- Config: `config/SimulationSpecificAdapterConfig.java`
- Adapter: `SimulationProtocolAdapter.java`

#### Frontend Files

- Protocol Mock: `src/__test-utils__/adapters/simulation.ts`
- Exports: `MOCK_PROTOCOL_SIMULATION`, `MOCK_ADAPTER_SIMULATION`, `MOCK_SCHEMA_SIMULATION`
- Legacy Mock: `src/api/hooks/useProtocolAdapters/__handlers__/index.ts`

#### Mock Completeness

| Component        | Status |
| ---------------- | ------ |
| Protocol         | ✅     |
| Adapter Instance | ✅     |
| Config Schema    | ✅     |
| UI Schema        | ✅     |
| Tag Schema       | ✅     |

---

### 11. BACnet/IP Adapter

| Property         | Backend                                      | Frontend Mock                         |
| ---------------- | -------------------------------------------- | ------------------------------------- |
| **ID**           | `bacnetip`                                   | `bacnetip`                            |
| **Protocol**     | BACnet/IP                                    | bacnetip                              |
| **Name**         | BACnet/IP Protocol Adapter                   | BACnet/IP Protocol Adapter            |
| **Module**       | `hivemq-edge-module-bacnetip` (**External**) | `__test-utils__/adapters/bacnetip.ts` |
| **Category**     | BUILDING_AUTOMATION                          | N/A (not in mock)                     |
| **Capabilities** | (not specified)                              | [] (empty)                            |
| **UI Schema**    | `bacnet-adapter-ui-schema.json`              | N/A (not in mock)                     |

#### Backend Files (External Repository)

- **Repository:** `/Users/nicolas/IdeaProjects/hivemq-edge-module-bacnetip/`
- **Adapter Info:** `src/main/java/com/hivemq/edge/adapters/bacnetip/BacnetipProtocolAdapterInformation.java`
- **Config:** `src/main/java/com/hivemq/edge/adapters/bacnetip/config/BacnetipSpecificAdapterConfig.java`
- **Tag Definition:** `src/main/java/com/hivemq/edge/adapters/bacnetip/config/tag/BacnetTagDefinition.java`
- **UI Schema:** `src/main/resources/bacnet-adapter-ui-schema.json`

#### Config Fields (Backend)

| Field                   | Type    | Required | Default |
| ----------------------- | ------- | -------- | ------- |
| id                      | string  | ✅       | -       |
| host                    | string  | ✅       | -       |
| port                    | integer | ✅       | 47808   |
| deviceId                | integer | ✅       | -       |
| subnetBroadcastAddress  | string  | ✅       | -       |
| discoveryIntervalMillis | integer | ✅       | 5000    |
| bacnetipToMqtt          | object  | ✅       | -       |

#### Tag Definition Fields (Backend)

| Field                | Type    | Required |
| -------------------- | ------- | -------- |
| deviceInstanceNumber | integer | ✅       |
| objectInstanceNumber | integer | ✅       |
| objectType           | enum    | ✅       |
| propertyType         | enum    | ✅       |

#### Frontend Files

- Protocol Mock: `src/__test-utils__/adapters/bacnetip.ts`
- Exports: `MOCK_PROTOCOL_BACNET_IP` (minimal, metadata only)

#### Mock Completeness

| Component        | Status                     |
| ---------------- | -------------------------- |
| Protocol         | ⚠️ Minimal (metadata only) |
| Adapter Instance | ❌ Missing                 |
| Config Schema    | ❌ Missing                 |
| UI Schema        | ❌ Missing                 |
| Tag Schema       | ❌ Missing                 |

#### Notes

- Backend adapter is in a **separate external repository** (not in main hivemq-edge repo)
- External module structure ✅ matches internal module patterns
- Frontend mock needs update with full schema from backend
- **Issue:** `bacnetipToMqtt` description has copy-paste error ("from ADS to MQTT")

---

## Summary Tables

### Mock Completeness Overview

| Adapter    | Protocol | Instance | Config | UI Schema | Tag Schema | Overall            |
| ---------- | -------- | -------- | ------ | --------- | ---------- | ------------------ |
| ADS        | ✅       | ✅       | ✅     | ✅        | ✅         | ✅ Complete        |
| Databases  | ✅       | ❌       | ✅     | ✅        | ❌         | ⚠️ Partial         |
| EIP        | ✅       | ✅       | ✅     | ✅        | ✅         | ✅ Complete        |
| File       | ✅       | ✅       | ✅     | ✅        | ❌         | 🔴 **Wrong Tag**   |
| HTTP       | ✅       | ✅       | ✅     | ✅        | ✅         | ✅ Complete        |
| Modbus     | ✅       | ✅       | ✅     | ⚠️        | ✅         | ⚠️ UI Mismatch     |
| MTConnect  | ✅       | ❌       | ✅     | ✅        | ❌         | ⚠️ Partial         |
| OPC-UA     | ✅       | ✅       | ✅     | ✅        | ✅         | ✅ Complete        |
| S7         | ✅       | ✅       | ✅     | ✅        | ✅         | ✅ Complete        |
| Simulation | ✅       | ✅       | ✅     | ✅        | ✅         | ✅ Complete        |
| BACnet/IP  | ⚠️       | ❌       | ❌     | ❌        | ❌         | ⚠️ Incomplete Mock |

### Issues Summary

| Issue                   | Adapter              | Severity    | Description                                      |
| ----------------------- | -------------------- | ----------- | ------------------------------------------------ |
| Wrong Tag Schema        | File                 | 🔴 Critical | Uses HTTP schema instead of File schema          |
| Backend Bug             | Databases            | 🔴 High     | Invalid string constraints on integer port field |
| UI Schema Mismatch      | Modbus               | 🟡 Medium   | `id.ui:disabled` differs from backend            |
| Missing Mock Components | Databases, MTConnect | 🟡 Medium   | Missing adapter instance and tag schema          |
| Not Installed           | BACnet/IP            | 🟡 Medium   | Backend exists but frontend mock incomplete      |

---

## MockAdapterType Enum Status

Current enum in `src/__test-utils__/adapters/types.ts`:

```typescript
export enum MockAdapterType {
  BACNET = 'bacnetip', // ✅
  S7 = 's7', // ✅
  MODBUS = 'modbus', // ✅
  FILE = 'file', // ✅
  HTTP = 'http', // ✅
  SIMULATION = 'simulation', // ✅
  EIP = 'eip', // ✅
  OPC_UA = 'opcua', // ✅
  ADS = 'ads', // ✅
  // Missing:
  // DATABASES = 'databases'
  // MTCONNECT = 'mtconnect'
}
```

---

## File Locations Reference

### Backend

```
/Users/nicolas/IdeaProjects/hivemq-edge/
├── hivemq-edge/
│   └── src/main/java/com/hivemq/edge/
│       ├── modules/adapters/
│       │   └── simulation/                     # Simulation adapter
│       └── adapters/
│           └── bacnetip/                       # BACnet/IP adapter
│
└── modules/
    ├── hivemq-edge-module-databases/
    │   └── src/main/java/.../databases/        # Databases adapter
    ├── hivemq-edge-module-etherip/
    │   └── src/main/java/.../etherip/          # EIP adapter
    ├── hivemq-edge-module-file/
    │   └── src/main/java/.../file/             # File adapter
    ├── hivemq-edge-module-http/
    │   └── src/main/java/.../http/             # HTTP adapter
    ├── hivemq-edge-module-modbus/
    │   └── src/main/java/.../modbus/           # Modbus adapter
    ├── hivemq-edge-module-mtconnect/
    │   └── src/main/java/.../mtconnect/        # MTConnect adapter
    ├── hivemq-edge-module-opcua/
    │   └── src/main/java/.../opcua/            # OPC-UA adapter
    └── hivemq-edge-module-plc4x/
        └── src/main/java/.../plc4x/types/
            ├── ads/                            # ADS adapter
            └── siemens/                        # S7 adapter
```

### Frontend

```
/Users/nicolas/IdeaProjects/hivemq-edge/hivemq-edge-frontend/
└── src/__test-utils__/adapters/
    ├── index.ts           # Exports
    ├── types.ts           # MockAdapterType enum
    ├── ads.ts             # ADS mock
    ├── bacnetip.ts        # BACnet/IP mock (minimal)
    ├── databases.ts       # Databases mock
    ├── eip.ts             # EIP mock
    ├── file.ts            # File mock (HAS ISSUES)
    ├── http.ts            # HTTP mock
    ├── modbus.ts          # Modbus mock
    ├── mqtt.ts            # MQTT constants
    ├── mtconnect.ts       # MTConnect mock
    ├── opc-ua.ts          # OPC-UA mock
    ├── s7.ts              # S7 mock
    └── simulation.ts      # Simulation mock
```
