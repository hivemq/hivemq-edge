# Intentionality Analysis: Semantic Mismatch Detection

**Task:** 38658 - Adapter JSON Schema Review
**Date:** December 17, 2025
**Version:** 1.0

---

## Source Code Context

| Repository    | Branch | Commit                                     | Date       |
| ------------- | ------ | ------------------------------------------ | ---------- |
| `hivemq-edge` | master | `55a75111c4e55173dab2332eaf1545ea265fedd7` | 2025-12-16 |

**Note:** Verify issues still exist if commits have changed.

---

## Overview

This analysis identifies properties where the name, title, or description suggests a different data type or structure than what is actually implemented in the schema. These semantic mismatches confuse users and prevent proper validation.

---

## Summary of Findings

| Severity  | Pattern                       | Count | Key Issues                    |
| --------- | ----------------------------- | ----- | ----------------------------- |
| 🔴 High   | Arrays disguised as strings   | 1     | OPC-UA retry intervals        |
| 🟡 Medium | Time units ambiguity          | 3     | Modbus timeout, HTTP timeouts |
| 🟡 Medium | Enums as integers             | 6+    | MQTT QoS across all adapters  |
| 🟢 Low    | Title clarity                 | 2     | Timeout titles without units  |
| ✅ OK     | Structured data as strings    | 0     | OPC-UA Node ID intentional    |
| ✅ OK     | Booleans with negative naming | 0     | None found                    |

---

## 🔴 High Priority Issues

### I-H1. OPC-UA Retry Intervals: Array Disguised as String

**Location:** `modules/hivemq-edge-module-opcua/src/main/java/.../ConnectionOptions.java:74`

**Current Implementation:**

```java
@JsonProperty("retryIntervalMs")
@ModuleConfigField(title = "Connection Retry Intervals (milliseconds)",
           description = "Comma-separated list of backoff delays in milliseconds for connection retry attempts. The adapter will use these delays sequentially for each retry attempt, repeating the last value if attempts exceed the list length.",
           defaultValue = DEFAULT_RETRY_INTERVALS)
String retryIntervalMs,
```

**Problem:**

- Description explicitly says "Comma-separated list"
- Users must manually format `"1000,2000,5000,10000"`
- No validation of individual values
- No proper array UI in RJSF

**Should Be:**

```java
@JsonProperty("retryIntervalMs")
@ModuleConfigField(title = "Connection Retry Intervals (milliseconds)",
           description = "Backoff delays in milliseconds for connection retry attempts...")
List<Long> retryIntervalMs,
```

**Impact:**

- Users can't easily add/remove/reorder retry intervals
- No individual value validation
- Error-prone manual comma formatting

**Recommendation:** Change to `List<Long>` with proper array UI.

---

## 🟡 Medium Priority Issues

### I-M1. Modbus Timeout: Title Missing Unit

**Location:** `modules/hivemq-edge-module-modbus/src/main/java/.../ModbusSpecificAdapterConfig.java:60`

**Current:**

```java
@JsonProperty("timeoutMillis")
@ModuleConfigField(title = "Timeout",  // ❌ No unit in title
           description = "Time (in milliseconds) to await a connection...")
private final int timeoutMillis;
```

**Problem:**

- Property name `timeoutMillis` has unit
- Description mentions "milliseconds"
- But title just says "Timeout" - users see only the title

**Fix:**

```java
@ModuleConfigField(title = "Timeout (ms)",  // ✅ Clear unit
           description = "Time to await a connection before the client gives up")
```

---

### I-M2. HTTP Connection Timeout: Inconsistent Unit Display

**Location:** `modules/hivemq-edge-module-http/src/main/java/.../HttpSpecificAdapterConfig.java:53`

**Current:**

```java
@JsonProperty("httpConnectTimeoutSeconds")
@ModuleConfigField(title = "HTTP Connection Timeout",  // ❌ No unit
           description = "Timeout (in seconds) to allow the underlying HTTP connection to be established")
```

**Analysis:** Property is `httpConnectTimeoutSeconds` and description says "in seconds", but title lacks unit.

**Fix:**

```java
@ModuleConfigField(title = "HTTP Connection Timeout (seconds)",
           description = "Timeout to allow the underlying HTTP connection to be established")
```

---

### I-M3. HTTP Request Timeout: Title Missing Unit

**Locations:**

- `modules/hivemq-edge-module-http/src/main/java/.../MqttToHttpMapping.java:66`
- `modules/hivemq-edge-module-http/src/main/java/.../HttpTagDefinition.java:47`

**Current:**

```java
@ModuleConfigField(title = "Http Request Timeout",  // ❌ What unit?
           ...)
```

**Problem:** Users don't know if this is seconds or milliseconds from the form.

---

### I-M4. MTConnect HTTP Connection Timeout: Missing Unit

**Location:** `modules/hivemq-edge-module-mtconnect/src/main/java/.../MtConnectAdapterConfig.java:45`

**Same pattern:** Title "HTTP Connection Timeout" without unit specification.

---

### I-M5. MQTT QoS: Integer Instead of Enum

**Locations:** All adapters with MQTT mappings:

- `hivemq-edge-module-http/config/http2mqtt/HttpToMqttMapping.java`
- `hivemq-edge-module-modbus/config/ModbusToMqttMapping.java`
- `hivemq-edge-module-file/config/FileToMqttMapping.java`
- `hivemq-edge-module-etherip/config/EipToMqttMapping.java`
- `hivemq-edge-module-plc4x/config/Plc4xToMqttMapping.java`
- `hivemq-edge/modules/adapters/simulation/config/SimulationToMqttMapping.java`

**Current:**

```java
@ModuleConfigField(title = "MQTT QoS",
           description = "MQTT Quality of Service level",
           numberMin = 0,
           numberMax = 2,
           defaultValue = "0")
private final int mqttQos;
```

**Problem:**

- QoS has only 3 valid values: 0, 1, 2
- Users see a number input instead of dropdown
- No semantic meaning displayed (At Most Once, At Least Once, Exactly Once)

**Should Be:**

```java
public enum MqttQos {
    AT_MOST_ONCE(0),
    AT_LEAST_ONCE(1),
    EXACTLY_ONCE(2)
}

@ModuleConfigField(title = "MQTT QoS",
           description = "MQTT Quality of Service level",
           enumDisplayValues = {"At Most Once (0)", "At Least Once (1)", "Exactly Once (2)"})
private final MqttQos mqttQos;
```

**Impact:** Users must know MQTT spec to understand what 0/1/2 mean.

**Note:** This is a widespread pattern - fixing requires changes across all adapters.

---

## ✅ Verified OK (No Issues)

### Structured Data as Strings

**OPC-UA Node ID:**

```java
@ModuleConfigField(title = "Destination Node ID",
           description = "identifier of the node on the OPC UA server. Example: \"ns=3;s=85/0:Temperature\"")
private final @NotNull String node;
```

**Analysis:** This is intentionally a string because:

- OPC-UA node IDs have protocol-specific format (`ns=X;s=Name` or `ns=X;i=123`)
- The format is standardized by OPC-UA spec
- A structured input would not improve UX significantly
- Users familiar with OPC-UA expect this format

**Verdict:** ✅ Intentional - no change needed.

---

### Booleans with Negative Naming

**Search Results:** No instances of `disable*`, `skip*`, `ignore*` boolean fields found in adapter configs.

**Verdict:** ✅ All booleans use positive naming conventions.

---

### Numbers Disguised as Strings

**Search Results:** No instances of numeric fields (port, count, size) implemented as String type.

**Verdict:** ✅ All numeric fields use proper `int`/`Integer`/`Long` types.

---

## Recommendations

### Immediate Actions (Backend)

| ID   | Issue                  | Module | Effort | Impact |
| ---- | ---------------------- | ------ | ------ | ------ |
| I-H1 | OPC-UA retry intervals | opcua  | Medium | High   |
| I-M1 | Modbus timeout title   | modbus | Low    | Medium |
| I-M2 | HTTP timeout title     | http   | Low    | Medium |

### Medium-Term Actions (Backend)

| ID   | Issue            | Scope        | Effort | Impact |
| ---- | ---------------- | ------------ | ------ | ------ |
| I-M5 | MQTT QoS as enum | All adapters | High   | High   |

### Considerations

**When NOT to change:**

- OPC-UA Node IDs should remain strings (protocol requirement)
- Changing types may break backwards compatibility for existing configs
- Some "comma-separated" patterns may be intentional for copy-paste from external systems

**When to prioritize:**

- New adapters should use proper types from the start
- Changes that improve validation (arrays, enums) have higher value
- Title/description fixes are low-risk, high-value improvements

---

## Search Commands Used

```bash
# Find arrays disguised as strings
grep -rn "description.*=" modules/ --include="*.java" | \
  grep -i "comma\|separated\|delimited\|list of"

# Find numbers disguised as strings
grep -rn "private.*String" modules/ --include="*.java" | \
  grep -iE "count|number|size|port|timeout|interval"

# Find time fields without units
grep -rn "title\|description" modules/ --include="*.java" | \
  grep -iE "timeout|interval|delay|duration" | \
  grep -vi "seconds\|milliseconds"

# Find potential missing enums
grep -rn "description.*=" modules/ --include="*.java" | \
  grep -iE "one of|either|allowed values"
```

---

## Cross-Reference

This analysis complements:

- [REMEDIATION_REPORT.md](./REMEDIATION_REPORT.md) - Schema validation issues
- [ENUM_DISPLAY_NAMES_AUDIT.md](./ENUM_DISPLAY_NAMES_AUDIT.md) - Enum display issues
- [ADDITIONAL_ANALYSIS_RECOMMENDATIONS.md](./ADDITIONAL_ANALYSIS_RECOMMENDATIONS.md) - Analysis methodology
