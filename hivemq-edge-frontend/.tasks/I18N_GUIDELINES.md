# HiveMQ Edge Frontend - i18n Guidelines

**Last Updated:** November 10, 2025

---

## 🌍 Core Principles

### 1. ✅ ALWAYS Use Plain String Keys

**CRITICAL:** Translation keys must ALWAYS be plain strings, never template literals or concatenations.

❌ **WRONG - Template Literals:**

```typescript
// Hard to find in IDE, prone to typos
t(`${someVar}.name`)
t(`workspace.wizard.${entityType}.description`)
```

✅ **CORRECT - Plain Strings:**

```typescript
// Easy to find, IDE can validate
t('workspace.wizard.entityType.name', { context: entityType })
t('workspace.wizard.entityType.description', { context: entityType })
```

**Why:**

- IDE tooling can find and validate keys
- No runtime string concatenation errors
- Easy to refactor and search
- Clear in code reviews

---

## 📝 Pattern: Using i18next Context

When you have multiple variations of the same key (e.g., different entity types), use **i18next context** instead of nested objects or template literals.

### Example: Entity Type Names

**Metadata:**

```typescript
export interface EntityTypeMetadata {
  type: EntityType // 'ADAPTER', 'BRIDGE', etc.
  icon: IconType
  category: 'entity' | 'integration'
  // ❌ NO: i18nKey: string
}

export const ENTITY_TYPE_METADATA: Record<EntityType, EntityTypeMetadata> = {
  [EntityType.ADAPTER]: {
    type: EntityType.ADAPTER, // ✅ Use type as context value
    icon: LuDatabase,
    category: 'entity',
  },
  // ... more types
}
```

**Component:**

```typescript
const EntityTypeCard = ({ metadata }) => {
  const { t } = useTranslation()

  return (
    <div>
      {/* ✅ Plain string key + context */}
      <h3>{t('workspace.wizard.entityType.name', { context: metadata.type })}</h3>
      <p>{t('workspace.wizard.entityType.description', { context: metadata.type })}</p>
    </div>
  )
}
```

**Translation JSON:**

```json
{
  "workspace": {
    "wizard": {
      "entityType": {
        "name_ADAPTER": "Adapter",
        "name_BRIDGE": "Bridge",
        "name_COMBINER": "Combiner",
        "description_ADAPTER": "Connect to external protocols",
        "description_BRIDGE": "Connect to another MQTT broker",
        "description_COMBINER": "Merge data from multiple sources"
      }
    }
  }
}
```

**How i18next Context Works:**

1. You call `t('key', { context: 'VALUE' })`
2. i18next looks for `key_VALUE`
3. If found, returns that translation
4. If not found, falls back to `key`

---

## 🎯 Real-World Example: Wizard Entity Types

### ❌ OLD Approach (Template Literals)

```typescript
// Metadata - had to store full i18n path
export const ENTITY_TYPE_METADATA = {
  [EntityType.ADAPTER]: {
    i18nKey: 'workspace.wizard.entityType.adapter',  // ❌ String duplication
  },
}

// Component - template literal
{t(`${metadata.i18nKey}.name`)}  // ❌ Hard to find in IDE
{t(`${metadata.i18nKey}.description`)}  // ❌ Prone to typos

// JSON - nested objects
{
  "entityType": {
    "adapter": {
      "name": "Adapter",
      "description": "..."
    },
    "bridge": {
      "name": "Bridge",
      "description": "..."
    }
  }
}
```

### ✅ NEW Approach (Context Pattern)

```typescript
// Metadata - just store the type
export const ENTITY_TYPE_METADATA = {
  [EntityType.ADAPTER]: {
    type: EntityType.ADAPTER,  // ✅ Single source of truth
  },
}

// Component - plain string keys
{t('workspace.wizard.entityType.name', { context: metadata.type })}  // ✅ IDE can find
{t('workspace.wizard.entityType.description', { context: metadata.type })}  // ✅ Clear

// JSON - flat with context suffix
{
  "entityType": {
    "name_ADAPTER": "Adapter",
    "name_BRIDGE": "Bridge",
    "description_ADAPTER": "Connect to external protocols",
    "description_BRIDGE": "Connect to another MQTT broker"
  }
}
```

---

## 🔍 Benefits of Context Pattern

### 1. **IDE Integration** ✅

- Cmd/Ctrl+Click on key navigates to JSON
- Find All References works
- Rename refactoring works
- JSON schema validation

### 2. **Type Safety** ✅

```typescript
// IDE knows these are plain strings
t('workspace.wizard.entityType.name')  // ✅ Can validate
t(`workspace.wizard.${var}.name`)      // ❌ Cannot validate
```

### 3. **Maintainability** ✅

- Easy to find all usages
- No string concatenation bugs
- Clear what keys are used
- Refactoring is safe

### 4. **Code Reviews** ✅

```typescript
// Reviewer can immediately see what key is used
t('workspace.wizard.entityType.name', { context: 'ADAPTER' }) // ✅ Clear

// Reviewer has no idea what this resolves to
t(`${metadata.i18nKey}.name`) // ❌ Unclear
```

---

## 📋 Implementation Checklist

When adding new i18n keys with variations:

- [ ] Use plain string keys in `t()` calls
- [ ] Pass variation as `context` parameter
- [ ] In JSON, use `key_CONTEXT` pattern
- [ ] Remove any `i18nKey` or similar fields from metadata
- [ ] Store only the context value (e.g., `type`) in metadata
- [ ] Test that all variations render correctly

---

## 🚫 Anti-Patterns to Avoid

### ❌ Template Literals

```typescript
t(`${prefix}.${key}`) // Hard to find, error-prone
```

### ❌ String Concatenation

```typescript
t(baseKey + '.name') // Can't validate, typo-prone
```

### ❌ Storing Full Keys in Data

```typescript
const metadata = {
  i18nKey: 'workspace.wizard.adapter', // Duplication, not DRY
}
```

### ❌ Nested Objects for Variations

```json
{
  "adapter": { "name": "..." },
  "bridge": { "name": "..." }
}

// Use context pattern instead
```

---

## ✅ Correct Patterns

### Plain Strings

```typescript
t('workspace.wizard.title') // ✅ Always
```

### With Interpolation

```typescript
t('workspace.wizard.step', { current: 1, total: 4 }) // ✅ Plain key
```

### With Context

```typescript
t('workspace.wizard.entityType.name', { context: type }) // ✅ Plain key + context
```

### With Pluralization

```typescript
t('workspace.items', { count: 5 }) // ✅ i18next handles plurals
```

---

## 🎓 Learning Resources

### i18next Context Documentation

- [Official Docs](https://www.i18next.com/translation-function/context)

### Key Points:

1. Context is part of i18next core
2. Use `_CONTEXT` suffix in JSON keys
3. Automatic fallback to base key
4. Works with all i18next features

---

## 📝 Examples in Codebase

### Wizard Entity Types

**File:** `src/modules/Workspace/components/wizard/steps/SelectEntityTypeStep.tsx`

```typescript
t('workspace.wizard.entityType.name', { context: metadata.type })
t('workspace.wizard.entityType.description', { context: metadata.type })
```

**File:** `src/locales/en/translation.json`

```json
{
  "entityType": {
    "name_ADAPTER": "Adapter",
    "name_BRIDGE": "Bridge",
    "description_ADAPTER": "Connect to external protocols",
    "description_BRIDGE": "Connect to another MQTT broker"
  }
}
```

---

## 🔧 Migration Guide

If you find template literals in code:

### 1. Identify the Pattern

```typescript
// OLD
t(`${metadata.i18nKey}.name`)
```

### 2. Determine the Context Value

```typescript
// What varies? The entity type
// metadata.i18nKey might be 'workspace.wizard.entityType.adapter'
// The varying part is 'adapter' (or metadata.type = 'ADAPTER')
```

### 3. Refactor to Context

```typescript
// NEW
t('workspace.wizard.entityType.name', { context: metadata.type })
```

### 4. Update JSON

```json
// OLD
{
  "adapter": { "name": "Adapter" },
  "bridge": { "name": "Bridge" }
}

// NEW
{
  "name_ADAPTER": "Adapter",
  "name_BRIDGE": "Bridge"
}
```

### 5. Remove Unnecessary Fields

```typescript
// OLD
interface Metadata {
  i18nKey: string // Remove this
}

// NEW
interface Metadata {
  type: EntityType // Use this as context
}
```

---

## ✅ Summary

**Golden Rule:** `t()` calls must use **plain string keys**, always.

**For Variations:** Use **i18next context** with `_SUFFIX` pattern in JSON.

**Benefits:**

- IDE tooling works
- Easy to find and refactor
- Type-safe
- Maintainable
- No runtime concatenation

---

**Date:** November 10, 2025  
**Task:** 99999-workspace-operation-wizard  
**Pattern Established In:** SelectEntityTypeStep component refactoring
