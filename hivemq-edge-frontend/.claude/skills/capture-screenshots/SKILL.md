---
name: capture-screenshots
description: >
  Use this skill when any artifact needs visual evidence of UI state. Invoke automatically when:
  creating a PR with UI changes (invoke before writing the PR body), updating docs/ with new
  screenshots, or preparing blog post visuals. Trigger phrases: "I need screenshots",
  "capture the UI", "show before/after", "add visuals to the PR".
argument-hint: '[component-or-route] [--for pr|docs|blog]'
disable-model-invocation: false
user-invocable: true
allowed-tools: Bash, Read, Glob, Grep
---

# Screenshot Capture

Captures consistent UI screenshots using Cypress for PRs, documentation, and blog posts.

---

## When to invoke

Invoke this skill when:

- A PR includes UI changes — run this **before** writing the PR body
- Documentation needs current screenshots of UI state
- A blog post or announcement needs visuals
- The user explicitly asks for screenshots or before/after comparisons

---

## Storage and naming by purpose

| Purpose | Storage                        | Naming convention                              |
| ------- | ------------------------------ | ---------------------------------------------- |
| PR      | `.tasks-log/screenshots/`      | `before-{feature}.png` / `after-{feature}.png` |
| Docs    | `docs/assets/screenshots/`     | `{feature}-{state}.png`                        |
| Blog    | `.tasks-log/screenshots/blog/` | `{feature}-hero.png`                           |

Create the target directory if it does not exist.

---

## Preferred method: inject `cy.screenshot()` temporarily

This is the most reproducible approach. Add screenshot commands to an existing E2E test, run it, move the output, then remove the commands.

### Step 1 — Find the right test

```bash
# Find tests covering the feature
grep -r "{feature-keyword}" cypress/e2e --include="*.cy.ts" -l
```

### Step 2 — Add a dedicated PR screenshot block

Add a separate describe block so screenshot tests stay isolated from functional tests:

```typescript
describe('Visual Regression - PR Screenshots', { tags: ['@percy'] }, () => {
  it('should capture {scenario}', () => {
    // Set up the full, realistic scenario
    // Navigate to the feature
    // Wait for all content to load

    cy.percySnapshot('{Feature} - {Scenario}')
    cy.screenshot('{feature}-{scenario}', { capture: 'viewport', overwrite: true })
  })
})
```

### Step 3 — Run the test

```bash
pnpm cypress:run:e2e --spec "cypress/e2e/{path}/{file}.spec.cy.ts"
```

Screenshots save to `cypress/screenshots/{test-file}/`.

### Step 4 — Move to correct location

Move output files to the storage path for the intended purpose (see table above).

### Step 5 — Remove temporary commands

Remove all `cy.screenshot()` commands added in step 2 before committing.
`cy.percySnapshot()` calls inside `@percy` describe blocks may stay.

---

## Alternative: headed mode (quick one-off)

When you need a fast capture without modifying test files:

```bash
pnpm cypress:open:e2e
```

Select E2E Testing → Chrome → run the test. Use `Cmd+Shift+4` (macOS) to capture when the desired state appears.

---

## BEFORE screenshots

For before/after comparisons where the old state is in git history:

```bash
git stash
git checkout {commit-before-change}
pnpm dev   # Screenshot the old state manually via headed mode
git checkout -
git stash pop
```

If the old state is not available: state "Before screenshot not available" in the PR. Do not fabricate state.

---

## Quality checklist

- [ ] Viewport: 1400×1016 (document in caption if different)
- [ ] Format: PNG, under 200 KB per image
- [ ] Descriptive filename: `{feature}-{state}.png`
- [ ] All temporary `cy.screenshot()` commands removed from test files
- [ ] Files stored in the correct location for their purpose
- [ ] Markdown image paths are relative
