# DG-34: Design Analysis — Instruction Ownership Display

**Status:** Draft — Awaiting design decision
**Date:** 2026-02-23
**Investigation test:** `src/components/rjsf/MqttTransformation/components/mapping/MappingInstruction.investigation.cy.tsx`

---

## 1. Root Cause

`MappingInstruction.tsx` renders the mapped source at line 190–196:

```tsx
{instruction?.source ? (
  <Code>{formatPath(fromJsonPath(instruction.source))}</Code>
) : (
  <Text ...>{placeholder}</Text>
)}
```

`instruction.source` is a raw JSON path string (`$.value`). It carries no ownership context.

The full ownership chain is stored in `instruction.sourceRef: DataIdentifierReference` — but this field is **never read** during rendering. It is only written during a drop event (lines 85–87 and 167–173).

---

## 2. Visual Evidence — Current State

Screenshots captured via investigation Cypress tests at 3 viewport widths. Key screenshots below.

### The core problem: three identical rows

![Ambiguity at full screen](./screenshots/DG-34_F_ambiguity-list_full-screen.png)

Three destination fields (`VALUE`, `VALUE2`, `VALUE3`) — all mapped to the source `value` — look completely identical. The sources are actually: a TAG on `my-adapter`, a TAG on `opcua-production-adapter`, and a TOPIC_FILTER. There is no way to distinguish them.

![Ambiguity at lg-panel](./screenshots/DG-34_F_ambiguity-list_lg-panel.png)

At the `lg` drawer size (504px), the layout is identical — same problem, more compact.

### Single instruction: `value` shows but ownership is invisible

![Single TAG instruction, full screen](./screenshots/DG-34_B_tag-short_full-screen.png)

At full-screen width the drop zone is enormous — there is substantial space available to the left of `value`. All of that space carries no information.

![Single TAG instruction, lg-panel](./screenshots/DG-34_B_tag-short_lg-panel.png)

At lg-panel (504px) the layout still has a reasonable drop zone width (~165px).

### Empty state (baseline for comparison)

![Empty required field, full screen](./screenshots/DG-34_A_empty-required_full-screen.png)

The placeholder `Drag a source property here` occupies the drop zone. The `Required` badge (red) is clipped at the right at full-screen viewport — this is a pre-existing issue, not introduced by this task.

---

## 3. Responsive Layout Analysis

### Layout anatomy

The `CardBody` uses `display="flex" flexDirection="row" gap={2}`:

```
[Box flex=3 drop zone] [ButtonGroup xs ~32px] [Alert w="140px"]
```

### Behaviour at each viewport

| Viewport              | Drop zone width (approx) | Status badge                      | Ownership label space |
| --------------------- | ------------------------ | --------------------------------- | --------------------- |
| Full screen (1280px+) | 800–1100px               | Clipped off-screen (pre-existing) | Ample — no constraint |
| Medium panel (800px)  | ~500px                   | Fully visible                     | Comfortable           |
| LG drawer (504px)     | ~165px                   | Fully visible                     | Tight but feasible    |

### Critical finding at full screen

The `Alert w="140px"` status badge is cut off at viewport widths where the component fills the full available width (full-screen drawer). This is pre-existing. The drop zone `flex=3` takes the surplus and becomes very wide. **At full-screen width there is abundant space for ownership information.**

### Critical finding at lg-panel (504px)

The drop zone is approximately 165px wide. A `PLCTag` chip with tag name `my/tag/t1` is approximately 100px wide. This fits on a single line alongside `value` in a VStack or as a separate row. **Long tag IDs will truncate within the chip** (the `TagLabel` inside `PLCTag` uses `overflow: hidden` by default via Chakra). This is acceptable — the chip provides enough context to distinguish sources.

---

## 4. Existing Ownership Vocabulary

The source panel already uses these components as section headers above each schema browser:

| Component     | `colorScheme` | Usage                     |
| ------------- | ------------- | ------------------------- |
| `PLCTag`      | `blue`        | TAG type sources          |
| `TopicFilter` | `orange`      | TOPIC_FILTER type sources |
| `AssetTag`    | `teal`        | PULSE_ASSET type sources  |

These live in `src/components/MQTT/EntityTag.tsx`. Their icon + color coding already teaches users "blue = tag, orange = topic filter". Reusing them on the destination side creates a direct visual link across the two panels.

### Ownership string format (EDG-35)

Task EDG-35 establishes the canonical format for displaying a tag with its ownership context. When scope context is needed, the `tagTitle` string is extended using the `::` separator (with surrounding whitespace), mirroring the visual role that `/` plays inside tag path segments:

```
{adapter-id} :: {tag/name/path}
```

Examples:

| `sourceRef`                                            | `tagTitle` passed to `PLCTag`                           |
| ------------------------------------------------------ | ------------------------------------------------------- |
| `{ id: "my/tag/t1", scope: "my-adapter" }`             | `"my-adapter :: my/tag/t1"`                             |
| `{ id: "my/tag/t1", scope: null }` (no scope)          | `"my/tag/t1"`                                           |
| `{ id: "factory/+/sensors/temp", type: TOPIC_FILTER }` | `"factory/+/sensors/temp"` (no scope for topic filters) |

**Consequence for this task:** there is no separate scope `<Badge>` component. The scope is embedded in the tag label string itself. The `PLCTag` and `TopicFilter` components receive the full contextual string as their `tagTitle` prop.

---

## 5. Candidate Solutions

### Option 1 — Ownership stacked inside the drop zone

Replace the single `<Code>` with a `<VStack>` when `instruction.sourceRef` is present. The drop zone `Box` itself is unmodified.

**Visual result (mapped, TAG type with scope):**

```
┌─── dashed border ──────────────────────────────────────┐
│  [🏷 my-adapter :: my/tag/t1]                          │  ← PLCTag, scope in title
│  value                                                  │  ← existing <Code>
└────────────────────────────────────────────────────────┘
```

**Visual result (mapped, TOPIC_FILTER type):**

```
┌─── dashed border ──────────────────────────────────────┐
│  [⟨ factory/+/sensors/temp ⟩]                          │  ← TopicFilter (orange), no scope
│  value                                                  │  ← existing <Code>
└────────────────────────────────────────────────────────┘
```

**Visual result (empty, no instruction):**

```
┌─── dashed border ──────────────────────────────────────┐
│  Drag a source property here                           │  ← unchanged
└────────────────────────────────────────────────────────┘
```

**Visual result (instruction present, no `sourceRef` — legacy):**

```
┌─── dashed border ──────────────────────────────────────┐
│  value                                                  │  ← unchanged (graceful fallback)
└────────────────────────────────────────────────────────┘
```

**Implementation sketch:**

```tsx
// Helper: build the tagTitle string following the EDG-35 :: pattern
const getOwnershipLabel = (ref: DataIdentifierReference): string =>
  ref.scope ? `${ref.scope} :: ${ref.id}` : ref.id

{instruction?.source ? (
  <VStack align="start" gap={1}>
    {instruction.sourceRef && (
      <>
        {instruction.sourceRef.type === DataIdentifierReference.type.TAG && (
          <PLCTag tagTitle={getOwnershipLabel(instruction.sourceRef)} size="sm" />
        )}
        {instruction.sourceRef.type === DataIdentifierReference.type.TOPIC_FILTER && (
          <TopicFilter tagTitle={instruction.sourceRef.id} size="sm" />
        )}
        {instruction.sourceRef.type === DataIdentifierReference.type.PULSE_ASSET && (
          <AssetTag tagTitle={instruction.sourceRef.id} size="sm" />
        )}
      </>
    )}
    <Code>{formatPath(fromJsonPath(instruction.source))}</Code>
  </VStack>
) : (
  <Text ...>{placeholder}</Text>
)}
```

**DnD impact:** None. `dropTargetForElements` binds to the outer `Box`; its children are irrelevant to the drop registration.

**Accessibility:** The ownership row is presentational within the existing `role="group"` + `aria-label` container. The drop zone's ARIA label (`t('rjsf.MqttTransformationField.instructions.dropzone.role')`) already describes the zone's purpose. No ARIA changes required; the new badges are decorative within a labelled group.

**Responsive at lg-panel (504px):** The VStack adds ~28px height. The tag ID will truncate inside the Chakra `TagLabel`. At very long IDs (`industrial/plant/floor2/machine-a/sensor-12`) the chip will truncate to the available ~165px drop zone width. This is acceptable — partial tag names are still distinguishing.

**Pros:**

- Ownership co-located with the mapped value — natural reading flow
- Direct visual echo of the source panel's PLCTag/TopicFilter headers
- Graceful fallback for legacy data (no `sourceRef`)
- Works at all viewport sizes

**Cons:**

- Adds ~28px height per mapped instruction (two lines instead of one)
- Long tag IDs truncate in the chip at lg-panel width

---

### Option 2 — Ownership row above the drop zone row

Keep the drop zone content entirely unchanged. Add a conditional row between `CardHeader` and the existing `CardBody` flex row. `CardBody` changes from `flexDirection="row"` to `flexDirection="column"`.

**Visual result (mapped, TAG with scope):**

```
[CardHeader]  Tr  VALUE
─────────────────────────────────────────────────────────
[Ownership row]  [🏷 my-adapter :: my/tag/t1]
[Drop zone row]  ┌─ dashed ─────┐  [🗑]  [✓ Matching]
                 │ value        │
                 └──────────────┘
```

**Implementation sketch:**

```tsx
// Helper: build the tagTitle string following the EDG-35 :: pattern
const getOwnershipLabel = (ref: DataIdentifierReference): string =>
  ref.scope ? `${ref.scope} :: ${ref.id}` : ref.id

<CardBody display="flex" flexDirection="column" gap={2}>
  {instruction?.sourceRef && (
    <Box>
      {instruction.sourceRef.type === DataIdentifierReference.type.TAG && (
        <PLCTag tagTitle={getOwnershipLabel(instruction.sourceRef)} size="sm" />
      )}
      {instruction.sourceRef.type === DataIdentifierReference.type.TOPIC_FILTER && (
        <TopicFilter tagTitle={instruction.sourceRef.id} size="sm" />
      )}
      {instruction.sourceRef.type === DataIdentifierReference.type.PULSE_ASSET && (
        <AssetTag tagTitle={instruction.sourceRef.id} size="sm" />
      )}
    </Box>
  )}
  <HStack gap={2}>
    {/* existing drop zone Box, ButtonGroup, Alert — completely unchanged */}
  </HStack>
</CardBody>
```

**DnD impact:** Zero. The drop target `Box` is structurally and referentially unmodified.

**Accessibility:** The ownership row sits above the drop zone in the DOM. It can carry `aria-label` for screen readers. The existing drop zone `role="group"` and `aria-label` are unchanged.

**Responsive at lg-panel (504px):** The ownership row spans the full card width (~460px), giving significantly more room for long tag names than Option 1's constrained drop zone width. Long IDs are far less likely to truncate.

**Pros:**

- Drop zone box is 100% unmodified — lowest DnD regression risk
- Full card width for ownership row → long tag IDs are less likely to truncate
- Cleanest semantic separation: context row / action row
- `flexWrap="wrap"` on the ownership HStack handles very long IDs gracefully

**Cons:**

- Ownership row and the mapped value it contextualises are in separate visual areas (two lines apart under the header)
- The extra `flexDirection="column"` change on CardBody is the only structural change, but it does affect all child spacing

---

### Option 3 — Integrated fully-qualified string

Replace `<Code>value</Code>` with a single fully-qualified path string that embeds ownership directly in the text, with no additional layout elements. The drop zone content remains a single visual unit.

**Concept:**

The field path is extended leftward with the ownership segments, all on one line:

```
my-adapter :: my/tag/t1 › value
```

- `::` separates ownership levels (consistent with EDG-35: adapter → tag)
- `›` (U+203A, single right-pointing angle quotation) separates the ownership chain from the field name inside the source schema — a visually lighter separator that signals "within this source"
- The `›` could alternatively be `/` (consistent with path notation already used inside tag names) or a second `::` for full consistency

**Visual result at full screen (mapped, TAG with scope):**

```
┌─── dashed border ───────────────────────────────────────────────────┐
│  my-adapter :: my/tag/t1 › value                                    │
└─────────────────────────────────────────────────────────────────────┘
```

**Visual result at lg-panel (mapped, long ID):**

```
┌─── dashed border ────────────────────────┐
│  ...oor2/machine-a :: sensor-12 › value  │  ← left-truncated
└──────────────────────────────────────────┘
```

**Visual result (TOPIC_FILTER, no scope):**

```
┌─── dashed border ──────────────────────────────────┐
│  factory/+/sensors/temperature › value             │
└────────────────────────────────────────────────────┘
```

**Visual result (empty or legacy — no `sourceRef`):**

```
┌─── dashed border ──────────────────────────────────┐
│  value                                             │  ← unchanged fallback
└────────────────────────────────────────────────────┘
```

**Overflow strategy — truncate from the LEFT:**

Standard CSS `text-overflow: ellipsis` truncates from the right — which would lose `value` at narrow widths. The correct strategy is left-truncation to preserve the field name:

```tsx
// Inside the drop zone, replace the <Code> ternary:
{instruction?.source ? (
  <Code
    display="block"
    overflow="hidden"
    whiteSpace="nowrap"
    textOverflow="ellipsis"
    direction="rtl"        // ← clips from the left
    unicodeBidi="plaintext" // ← keeps character order correct
    title={fullQualifiedLabel} // ← native tooltip with full string
  >
    {fullQualifiedLabel}
  </Code>
) : (
  <Text ...>{placeholder}</Text>
)}
```

With `direction: rtl` the overflow clips at the left edge, preserving the rightmost characters (`› value`). `unicode-bidi: plaintext` ensures the Latin characters within the string still render left-to-right — only the overflow direction is reversed.

**Building the fully-qualified label:**

```tsx
const getFullyQualifiedLabel = (source: string, sourceRef?: DataIdentifierReference): string => {
  const field = formatPath(fromJsonPath(source))
  if (!sourceRef) return field
  const ownership =
    sourceRef.type === DataIdentifierReference.type.TAG && sourceRef.scope
      ? `${sourceRef.scope} :: ${sourceRef.id}`
      : sourceRef.id
  return `${ownership} › ${field}`
}
```

**DnD impact:** Zero. The outer `Box` is completely unmodified.

**Accessibility:** The `title` attribute on the `<Code>` provides the full untruncated string for assistive technologies and as a native tooltip. The `role="group"` + `aria-label` on the drop zone are unchanged.

**Responsive at lg-panel (504px):** Left-truncation ensures `› value` is always visible. The ownership prefix truncates instead. At ~165px drop zone width:

- Short IDs (`my/tag/t1 › value`) — fits fully
- Long IDs (`...machine-a :: sensor-12 › value`) — ownership prefix truncated, field preserved

**Pros:**

- No new layout elements — the drop zone remains a single line at the same height as today
- Zero height increase per instruction row — most compact of all options
- Natural reading of the full path as a single entity
- Left-truncation keeps the most meaningful part (`value`) always visible
- `title` attribute provides the full string without requiring a separate Tooltip component

**Cons:**

- The `direction: rtl` trick is non-obvious and requires a code comment to explain
- The fully-qualified string lacks the colour-coding of `PLCTag`/`TopicFilter` — no blue/orange visual distinction between TAG and TOPIC_FILTER sources
- The ownership context and the field value are visually inseparable — a user skimming quickly may not immediately recognise which part of the string is "their field" vs "the source path"
- At very narrow widths, even the field name could be partially clipped if it's long (e.g., `my-adapter :: tag › billing-address-line-2`)

**Separator variation:**

| Variant         | Example                            | Notes                                                                     |
| --------------- | ---------------------------------- | ------------------------------------------------------------------------- |
| `::` throughout | `my-adapter :: my/tag/t1 :: value` | Fully consistent with EDG-35, may flatten hierarchy                       |
| `::` + `›`      | `my-adapter :: my/tag/t1 › value`  | Distinguishes ownership chain from field name                             |
| `::` + `/`      | `my-adapter :: my/tag/t1/value`    | Looks like an extended path; `value` could be confused with a tag segment |

The `::` + `›` variant is recommended if this option is chosen: it keeps EDG-35 consistency for ownership while visually signalling the transition to the schema field.

---

### Option 4 — Tooltip + left border colour (ruled out as standalone)

Wrap `<Code>` in `Tooltip`. Add a colored `borderLeft` on the drop zone matching the source type (blue/orange).

**Why ruled out as standalone:** Tooltip-only information fails WCAG 1.4.13. More critically, two TAG sources both get a blue border — same-type collisions remain unresolvable.

**Valid as supplement:** The colored left border can be added on top of any of the above options as a fast passive differentiator at near-zero cost.

---

## 6. Comparison Matrix

| Criterion                      | Option 1 (stacked in zone) | Option 2 (row above zone) | Option 3 (integrated string)       | Option 4 standalone |
| ------------------------------ | -------------------------- | ------------------------- | ---------------------------------- | ------------------- |
| Ownership always visible       | ✅                         | ✅                        | ✅ left-truncated                  | ❌ hover only       |
| Same-type collision resolution | ✅                         | ✅                        | ✅ (scope in string)               | ❌                  |
| Long ID at lg-panel            | ⚠️ chip truncates          | ✅ full-width row         | ✅ left-truncates, field preserved | n/a                 |
| Row height increase            | ⚠️ +28px                   | ⚠️ +28px                  | ✅ none — single line              | ✅ none             |
| DnD regression risk            | ✅ none                    | ✅ none (lowest)          | ✅ none                            | ✅ none             |
| Accessibility                  | ✅                         | ✅ best                   | ✅ title attr fallback             | ❌ standalone       |
| Colour-coded source type       | ✅ PLCTag/TopicFilter      | ✅ PLCTag/TopicFilter     | ❌ plain text only                 | partial             |
| Visual echo of source panel    | ✅ direct                  | ✅ direct                 | ❌ no chip                         | partial             |
| Legacy fallback (no sourceRef) | ✅ graceful                | ✅ graceful               | ✅ graceful                        | ✅                  |
| Structural change              | VStack inside zone         | column on CardBody        | none                               | none                |

---

## 7. Recommendation

The three options represent a trade-off between **visual richness** (colour-coded chip vocabulary), **layout density** (single-line compactness), and **forward compatibility** (robustness when transform expressions are added).

**Option 2 is the recommended implementation.** The full-width ownership row is independent of whatever expression the drop zone contains — today `value`, tomorrow `value.toNumber()`. The drop zone `Box` is unmodified. At lg-panel widths, the ownership row has the full card width (~460px) for the tag label, eliminating truncation concerns.

**Option 1** is viable but weaker than Option 2 at narrow widths. It handles transforms as cleanly as Option 2 (ownership and expression are on separate lines), but the chip at lg-panel has only ~165px versus Option 2's ~460px.

**Option 3 is not recommended** if transforms are on the roadmap. The left-truncation strategy anchors to the right end of the string. When a transform is appended, the field name moves into the middle of the string and has no guaranteed visibility at narrow widths. Fixing this requires the two-span mitigation described in section 8, which recovers most of the complexity that Option 3 was designed to avoid.

**Supplement with Option 4** (coloured left border on the drop zone) regardless of which option is chosen — it is a near-zero cost passive differentiator that works at all widths and is unaffected by transform length.

---

## 8. Forward Compatibility: Transformation Functions

> **Status: optional / speculative** — transformations are not implemented, but the `instruction.source` field is already a plain string specifically to accommodate expressions like `value.toNumber()` or `value.firstElement()`.

### The shift in the "important end"

Today: `instruction.source` = `value` — short, always fits, field name is the whole content.

Future: `instruction.source` = `value.toNumber()`, `sensors[0].temperature.toFixed(2)`, potentially chained: `readings.firstElement().trim().toNumber()`.

The string has three semantic layers:

```
[ownership prefix]  ›  [field name]  [transform chain]
my-adapter :: tag       value         .toNumber()
```

- The ownership prefix is the least important to preserve under truncation — the user already knows which adapter they're working on.
- The field name is the anchor — it is what was dragged and dropped.
- The transform chain is a user-configured expression applied to that field — losing it silently would be misleading.

### Impact per option

**Option 1 — stacked inside drop zone**

The ownership chip sits on its own line; the `<Code>` below it shows `value.toNumber()`. The two concerns are fully decoupled. Adding a transform makes the `<Code>` line longer, but it does not affect the chip above. At lg-panel, the `<Code>` line may overflow horizontally, which is a general expression-display problem independent of ownership. The `<Code>` can wrap or receive its own truncation rules.

**Verdict: handles transforms cleanly.** Ownership and expression are separate visual elements with separate overflow rules.

---

**Option 2 — ownership row above drop zone**

Same structural argument as Option 1 — the ownership row and the drop zone content are separate DOM elements with independent overflow. The drop zone shows `value.toNumber()` exactly as it shows `value` today; whatever expression-display rules are needed can be applied to the drop zone `<Code>` independently.

At lg-panel (~165px drop zone), a short transform like `.toNumber()` adds ~9 chars × ~8px ≈ 72px to the existing `value` (~5 chars × ~8px = 40px), totalling ~112px. Still fits. A long chained expression (`sensors.temperature.firstElement().toFixed(2)`) at ~49 chars ≈ 392px would overflow — but that is an expression-display concern for the future transform feature, not an ownership concern.

**Verdict: handles transforms cleanly.** Ownership row is completely insulated from expression length.

---

**Option 3 — integrated string**

This is where the future constraint becomes a significant problem.

**Current integrated string:** `my-adapter :: my/tag/t1 › value`
**With a transform:** `my-adapter :: my/tag/t1 › value.toNumber()`
**With a long transform:** `my-adapter :: my/tag/t1 › readings.firstElement().trim().toNumber()`

The left-truncation strategy (`direction: rtl`) was chosen to preserve the rightmost characters — `value` — at narrow widths. With a transform appended, the right edge is now the **end of the transform chain**, not the field name. The field name is buried in the **middle** of the string.

Left-truncation at lg-panel with a long transform:

```
...tag/t1 › readings.firstElement().trim().toNumber()
```

The ownership prefix is truncated — acceptable. But:

```
...› readings.firstElement().trim().toNumber()
```

At very narrow widths, the field name `readings` itself gets truncated:

```
...firstElement().trim().toNumber()
```

The user sees the transform but not the field it operates on. This is the worst failure mode — the expression is present but its subject is missing.

The string now has **two** important segments to preserve: the field name and the transform. Left-truncation preserves only one anchor (the right end). The field name in the middle has no guaranteed visibility.

**Potential mitigation (significantly complicates the option):** Split the integrated string into two parts with different overflow rules:

```tsx
// Two-span approach inside <Code display="flex">:
<Code display="flex" overflow="hidden">
  <Text
    as="span"
    overflow="hidden"
    textOverflow="ellipsis"
    whiteSpace="nowrap"
    direction="rtl"
    unicodeBidi="plaintext"
    flexShrink={1}
    minWidth={0}
  >
    {ownershipPrefix} {/* "my-adapter :: my/tag/t1 › " */}
  </Text>
  <Text as="span" whiteSpace="nowrap" flexShrink={0}>
    {expression} {/* "value.toNumber()" */}
  </Text>
</Code>
```

This anchors the expression on the right (never truncated) and left-truncates the ownership prefix. However, this approach:

- Reintroduces multiple DOM elements (negating Option 3's "single element" advantage)
- Requires that the expression itself fits in the available width — if the expression is longer than the container, the overflow rules conflict
- Is complex enough to be a separate design problem

**Verdict: Option 3's integrated string strategy is fundamentally undermined by transforms.** The left-truncation anchor breaks as soon as the expression has variable length. The option either requires the two-span mitigation (losing its simplicity) or accepts that field names may be invisible at narrow widths with long transforms.

---

**Option 4 — tooltip + border (unchanged)**

No impact from transforms — the drop zone content is untouched. As a supplement to Options 1 or 2, it remains viable regardless of transform length.

---

### Summary

|                                                 | Option 1                           | Option 2                           | Option 3                                     | Option 4 supplement |
| ----------------------------------------------- | ---------------------------------- | ---------------------------------- | -------------------------------------------- | ------------------- |
| Short transform (`value.toNumber()`)            | ✅ separate line                   | ✅ separate row                    | ✅ left-truncation holds                     | ✅                  |
| Long expression (`x.firstElement().toFixed(2)`) | ✅ Code wraps independently        | ✅ Code wraps independently        | ⚠️ field name may disappear                  | ✅                  |
| Chained transforms                              | ✅ expression-display problem only | ✅ expression-display problem only | ❌ middle segment (field name) has no anchor | ✅                  |
| Ownership unaffected by transform length        | ✅                                 | ✅                                 | ❌                                           | ✅                  |

**The transform constraint decisively favours Options 1 and 2** over Option 3. In both Options 1 and 2, ownership and expression are independent elements with independent overflow rules. Adding transform support later requires only changes to the `<Code>` element inside the drop zone — the ownership display is never involved.

Option 3 would require a structural rework when transforms arrive, specifically the two-span mitigation described above. If transforms are a near-term roadmap item, investing in Option 3 now creates technical debt to unwind later.

---

## 9. Open Questions for Decision

1. **Which option?** The analysis is complete. A decision is needed before implementation begins.

2. **Separator between ownership chain and field name (Option 3):** Three candidates — which is preferred?

   - `::` throughout: `my-adapter :: my/tag/t1 :: value` — consistent with EDG-35, but `value` looks like another tag segment
   - `::` + `›`: `my-adapter :: my/tag/t1 › value` — the `›` signals "within this source's schema"
   - `::` + `/`: `my-adapter :: my/tag/t1/value` — reads as a unified path but `value` could be confused with a sub-tag segment

3. **Scope always shown, or only on collision?** The `::` format can be applied unconditionally (always `{scope} :: {id}` when scope is set) or only when the same `id` appears on multiple sources. Unconditional is simpler and recommended; conditional requires a comparison pass over all instructions in the current mapping.

4. **`PULSE_ASSET` type:** All three options include `AssetTag` (teal) for `PULSE_ASSET`. Confirm this is the correct treatment.

5. **Legacy data without `sourceRef`:** All options fall back to the current behaviour (just the source path) when `sourceRef` is absent. Confirm this graceful fallback is intended.

---

## 9. Investigation Test

The baseline Cypress test is live and all 17 tests pass:

```
src/components/rjsf/MqttTransformation/components/mapping/MappingInstruction.investigation.cy.tsx
```

Run command:

```sh
pnpm cypress:run:component --spec "src/components/rjsf/MqttTransformation/components/mapping/MappingInstruction.investigation.cy.tsx"
```

Screenshots are saved to `.tasks/DG-34-instruction-scope/screenshots/` and `cypress/screenshots/MappingInstruction.investigation.cy.tsx/`.

Once the implementation option is chosen, the investigation test will be extended with:

1. Before/after screenshots for each option
2. Acceptance assertions for ownership badge presence
3. `cy.checkAccessibility()` at both viewport widths
