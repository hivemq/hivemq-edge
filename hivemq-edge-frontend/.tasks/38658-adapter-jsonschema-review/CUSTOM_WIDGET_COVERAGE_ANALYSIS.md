# Custom Widget Coverage Analysis (Corrected)

**Task:** 38658-adapter-jsonschema-review
**Analysis Date:** 2025-12-17 (Corrected)
**Scope:** Backend Schema Specifications vs Frontend RJSF Implementations

---

## Executive Summary

This analysis verifies that every `ui:widget`, `ui:field`, and `format` specified in backend adapter schemas has a corresponding React component implementation in the frontend RJSF framework.

**Key Findings:**

- ✅ **Widget Coverage:** All backend-specified widgets are supported (100% coverage)
- ✅ **Field Coverage:** Backend doesn't specify custom fields (N/A)
- ⚠️ **Format Coverage:** Missing validator for `HOSTNAME` format (1 gap)
- 🧹 **Orphaned Components:** 3 frontend widgets never used anywhere (cleanup candidates)

**Issues Identified:** 1 Medium, 3 Low

---

## Methodology

### Analysis Approach (Corrected)

**Previous Approach (INCORRECT):**

- ❌ Analyzed frontend mocks (`src/__test-utils__/adapters/*.ts`)
- ❌ Compared mocks to backend to find missing widgets
- ❌ Classified missing widgets as frontend issues

**Corrected Approach:**

1. ✅ Scanned **BACKEND** sources for all widget/field/format specifications:
   - Java `@ModuleConfigField` annotations with `format = FieldType.X`
   - UI Schema JSON files with `"ui:widget"` and `"ui:field"` properties
2. ✅ Inventoried **FRONTEND** RJSF component implementations:
   - Custom widgets in `src/components/rjsf/Widgets/`
   - Custom fields in `src/components/rjsf/Fields/`
   - Custom format validators in `src/components/rjsf/Form/validation.utils.ts`
3. ✅ Identified **GAPS:** Backend specifies but frontend doesn't implement
4. ✅ Identified **ORPHANS:** Frontend implements but backend never uses

**Why Frontend Mocks Cannot Be Trusted:**

- Mocks are out of sync with backend (being fixed with generate/update scripts)
- Missing widgets in mocks doesn't mean frontend lacks implementation
- Specification issues (missing `ui:widget`) are **BACKEND problems**, not frontend

---

## Backend Specifications Inventory

### ui:widget Specifications

| Widget Name | Used In Adapters     | File Location                                                                                                            |
| ----------- | -------------------- | ------------------------------------------------------------------------------------------------------------------------ |
| `password`  | Databases            | `databases-adapter-ui-schema.json`                                                                                       |
| `textarea`  | HTTP                 | `http-adapter-ui-schema.json`                                                                                            |
| `updown`    | EIP, Modbus, ADS, S7 | `eip-adapter-ui-schema.json`, `modbus-adapter-ui-schema.json`, `ads-adapter-ui-schema.json`, `s7-adapter-ui-schema.json` |

**Total:** 3 unique widget types specified

### ui:field Specifications

**None found** - Backend doesn't use custom `ui:field` in any adapter schema.

### format Specifications (from Java @ModuleConfigField)

| Format Type         | Usage Count | Example Location                            |
| ------------------- | ----------- | ------------------------------------------- |
| `MQTT_TOPIC`        | ~15         | Simulation, HTTP, Modbus, File, EIP, PLC4X  |
| `MQTT_TAG`          | ~10         | All tag-based adapters                      |
| `MQTT_TOPIC_FILTER` | ~2          | HTTP adapter                                |
| `IDENTIFIER`        | ~10         | All adapters (id field)                     |
| `URI`               | ~5          | HTTP, MTConnect, OPC-UA                     |
| `BOOLEAN`           | ~15         | Multiple adapters                           |
| `HOSTNAME`          | ~3          | Modbus, EIP, PLC4X                          |
| `UNSPECIFIED`       | ~10         | Databases adapter (password, encrypt, etc.) |

**Total:** 8 unique format types specified

---

## Frontend Implementations Inventory

### Custom Widgets

Located in `src/components/rjsf/Widgets/`:

| Widget File              | Registered?                 | Used In           | Status              |
| ------------------------ | --------------------------- | ----------------- | ------------------- |
| `UpDownWidget.tsx`       | ✅ Yes (in `ChakraRJSForm`) | Protocol adapters | ✅ Active           |
| `ToggleWidget.tsx`       | ❌ No                       | Never used        | ⚠️ Orphan           |
| `AdapterTagSelect.tsx`   | ❌ No                       | Never used        | ⚠️ Orphan           |
| `SchemaWidget.tsx`       | ❌ No (DataHub only)        | DataHub schemas   | ✅ Active (DataHub) |
| `EntitySelectWidget.tsx` | ❌ No (DataHub only)        | DataHub schemas   | ✅ Active (DataHub) |

**Widget Registry** (`src/components/rjsf/Form/ChakraRJSForm.tsx`, `src/modules/ProtocolAdapters/utils/uiSchema.utils.ts`):

```typescript
export const adapterJSFWidgets: RegistryWidgetsType = {
  'discovery:tagBrowser': 'text',  // Disabled (issue #24369)
  'application/schema+json': JSONSchemaEditor,  // DataHub only
}

// In ChakraRJSForm:
widgets={{
  ...(!showNativeWidgets && adapterJSFWidgets),
  UpDownWidget,  // Only custom widget for adapters
}}
```

### Custom Fields

Located in `src/components/rjsf/Fields/`:

| Field File                    | Registered?                  | Used In            | Status    |
| ----------------------------- | ---------------------------- | ------------------ | --------- |
| `CompactArrayField.tsx`       | ✅ Yes (as `compactTable`)   | Adapter arrays     | ✅ Active |
| `MqttTransformationField.tsx` | ✅ Yes (as `mqtt:transform`) | DataHub transforms | ✅ Active |
| `InternalNotice.tsx`          | ❌ No                        | Never imported     | ⚠️ Orphan |

### Custom Format Validators

Located in `src/components/rjsf/Form/validation.utils.ts`:

| Format Name                | Validator Function        | Backend Usage          | Status     |
| -------------------------- | ------------------------- | ---------------------- | ---------- |
| `mqtt-topic`               | `validationTopic()`       | ✅ MQTT_TOPIC          | ✅ Match   |
| `mqtt-tag`                 | `validationTag()`         | ✅ MQTT_TAG            | ✅ Match   |
| `mqtt-topic-filter`        | `validationTopicFilter()` | ✅ MQTT_TOPIC_FILTER   | ✅ Match   |
| `identifier`               | `() => true` (stub)       | ✅ IDENTIFIER          | ✅ Match   |
| `boolean`                  | `() => true` (hack)       | ✅ BOOLEAN             | ✅ Match   |
| `interpolation`            | `() => true` (hack)       | ❌ Not used by backend | ⚠️ Orphan  |
| `jwt`                      | `validationJWT()`         | ❌ Not used by backend | ⚠️ Orphan  |
| `application/octet-stream` | `() => true`              | ❓ Unknown             | ❓ Unknown |

**Missing Format Validators:**

- `URI` - Backend specifies, frontend missing (likely uses standard JSON Schema validator)
- `HOSTNAME` - Backend specifies, frontend missing ⚠️

---

## Gap Analysis

### ✅ Widget Gaps: NONE

All backend-specified widgets have implementations:

1. **password** → Standard RJSF widget (Chakra UI provides built-in password input)
2. **textarea** → Standard RJSF widget (Chakra UI provides built-in textarea)
3. **updown** → Custom `UpDownWidget` ✅ implemented and registered

**Conclusion:** 100% widget coverage. No implementation gaps.

---

### ✅ Field Gaps: NONE

Backend doesn't specify any custom `ui:field` properties in adapter schemas.

Frontend provides custom fields (`CompactArrayField`, `MqttTransformationField`) but these are used by frontend UI schemas, not backend specifications.

**Conclusion:** N/A - Backend doesn't require custom fields for adapters.

---

### ⚠️ Format Gaps: 1 Missing Validator

| Format            | Backend Specifies? | Frontend Validator?              | Gap?         |
| ----------------- | ------------------ | -------------------------------- | ------------ |
| MQTT_TOPIC        | ✅ Yes             | ✅ Yes (`validationTopic`)       | ✅ No gap    |
| MQTT_TAG          | ✅ Yes             | ✅ Yes (`validationTag`)         | ✅ No gap    |
| MQTT_TOPIC_FILTER | ✅ Yes             | ✅ Yes (`validationTopicFilter`) | ✅ No gap    |
| IDENTIFIER        | ✅ Yes             | ✅ Yes (stub)                    | ✅ No gap    |
| BOOLEAN           | ✅ Yes             | ✅ Yes (stub)                    | ✅ No gap    |
| URI               | ✅ Yes             | ❓ Standard JSON Schema          | ℹ️ Likely OK |
| **HOSTNAME**      | ✅ Yes             | ❌ **Missing**                   | ⚠️ **GAP**   |
| UNSPECIFIED       | ✅ Yes             | N/A                              | ✅ No gap    |

**Issue Identified:**

**F-M1 (MEDIUM): Missing HOSTNAME Format Validator**

- **Impact:** Backend specifies `format = ModuleConfigField.FieldType.HOSTNAME` in Modbus, EIP, and PLC4X adapters
- **Current Behavior:** No custom validation - field accepts any string
- **Expected Behavior:** Should validate hostname/IP address format
- **Affected Adapters:** Modbus (`host` field), EIP (`host` field), PLC4X (ADS/S7 `host` field)
- **Fix:** Implement `hostname` format validator in `validation.utils.ts`
  ```typescript
  hostname: (host) => validationHostname(host) === undefined
  ```

---

## Orphan Analysis

### 🧹 Orphaned Widgets: 2

Widgets implemented but never used:

**F-L1 (LOW): ToggleWidget - Unused Custom Widget**

- **File:** `src/components/rjsf/Widgets/ToggleWidget.tsx`
- **Status:** Implemented with tests but never registered in widget registry
- **Usage:** Never imported/used anywhere
- **Impact:** Dead code, maintenance burden
- **Recommendation:** Remove or document why it exists

**F-L2 (LOW): AdapterTagSelect - Unused Custom Widget**

- **File:** `src/components/rjsf/Widgets/AdapterTagSelect.tsx`
- **Status:** Implemented with tests but never registered in widget registry
- **Usage:** Never imported/used anywhere (likely replaced by `discovery:tagBrowser`)
- **Impact:** Dead code, maintenance burden
- **Recommendation:** Remove if truly unused, or document intended use case

**NOT Orphans (DataHub Widgets):**

- `SchemaWidget` - Used in DataHub managed asset schemas
- `EntitySelectWidget` - Used in DataHub operation and northbound schemas

---

### 🧹 Orphaned Fields: 1

**F-L3 (LOW): InternalNotice - Unused Custom Field**

- **File:** `src/components/rjsf/Fields/InternalNotice.tsx`
- **Status:** Exported from `Fields/index.ts` but never imported/used
- **Usage:** No references found in codebase
- **Impact:** Dead code
- **Recommendation:** Remove or document intended use case

---

### 🧹 Orphaned Format Validators: 2

Format validators implemented but backend never uses:

1. **jwt** - Full JWT validation implemented but backend doesn't specify JWT format anywhere
2. **interpolation** - Stub validator but backend doesn't use interpolation format

**Recommendation:** Keep these as they might be used by DataHub or future features. Low maintenance cost.

---

## Issues Summary

| ID       | Severity  | Category      | Issue                       | Fix Location                    |
| -------- | --------- | ------------- | --------------------------- | ------------------------------- |
| **F-M1** | 🟡 Medium | Format Gap    | Missing HOSTNAME validator  | Frontend: `validation.utils.ts` |
| **F-L1** | 🔵 Low    | Orphan Widget | ToggleWidget never used     | Frontend: Cleanup               |
| **F-L2** | 🔵 Low    | Orphan Widget | AdapterTagSelect never used | Frontend: Cleanup               |
| **F-L3** | 🔵 Low    | Orphan Field  | InternalNotice never used   | Frontend: Cleanup               |

**Total:** 1 Medium, 3 Low

**Note on Previous Widget Coverage Issues (CW-C1, CW-H1-H6, CW-M1-M3):**
These were incorrectly identified as frontend issues. They are actually **BACKEND issues** - the backend schemas are missing `ui:widget` specifications. These will be addressed in backend tickets.

---

## Recommendations

### 1. Fix Format Gap (F-M1) - Medium Priority

**Implement HOSTNAME format validator:**

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

**Add to customLocalizer:**

```typescript
if (error.schema === 'hostname') {
  error.message = validationHostname(error.data as string)
}
```

**Add translation:**

```json
{
  "rjsf.customFormats.validation.invalidHostname": "Must be a valid hostname or IP address"
}
```

### 2. Clean Up Orphaned Components (F-L1, F-L2, F-L3) - Low Priority

**Option A: Remove Dead Code**

- Delete `ToggleWidget.tsx` and tests if truly unused
- Delete `AdapterTagSelect.tsx` and tests if truly unused
- Delete `InternalNotice.tsx` if truly unused

**Option B: Document Intended Use**

- Add comments explaining why these exist
- Create GitHub issues to track intended future usage
- Add to storybook if they're meant to be reusable components

**Recommended:** Remove unless there's a clear future use case. Reduces maintenance burden.

---

## Cross-Team Coordination

### Backend Team: NO ACTION REQUIRED ✅

All backend widget/field/format specifications are correctly supported by frontend:

- ✅ Standard widgets (`password`, `textarea`) work out of the box
- ✅ Custom widgets (`updown`) have frontend implementations
- ⚠️ HOSTNAME format missing validator (frontend issue, not backend)

**Note:** Missing widget specifications in backend schemas (e.g., OPC-UA password fields not having `ui:widget: "password"`) are **BACKEND issues** and will be addressed in backend tickets.

### Frontend Team: 1 Medium Fix, 3 Low Cleanups

1. **Implement HOSTNAME validator** (F-M1 - Medium)
2. **Remove orphaned widgets** (F-L1, F-L2 - Low)
3. **Remove orphaned field** (F-L3 - Low)

---

## Related Documents

- **Parent Task:** `.tasks/38658-adapter-jsonschema-review/TASK_BRIEF.md`
- **Remediation Report:** `.tasks/38658-adapter-jsonschema-review/REMEDIATION_REPORT.md`
- **Backend Ticket Summary:** `.tasks/38658-adapter-jsonschema-review/TICKETS_BACKEND_SUMMARY.md`

---

## Appendix: Widget Registry Configuration

### ChakraRJSForm Widget Registration

**File:** `src/components/rjsf/Form/ChakraRJSForm.tsx`

```typescript
widgets={{
  ...(!showNativeWidgets && adapterJSFWidgets),
  UpDownWidget,  // Custom numeric input with +/- buttons
}}
```

### Adapter Widget Utilities

**File:** `src/modules/ProtocolAdapters/utils/uiSchema.utils.ts`

```typescript
export const adapterJSFWidgets: RegistryWidgetsType = {
  'discovery:tagBrowser': 'text', // Disabled (issue #24369)
  'application/schema+json': JSONSchemaEditor, // DataHub only
}

export const adapterJSFFields: RegistryFieldsType = {
  compactTable: CompactArrayField,
  'mqtt:transform': MqttTransformationField,
}
```

### How Widgets Are Resolved

1. Backend specifies `"ui:widget": "updown"` in UI schema JSON
2. RJSF looks up `UpDownWidget` in registered widgets
3. If not found, RJSF falls back to Chakra UI's default widget
4. Standard widgets (`password`, `textarea`) use Chakra UI defaults

**Standard RJSF Widgets (No Custom Implementation Needed):**

- `text` (default)
- `password` (HTML type="password")
- `textarea` (HTML textarea)
- `checkbox` (HTML checkbox)
- `radio` (HTML radio group)
- `select` (HTML select dropdown)
- `range` (HTML range slider)
- `color` (HTML color picker)

**Custom HiveMQ Edge Widgets:**

- `updown` → `UpDownWidget` (numeric input with increment/decrement buttons)
- `application/schema+json` → `JSONSchemaEditor` (DataHub only)

---

**End of Analysis**
