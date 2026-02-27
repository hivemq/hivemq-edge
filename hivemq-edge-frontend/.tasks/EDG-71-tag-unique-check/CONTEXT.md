# EDG-71 — Tag Uniqueness: Local Validation in TagEditorDrawer

**Linear**: https://linear.app/hivemq/issue/EDG-71/tag-uniqueness-of-tags-locally-and-globally-is-not-validated-on-the
**Project**: Tagname Scoping (Namespace Normalisation)
**Status**: In Progress

---

## Business Requirement

Tag names must be **unique within a single adapter**. Global uniqueness across adapters is **not** a requirement — the same tag name (e.g. `/tag/temperature`) is valid in multiple adapters (e.g. several boilers).

---

## Current State (before this task)

### What was incorrectly implemented

`customUniqueTagValidation` in `validation.utils.ts` enforces **two** checks:

1. Local duplicates within the current form ✅ correct, keep
2. Edge-wide duplicates across all adapters ❌ incorrect, must be removed

`DeviceTagForm.tsx` fetches **all edge tags** via `useListDomainTags()` and passes them as the `allTags` arg to `customUniqueTagValidation`. This incorrectly blocks valid duplicate-name-across-adapters scenarios.

### What was missing

`TagEditorDrawer` edits a single `DomainTag` with **no uniqueness validation at all** — a user can save a tag whose name already exists in the same adapter.

---

## Scope of Changes

### Remove global uniqueness enforcement

| File                       | Change                                                                           |
| -------------------------- | -------------------------------------------------------------------------------- |
| `validation.utils.ts`      | Remove `allTags` param + `edgeDuplicates` block from `customUniqueTagValidation` |
| `DeviceTagForm.tsx`        | Remove `useListDomainTags` import + usage; simplify `customValidate` call        |
| `validation.utils.spec.ts` | Remove global-uniqueness test case                                               |
| `translation.json`         | Remove `validation.identifier.tag.uniqueEdge` key                                |

### Add adapter-level uniqueness in the drawer

| File                          | Change                                                                                             |
| ----------------------------- | -------------------------------------------------------------------------------------------------- |
| `validation.utils.ts`         | Add `customUniqueTagInAdapterValidation(existingNames: string[])`                                  |
| `validation.utils.spec.ts`    | Add unit tests for `customUniqueTagInAdapterValidation`                                            |
| `TagEditorDrawer.tsx`         | Add `customValidate?: CustomValidator<DomainTag>` prop, pass to `ChakraRJSForm`                    |
| `TagTableField.tsx`           | Compute `otherTagNames` from `props.formData` (excluding `selectedItem`), pass validator to drawer |
| `TagEditorDrawer.spec.cy.tsx` | Add test verifying error is shown on duplicate name                                                |

---

## Architecture

### Component flow

```
DeviceTagList
  └─ ChakraRJSForm (tag-listing-form)
       └─ TagTableField (custom RJSF field, FieldProps<DomainTag[]>)
            └─ TagEditorDrawer (opens on edit; renders ChakraRJSForm for a single DomainTag)
```

### Key types

- `DomainTag` — `{ name: string; description?: string; definition: object }`
- `DomainTagList` — `{ items: DomainTag[] }`
- `DeviceTagListContext` — `{ adapterId: string; capabilities?: string[] }`

### Data source for uniqueness check

`props.formData` in `TagTableField` holds the full current tag list for the adapter.
It covers both:

- Tags already saved (loaded from API via React Query into `formData`)
- Tags added in the form but not yet submitted

Exclude `selectedItem` index to get `otherTagNames`.

---

## Implementation Detail

### `customUniqueTagInAdapterValidation`

```typescript
// Validates a single DomainTag against other tag names in the same adapter
export const customUniqueTagInAdapterValidation =
  (existingNames: string[]) => (formData: DomainTag | undefined, errors: FormValidation<DomainTag>) => {
    if (!formData?.name) return errors
    if (existingNames.includes(formData.name)) {
      errors.name?.addError(i18n.t('validation.identifier.tag.uniqueDevice', { ns: 'translation' }))
    }
    return errors
  }
```

### `customUniqueTagValidation` (simplified — local only)

Remove `allTags` parameter entirely; keep only the local-duplicate check.

---

## Translation

| Key                                      | Value                                              | Status    |
| ---------------------------------------- | -------------------------------------------------- | --------- |
| `validation.identifier.tag.uniqueDevice` | "This tag name is already used on this device"     | ✅ Keep   |
| `validation.identifier.tag.uniqueEdge`   | "This tag name is already used on another devices" | ❌ Remove |
