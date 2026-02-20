---
name: debug-cypress
description: >
  Use this skill when a Cypress test is failing and the cause is not immediately obvious.
  Invoke automatically when: a test throws "element not found", "expected to find element",
  "element is disabled", or any assertion failure. Also invoke before reaching for
  {force: true}, cy.wait(), or rewriting a test from scratch — investigate first.
argument-hint: '[failing-test-file]'
disable-model-invocation: false
user-invocable: true
allowed-tools: Bash, Grep, Read, Glob
---

# Debug Cypress

Systematic investigation protocol for Cypress test failures.

**Rule: if you cannot answer "where in the code is this defined?" — stop guessing and find it.**

---

## When to invoke

Invoke this skill when:

- A Cypress test fails with "element not found", "expected element", or any assertion failure
- A test passes locally but fails in CI
- You are about to use `{force: true}`, `cy.wait(500)`, or rewrite a test
- You are about to make an assumption about a selector, label, or text value

---

## Investigation protocol — follow in order, do not skip steps

### Step 1 — Read the exact error

Copy the full error message. Note:
- The exact selector or text it was looking for
- The line number that failed
- Any "found N elements, expected 1" details

Do not guess from a partial error.

### Step 2 — Find the implementation

Locate where the element is defined in source:

```bash
# Search by visible text or aria-label
grep -r "the-label-from-error" src --include="*.tsx" -l

# Search by data-testid
grep -r "data-testid.*the-id" src --include="*.tsx"

# Search for the component by name
grep -r "ComponentName" src --include="*.tsx" -l
```

Read the found file. Do not assume — look at the actual JSX.

### Step 3 — Resolve translations (if using t())

If the component uses `aria-label={t('some.key', { context: value })}`:

```bash
# The resolved key is: some.key_{CONTEXT_VALUE}
grep -r "some.key_CONTEXT_VALUE" src --include="*.json"
```

Translation files:
- Main app: `src/locales/en/`
- DataHub: `src/extensions/datahub/locales/en/datahub.json`

### Step 4 — Check how other tests use the same element

```bash
# Find existing usage of the selector
grep -r "the-selector-or-label" cypress/e2e --include="*.cy.ts"
grep -r "the-selector-or-label" src --include="*.spec.cy.tsx"
```

If other tests already select this element correctly, match their approach exactly.

### Step 5 — Check the Page Object Model

```bash
# Check if the POM already has a getter
grep -r "relevant-term" cypress/pages --include="*.ts"
```

Page objects live in `cypress/pages/{Module}/{ModulePage}.ts`.

If the element exists but has no POM getter: add one. Never use raw selectors in test bodies.

### Step 6 — Fix and verify

- Update the POM getter if needed
- Update the test to use the POM getter
- Run the specific test once to verify:

```bash
pnpm cypress:run:component --spec "src/path/to/Component.spec.cy.tsx"
pnpm cypress:run:e2e --spec "cypress/e2e/path/to/test.spec.cy.ts"
```

---

## Quick command reference

```bash
# Find element by visible text
grep -r "Button Label" src --include="*.tsx" -l

# Find aria-label usage in a component
grep -r "aria-label" src/modules/ModuleName --include="*.tsx"

# Resolve a context-based translation
# t('workspace.nodes.type', { context: 'FUNCTION' }) → key: workspace.nodes.type_FUNCTION
grep -r "nodes.type_FUNCTION" src --include="*.json"

# Find how other tests select an element
grep -r "aria-label.*Save" cypress/e2e --include="*.cy.ts"

# Inspect a page object
grep -A 3 "get save\|get submit" cypress/pages/Bridges/BridgePage.ts
```

---

## What good investigation looks like

**Error:** `Expected to find element: [aria-label="Function"]`

1. Search source: `grep -r "aria-label.*Function" src --include="*.tsx"`
2. Find: `aria-label={t('workspace.nodes.type', { context: nodeType })}`
3. Resolve key: `workspace.nodes.type_FUNCTION` → value: `"JS Function"`
4. Fix POM: use `"JS Function"` not `"Function"`
5. Run test once — passes

**Total time: 3–5 minutes.**

---

## Execution and artifacts

### Running tests

```bash
# ✅ Always use pnpm — never npx cypress run
pnpm cypress:run:component --spec "src/path/to/Component.spec.cy.tsx"
pnpm cypress:run:e2e --spec "cypress/e2e/path/to/test.spec.cy.ts"

# Filter noisy output
pnpm cypress:run:e2e --spec "..." 2>&1 | head -100          # First 100 lines
pnpm cypress:run:e2e --spec "..." 2>&1 | grep -E "passing|failing"  # Summary only
```

**Never prepend `rm -rf cypress/videos`** — `rm` is not in the allowed commands list. Cypress overwrites videos automatically.

### CLI output — what you get

The CLI output contains the full error with the exact selector and line number:

```
AssertionError: Expected to find element: [aria-label="Save"], but never found it.
  at Context.eval (cypress/e2e/Bridges/bridge.spec.cy.ts:47:7)
```

This is enough to start Step 2 (find the implementation). You do not need screenshots.

### DOM state snapshot

If you need to see what elements are actually on the page, add `cy.logDOMState()` before the failing assertion. It writes a JSON file to `cypress/html-snapshots/` with all IDs, `data-testid` values, and ARIA roles visible at that moment:

```bash
cat cypress/html-snapshots/dom-state-*.json | jq '.availableTestIds'
```

This is more useful than a screenshot: you get a machine-readable list of every selector available on the page.

### Accessibility violation details in CI

If `cy.checkAccessibility()` fails but the log shows no violation details, check `cypress.config.ts`:

```typescript
installLogsPrinter(on, { printLogsToConsole: 'onFail' })  // not 'never'
```

And `cypress/support/e2e.ts`:

```typescript
installLogsCollector({ collectTypes: ['cy:log', 'cy:command'] })
```

Without `cy:log` in `collectTypes`, axe violation details are swallowed.

---

## Never do this

- Add `{force: true}` to click a covered element — find the correct element instead
- Add `cy.wait(500)` — use `cy.should()` assertions to wait for state
- Assume the selector is correct and the app is broken
- Rewrite the test before understanding why it fails
- Use `npx cypress run` — use `pnpm cypress:run:e2e` instead
