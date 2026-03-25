# EDG-176 — Analysis & Implementation Plan

## 1. Architecture Context

The combining editor (`DataCombiningEditorField`) renders three stacked selectors:

```
[CombinedEntitySelect]   ← multi-select; sources (tags + topic filters)
[PrimarySelect]          ← single-select; the trigger/primary key  ← THIS TASK
[DestinationSchemaLoader / mapping table]
```

`CombinedEntitySelect` is the reference implementation — it already renders:

- **Options**: label + adapterId (gray right-aligned) + type badge, description below
- **Selected chips**: `PLCTag` / `TopicFilter` / `Topic` badges via `MultiValueContainer`

## 2. Current State of `PrimarySelect`

### Data flow

```
formData.sources.tags       (string[])
formData.sources.topicFilters (string[])
formContext.selectedSources  (DataIdentifierReference[] with scope)  ← richer
```

`primaryOptions` is built from `formData.sources.tags` (plain strings) + `getAdapterIdForTag()` lookup.
`primaryValue` is built from `formData.sources.primary` which DOES carry `scope`.

### What's missing

| Concern              | Current                           | Target                               |
| -------------------- | --------------------------------- | ------------------------------------ |
| Option scope display | Not shown                         | adapterId in gray, right-aligned     |
| Option type badge    | Not shown                         | TAG / TOPIC_FILTER                   |
| Option description   | Not shown                         | Below label                          |
| Selected value       | Plain string                      | PLCTag / TopicFilter badge           |
| Options data source  | `formData.sources.tags` (strings) | Prefer `formContext.selectedSources` |

## 3. Reference Implementation (`CombinedEntitySelect`)

### Option component

```tsx
Option: ({ children, ...props }) => (
  <chakraComponents.Option {...props}>
    <VStack gap={0} alignItems="stretch" w="100%">
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
      <Text fontSize="sm" noOfLines={3} ml={4} lineHeight="normal" textAlign="justify">
        {props.data.description}
      </Text>
    </VStack>
  </chakraComponents.Option>
)
```

### Multi-value chip

```tsx
MultiValueContainer: ({ children, ...props }) => (
  <>
    {props.data.type === SelectEntityType.TAG && <PLCTag tagTitle={children} mr={3} />}
    {props.data.type === SelectEntityType.TOPIC_FILTER && <TopicFilterComponent tagTitle={children} mr={3} />}
  </>
)
```

For the single-select `PrimarySelect`, the equivalent of `MultiValueContainer` is the `SingleValue` component.

## 4. Key Utilities Available

| Utility                      | Location              | Purpose                       |
| ---------------------------- | --------------------- | ----------------------------- |
| `formatOwnershipString(ref)` | `topic-utils.ts`      | `scope :: id` or just `id`    |
| `PLCTag`                     | `EntityTag.tsx`       | Blue badge with PLC icon      |
| `TopicFilter`                | `EntityTag.tsx`       | Orange badge with filter icon |
| `chakraComponents`           | `chakra-react-select` | Base components to wrap       |
| `getAdapterIdForTag`         | `combining.utils.ts`  | Scope lookup from context     |

## 5. Implementation Plan

### Step 1 — Enrich `PrimaryOption` type

Add `description?: string` to `PrimaryOption`. No behavioural change, prepares for option rendering.

### Step 2 — Update `primaryOptions` useMemo

**Prefer `formContext.selectedSources`** as source of truth (same pattern as `CombinedEntitySelect.values`). It carries `DataIdentifierReference[]` with `scope` already resolved.

```ts
// Prefer selectedSources (has full scope info)
if (formContext?.selectedSources) {
  return [
    ...formContext.selectedSources.tags.map<PrimaryOption>((ref) => ({
      label: ref.id, // raw name — scope shown separately in Option UI
      value: ref.id,
      type: DataIdentifierReference.type.TAG,
      adapterId: ref.scope || undefined,
    })),
    ...formContext.selectedSources.topicFilters.map<PrimaryOption>((ref) => ({
      label: ref.id,
      value: ref.id,
      type: DataIdentifierReference.type.TOPIC_FILTER,
    })),
  ]
}
// Fallback: deprecated string arrays + context lookup
```

Note: `label` stays as the raw name in the options list (scope is shown separately in the Option UI, matching `CombinedEntitySelect`).

### Step 3 — Update `primaryValue` useMemo

Use `formatOwnershipString` so the selected badge shows the scope:

```ts
const primaryValue = useMemo<PrimaryOption | null>(() => {
  if (!formData?.sources.primary) return null
  const primary = formData.sources.primary
  return {
    label: formatOwnershipString(primary), // "my-adapter :: my/tag/t1"
    value: primary.id,
    type: primary.type,
    adapterId: primary.scope || undefined,
  }
}, [formData?.sources.primary])
```

### Step 4 — Add `Option` component (matches `CombinedEntitySelect.Option`)

```tsx
Option: ({ children, ...props }) => (
  <chakraComponents.Option {...props}>
    <VStack gap={0} alignItems="stretch" w="100%">
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
    </VStack>
  </chakraComponents.Option>
)
```

Note: description is omitted for now since `formData.sources.tags` is `string[]` — there's no description at this point in the data flow. The HStack structure is identical to `CombinedEntitySelect`.

### Step 5 — Add `SingleValue` component (replaces plain string display)

```tsx
SingleValue: ({ data }) =>
  data.type === DataIdentifierReference.type.TAG ? (
    <PLCTag tagTitle={data.label} ml={2} />
  ) : (
    <TopicFilter tagTitle={data.label} ml={2} />
  )
```

`data.label` here is the `formatOwnershipString` result (`my-adapter :: my/tag/t1`), which `EntityTag` will then pass through `formatTopicString` (expanding `/` to `/`). This is consistent with how `CombinedEntitySelect` renders selected chips.

### Step 6 — Imports cleanup

Add to imports: `formatOwnershipString` from `topic-utils`, `PLCTag`, `TopicFilter` (renamed) from `EntityTag`, `VStack`, `HStack`, `Text` from Chakra.

The i18n key `combiner.schema.mapping.combinedSelector.type` already exists (used by `CombinedEntitySelect`) and supports `context: 'TAG'` and `context: 'TOPIC_FILTER'`.

## 6. Test Updates (`PrimarySelect.spec.cy.tsx`)

The existing tests pass no `formContext`, so:

- `formContext.selectedSources` is undefined → fallback to string arrays + `getAdapterIdForTag`
- `getAdapterIdForTag` returns `undefined` (no context) → `adapterId` is undefined
- Labels remain raw strings in options (no scope shown without context)
- **Option assertions** (`have.text 'my/tag/t1'`) remain valid for options (raw label, no adapterId in gray since it's undefined)

What changes:

- `should render properly` test: `cy.get('label + div').should('have.text', 'my/tag/t3')` will **fail** because the selected value is now a badge. The mock `mockPrimary` has no scope (`scope` not set in that test's mock), so `formatOwnershipString` returns `'my/tag/t3'`, which `EntityTag` formats as `'my / tag / t3'`.

**Required test update:** Replace the plain text assertion with a badge assertion:

```ts
// Before:
cy.get('label + div').should('have.text', 'my/tag/t3')
// After:
cy.get('label + div [data-testid="topic-wrapper"]').should('be.visible')
cy.get('label + div [data-testid="topic-wrapper"]').should('contain.text', 'my')
```

The `onChange` stub assertions are unaffected — the callback contract (`value`, `type`, `adapterId`) does not change.

## 7. What We Are NOT Doing

- No description support in options (data not available at this layer — `formData.sources.tags` is `string[]`)
- No changes to `DataCombiningEditorField` — `PrimarySelect` props contract unchanged
- No new components — all rendering reuses `PLCTag`, `TopicFilter`, `chakraComponents`
- No i18n additions — existing `combiner.schema.mapping.combinedSelector.type` key reused

## 8. Risk Assessment

| Risk                                                                       | Likelihood | Mitigation                                                        |
| -------------------------------------------------------------------------- | ---------- | ----------------------------------------------------------------- |
| Test assertion on selected value text breaks                               | Certain    | Update `should render properly` test                              |
| `formatTopicString` inside `EntityTag` formats `::` separator unexpectedly | Low        | Consistent with `CombinedEntitySelect` — already accepted pattern |
| Missing `formContext.selectedSources` fallback coverage                    | Low        | Fallback path kept, covered by existing tests (no context)        |
