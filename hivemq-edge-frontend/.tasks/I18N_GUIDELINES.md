# Internationalization (i18n) Guidelines

## Overview

This codebase uses **i18next** for internationalization. While currently only English (US) is supported, **all text must be externalized** to locale files to maintain consistency and enable future localization.

## ⚠️ CRITICAL RULE

**NEVER hardcode user-facing text strings in components.**

This is a **very bad practice** and **MUST be avoided at all cost**.

❌ **WRONG:**

```typescript
<Button>Save</Button>
<Text>Layout Options</Text>
<aria-label="Delete preset">
```

✅ **CORRECT:**

```typescript
<Button>{t('common.actions.save')}</Button>
<Text>{t('workspace.autoLayout.options.title')}</Text>
<aria-label={t('workspace.autoLayout.presets.actions.delete')}>
```

## Setup

### Supported Language

- **Code:** `en` (English US)
- **Display:** English (United States)

### Locale Files Structure

```
src/locales/en/
├── components.json    # Reusable UI components
├── translation.json   # Main application modules
└── schemas.json       # RJSF schema internationalization (experimental)
```

### File Usage

- **`components.json`** - Generic, reusable components (buttons, inputs, common UI)
- **`translation.json`** - Module-specific text (workspace, adapters, bridges, etc.)
- **`schemas.json`** - Tentative approach for RJSF form internationalization

## Implementation

### 1. Import and Setup Hook

```typescript
import { useTranslation } from 'react-i18next'

const MyComponent: FC = () => {
  const { t } = useTranslation()

  // Use t() function for all strings
  return <Button>{t('myModule.action')}</Button>
}
```

### 2. Key Structure Best Practices

Follow hierarchical namespacing:

```json
{
  "module": {
    "feature": {
      "element": {
        "property": "Text value"
      }
    }
  }
}
```

**Example from workspace.autoLayout:**

```json
{
  "workspace": {
    "autoLayout": {
      "options": {
        "title": "Layout Options",
        "actions": {
          "cancel": "Cancel",
          "apply": "Apply Options"
        }
      },
      "presets": {
        "tooltip": "Saved Presets",
        "actions": {
          "save": "Save Current Layout",
          "delete": "Delete preset"
        }
      }
    }
  }
}
```

### 3. Good Key Examples

```typescript
// ✅ Clear hierarchy
t('workspace.autoLayout.options.title')
t('workspace.autoLayout.options.actions.apply')
t('workspace.autoLayout.presets.toast.saved')

// ✅ Common actions can be reused
t('common.actions.save')
t('common.actions.cancel')
t('common.actions.delete')

// ✅ aria-labels for accessibility
aria-label={t('workspace.autoLayout.options.aria-label')}
```

### 4. Interpolation

Use placeholders for dynamic content:

**In locale file:**

```json
{
  "presets": {
    "toast": {
      "saved": "\"{{name}}\" saved",
      "loaded": "\"{{name}}\" loaded"
    }
  }
}
```

**In component:**

```typescript
toast({
  description: t('workspace.autoLayout.presets.toast.saved', {
    name: presetName,
  }),
})
```

### 5. Pluralization

```json
{
  "items": {
    "count_one": "{{count}} item",
    "count_other": "{{count}} items"
  }
}
```

```typescript
t('items.count', { count: nodes.length })
```

## Component Checklist

When creating or reviewing a component, check for:

- [ ] `useTranslation()` hook imported and used
- [ ] All button text uses `t()`
- [ ] All labels use `t()`
- [ ] All headings use `t()`
- [ ] All messages (toast, modal, alert) use `t()`
- [ ] All `aria-label` attributes use `t()`
- [ ] All placeholder text uses `t()`
- [ ] All tooltips use `t()`
- [ ] No English strings hardcoded in JSX
- [ ] New keys added to appropriate locale file

## Good Reference Example

See `LayoutSelector.tsx` for proper i18n structure:

```typescript
const LayoutSelector: FC = () => {
  const { t } = useTranslation()

  return (
    <Tooltip label={t('workspace.autoLayout.selector.tooltip')}>
      <Select>
        {algorithms.map(algo => (
          <option key={algo.type} value={algo.type}>
            {t(`workspace.autoLayout.algorithms.${algo.type}.name`)}
          </option>
        ))}
      </Select>
    </Tooltip>
  )
}
```

## Where to Add Keys

### Module-Specific Text → `translation.json`

```json
{
  "workspace": { ... },
  "bridges": { ... },
  "adapters": { ... }
}
```

### Generic Components → `components.json`

```json
{
  "button": {
    "save": "Save",
    "cancel": "Cancel"
  },
  "pagination": { ... }
}
```

### RJSF Forms → `schemas.json` (experimental)

```json
{
  "fields": {
    "name": {
      "label": "Name",
      "help": "Enter a unique name"
    }
  }
}
```

## Common Patterns

### Buttons in Footer

```typescript
<ButtonGroup>
  <Button onClick={onCancel}>
    {t('common.actions.cancel')}
  </Button>
  <Button variant="primary" type="submit">
    {t('workspace.autoLayout.options.actions.apply')}
  </Button>
</ButtonGroup>
```

### Modal Headers

```typescript
<ModalHeader>
  {t('workspace.autoLayout.presets.modal.title')}
</ModalHeader>
```

### Toast Messages

```typescript
toast({
  title: t('workspace.autoLayout.presets.toast.saved.title'),
  description: t('workspace.autoLayout.presets.toast.saved.description', {
    name: preset.name,
  }),
  status: 'success',
})
```

### Conditional Messages

```typescript
{isLoading ? (
  <Text>{t('common.status.loading')}</Text>
) : (
  <Text>{t('workspace.data.ready')}</Text>
)}
```

## Migration Strategy

If you find hardcoded strings:

1. **Identify the module/feature context**
2. **Create appropriate keys in locale file**
3. **Add `useTranslation()` hook if missing**
4. **Replace strings with `t()` calls**
5. **Test that text displays correctly**

## Anti-Patterns to Avoid

❌ **String concatenation**

```typescript
// WRONG
<Text>{t('hello') + ' ' + userName}</Text>

// CORRECT
<Text>{t('greeting', { name: userName })}</Text>
```

❌ **Hardcoded defaults**

```typescript
// WRONG
const title = someValue || 'Default Title'

// CORRECT
const title = someValue || t('common.defaults.title')
```

❌ **Mixed hardcoded and translated**

```typescript
// WRONG
<Button>Save {t('item')}</Button>

// CORRECT
<Button>{t('actions.saveItem')}</Button>
```

## Why This Matters

1. **Consistency** - All text changes in one place
2. **Future i18n** - Easy to add languages later
3. **Content Management** - Non-developers can update text
4. **Accessibility** - Screen readers get proper context
5. **Testing** - Can test with mock translations
6. **Professional** - Industry standard practice

## Even if restructuring later...

**Start with i18n from day one!**

It's easier to restructure keys than to hunt down hardcoded strings across hundreds of components.

---

**Remember: No hardcoded strings. Ever. Use i18n.** 🌍
