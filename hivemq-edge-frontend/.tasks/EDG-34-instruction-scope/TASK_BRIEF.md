# Task DG-34: Instruction Scope / Mapping Ownership Display

**Task ID:** DG-34
**Task Name:** instruction-scope
**Branch:** `refactor/38943-mapping-ownership-overall`
**Status:** 🔍 Planning / Design Phase
**Created:** 2026-02-23
**Parent Task:** 38943 (Mapping Ownership Overall)

---

## Objective

In the data combiner's mapping editor, a rendered instruction currently shows only the source property name (e.g., `value`) with no indication of which tag or topic filter it belongs to, nor which adapter owns that tag. When multiple sources expose properties with identical names (e.g., all tags have a `value` field), the UI becomes ambiguous and unusable.

Revise the visual presentation of a mapped instruction to clearly communicate ownership, without breaking drag-and-drop, accessibility, or readability.

---

## Problem Statement

### What users see today

```
┌──────────────────────────────────────────────────────────────────┐
│ CardHeader: 🟢 value          ← destination property            │
│                                                                  │
│ CardBody:                                                        │
│   ┌──────────────────────┐  [🗑]  [● Matching]                  │
│   │ value                │                                       │
│   └──────────────────────┘                                       │
└──────────────────────────────────────────────────────────────────┘
```

When three tags each have a `value` field, all three instructions display identically:

- Destination `value` ← source `value`
- Destination `value` ← source `value`
- Destination `value` ← source `value`

Which source is mapped to which destination is completely opaque.

### What data is available but unused

`Instruction.sourceRef: DataIdentifierReference` is stored on every mapped instruction and contains:

| Field   | Example      | Meaning                               |
| ------- | ------------ | ------------------------------------- |
| `id`    | `my/tag/t1`  | Tag name or topic filter              |
| `type`  | `TAG`        | `TAG`, `TOPIC_FILTER`, `PULSE_ASSET`  |
| `scope` | `my-adapter` | Adapter ID (TAG only; null otherwise) |

This data is never read in the rendering path of `MappingInstruction.tsx`.

---

## Context

### Relevant background (task 38943)

Task 38943 established the ownership model: `instruction.sourceRef` now reliably carries scope information for all mapped instructions. The ownership data is present and correct — only the visual layer is missing.

### Files involved

| File                                                                               | Role                                                                            |
| ---------------------------------------------------------------------------------- | ------------------------------------------------------------------------------- |
| `src/components/rjsf/MqttTransformation/components/mapping/MappingInstruction.tsx` | **Primary change target** — renders each mapped instruction                     |
| `src/components/rjsf/MqttTransformation/components/MappingInstructionList.tsx`     | Renders the list of instructions                                                |
| `src/components/rjsf/MqttTransformation/components/schema/PropertyItem.tsx`        | Renders a property row (source or destination)                                  |
| `src/components/MQTT/EntityTag.tsx`                                                | `PLCTag` (blue), `TopicFilter` (orange) — existing ownership display components |
| `src/modules/Mappings/combiner/DestinationSchemaLoader.tsx`                        | Hosts the `MappingInstructionList`                                              |
| `src/modules/Mappings/combiner/CombinedSchemaLoader.tsx`                           | Source panel — already uses `PLCTag`/`TopicFilter` per source                   |

### Existing ownership vocabulary

The source panel already uses `PLCTag` (blue chip) and `TopicFilter` (orange chip) as section headers above each schema browser. The visual language exists — it just isn't carried through to the destination/mapping side.

---

## Requirements

1. Ownership (tag name + adapter scope for TAG type, topic filter name for TOPIC_FILTER) must be clearly visible on each mapped instruction — not hidden behind hover or interaction
2. The mapping component supports drag-and-drop using Pragmatic DnD; the `Box` with `dropTargetRef` is the DnD target — its DOM binding must not be disturbed
3. Accessibility standards must be maintained (ARIA roles, keyboard navigation, axe checks)
4. The solution should reuse existing `PLCTag` / `TopicFilter` components for visual consistency with the source panel
5. When no instruction is mapped (drop zone is empty), behaviour is unchanged

---

## Success Criteria

- [ ] A mapped instruction shows which tag or topic filter the source property belongs to
- [ ] For TAG type, the adapter scope (`sourceRef.scope`) is also visible
- [ ] Two instructions with the same source path but different owners are visually distinguishable
- [ ] All existing Cypress tests continue to pass
- [ ] New tests added for the ownership display (with `sourceRef` present and absent)
- [ ] `cy.checkAccessibility()` passes for the updated component

---

## Documents

| Document                                   | Purpose                                       | Status |
| ------------------------------------------ | --------------------------------------------- | ------ |
| [TASK_BRIEF.md](./TASK_BRIEF.md)           | This file — context and requirements          | ✅     |
| [DESIGN_ANALYSIS.md](./DESIGN_ANALYSIS.md) | Candidate solutions, comparison, testing plan | ✅     |

---

## Related Tasks

- **38943** — Mapping Ownership Overall (parent, established `sourceRef` data model) ✅ Complete
- **38936** — Tag Reference Scope (prerequisite) ✅ Complete
