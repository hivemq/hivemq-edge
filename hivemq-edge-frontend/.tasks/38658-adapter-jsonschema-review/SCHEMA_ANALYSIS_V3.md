# Backend Schema Analysis Report

**Task:** 38658 - Adapter JSON Schema Review  
**Date:** December 16, 2025  
**Version:** 3.1 - Backend-Focused Analysis

---

## Source Code Context

Analysis performed against the following repository states:

| Repository                    | Branch | Commit                                     | Date       |
| ----------------------------- | ------ | ------------------------------------------ | ---------- |
| `hivemq-edge`                 | master | `eabcb94278d8e5b66a2daea7b491a3ea76751d99` | 2025-12-09 |
| `hivemq-edge-module-bacnetip` | master | `b3458e0d5eee7fab3dddbc6cc1730e8727eb3027` | 2025-11-18 |

**Note:** Re-run analysis if commits have changed significantly.

---

## Table of Contents

1. [Analysis Methodology](#1-analysis-methodology)
2. [JSON Schema Generation Analysis](#2-json-schema-generation-analysis)
3. [UI Schema Analysis](#3-ui-schema-analysis)
4. [User-Facing Content Analysis](#4-user-facing-content-analysis)
5. [Per-Adapter Analysis](#5-per-adapter-analysis)
6. [External Module Structure Audit](#6-external-module-structure-audit)
7. [Property Dependencies Analysis](#7-property-dependencies-analysis)
8. [Summary & Recommendations](#8-summary--recommendations)

---

## 1. Analysis Methodology

### Backend Schema Generation

The HiveMQ Edge backend generates JSON Schema from Java annotations:

```java
@JsonProperty(value = "fieldName", required = true)
@ModuleConfigField(
    title = "Field Title",
    description = "Field description",
    format = ModuleConfigField.FieldType.IDENTIFIER,
    required = true,
    defaultValue = "default",
    numberMin = 0,
    numberMax = 100,
    stringMinLength = 1,
    stringMaxLength = 1024,
    stringPattern = "^[a-zA-Z0-9]*$"
)
private String fieldName;
```

### Analysis Checklist

#### A. JSON Schema Consistency

- [ ] Property types match Java types
- [ ] `required` array matches `required = true` annotations
- [ ] `minimum`/`maximum` match `numberMin`/`numberMax`
- [ ] `minLength`/`maxLength` match `stringMinLength`/`stringMaxLength`
- [ ] `pattern` matches `stringPattern`
- [ ] `default` matches `defaultValue`
- [ ] `enum` values match Java enum types
- [ ] Nested objects properly defined

#### B. JSON Schema Completeness

- [ ] All `@ModuleConfigField` properties exported to schema
- [ ] All format types properly converted
- [ ] All validations represented (regex, ranges, etc.)
- [ ] `writeOnly` flag set for `JsonProperty.Access.WRITE_ONLY`

#### C. Error Handling Compatibility

- [ ] Backend validation errors map to JSON Schema constraints
- [ ] Error messages are user-friendly
- [ ] Error paths match schema property paths

#### D. UI Schema Correctness

- [ ] Appropriate widgets for data types
- [ ] Correct tab organization
- [ ] Field ordering makes sense
- [ ] Batch mode for arrays
- [ ] Collapsible items configured

#### E. User-Facing Content

- [ ] All fields have `title` (US-EN)
- [ ] All fields have `description` (US-EN)
- [ ] No internal/technical names exposed
- [ ] Consistent terminology
- [ ] i18n readiness (externalized strings)

---

## 2. JSON Schema Generation Analysis

### 2.1 @ModuleConfigField to JSON Schema Mapping

| Java Annotation     | JSON Schema Property  | Type Applicability |
| ------------------- | --------------------- | ------------------ |
| `title`             | `title`               | All                |
| `description`       | `description`         | All                |
| `required`          | Added to `required[]` | All                |
| `defaultValue`      | `default`             | All                |
| `format`            | `format`              | All                |
| `numberMin`         | `minimum`             | number, integer    |
| `numberMax`         | `maximum`             | number, integer    |
| `stringMinLength`   | `minLength`           | string             |
| `stringMaxLength`   | `maxLength`           | string             |
| `stringPattern`     | `pattern`             | string             |
| `enumDisplayValues` | `enumNames`           | enum               |

### 2.2 Format Type Mapping

| ModuleConfigField.FieldType | JSON Schema format | Widget          |
| --------------------------- | ------------------ | --------------- |
| `IDENTIFIER`                | `identifier`       | text input      |
| `HOSTNAME`                  | `hostname`         | text input      |
| `BOOLEAN`                   | `boolean`          | checkbox/toggle |
| `URI`                       | `uri`              | text input      |
| `PASSWORD`                  | N/A                | password widget |

### 2.3 Known Issues: Type Constraint Mismatches

#### Issue: String Constraints on Integer Fields

**Affected Adapter:** Databases

**Backend Code (`DatabasesAdapterConfig.java`):**

```java
@JsonProperty(value = "port", required = true)
@ModuleConfigField(title = "Port",
    description = "Server port...",
    required = true,
    stringPattern = ID_REGEX,     // ❌ Invalid for Integer
    stringMinLength = 1,          // ❌ Invalid for Integer
    stringMaxLength = 6,          // ❌ Invalid for Integer
    defaultValue = "5432")
protected @NotNull Integer port;
```

**Generated Schema (INCORRECT):**

```json
{
  "port": {
    "type": "integer",
    "minLength": 1,
    "maxLength": 6,
    "pattern": "^([a-zA-Z_0-9-_])*$"
  }
}
```

**Should Be:**

```json
{
  "port": {
    "type": "integer",
    "minimum": 1,
    "maximum": 65535,
    "default": 5432
  }
}
```

**Impact:**

- JSON Schema validators may ignore invalid constraints
- RJSF will not validate correctly
- User may enter invalid port values

**Recommendation:** Use `numberMin`/`numberMax` for numeric fields

---

### 2.4 Consistency Check: Required Fields

#### Pattern Analysis

Each adapter should have:

1. `@JsonProperty(required = true)` - Jackson serialization
2. `@ModuleConfigField(required = true)` - Schema generation
3. Field added to `required` array in schema

**Potential Issues:**

- If `@JsonProperty(required = true)` but `@ModuleConfigField(required = false)`, the schema won't mark field as required
- Frontend validation passes but backend throws error

#### Verification Checklist by Adapter

| Adapter    | id  | host | port | Other Required                             |
| ---------- | --- | ---- | ---- | ------------------------------------------ |
| ADS        | ✅  | ✅   | ✅   | sourceAmsNetId, targetAmsNetId, adsToMqtt  |
| Databases  | ✅  | N/A  | ✅   | type, server, database, username, password |
| EIP        | ✅  | ✅   | ✅   | eipToMqtt                                  |
| File       | ✅  | N/A  | N/A  | fileToMqtt                                 |
| HTTP       | ✅  | N/A  | N/A  | None                                       |
| Modbus     | ✅  | ✅   | ✅   | modbusToMqtt                               |
| MTConnect  | ✅  | N/A  | N/A  | pollingIntervalMillis                      |
| OPC-UA     | ✅  | N/A  | N/A  | uri                                        |
| S7         | ✅  | ✅   | ✅   | controllerType, s7ToMqtt                   |
| Simulation | ✅  | N/A  | N/A  | simulationToMqtt                           |
| BACnet/IP  | TBD | TBD  | TBD  | TBD                                        |

---

### 2.5 Enum Validation

#### Pattern: Enum with Display Names

**Backend:**

```java
@ModuleConfigField(
    title = "Content Type",
    enumDisplayValues = {
        "application/octet-stream",
        "text/plain",
        "application/json"
    }
)
private ContentType contentType;
```

**Generated Schema:**

```json
{
  "contentType": {
    "type": "string",
    "enum": ["BINARY", "TEXT", "JSON"],
    "enumNames": ["application/octet-stream", "text/plain", "application/json"]
  }
}
```

#### Check: Enum Count Match

| Adapter   | Field             | enum count | enumNames count | Match |
| --------- | ----------------- | ---------- | --------------- | ----- |
| File      | contentType       | 5          | 5               | ✅    |
| S7        | controllerType    | 6          | 6               | ✅    |
| Modbus    | dataType          | 14         | 14              | ✅    |
| ADS       | dataType          | 29         | 29              | ✅    |
| HTTP      | httpRequestMethod | 3          | 3               | ✅    |
| Databases | type              | 3          | 3               | ✅    |

---

### 2.6 Pattern/Regex Validation

#### Common Patterns Used

| Pattern Name | Regex                                                  | Used By                 |
| ------------ | ------------------------------------------------------ | ----------------------- |
| ID_REGEX     | `^([a-zA-Z_0-9-_])*$`                                  | All adapters (id field) |
| AMS Net ID   | `\d{1,3}\.\d{1,3}\.\d{1,3}\.\d{1,3}\.\d{1,3}\.\d{1,3}` | ADS                     |
| Hostname     | N/A (uses format)                                      | Industrial adapters     |

#### Issues Found

1. **ID_REGEX has redundant characters:** `[a-zA-Z_0-9-_]` - the `_` appears twice

   - Should be: `^[a-zA-Z0-9_-]*$`

2. **Pattern not validated in UI:** RJSF validates patterns, but custom error messages are not defined

---

## 3. UI Schema Analysis

### 3.1 Widget Selection Audit

| Field Type | Expected Widget           | Adapters Using       |
| ---------- | ------------------------- | -------------------- |
| `id`       | text (disabled for edit)  | All                  |
| `port`     | `updown`                  | ADS, EIP, Modbus, S7 |
| `password` | `password`                | Databases, OPC-UA    |
| `host`     | text with hostname format | Industrial adapters  |
| `textarea` | `textarea`                | HTTP (requestBody)   |
| `boolean`  | checkbox                  | Various              |

### 3.2 Missing Widget Specifications

| Adapter   | Field                         | Current | Recommended         | Reason                          |
| --------- | ----------------------------- | ------- | ------------------- | ------------------------------- |
| Databases | port                          | default | `updown`            | Consistency with other adapters |
| MTConnect | pollingIntervalMillis         | default | `updown`            | Numeric with bounds             |
| MTConnect | maxPollingErrorsBeforeRemoval | default | `updown`            | Numeric with bounds             |
| All       | URL fields                    | default | Consider URL widget | URL validation feedback         |

### 3.3 Tab Organization Audit

#### Standard Tab Pattern

Most adapters follow:

1. **Connection** - Core connection settings (id, host, port)
2. **{Protocol} To MQTT** - Northbound mappings
3. **{Device/Protocol}** - Protocol-specific settings

#### Deviations

| Adapter    | Deviation                 | Assessment                       |
| ---------- | ------------------------- | -------------------------------- |
| HTTP       | Has "MQTT to HTTP" tab    | ✅ Correct (bidirectional)       |
| MTConnect  | Only 1 tab with `id` only | ⚠️ Missing tabs for other fields |
| Simulation | No device-specific tab    | ✅ Correct (no device settings)  |

### 3.4 ui:disabled Analysis

The `ui:disabled` property controls whether the `id` field is editable.

| Adapter    | Backend Value | Meaning                   |
| ---------- | ------------- | ------------------------- |
| ADS        | `false`       | ID editable (create mode) |
| Databases  | Not set       | ID editable (default)     |
| EIP        | `false`       | ID editable               |
| File       | `false`       | ID editable               |
| HTTP       | `true`        | ID NOT editable           |
| Modbus     | `true`        | ID NOT editable           |
| MTConnect  | Not set       | ID editable (default)     |
| OPC-UA     | `true`        | ID NOT editable           |
| S7         | `false`       | ID editable               |
| Simulation | N/A           | Unknown                   |

**Question:** Why do some adapters have `ui:disabled: true` and others `false`?

**Hypothesis:** This may be a per-adapter decision, but it should be consistent. The `id` field should:

- Be **editable** during **creation** (user provides ID)
- Be **disabled** during **editing** (ID cannot change)

**Issue:** The UI schema is static - it doesn't differentiate between create and edit modes.

**Recommendation:** Frontend should dynamically set `ui:disabled` based on mode, not rely on static backend value.

---

## 4. User-Facing Content Analysis

### 4.1 Title and Description Audit

RJSF displays text from (in priority order):

1. `uiSchema['ui:title']` / `uiSchema['ui:description']`
2. `jsonSchema.title` / `jsonSchema.description`
3. Property name (fallback)

#### Common Title Issues

| Issue               | Example                                           | Impact                  |
| ------------------- | ------------------------------------------------- | ----------------------- |
| Missing title       | Some nested properties                            | Shows raw property name |
| Technical title     | "pollingIntervalMillis"                           | Not user-friendly       |
| Inconsistent casing | "Max. Polling Errors" vs "Maximum polling errors" | Inconsistent UX         |

### 4.2 Per-Field Content Review

#### ADS Adapter

| Field          | Title                | Description                                                   | Assessment               |
| -------------- | -------------------- | ------------------------------------------------------------- | ------------------------ |
| id             | "Identifier"         | "Unique identifier for this protocol adapter"                 | ✅ Good                  |
| host           | "Host"               | "IP Address or hostname of the device you wish to connect to" | ✅ Good                  |
| port           | "Port"               | "The port number on the device to connect to"                 | ✅ Good                  |
| sourceAmsNetId | "Source Ams Net Id"  | "The AMS Net ID used by HiveMQ Edge"                          | ⚠️ "Ams" should be "AMS" |
| targetAmsNetId | "Target Ams Net Id"  | "The AMS Net ID of the device to connect to"                  | ⚠️ "Ams" should be "AMS" |
| adsToMqtt      | "ADS To MQTT Config" | "The configuration for a data stream from ADS to MQTT"        | ✅ Good                  |

#### Databases Adapter

| Field                         | Title                      | Description                                                             | Assessment                                                       |
| ----------------------------- | -------------------------- | ----------------------------------------------------------------------- | ---------------------------------------------------------------- |
| id                            | "Identifier"               | "Unique identifier for this protocol adapter"                           | ✅ Good                                                          |
| type                          | "Type"                     | "Database type"                                                         | ⚠️ Could be more descriptive                                     |
| server                        | "Server"                   | "Server address"                                                        | ⚠️ Could mention hostname/IP                                     |
| port                          | "Port"                     | "Server port (Default --> PostgreSQL: 5432, MySQL: 3306, MS SQL: 1433)" | ⚠️ Uses "-->" which is informal                                  |
| database                      | "Database"                 | "Database name"                                                         | ✅ Good                                                          |
| username                      | "Username"                 | "Username for the connection to the database"                           | ✅ Good                                                          |
| password                      | "Password"                 | "Password for the connection to the database"                           | ✅ Good                                                          |
| encrypt                       | "Encrypt"                  | "Use TLS to communicate with the remote database"                       | ✅ Good                                                          |
| trustCertificate              | "Trust Certificate"        | "Do you want to trust remote certificate"                               | ⚠️ Missing "?" - sounds like a question                          |
| connectionTimeoutSeconds      | "connectionTimeoutSeconds" | "The timeout for connection establishment to the database."             | ❌ Title is camelCase - should be "Connection Timeout (seconds)" |
| pollingIntervalMillis         | "Polling Interval [ms]"    | "Time in millisecond that this endpoint will be polled"                 | ⚠️ "millisecond" should be "milliseconds"                        |
| maxPollingErrorsBeforeRemoval | "Max. Polling Errors"      | "Max. errors polling the endpoint before the polling daemon is stopped" | ✅ Good                                                          |

#### EIP Adapter

| Field     | Title                        | Description                                                    | Assessment                              |
| --------- | ---------------------------- | -------------------------------------------------------------- | --------------------------------------- |
| id        | "Identifier"                 | "Unique identifier for this protocol adapter"                  | ✅ Good                                 |
| host      | "Host"                       | "IP Address or hostname of the device you wish to connect to"  | ✅ Good                                 |
| port      | "Port"                       | "The port number on the device you wish to connect to"         | ✅ Good                                 |
| backplane | "Backplane"                  | "Backplane device value"                                       | ⚠️ Not very descriptive for non-experts |
| slot      | "Slot"                       | "Slot device value"                                            | ⚠️ Not very descriptive for non-experts |
| eipToMqtt | "Ethernet IP To MQTT Config" | "The configuration for a data stream from Ethernet IP to MQTT" | ✅ Good                                 |

#### File Adapter

| Field                         | Title                   | Description                                                                                        | Assessment                                |
| ----------------------------- | ----------------------- | -------------------------------------------------------------------------------------------------- | ----------------------------------------- |
| id                            | "Identifier"            | "Unique identifier for this protocol adapter"                                                      | ✅ Good                                   |
| fileToMqtt                    | "File To MQTT Config"   | "The configuration for a data stream from File to MQTT"                                            | ✅ Good                                   |
| pollingIntervalMillis         | "Polling Interval [ms]" | "Time in millisecond that this endpoint will be polled"                                            | ⚠️ "millisecond" should be "milliseconds" |
| maxPollingErrorsBeforeRemoval | "Max. Polling Errors"   | "Max. errors polling the endpoint before the polling daemon is stopped (-1 for unlimited retries)" | ✅ Good                                   |

#### HTTP Adapter

| Field                            | Title                           | Description                                                                                                      | Assessment                    |
| -------------------------------- | ------------------------------- | ---------------------------------------------------------------------------------------------------------------- | ----------------------------- |
| id                               | "Identifier"                    | "Unique identifier for this protocol adapter"                                                                    | ✅ Good                       |
| httpConnectTimeoutSeconds        | "HTTP Connection Timeout"       | "Timeout (in seconds) to allow the underlying HTTP connection to be established"                                 | ✅ Good                       |
| allowUntrustedCertificates       | "Allow Untrusted Certificates"  | "Allow the adapter to connect to untrusted SSL sources (for example expired certificates)."                      | ✅ Good                       |
| assertResponseIsJson             | "Assert JSON Response?"         | "Always attempt to parse the body of the response as JSON data, regardless of the Content-Type on the response." | ⚠️ Has "?" in title - unusual |
| httpPublishSuccessStatusCodeOnly | "Publish Only On Success Codes" | "Only publish data when HTTP response code is successful ( 200 - 299 )"                                          | ✅ Good                       |

#### Modbus Adapter

| Field         | Title                   | Description                                                               | Assessment |
| ------------- | ----------------------- | ------------------------------------------------------------------------- | ---------- |
| id            | "Identifier"            | "Unique identifier for this protocol adapter"                             | ✅ Good    |
| host          | "Host"                  | "IP Address or hostname of the device you wish to connect to"             | ✅ Good    |
| port          | "Port"                  | "The port number on the device you wish to connect to"                    | ✅ Good    |
| timeoutMillis | "Timeout"               | "Time (in milliseconds) to await a connection before the client gives up" | ✅ Good    |
| modbusToMqtt  | "Modbus To MQTT Config" | "The configuration for a data stream from Modbus to MQTT"                 | ✅ Good    |

#### MTConnect Adapter

| Field                         | Title                          | Description                                                                                 | Assessment                                |
| ----------------------------- | ------------------------------ | ------------------------------------------------------------------------------------------- | ----------------------------------------- |
| id                            | "Identifier"                   | "Unique identifier for this protocol adapter"                                               | ✅ Good                                   |
| allowUntrustedCertificates    | "Allow Untrusted Certificates" | "Allow the adapter to connect to untrusted SSL sources (for example expired certificates)." | ✅ Good                                   |
| httpConnectTimeoutSeconds     | "HTTP Connection Timeout"      | "Timeout (in seconds) to allow the underlying HTTP connection to be established"            | ✅ Good                                   |
| pollingIntervalMillis         | "Polling Interval [ms]"        | "Time in millisecond that this endpoint will be polled"                                     | ⚠️ "millisecond" should be "milliseconds" |
| maxPollingErrorsBeforeRemoval | "Max. Polling Errors"          | "Max. errors polling the endpoint before the polling daemon is stopped"                     | ✅ Good                                   |

#### S7 Adapter

| Field          | Title               | Description                                                   | Assessment                               |
| -------------- | ------------------- | ------------------------------------------------------------- | ---------------------------------------- |
| id             | "Identifier"        | "Unique identifier for this protocol adapter"                 | ✅ Good                                  |
| host           | "Host"              | "IP Address or hostname of the device you wish to connect to" | ✅ Good                                  |
| port           | "Port"              | "The port number on the device you wish to connect to"        | ✅ Good                                  |
| controllerType | "Controller Type"   | "The type of controller to connect to"                        | ✅ Good                                  |
| remoteRack     | "Remote Rack"       | "Rack value for the remote main CPU"                          | ✅ Good                                  |
| remoteSlot     | "Remote Slot"       | "Slot value for the remote main CPU"                          | ✅ Good                                  |
| remoteTsap     | "Remote TSAP"       | "Remote TSAP value"                                           | ⚠️ Technical term - may need explanation |
| s7ToMqtt       | "S7 To MQTT Config" | "The configuration for a data stream from S7 to MQTT"         | ✅ Good                                  |

#### Simulation Adapter

| Field            | Title                          | Description                                                               | Assessment                  |
| ---------------- | ------------------------------ | ------------------------------------------------------------------------- | --------------------------- |
| id               | "Identifier"                   | "Unique identifier for this protocol adapter"                             | ✅ Good                     |
| minValue         | "Min. Generated Value"         | "Minimum value of the generated decimal number"                           | ✅ Good                     |
| maxValue         | "Max. Generated Value (Excl.)" | "Maximum value of the generated decimal number (excluded)"                | ✅ Good                     |
| minDelay         | "Minimum of delay"             | "Minimum of artificial delay before the polling method generates a value" | ⚠️ Grammar: "Minimum delay" |
| maxDelay         | "Maximum of delay"             | "Maximum of artificial delay before the polling method generates a value" | ⚠️ Grammar: "Maximum delay" |
| simulationToMqtt | "simulationToMqtt"             | "Define Simulations to create MQTT messages."                             | ❌ Title is camelCase       |

### 4.3 Consistency Issues Summary

| Issue Type                | Count | Examples                                   |
| ------------------------- | ----- | ------------------------------------------ |
| CamelCase titles          | 2     | connectionTimeoutSeconds, simulationToMqtt |
| Inconsistent abbreviation | 2     | "Ams" vs "AMS", "ms" vs "milliseconds"     |
| Grammar issues            | 3     | "millisecond" singular, "Minimum of delay" |
| Informal language         | 1     | "-->" in description                       |
| Questions in titles       | 1     | "Assert JSON Response?"                    |
| Missing descriptions      | 0     | All fields have descriptions               |

### 4.4 Internationalization (i18n) Analysis

**Current State:** All titles and descriptions are hardcoded in Java annotations in US-EN.

**i18n Readiness:** ❌ Not ready

**What would be needed:**

1. Extract strings to properties files
2. Use resource bundle keys in annotations
3. Frontend would need to load translations

**Recommendation:** Low priority unless product requires multi-language support.

---

## 5. Per-Adapter Analysis

### 5.1 ADS Adapter

**Overall Assessment:** ✅ Good

| Aspect          | Status | Notes                  |
| --------------- | ------ | ---------------------- |
| JSON Schema     | ✅     | Properly structured    |
| UI Schema       | ✅     | Correct tabs, widgets  |
| Titles          | ⚠️     | "Ams" should be "AMS"  |
| Descriptions    | ✅     | Clear and helpful      |
| Required fields | ✅     | Properly marked        |
| Validation      | ✅     | Pattern for AMS Net ID |

### 5.2 Databases Adapter

**Overall Assessment:** ❌ Has Issues

| Aspect          | Status | Notes                                             |
| --------------- | ------ | ------------------------------------------------- |
| JSON Schema     | ❌     | **BUG:** String constraints on Integer port field |
| UI Schema       | ⚠️     | Missing port widget, no id disabled               |
| Titles          | ❌     | connectionTimeoutSeconds is camelCase             |
| Descriptions    | ⚠️     | "-->" informal, grammar issues                    |
| Required fields | ✅     | Properly marked                                   |
| Validation      | ❌     | Port validation broken                            |

### 5.3 EIP Adapter

**Overall Assessment:** ✅ Good

| Aspect          | Status | Notes                                |
| --------------- | ------ | ------------------------------------ |
| JSON Schema     | ✅     | Properly structured                  |
| UI Schema       | ✅     | Correct tabs, widgets                |
| Titles          | ⚠️     | "Backplane", "Slot" could be clearer |
| Descriptions    | ⚠️     | Not descriptive for non-experts      |
| Required fields | ✅     | Properly marked                      |
| Validation      | ✅     | Port has correct bounds              |

### 5.4 File Adapter

**Overall Assessment:** ⚠️ Partial Issues

| Aspect          | Status | Notes                                 |
| --------------- | ------ | ------------------------------------- |
| JSON Schema     | ✅     | Properly structured                   |
| UI Schema       | ✅     | Correct tabs                          |
| Titles          | ✅     | Good                                  |
| Descriptions    | ⚠️     | "millisecond" singular                |
| Required fields | ✅     | Properly marked                       |
| Validation      | ✅     | Correct                               |
| **Tag Schema**  | ❌     | **Frontend mock is completely wrong** |

### 5.5 HTTP Adapter

**Overall Assessment:** ✅ Good

| Aspect          | Status | Notes                           |
| --------------- | ------ | ------------------------------- |
| JSON Schema     | ✅     | Properly structured             |
| UI Schema       | ✅     | Correct tabs, widgets           |
| Titles          | ⚠️     | "Assert JSON Response?" has "?" |
| Descriptions    | ✅     | Clear and helpful               |
| Required fields | ✅     | Properly marked                 |
| Validation      | ✅     | Correct                         |

### 5.6 Modbus Adapter

**Overall Assessment:** ✅ Good

| Aspect          | Status | Notes                 |
| --------------- | ------ | --------------------- |
| JSON Schema     | ✅     | Properly structured   |
| UI Schema       | ✅     | Correct tabs, widgets |
| Titles          | ✅     | Good                  |
| Descriptions    | ✅     | Clear and helpful     |
| Required fields | ✅     | Properly marked       |
| Validation      | ✅     | Correct port bounds   |

### 5.7 MTConnect Adapter

**Overall Assessment:** ⚠️ Needs Improvement

| Aspect          | Status | Notes                           |
| --------------- | ------ | ------------------------------- |
| JSON Schema     | ✅     | Properly structured             |
| UI Schema       | ⚠️     | **Minimal** - only `id` in tabs |
| Titles          | ✅     | Good                            |
| Descriptions    | ⚠️     | "millisecond" singular          |
| Required fields | ✅     | Properly marked                 |
| Validation      | ✅     | Correct                         |

### 5.8 OPC-UA Adapter

**Overall Assessment:** ✅ Good - Complex but Well Structured

**Module Location:** `modules/hivemq-edge-module-opcua`

| Aspect          | Status | Notes                                        |
| --------------- | ------ | -------------------------------------------- |
| JSON Schema     | ✅     | Complex nested structure, properly annotated |
| UI Schema       | ✅     | Loaded from classpath, well organized        |
| Titles          | ✅     | Good, clear and descriptive                  |
| Descriptions    | ✅     | Comprehensive and helpful                    |
| Required fields | ✅     | Properly marked                              |
| Validation      | ✅     | Correct numeric bounds on timeouts           |
| Tag Schema      | ✅     | Simple - single `node` field                 |

#### Main Config Fields (`OpcUaSpecificAdapterConfig.java`)

| Field             | Type    | Title                         | Constraints           | Assessment |
| ----------------- | ------- | ----------------------------- | --------------------- | ---------- |
| id                | string  | "Identifier"                  | pattern, min/max      | ✅ Good    |
| uri               | string  | "OPC UA Server URI"           | format: URI, required | ✅ Good    |
| overrideUri       | boolean | "Override server returned..." | default: false        | ✅ Good    |
| applicationUri    | string  | "Application URI Override"    | optional              | ✅ Good    |
| auth              | object  | "Authentication Config..."    | optional              | ✅ Good    |
| tls               | object  | "TLS Configuration"           | has nested fields     | ✅ Good    |
| security          | object  | "Message Security Config..."  | has nested fields     | ✅ Good    |
| opcuaToMqtt       | object  | "OPC UA To MQTT Config"       | required              | ✅ Good    |
| connectionOptions | object  | "Options for connection..."   | has nested fields     | ✅ Good    |

#### Nested Config: TLS (`Tls.java`)

| Field      | Type    | Title                    | Default  | Assessment                      |
| ---------- | ------- | ------------------------ | -------- | ------------------------------- |
| enabled    | boolean | "Enable TLS"             | false    | ✅ Good                         |
| tlsChecks  | enum    | "Certificate validation" | STANDARD | ✅ Good                         |
| keystore   | object  | "Keystore"               | optional | ⚠️ Should be conditional on TLS |
| truststore | object  | "Truststore"             | optional | ⚠️ Should be conditional on TLS |

**Issue:** `keystore` and `truststore` shown even when `tls.enabled = false`. Should use JSON Schema `dependencies`.

#### Nested Config: Security (`Security.java`)

| Field               | Type | Title                 | Default | Assessment |
| ------------------- | ---- | --------------------- | ------- | ---------- |
| policy              | enum | "OPC UA security..."  | NONE    | ✅ Good    |
| messageSecurityMode | enum | "Message Security..." | NONE    | ✅ Good    |

#### Nested Config: Auth (`Auth.java`)

| Field | Type   | Title                  | Assessment |
| ----- | ------ | ---------------------- | ---------- |
| basic | object | "Basic Authentication" | ✅ Good    |
| x509  | object | "X509 Authentication"  | ✅ Good    |

#### Nested Config: ConnectionOptions (`ConnectionOptions.java`)

| Field                    | Type    | Title                          | Constraints            | Assessment |
| ------------------------ | ------- | ------------------------------ | ---------------------- | ---------- |
| sessionTimeoutMs         | long    | "Session Timeout (ms)"         | min:10000, max:3600000 | ✅ Good    |
| requestTimeoutMs         | long    | "Request Timeout (ms)"         | min:5000, max:300000   | ✅ Good    |
| keepAliveIntervalMs      | long    | "Keep-Alive Interval (ms)"     | min:1000, max:60000    | ✅ Good    |
| keepAliveFailuresAllowed | integer | "Keep-Alive Failures Allowed"  | min:1, max:10          | ✅ Good    |
| connectionTimeoutMs      | long    | "Connection Timeout (ms)"      | min:2000, max:300000   | ✅ Good    |
| healthCheckIntervalMs    | long    | "Health Check Interval (ms)"   | min:10000, max:300000  | ✅ Good    |
| retryIntervalMs          | long    | "Connection Retry Interval..." | min:5000, max:300000   | ✅ Good    |
| autoReconnect            | boolean | "Automatic Reconnection"       | default: true          | ✅ Good    |
| reconnectOnServiceFault  | boolean | "Reconnect on Service Fault"   | default: true          | ✅ Good    |

#### Tag Definition (`OpcuaTagDefinition.java`)

| Field | Type   | Title                 | Assessment                                |
| ----- | ------ | --------------------- | ----------------------------------------- |
| node  | string | "Destination Node ID" | ✅ Good - includes example in description |

#### UI Schema Analysis (`opcua-adapter-ui-schema.json`)

**Tab Structure:**

- **Connection:** id, uri, overrideUri, applicationUri, security, tls, auth
- **OPC UA to MQTT:** opcuaToMqtt
- **Connection Options:** connectionOptions

**Widget/Ordering Assignments:**

- ✅ `id.ui:disabled: true` (edit mode)
- ✅ Nested ordering for `auth.basic`, `auth.x509`
- ✅ Nested ordering for `security`
- ✅ Nested ordering for `tls` with keystore/truststore
- ✅ `tlsChecks` has `ui:enumNames` with descriptive explanations

**Notable Implementation Features:**

- Uses Java record classes (`Tls`, `Security`, `Auth`, `ConnectionOptions`)
- Custom deserializers for `Auth` and `Security` (backwards compatibility)
- Comprehensive timeout/reconnection options
- Full TLS/X509 certificate support

#### Issues Found

1. **Missing TLS dependency:** `tls.keystore` and `tls.truststore` shown regardless of `tls.enabled` value
2. **No cross-field validation:** Security policy should influence available message security modes

#### Recommendations

Add `dependencies` in JSON Schema for TLS:

```json
{
  "tls": {
    "dependencies": {
      "enabled": {
        "oneOf": [
          { "properties": { "enabled": { "const": false } } },
          {
            "properties": {
              "enabled": { "const": true },
              "keystore": { "type": "object" },
              "truststore": { "type": "object" }
            }
          }
        ]
      }
    }
  }
}
```

---

### 5.9 S7 Adapter

**Overall Assessment:** ✅ Good

| Aspect          | Status | Notes                      |
| --------------- | ------ | -------------------------- |
| JSON Schema     | ✅     | Properly structured        |
| UI Schema       | ✅     | Correct tabs, widgets      |
| Titles          | ⚠️     | "Remote TSAP" is technical |
| Descriptions    | ✅     | Clear and helpful          |
| Required fields | ✅     | Properly marked            |
| Validation      | ✅     | Correct                    |

### 5.10 Simulation Adapter

**Overall Assessment:** ⚠️ Minor Issues

| Aspect          | Status | Notes                           |
| --------------- | ------ | ------------------------------- |
| JSON Schema     | ✅     | Properly structured             |
| UI Schema       | ⚠️     | Unable to verify (no JSON file) |
| Titles          | ❌     | "simulationToMqtt" is camelCase |
| Descriptions    | ⚠️     | "Minimum of delay" grammar      |
| Required fields | ✅     | Properly marked                 |
| Validation      | ✅     | Correct                         |

### 5.11 BACnet/IP Adapter

**Overall Assessment:** ✅ Good (External Module)

**Module Location:** `/Users/nicolas/IdeaProjects/hivemq-edge-module-bacnetip/` (Separate repository)

| Aspect          | Status | Notes                                              |
| --------------- | ------ | -------------------------------------------------- |
| JSON Schema     | ✅     | Properly structured, correct constraints           |
| UI Schema       | ✅     | Correct tabs, widgets (`updown` for port/deviceId) |
| Titles          | ✅     | Good, properly capitalized                         |
| Descriptions    | ⚠️     | Minor: "from ADS to MQTT" copy-paste error         |
| Required fields | ✅     | Properly marked                                    |
| Validation      | ✅     | Correct numeric bounds                             |
| Tag Schema      | ✅     | Complete with 4 fields                             |

#### Config Fields

| Field                   | Type    | Title                      | Constraints         | Assessment           |
| ----------------------- | ------- | -------------------------- | ------------------- | -------------------- |
| id                      | string  | "Identifier"               | pattern, min/max    | ✅ Good              |
| host                    | string  | "Host"                     | format: hostname    | ✅ Good              |
| port                    | integer | "Port"                     | min:1, max:65535    | ✅ Good              |
| deviceId                | integer | "Device Id"                | min:1, max:65535    | ✅ Good              |
| subnetBroadcastAddress  | string  | "Subnet Broadcast Address" | format: hostname    | ✅ Good              |
| discoveryIntervalMillis | integer | "Discovery Interval [ms]"  | min:1, default:5000 | ✅ Good              |
| bacnetipToMqtt          | object  | "BACnet/IP To MQTT Config" | -                   | ⚠️ Description error |

**Issue Found:** The `bacnetipToMqtt` description says "from ADS to MQTT" - copy-paste error from ADS adapter.

#### Tag Definition Fields

| Field                | Type    | Title                    | Assessment |
| -------------------- | ------- | ------------------------ | ---------- |
| deviceInstanceNumber | integer | "Device Instance Number" | ✅ Good    |
| objectInstanceNumber | integer | "Object Instance Number" | ✅ Good    |
| objectType           | enum    | "Object Type"            | ✅ Good    |
| propertyType         | enum    | "Property Value"         | ✅ Good    |

#### UI Schema Analysis

- ✅ Uses `updown` widget for `port` and `deviceId`
- ✅ Has proper tab organization (Settings, BACnet/IP to MQTT)
- ✅ `id.ui:disabled: false` (consistent with create mode)
- ✅ Batch mode for mappings
- ✅ Collapsible items with `titleKey: mqttTopic`

---

## 6. External Module Structure Audit

### 6.1 Overview

Some protocol adapters are developed in separate repositories outside the main HiveMQ Edge monorepo. This section audits whether these external modules follow the same structural patterns as internal modules.

### 6.2 Module Locations

| Adapter    | Location Type   | Repository/Path                                   |
| ---------- | --------------- | ------------------------------------------------- |
| ADS        | Internal        | `modules/hivemq-edge-module-plc4x`                |
| BACnet/IP  | **External**    | `hivemq-edge-module-bacnetip` (separate repo)     |
| Databases  | Internal        | `modules/hivemq-edge-module-databases`            |
| EIP        | Internal        | `modules/hivemq-edge-module-etherip`              |
| File       | Internal        | `modules/hivemq-edge-module-file`                 |
| HTTP       | Internal        | `modules/hivemq-edge-module-http`                 |
| Modbus     | Internal        | `modules/hivemq-edge-module-modbus`               |
| MTConnect  | Internal        | `modules/hivemq-edge-module-mtconnect`            |
| OPC-UA     | Internal        | `modules/hivemq-edge-module-opcua`                |
| S7         | Internal        | `modules/hivemq-edge-module-plc4x`                |
| Simulation | Internal (core) | `hivemq-edge/src/.../modules/adapters/simulation` |

### 6.3 External Module: BACnet/IP Structure Audit

**Repository:** `/Users/nicolas/IdeaProjects/hivemq-edge-module-bacnetip/`

#### Directory Structure Comparison

| Expected Pattern (Internal)           | BACnet/IP External                                           | Match |
| ------------------------------------- | ------------------------------------------------------------ | ----- |
| `src/main/java/.../config/`           | ✅ `src/main/java/com/hivemq/edge/adapters/bacnetip/config/` | ✅    |
| `src/main/java/.../config/tag/`       | ✅ `src/main/java/.../config/tag/`                           | ✅    |
| `src/main/resources/*-ui-schema.json` | ✅ `src/main/resources/bacnet-adapter-ui-schema.json`        | ✅    |
| `*ProtocolAdapterInformation.java`    | ✅ `BacnetipProtocolAdapterInformation.java`                 | ✅    |
| `*SpecificAdapterConfig.java`         | ✅ `BacnetipSpecificAdapterConfig.java`                      | ✅    |
| `*TagDefinition.java`                 | ✅ `BacnetTagDefinition.java`                                | ✅    |
| `*ProtocolAdapterFactory.java`        | ✅ `BacnetipProtocolAdapterFactory.java`                     | ✅    |
| `*ProtocolAdapter.java`               | ✅ `BacnetipProtocolAdapter.java`                            | ✅    |

**Result:** ✅ **Structure matches internal modules**

#### Annotation Pattern Comparison

| Pattern                                           | Internal Modules | BACnet/IP | Match |
| ------------------------------------------------- | ---------------- | --------- | ----- |
| `@JsonProperty` with `required`                   | ✅ Used          | ✅ Used   | ✅    |
| `@JsonProperty` with `access = WRITE_ONLY` for id | ✅ Used          | ✅ Used   | ✅    |
| `@ModuleConfigField` with `title`                 | ✅ Used          | ✅ Used   | ✅    |
| `@ModuleConfigField` with `description`           | ✅ Used          | ✅ Used   | ✅    |
| `@ModuleConfigField` with `format`                | ✅ Used          | ✅ Used   | ✅    |
| `@ModuleConfigField` with `numberMin/Max`         | ✅ Used          | ✅ Used   | ✅    |
| `@ModuleConfigField` with `stringPattern`         | ✅ Used          | ✅ Used   | ✅    |
| `@JsonCreator` constructor                        | ✅ Used          | ✅ Used   | ✅    |

**Result:** ✅ **Annotation patterns match internal modules**

#### UI Schema Pattern Comparison

| Pattern                          | Internal Modules | BACnet/IP  | Match |
| -------------------------------- | ---------------- | ---------- | ----- |
| `ui:tabs` array                  | ✅               | ✅         | ✅    |
| `ui:order` array                 | ✅               | ✅         | ✅    |
| `id.ui:disabled`                 | ✅               | ✅ `false` | ✅    |
| `port.ui:widget: updown`         | ✅ (most)        | ✅         | ✅    |
| `ui:batchMode` for mappings      | ✅               | ✅         | ✅    |
| `ui:collapsable` with `titleKey` | ✅               | ✅         | ✅    |

**Result:** ✅ **UI schema patterns match internal modules**

#### Issues Found in External Module

| Issue            | Severity | Description                                                             |
| ---------------- | -------- | ----------------------------------------------------------------------- |
| Description typo | 🟡 Low   | `bacnetipToMqtt` description says "from ADS to MQTT" (copy-paste error) |

### 6.4 Recommendations for External Modules

When developing adapters in external repositories:

1. **Follow the same directory structure** as internal modules
2. **Use identical annotation patterns** (`@JsonProperty`, `@ModuleConfigField`)
3. **Include UI schema JSON file** in `src/main/resources/`
4. **Implement `ProtocolAdapterInformation`** with `getUiSchema()` loading from resources
5. **Use consistent naming conventions** (`*SpecificAdapterConfig`, `*TagDefinition`)
6. **Run schema validation** to ensure JSON Schema is correctly generated

### 6.5 External Module Checklist

For external adapter modules, verify:

- [ ] Directory structure matches internal modules
- [ ] All `@ModuleConfigField` annotations present and correct
- [ ] UI schema JSON file exists in resources
- [ ] `getUiSchema()` method loads from classpath
- [ ] Tag definition class follows pattern
- [ ] No copy-paste errors in descriptions
- [ ] Numeric fields use `numberMin`/`numberMax` (not string constraints)
- [ ] Category is appropriate for the adapter type

---

## 7. Property Dependencies Analysis

### 7.1 What Are Property Dependencies?

JSON Schema supports conditional rendering through:

1. **`dependencies`** - Show/require fields when another field has a value
2. **`if/then/else`** - Conditional schema based on field values
3. **`oneOf/anyOf`** - Alternative schemas based on selection

RJSF renders these as conditionally visible/required fields.

**Example:**

```json
{
  "properties": {
    "enableTls": { "type": "boolean" },
    "certificate": { "type": "string" }
  },
  "dependencies": {
    "enableTls": {
      "oneOf": [
        {
          "properties": { "enableTls": { "const": false } }
        },
        {
          "properties": {
            "enableTls": { "const": true },
            "certificate": { "type": "string" }
          },
          "required": ["certificate"]
        }
      ]
    }
  }
}
```

### 7.2 Identified Dependencies in Backend Code

#### Databases Adapter: encrypt → trustCertificate

**Code Pattern (`DatabaseConnection.java`):**

```java
case MSSQL -> {
    // ...
    if (encrypt) {
        properties.setProperty("encrypt", "true");
        properties.setProperty("trustServerCertificate", "true");
    } else {
        properties.setProperty("encrypt", "false");
    }
}
```

**Dependency Logic:**

- `trustCertificate` is only meaningful when `encrypt = true`
- When `encrypt = false`, `trustCertificate` has no effect

**Current Schema:** No dependency defined

**Should Have:**

```json
{
  "dependencies": {
    "encrypt": {
      "oneOf": [
        {
          "properties": { "encrypt": { "const": false } }
        },
        {
          "properties": {
            "encrypt": { "const": true },
            "trustCertificate": {
              "type": "boolean",
              "title": "Trust Server Certificate",
              "description": "Trust the server certificate without validation"
            }
          }
        }
      ]
    }
  }
}
```

**Impact:** User sees `trustCertificate` even when `encrypt` is false (confusing)

---

#### Databases Adapter: type → port (Default Value)

**Code Pattern:**

- PostgreSQL default port: 5432
- MySQL default port: 3306
- MS SQL default port: 1433

**Dependency Logic:**

- The default port should change based on database `type`

**Current Schema:** Static default `5432`

**Should Have:** Dynamic default or at least help text per type

**Possible Implementation:**

```json
{
  "if": {
    "properties": { "type": { "const": "POSTGRESQL" } }
  },
  "then": {
    "properties": { "port": { "default": 5432 } }
  }
}
```

**Note:** JSON Schema `if/then` with `default` has limited RJSF support. Alternative: Clear description mentioning all defaults.

---

#### OPC-UA Adapter: TLS Configuration

**Expected Structure:**

```
tls:
  enabled: boolean          ← Master toggle
  tlsChecks: enum           ← Only if enabled
  keystore: object          ← Only if enabled
  truststore: object        ← Only if enabled
```

**Dependency Logic:**

- `tlsChecks`, `keystore`, `truststore` only relevant when `tls.enabled = true`

**Current Schema:** Unknown (UI schema loaded from classpath)

**Should Have:**

```json
{
  "properties": {
    "tls": {
      "type": "object",
      "properties": {
        "enabled": { "type": "boolean", "default": false }
      },
      "dependencies": {
        "enabled": {
          "oneOf": [
            { "properties": { "enabled": { "const": false } } },
            {
              "properties": {
                "enabled": { "const": true },
                "tlsChecks": { "type": "string", "enum": ["STANDARD", "LOOSE"] },
                "keystore": { "$ref": "#/definitions/Keystore" },
                "truststore": { "$ref": "#/definitions/Truststore" }
              }
            }
          ]
        }
      }
    }
  }
}
```

---

#### OPC-UA Adapter: Security → Authentication

**Expected Dependencies:**

- If `security.secPolicy` ≠ `NONE`, authentication options become relevant
- If `security.msgSecurityMode` requires signing/encryption, certificates needed

**Status:** Needs detailed analysis of OPC-UA config classes

---

#### HTTP Adapter: httpToMqtt ↔ mqttToHttp

**Logic:**

- Adapter supports bidirectional communication
- At least one direction should be configured

**Current Schema:** Both are optional objects

**Potential Enhancement:**

```json
{
  "anyOf": [{ "required": ["httpToMqtt"] }, { "required": ["mqttToHttp"] }]
}
```

**Note:** This would require at least one direction to be configured.

---

#### S7 Adapter: controllerType → remoteRack/remoteSlot/remoteTsap

**Logic:**

- Different controller types may have different slot/rack requirements
- Some types use TSAP, others use rack/slot

**Current Schema:** All fields always visible

**Potential Enhancement:** Hide TSAP or rack/slot based on controller type

---

#### Modbus Adapter: addressRange Fields

**Expected Dependencies in Tag Schema:**

- `startIdx` and `endIdx` define a range
- `endIdx` should be ≥ `startIdx`

**Current Schema:** No cross-field validation

**Impact:** User can enter invalid range (startIdx > endIdx)

---

### 7.3 Error Processing Analysis

#### Backend Validation Patterns

**Pattern 1: Constructor Validation**

```java
@JsonCreator
public ModbusSpecificAdapterConfig(
    @JsonProperty(value = "port", required = true) final int port,
    @JsonProperty(value = "host", required = true) final @NotNull String host,
    ...
) {
    this.port = port;  // No explicit validation
    this.host = host;
    this.timeoutMillis = Objects.requireNonNullElse(timeoutMillis, 5000);
}
```

**Issue:** No range validation in constructor for `port`

---

**Pattern 2: Getter Validation**

```java
public @NotNull Boolean getTrustCertificate() {
    return encrypt;  // BUG: Returns wrong field!
}
```

**Found in:** `DatabasesAdapterConfig.java` - `getTrustCertificate()` returns `encrypt` instead of `trustCertificate`

**Impact:** Backend logic using this getter will get wrong value

---

**Pattern 3: Default Value Handling**

```java
this.timeoutMillis = Objects.requireNonNullElse(timeoutMillis, 5000);
```

**Good Pattern:** Provides fallback for null values

---

#### Error Message Analysis

**Current State:** Backend throws standard Jackson/validation exceptions

**Issues:**

1. Error messages reference Java field names (e.g., "pollingIntervalMillis")
2. No custom user-friendly error messages
3. Error paths may not match frontend form paths

**Example Backend Error:**

```
Validation failed for field 'pollingIntervalMillis': must be greater than 0
```

**Better Error:**

```
Polling Interval must be at least 1 millisecond
```

---

### 7.4 Cross-Field Validation Missing

| Adapter    | Fields                                               | Validation Needed                  | Current   |
| ---------- | ---------------------------------------------------- | ---------------------------------- | --------- |
| Databases  | encrypt, trustCertificate                            | trustCertificate only with encrypt | ❌ None   |
| Databases  | type, port                                           | Port default matches type          | ❌ Static |
| Modbus     | startIdx, endIdx                                     | endIdx ≥ startIdx                  | ❌ None   |
| Simulation | minValue, maxValue                                   | maxValue > minValue                | ❌ None   |
| HTTP       | httpToMqtt, mqttToHttp                               | At least one configured            | ❌ None   |
| All        | pollingIntervalMillis, maxPollingErrorsBeforeRemoval | Related timing logic               | ❌ None   |

---

### 7.5 Recommendations for Dependencies

#### High Priority

| Adapter   | Dependency                   | Implementation                      |
| --------- | ---------------------------- | ----------------------------------- |
| Databases | encrypt → trustCertificate   | Add `dependencies` with `oneOf`     |
| OPC-UA    | tls.enabled → tls sub-fields | Add `dependencies` in nested object |

#### Medium Priority

| Adapter    | Dependency              | Implementation                    |
| ---------- | ----------------------- | --------------------------------- |
| Modbus     | addressRange validation | Add custom validator or `if/then` |
| Simulation | minValue < maxValue     | Add cross-field validation        |

#### Implementation Pattern for RJSF

```json
{
  "dependencies": {
    "enableFeature": {
      "oneOf": [
        {
          "properties": {
            "enableFeature": { "const": false }
          }
        },
        {
          "properties": {
            "enableFeature": { "const": true },
            "featureConfig": {
              "type": "object",
              "properties": {
                "option1": { "type": "string" },
                "option2": { "type": "number" }
              },
              "required": ["option1"]
            }
          },
          "required": ["featureConfig"]
        }
      ]
    }
  }
}
```

---

### 7.6 Backend Bug Found

**File:** `DatabasesAdapterConfig.java`

**Method:** `getTrustCertificate()`

```java
public @NotNull Boolean getTrustCertificate() {
    return encrypt;  // ❌ BUG: Returns wrong field!
}
```

**Should Be:**

```java
public @NotNull Boolean getTrustCertificate() {
    return trustCertificate;  // ✅ Correct
}
```

**Impact:** Any backend code using `getTrustCertificate()` gets the `encrypt` value instead.

---

## 8. Summary & Recommendations

### 8.1 Critical Issues (Must Fix)

| #   | Adapter   | Issue                                       | Fix                                     |
| --- | --------- | ------------------------------------------- | --------------------------------------- |
| 1   | Databases | Port field has invalid string constraints   | Use `numberMin`/`numberMax` instead     |
| 2   | Databases | `getTrustCertificate()` returns wrong field | Return `trustCertificate` not `encrypt` |

### 8.2 High Priority Issues (Should Fix)

| #   | Adapter    | Issue                                         | Fix                                      |
| --- | ---------- | --------------------------------------------- | ---------------------------------------- |
| 3   | Databases  | connectionTimeoutSeconds title is camelCase   | Change to "Connection Timeout (seconds)" |
| 4   | Simulation | simulationToMqtt title is camelCase           | Change to "Simulation To MQTT Config"    |
| 5   | ADS        | "Ams" inconsistent casing                     | Change to "AMS"                          |
| 6   | Databases  | Missing encrypt → trustCertificate dependency | Add JSON Schema `dependencies`           |
| 7   | OPC-UA     | Missing tls.enabled → sub-fields dependency   | Add JSON Schema `dependencies`           |

### 8.3 Medium Priority Issues (Consider Fixing)

| #   | Adapter    | Issue                                                | Fix                         |
| --- | ---------- | ---------------------------------------------------- | --------------------------- |
| 8   | Multiple   | "millisecond" should be "milliseconds"               | Grammar fix                 |
| 9   | Simulation | "Minimum of delay" grammar                           | Change to "Minimum Delay"   |
| 10  | Databases  | "-->" informal in description                        | Use "e.g.," or similar      |
| 11  | HTTP       | "Assert JSON Response?" has "?"                      | Remove question mark        |
| 12  | Databases  | "Trust Certificate" description sounds like question | Reword                      |
| 13  | MTConnect  | UI Schema minimal                                    | Add tabs for other fields   |
| 14  | Modbus     | No addressRange cross-validation                     | Add startIdx ≤ endIdx check |
| 15  | Simulation | No minValue < maxValue validation                    | Add cross-field validation  |

### 8.4 Widget Recommendations

| Adapter   | Field                         | Recommended Widget     | Reason         |
| --------- | ----------------------------- | ---------------------- | -------------- |
| Databases | port                          | `updown`               | Consistency    |
| MTConnect | pollingIntervalMillis         | `updown`               | Numeric bounds |
| MTConnect | maxPollingErrorsBeforeRemoval | `updown`               | Numeric bounds |
| All       | URL fields                    | Consider URL validator | Better UX      |

### 8.5 ui:disabled Recommendation

**Current State:** Inconsistent across adapters (some `true`, some `false`, some not set)

**Recommendation:**

- Remove `ui:disabled` from backend UI schemas
- Frontend should set dynamically based on create vs edit mode
- Create mode: `ui:disabled: false`
- Edit mode: `ui:disabled: true`

### 8.6 Dependency Implementation Recommendations

**For Databases Adapter - encrypt → trustCertificate:**

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

**For OPC-UA Adapter - tls.enabled → sub-fields:**

```json
{
  "tls": {
    "dependencies": {
      "enabled": {
        "oneOf": [
          { "properties": { "enabled": { "const": false } } },
          {
            "properties": {
              "enabled": { "const": true },
              "tlsChecks": { "type": "string" },
              "keystore": { "type": "object" },
              "truststore": { "type": "object" }
            }
          }
        ]
      }
    }
  }
}
```

### 8.7 Checklist for New Adapters

When creating a new adapter, ensure:

- [ ] All `@ModuleConfigField` annotations have `title` and `description`
- [ ] Title is properly capitalized (not camelCase)
- [ ] Description is grammatically correct US-EN
- [ ] Numeric fields use `numberMin`/`numberMax` (not string constraints)
- [ ] String fields with patterns use `stringPattern`
- [ ] Required fields have both `@JsonProperty(required = true)` and `@ModuleConfigField(required = true)`
- [ ] UI schema has appropriate tabs for field grouping
- [ ] UI schema uses `updown` widget for bounded numeric fields
- [ ] UI schema uses `password` widget for sensitive fields
- [ ] Enum fields have matching `enum` and `enumNames` counts
- [ ] **NEW:** Boolean toggle fields have `dependencies` for conditional sub-fields
- [ ] **NEW:** Cross-field validations are documented and implemented
- [ ] **NEW:** Getter methods return the correct field values
