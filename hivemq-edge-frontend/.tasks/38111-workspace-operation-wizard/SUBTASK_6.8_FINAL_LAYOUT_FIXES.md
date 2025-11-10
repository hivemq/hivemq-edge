# SUBTASK_6.8: Protocol Selector Final Layout Fixes

**Date:** November 10, 2025  
**Issues Fixed:** Search button position conflict, two-column forced layout  
**Status:** ✅ Complete

---

## Issues Fixed

### ❌ Issue 1: Search Button Conflicting with Close Button

- Search icon button in header overlapped with close button
- Poor visual hierarchy
- Confusing UX

### ❌ Issue 2: ProtocolsBrowser Always Two Columns

- Component used media queries to show 2 columns on xl screens
- No way to override and force single column
- Wizard needed single column for cleaner layout

---

## Solutions Implemented

### ✅ Solution 1: Moved Search Toggle to Footer

**Before (Header):**

```
┌─────────────────────────────────┐
│ Select Protocol    [🔍] [X]     │ ← Buttons too close
└─────────────────────────────────┘
```

**After (Footer):**

```
┌─────────────────────────────────┐
│ Select Protocol             [X] │ ← Clean header
├─────────────────────────────────┤
│ (protocol cards)                │
├─────────────────────────────────┤
│      [🔍 Show Search & Filters] │ ← Clear button
└─────────────────────────────────┘
```

### ✅ Solution 2: Added `forceSingleColumn` Prop

**Modified:** `ProtocolsBrowser.tsx`

```tsx
interface ProtocolsBrowserProps {
  // ...existing props
  forceSingleColumn?: boolean  // ← NEW
}

<SimpleGrid
  templateColumns={
    forceSingleColumn
      ? 'repeat(1, 1fr)'  // ← Always 1 column
      : { base: 'repeat(1, 1fr)', xl: 'repeat(2, 1fr)' }  // ← Original behavior
  }
>
```

**Usage in wizard:**

```tsx
<ProtocolsBrowser
  items={safeData}
  facet={facet}
  onCreate={onSelect}
  forceSingleColumn // ← Force single column
/>
```

---

## Visual Results

### Default View (No Search)

```
┌─────────────────────────────────┐
│ Select Protocol Type         [X]│
│ Choose the protocol adapter...  │
├─────────────────────────────────┤
│ ┌─────────────────────────────┐ │
│ │ Modbus TCP                  │ │ ← Single column
│ │ [Create]                    │ │    Full width
│ └─────────────────────────────┘ │
│ ┌─────────────────────────────┐ │
│ │ OPC-UA                      │ │
│ │ [Create]                    │ │
│ └─────────────────────────────┘ │
├─────────────────────────────────┤
│   [🔍 Show Search & Filters]    │ ← Footer button
└─────────────────────────────────┘
```

### With Search Active

```
┌─────────────────────────────────┐
│ Select Protocol Type         [X]│
│ Choose the protocol adapter...  │
├─────────────────────────────────┤
│ ┌────────┬──────────────────────┤
│ │Search  │ ┌──────────────────┐ │
│ │        │ │ Modbus TCP       │ │ ← Still single
│ │Filters │ │ [Create]         │ │    column
│ │        │ └──────────────────┘ │
│ │        │ ┌──────────────────┐ │
│ │        │ │ OPC-UA           │ │
│ │        │ │ [Create]         │ │
│ └────────┴──────────────────────┤
├─────────────────────────────────┤
│   [🔍 Hide Search & Filters]    │ ← Toggle to hide
└─────────────────────────────────┘
```

---

## Technical Changes

### 1. ProtocolsBrowser.tsx

**Added prop:**

```tsx
forceSingleColumn?: boolean
```

**Updated template columns:**

```tsx
templateColumns={
  forceSingleColumn
    ? 'repeat(1, 1fr)'
    : { base: 'repeat(1, 1fr)', xl: 'repeat(2, 1fr)' }
}
```

**Default:** `false` - preserves existing behavior  
**When true:** Always single column, regardless of screen size

### 2. WizardProtocolSelector.tsx

**Removed from header:**

```tsx
// ❌ OLD: IconButton in header conflicted with close button
<IconButton icon={<SearchIcon />} />
```

**Added to footer:**

```tsx
// ✅ NEW: Button in footer, centered
<DrawerFooter borderTopWidth="1px" justifyContent="center">
  <Button leftIcon={<SearchIcon />} variant={showSearch ? 'solid' : 'outline'}>
    {showSearch ? 'Hide Search' : 'Show Search'}
  </Button>
</DrawerFooter>
```

**Updated grid layout:**

```tsx
<Grid templateColumns="175px 1fr" gap={4}>
  {' '}
  // ← Optimized width
  <GridItem>
    <FacetSearch />
  </GridItem>
  <GridItem>
    <ProtocolsBrowser forceSingleColumn />
  </GridItem>
</Grid>
```

**Added forceSingleColumn prop:**

```tsx
<ProtocolsBrowser
  items={safeData}
  facet={facet}
  onCreate={onSelect}
  forceSingleColumn // ← Always single column in wizard
/>
```

---

## Benefits

### ✅ Clean Header

- No conflict between search and close buttons
- Clear title and description
- Professional appearance

### ✅ Clear Footer Action

- Prominent search toggle button
- Centered for easy access
- Clear label (Show/Hide Search)
- Visual state (outline/solid variant)

### ✅ Consistent Layout

- Single column throughout
- More space per protocol card
- Easier to scan
- Better for focused selection

### ✅ No Breaking Changes

- `forceSingleColumn` defaults to `false`
- Existing ProtocolsBrowser usage unaffected
- Only wizard uses new behavior

---

## i18n Keys

**Added:**

```json
{
  "showSearch": "Show Search & Filters",
  "hideSearch": "Hide Search & Filters"
}
```

**Removed:**

```json
{
  "toggleSearch": "Toggle search and filters" // ← No longer needed
}
```

---

## Testing Checklist

- [x] Header shows title and description only
- [x] Close button in top-right (no conflicts)
- [x] Footer shows search toggle button
- [x] Button centered in footer
- [x] Default: Shows "Show Search & Filters"
- [x] After click: Shows "Hide Search & Filters"
- [x] Button variant changes (outline → solid)
- [x] Protocol cards in single column (default)
- [x] Protocol cards in single column (with search)
- [x] Two-column grid layout works (search on left)
- [x] Existing ProtocolAdapter page unaffected

---

## Comparison: Original vs Wizard

### Original ProtocolAdapter Page

```tsx
<ProtocolsBrowser items={data} facet={facet} onCreate={handleCreate} />
// Result: 2 columns on xl screens
```

### Wizard Usage

```tsx
<ProtocolsBrowser
  items={data}
  facet={facet}
  onCreate={handleCreate}
  forceSingleColumn // ← Override
/>
// Result: Always 1 column
```

---

**Status:** ✅ Both issues resolved - Clean layout with no conflicts!
