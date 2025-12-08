# 🛑 STOP AND INVESTIGATE - Mandatory Checklist for Cypress Issues

**USE THIS EVERY TIME A CYPRESS TEST FAILS OR SOMETHING IS NOT FOUND**

---

## ❌ When Test Fails with "Element not found" / "Button disabled" / etc.

### STOP. Do NOT guess. Follow these steps IN ORDER:

### 1. READ THE ERROR MESSAGE

- [ ] What EXACTLY is the error? (copy it)
- [ ] What selector/aria-label is it looking for?
- [ ] What line number failed?

### 2. FIND THE IMPLEMENTATION

```bash
# For buttons/UI elements:
grep -r "the-text-from-error" src/extensions/datahub/**/*.tsx

# For aria-labels:
grep -r "aria-label" src/extensions/datahub/**/ComponentName.tsx
```

### 3. CHECK TRANSLATIONS (if using t() function)

```typescript
// If you see: aria-label={t('workspace.nodes.type', { context: nodeType })}
// Then look up: workspace.nodes.type_FUNCTION in:
src / extensions / datahub / locales / en / datahub.json
```

### 4. VERIFY IN EXISTING TESTS

```bash
# How do other tests use this?
grep -r "the-selector" cypress/e2e/**/*.spec.cy.ts
```

### 5. CHECK THE POM

```typescript
// Does the POM already have this?
cypress / pages / DataHub / DesignerPage.ts
cypress / pages / DataHub / DatahubPage.ts
```

### 6. FIX AND VERIFY

- [ ] Update POM if needed
- [ ] Use POM in test (NO direct selectors)
- [ ] Run test ONCE to verify

---

## ❌ When You're About to Make an Assumption

### STOP. Ask yourself:

1. **Have I actually LOOKED at the code?**

   - If NO → Go find it first
   - If YES → Did I read the WHOLE relevant section?

2. **Am I guessing based on "maybe" or "probably"?**

   - If YES → STOP. Go investigate.
   - If NO → Proceed with confidence.

3. **Have I checked translations for i18next usage?**
   - If you see `t('key', {context: value})` → Look up translation
   - Pattern: `key_context` in JSON file

---

## ✅ GOOD Investigation Example

**Error:** `Expected to find element: [aria-label="Function"]`

**Steps:**

1. ❌ Don't assume "Function" is wrong
2. ✅ Find where button is defined: `src/extensions/datahub/components/controls/ToolItem.tsx`
3. ✅ See: `aria-label={t('workspace.nodes.type', { context: nodeType })}`
4. ✅ Look up translation: `workspace.nodes.type_FUNCTION` → "JS Function"
5. ✅ Fix POM: Use "JS Function" not "Function"
6. ✅ Run test once

**Time: 3-5 minutes. Result: Fixed correctly.**

---

## ❌ BAD Investigation Example

**Error:** `Expected to find element: [aria-label="Function"]`

**Steps:**

1. ❌ Assume button is disabled
2. ❌ Try adding `{ force: true }`
3. ❌ Fails again
4. ❌ Assume need pre-existing policy
5. ❌ Rewrite entire test
6. ❌ Fails again
7. ❌ Ask user for help

**Time: 30+ minutes. Result: User frustrated.**

---

## 🎯 The Rule

**If you cannot answer "Where in the code is this defined?" → STOP GUESSING. GO FIND IT.**

---

## 📋 Quick Commands Reference

### Find component implementation:

```bash
grep -r "ComponentName" src/extensions/datahub/**/*.tsx
```

### Find aria-label/test-id:

```bash
grep -r "aria-label.*SchemaButton" src/**/*.tsx
grep -r "data-testid.*save-button" src/**/*.tsx
```

### Find translation key:

```bash
# Pattern: t('workspace.nodes.type', { context: 'FUNCTION' })
# Becomes: workspace.nodes.type_FUNCTION
grep -r "type_FUNCTION" src/extensions/datahub/locales/en/datahub.json
```

### Check existing tests:

```bash
grep -r "schemaPanel\|function.*drag" cypress/e2e/**/*.cy.ts
```

### Check POM:

```bash
cat cypress/pages/DataHub/DesignerPage.ts | grep -A 3 "get schema\|get function"
```

---

## 💡 Remember

- **Documents are for reference**
- **This checklist is for ACTION**
- **Follow it EVERY TIME before guessing**

**When in doubt: INVESTIGATE, don't SPECULATE**
