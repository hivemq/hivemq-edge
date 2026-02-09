# Backend Analysis Operational Guide

**Task:** 38658 - Adapter JSON Schema Review  
**Date:** December 16, 2025  
**Purpose:** Instructions for AI agents to collect and analyze adapter schemas from backend

---

## Table of Contents

1. [Overview](#overview)
2. [Source Tracking](#source-tracking)
3. [Backend Repository Structure](#backend-repository-structure)
4. [Step-by-Step Collection Process](#step-1-locate-all-backend-adapter-modules)
5. [Analysis Templates](#analysis-templates)
6. [Output Document Structure](#output-document-structure)
7. [Known Backend Module Locations](#known-backend-module-locations)
8. [Common Issues to Check](#common-issues-to-check)
9. [Troubleshooting](#troubleshooting)

---

## Overview

This document describes the process to collect adapter information from the HiveMQ Edge backend codebase for comparison with frontend mocks.

**Output Documents:**

- `SCHEMA_ANALYSIS_V3.md` - Backend-focused JSON Schema analysis
- `FRONTEND_MOCKS_ANALYSIS.md` - Frontend mock compliance analysis
- `ADAPTER_INVENTORY_V2.md` - Complete backend/frontend inventory

---

## Source Tracking

**IMPORTANT:** Always record the git commit hash and date when performing analysis.

### Getting Current Commit Info

```bash
# For main hivemq-edge repository
cd /Users/nicolas/IdeaProjects/hivemq-edge
git log -1 --format="Hash: %H%nDate: %ci%nBranch: $(git rev-parse --abbrev-ref HEAD)"

# For external BACnet/IP module
cd /Users/nicolas/IdeaProjects/hivemq-edge-module-bacnetip
git log -1 --format="Hash: %H%nDate: %ci%nBranch: $(git rev-parse --abbrev-ref HEAD)"
```

### Document Header Template

All analysis documents should include:

```markdown
## Source Code Context

Analysis performed against the following repository states:

| Repository                    | Branch | Commit          | Date       |
| ----------------------------- | ------ | --------------- | ---------- |
| `hivemq-edge`                 | master | `<commit-hash>` | YYYY-MM-DD |
| `hivemq-edge-module-bacnetip` | master | `<commit-hash>` | YYYY-MM-DD |

**Note:** Re-run analysis if commits have changed significantly.
```

### Generator Script

The `tools/generate-adapter-mocks.cjs` script automatically includes git commit info in generated files.

---

## Backend Repository Structure

The HiveMQ Edge backend code is located at:

```
/Users/nicolas/IdeaProjects/hivemq-edge/
├── hivemq-edge/                    # Core module (contains Simulation adapter)
│   └── src/main/java/com/hivemq/edge/modules/adapters/
│       └── simulation/             # Simulation adapter
│
└── modules/                        # Protocol adapter modules
    ├── hivemq-edge-module-databases/
    ├── hivemq-edge-module-etherip/     # EIP adapter
    ├── hivemq-edge-module-file/
    ├── hivemq-edge-module-http/
    ├── hivemq-edge-module-modbus/
    ├── hivemq-edge-module-mtconnect/
    ├── hivemq-edge-module-opcua/
    └── hivemq-edge-module-plc4x/       # ADS + S7 adapters
```

---

## Step 1: Locate All Backend Adapter Modules

### Command to find all adapter directories:

```bash
find /Users/nicolas/IdeaProjects/hivemq-edge -type d -name "adapters" 2>/dev/null
```

### Command to find all adapter modules:

```bash
ls /Users/nicolas/IdeaProjects/hivemq-edge/modules/
```

---

## Step 2: Find Key Files for Each Adapter

For each adapter module, look for these key files:

### A. Protocol Adapter Information (Metadata)

**File pattern:** `*ProtocolAdapterInformation.java`

**Contains:**

- `getProtocolId()` - Unique adapter ID (e.g., "opcua", "modbus")
- `getProtocolName()` - Display protocol name
- `getDisplayName()` - User-facing adapter name
- `getDescription()` - Adapter description
- `getUrl()` - Documentation URL
- `getVersion()` - Version string
- `getLogoUrl()` - Icon path
- `getAuthor()` - Author name
- `getCategory()` - Adapter category enum
- `getCapabilities()` - EnumSet of capabilities (READ, WRITE, DISCOVER, COMBINE)
- `getUiSchema()` - Method that loads UI schema from resources

**Example location:**

```
modules/hivemq-edge-module-modbus/src/main/java/com/hivemq/edge/adapters/modbus/ModbusProtocolAdapterInformation.java
```

### B. Adapter Configuration (JSON Schema source)

**File pattern:** `*SpecificAdapterConfig.java` or `*AdapterConfig.java`

**Contains:**

- `@JsonProperty` annotations define property names
- `@ModuleConfigField` annotations define schema metadata:
  - `title` - Field title
  - `description` - Field description
  - `format` - Field type (IDENTIFIER, HOSTNAME, BOOLEAN, etc.)
  - `required` - Whether field is required
  - `defaultValue` - Default value
  - `numberMin`, `numberMax` - Number constraints
  - `stringMinLength`, `stringMaxLength` - String length constraints
  - `stringPattern` - Regex pattern

**Example location:**

```
modules/hivemq-edge-module-modbus/src/main/java/com/hivemq/edge/adapters/modbus/config/ModbusSpecificAdapterConfig.java
```

### C. UI Schema File

**File pattern:** `*-adapter-ui-schema.json`

**Location:** `src/main/resources/` or `build/resources/main/`

**Contains:**

- `ui:tabs` - Tab organization
- `ui:order` - Field ordering
- `ui:widget` - Widget overrides (updown, password, textarea)
- `ui:disabled` - Field disabled state
- `ui:batchMode` - Batch mode for arrays
- `ui:collapsable` - Collapsible array items

**Example location:**

```
modules/hivemq-edge-module-modbus/src/main/resources/modbus-adapter-ui-schema.json
```

### D. Tag Definition

**File pattern:** `*Tag.java` and `*TagDefinition.java`

**Location:** `config/tag/` subdirectory

**Contains:**

- Tag property definitions
- Tag-specific schema fields

**Example location:**

```
modules/hivemq-edge-module-modbus/src/main/java/com/hivemq/edge/adapters/modbus/config/tag/ModbusTagDefinition.java
```

---

## Step 3: Read UI Schema Files

### Command to find all UI schema files:

```bash
find /Users/nicolas/IdeaProjects/hivemq-edge -name "*-adapter-ui-schema.json" 2>/dev/null
```

### Command to read a UI schema:

```bash
cat /Users/nicolas/IdeaProjects/hivemq-edge/modules/hivemq-edge-module-modbus/src/main/resources/modbus-adapter-ui-schema.json
```

---

## Step 4: Read Adapter Configuration Classes

### Command to read adapter config:

```bash
cat /Users/nicolas/IdeaProjects/hivemq-edge/modules/hivemq-edge-module-modbus/src/main/java/com/hivemq/edge/adapters/modbus/config/ModbusSpecificAdapterConfig.java
```

---

## Step 5: Extract Key Information

For each adapter, extract:

### Protocol Metadata

| Field        | Java Method         | Example                                              |
| ------------ | ------------------- | ---------------------------------------------------- |
| ID           | `getProtocolId()`   | `"modbus"`                                           |
| Name         | `getProtocolName()` | `"Modbus TCP"`                                       |
| Display Name | `getDisplayName()`  | `"Modbus Protocol Adapter"`                          |
| Description  | `getDescription()`  | `"Connects HiveMQ Edge to existing Modbus devices."` |
| URL          | `getUrl()`          | `"https://docs.hivemq.com/..."`                      |
| Logo         | `getLogoUrl()`      | `"/module/images/modbus-icon.png"`                   |
| Author       | `getAuthor()`       | `"HiveMQ"`                                           |
| Category     | `getCategory()`     | `ProtocolAdapterCategory.INDUSTRIAL`                 |
| Capabilities | `getCapabilities()` | `EnumSet.of(READ, DISCOVER)`                         |

### Schema Properties

For each `@ModuleConfigField` annotation:
| Annotation Field | JSON Schema Field |
|------------------|-------------------|
| `title` | `title` |
| `description` | `description` |
| `format` | `format` |
| `required` | In `required` array |
| `defaultValue` | `default` |
| `numberMin` | `minimum` |
| `numberMax` | `maximum` |
| `stringMinLength` | `minLength` |
| `stringMaxLength` | `maxLength` |
| `stringPattern` | `pattern` |

---

## Step 6: Compare with Frontend Mocks

### Frontend mock location:

```
hivemq-edge-frontend/src/__test-utils__/adapters/
```

### Comparison checklist:

1. [ ] Protocol ID matches
2. [ ] Protocol name matches
3. [ ] Display name matches
4. [ ] Description matches
5. [ ] URL matches
6. [ ] Logo URL matches
7. [ ] Author matches
8. [ ] Category matches
9. [ ] Capabilities match
10. [ ] Config schema properties match
11. [ ] UI schema matches
12. [ ] Tag schema matches

---

## Analysis Templates

Use these templates when analyzing adapters to ensure consistent documentation.

### Template A: Per-Adapter Schema Analysis

For each adapter in `SCHEMA_ANALYSIS_V3.md`, use this format:

```markdown
### 5.X {Adapter Name} Adapter

**Overall Assessment:** ✅ Good | ⚠️ Partial Issues | ❌ Has Issues | 🔴 Critical

| Aspect          | Status   | Notes         |
| --------------- | -------- | ------------- |
| JSON Schema     | ✅/⚠️/❌ | {description} |
| UI Schema       | ✅/⚠️/❌ | {description} |
| Titles          | ✅/⚠️/❌ | {description} |
| Descriptions    | ✅/⚠️/❌ | {description} |
| Required fields | ✅/⚠️/❌ | {description} |
| Validation      | ✅/⚠️/❌ | {description} |
| Tag Schema      | ✅/⚠️/❌ | {description} |

#### Config Fields

| Field  | Type   | Title     | Constraints       | Assessment |
| ------ | ------ | --------- | ----------------- | ---------- |
| {name} | {type} | "{title}" | {min/max/pattern} | ✅/⚠️/❌   |

#### Issues Found

1. {issue description}

#### UI Schema Analysis

- Widget usage: {correct/incorrect}
- Tab organization: {description}
- Field ordering: {description}
```

### Template B: Frontend Mock Compliance

For each adapter in `FRONTEND_MOCKS_ANALYSIS.md`, use this format:

```markdown
### 3.X {Adapter Name} Adapter

**File:** `{filename}.ts`

| Component            | Status   | Notes         |
| -------------------- | -------- | ------------- |
| MOCK*PROTOCOL*{NAME} | ✅/⚠️/❌ | {description} |
| MOCK*ADAPTER*{NAME}  | ✅/⚠️/❌ | {description} |
| MOCK*SCHEMA*{NAME}   | ✅/⚠️/❌ | {description} |

**Config Schema Compliance:**

| Field  | Backend | Frontend | Match |
| ------ | ------- | -------- | ----- |
| {name} | ✅/❌   | ✅/❌    | ✅/❌ |

**UI Schema Compliance:**

| Property   | Backend | Frontend | Match |
| ---------- | ------- | -------- | ----- |
| {property} | {value} | {value}  | ✅/❌ |

**Issues Found:**

1. {issue with severity}

**Overall:** ✅ COMPLIANT | ⚠️ PARTIAL | ❌ NON-COMPLIANT | 🔴 CRITICAL
```

### Template C: Adapter Inventory Entry

For each adapter in `ADAPTER_INVENTORY_V2.md`, use this format:

```markdown
### X. {Adapter Name} Adapter

| Property         | Backend         | Frontend Mock      |
| ---------------- | --------------- | ------------------ |
| **ID**           | `{id}`          | `{id}`             |
| **Protocol**     | {protocol name} | {protocol name}    |
| **Name**         | {display name}  | {display name}     |
| **Module**       | `{module name}` | `{mock file path}` |
| **Category**     | {CATEGORY}      | {category}         |
| **Capabilities** | {caps}          | {caps}             |
| **UI Schema**    | `{filename}`    | {status}           |

#### Backend Files

- Config: `{path to config class}`
- Information: `{path to info class}`
- UI Schema: `{path to ui schema json}`
- Tag Definition: `{path to tag class}`

#### Frontend Files

- Protocol Mock: `{path}`
- Exports: `{export names}`

#### Mock Completeness

| Component        | Status   |
| ---------------- | -------- |
| Protocol         | ✅/⚠️/❌ |
| Adapter Instance | ✅/⚠️/❌ |
| Config Schema    | ✅/⚠️/❌ |
| UI Schema        | ✅/⚠️/❌ |
| Tag Schema       | ✅/⚠️/❌ |

#### Notes

- {any special notes}
```

---

## Output Document Structure

### SCHEMA_ANALYSIS Structure

```markdown
# Backend Schema Analysis Report

## 1. Analysis Methodology

- Checklist of what to verify
- Mapping from Java annotations to JSON Schema

## 2. JSON Schema Generation Analysis

- @ModuleConfigField to JSON Schema mapping
- Format type mapping
- Known issues (type constraint mismatches)
- Required fields verification
- Enum validation
- Pattern/Regex validation

## 3. UI Schema Analysis

- Widget selection audit
- Tab organization audit
- ui:disabled analysis

## 4. User-Facing Content Analysis

- Title and description audit per adapter
- Consistency issues summary
- i18n readiness

## 5. Per-Adapter Analysis

- 5.1 through 5.11 (one section per adapter using Template A)

## 6. External Module Structure Audit

- Internal vs External module comparison
- Structure compliance checklist

## 7. Property Dependencies Analysis

- Identified dependencies in backend code
- Error processing analysis
- Cross-field validation missing

## 8. Summary & Recommendations

- Critical issues table
- High priority issues table
- Medium priority issues table
- Widget recommendations
- Checklist for new adapters
```

### FRONTEND_MOCKS_ANALYSIS Structure

```markdown
# Frontend Mocks Analysis Report

## 1. Overview

- Mock location
- Mock components explanation

## 2. Mock Structure Analysis

- Expected mock structure
- MockAdapterType enum status

## 3. Per-Adapter Compliance Analysis

- 3.1 through 3.11 (one section per adapter using Template B)

## 4. Summary of Issues

- Critical issues table
- High priority issues table
- Medium priority issues table
- Compliance summary table

## 5. Remediation Scripts

- Code fixes for each issue

## 6. Automated Update Script

- Usage instructions
```

### ADAPTER_INVENTORY Structure

```markdown
# Adapter Inventory

## Overview

- Backend module locations table
- Frontend mock locations table

## Per-Adapter Sections

- 1 through 11 (one section per adapter using Template C)

## Summary Tables

- Mock completeness overview
- Issues summary

## File Locations Reference

- Backend directory tree
- Frontend directory tree
```

---

## Analysis Checklist

When performing analysis, verify each of these items:

### JSON Schema Consistency

- [ ] Property types match Java types
- [ ] `required` array matches `required = true` annotations
- [ ] `minimum`/`maximum` match `numberMin`/`numberMax`
- [ ] `minLength`/`maxLength` match `stringMinLength`/`stringMaxLength`
- [ ] `pattern` matches `stringPattern`
- [ ] `default` matches `defaultValue`
- [ ] `enum` values match Java enum types
- [ ] Nested objects properly defined

### JSON Schema Completeness

- [ ] All `@ModuleConfigField` properties exported to schema
- [ ] All format types properly converted
- [ ] All validations represented (regex, ranges)
- [ ] `writeOnly` flag set for `JsonProperty.Access.WRITE_ONLY`

### UI Schema Correctness

- [ ] Appropriate widgets for data types
- [ ] Correct tab organization
- [ ] Field ordering makes sense
- [ ] Batch mode for arrays
- [ ] Collapsible items configured

### User-Facing Content

- [ ] All fields have `title` (US-EN)
- [ ] All fields have `description` (US-EN)
- [ ] No internal/technical names exposed (no camelCase)
- [ ] Consistent terminology
- [ ] Proper grammar

### Property Dependencies

- [ ] Boolean toggles have conditional sub-fields
- [ ] Cross-field validations documented
- [ ] Error messages user-friendly

### Frontend Mock Compliance

- [ ] Protocol ID matches backend
- [ ] Config schema matches backend
- [ ] UI schema matches backend
- [ ] Tag schema matches backend (correct protocolId!)
- [ ] All required mock components present

---

## Known Backend Module Locations

| Adapter    | Module                         | Location     | Information Class                           |
| ---------- | ------------------------------ | ------------ | ------------------------------------------- |
| ADS        | `hivemq-edge-module-plc4x`     | Internal     | `ADSProtocolAdapterInformation.java`        |
| BACnet/IP  | `hivemq-edge-module-bacnetip`  | **External** | `BacnetipProtocolAdapterInformation.java`   |
| Databases  | `hivemq-edge-module-databases` | Internal     | `DatabasesProtocolAdapterInformation.java`  |
| EIP        | `hivemq-edge-module-etherip`   | Internal     | `EipProtocolAdapterInformation.java`        |
| File       | `hivemq-edge-module-file`      | Internal     | `FileProtocolAdapterInformation.java`       |
| HTTP       | `hivemq-edge-module-http`      | Internal     | `HttpProtocolAdapterInformation.java`       |
| Modbus     | `hivemq-edge-module-modbus`    | Internal     | `ModbusProtocolAdapterInformation.java`     |
| MTConnect  | `hivemq-edge-module-mtconnect` | Internal     | `MtConnectProtocolAdapterInformation.java`  |
| OPC-UA     | `hivemq-edge-module-opcua`     | Internal     | `OpcUaProtocolAdapterInformation.java`      |
| S7         | `hivemq-edge-module-plc4x`     | Internal     | `S7ProtocolAdapterInformation.java`         |
| Simulation | `hivemq-edge` (core)           | Internal     | `SimulationProtocolAdapterInformation.java` |

### Internal Modules Location

Adapters in the main `hivemq-edge` repository:

```
/Users/nicolas/IdeaProjects/hivemq-edge/
├── hivemq-edge/                    # Core module
│   └── src/main/java/com/hivemq/edge/
│       └── modules/adapters/
│           └── simulation/         # Simulation adapter
│
└── modules/                        # Protocol adapter modules
    ├── hivemq-edge-module-databases/
    ├── hivemq-edge-module-etherip/
    ├── hivemq-edge-module-file/
    ├── hivemq-edge-module-http/
    ├── hivemq-edge-module-modbus/
    ├── hivemq-edge-module-mtconnect/
    ├── hivemq-edge-module-opcua/
    └── hivemq-edge-module-plc4x/   # Contains ADS + S7 adapters
```

### External Modules Location

Some adapters are in separate repositories:

```
/Users/nicolas/IdeaProjects/
└── hivemq-edge-module-bacnetip/    # BACnet/IP adapter (separate repo)
    └── src/
        ├── main/
        │   ├── java/com/hivemq/edge/adapters/bacnetip/
        │   │   ├── BacnetipProtocolAdapterInformation.java
        │   │   ├── config/
        │   │   │   ├── BacnetipSpecificAdapterConfig.java
        │   │   │   └── tag/
        │   │   │       └── BacnetTagDefinition.java
        │   └── resources/
        │       └── bacnet-adapter-ui-schema.json
        └── test/
```

---

## Common Issues to Check

1. **Invalid type constraints** - String constraints (minLength, maxLength, pattern) applied to integer fields
2. **UI:disabled mismatch** - Backend may have different default than frontend
3. **Missing writeOnly** - `access = JsonProperty.Access.WRITE_ONLY` should result in `writeOnly: true`
4. **Tag schema protocolId** - Must match adapter's protocol ID
5. **Capability mismatch** - Frontend may have different capabilities than backend

---

## Troubleshooting

If UI schema file is not found in `src/main/resources/`:

1. Check `build/resources/main/` for built version
2. Look in the `*ProtocolAdapterInformation.java` file for `getUiSchema()` method to see where it loads from
3. Check if schema is embedded in Java code rather than external JSON file
