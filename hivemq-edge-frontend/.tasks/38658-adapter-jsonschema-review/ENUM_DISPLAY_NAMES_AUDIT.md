# Enum Display Names Audit

**Task:** 38658 - Adapter JSON Schema Review  
**Date:** December 16, 2025

---

## Source Code Context

| Repository                    | Branch | Commit                                     | Date       |
| ----------------------------- | ------ | ------------------------------------------ | ---------- |
| `hivemq-edge`                 | master | `eabcb94278d8e5b66a2daea7b491a3ea76751d99` | 2025-12-09 |
| `hivemq-edge-module-bacnetip` | master | `b3458e0d5eee7fab3dddbc6cc1730e8727eb3027` | 2025-11-18 |

---

## Overview

This audit identifies all enum properties in adapter configurations and checks whether user-friendly display names are provided.

**Without `enumNames`:** Users see raw enum values like `BASIC256SHA256`
**With `enumNames`:** Users see friendly names like `"Basic 256 SHA256"`

---

## Audit Results

### Summary

| Adapter    | Enum Fields                                | Has enumNames      | Has ui:enumNames    | Status     |
| ---------- | ------------------------------------------ | ------------------ | ------------------- | ---------- |
| ADS        | 1 (dataType)                               | ✅ Java            | ❌                  | ⚠️ Partial |
| BACnet/IP  | 2 (objectType, propertyType)               | ❌                 | ❌                  | ❌ Missing |
| Databases  | 1 (type)                                   | ❌                 | ❌                  | ❌ Missing |
| EIP        | 1 (dataType)                               | ✅ Java            | ❌                  | ⚠️ Partial |
| File       | 1 (contentType)                            | ✅ Java            | ❌                  | ⚠️ Partial |
| HTTP       | 1 (httpRequestMethod)                      | ❌                 | ❌                  | ❌ Missing |
| Modbus     | 2 (dataType, addressType)                  | ✅ Java (dataType) | ❌                  | ⚠️ Partial |
| MTConnect  | 0                                          | N/A                | N/A                 | ✅ N/A     |
| OPC-UA     | 3 (policy, messageSecurityMode, tlsChecks) | ❌                 | ✅ (tlsChecks only) | ⚠️ Partial |
| S7         | 2 (dataType, controllerType)               | ✅ Java (dataType) | ❌                  | ⚠️ Partial |
| Simulation | 0                                          | N/A                | N/A                 | ✅ N/A     |

---

## Detailed Findings

### OPC-UA Adapter

#### `SecPolicy` (Security Policy)

**Location:** `config/SecPolicy.java`

**Enum Values:**

```
NONE, BASIC128RSA15, BASIC256, BASIC256SHA256, AES128_SHA256_RSAOAEP, AES256_SHA256_RSAPSS
```

**Current:** No `enumNames` defined - users see raw technical values

**Recommendation:**

```json
{
  "policy": {
    "ui:enumNames": [
      "None",
      "Basic 128 RSA 15 (deprecated)",
      "Basic 256 (deprecated)",
      "Basic 256 SHA256",
      "AES 128 SHA256 RSA-OAEP",
      "AES 256 SHA256 RSA-PSS"
    ]
  }
}
```

#### `MsgSecurityMode` (Message Security Mode)

**Location:** `config/MsgSecurityMode.java`

**Enum Values:**

```
IGNORED, NONE, SIGN, SIGN_AND_ENCRYPT
```

**Current:** No `enumNames` defined

**Recommendation:**

```json
{
  "messageSecurityMode": {
    "ui:enumNames": ["Ignored (use policy default)", "None", "Sign Only", "Sign and Encrypt"]
  }
}
```

#### `TlsChecks` (Certificate Validation)

**Location:** `config/TlsChecks.java`

**Enum Values:**

```
NONE, APPLICATION_URI, STANDARD, ALL
```

**Current:** ✅ Has `ui:enumNames` in UI schema with descriptions

---

### BACnet/IP Adapter

#### `ObjectType`

**Location:** `config/ObjectType.java`

**Enum Values:** (needs verification - likely many BACnet object types)

**Current:** No `enumNames` defined

**Issue:** BACnet object types are technical - users need descriptions

#### `PropertyType`

**Location:** `config/PropertyType.java`

**Enum Values:** (needs verification)

**Current:** No `enumNames` defined

---

### Databases Adapter

#### Database Type (`type`)

**Location:** `DatabasesAdapterConfig.java`

**Enum Values:**

```
POSTGRESQL, MYSQL, MSSQL
```

**Current:** No `enumNames` defined - shows raw values

**Recommendation:**

```json
{
  "type": {
    "ui:enumNames": ["PostgreSQL", "MySQL", "Microsoft SQL Server"]
  }
}
```

---

### HTTP Adapter

#### `HttpRequestMethod`

**Location:** HTTP adapter config

**Enum Values:**

```
GET, POST, PUT, PATCH, DELETE
```

**Current:** No `enumNames` defined

**Assessment:** ✅ Acceptable - HTTP methods are well-known

---

### Modbus Adapter

#### `ModbusDataType` (Data Type)

**Location:** `config/ModbusToMqttMapping.java`

**Has `enumDisplayValues` in Java:**

```java
enumDisplayValues = {
    "Int16 (Signed 16-bit Integer)",
    "UInt16 (Unsigned 16-bit Integer)",
    // ... etc
}
```

**Status:** ✅ Good - has display values

#### `AddressType`

**Location:** Tag definition

**Current:** Unknown - needs verification

---

### File Adapter

#### `ContentType`

**Location:** `tag/FileTagDefinition.java`

**Has `enumDisplayValues` in Java:**

```java
enumDisplayValues = {
    "Binary (raw bytes)",
    "Text (UTF-8 string)",
    "JSON (parsed JSON)",
    "XML (parsed XML)",
    "CSV (parsed CSV)"
}
```

**Status:** ✅ Good - has display values

---

### S7 Adapter

#### `S7DataType` (Data Type)

**Location:** `types/siemens/config/Plc4xToMqttMapping.java`

**Has `enumDisplayValues` in Java:**

```java
enumDisplayValues = {
    "BOOL (Boolean)",
    "BYTE (8-bit unsigned)",
    // ... etc
}
```

**Status:** ✅ Good - has display values

#### `S7ControllerType`

**Location:** `S7SpecificAdapterConfig.java`

**Enum Values:**

```
S7_300, S7_400, S7_1200, S7_1500, LOGO
```

**Current:** Unknown if has display values

**Recommendation:**

```json
{
  "controllerType": {
    "ui:enumNames": ["S7-300", "S7-400", "S7-1200", "S7-1500", "LOGO!"]
  }
}
```

---

### EIP Adapter

#### `EipDataType` (Data Type)

**Location:** `config/EipToMqttMapping.java`

**Has `enumDisplayValues` in Java:**

```java
enumDisplayValues = {
    "BOOL (Boolean)",
    "SINT (8-bit signed integer)",
    // ... etc
}
```

**Status:** ✅ Good - has display values

---

### ADS Adapter

#### `ADSDataType` (Data Type)

**Location:** `types/ads/config/Plc4xToMqttMapping.java` (shared with S7)

**Has `enumDisplayValues` in Java:** ✅

**Status:** ✅ Good - has display values

---

## Recommendations

### High Priority (User Experience Impact)

| Adapter   | Field                        | Action                                                         |
| --------- | ---------------------------- | -------------------------------------------------------------- |
| OPC-UA    | `policy`                     | Add `ui:enumNames` to UI schema                                |
| OPC-UA    | `messageSecurityMode`        | Add `ui:enumNames` to UI schema                                |
| Databases | `type`                       | Add `enumDisplayValues` to Java or `ui:enumNames` to UI schema |
| BACnet/IP | `objectType`, `propertyType` | Add `enumDisplayValues` to Java                                |

### Medium Priority (Acceptable but Improvable)

| Adapter | Field            | Action                             |
| ------- | ---------------- | ---------------------------------- |
| S7      | `controllerType` | Add `ui:enumNames` for consistency |

### Low Priority (Already Acceptable)

| Adapter | Field               | Reason                             |
| ------- | ------------------- | ---------------------------------- |
| HTTP    | `httpRequestMethod` | HTTP methods are universally known |

---

## Implementation Options

### Option 1: Java `enumDisplayValues` (Backend)

```java
@ModuleConfigField(
    title = "Security Policy",
    enumDisplayValues = {
        "None",
        "Basic 128 RSA 15 (deprecated)",
        // ...
    }
)
private SecPolicy policy;
```

**Pros:** Single source of truth, works for all frontends
**Cons:** Requires backend change

### Option 2: UI Schema `ui:enumNames` (Frontend)

```json
{
  "policy": {
    "ui:enumNames": ["None", "Basic 128 RSA 15", ...]
  }
}
```

**Pros:** Can be done in UI schema file
**Cons:** Duplicate definition, may get out of sync

### Recommendation

Use **Option 1 (Java `enumDisplayValues`)** for consistency across the codebase, since most adapters already use this approach.

---

## Issues to Add to REMEDIATION_REPORT

### B-M9. OPC-UA Missing enumNames for Security Policy

**Location:** `modules/hivemq-edge-module-opcua/src/main/java/.../config/SecPolicy.java` or UI schema

**Problem:** `policy` enum shows raw values like `BASIC256SHA256` instead of friendly names

**Fix:** Add `enumDisplayValues` to `@ModuleConfigField` or `ui:enumNames` to UI schema

### B-M10. OPC-UA Missing enumNames for Message Security Mode

**Location:** `modules/hivemq-edge-module-opcua/src/main/java/.../config/Security.java` or UI schema

**Problem:** `messageSecurityMode` enum shows raw values

**Fix:** Add `enumDisplayValues` or `ui:enumNames`

### B-M11. Databases Missing enumNames for Database Type

**Location:** `modules/hivemq-edge-module-databases/src/main/java/.../DatabasesAdapterConfig.java`

**Problem:** `type` enum shows `POSTGRESQL`, `MYSQL`, `MSSQL` instead of friendly names

**Fix:** Add `enumDisplayValues` with values like "PostgreSQL", "MySQL", "Microsoft SQL Server"

### B-M12. BACnet/IP Missing enumNames for Object/Property Types

**Location:** `hivemq-edge-module-bacnetip/src/main/java/.../config/ObjectType.java` and `PropertyType.java`

**Problem:** BACnet object and property types show raw technical values

**Fix:** Add `enumDisplayValues` to make BACnet types understandable to users
