# Frontend Mocks Analysis Report

**Task:** 38658 - Adapter JSON Schema Review  
**Date:** December 16, 2025  
**Version:** 1.1

---

## Source Code Context

Analysis performed against the following repository states:

| Repository                    | Branch | Commit                                     | Date       |
| ----------------------------- | ------ | ------------------------------------------ | ---------- |
| `hivemq-edge` (frontend)      | master | `eabcb94278d8e5b66a2daea7b491a3ea76751d99` | 2025-12-09 |
| `hivemq-edge` (backend)       | master | `eabcb94278d8e5b66a2daea7b491a3ea76751d99` | 2025-12-09 |
| `hivemq-edge-module-bacnetip` | master | `b3458e0d5eee7fab3dddbc6cc1730e8727eb3027` | 2025-11-18 |

**Note:** Re-run analysis if commits have changed significantly.

---

## Table of Contents

1. [Overview](#1-overview)
2. [Mock Structure Analysis](#2-mock-structure-analysis)
3. [Per-Adapter Compliance Analysis](#3-per-adapter-compliance-analysis)
4. [Summary of Issues](#4-summary-of-issues)
5. [Remediation Scripts](#5-remediation-scripts)
6. [Automated Update Script](#6-automated-update-script)
7. [Automated Generate Script](#7-automated-generate-script)

---

## 1. Overview

### Frontend Mock Location

```
src/__test-utils__/adapters/
├── index.ts           # Exports
├── types.ts           # MockAdapterType enum
├── ads.ts             # ADS adapter mock
├── bacnetip.ts        # BACnet/IP adapter mock
├── databases.ts       # Databases adapter mock
├── eip.ts             # EIP adapter mock
├── file.ts            # File adapter mock
├── http.ts            # HTTP adapter mock
├── modbus.ts          # Modbus adapter mock
├── mqtt.ts            # MQTT constants
├── mtconnect.ts       # MTConnect adapter mock
├── opc-ua.ts          # OPC-UA adapter mock
├── s7.ts              # S7 adapter mock
└── simulation.ts      # Simulation adapter mock
```

### Mock Components

Each adapter can export up to 3 mock components:

| Component         | Type              | Purpose                                       |
| ----------------- | ----------------- | --------------------------------------------- |
| `MOCK_PROTOCOL_*` | `ProtocolAdapter` | Protocol metadata + config schema + UI schema |
| `MOCK_ADAPTER_*`  | `Adapter`         | Sample adapter instance with config data      |
| `MOCK_SCHEMA_*`   | `TagSchema`       | Tag configuration schema                      |

---

## 4. Summary of Issues

### 4.1 Critical Issues (Must Fix)

| #   | Adapter | Issue                      | File      | Line    |
| --- | ------- | -------------------------- | --------- | ------- |
| 1   | File    | Tag schema is HTTP copy    | `file.ts` | 113-193 |
| 2   | File    | `protocolId: 'http'` wrong | `file.ts` | ~193    |

### 4.2 High Priority Issues (Should Fix)

| #   | Adapter   | Issue                    | File           |
| --- | --------- | ------------------------ | -------------- |
| 3   | Databases | Invalid port constraints | `databases.ts` |
| 4   | Databases | Invalid port uiSchema    | `databases.ts` |
| 5   | Modbus    | id.ui:disabled mismatch  | `modbus.ts`    |
| 6   | types.ts  | Missing DATABASES enum   | `types.ts`     |
| 7   | types.ts  | Missing MTCONNECT enum   | `types.ts`     |

### 4.3 Medium Priority Issues (Consider)

| #   | Adapter   | Issue                     | File           |
| --- | --------- | ------------------------- | -------------- |
| 8   | Databases | Missing writeOnly on id   | `databases.ts` |
| 9   | BACnet/IP | Incomplete mock           | `bacnetip.ts`  |
| 10  | MTConnect | Missing adapter/tag mocks | `mtconnect.ts` |

### 4.4 Compliance Summary

| Adapter    | Protocol | Adapter | Tag | Overall         |
| ---------- | -------- | ------- | --- | --------------- |
| ADS        | ✅       | ✅      | ✅  | ✅ Compliant    |
| BACnet/IP  | ⚠️       | ❌      | ❌  | ❌ Incomplete   |
| Databases  | ⚠️       | ❌      | ❌  | ❌ Has Issues   |
| EIP        | ✅       | ✅      | ✅  | ✅ Compliant    |
| File       | ✅       | ✅      | 🔴  | 🔴 **Critical** |
| HTTP       | ✅       | ✅      | ✅  | ✅ Compliant    |
| Modbus     | ⚠️       | ✅      | ✅  | ⚠️ UI Mismatch  |
| MTConnect  | ✅       | ❌      | ❌  | ⚠️ Incomplete   |
| OPC-UA     | ✅       | ✅      | ✅  | ⚠️ Unverified   |
| S7         | ✅       | ✅      | ✅  | ✅ Compliant    |
| Simulation | ✅       | ✅      | ✅  | ✅ Compliant    |

---

## 7. Automated Generate Script

**File:** [`tools/generate-adapter-mocks.cjs`](../../tools/generate-adapter-mocks.cjs)

This script reads backend Java adapter configuration files and UI schema JSON files to generate TypeScript mock files for use in testing and development.

### Usage

```bash
# Dry run - see what would be generated
node tools/generate-adapter-mocks.cjs --dry-run

# Generate all adapters
node tools/generate-adapter-mocks.cjs

# Generate specific adapter
node tools/generate-adapter-mocks.cjs --adapter=modbus

# Custom backend path
node tools/generate-adapter-mocks.cjs --backend-path=/path/to/hivemq-edge

# Verbose output
node tools/generate-adapter-mocks.cjs --verbose
```

### Output Structure

```
src/api/__generated__/adapters/
├── index.ts           # Re-exports all adapters
├── types.ts           # AdapterType enum
├── ads.ts             # ADS adapter mock
├── modbus.ts          # Modbus adapter mock
├── s7.ts              # S7 adapter mock
├── file.ts            # File adapter mock
├── http.ts            # HTTP adapter mock
├── simulation.ts      # Simulation adapter mock
├── eip.ts             # EIP adapter mock
├── databases.ts       # Databases adapter mock (placeholder)
└── ...
```

### Generated Content

Each adapter file contains:

| Export              | Type              | Description                                |
| ------------------- | ----------------- | ------------------------------------------ |
| `MOCK_PROTOCOL_*`   | `ProtocolAdapter` | Protocol metadata, config schema, uiSchema |
| `MOCK_ADAPTER_*`    | `Adapter`         | Sample adapter instance                    |
| `MOCK_TAG_SCHEMA_*` | `TagSchema`       | Tag configuration schema (if available)    |
