# Conditional Field Visibility Analysis

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

This analysis identifies fields that SHOULD be conditionally visible based on other field values but are always displayed, regardless of context. This creates confusing UX where users see irrelevant configuration options.

---

## Summary of Findings

| Severity  | Adapter   | Toggle Field                 | Hidden Fields                   | Status     |
| --------- | --------- | ---------------------------- | ------------------------------- | ---------- |
| 🔴 High   | OPC-UA    | `tls.enabled`                | keystore, truststore, tlsChecks | ❌ Missing |
| 🔴 High   | Databases | `encrypt`                    | trustCertificate                | ❌ Missing |
| 🟡 Medium | OPC-UA    | `auth.x509.enabled`          | (keystore in tls)               | ❌ Missing |
| 🟡 Medium | OPC-UA    | `overrideUri`                | applicationUri                  | ❌ Missing |
| 🟢 Low    | HTTP      | `allowUntrustedCertificates` | N/A (standalone)                | ✅ OK      |

**Total Issues: 4** (conditional visibility not implemented)

---

## 🔴 High Priority Issues

### CV-H1. OPC-UA TLS: Sub-fields Shown When TLS Disabled

**Location:** `modules/hivemq-edge-module-opcua/src/main/java/.../Tls.java`

**Current Structure:**

```java
public record Tls (
    @JsonProperty("enabled")
    @ModuleConfigField(title = "Enable TLS", defaultValue = "false")
    boolean enabled,

    @JsonProperty("tlsChecks")
    @ModuleConfigField(title = "Certificate validation", defaultValue = "STANDARD")
    @Nullable TlsChecks tlsChecks,

    @JsonProperty("keystore")
    @ModuleConfigField(title = "Keystore", description = "...Required for X509 authentication.")
    @Nullable Keystore keystore,

    @JsonProperty("truststore")
    @ModuleConfigField(title = "Truststore", description = "...trusted server certificates...")
    @Nullable Truststore truststore
) { ... }
```

**Problem:**

- When `enabled = false`, users still see `tlsChecks`, `keystore`, and `truststore` fields
- These fields are meaningless without TLS enabled
- Clutters the form with irrelevant options

**UI Schema (current):**

```json
"tls": {
  "ui:order": ["enabled", "tlsChecks", "keystore", "truststore"],
  ...
}
```

**Required Fix - JSON Schema `dependencies`:**

```json
{
  "tls": {
    "type": "object",
    "properties": {
      "enabled": { "type": "boolean", "default": false },
      "tlsChecks": { "type": "string", "enum": [...] },
      "keystore": { "type": "object" },
      "truststore": { "type": "object" }
    },
    "dependencies": {
      "enabled": {
        "oneOf": [
          {
            "properties": { "enabled": { "const": false } }
          },
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

**Impact:** Users see 4+ irrelevant fields when TLS is disabled (default).

---

### CV-H2. Databases: trustCertificate Shown When encrypt=false

**Location:** `modules/hivemq-edge-module-databases/src/main/java/.../DatabasesAdapterConfig.java`

**Current Structure:**

```java
@JsonProperty(value = "encrypt")
@ModuleConfigField(title = "Encrypt",
           description = "Use TLS to communicate with the remote database")
protected Boolean encrypt;

@JsonProperty(value = "trustCertificate")
@ModuleConfigField(title = "Trust Certificate",
           description = "Do you want to trust remote certificate")
protected Boolean trustCertificate;
```

**Problem:**

- `trustCertificate` is only relevant when `encrypt = true`
- When encryption is disabled, trusting certificates is meaningless
- Field is always visible regardless of `encrypt` value

**UI Schema (current):**

```json
{
  "ui:tabs": [
    {
      "id": "coreFields",
      "title": "Settings",
      "properties": ["id", "type", "server", "port", "database", "username", "password"]
    }
    // NOTE: encrypt and trustCertificate not even in tabs!
  ]
}
```

**Additional Issue:** The `encrypt` and `trustCertificate` fields are **missing from UI schema tabs entirely**.

**Required Fix:**

1. Add `encrypt` and `trustCertificate` to UI schema tabs
2. Add JSON Schema dependency:

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

## 🟡 Medium Priority Issues

### CV-M1. OPC-UA X509 Auth: Keystore Dependency

**Location:** `modules/hivemq-edge-module-opcua/src/main/java/.../X509Auth.java`

**Current Structure:**

```java
public record X509Auth(
    @JsonProperty("enabled")
    @ModuleConfigField(title = "Enable X509", description = "Enables X509 auth")
    boolean enabled
) { ... }
```

**Problem:**

- X509 authentication requires a keystore (from TLS section)
- When `x509.enabled = true`, users should be guided to configure keystore
- Currently no visual connection between X509 auth and TLS keystore requirement

**Recommendation:**

- Either show keystore fields conditionally within auth section
- Or add help text explaining keystore must be configured in TLS section
- Consider restructuring: if X509 is enabled, TLS keystore becomes required

---

### CV-M2. OPC-UA Override URI: applicationUri Dependency

**Location:** `modules/hivemq-edge-module-opcua/src/main/java/.../OpcUaSpecificAdapterConfig.java`

**Current Structure:**

```java
@JsonProperty(value = "overrideUri")
@ModuleConfigField(title = "Override Application URI?",
           description = "...")
private final boolean overrideUri;

@JsonProperty(value = "applicationUri")
@ModuleConfigField(title = "Application URI Override",
           description = "...")
private final @Nullable String applicationUri;
```

**Problem:**

- `applicationUri` field only relevant when `overrideUri = true`
- When override is disabled, the URI field is confusing

**Required Fix:**

```json
{
  "dependencies": {
    "overrideUri": {
      "oneOf": [
        { "properties": { "overrideUri": { "const": false } } },
        {
          "properties": {
            "overrideUri": { "const": true },
            "applicationUri": { "type": "string" }
          },
          "required": ["applicationUri"]
        }
      ]
    }
  }
}
```

---

## ✅ Verified OK (No Issues)

### Standalone Boolean Fields

These boolean fields are independent toggles that don't control other field visibility:

| Adapter   | Field                              | Purpose                      |
| --------- | ---------------------------------- | ---------------------------- |
| HTTP      | `allowUntrustedCertificates`       | Security toggle (standalone) |
| HTTP      | `assertResponseIsJson`             | Validation option            |
| HTTP      | `httpPublishSuccessStatusCodeOnly` | Filter option                |
| MTConnect | `allowUntrustedCertificates`       | Security toggle              |
| Modbus    | `publishChangedDataOnly`           | Performance option           |
| EIP       | `publishChangedDataOnly`           | Performance option           |
| PLC4x     | `publishChangedDataOnly`           | Performance option           |
| ADS/S7    | `keepAlive`                        | Connection option            |

**Verdict:** ✅ These are properly independent - no conditional visibility needed.

---

## UI Schema Gaps Discovered

During analysis, found UI schema configuration issues:

### Databases Adapter UI Schema

**Missing Fields in Tabs:**

- `encrypt` - not in any tab
- `trustCertificate` - not in any tab
- `connectionTimeoutSeconds` - not in any tab

**Current tabs only cover:**

```json
"properties": ["id", "type", "server", "port", "database", "username", "password"]
```

**Recommendation:** Add missing fields to tabs or create "Security" tab.

---

## Implementation Approaches

### Option A: JSON Schema Dependencies (Recommended)

Add `dependencies` to config schema generation:

```java
@ModuleConfigField(
    title = "Truststore",
    conditionalOn = "enabled",  // NEW: conditional visibility
    conditionalValue = "true"
)
```

**Pros:**

- RJSF natively supports `dependencies`
- Clean separation of concerns
- Works without custom widgets

**Cons:**

- Requires backend schema generation changes
- May need new annotation attributes

### Option B: UI Schema with Custom Widget

Create React component that handles visibility:

```json
{
  "tls": {
    "ui:widget": "ConditionalObjectWidget",
    "ui:options": {
      "toggleField": "enabled",
      "hiddenWhenFalse": ["tlsChecks", "keystore", "truststore"]
    }
  }
}
```

**Pros:**

- No backend changes needed
- Can be implemented incrementally

**Cons:**

- Custom widget maintenance
- Logic split between schema and widget

### Option C: if/then/else in JSON Schema

Modern JSON Schema approach:

```json
{
  "if": {
    "properties": { "enabled": { "const": true } }
  },
  "then": {
    "properties": {
      "keystore": { "type": "object" },
      "truststore": { "type": "object" }
    }
  }
}
```

**Pros:**

- Standard JSON Schema 7 feature
- Cleaner than `dependencies`

**Cons:**

- May require Ajv8 configuration
- RJSF support varies by version

---

## Recommendations

### Immediate Actions (Backend)

| ID    | Issue                        | Module    | Effort | Impact |
| ----- | ---------------------------- | --------- | ------ | ------ |
| CV-H1 | OPC-UA TLS visibility        | opcua     | Medium | High   |
| CV-H2 | Databases encrypt visibility | databases | Medium | High   |

### Short-term Actions (Backend + Frontend)

| ID    | Issue                   | Module | Effort | Impact |
| ----- | ----------------------- | ------ | ------ | ------ |
| CV-M1 | X509/Keystore guidance  | opcua  | Low    | Medium |
| CV-M2 | Override URI visibility | opcua  | Low    | Low    |

### UI Schema Fixes (Frontend)

1. Add missing fields to Databases adapter tabs
2. Consider custom conditional widget for complex cases

---

## Cross-Reference

This analysis complements:

- [REMEDIATION_REPORT.md](./REMEDIATION_REPORT.md) - B-H2, B-H3 cover these issues
- [INTENTIONALITY_ANALYSIS.md](./INTENTIONALITY_ANALYSIS.md) - Semantic analysis
- [ADDITIONAL_ANALYSIS_RECOMMENDATIONS.md](./ADDITIONAL_ANALYSIS_RECOMMENDATIONS.md) - Analysis methodology

---

## Appendix: RJSF Dependencies Support

RJSF supports JSON Schema `dependencies` out of the box:

```jsx
// Example: field B only shows when field A is true
const schema = {
  type: 'object',
  properties: {
    enableFeature: { type: 'boolean' },
  },
  dependencies: {
    enableFeature: {
      oneOf: [
        { properties: { enableFeature: { const: false } } },
        {
          properties: {
            enableFeature: { const: true },
            featureConfig: { type: 'string' },
          },
        },
      ],
    },
  },
}
```

Reference: https://rjsf-team.github.io/react-jsonschema-form/docs/json-schema/dependencies
