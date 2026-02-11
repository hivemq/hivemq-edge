# Backend Tickets Summary

**Source:** Task 38658 - Adapter JSON Schema Review
**Total Backend Issues:** 29 (1 Critical Schema Bug, 1 Critical Security, 3 High Schema, 6 High Widget Specs, 15 Medium, 3 Low)
**Grouped into:** 5 Tickets

---

## Ticket Overview

| #      | Prefix                | Title                                                    | Issues | Priority         | Estimated Effort |
| ------ | --------------------- | -------------------------------------------------------- | ------ | ---------------- | ---------------- |
| **01** | `[Databases Adapter]` | Critical JSON Schema and Config Issues                   | 6      | 🔴 Critical/High | ~35 mins         |
| **02** | `[OPC-UA]`            | Missing JSON Schema Dependencies and Enum Display Names  | 3      | 🟠 High/Medium   | ~35 mins         |
| **03** | `[Schema Validation]` | Add Cross-Field Validation for Modbus and Simulation     | 2      | 🟡 Medium        | ~40 mins         |
| **04** | `[UI Polish]`         | Improve Field Titles, Casing, and Enum Display Names     | 8      | 🟡 Medium/Low    | ~60 mins         |
| **05** | `[Widget Specs]`      | Add Missing ui:widget Specifications Across All Adapters | 10     | 🔴 Critical/High | ~60 mins         |

**Total Estimated Effort:** ~4 hours (all backend fixes)

---

## Ticket 01: [Databases Adapter] Critical JSON Schema and Config Issues

**File:** `TICKET_BACKEND_01_DATABASES_CRITICAL.md`
**Priority:** 🔴 Critical/High
**Swimlane:** Landing zone
**Linked to:** Card 38658 (relative)

### Issues Covered (6)

- ✅ **B-C1 (CRITICAL):** `getTrustCertificate()` returns wrong field
- ✅ **B-H1 (HIGH):** Port field has invalid string constraints on integer type
- ✅ **B-H2 (HIGH):** Missing conditional visibility for `trustCertificate` field
- ✅ **B-M1 (MEDIUM):** Field title uses raw camelCase
- ✅ **B-M8 (MEDIUM):** Missing `writeOnly` on id field
- ✅ **B-M11 (MEDIUM):** Database type enum missing friendly names

### Impact

- **Critical:** TLS certificate trust settings completely broken
- **High:** Port validation doesn't work, confusing UI
- **Medium:** Poor UX with technical names

### Solutions Provided

- Detailed code fixes for each issue
- Multiple implementation approaches where applicable
- Clear acceptance criteria

---

## Ticket 02: [OPC-UA] Missing JSON Schema Dependencies and Enum Display Names

**File:** `TICKET_BACKEND_02_OPCUA_SCHEMA.md`
**Priority:** 🟠 High/Medium
**Swimlane:** Landing zone
**Linked to:** Card 38658 (relative), Backend Ticket 01 (relative)

### Issues Covered (3)

- ✅ **B-H3 (HIGH):** TLS fields shown even when `tls.enabled = false`
- ✅ **B-M9 (MEDIUM):** Security policy enum shows raw values like `BASIC256SHA256`
- ✅ **B-M10 (MEDIUM):** Message security mode shows raw values like `SIGN_AND_ENCRYPT`

### Impact

- **High:** Confusing UX with irrelevant TLS fields shown
- **Medium:** Technical jargon confuses users unfamiliar with OPC-UA security

### Solutions Provided

- JSON Schema dependencies approach
- Backend `enumDisplayValues` approach
- UI schema `ui:enumNames` approach
- Recommended approach for each issue

---

## Ticket 03: [Schema Validation] Add Cross-Field Validation

**File:** `TICKET_BACKEND_03_VALIDATION.md`
**Priority:** 🟡 Medium
**Swimlane:** Landing zone
**Linked to:** Card 38658 (relative), Backend Tickets 01-02 (relative)

### Issues Covered (2)

- ✅ **B-M5 (MEDIUM):** Modbus missing `endIdx >= startIdx` validation
- ✅ **B-M6 (MEDIUM):** Simulation missing `maxValue > minValue` validation

### Impact

- Runtime errors with invalid address ranges
- Unreliable testing with invalid simulation bounds
- Users waste time debugging illogical configurations

### Solutions Provided

- Java Bean Validation approach (recommended)
- JSON Schema if/then approach
- Custom Validator approach
- Complete test cases for validation logic

---

## Ticket 04: [UI Polish] Improve Field Titles, Casing, and Enum Display Names

**File:** `TICKET_BACKEND_04_UI_POLISH.md`
**Priority:** 🟡 Medium/Low
**Swimlane:** Landing zone
**Linked to:** Card 38658 (relative), Backend Tickets 01-03 (relative)

### Issues Covered (8)

- ✅ **B-M2 (MEDIUM):** Simulation - raw camelCase title
- ✅ **B-M3 (MEDIUM):** ADS - "Ams" inconsistent casing
- ✅ **B-M4 (MEDIUM):** MTConnect - minimal UI schema organization
- ✅ **B-M7 (MEDIUM):** BACnet/IP - copy-paste description error
- ✅ **B-M12 (MEDIUM):** BACnet/IP - missing enum display names
- ✅ **B-L1 (LOW):** Multiple adapters - "millisecond" grammar error
- ✅ **B-L2 (LOW):** Simulation - "Minimum of delay" grammar
- ✅ **B-L3 (LOW):** HTTP - question mark in title

### Impact

- Unprofessional appearance across adapters
- Harder configuration due to poor organization
- Technical values confuse users

### Solutions Provided

- Simple text fixes for each issue
- UI schema tab organization for MTConnect
- Enum display names for BACnet types

---

## Ticket 05: [Widget Specs] Add Missing ui:widget Specifications

**File:** `TICKET_BACKEND_05_WIDGET_SPECIFICATIONS.md`
**Priority:** 🔴 Critical/High (Critical security + High UX)
**Swimlane:** Landing zone
**Linked to:** Card 38658 (relative), Backend Tickets 01-04 (relative)

### Issues Covered (10)

**Critical Security (1):**

- ✅ **WS-C1 (CRITICAL):** OPC-UA 4 password fields not masked - **SECURITY EXPOSURE**

**High Priority UX (6):**

- ✅ **WS-H1 (HIGH):** HTTP adapter missing updown widgets (4 fields)
- ✅ **WS-H2 (HIGH):** Databases adapter missing updown widgets (2 fields)
- ✅ **WS-H3 (HIGH):** MTConnect adapter missing updown widgets (3 fields)
- ✅ **WS-H4 (HIGH):** OPC-UA adapter missing updown widgets (2 fields)
- ✅ **WS-H5 (HIGH):** File adapter missing updown widget (1 field)
- ✅ **WS-H6 (HIGH):** Simulation adapter missing updown widgets (3 fields)

**Medium Priority Consistency (3):**

- ✅ **WS-M1 (MEDIUM):** EIP adapter slot missing widget
- ✅ **WS-M2 (MEDIUM):** S7 adapter rack/slot missing widgets (2 fields)
- ✅ **WS-M3 (MEDIUM):** Modbus adapter timeout missing widget

### Impact

- **CRITICAL SECURITY:** OPC-UA passwords visible in plain text (shoulder-surfing risk)
- **High UX:** Poor user experience with manual number entry, no visual bounds
- **Medium:** Inconsistent UX within adapters

### Solutions Provided

- Complete UI schema JSON for all affected adapters
- Password masking for security compliance
- Updown widgets for better numeric input UX

### Note

These issues were initially misclassified as frontend issues. The corrected analysis (CUSTOM_WIDGET_COVERAGE_ANALYSIS.md v2) determined they are backend UI schema specification issues.

---

## Coverage Verification

### Schema Issues (Tickets 01-04) - 19 Issues ✅

**Critical (1):**

- ✅ B-C1 → Ticket 01

**High (3):**

- ✅ B-H1 → Ticket 01
- ✅ B-H2 → Ticket 01
- ✅ B-H3 → Ticket 02

**Medium (12):**

- ✅ B-M1 → Ticket 01
- ✅ B-M2 → Ticket 04
- ✅ B-M3 → Ticket 04
- ✅ B-M4 → Ticket 04
- ✅ B-M5 → Ticket 03
- ✅ B-M6 → Ticket 03
- ✅ B-M7 → Ticket 04
- ✅ B-M8 → Ticket 01
- ✅ B-M9 → Ticket 02
- ✅ B-M10 → Ticket 02
- ✅ B-M11 → Ticket 01
- ✅ B-M12 → Ticket 04

**Low (3):**

- ✅ B-L1 → Ticket 04
- ✅ B-L2 → Ticket 04
- ✅ B-L3 → Ticket 04

### Widget Specification Issues (Ticket 05) - 10 Issues ✅

**Critical (1):**

- ✅ WS-C1 → Ticket 05 (OPC-UA password exposure)

**High (6):**

- ✅ WS-H1 → Ticket 05 (HTTP updown widgets)
- ✅ WS-H2 → Ticket 05 (Databases updown widgets)
- ✅ WS-H3 → Ticket 05 (MTConnect updown widgets)
- ✅ WS-H4 → Ticket 05 (OPC-UA updown widgets)
- ✅ WS-H5 → Ticket 05 (File updown widget)
- ✅ WS-H6 → Ticket 05 (Simulation updown widgets)

**Medium (3):**

- ✅ WS-M1 → Ticket 05 (EIP slot widget)
- ✅ WS-M2 → Ticket 05 (S7 rack/slot widgets)
- ✅ WS-M3 → Ticket 05 (Modbus timeout widget)

**All 29 Backend Issues Covered ✅**

---

## Ticket Template Structure

Each ticket follows the **Bug - Broker** template:

1. **Header**

   - Prefix, Title, Type, Swimlane, Links, Priority

2. **EXPECTED BEHAVIOR**

   - Checkbox list of all issues to fix
   - Clear acceptance criteria

3. **ACTUAL BEHAVIOR**

   - Detailed problem description for each issue
   - Code examples showing bugs
   - Impact statements

4. **STEPS TO REPRODUCE**

   - How to observe each issue

5. **PROPOSED SOLUTIONS**

   - Detailed fixes with code examples
   - Multiple approaches where applicable
   - Recommended approach

6. **CUSTOMER IMPACT**

   - Clear impact statements per priority level

7. **AFFECTED VERSIONS**

   - Version information

8. **RELATED ISSUES**

   - Links to analysis documents
   - Links to related tickets

9. **DoR (Definition of Ready)**

   - Complete DoR checklist

10. **PRs**

    - Checkbox list for implementation

11. **ACCEPTANCE TEST RESULTS**

    - Test cases to verify fixes

12. **RELEASE NOTE SUGGESTIONS**
    - Ready-to-use release notes

---

## Linking Strategy

```
Card 38658 (Parent Investigation)
    ↓ relative
    ├── Backend Ticket 01 [Databases Adapter]
    │       ↓ relative
    ├── Backend Ticket 02 [OPC-UA]
    │       ↓ relative
    ├── Backend Ticket 03 [Schema Validation]
    │       ↓ relative
    ├── Backend Ticket 04 [UI Polish]
    │       ↓ relative
    └── Backend Ticket 05 [Widget Specs] ← NEW
```

All backend tickets are:

- Created in **Landing zone** swimlane
- Linked to **Card 38658** as `relative`
- Linked to **each other** as `relative` (sequentially)

---

## Priority Recommendation

**Immediate Action (Security Critical):**

1. **Ticket 05 (WS-C1)** - OPC-UA password exposure - CRITICAL SECURITY FIX
2. **Ticket 01 (B-C1)** - Databases `getTrustCertificate()` bug - CRITICAL LOGIC BUG

**High Priority (Fix Soon):** 3. **Ticket 01 (B-H1, B-H2)** - Databases schema validation issues 4. **Ticket 02 (B-H3)** - OPC-UA conditional visibility 5. **Ticket 05 (WS-H1-H6)** - All updown widget specifications (UX improvement)

**Medium Priority (Next Sprint):** 6. **Ticket 03** - Cross-field validation (Modbus, Simulation) 7. **Ticket 04** - UI Polish (titles, grammar, enum names) 8. **Ticket 05 (WS-M1-M3)** - Remaining updown widgets (consistency)

**Low Priority (Backlog):** 9. Grammar fixes (B-L1, B-L2, B-L3) in Ticket 04

---

## Next Steps

1. **Review** these 5 ticket documents
2. **Adjust** content if needed
3. **Prioritize** Ticket 05 WS-C1 for immediate security fix
4. **Create tickets** in BusinessMap when approved
5. **Coordinate** with frontend team on mock regeneration after backend fixes

---

**See:** `CUSTOM_WIDGET_COVERAGE_ANALYSIS.md` (corrected version) for detailed analysis.
