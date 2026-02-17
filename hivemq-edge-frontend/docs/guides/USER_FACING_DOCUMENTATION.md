---
title: "User-Facing Documentation"
author: "Edge Frontend Team"
last_updated: "2026-02-17"
purpose: "How we create, maintain, and generate all user-facing documentation — external docs, PR descriptions, feature announcements, and screenshots"
audience: "Frontend developers, AI agents writing documentation or PRs"
maintained_at: "docs/guides/USER_FACING_DOCUMENTATION.md"
---

# User-Facing Documentation

---

## Table of Contents

- [Documentation Layers](#documentation-layers)
- [External HiveMQ Documentation](#external-hivemq-documentation)
- [PR Descriptions](#pr-descriptions)
- [Feature Announcements (Release Notes / Blog Posts)](#feature-announcements-release-notes--blog-posts)
- [Screenshot Generation](#screenshot-generation)
  - [Philosophy](#philosophy)
  - [Two Types of Screenshot Specs](#two-types-of-screenshot-specs)
  - [Writing a Screenshot Spec](#writing-a-screenshot-spec)
  - [Running and Collecting Screenshots](#running-and-collecting-screenshots)
  - [Common Failures and Fixes](#common-failures-and-fixes)
  - [Screenshot Naming and Storage](#screenshot-naming-and-storage)

---

## Documentation Layers

The frontend team produces documentation at three levels. Each has a different audience, format, and tooling:

| Layer | What | Audience | Author | Format |
|-------|------|----------|--------|--------|
| **External docs** | Product documentation on docs.hivemq.com | End users, operators | Frontend developer (manual) | Markdown + screenshots |
| **PR descriptions** | GitHub pull request description | Reviewers, product team | AI agent (templated) | Markdown |
| **Feature announcements** | Release notes, blog posts, changelogs | Users, marketing | AI agent (templated) | Markdown |

All three layers share a common dependency: **screenshots generated from Cypress tests**. See [Screenshot Generation](#screenshot-generation).

---

## External HiveMQ Documentation

### URLs

- **HiveMQ Edge documentation:** [https://docs.hivemq.com/hivemq-edge/index.html](https://docs.hivemq.com/hivemq-edge/index.html)
- **Data Hub documentation:** [https://docs.hivemq.com/hivemq/latest/data-hub/index.html](https://docs.hivemq.com/hivemq/latest/data-hub/index.html)

### Who Writes It

Writing and updating the external documentation is the **frontend developer's responsibility** when a feature ships to production. It is not a separate technical writer's job.

### What Triggers an Update

Update the external docs when:

- A new feature is shipped that users need to know how to use
- An existing feature's UI or behaviour changes significantly
- A feature is removed or deprecated
- Screenshots are outdated (UI redesign, Chakra version change, etc.)

### How It Is Written

The external docs use **Markdown**. They live in a separate repository (not this one) and are published via a static site generator. The frontend developer:

1. Writes the Markdown content following the product documentation style guide
2. Generates screenshots using the [Screenshot Generation](#screenshot-generation) process below
3. Submits the documentation update as a PR to the docs repository

### What to Include

- **Step-by-step instructions** — assume users have no prior knowledge
- **Screenshots** — for every non-trivial UI screen or interaction
- **Conceptual explanations** — what the feature is for, not just how to use it
- **Cross-references** — link to related features (e.g., Data Hub ↔ schemas ↔ policies)

> [!NOTE]
> The Data Hub docs live under the main HiveMQ docs URL, not the Edge-specific URL, because the Data Hub feature is shared between HiveMQ Edge and HiveMQ Platform. Ensure screenshots and examples are compatible with both.

---

## PR Descriptions

Every pull request must have a structured, user-centric description. The template and full guidelines live in:

**`.tasks/PULL_REQUEST_TEMPLATE.md`**

### Key Principles

The PR description is for **product managers, designers, and reviewers** — not just engineers. Write it from the user's perspective.

- **Lead with user value:** What changes for users? What can they do now that they couldn't before?
- **BEFORE / AFTER structure:** Show the transformation with screenshots
- **Screenshots for every UI change:** Screenshots come from dedicated Cypress test describe blocks (see below)
- **No code dumps:** Implementation details belong in code review comments, not the description

### The Screenshot Requirement

PR descriptions require **BEFORE** and **AFTER** screenshots. These must come from Cypress tests — not manual screenshots. See [Screenshot Generation](#screenshot-generation) for the exact process.

Screenshots for PRs are stored in `.tasks/{task-id}/screenshots/` and referenced in the PR Markdown with relative paths.

**See:** [`.tasks/PULL_REQUEST_TEMPLATE.md`](../../.tasks/PULL_REQUEST_TEMPLATE.md) for the complete template and writing guide.

---

## Feature Announcements (Release Notes / Blog Posts)

Each shipped feature that is user-visible gets a short user-facing write-up. These are collected at release time into the release notes or a blog post. The template and full guidelines live in:

**`.tasks/USER_DOCUMENTATION_GUIDELINE.md`**

### Structure

Every feature announcement follows a four-section structure:

1. **What It Is** — One paragraph defining the feature in plain language with the primary benefit leading
2. **How It Works** — Numbered steps a user can follow immediately; 3–6 steps; includes a screenshot
3. **How It Helps** — 3–4 subsections articulating concrete user benefits
4. **Looking Ahead** — For new/experimental features: sets expectations and invites feedback

Target length: **~500 words** (leaves room for other features in a multi-feature announcement).

### Where the Output Goes

The `USER_DOCUMENTATION.md` file lives in the task directory:

```
.tasks/{TASK_ID}-{TASK_NAME}/USER_DOCUMENTATION.md
```

At release time, all feature `USER_DOCUMENTATION.md` files are combined into a single document with a top-level `#` heading.

**See:** [`.tasks/USER_DOCUMENTATION_GUIDELINE.md`](../../.tasks/USER_DOCUMENTATION_GUIDELINE.md) for tone guide, section templates, and pitfall list.

---

## Screenshot Generation

### Philosophy

Every screenshot in every piece of documentation — external docs, PR descriptions, or feature announcements — **must come from a Cypress test**. Manual screenshots are not acceptable.

**Why:**

- **Reproducibility:** Anyone can regenerate any screenshot by running a single test command
- **Consistency:** Standard viewport (1280×720 HD), clean UI state, no dev tools visible
- **No sensitive data:** The test controls what data is visible; there is no risk of credentials or internal IPs appearing
- **Maintainability:** When the UI changes, re-running the test updates the screenshot

The rule: **if a screenshot cannot be regenerated by running a test, it should not be in the documentation**.

---

### Two Types of Screenshot Specs

#### Type 1: Documentation Screenshots (Permanent)

These live permanently in `docs/assets/screenshots/{domain}/` and are referenced in long-lived documentation like architecture docs, guides, and external docs.

**Characteristics:**

- One dedicated spec file per domain: `cypress/e2e/{domain}/{domain}-documentation-screenshots.spec.cy.ts`
- Screenshots are committed to the repository
- Must be regenerated when the UI changes materially
- Tracked in `docs/assets/screenshots/INDEX.md`

**Example:**

```
cypress/e2e/datahub/datahub-documentation-screenshots.spec.cy.ts
→ generates → cypress/screenshots/datahub/.../
→ copy to → docs/assets/screenshots/datahub/datahub-designer-canvas-empty.png
```

#### Type 2: PR Screenshots (Per Task)

These are generated for a specific PR and stored in the task directory. They are not committed to the permanent screenshot store.

**Characteristics:**

- A dedicated `describe` block inside the feature's E2E test, or a separate `*-pr-screenshots.spec.cy.ts` file
- Stored in `.tasks/{task-id}/screenshots/`
- Referenced in the PR Markdown description
- Not committed to `docs/assets/screenshots/`

**Example:**

```
cypress/e2e/workspace/workspace-pr-screenshots.spec.cy.ts
→ generates → cypress/screenshots/workspace/.../
→ copy to → .tasks/38943-mapping-ownership-review/screenshots/
```

> [!IMPORTANT]
> Keep PR screenshot tests in a **separate `describe` block** from functional tests. PR screenshots need realistic, complete scenarios showing the whole feature. Functional tests are narrow and targeted. Mixing them produces screenshots that miss context.

---

### Writing a Screenshot Spec

This is the exact pattern to follow. Do not deviate from it — the viewport, intercepts, and wait are not optional.

```typescript
/// <reference types="cypress" />

import { cy_interceptCoreE2E } from 'cypress/utils/intercept.utils.ts'

/**
 * Documentation screenshots for [Feature Name].
 * After running: copy screenshots from cypress/screenshots/.../ to docs/assets/screenshots/{domain}/
 *
 * Run with:
 * pnpm cypress:run:e2e --spec "cypress/e2e/{domain}/{filename}.spec.cy.ts"
 */
describe('[Feature] - Documentation Screenshots', () => {
  beforeEach(() => {
    // REQUIRED: HD viewport for all documentation screenshots
    cy.viewport(1280, 720)

    // REQUIRED: sets up auth, config, and core API intercepts
    cy_interceptCoreE2E()

    // REQUIRED: mock every API the page will call
    // A missing intercept = loading spinner in screenshot
    cy.intercept('/api/v1/...', { items: [] })
  })

  it('should capture [state description]', () => {
    cy.visit('/path/to/feature')

    // REQUIRED: allow React rendering and animations to settle
    // 800ms is the established minimum for canvas-based pages
    cy.wait(800)

    // Optionally interact: cy.getByTestId('...').click()
    // Then wait for the result: cy.getByTestId('result').should('be.visible')

    cy.screenshot('[feature]-[state]', {
      capture: 'viewport', // Always viewport, never fullPage
      overwrite: true,     // Re-runs replace previous screenshots
    })
  })
})
```

#### Key Rules

**`cy.viewport(1280, 720)` in `beforeEach` is mandatory.**
All documentation screenshots must use 1280×720. Set it in `beforeEach`, not once at the top of the file — Cypress resets the viewport between tests.

**`cy_interceptCoreE2E()` is mandatory.**
Without it, the auth wall or config loading will block the page from rendering. This helper sets up all the baseline intercepts the app needs to boot.

**Mock every API call the page makes.**
If any API call is not intercepted, the page will show a loading state or error in the screenshot. Use the browser's Network tab (in headed mode) to discover what the page calls.

**`cy.wait(800)` after `cy.visit()` is acceptable here.**
This is an exception to the general "no arbitrary waits" rule. Screenshot tests are not functional tests — we are waiting for visual stability (CSS transitions, canvas layout), not for a specific DOM state. 800ms is the established minimum; increase to 1200ms for complex canvas pages.

**Use `capture: 'viewport'`.**
`fullPage` captures the entire scrollable height, which is rarely what you want. `viewport` captures exactly what a user sees at 1280×720.

**Use `overwrite: true`.**
Without this, Cypress appends a counter suffix (`screenshot (1).png`) after the first run.

---

### Running and Collecting Screenshots

#### Step 1: Run the spec headlessly

```bash
pnpm cypress:run:e2e --spec "cypress/e2e/{domain}/{filename}.spec.cy.ts"
```

#### Step 2: Find the screenshots

Cypress saves screenshots to:

```
cypress/screenshots/{path-to-spec}/{test-name}/{screenshot-name}.png
```

For a spec at `cypress/e2e/datahub/datahub-documentation-screenshots.spec.cy.ts`:

```
cypress/screenshots/datahub/datahub-documentation-screenshots.spec.cy.ts/
  DataHub - Documentation Screenshots -- Policy Designer Canvas -- should capture empty policy designer canvas/
    datahub-designer-canvas-empty.png
```

#### Step 3: Copy to the right destination

**For documentation screenshots:**

```bash
cp cypress/screenshots/.../screenshot-name.png docs/assets/screenshots/{domain}/screenshot-name.png
```

**For PR screenshots:**

```bash
cp cypress/screenshots/.../*.png .tasks/{task-id}/screenshots/
```

#### Step 4: Update the index (documentation screenshots only)

Add or update the entry in `docs/assets/screenshots/INDEX.md` with:

- Screenshot filename
- Description
- Which documentation file uses it
- Which test generates it

#### Step 5: Reference in documentation

```markdown
<!-- From docs/architecture/*.md — path relative to the .md file -->
![DataHub policy designer — empty canvas](../assets/screenshots/datahub/datahub-designer-canvas-empty.png)

<!-- From docs/guides/*.md -->
![Feature state description](../assets/screenshots/{domain}/screenshot-name.png)
```

Always include descriptive alt text. "Screenshot of feature" is not descriptive. Describe what the user sees.

---

### Common Failures and Fixes

#### Screenshot shows a loading spinner

**Cause:** An API call is not intercepted. The page is waiting for a response.

**Fix:** Run the test in headed mode (`pnpm cypress:open:e2e`) and watch the Network tab. Add `cy.intercept()` for every unhandled call. A `{ items: [] }` response is enough to show an empty state.

#### Screenshot shows the login page

**Cause:** `cy_interceptCoreE2E()` is missing or called after `cy.visit()`.

**Fix:** Call `cy_interceptCoreE2E()` in `beforeEach`, before any `cy.visit()`.

#### Screenshot is partially rendered or has a flash

**Cause:** `cy.wait(800)` is too short. CSS transitions or React rendering hasn't settled.

**Fix:** Increase to `cy.wait(1200)` or `cy.wait(1500)` for complex pages. Add a specific assertion before the screenshot to confirm the key element is visible:

```typescript
cy.getByTestId('policy-canvas').should('be.visible')
cy.wait(800)
cy.screenshot(...)
```

#### Screenshot filename has a counter suffix (`name (1).png`)

**Cause:** `overwrite: true` is missing.

**Fix:** Add `overwrite: true` to the `cy.screenshot()` options.

#### Cypress can't find the element I want to interact with before screenshotting

**Cause:** The element needs the correct test ID or you need to wait for a specific state.

**Fix:** Follow the standard selector priority: `cy.getByTestId(...)` first, then ARIA roles. See the [Cypress Guide](./CYPRESS_GUIDE.md) for selectors.

---

### Screenshot Naming and Storage

#### Naming Convention

```
{feature}-{state}-{descriptor}.png
```

Examples:

- `datahub-designer-canvas-empty.png`
- `datahub-schema-table-with-data.png`
- `workspace-healthy-all-operational.png`
- `combiner-mapping-drawer-open.png`

Use lowercase with hyphens. No spaces. No underscores.

#### Permanent Storage (`docs/assets/screenshots/`)

```
docs/assets/screenshots/
├── datahub/           # DataHub extension screenshots
├── workspace/         # Workspace canvas screenshots
├── adapters/          # Protocol adapter screenshots
├── bridges/           # Bridge configuration screenshots
├── ui-components/     # Shared UI component screenshots
├── development/       # Dev tools and process screenshots
└── common/            # Common UI elements
```

Each domain directory must be created before copying screenshots into it. Every screenshot must be registered in `docs/assets/screenshots/INDEX.md`.

#### PR / Task Storage (`.tasks/{task-id}/screenshots/`)

Not committed to the permanent store. Not tracked in `INDEX.md`. Referenced from the PR Markdown description only.

---

## Related Documents

| Document | What it covers |
|----------|---------------|
| [`docs/assets/screenshots/INDEX.md`](../assets/screenshots/INDEX.md) | Registry of all permanent documentation screenshots |
| [`.tasks/PULL_REQUEST_TEMPLATE.md`](../../.tasks/PULL_REQUEST_TEMPLATE.md) | Full PR description template and writing guide |
| [`.tasks/USER_DOCUMENTATION_GUIDELINE.md`](../../.tasks/USER_DOCUMENTATION_GUIDELINE.md) | Feature announcement template, tone guide, pitfall list |
| [`docs/guides/CYPRESS_GUIDE.md`](./CYPRESS_GUIDE.md) | Cypress selectors, custom commands, debugging |
| [`docs/guides/TESTING_GUIDE.md`](./TESTING_GUIDE.md) | General testing philosophy and patterns |
