# RJSF & Protocol Adapter Documentation Plan

**Created:** 2026-02-16
**Scope:** RJSF Guide + Protocol Adapter Architecture Documentation

---

## Context

RJSF (React JSON Schema Form) is a **core UX component** of the entire application, used for:

- Protocol adapter configuration (15+ adapter types)
- Bridge configuration (MQTT bridges)
- DataHub policy designer (schemas, scripts, behaviors, validators)
- Domain tags and managed assets
- Workspace configuration

**Current State:**

- Task documentation exists (`.tasks/RJSF_GUIDELINES.md`, `.tasks/RJSF_WIDGET_DESIGN_AND_TESTING.md`)
- Comprehensive adapter analysis completed (task 38658-adapter-jsonschema-review)
- 28 issues identified (9 frontend, 19 backend) with critical problems documented
- Implementation inconsistencies and architecture gaps

---

## Deliverables

### 1. docs/guides/RJSF_GUIDE.md

**Purpose:** Comprehensive guide for implementing RJSF forms

**Structure:**

```markdown
---
title: 'RJSF Integration Guide'
author: 'Edge Frontend Team'
last_updated: '2026-02-16'
purpose: 'Complete guide for implementing JSON Schema forms with RJSF and Chakra UI'
audience: 'Frontend Developers, AI Agents'
maintained_at: 'docs/guides/RJSF_GUIDE.md'
---

# RJSF Integration Guide

## Overview

- What is RJSF and when to use it
- Architecture overview (RJSF + Chakra UI + custom widgets)
- When NOT to use RJSF

## JSON Schema Patterns

- Basic structure and types
- Validation constraints
- Conditional schemas (if/then/else)
- Schema definitions ($ref)
- Array and object schemas

## UI Schema Patterns

- Complete uiSchema property reference (from online docs)
- Field ordering (ui:order)
- Widget selection (ui:widget)
- Field visibility (ui:options.hidden)
- Conditional display patterns
- Tabs configuration
- Enum display names (ui:enumNames)
- Description vs ui:help vs ui:description (critical distinction)
- Disabled/readonly fields

## Custom Widgets

- Available widgets inventory (from codebase analysis)
  - UpDownWidget
  - SchemaWidget
  - EntitySelectWidget
  - DataHub widgets (18 widgets from datahubRJSFWidgets)
  - Standard RJSF widgets (password, textarea, select, etc.)
- Widget vs Field distinction
- FormContext for cross-field dependencies
- Implementation pattern (from MessageTypeSelect example)
- Widget registration

## Custom Fields

- CompactArrayField
- MqttTransformationField
- When to use fields vs widgets

## Custom Templates

- ArrayFieldTemplate/ArrayFieldItemTemplate
- ObjectFieldTemplate
- FieldTemplate
- Compact templates
- BaseInputTemplate

## Validation

- Schema-based validation
- Custom format validators (mqtt-topic, mqtt-tag, identifier, etc.)
- Custom validation functions
- Cross-field validation

## Component Integration

- Drawer pattern (AdapterInstanceDrawer example)
- Form ID matching for external submit buttons
- Hide default submit button
- Card wrapping pattern
- formContext usage

## Testing Patterns

- Component test patterns
- Page Object Model (RJSFormField)
- React-select testing gotchas
- Accessibility testing requirements
- Widget testing checklist

## Common Issues & Solutions

[Table format with Issue/Cause/Solution]

- Submit button doesn't work
- Two submit buttons appear
- Conditional visibility doesn't work
- Widget doesn't apply
- Validation doesn't work
- Enum shows values instead of labels
- React-select value verification

## File Locations

[Table of key files]

- ChakraRJSForm.tsx
- Validation utils
- Widget directory
- Field directory
- Template directory
- Schema locations (api/schemas, module schemas)

## Related Documentation

- Protocol Adapter Architecture
- DataHub Architecture
- Testing Guide
- Design Guide
```

**Content Sources:**

- `.tasks/RJSF_GUIDELINES.md` (foundation, schema/uiSchema patterns)
- `.tasks/RJSF_WIDGET_DESIGN_AND_TESTING.md` (widget design, testing patterns)
- Online RJSF docs (complete uiSchema reference)
- Codebase analysis (actual implementations)
- 38658 task analysis (issues and gaps)

**Key Points:**

- Navigation-focused (file paths, not code snippets)
- Complete uiSchema property reference
- Every custom widget/field/template documented with location
- Cross-reference to Protocol Adapter Architecture
- Testing patterns from actual component tests
- Common issues from remediation report

---

### 2. docs/architecture/PROTOCOL_ADAPTER_ARCHITECTURE.md

**Purpose:** Architecture document for protocol adapter configuration system

**Structure:**

```markdown
---
title: 'Protocol Adapter Architecture'
author: 'Edge Frontend Team'
last_updated: '2026-02-16'
purpose: 'Architecture and data flow for protocol adapter configuration using RJSF'
audience: 'Frontend Developers, Backend Developers, AI Agents'
maintained_at: 'docs/architecture/PROTOCOL_ADAPTER_ARCHITECTURE.md'
---

# Protocol Adapter Architecture

## Overview

- What are protocol adapters
- Backend-driven schema architecture
- 15+ adapter types (list with brief description)

## Architecture Flow

[Mermaid diagram showing flow]
Backend OpenAPI → Frontend API Client → Schema Registry → RJSF Form → User Input → Validation → API Submission

## Data Flow

1. Backend provides JSON Schema + UI Schema via OpenAPI
2. Frontend fetches adapter types (configSchema, uiSchema, capabilities)
3. AdapterInstanceDrawer renders ChakraRJSForm
4. User fills form
5. RJSF validates against schema
6. Custom validation (unique ID check)
7. Submit to backend API

## Code Structure

[File path table]

- AdapterInstanceDrawer.tsx (main drawer)
- useGetAdapterTypes.ts (API hook)
- uiSchema.utils.ts (schema transformations)
- validation-utils.ts (custom validation)
- Test utils (adapters/ mocks)

## Schema Architecture

### Backend Schema Generation

- Java annotations (@ModuleConfigField)
- JSON Schema generation
- UI Schema JSON files
- Format types (MQTT_TOPIC, HOSTNAME, IDENTIFIER, etc.)
- Widget specifications (ui:widget)

### Frontend Schema Consumption

- OpenAPI client generation
- Mock schemas for testing
- Schema transformation (getRequiredUiSchema)
- formContext injection

## Adapter Registry

- How adapters are registered
- Discovery capability
- Logo/branding
- Tag configuration

## Custom Widgets for Adapters

- updown (numeric fields)
- password (sensitive fields)
- textarea (large text)
- JSONSchemaEditor (DataHub only)
- Disabled: discovery:tagBrowser (issue #24369)

## Validation Strategy

- Schema-based validation (required, min/max, format)
- Custom format validators (MQTT topics, tags, identifiers)
- Unique adapter ID validation
- Cross-field validation (minValue < maxValue)

## Known Issues & Gaps

[Reference to 38658 analysis]

### Critical Issues

- File adapter tag schema wrong (F-C1)
- Databases getTrustCertificate() bug (B-C1)

### Schema Validation Issues

[Table from remediation report]

- Port constraints wrong
- Missing conditional visibility
- Enum display names missing

### Frontend Implementation Gaps

- Missing HOSTNAME format validator (F-M1)
- Orphaned widgets (ToggleWidget, AdapterTagSelect, InternalNotice)

### Backend Specification Gaps

- Missing ui:widget specifications (password masking, updown for ports)
- Missing conditional field dependencies
- Missing enumNames for technical enums

### Remediation Status

- Frontend automated fixes available
- Backend requires manual updates
- Cross-team coordination needed

## Testing

- Mock schemas in **test-utils**/adapters/
- AdapterInstanceDrawer component tests
- E2E adapter configuration tests
- See: TESTING_GUIDE.md, RJSF_GUIDE.md

## Common Issues & Solutions

[Table format]

- Schema doesn't load
- Validation doesn't work
- Custom widget not applying
- Conditional fields not hiding
- Enum showing raw values

## Related Documentation

- RJSF Guide (implementation details)
- Testing Guide (testing patterns)
- Design Guide (UI patterns)
- Technical Stack (dependencies)

## External References

- Task 38658 analysis: `.tasks/38658-adapter-jsonschema-review/`
  - REMEDIATION_REPORT.md (28 issues documented)
  - CUSTOM_WIDGET_COVERAGE_ANALYSIS.md
  - CONDITIONAL_VISIBILITY_ANALYSIS.md
  - ENUM_DISPLAY_NAMES_AUDIT.md
```

**Content Sources:**

- `.tasks/38658-adapter-jsonschema-review/` (comprehensive analysis)
- AdapterInstanceDrawer.tsx (actual implementation)
- uiSchema.utils.ts (schema transformations)
- Backend adapter structure (from task docs)
- REMEDIATION_REPORT.md (all 28 issues)

**Key Points:**

- Explains backend-driven architecture
- Clear data flow with diagram
- Documents all 28 issues from 38658 analysis
- Cross-references to remediation report for details
- Architecture decisions and rationale
- Testing strategy

---

## Content Migration Strategy

### From .tasks/ to docs/

**RJSF_GUIDELINES.md → RJSF_GUIDE.md:**

- ✅ Keep: Schema patterns, uiSchema patterns, validation, best practices
- ✅ Enhance: Add complete uiSchema reference from online docs
- ✅ Add: Custom widgets inventory from codebase
- ✅ Add: Custom fields/templates from codebase
- ✅ Condense: Remove verbose examples, keep file paths
- ❌ Remove: Internal task context, line number references

**RJSF_WIDGET_DESIGN_AND_TESTING.md → RJSF_GUIDE.md (Testing section):**

- ✅ Keep: Widget implementation checklist, testing patterns
- ✅ Keep: React-select integration requirements
- ✅ Keep: Page Object Model patterns
- ✅ Condense: Merge into testing section of guide
- ❌ Remove: Task-specific context

**38658 Task Analysis → PROTOCOL_ADAPTER_ARCHITECTURE.md:**

- ✅ Reference: Link to task directory for full analysis
- ✅ Summarize: Key findings (28 issues breakdown)
- ✅ Document: Critical issues affecting architecture
- ✅ Explain: Schema validation gaps, widget coverage
- ❌ Don't duplicate: Full remediation report (keep in tasks/)

---

## Document Relationships

```
RJSF_GUIDE.md (How to implement)
    ↓ used by
PROTOCOL_ADAPTER_ARCHITECTURE.md (Adapter-specific architecture)
    ↓ references
.tasks/38658-adapter-jsonschema-review/ (Detailed analysis)
    ↓ cross-references
TESTING_GUIDE.md (General testing patterns)
DESIGN_GUIDE.md (UI component patterns)
TECHNICAL_STACK.md (Dependencies: @rjsf/chakra-ui, validator-ajv8)
```

---

## Widget & Field Inventory

### Custom Widgets (Complete List)

**Protocol Adapters:**

- UpDownWidget (registered in ChakraRJSForm)
- password, textarea, select (standard RJSF/Chakra)
- ToggleWidget ⚠️ (orphaned - never used)
- AdapterTagSelect ⚠️ (orphaned - replaced by discovery:tagBrowser which is disabled)

**DataHub:**

- JSONSchemaEditor (application/schema+json)
- JavascriptEditor (text/javascript)
- ProtoSchemaEditor (application/octet-stream)
- FunctionCreatableSelect (datahub:function-selector)
- MetricCounterInput (datahub:metric-counter)
- VersionManagerSelect (datahub:version)
- MessageInterpolationTextArea (datahub:message-interpolation)
- MessageTypeSelect (datahub:message-type)
- TransitionSelect (datahub:transition-selector)
- BehaviorModelSelect (datahub:behavior-model-selector-radio)
- BehaviorModelSelectDropdown (datahub:behavior-model-selector, datahub:behavior-model-selector-dropdown)
- BehaviorModelReadOnlyDisplay (datahub:behavior-model-readonly)
- ScriptNameCreatableSelect (datahub:function-name)
- SchemaNameCreatableSelect (datahub:schema-name)
- ScriptNameSelect (datahub:function-name-select)
- SchemaNameSelect (datahub:schema-name-select)
- AdapterSelect (edge:adapter-selector)

### Custom Fields

- CompactArrayField (compactTable) - Adapter arrays
- MqttTransformationField (mqtt:transform) - DataHub transforms
- InternalNotice ⚠️ (orphaned - never used)

### Custom Templates

- ArrayFieldTemplate/ArrayFieldItemTemplate
- ObjectFieldTemplate
- FieldTemplate
- BaseInputTemplate
- CompactArrayFieldTemplate/CompactArrayFieldItemTemplate
- CompactObjectFieldTemplate
- CompactFieldTemplate
- CompactBaseInputTemplate
- DescriptionFieldTemplate
- TitleFieldTemplate
- ErrorListTemplate

### Format Validators

- mqtt-topic (validationTopic)
- mqtt-tag (validationTag)
- mqtt-topic-filter (validationTopicFilter)
- identifier (stub)
- boolean (stub hack)
- interpolation ⚠️ (orphaned)
- jwt ⚠️ (orphaned)
- **Missing: hostname** (F-M1 - needed by Modbus, EIP, PLC4X)

---

## Critical Issues to Document

### F-C1: File Adapter Tag Schema Wrong

**Impact:** Broken functionality - tag configuration shows HTTP fields
**Location:** `src/__test-utils__/adapters/file.ts`
**Status:** Automated fix available

### B-C1: Databases getTrustCertificate() Returns Wrong Field

**Impact:** Runtime logic bug - method returns `encrypt` instead of `trustCertificate`
**Location:** Backend DatabasesAdapterConfig.java
**Status:** Backend fix required

### F-M1: Missing HOSTNAME Format Validator

**Impact:** No validation for hostname fields in Modbus, EIP, PLC4X adapters
**Affected:** 3 adapter types
**Status:** Frontend implementation needed

### Missing Conditional Visibility

**Impact:** Irrelevant fields shown (e.g., trustCertificate when encrypt=false)
**Affected:** Databases (encrypt→trustCertificate), OPC-UA (tls.enabled→keystore/truststore)
**Status:** Backend schema dependencies needed

### Missing Enum Display Names

**Impact:** Technical values shown instead of user-friendly labels
**Affected:** OPC-UA security policy, Databases type, BACnet object/property types
**Status:** Backend enumNames needed

---

## Completion Checklist

### RJSF_GUIDE.md

- [ ] YAML frontmatter
- [ ] Overview and when to use RJSF
- [ ] Complete JSON Schema patterns
- [ ] Complete uiSchema reference (from online docs)
- [ ] Custom widgets inventory (all 18+ widgets with file paths)
- [ ] Custom fields inventory (3 fields)
- [ ] Custom templates inventory (11 templates)
- [ ] Validation patterns (schema + custom)
- [ ] Component integration pattern
- [ ] Testing patterns (component + POM)
- [ ] Common issues table
- [ ] File locations table
- [ ] Cross-references to architecture docs

### PROTOCOL_ADAPTER_ARCHITECTURE.md

- [ ] YAML frontmatter
- [ ] Overview and adapter types list
- [ ] Architecture flow diagram
- [ ] Data flow explanation
- [ ] Code structure table
- [ ] Backend schema generation (Java annotations)
- [ ] Frontend schema consumption
- [ ] Adapter registry
- [ ] Custom widgets for adapters
- [ ] Validation strategy
- [ ] Known issues section (28 issues summarized)
- [ ] Critical issues detailed (F-C1, B-C1, F-M1)
- [ ] Schema validation gaps table
- [ ] Remediation status
- [ ] Testing strategy
- [ ] Common issues table
- [ ] Cross-references to RJSF guide and task analysis

### Documentation Quality

- [ ] No TODO markers
- [ ] No broken links
- [ ] File paths instead of code snippets
- [ ] Tables for scannable information
- [ ] WCAG AA diagrams
- [ ] Cross-references working
- [ ] Related Documentation sections complete
- [ ] Consistent with documentation philosophy

---

## Questions for User

1. **Scope confirmation:** Should PROTOCOL_ADAPTER_ARCHITECTURE.md also cover Bridge configuration, or focus only on protocol adapters?

2. **Task reference policy:** Should we reference `.tasks/38658-adapter-jsonschema-review/` or create a permanent `docs/analysis/ADAPTER_SCHEMA_ANALYSIS.md`?

3. **Orphaned widgets:** Document them as "unused" or propose removal in the docs?

4. **Backend issues:** How much detail in architecture doc vs "see remediation report"?

5. **Widget coverage:** Should RJSF_GUIDE include DataHub-specific widgets or only adapter widgets? (I'm inclined to document ALL widgets since RJSF is shared infrastructure)

---

## Estimated Content Size

**RJSF_GUIDE.md:** ~800-1000 lines (comprehensive guide with all widgets/fields/templates)
**PROTOCOL_ADAPTER_ARCHITECTURE.md:** ~500-600 lines (architecture + issues summary)

**Total:** ~1,300-1,600 lines of new documentation

**Comparison:**

- WORKSPACE_ARCHITECTURE.md: 434 lines (was 1613)
- DATAHUB_ARCHITECTURE.md: 340 lines (was 948)
- TESTING_GUIDE.md: ~400 lines
- CYPRESS_GUIDE.md: ~500 lines

---

**Ready to proceed with document creation?**
