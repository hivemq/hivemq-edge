# EDG-35: Design Analysis — Tag Ownership Rendering

**Status:** Draft — Awaiting implementation approval
**Date:** 2026-02-24

---

## 1. Scope of the Problem

### Established convention (EDG-34)

`formatOwnershipString(ref: DataIdentifierReference): string` in `src/components/MQTT/topic-utils.ts`:

```ts
ref.scope ? `${ref.scope} :: ${ref.id}` : ref.id
```

`MappingInstruction.tsx` already uses this. All Tier 1/2 locations below must do the same.

### Audit methodology

Every `PLCTag`, `TopicFilter`, and `AssetTag` usage was catalogued and assessed for:

1. **Is scope data available at the call site?**
2. **Can multiple adapters produce same-named tags on this page?**

---

## 2. Location Analysis

| Location                              | Scope available                  | Ambiguity risk | Structural change                  | Priority |
| ------------------------------------- | -------------------------------- | -------------- | ---------------------------------- | -------- |
| A — `JsonSchemaBrowser`               | ✅ `dataReference.scope`         | High           | 1-line string change               | 🔴       |
| B — `CombinedSchemaLoader` error path | ✅ `dataReference.scope`         | High           | 1-line string change               | 🔴       |
| C — `CombinedEntitySelect` chips      | ✅ `props.data.adapterId`        | Medium         | Change `label` in `values` useMemo | 🟠       |
| D — `DataCombiningTableField` sources | ⚠️ reconstruct from instructions | Medium         | Helper + inline lookup             | 🟠       |
| E — `FilterTopics` MultiValue         | ❌ not in option data            | Low            | Widget swap only                   | 🟡       |

---

### Location A — `JsonSchemaBrowser.tsx:38` 🔴 HIGH

**Current code:**

```tsx
{
  isTagShown && dataReference?.type === DataIdentifierReference.type.TAG && (
    <PLCTag tagTitle={dataReference?.id} mr={3} />
  )
}
```

**Context:** Source schema panel heading in the data combiner. Called from
`CombinedSchemaLoader` once per selected source. `dataReference` is typed as `DataReference`
which extends `DataIdentifierReference` — `scope` is present.

**Ambiguity:** When two adapters are selected as sources and both export a tag named `value`,
the panel shows two identical `[🏷 value]` headings. The schemas below each may differ.

**Scope data available:** Yes — `dataReference.scope` is already set by `getFilteredDataReferences`.

**Fix:**

```tsx
{
  isTagShown && dataReference?.type === DataIdentifierReference.type.TAG && (
    <PLCTag tagTitle={formatOwnershipString(dataReference)} mr={3} />
  )
}
```

Add import: `import { formatOwnershipString } from '@/components/MQTT/topic-utils'`

**DnD / layout impact:** None. The heading is outside the draggable list.

---

### Location B — `CombinedSchemaLoader.tsx:45` 🔴 HIGH

**Current code (error fallback path):**

```tsx
{
  dataReference.type === DataIdentifierReference.type.TAG && <PLCTag tagTitle={dataReference?.id} mr={3} />
}
```

**Context:** When the schema query for a source fails, `CombinedSchemaLoader` renders an
error block with the same chip to identify which source failed. The `dataReference` object
passed here is the same `DataReference` used in Location A — `scope` is available.

**Ambiguity:** Same as Location A. If two adapters fail to load and both have a tag named
`value`, the two error blocks are indistinguishable.

**Fix:**

```tsx
{
  dataReference.type === DataIdentifierReference.type.TAG && (
    <PLCTag tagTitle={formatOwnershipString(dataReference)} mr={3} />
  )
}
```

Add import: `import { formatOwnershipString } from '@/components/MQTT/topic-utils'`

**Note:** The `TopicFilter` chip on line 48 does not need changing — topic filters have no
scope by design.

---

### Location C — `CombinedEntitySelect.tsx` — `MultiValueContainer` 🟠 MEDIUM

**Current code:**

```tsx
MultiValueContainer: ({ children, ...props }) => (
  <>
    {props.data.type === SelectEntityType.TOPIC && <Topic tagTitle={children} mr={3} />}
    {props.data.type === SelectEntityType.TAG && <PLCTag tagTitle={children} mr={3} />}
    {props.data.type === SelectEntityType.TOPIC_FILTER && <TopicFilterComponent tagTitle={children} mr={3} />}
  </>
),
```

**Context:** Multi-select showing selected data sources in the combiner editor. Each selected
item renders as a colored chip.

**Problem:** `children` is a `ReactNode` (chakra-react-select's `MultiValueLabel` + `MultiValueRemove`
content). When passed as `tagTitle`, `EntityTag` renders it directly (the non-string branch):

```tsx
{
  typeof tagTitle === 'string' ? <TagLabel>{expandedTagTitle}</TagLabel> : tagTitle
}
```

This works visually (the remove × button is inside the chip) but the label text is just
`tag.name` — no scope. `props.data.adapterId` already holds the scope (set during option
construction from `entity.id`).

**Ambiguity:** Selecting `my-adapter::value` and `opcua-prod::value` produces two identical
`[🏷 value ×]` chips.

**Fix approach — change `label` in the `values` useMemo:**

The `MultiValueContainer` passes `children` (a ReactNode built by react-select from
`option.label`) as `tagTitle` to `PLCTag`/`TopicFilter`. Changing `option.label` to the
ownership string is all that is needed — the `MultiValueContainer` stays untouched.

```tsx
// values useMemo — TAG branch
const tagValue = formContext.selectedSources.tags.map<EntityOption>((ref) => ({
  value: ref.id,
  label: formatOwnershipString(ref), // was: ref.id
  type: ref.type,
  adapterId: ref.scope || undefined,
}))
```

Add import: `import { formatOwnershipString } from '@/components/MQTT/topic-utils'`

**Why not `MultiValueLabel`:** Switching to `MultiValueLabel` would place a `PLCTag <Tag>`
component inside the default `MultiValueContainer`, which itself wraps content in a `<Tag>`.
That nesting produces incorrect markup. The `values` label approach avoids any structural change.

**Scope propagation:** `adapterId` is already set in `allOptions` (from `entity.id`) and in
`values` (from `ref.scope`). No data-layer change needed.

---

### Location C2 — `CombinedEntitySelect.tsx` — `Option` renderer 🟠 MEDIUM

**Problem:** The dropdown option list does not show ownership. When two adapters both export a
tag named `temperature`, the two options in the list are visually identical (only the
`description` field below distinguishes them — and descriptions can be absent or identical).

**What is _not_ wanted:** A PLCTag/TopicFilter chip inside the option row. Plain styled text only.

**Root tension: `label` serves two masters**

`option.label` in react-select drives:

1. **Default filter** — `option.label.toLowerCase().includes(inputValue)`. If we change
   `label` to an ownership string, users can still type tag names to filter (the tag name is a
   suffix), but also type adapter names — which is arguably useful.
2. **Sort order** — react-select preserves `options` array order; it does _not_ sort
   alphabetically on its own. However, a future alphabetical sort on `label` would order by
   adapter prefix if the label is `"opcua-adapter :: temperature"`, pushing same-named tags
   from different adapters far apart instead of together.

**Current state of `allOptions`:** insertion order from entity queries (adapter A's tags, then
adapter B's tags, then topic filters). No explicit sort today.

**The three approaches:**

#### Approach 1 — Keep `label = tag.name`, annotate `adapterId` in the renderer (recommended)

`label` in `allOptions` stays as `tag.name`. The custom `Option` renderer already accesses
`props.data.adapterId` — add it as secondary text in the existing layout.

Layout (no structural change to HStack/VStack):

```
temperature           opcua-adapter    [Tag]
  Description of the sensor…
```

`adapterId` rendered as a muted `<Text>` between the tag name and the type badge:

```tsx
<HStack>
  <Text flex={1}>{props.data.label}</Text>
  {props.data.adapterId && (
    <Text fontSize="sm" color="gray.500">
      {props.data.adapterId}
    </Text>
  )}
  <Text fontSize="sm" fontWeight="bold">
    {t('combiner.schema.mapping.combinedSelector.type', { context: props.data.type })}
  </Text>
</HStack>
```

**Filtering:** Override `filterOption` so that typing an adapter name also works:

```tsx
filterOption={(option, inputValue) => {
  const lower = inputValue.toLowerCase()
  return (
    option.label.toLowerCase().includes(lower) ||
    (option.data.adapterId?.toLowerCase().includes(lower) ?? false)
  )
}}
```

**Sort order:** `label = tag.name` → a future alphabetical sort on `label` groups same-named
tags from different adapters next to each other (`temperature × 2` before `voltage × 1`),
which is exactly what the user expects.

**Trade-offs:** `label` and display text diverge slightly — the chip shows the ownership
string (via `values`) while the option shows only the tag name. This is intentional and
consistent with established patterns (chips compress to a short form; options show full detail).

---

#### Approach 2 — Change `label` to ownership string in `allOptions`, custom sort

`label = "opcua-adapter :: temperature"`. The Option renderer shows it as-is, ownership
visible without any renderer change. To preserve tag-name-first ordering, add an explicit
sort comparator to `allOptions`:

```tsx
return sorted.sort((a, b) => {
  const tagCmp = a.value.localeCompare(b.value)
  if (tagCmp !== 0) return tagCmp
  return (a.adapterId ?? '').localeCompare(b.adapterId ?? '')
})
```

**Trade-offs:** Requires changing `allOptions` label (ripple risk), custom sort logic, and
means the filter searches the full ownership string. The `values` array already uses
`formatOwnershipString` for chips, so `getOptionValue` must remain the triple key
(`value@adapterId@type`) to avoid react-select treating chip and option as mismatched.

---

#### Approach 3 — react-select `GroupBase` grouping by adapter

Pass options as groups: `[{ label: 'opcua-adapter', options: [...] }, ...]`. Within each
group, tags are ordered alphabetically. The group header identifies the adapter.

**Trade-offs:** Visual separation by adapter is clear, but users looking for all tags named
`temperature` from any adapter must scan multiple groups. Searching collapses groups, but the
UI change is more disruptive. Deferred unless the adapter-first mental model is confirmed by
UX review.

---

**Recommendation: Approach 1**

Keeps `label = tag.name` for natural tag-name ordering, adds `adapterId` as secondary muted
text in the existing HStack, and adds a `filterOption` override for adapter-name search.
Minimal change, no risk to the sort story.

**Open:** Should `allOptions` be explicitly sorted by `[tag.name, adapterId]`? Currently
insertion order means all of adapter A's tags come before all of adapter B's. An explicit
sort would interleave `temperature (modbus)` and `temperature (opcua)` — making the
ambiguity immediately visible in the list. This is a bonus fix worth including in the same
change.

---

### Location D — `DataCombiningTableField.tsx` — sources column 🟠 MEDIUM

**Current code:**

```tsx
{
  info.row.original.sources?.tags?.map((tag) => (
    <PrimaryWrapper key={tag} isPrimary={Boolean(isPrimary(DataIdentifierReference.type.TAG, tag))}>
      <PLCTag tagTitle={tag} />
    </PrimaryWrapper>
  ))
}
```

**Context:** Summary table of all combiner mappings. Each row shows source tags for that
mapping as chips.

**Problem:** `sources.tags` is the deprecated string-array field — it stores tag IDs only,
not `DataIdentifierReference`. Scope is not directly available.

**Scope reconstruction strategy:**

Within a `DataCombining` object, scope can be recovered via:

1. **Primary:** `sources.primary.scope` when `sources.primary.id === tag` and type is TAG
2. **Instructions:** scan `instructions` for `sourceRef.id === tag && sourceRef.type === TAG`
   → use `sourceRef.scope`
3. **Fallback:** show plain tag name (scope = null; graceful)

A small inline helper:

```ts
const getScopeForTag = (tagId: string, row: DataCombining): string | null => {
  if (row.sources.primary?.id === tagId && row.sources.primary.type === DataIdentifierReference.type.TAG) {
    return row.sources.primary.scope ?? null
  }
  const inst = row.instructions?.find(
    (i) => i.sourceRef?.id === tagId && i.sourceRef?.type === DataIdentifierReference.type.TAG
  )
  return inst?.sourceRef?.scope ?? null
}
```

Usage:

```tsx
{
  info.row.original.sources?.tags?.map((tag) => {
    const scope = getScopeForTag(tag, info.row.original)
    const tagTitle = scope ? `${scope} :: ${tag}` : tag
    return (
      <PrimaryWrapper key={tag} isPrimary={Boolean(isPrimary(DataIdentifierReference.type.TAG, tag))}>
        <PLCTag tagTitle={tagTitle} />
      </PrimaryWrapper>
    )
  })
}
```

**Note:** `sources.tags` is marked deprecated; this fix is pragmatic rather than ideal. A
future task should migrate the table to use the instructions-based model directly, eliminating
the need for reconstruction.

---

### Location E — `FilterTopics.tsx` — `MultiValue` 🟡 LOW (secondary: widget absent)

**Current code:**

```tsx
MultiValue: (props: MultiValueProps<FilterTopicsOption, true>) => (
  <chakraComponents.MultiValue {...props}>
    <Text data-testid="workspace-filter-topics-values">{props.data.label}</Text>
  </chakraComponents.MultiValue>
),
```

**Context:** Workspace search/filter panel. Dropdown `Option` uses PLCTag / TopicFilter chips
but the selected chips show plain text — visual inconsistency.

**Ambiguity for ownership:** `FilterTopicsOption` has no `adapterId`; `useGetDomainOntology`
does not expose adapter scope. **Ownership cannot be added here without a data-layer change.**

**What can be fixed:** Widget consistency only — replace `<Text>` with the matching EntityTag
component by type, matching what the `Option` already shows.

```tsx
MultiValue: (props: MultiValueProps<FilterTopicsOption, true>) => {
  const { type, label } = props.data
  return (
    <chakraComponents.MultiValue {...props}>
      {type === SelectEntityType.TAG && <PLCTag tagTitle={label} data-testid="workspace-filter-topics-values" />}
      {type === SelectEntityType.TOPIC && <Topic tagTitle={label} data-testid="workspace-filter-topics-values" />}
      {type === SelectEntityType.TOPIC_FILTER && <TopicFilter tagTitle={label} data-testid="workspace-filter-topics-values" />}
    </chakraComponents.MultiValue>
  )
},
```

**Scope display:** Deferred. Requires `useGetDomainOntology` to expose adapter IDs per tag —
outside EDG-35 scope.

---

## 3. Implementation Sequence

### Step 1 — Locations A + B (JsonSchemaBrowser + CombinedSchemaLoader)

Single-line changes; same rendering path; do together.

- Add `formatOwnershipString` import
- Replace `dataReference?.id` with `formatOwnershipString(dataReference)` for TAG type
- Update / extend Cypress component tests for `JsonSchemaBrowser` (scope present / absent)

### Step 2 — Location C (CombinedEntitySelect) chips ✅ DONE

- Changed `label: ref.id` → `label: formatOwnershipString(ref)` in `values` useMemo (TAG branch)
- `MultiValueContainer` unchanged — `children` naturally carries the updated label text
- Added 3 Cypress tests under `describe('chip ownership display')`

### Step 2b — Location C2 (CombinedEntitySelect) option list ✅ DONE

- `label` in `allOptions` unchanged (`tag.name`) — sort order preserved
- `adapterId` rendered as muted `<Text color="gray.500">` between tag name and type badge in the `Option` renderer
- `filterOption` override added — typing an adapter name filters the list
- `allOptions` now sorted by `[tag.name, adapterId]` — same-named tags from different adapters are adjacent
- Updated existing "render properly" test (positional `eq(0)` → `filter(':contains(...)')`)
- Added 3 new tests under `describe('option list ownership display')`

### Step 3 — Location D (DataCombiningTableField) ✅ DONE

- Added `getScopeForTag(tagId, row)` helper above the component (checks `sources.primary` then `instructions[].sourceRef`, falls back to `null`)
- Changed `<PLCTag tagTitle={tag} />` to `<PLCTag tagTitle={formatOwnershipString({ id: tag, type: TAG, scope: getScopeForTag(tag, row) })} />`
- Added 3 new tests under `describe('sources column ownership display')`:
  - scope from `sources.primary`
  - scope from `instructions[].sourceRef`
  - plain name when scope cannot be reconstructed (confirmed no regression on existing `mockPrimary` tests)

### Step 4 — Location E (FilterTopics) ✅ DONE

- Removed `Text` import (no longer used)
- Replaced `<Text data-testid="workspace-filter-topics-values">{label}</Text>` with typed EntityTag components (`PLCTag` / `Topic` / `TopicFilter`), passing `data-testid="workspace-filter-topics-values"` as a prop (overrides the hardcoded `topic-wrapper` via `{...rest}` spread in `EntityTag`)
- No ownership string — `FilterTopicsOption` has no scope data (deferred per open question 3)
- Updated existing "should render properly" assertions: raw-slash strings (`'test/tag1'`) → `formatTopicString('test/tag1')` to match EntityTag's internal `formatTopicString` call
- Added 2 new tests under `describe('chip widget type consistency')`:
  - TAG chip → `aria-label="Tag"` SVG icon
  - TOPIC chip → `aria-label="Topic"` SVG icon

---

## 4. Test Strategy

### Mandatory conventions (from TESTING_GUIDELINES.md + CYPRESS_TESTING_GUIDELINES.md)

- Use `cy.getByTestId()` — NEVER `cy.get('[data-testid="..."]')`
- Every test file MUST have `it('should be accessible')` — last test in the block
- Use `cy.checkAccessibility()` — NEVER `cy.checkA11y()` directly
- Add `cy.checkI18nKeys()` inside the accessibility test during development
- No arbitrary `cy.wait()` — wait for element conditions instead
- No chaining after action commands (`.click().should()` is wrong)
- All mock objects must be explicitly typed (no implicit `any`)
- Use enums from the API, not string literals (`DataIdentifierReference.type.TAG`, not `'TAG'`)

### React-select chip assertions (from REACT_SELECT_TESTING_PATTERNS.md)

```typescript
// Verify ownership text in selected chip
cy.get('[class*="multi-value"]').eq(0).should('contain.text', 'my-adapter :: value')

// Verify plain tag name (no scope)
cy.get('[class*="multi-value"]').eq(0).should('contain.text', 'value')
cy.get('[class*="multi-value"]').eq(0).should('not.contain.text', '::')

// Verify chip count
cy.get('[class*="multi-value"]').should('have.length', 2)

// Never check input.value — react-select doesn't set it
// ❌ cy.get('#my-select').should('have.value', 'value')
// ✅ cy.get('[class*="react-select"]').should('contain.text', 'value')
```

### Per-location test scenarios

**Location A — `JsonSchemaBrowser`**

```typescript
it('should show ownership string in heading when scope is set', () => {
  const ref: DataReference = { id: 'temperature', scope: 'opc-adapter', type: DataIdentifierReference.type.TAG }
  cy.mountWithProviders(<JsonSchemaBrowser dataReference={ref} ... />)
  cy.getByTestId('topic-wrapper').should('contain.text', 'opc-adapter :: temperature')
})

it('should show plain tag name in heading when scope is null', () => {
  const ref: DataReference = { id: 'temperature', scope: null, type: DataIdentifierReference.type.TAG }
  cy.mountWithProviders(<JsonSchemaBrowser dataReference={ref} ... />)
  cy.getByTestId('topic-wrapper').should('contain.text', 'temperature')
  cy.getByTestId('topic-wrapper').should('not.contain.text', '::')
})

it('should be accessible', () => {
  cy.injectAxe()
  cy.mountWithProviders(<JsonSchemaBrowser ... />)
  cy.checkAccessibility()
  cy.checkI18nKeys()
})
```

**Location C — `CombinedEntitySelect` multi-value chips**

`MultiValueContainer` is fully replaced, so react-select CSS classes are absent on chips.
Use `cy.getByTestId('topic-wrapper')` (the `data-testid` on every `PLCTag`/`TopicFilter`).

```typescript
it('should show ownership string in chip when two adapters have same tag name', () => {
  // Mount with formContext.selectedSources containing two tags named 'value' from different adapters
  cy.getByTestId('topic-wrapper').eq(0).should('contain.text', 'adapter-1 :: value')
  cy.getByTestId('topic-wrapper').eq(1).should('contain.text', 'adapter-2 :: value')
})

it('should show plain tag name when only one adapter', () => {
  cy.getByTestId('topic-wrapper').eq(0).should('contain.text', 'value')
  cy.getByTestId('topic-wrapper').eq(0).should('not.contain.text', '::')
})
```

**Location D — `DataCombiningTableField` sources column**

```typescript
it('should show ownership string for tags with reconstructed scope', () => {
  // Row data has primary with scope + matching instruction
  cy.getByTestId('sources-cell').should('contain.text', 'opc-adapter :: temperature')
})

it('should show plain tag name when scope cannot be reconstructed', () => {
  cy.getByTestId('sources-cell').should('contain.text', 'temperature')
  cy.getByTestId('sources-cell').should('not.contain.text', '::')
})
```

Existing tests must all continue to pass without modification.

---

## 5. Open Questions

1. ~~**Location C visual diff:**~~ Resolved. The `MultiValueContainer` was not replaced;
   only the `label` field in the `values` useMemo was changed. No visual regression possible.

2. **Location D scope for non-primary tags:** In practice, are there combiner mappings where
   a non-primary tag has no matching instruction? (Unlikely after EDG-34, but worth confirming
   for the fallback path.)

3. **Location E scope deferral:** Is there appetite to extend `useGetDomainOntology` to expose
   adapter IDs so FilterTopics can also show ownership? Or is this a separate task?
