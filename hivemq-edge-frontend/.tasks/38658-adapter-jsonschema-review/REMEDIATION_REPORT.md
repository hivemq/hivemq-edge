# Remediation Report

**Task:** 38658 - Adapter JSON Schema Review
**Date:** December 17, 2025
**Version:** 4.0 (Corrected Widget Coverage Analysis)

---

## Executive Summary

This report summarizes **28 issues** identified during the comprehensive adapter JSON Schema review, including schema validation, UI schema configuration, enum display names, conditional visibility, and RJSF implementation gaps.

### Issue Overview

| Codebase     | Severity    | Count | Automated Fix |
| ------------ | ----------- | ----- | ------------- |
| **Frontend** | 🔴 Critical | 1     | ✅ Yes        |
| **Frontend** | 🟠 High     | 4     | ✅ Yes        |
| **Frontend** | 🟡 Medium   | 1     | ❌ No         |
| **Frontend** | 🟢 Low      | 3     | ❌ No         |
| **Backend**  | 🔴 Critical | 1     | ❌ No         |
| **Backend**  | 🟠 High     | 3     | ❌ No         |
| **Backend**  | 🟡 Medium   | 12    | ❌ No         |
| **Backend**  | 🟢 Low      | 3     | ❌ No         |

**Total Issues: 28** (9 Frontend, 19 Backend)

### Critical Issues Requiring Immediate Action

| ID   | Severity    | Component    | Issue                                         | Impact               |
| ---- | ----------- | ------------ | --------------------------------------------- | -------------------- |
| F-C1 | 🔴 Critical | File Adapter | Tag schema completely wrong (has HTTP fields) | Broken functionality |
| B-C1 | 🔴 Critical | Databases    | `getTrustCertificate()` returns wrong field   | Runtime logic bug    |

### High Priority Issues by Category

| Category               | Count | Description                                   |
| ---------------------- | ----- | --------------------------------------------- |
| Schema Validation      | 4     | Invalid constraints, missing fields           |
| Conditional Visibility | 2     | Missing field dependencies                    |
| Enum Display Names     | 4     | Raw technical values instead of friendly text |

### Analysis Coverage

This report incorporates findings from:

- ✅ Schema structure analysis
- ✅ Backend vs Frontend comparison
- ✅ Enum display names audit
- ✅ Intentionality analysis (semantic mismatches)
- ✅ Conditional field visibility analysis
- ✅ Custom widget coverage analysis (corrected)

**Important Note on Widget Specification Issues:**

The initial widget coverage analysis incorrectly identified missing widget specifications (e.g., OPC-UA password fields not masked, missing updown widgets) as **frontend issues**. After correcting the analysis approach:

- ✅ **Frontend has 100% coverage** for widgets specified by backend
- ⚠️ **Missing widget specifications are BACKEND issues** - backend UI schemas need to add `ui:widget` properties
- 📋 **These will be addressed in separate backend tickets** for widget specification improvements

See [CUSTOM_WIDGET_COVERAGE_ANALYSIS.md](./CUSTOM_WIDGET_COVERAGE_ANALYSIS.md) for the corrected analysis.

---

## All Issues Summary Tables

### Frontend Issues Summary

| ID   | Severity    | Adapter   | Issue                             | Fix Type     |
| ---- | ----------- | --------- | --------------------------------- | ------------ |
| F-C1 | 🔴 Critical | File      | Tag schema wrong (HTTP fields)    | ✅ Automated |
| F-H1 | 🟠 High     | Databases | Port field invalid constraints    | ✅ Automated |
| F-H2 | 🟠 High     | Databases | Port UI schema invalid            | ✅ Automated |
| F-H3 | 🟠 High     | Modbus    | id.ui:disabled mismatch           | ✅ Automated |
| F-H4 | 🟠 High     | Types     | MockAdapterType enum incomplete   | ✅ Automated |
| F-M1 | 🟡 Medium   | RJSF      | Missing HOSTNAME format validator | ❌ Manual    |
| F-L1 | 🟢 Low      | RJSF      | ToggleWidget orphaned (unused)    | ❌ Manual    |
| F-L2 | 🟢 Low      | RJSF      | AdapterTagSelect orphaned         | ❌ Manual    |
| F-L3 | 🟢 Low      | RJSF      | InternalNotice orphaned           | ❌ Manual    |

### Backend Issues Summary

| ID    | Severity    | Module      | File                                   | Change Required                         |
| ----- | ----------- | ----------- | -------------------------------------- | --------------------------------------- |
| B-C1  | 🔴 Critical | databases   | `DatabasesAdapterConfig.java`          | Fix getter return value                 |
| B-H1  | 🟠 High     | databases   | `DatabasesAdapterConfig.java`          | Fix port constraints                    |
| B-H2  | 🟠 High     | databases   | Schema generation                      | Add encrypt→trustCertificate dependency |
| B-H3  | 🟠 High     | opcua       | Config/Schema                          | Add tls.enabled dependency              |
| B-M1  | 🟡 Medium   | databases   | `DatabasesAdapterConfig.java`          | Fix title casing                        |
| B-M2  | 🟡 Medium   | simulation  | `SimulationSpecificAdapterConfig.java` | Fix title casing                        |
| B-M3  | 🟡 Medium   | plc4x (ADS) | `ADSSpecificAdapterConfig.java`        | Fix "Ams" → "AMS"                       |
| B-M4  | 🟡 Medium   | mtconnect   | `mtconnect-adapter-ui-schema.json`     | Add proper tabs                         |
| B-M5  | 🟡 Medium   | modbus      | `ModbusTagDefinition.java`             | Add cross-field validation              |
| B-M6  | 🟡 Medium   | simulation  | `SimulationSpecificAdapterConfig.java` | Add cross-field validation              |
| B-M7  | 🟡 Medium   | bacnetip    | `BacnetipSpecificAdapterConfig.java`   | Fix copy-paste description              |
| B-M8  | 🟡 Medium   | databases   | `DatabasesAdapterConfig.java`          | Add writeOnly to id                     |
| B-M9  | 🟡 Medium   | opcua       | `SecPolicy.java` or UI schema          | Add enumNames for security policy       |
| B-M10 | 🟡 Medium   | opcua       | `Security.java` or UI schema           | Add enumNames for message security mode |
| B-M11 | 🟡 Medium   | databases   | `DatabasesAdapterConfig.java`          | Add enumNames for database type         |
| B-M12 | 🟡 Medium   | bacnetip    | `ObjectType.java`, `PropertyType.java` | Add enumNames for BACnet types          |
| B-L1  | 🟢 Low      | multiple    | Multiple files                         | Fix "millisecond" grammar               |
| B-L2  | 🟢 Low      | simulation  | `SimulationSpecificAdapterConfig.java` | Fix grammar                             |
| B-L3  | 🟢 Low      | http        | `HttpSpecificAdapterConfig.java`       | Remove question mark                    |

---

## Source Code Context

Issues identified against the following repository states:

| Repository                    | Branch                             | Commit                                     | Date       |
| ----------------------------- | ---------------------------------- | ------------------------------------------ | ---------- |
| `hivemq-edge`                 | master                             | `eabcb94278d8e5b66a2daea7b491a3ea76751d99` | 2025-12-09 |
| `hivemq-edge-module-bacnetip` | master                             | `b3458e0d5eee7fab3dddbc6cc1730e8727eb3027` | 2025-11-18 |
| Frontend mocks                | refactor/38512-datahub-js-validate | Local analysis                             | 2025-12-17 |

**Note:** Verify issues still exist if commits have changed.

---

# Part 1: Frontend Issues

> **Assignee:** Frontend Team
> **Repository:** `hivemq-edge-frontend` > **Automated fixes available:** Run `node tools/update-adapter-mocks.cjs --all`

---

## 🔴 Critical Frontend Issues

### F-C1. File Adapter Tag Schema Completely Wrong

**Location:** `src/__test-utils__/adapters/file.ts`

**Problem:** `MOCK_SCHEMA_FILE` is a copy of HTTP adapter tag schema with wrong fields and wrong `protocolId`.

**Current (Wrong):**

```typescript
export const MOCK_SCHEMA_FILE: TagSchema = {
  configSchema: {
    properties: {
      definition: {
        properties: {
          httpHeaders: {...},      // ❌ HTTP field
          httpRequestBody: {...},  // ❌ HTTP field
          url: {...},              // ❌ HTTP field
        },
        required: ['url'],
      },
    },
  },
  protocolId: 'http',  // ❌ WRONG
}
```

**Expected (from `FileTagDefinition.java`):**

- `filePath` (string, required)
- `contentType` (enum: BINARY, TEXT, JSON, XML, CSV, required)
- `protocolId: 'file'`

**Automated Fix:** `node tools/update-adapter-mocks.cjs --adapter=file`

---

## 🟠 High Priority Frontend Issues

### F-H1. Databases Adapter Port Field Invalid Constraints (Frontend Mock)

**Location:** `src/__test-utils__/adapters/databases.ts`

**Problem:** Mock has string constraints on integer field (mirrors backend bug).

**Current (Wrong):**

```typescript
port: {
  type: 'integer',
  minLength: 1,      // ❌ Invalid for integer
  maxLength: 6,      // ❌ Invalid for integer
  pattern: '...',    // ❌ Invalid for integer
},
```

**Fix:**

```typescript
port: {
  type: 'integer',
  minimum: 1,
  maximum: 65535,
},
```

**Automated Fix:** `node tools/update-adapter-mocks.cjs --adapter=databases`

---

### F-H2. Databases Adapter Port UI Schema Invalid

**Location:** `src/__test-utils__/adapters/databases.ts` (lines 129-131)

**Problem:** UI schema uses `type: 'integer'` which is invalid for uiSchema.

**Current (Wrong):**

```typescript
port: {
  type: 'integer',  // ❌ Invalid in uiSchema
},
```

**Fix:**

```typescript
port: {
  'ui:widget': 'updown',  // ✅ Correct
},
```

**Automated Fix:** `node tools/update-adapter-mocks.cjs --adapter=databases`

---

### F-H3. Modbus Adapter `id.ui:disabled` Mismatch

**Location:** `src/__test-utils__/adapters/modbus.ts`

**Problem:** Frontend has `false`, backend has `true`.

**Current:**

- Backend (`modbus-adapter-ui-schema.json`): `"ui:disabled": true`
- Frontend mock: `'ui:disabled': false`

**Fix:** Change frontend to match backend: `'ui:disabled': true`

**Automated Fix:** `node tools/update-adapter-mocks.cjs --adapter=modbus`

---

### F-H4. MockAdapterType Enum Missing Entries

**Location:** `src/__test-utils__/adapters/types.ts`

**Problem:** Enum missing `DATABASES` and `MTCONNECT`.

**Fix:**

```typescript
export enum MockAdapterType {
  // ...existing entries...
  DATABASES = 'databases',
  MTCONNECT = 'mtconnect',
}
```

**Automated Fix:** `node tools/update-adapter-mocks.cjs --adapter=types`

---

## 🟡 Medium Priority Frontend Issues

### F-M1. Missing HOSTNAME Format Validator

**Location:** `src/components/rjsf/Form/validation.utils.ts`

**Problem:** Backend specifies `format = ModuleConfigField.FieldType.HOSTNAME` in Modbus, EIP, and PLC4X adapters, but frontend has no custom validator for this format.

**Current Behavior:** No custom validation - field accepts any string.

**Expected Behavior:** Should validate hostname/IP address format.

**Affected Adapters:**

- Modbus (`host` field)
- EIP (`host` field)
- PLC4X (ADS/S7 `host` field)

**Fix:** Implement `hostname` format validator:

```typescript
// src/components/rjsf/Form/validation.utils.ts

export const validationHostname = (host: string): string | undefined => {
  if (host.length === 0) return i18n.t('rjsf.customFormats.validation.noEmptyString', { ns: 'components' })

  // Allow IPv4, IPv6, and hostnames
  const ipv4Regex = /^(\d{1,3}\.){3}\d{1,3}$/
  const ipv6Regex = /^([0-9a-fA-F]{0,4}:){7}[0-9a-fA-F]{0,4}$/
  const hostnameRegex = /^[a-zA-Z0-9]([a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?(\.[a-zA-Z0-9]([a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?)*$/

  if (ipv4Regex.test(host) || ipv6Regex.test(host) || hostnameRegex.test(host)) {
    return undefined
  }

  return i18n.t('rjsf.customFormats.validation.invalidHostname', { ns: 'components' })
}

// Add to customLocalizer:
if (error.schema === 'hostname') {
  error.message = validationHostname(error.data as string)
}

// Add to customFormatsValidator:
export const customFormatsValidator = customizeValidator(
  {
    customFormats: {
      // ... existing formats
      hostname: (host) => validationHostname(host) === undefined,
    },
  },
  customLocalizer
)
```

**Add translation:**

```json
{
  "rjsf.customFormats.validation.invalidHostname": "Must be a valid hostname or IP address"
}
```

**Manual Fix Required**

**Impact:** Users can enter invalid hostnames, causing connection errors at runtime.

---

## 🟢 Low Priority Frontend Issues

### F-L1. ToggleWidget - Orphaned Component

**Location:** `src/components/rjsf/Widgets/ToggleWidget.tsx`

**Problem:** Implemented with tests but never registered in widget registry or used anywhere.

**Impact:** Dead code, maintenance burden.

**Recommendation:** Remove or document intended future use case.

**Manual Fix Required**

---

### F-L2. AdapterTagSelect - Orphaned Component

**Location:** `src/components/rjsf/Widgets/AdapterTagSelect.tsx`

**Problem:** Implemented with tests but never registered in widget registry or used anywhere. Likely replaced by `discovery:tagBrowser` (which is currently disabled - issue #24369).

**Impact:** Dead code, maintenance burden.

**Recommendation:** Remove if truly unused, or document intended use case.

**Manual Fix Required**

---

### F-L3. InternalNotice - Orphaned Component

**Location:** `src/components/rjsf/Fields/InternalNotice.tsx`

**Problem:** Exported from `Fields/index.ts` but never imported or used anywhere.

**Impact:** Dead code.

**Recommendation:** Remove or document intended use case.

**Manual Fix Required**

---

## Frontend Remediation Commands

### Automated Fixes

```bash
# Preview all changes (dry run)
node tools/update-adapter-mocks.cjs --all --dry-run

# Apply automated fixes (F-C1, F-H1 to F-H4)
node tools/update-adapter-mocks.cjs --all

# Or fix individually:
node tools/update-adapter-mocks.cjs --adapter=file
node tools/update-adapter-mocks.cjs --adapter=databases
node tools/update-adapter-mocks.cjs --adapter=modbus
node tools/update-adapter-mocks.cjs --adapter=types
```

### Manual Fixes Required

The following issues require manual editing:

1. **F-M1** - Implement HOSTNAME format validator in `validation.utils.ts`
2. **F-L1** - Remove or document ToggleWidget
3. **F-L2** - Remove or document AdapterTagSelect
4. **F-L3** - Remove or document InternalNotice

---

## Frontend Verification

After applying fixes:

```bash
pnpm test
pnpm cypress:run:component
pnpm typecheck
```

**HOSTNAME Validator Verification:**

- Test valid hostnames: `example.com`, `sub.example.com`
- Test valid IPv4: `192.168.1.1`
- Test valid IPv6: `2001:db8::1`
- Test invalid values: `invalid..hostname`, `192.168.1.256`

---

# Part 2: Backend Issues

> **Assignee:** Backend Team
> **Repositories:** `hivemq-edge`, `hivemq-edge-module-*`, `hivemq-edge-module-bacnetip` > **Automated fixes:** None available

---

## 🔴 Critical Backend Issues

### B-C1. Databases Adapter `getTrustCertificate()` Returns Wrong Field

**Location:** `modules/hivemq-edge-module-databases/src/main/java/com/hivemq/edge/adapters/databases/config/DatabasesAdapterConfig.java`

**Problem:** Getter method returns wrong field value.

**Current (Bug):**

```java
public @NotNull Boolean getTrustCertificate() {
    return encrypt;  // ❌ Returns wrong field!
}
```

**Fix:**

```java
public @NotNull Boolean getTrustCertificate() {
    return trustCertificate;  // ✅ Correct
}
```

**Impact:** Any backend code using `getTrustCertificate()` gets the `encrypt` value instead.

---

## 🟠 High Priority Backend Issues

### B-H1. Databases Adapter Port Field Invalid Constraints

**Location:** `modules/hivemq-edge-module-databases/src/main/java/.../DatabasesAdapterConfig.java`

**Problem:** String constraints applied to Integer field.

**Current (Wrong):**

```java
@ModuleConfigField(
    stringPattern = ID_REGEX,     // ❌ Invalid for Integer
    stringMinLength = 1,          // ❌ Invalid for Integer
    stringMaxLength = 6,          // ❌ Invalid for Integer
)
protected @NotNull Integer port;
```

**Fix:**

```java
@ModuleConfigField(
    numberMin = 1,
    numberMax = 65535,
)
protected @NotNull Integer port;
```

---

### B-H2. Databases Adapter Missing `encrypt → trustCertificate` Dependency

**Location:** `DatabasesAdapterConfig.java` or schema generation

**Problem:** `trustCertificate` field shown even when `encrypt = false` (meaningless).

**Fix:** Add JSON Schema `dependencies` to hide `trustCertificate` when `encrypt = false`.

```json
{
  "dependencies": {
    "encrypt": {
      "oneOf": [
        { "properties": { "encrypt": { "const": false } } },
        {
          "properties": {
            "encrypt": { "const": true },
            "trustCertificate": { "type": "boolean" }
          }
        }
      ]
    }
  }
}
```

---

### B-H3. OPC-UA Adapter Missing `tls.enabled` Dependency

**Location:** `modules/hivemq-edge-module-opcua/src/main/java/com/hivemq/edge/adapters/opcua/config/Tls.java`

**Problem:** TLS sub-fields (`tlsChecks`, `keystore`, `truststore`) shown even when `tls.enabled = false`.

**Current Structure (from `Tls.java`):**

```java
public record Tls (
    @JsonProperty("enabled") boolean enabled,
    @JsonProperty("tlsChecks") @Nullable TlsChecks tlsChecks,
    @JsonProperty("keystore") @Nullable Keystore keystore,
    @JsonProperty("truststore") @Nullable Truststore truststore
) { ... }
```

**Fix:** Add JSON Schema `dependencies` to conditionally show keystore/truststore only when TLS is enabled:

```json
{
  "tls": {
    "type": "object",
    "properties": { ... },
    "dependencies": {
      "enabled": {
        "oneOf": [
          {
            "properties": { "enabled": { "const": false } },
            "required": ["enabled"]
          },
          {
            "properties": {
              "enabled": { "const": true },
              "tlsChecks": { "type": "string" },
              "keystore": { "type": "object" },
              "truststore": { "type": "object" }
            },
            "required": ["enabled"]
          }
        ]
      }
    }
  }
}
```

**Note:** The UI schema (`opcua-adapter-ui-schema.json`) is well-structured with proper ordering but cannot enforce conditional display without JSON Schema dependencies.

---

## 🟡 Medium Priority Backend Issues

### B-M1. Databases `connectionTimeoutSeconds` Title is camelCase

**Location:** `DatabasesAdapterConfig.java`

**Current:** `title = "connectionTimeoutSeconds"`
**Fix:** `title = "Connection Timeout (seconds)"`

---

### B-M2. Simulation `simulationToMqtt` Title is camelCase

**Location:** `hivemq-edge/src/main/java/.../simulation/config/SimulationSpecificAdapterConfig.java`

**Current:** `title = "simulationToMqtt"`
**Fix:** `title = "Simulation To MQTT Config"`

---

### B-M3. ADS Adapter "Ams" Inconsistent Casing

**Location:** `modules/hivemq-edge-module-plc4x/src/main/java/.../ads/config/ADSSpecificAdapterConfig.java`

**Current:** `"Source Ams Net Id"`, `"Target Ams Net Id"`
**Fix:** `"Source AMS Net ID"`, `"Target AMS Net ID"`

---

### B-M4. MTConnect UI Schema Minimal

**Location:** `modules/hivemq-edge-module-mtconnect/src/main/resources/mtconnect-adapter-ui-schema.json`

**Problem:** Only has `id` in tabs, other fields not organized.

**Fix:** Add proper tabs for `allowUntrustedCertificates`, `httpConnectTimeoutSeconds`, `pollingIntervalMillis`, `maxPollingErrorsBeforeRemoval`.

---

### B-M5. Modbus Missing `addressRange` Cross-Validation

**Location:** `modules/hivemq-edge-module-modbus/src/main/java/.../tag/ModbusTagDefinition.java`

**Problem:** No validation that `endIdx >= startIdx`.

**Fix:** Add custom validation or JSON Schema `if/then` logic.

---

### B-M6. Simulation Missing `minValue < maxValue` Validation

**Location:** `hivemq-edge/src/main/java/.../simulation/config/SimulationSpecificAdapterConfig.java`

**Problem:** No validation that `maxValue > minValue`.

**Fix:** Add cross-field validation.

---

### B-M7. BACnet/IP Description Copy-Paste Error

**Location:** `hivemq-edge-module-bacnetip/src/main/java/.../config/BacnetipSpecificAdapterConfig.java`

**Current:** `bacnetipToMqtt` description says "from ADS to MQTT"
**Fix:** Change to "from BACnet/IP to MQTT"

---

### B-M8. Databases Adapter Missing `writeOnly` on ID Field

**Location:** `DatabasesAdapterConfig.java`

**Problem:** `id` field missing `access = JsonProperty.Access.WRITE_ONLY` which other adapters have.

**Fix:** Add `@JsonProperty(value = "id", required = true, access = JsonProperty.Access.WRITE_ONLY)`

---

### B-M9. OPC-UA Missing enumNames for Security Policy

**Location:** `modules/hivemq-edge-module-opcua/src/main/java/.../config/SecPolicy.java` or UI schema

**Problem:** `policy` enum shows raw values like `BASIC256SHA256` instead of user-friendly names.

**Enum values:** `NONE, BASIC128RSA15, BASIC256, BASIC256SHA256, AES128_SHA256_RSAOAEP, AES256_SHA256_RSAPSS`

**Fix:** Add `enumDisplayValues` to `@ModuleConfigField` or `ui:enumNames` to UI schema:

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

---

### B-M10. OPC-UA Missing enumNames for Message Security Mode

**Location:** `modules/hivemq-edge-module-opcua/src/main/java/.../config/Security.java` or UI schema

**Problem:** `messageSecurityMode` enum shows raw values like `SIGN_AND_ENCRYPT`.

**Enum values:** `IGNORED, NONE, SIGN, SIGN_AND_ENCRYPT`

**Fix:** Add display names:

```json
{
  "messageSecurityMode": {
    "ui:enumNames": ["Ignored (use policy default)", "None", "Sign Only", "Sign and Encrypt"]
  }
}
```

---

### B-M11. Databases Missing enumNames for Database Type

**Location:** `modules/hivemq-edge-module-databases/src/main/java/.../DatabasesAdapterConfig.java`

**Problem:** `type` enum shows `POSTGRESQL`, `MYSQL`, `MSSQL` instead of friendly names.

**Fix:** Add `enumDisplayValues`:

```java
@ModuleConfigField(
    title = "Database Type",
    enumDisplayValues = {"PostgreSQL", "MySQL", "Microsoft SQL Server"}
)
```

---

### B-M12. BACnet/IP Missing enumNames for Object/Property Types

**Location:** `hivemq-edge-module-bacnetip/src/main/java/.../config/ObjectType.java` and `PropertyType.java`

**Problem:** BACnet object and property types show raw technical values to users.

**Fix:** Add `enumDisplayValues` to make BACnet types understandable.

---

## 🟢 Low Priority Backend Issues

### B-L1. Multiple Adapters: "millisecond" Should Be "milliseconds"

**Locations:**

- `modules/hivemq-edge-module-file/src/main/java/.../FileSpecificAdapterConfig.java`
- `modules/hivemq-edge-module-databases/src/main/java/.../DatabasesAdapterConfig.java`
- `modules/hivemq-edge-module-mtconnect/src/main/java/.../MtConnectAdapterConfig.java`

**Current:** `"Time in millisecond that this endpoint will be polled"`
**Fix:** `"Time in milliseconds that this endpoint will be polled"`

---

### B-L2. Simulation "Minimum of delay" Grammar

**Location:** `hivemq-edge/src/main/java/.../simulation/config/SimulationSpecificAdapterConfig.java`

**Current:** `"Minimum of delay"`
**Fix:** `"Minimum Delay"` or `"Minimum delay time"`

---

### B-L3. HTTP "Assert JSON Response?" Has Question Mark

**Location:** `modules/hivemq-edge-module-http/src/main/java/.../HttpSpecificAdapterConfig.java`

**Current:** `title = "Assert JSON Response?"`
**Fix:** `title = "Assert JSON Response"` (remove question mark)

---

## Cross-Team Coordination Notes

1. **B-H1 (Backend port constraints)** should be fixed before **F-H1 (Frontend mock)** to ensure frontend mock matches actual backend behavior.

2. **After backend fixes**, frontend team should regenerate mocks:

   ```bash
   node tools/generate-adapter-mocks.cjs
   ```

3. **BACnet/IP mock** can be generated from external repo:

   ```bash
   node tools/generate-adapter-mocks.cjs --adapter=bacnetip
   ```

4. **Widget specification improvements** will be addressed in separate backend tickets:
   - Missing `ui:widget: "password"` for password fields (e.g., OPC-UA)
   - Missing `ui:widget: "updown"` for numeric fields (timeouts, intervals, ports)
   - See [CUSTOM_WIDGET_COVERAGE_ANALYSIS.md](./CUSTOM_WIDGET_COVERAGE_ANALYSIS.md) for details

---

## Related Documentation

For detailed analysis supporting these issues, see:

- [CUSTOM_WIDGET_COVERAGE_ANALYSIS.md](./CUSTOM_WIDGET_COVERAGE_ANALYSIS.md) - RJSF widget/field/format implementation gaps (corrected)
- [ENUM_DISPLAY_NAMES_AUDIT.md](./ENUM_DISPLAY_NAMES_AUDIT.md) - Enum display name coverage
- [CONDITIONAL_VISIBILITY_ANALYSIS.md](./CONDITIONAL_VISIBILITY_ANALYSIS.md) - Conditional field dependencies
- [INTENTIONALITY_ANALYSIS.md](./INTENTIONALITY_ANALYSIS.md) - Semantic mismatch detection
- [SCHEMA_ANALYSIS_V3.md](./SCHEMA_ANALYSIS_V3.md) - Backend schema analysis
- [FRONTEND_MOCKS_ANALYSIS.md](./FRONTEND_MOCKS_ANALYSIS.md) - Frontend mock compliance
- [TICKETS_BACKEND_SUMMARY.md](./TICKETS_BACKEND_SUMMARY.md) - Backend ticket grouping and remediation plan
