---
title: 'Adapter Schema Analysis 2025'
author: 'Edge Frontend Team'
last_updated: '2026-02-16'
purpose: 'Permanent summary of 2025 adapter JSON Schema review (task 38658)'
audience: 'Frontend Developers, Backend Developers, Technical Leadership'
maintained_at: 'docs/analysis/ADAPTER_SCHEMA_ANALYSIS_2025.md'
---

# Adapter Schema Analysis 2025

**Analysis Date:** December 2025
**Task:** 38658-adapter-jsonschema-review
**Scope:** Comprehensive review of all protocol adapter JSON Schema and UI Schema configurations

---

## Executive Summary

A comprehensive review of all 15+ protocol adapter schema configurations identified **28 issues** across frontend and backend codebases, including 2 critical bugs, 7 high-priority schema validation issues, and extensive gaps in enum display names and conditional visibility.

### Key Findings

- **Critical Issues:** 2 (File adapter broken, Databases getter bug)
- **High Priority:** 7 (Schema validation, conditional visibility)
- **Medium Priority:** 13 (Enum display names, title casing)
- **Low Priority:** 6 (Grammar, orphaned components)

### Impact

- **File Adapter:** Completely broken tag configuration (shows HTTP fields)
- **Databases Adapter:** Runtime bug in getter method
- **User Experience:** Technical enum values shown instead of friendly labels
- **Validation Gaps:** Missing hostname validator affects 3 adapters
- **Conditional Visibility:** Irrelevant fields shown (TLS settings when TLS disabled)

---

## Issue Breakdown

| Category | Frontend Issues | Backend Issues | Total |
|----------|-----------------|----------------|-------|
| 🔴 Critical | 1 | 1 | 2 |
| 🟠 High | 4 | 3 | 7 |
| 🟡 Medium | 1 | 12 | 13 |
| 🟢 Low | 3 | 3 | 6 |
| **Total** | **9** | **19** | **28** |

---

## Critical Issues

### F-C1: File Adapter Tag Schema Wrong

**Status:** 🔴 Open (Frontend)
**Impact:** Broken functionality

**Problem:**
- Mock schema in `src/__test-utils__/adapters/file.ts` is a copy of HTTP adapter schema
- Shows HTTP fields (`httpHeaders`, `httpRequestBody`, `url`) instead of File fields
- `protocolId` incorrectly set to `'http'` instead of `'file'`

**Expected:**
- `filePath` (string, required)
- `contentType` (enum: BINARY, TEXT, JSON, XML, CSV)
- `protocolId: 'file'`

**Fix:** Automated - `node tools/update-adapter-mocks.cjs --adapter=file`

### B-C1: Databases Adapter getTrustCertificate() Returns Wrong Field

**Status:** 🔴 Open (Backend)
**Impact:** Runtime logic bug

**Problem:**
```java
public Boolean getTrustCertificate() {
    return encrypt;  // Returns wrong field!
}
```

**Fix:** Backend code change required
**File:** `modules/hivemq-edge-module-databases/src/main/java/.../DatabasesAdapterConfig.java`

---

## High Priority Issues

### Schema Validation Issues

| Issue | Adapter | Problem | Severity |
|-------|---------|---------|----------|
| **F-H1** | Databases | Port field has string constraints (`minLength`, `pattern`) on integer type | 🟠 High |
| **F-H2** | Databases | Port UI schema uses invalid `type: 'integer'` property | 🟠 High |
| **F-H3** | Modbus | Frontend `id.ui:disabled: false` doesn't match backend `true` | 🟠 High |
| **B-H1** | Databases | Backend port constraints use string validation on Integer field | 🟠 High |

### Missing Conditional Visibility

| Issue | Adapter | Condition | Hidden Fields | Severity |
|-------|---------|-----------|---------------|----------|
| **B-H2** | Databases | `encrypt = false` | `trustCertificate` | 🟠 High |
| **B-H3** | OPC-UA | `tls.enabled = false` | `keystore`, `truststore`, `tlsChecks` | 🟠 High |

### Missing Format Validator

| Issue | Format | Affected Adapters | Severity |
|-------|--------|-------------------|----------|
| **F-M1** | `hostname` | Modbus, EIP, PLC4X (ADS/S7) | 🟡 Medium |

**Impact:** Users can enter invalid hostnames, causing connection errors at runtime.

**Fix Needed:** Implement `hostname` format validator in `src/components/rjsf/Form/validation.utils.ts`

---

## Medium Priority Issues

### Missing Enum Display Names

Technical enum values shown instead of user-friendly labels:

| Adapter | Field | Current Display | Should Display | Issue |
|---------|-------|-----------------|----------------|-------|
| OPC-UA | `policy` | `BASIC256SHA256` | "Basic 256 SHA256" | B-M9 |
| OPC-UA | `messageSecurityMode` | `SIGN_AND_ENCRYPT` | "Sign and Encrypt" | B-M10 |
| Databases | `type` | `POSTGRESQL`, `MYSQL` | "PostgreSQL", "MySQL" | B-M11 |
| BACnet/IP | `objectType`, `propertyType` | Technical codes | Descriptive names | B-M12 |

**Fix:** Backend needs to add `enumDisplayValues` annotation or `ui:enumNames` in UI Schema.

### Title Casing Issues

| Adapter | Field | Current Title | Should Be | Issue |
|---------|-------|---------------|-----------|-------|
| Databases | `connectionTimeoutSeconds` | "connectionTimeoutSeconds" | "Connection Timeout (seconds)" | B-M1 |
| Simulation | `simulationToMqtt` | "simulationToMqtt" | "Simulation To MQTT Config" | B-M2 |
| ADS | Multiple | "Source Ams Net Id" | "Source AMS Net ID" | B-M3 |

---

## Low Priority Issues

### Grammar Issues

- **B-L1:** "millisecond" should be "milliseconds" in multiple adapters (File, Databases, MTConnect)
- **B-L2:** Simulation adapter: "Minimum of delay" → "Minimum Delay"
- **B-L3:** HTTP adapter: "Assert JSON Response?" → "Assert JSON Response" (remove question mark)

### Orphaned Frontend Components

| Component | Status | File | Issue |
|-----------|--------|------|-------|
| ToggleWidget | ⚠️ Not in use | `src/components/rjsf/Widgets/ToggleWidget.tsx` | F-L1 |
| AdapterTagSelect | ⚠️ Not in use | `src/components/rjsf/Widgets/AdapterTagSelect.tsx` | F-L2 |
| InternalNotice | ⚠️ Not in use | `src/components/rjsf/Fields/InternalNotice.tsx` | F-L3 |

**Recommendation:** Document intended use case or remove to reduce maintenance burden.

---

## Remediation Status

### Frontend

**Automated Fixes (5 issues):**
```bash
# Fixes F-C1, F-H1, F-H2, F-H3, F-H4
node tools/update-adapter-mocks.cjs --all
```

**Status:** ✅ Tool available, awaiting execution

**Manual Fixes (4 issues):**
- F-M1: Implement HOSTNAME format validator *(Medium priority)*
- F-L1, F-L2, F-L3: Document or remove orphaned components *(Low priority)*

**Status:** 🔄 Awaiting implementation

### Backend

**Critical (2 issues):**
- B-C1: Fix Databases `getTrustCertificate()` method
- B-H1: Fix port field constraints

**High Priority (2 issues):**
- B-H2: Add Databases `encrypt → trustCertificate` conditional visibility
- B-H3: Add OPC-UA `tls.enabled` conditional visibility

**Medium Priority (12 issues):**
- B-M1 to B-M12: Title casing, enum display names, cross-field validation, MTConnect UI schema

**Low Priority (3 issues):**
- B-L1 to B-L3: Grammar fixes

**Status:** 🔄 Awaiting backend team implementation

---

## Analysis Methodology

### Data Sources

**Backend Analysis:**
- Java source code (`@ModuleConfigField` annotations)
- UI Schema JSON files
- 3 repositories: `hivemq-edge`, `hivemq-edge-module-*`, `hivemq-edge-module-bacnetip`
- Commit: `eabcb94278d8e5b66a2daea7b491a3ea76751d99` (2025-12-09)

**Frontend Analysis:**
- Mock schemas in `src/__test-utils__/adapters/`
- RJSF custom widgets, fields, templates
- Format validators
- Component implementations

### Analysis Documents

Complete analysis available in `.tasks/38658-adapter-jsonschema-review/`:

| Document | Purpose |
|----------|---------|
| `REMEDIATION_REPORT.md` | Complete remediation report with all 28 issues |
| `CUSTOM_WIDGET_COVERAGE_ANALYSIS.md` | Widget/field/format coverage analysis |
| `CONDITIONAL_VISIBILITY_ANALYSIS.md` | Conditional field dependency gaps |
| `ENUM_DISPLAY_NAMES_AUDIT.md` | Enum display name coverage audit |
| `SCHEMA_ANALYSIS_V3.md` | Backend schema structure analysis |
| `FRONTEND_MOCKS_ANALYSIS.md` | Frontend mock compliance analysis |
| `INTENTIONALITY_ANALYSIS.md` | Semantic mismatch detection |

---

## Affected Adapters

| Adapter | Critical Issues | High Issues | Medium Issues | Low Issues | Total |
|---------|----------------|-------------|---------------|------------|-------|
| **File** | 1 | 0 | 0 | 1 | 2 |
| **Databases** | 1 | 3 | 3 | 1 | 8 |
| **Modbus** | 0 | 1 | 1 | 0 | 2 |
| **OPC-UA** | 0 | 1 | 2 | 0 | 3 |
| **BACnet/IP** | 0 | 0 | 2 | 0 | 2 |
| **Simulation** | 0 | 0 | 2 | 1 | 3 |
| **ADS (PLC4X)** | 0 | 0 | 1 | 0 | 1 |
| **MTConnect** | 0 | 0 | 1 | 1 | 2 |
| **HTTP** | 0 | 0 | 0 | 1 | 1 |
| **EIP, S7** | 0 | 0 | 0 | 0 | 0 |
| **MQTT** | 0 | 0 | 0 | 0 | 0 |
| **RJSF Framework** | 0 | 1 | 1 | 3 | 5 |

**Most Affected:** Databases adapter (8 issues)

---

## Recommendations

### Immediate Actions

1. **Fix Critical Issues**
   - Run frontend automated fixes
   - Fix Databases `getTrustCertificate()` method

2. **Implement High Priority Fixes**
   - Add hostname format validator (frontend)
   - Fix port field validation (backend)
   - Add conditional visibility for encrypt and TLS fields (backend)

### Short-Term Improvements

3. **Add Enum Display Names**
   - OPC-UA security policies and message modes
   - Databases type enum
   - BACnet/IP object and property types

4. **Fix Title Casing**
   - Update all camelCase titles to "Proper Case"

### Long-Term Maintenance

5. **Automated Schema Sync**
   - Consider tool to automatically sync backend schemas to frontend mocks
   - Reduce manual maintenance burden

6. **Widget Specification Standardization**
   - Backend should consistently specify widgets (`ui:widget: "password"`, `"updown"`)
   - Document widget specification guidelines

7. **Conditional Visibility Patterns**
   - Establish JSON Schema patterns for conditional fields
   - Document in backend guidelines

---

## Cross-Team Coordination

### Dependencies

- **Backend fixes port constraints (B-H1)** → Frontend regenerates mocks (F-H1)
- **Backend adds widget specifications** → Frontend uses automatically
- **Backend adds enum display names** → Frontend displays user-friendly labels

### Communication Channels

- Frontend issues tracked in frontend repository
- Backend issues communicated to backend team
- Schema synchronization requires coordination

---

## Lessons Learned

### Schema Design

1. **Consistency:** Use consistent naming (title casing, plural forms)
2. **User-Friendly:** Always provide enum display names for technical values
3. **Conditional Logic:** Use JSON Schema dependencies for conditional visibility
4. **Validation:** Specify correct constraints for data types (integer vs string)

### Frontend/Backend Sync

1. **Mock Schemas:** Frontend mocks must stay in sync with backend
2. **Automated Tools:** Tools like `update-adapter-mocks.cjs` reduce manual errors
3. **Format Validators:** Frontend must implement all backend format types

### Quality Assurance

1. **Comprehensive Review:** Systematic review identified issues missed in development
2. **Cross-Functional:** Review required both frontend and backend expertise
3. **Documentation:** Detailed analysis aids remediation and prevents regressions

---

## Related Documentation

**Architecture:**
- [Protocol Adapter Architecture](../architecture/PROTOCOL_ADAPTER_ARCHITECTURE.md) - Adapter configuration architecture
- [DataHub Architecture](../architecture/DATAHUB_ARCHITECTURE.md) - DataHub policy designer

**Guides:**
- [RJSF Guide](../guides/RJSF_GUIDE.md) - Complete RJSF implementation guide
- [Testing Guide](../guides/TESTING_GUIDE.md) - Testing patterns
- [Cypress Guide](../guides/CYPRESS_GUIDE.md) - Cypress-specific patterns

**Technical:**
- [Technical Stack](../technical/TECHNICAL_STACK.md) - Dependencies and toolchain

---

## Next Review

**Recommended:** Q1 2026 (After remediation)

**Focus Areas:**
- Verify all issues resolved
- Check for new adapters added
- Review schema consistency across all adapters
- Validate conditional visibility implementations

---

**Analysis Team:** Edge Frontend Team + Backend Collaboration
**Review Period:** December 2025
**Status:** Permanent handover documentation - maintained as issues are resolved
