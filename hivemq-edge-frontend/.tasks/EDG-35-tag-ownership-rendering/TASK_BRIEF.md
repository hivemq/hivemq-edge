# Task EDG-35: Tag Ownership Rendering

**Task ID:** EDG-35
**Task Name:** tag-ownership-rendering
**Branch:** `refactor/DG-35/tag-ownership-rendering` (TBC)
**Status:** 🔍 Planning / Design Phase
**Created:** 2026-02-24
**Parent Task:** 38943 (Mapping Ownership Overall)

---

## Objective

Wherever a PLC tag is rendered in the UI, show the owning adapter when the context is
ambiguous — i.e. when multiple tags with the same name from different adapters could appear
on the same page or panel. When context is unambiguous (single-adapter scope), the plain tag
name is sufficient.

This is a codebase-wide sweep of every `PLCTag` / `TopicFilter` / `AssetTag` usage, applying
the same ownership-display convention established by EDG-34 for `MappingInstruction`.

---

## Convention (established by EDG-34)

The canonical ownership string is built by `formatOwnershipString(ref: DataIdentifierReference)`:

```
{adapter-id} :: {tag/name/path}   ← when scope is set
{tag/name/path}                   ← when scope is null
```

The helper lives in `src/components/MQTT/topic-utils.ts`. All new usages must reuse it.

---

## Problem Statement

EDG-34 solved ownership display for `MappingInstruction` (the mapped-instruction drop zone).
The following locations were **not addressed** and still show plain tag IDs with no ownership
context, even when multiple adapters can contribute same-named tags to the same view.

### Example — source panel with two adapters

When `my-adapter` and `opcua-prod` both export a tag named `value`, the source schema browser
shows two identical headings:

```
[🏷 value]   ← which adapter?
[🏷 value]   ← which adapter?
```

The user cannot tell which schema belongs to which adapter.

---

## Files Involved

| File                                                           | Role                                           | Ambiguity                                   | Priority |
| -------------------------------------------------------------- | ---------------------------------------------- | ------------------------------------------- | -------- |
| `src/components/rjsf/MqttTransformation/JsonSchemaBrowser.tsx` | Source schema panel heading                    | HIGH — multi-adapter combiner               | 🔴       |
| `src/modules/Mappings/combiner/CombinedSchemaLoader.tsx`       | Error fallback heading in same panel           | HIGH — same context                         | 🔴       |
| `src/modules/Mappings/combiner/CombinedEntitySelect.tsx`       | Multi-select chips for selected sources        | MEDIUM — same-name tag from two adapters    | 🟠       |
| `src/modules/Mappings/combiner/DataCombiningTableField.tsx`    | Sources column in combiner summary table       | MEDIUM — co-sources from different adapters | 🟠       |
| `src/modules/Workspace/components/filters/FilterTopics.tsx`    | Selected-value chip (secondary: widget absent) | LOW — no scope data available               | 🟡       |

### Already correct — no changes needed

| File                                                                               | Reason                                            |
| ---------------------------------------------------------------------------------- | ------------------------------------------------- |
| `src/components/rjsf/MqttTransformation/components/mapping/MappingInstruction.tsx` | Done in EDG-34                                    |
| `src/components/rjsf/MqttTransformation/components/ListMappings.tsx`               | Single-adapter southbound context                 |
| `src/modules/Device/components/TagTableField.tsx`                                  | Per-adapter tag management, one adapter at a time |
| `src/modules/Device/components/TagSchemaPanel.tsx`                                 | Single-tag schema view; unambiguous               |
| `src/modules/Workspace/components/parts/MappingBadge.tsx`                          | Per-adapter workspace node; unambiguous           |
| `src/modules/Workspace/components/nodes/NodeAdapter.tsx`                           | Shows TOPIC type only, not TAG                    |

---

## Requirements

1. Every `PLCTag` rendering in an ambiguous multi-adapter context must show
   `{scope} :: {tag/name}` when `scope` is set.
2. When scope is null or the context is unambiguously single-adapter, plain tag name is shown
   (graceful fallback — no change to existing behaviour).
3. The `formatOwnershipString` utility must be reused; no duplicated formatting logic.
4. All changed components must have updated or new Cypress component tests covering:
   - Scope present → ownership string visible
   - Scope absent → plain tag name (fallback)
5. Accessibility (`cy.checkAccessibility()`) must pass for every changed component.
6. All existing tests must continue to pass.

---

## Success Criteria

- [ ] `JsonSchemaBrowser` shows `{scope} :: {id}` heading for TAG sources when scope is set
- [ ] `CombinedSchemaLoader` error fallback shows the same format
- [ ] `CombinedEntitySelect` multi-value chips show ownership for same-named tags from different adapters
- [ ] `DataCombiningTableField` sources column shows ownership where scope is reconstructable
- [ ] `FilterTopics` selected chips use the EntityTag widget (visual consistency; no scope data available yet)
- [ ] All Cypress tests pass (existing + new)
- [ ] `cy.checkAccessibility()` passes for each changed component

---

## Documents

| Document                                   | Purpose                                        | Status |
| ------------------------------------------ | ---------------------------------------------- | ------ |
| [TASK_BRIEF.md](./TASK_BRIEF.md)           | This file — context and requirements           | ✅     |
| [DESIGN_ANALYSIS.md](./DESIGN_ANALYSIS.md) | Per-location analysis, fix approach, test plan | ✅     |

---

## Related Tasks

- **EDG-34** — Instruction Scope (applied ownership to `MappingInstruction`) ✅ Complete
- **38943** — Mapping Ownership Overall (parent) ✅ Complete
- **38936** — Tag Reference Scope (prerequisite) ✅ Complete
