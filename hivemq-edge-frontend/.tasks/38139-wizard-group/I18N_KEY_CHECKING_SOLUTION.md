# i18n Translation Key Checking Solution

**Task:** 38139-wizard-group  
**Date:** November 22, 2025  
**Status:** ✅ COMPLETE

---

## Problem

Missing i18n translation keys were not detected during development, leading to:

- Components displaying raw key strings instead of translated text
- Errors only discovered when manually testing in browser
- No systematic way to catch missing keys during test runs

**Example Error Found:**

- Component used: `t('workspace.wizard.group.configure')`
- Key missing from: `src/locales/en/translation.json`
- Result: Displayed "workspace.wizard.group.configure" instead of "Configure Group"

---

## Solution Implemented

### 1. i18n Configuration Enhancement

**File:** `src/config/i18n.config.ts`

Added `missingKeyHandler` that:

- Stores missing keys in `window.__i18nextMissingKeys` array
- Logs errors to console for visibility
- Works automatically in all environments

```typescript
i18n.init({
  // ...existing config
  saveMissing: true,
  missingKeyHandler: (lngs, ns, key, fallbackValue) => {
    const errorMsg = `❌ Missing i18n key: "${key}" in namespace "${ns}"`
    console.error(errorMsg)

    // Store missing keys for Cypress tests to check
    if (typeof window !== 'undefined') {
      window.__i18nextMissingKeys = window.__i18nextMissingKeys || []
      window.__i18nextMissingKeys.push({ key, ns, lngs })
    }
  },
})
```

### 2. Cypress Custom Command

**File:** `cypress/support/commands/checkI18nKeys.ts`

Created `cy.checkI18nKeys()` command that:

- Reads `window.__i18nextMissingKeys` array
- Fails test if any missing keys detected
- Shows exact missing keys in error message
- Clears array after each check

```typescript
Cypress.Commands.add('checkI18nKeys', () => {
  cy.window().then((win) => {
    const missingKeys = win.__i18nextMissingKeys || []
    win.__i18nextMissingKeys = [] // Clear for next test

    if (missingKeys.length > 0) {
      const uniqueKeys = Array.from(new Set(missingKeys.map((m) => `${m.ns}:${m.key}`)))
      const keysList = uniqueKeys.map((k) => `  - ${k}`).join('\n')

      throw new Error(
        `\n❌ Missing i18n translation keys detected:\n\n${keysList}\n\n` +
          `Add these keys to src/locales/en/translation.json (or the appropriate namespace file).\n`
      )
    }
  })
})
```

### 3. TypeScript Definitions

**File:** `src/types/cypress.d.ts`

Added type definition:

```typescript
interface Chainable {
  checkI18nKeys(): Chainable<void>
}
```

### 4. Testing Guidelines Updated

**File:** `.tasks/TESTING_GUIDELINES.md`

Added comprehensive section: "i18n Translation Key Testing" including:

- Mandatory requirement during development phase
- Pragmatic testing strategy (keep vs remove decision)
- How it works
- How to fix missing keys
- Best practices for i18n keys

---

## Usage

### During Development (MANDATORY)

```typescript
it('should be accessible', () => {
  cy.injectAxe()
  cy.mountWithProviders(<YourComponent {...meaningfulProps} />)
  cy.checkAccessibility()

  // ✅ MANDATORY during development phase
  cy.checkI18nKeys()
})
```

### After Verification (OPTIONAL)

```typescript
it('should be accessible', () => {
  cy.injectAxe()
  cy.mountWithProviders(<YourComponent {...meaningfulProps} />)
  cy.checkAccessibility()

  // Optional: Can be removed after all keys verified
  // cy.checkI18nKeys()
})
```

---

## How It Detects Errors

**Test Run Example:**

```
WizardGroupForm
  rendering
    1) should render successfully

1 failing

1) WizardGroupForm
     rendering
       should render successfully:

❌ Missing i18n translation keys detected:

  - translation:workspace.wizard.group.configure
  - translation:workspace.grouping.editor.contentManagement

Add these keys to src/locales/en/translation.json
```

---

## Fixing Missing Keys

### Step 1: Find the Component

```bash
grep -r "workspace.wizard.group.configure" src/
```

### Step 2: Check Translation File

```bash
grep "configure" src/locales/en/translation.json
```

### Step 3: Add Missing Key

Add to appropriate section in `translation.json`:

```json
{
  "workspace": {
    "wizard": {
      "group": {
        "configTitle": "Configure Group"
      }
    }
  }
}
```

### Step 4: Update Component (if needed)

```typescript
// Use the correct existing key
<Heading>{t('workspace.wizard.group.configTitle')}</Heading>
```

### Step 5: Re-run Test

```bash
pnpm cypress:run:component --spec "src/path/to/Component.spec.cy.tsx"
```

---

## Real Example: WizardGroupForm

### Missing Keys Found

1. `workspace.wizard.group.configure` - Used in WizardGroupForm.tsx line 79
2. `Content Management` - Hardcoded string in GroupContentEditor.tsx line 96

### Fixes Applied

**Fix 1: Use existing key**

```typescript
// Before
<Heading>{t('workspace.wizard.group.configure')}</Heading>

// After (key already existed as 'configTitle')
<Heading>{t('workspace.wizard.group.configTitle')}</Heading>
```

**Fix 2: Add key and use it**

```typescript
// Before
<CardHeader>{t('Content Management')}</CardHeader>

// After
// 1. Added key to translation.json:
"grouping": {
  "editor": {
    "contentManagement": "Content Management"
  }
}

// 2. Updated component:
<CardHeader>{t('workspace.grouping.editor.contentManagement')}</CardHeader>
```

### Test Result

```bash
✔  All specs passed!
Tests: 12 passing
Duration: 5s
```

---

## Benefits

1. **Early Detection** - Catches missing keys during test runs, not in production
2. **Clear Error Messages** - Shows exactly which keys are missing
3. **Systematic** - Integrated into standard testing workflow
4. **Preventive** - Can be kept permanently for regression detection
5. **Fast Feedback** - Fails immediately when component renders with missing key

---

## Files Modified

### Created

1. `cypress/support/commands/checkI18nKeys.ts` - Custom Cypress command

### Modified

1. `src/config/i18n.config.ts` - Added `missingKeyHandler`
2. `cypress/support/component.ts` - Import checkI18nKeys command
3. `src/types/cypress.d.ts` - Added TypeScript definition
4. `.tasks/TESTING_GUIDELINES.md` - Added comprehensive guideline section
5. `src/modules/Workspace/components/wizard/steps/WizardGroupForm.tsx` - Fixed key usage
6. `src/modules/Workspace/components/parts/GroupContentEditor.tsx` - Fixed key usage
7. `src/locales/en/translation.json` - Added `contentManagement` key

---

## Decision: Keep or Remove cy.checkI18nKeys()?

### Current Recommendation: Remove After Verification

**Reasoning:**

- Similar to other development-time checks
- i18n config still logs to console
- Faster test execution
- Cleaner test code

**Alternative: Keep Permanently**

Pros:

- Continuous regression detection
- Same philosophy as permanent accessibility testing
- Catches accidental key removal

Cons:

- Slightly slower tests
- More verbose test code

**Team Decision:** Document both options in guidelines, let developers choose based on component criticality.

---

## Related Documentation

- [TESTING_GUIDELINES.md](../.tasks/TESTING_GUIDELINES.md) - Section: "i18n Translation Key Testing"
- [I18N_GUIDELINES.md](../.tasks/I18N_GUIDELINES.md) - i18next best practices
- Component test examples in `src/modules/Workspace/components/wizard/steps/*.spec.cy.tsx`
