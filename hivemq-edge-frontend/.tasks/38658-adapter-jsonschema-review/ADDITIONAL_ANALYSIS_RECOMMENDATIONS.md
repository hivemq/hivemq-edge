# Additional Analysis Recommendations

**Task:** 38658 - Adapter JSON Schema Review  
**Date:** December 16, 2025

---

## Proposed Additional Analyses

Based on the completed schema analysis and understanding of RJSF rendering, here are recommended additional analyses:

---

## 1. Intentionality Analysis (Semantic Mismatch Detection)

**Purpose:** Identify properties where the name, title, or description suggests a different data type or structure than what is actually implemented in the schema.

**Why it matters:** Users expect properties to behave as described. A property described as "a list of items" but implemented as a comma-separated string forces users into awkward workarounds and prevents proper validation.

### Patterns to Detect

#### Pattern 1: Arrays Disguised as Strings

**Indicators:**

- Description contains: "comma-separated", "delimited", "list of", "multiple values"
- Type is `string` but semantically represents multiple items

**Example Issue:**

```java
@ModuleConfigField(
    title = "Allowed Topics",
    description = "Comma-separated list of MQTT topics"  // ❌ Suggests array
)
private String allowedTopics;  // ❌ Implemented as string
```

**Should be:**

```java
private List<String> allowedTopics;  // ✅ Proper array
```

**Automated Check:**

```bash
grep -rn "description.*=" modules/ --include="*.java" | \
  grep -i "comma\|separated\|delimited\|list of" | \
  grep -v "List<\|array\|Array"
```

#### Pattern 2: Numbers Disguised as Strings

**Indicators:**

- Field name ends with: `Count`, `Number`, `Size`, `Length`, `Port`, `Timeout`
- Description mentions numeric operations or ranges
- Type is `string` instead of `integer`/`number`

**Automated Check:**

```bash
grep -rn "private.*String.*\(Count\|Number\|Size\|Port\|Timeout\)" modules/ --include="*.java"
```

#### Pattern 3: Booleans with Wrong Semantics

**Indicators:**

- Boolean property named negatively (e.g., `disableFeature` instead of `enableFeature`)
- Description uses opposite logic from property name

**Example Issue:**

```java
@ModuleConfigField(title = "Disable Validation")
private boolean disableValidation;  // ❌ Double negative confusion
```

**Should be:**

```java
@ModuleConfigField(title = "Enable Validation")
private boolean enableValidation;  // ✅ Positive logic
```

#### Pattern 4: Time Units Ambiguity

**Indicators:**

- Property name contains time unit (e.g., `timeoutSeconds`) but description mentions different unit
- No unit in property name but description assumes specific unit

**Example Issue:**

```java
@ModuleConfigField(
    title = "Timeout",
    description = "Connection timeout"  // ❌ No unit specified
)
private int timeout;  // Is this seconds? milliseconds?
```

**Should be:**

```java
@ModuleConfigField(
    title = "Timeout (seconds)",
    description = "Connection timeout in seconds"
)
private int timeoutSeconds;  // ✅ Clear unit
```

#### Pattern 5: Enums Disguised as Strings

**Indicators:**

- Description lists specific allowed values
- Type is `string` without `enum` constraint

**Example Issue:**

```java
@ModuleConfigField(
    title = "Protocol",
    description = "Protocol to use: HTTP or HTTPS"  // ❌ Fixed options
)
private String protocol;  // ❌ No validation
```

**Should be:**

```java
private Protocol protocol;  // ✅ Enum with HTTP, HTTPS
```

#### Pattern 6: Structured Data as Flat Strings

**Indicators:**

- Description mentions "format", "pattern", or provides structure example
- Complex data represented as single string

**Example Issue:**

```java
@ModuleConfigField(
    title = "Address",
    description = "Address in format: ns=X;s=NodeName"  // ❌ Structured
)
private String address;
```

**Consideration:** Sometimes flat strings are intentional (e.g., OPC UA node IDs). The question is whether a structured input would improve UX.

### Operationalization

#### Step 1: Keyword Search

```bash
# Find potential array-as-string issues
grep -rn "@ModuleConfigField" modules/ -A3 | \
  grep -i "comma\|separated\|list of\|multiple" | \
  grep "String"

# Find potential number-as-string issues
grep -rn "private.*String" modules/ --include="*.java" | \
  grep -i "count\|number\|size\|port\|timeout\|interval"

# Find potential missing enums
grep -rn "description.*=" modules/ --include="*.java" | \
  grep -i "one of\|either\|or.*or\|allowed values"
```

#### Step 2: Cross-Reference Analysis

For each finding:

1. Check if property type matches semantic intent
2. Check if RJSF would render appropriate widget
3. Check if validation constraints match description
4. Propose schema change if mismatch found

#### Step 3: Document Findings

Create table:

| Adapter | Property | Described As     | Implemented As | Issue           | Recommendation  |
| ------- | -------- | ---------------- | -------------- | --------------- | --------------- |
| X       | topics   | "list of topics" | string         | Array as string | Change to array |

### Implementation Considerations

**When to keep strings:**

- Protocol requires specific string format (e.g., OPC UA node IDs)
- Backwards compatibility concerns
- User familiarity with format

**When to change to proper types:**

- New adapters (no backwards compatibility)
- Clear UX improvement
- Enables better validation

---

## 2. Custom Widget Coverage Analysis

## 2. Custom Widget Coverage Analysis

**Purpose:** Verify that appropriate custom widgets are used for specialized data types.

**Approach:**

- Map each property type/format to expected widget
- Identify fields that would benefit from custom widgets but don't have them
- Check if existing custom widgets in `src/components/rjsf/Widgets/` are being utilized

**Expected Findings:**
| Property Type/Format | Expected Widget | Current Usage |
|---------------------|-----------------|---------------|
| `type: integer` with port semantics | `updown` | Check all adapters |
| `type: string` + `format: password` | `password` | Check TLS/Auth sections |
| `type: string` + long text | `textarea` | Check body/description fields |
| `type: string` + `format: mqtt-topic` | Custom `MqttTopicWidget` | Check topic fields |
| `type: string` + `format: uri` | URL input | Check URI fields |
| `type: array` with mappings | Batch mode | Check mapping arrays |

---

## 3. Enum Display Names (enumNames) Audit

**Purpose:** Ensure user-friendly display names for all enum fields.

**Approach:**

- Find all `enum` properties in backend schemas
- Check if corresponding `enumNames` or `ui:enumNames` are defined
- Verify display names are user-friendly (not technical constants)

**Example Issue:**

```java
// Backend enum
public enum SecurityPolicy {
    NONE,
    BASIC128RSA15,
    BASIC256,
    BASIC256SHA256
}
```

Without `enumNames`, users see raw values. Should have:

```json
{
  "ui:enumNames": ["None", "Basic 128 RSA 15", "Basic 256", "Basic 256 SHA256"]
}
```

---

## 4. Conditional Field Visibility Analysis

**Purpose:** Identify fields that SHOULD be conditional but aren't.

**Approach:**

- Analyze logical relationships between fields
- Check for boolean "enable" flags that should control sub-field visibility
- Verify `if/then/else` or `dependencies` are used appropriately

**Known Gaps (from current analysis):**
| Adapter | Toggle Field | Dependent Fields | Status |
|---------|-------------|------------------|--------|
| OPC-UA | `tls.enabled` | keystore, truststore | ❌ Missing |
| Databases | `encrypt` | trustCertificate | ❌ Missing |
| All with Auth | auth type selection | auth-specific fields | ? Unknown |

---

## 5. Default Values Consistency Analysis

**Purpose:** Ensure sensible defaults reduce user configuration effort.

**Approach:**

- List all fields with defaults
- Verify defaults match common use cases
- Check for missing defaults on optional fields
- Verify numeric defaults are within min/max constraints

**Example Check:**

```java
@ModuleConfigField(
    defaultValue = "1883",  // ✅ Good - standard MQTT port
    numberMin = 1,
    numberMax = 65535
)
private int port;
```

---

## 6. Array/Mapping Field UX Analysis

**Purpose:** Ensure array fields (mappings, tags, etc.) have good UX.

**Approach:**

- Check all array properties for `ui:batchMode`
- Verify `ui:collapsable` with appropriate `titleKey`
- Check item ordering within arrays
- Verify add/remove functionality works

**Key Properties:**

```json
{
  "arrayField": {
    "ui:batchMode": true,
    "items": {
      "ui:order": ["key", "value", "*"],
      "ui:collapsable": {
        "titleKey": "key"
      }
    }
  }
}
```

---

## 7. Field Ordering Analysis

**Purpose:** Ensure logical field order improves form usability.

**Approach:**

- Verify `ui:order` is defined for each adapter
- Check that most important fields come first
- Ensure related fields are grouped together
- Verify wildcard `*` placement is appropriate

**Best Practice Order:**

1. Identifier (`id`)
2. Connection settings (host, port, uri)
3. Authentication
4. Security/TLS
5. Protocol-specific settings
6. Advanced options
7. Publishing/Polling settings

---

## 8. Accessibility Analysis

**Purpose:** Ensure forms are accessible to all users.

**Approach:**

- Check all fields have proper labels (`title`)
- Verify descriptions provide sufficient context
- Check for ARIA attributes on custom widgets
- Verify keyboard navigation works
- Check color contrast for validation states

---

## 9. Internationalization (i18n) Readiness Analysis

**Purpose:** Assess readiness for multi-language support.

**Approach:**

- Check if titles/descriptions are hardcoded or extracted
- Identify strings that need extraction
- Check for property files or i18n infrastructure
- Document which adapters have i18n support

**Current State:**

- Backend: Titles/descriptions hardcoded in `@ModuleConfigField` annotations
- Frontend: Some i18n infrastructure exists but adapter schemas are hardcoded
- Recommendation: Plan for extracting adapter strings to property files

---

## 10. Error Message Analysis

**Purpose:** Ensure validation errors are user-friendly.

**Approach:**

- Trigger validation errors for each constraint type
- Check error messages are clear and actionable
- Verify custom error messages are defined where needed
- Check error message localization

**Constraint Types to Test:**

- `required` - missing required field
- `minLength/maxLength` - string length
- `minimum/maximum` - number range
- `pattern` - regex validation
- `format` - format validation (uri, email, etc.)

---

## 11. Form Performance Analysis

**Purpose:** Identify potential performance issues with large forms.

**Approach:**

- Profile form render time for each adapter
- Check for unnecessary re-renders
- Verify lazy loading of nested schemas
- Test with maximum array items

**Adapters with Complex Schemas:**

- OPC-UA (deeply nested TLS/Auth/Security)
- Databases (multiple connection types)
- Any adapter with large mapping arrays

---

## Recommended Priority

| Priority  | Analysis                     | Effort | Impact       | Status       |
| --------- | ---------------------------- | ------ | ------------ | ------------ |
| 🔴 High   | **Intentionality Analysis**  | Medium | High         | ✅ COMPLETED |
| 🔴 High   | Conditional Field Visibility | Medium | High         | ✅ COMPLETED |
| 🔴 High   | Enum Display Names           | Low    | Medium       | ✅ COMPLETED |
| 🟡 Medium | **Custom Widget Coverage**   | Medium | Medium       | ✅ COMPLETED |
| 🟡 Medium | Default Values               | Low    | Medium       |
| 🟡 Medium | Array/Mapping UX             | Medium | High         |
| 🟢 Low    | Field Ordering               | Low    | Low          |
| 🟢 Low    | Accessibility                | High   | Medium       |
| 🟢 Low    | i18n Readiness               | High   | Low (future) |
| 🟢 Low    | Error Messages               | Medium | Medium       |
| 🟢 Low    | Performance                  | High   | Low          |

---

## Next Steps

1. **Immediate:** Intentionality Analysis (detect semantic mismatches) - ✅ COMPLETED
2. **Immediate:** Conditional Field Visibility analysis (builds on existing work) - ✅ COMPLETED
3. **Short-term:** Enum Display Names audit (quick win) - ✅ COMPLETED
4. **Medium-term:** Custom Widget Coverage (requires frontend work) - ✅ COMPLETED
5. **Long-term:** Accessibility and i18n (significant effort)
