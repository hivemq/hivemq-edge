---
name: create-pr
description: >
  Use this skill when creating or writing a pull request description.
  Invoke automatically when: the user says "create a PR", "open a pull request",
  "submit for review", "write the PR body", or "push this for review".
  If the PR includes UI changes, invoke the capture-screenshots skill before
  writing the description.
argument-hint: '[linear-issue-id]'
disable-model-invocation: false
user-invocable: true
allowed-tools: Bash, Read, Glob, Grep, mcp__linear-server__get_issue
---

# Create PR

Structured approach for writing effective, user-centric pull request descriptions.

**Rule: The PR description is for understanding user value and testing the change — not for explaining how the code works.**

---

## When to invoke

Invoke this skill when:

- The user asks to create, open, or write a pull request
- The user says "submit for review", "push this for review", or "write the PR body"
- You are about to run `gh pr create`

If the PR includes UI changes: invoke the `capture-screenshots` skill **before** writing the description.

---

## Before writing

### 1 — Get the Linear issue URL

Use `mcp__linear-server__get_issue` with the issue identifier:

```bash
# Returns the full issue URL needed for the PR header
mcp__linear-server__get_issue(id: "EDG-40")
```

### 2 — Capture screenshots (UI changes only)

If this PR touches any UI: run the `capture-screenshots` skill first (`--for pr`).
Screenshots must appear in the AFTER section before submitting.

---

## Writing guidelines

**Audience:** Product managers, designers, and developers — not just technical reviewers.

| Do                               | Avoid                            |
| -------------------------------- | -------------------------------- |
| Start with what users gain       | Replicating the Linear ticket    |
| Use plain language               | Technical jargon in the opening  |
| Screenshot first, explain second | Code snippets in the description |
| State explicit user benefits     | Vague improvements ("better UX") |

**Opening formula:**

```
This PR [transforms/enhances/improves] how users [do something].
Previously, [limitation]. Now, [new capability].
```

**Good:** "This PR transforms how users understand their data pipelines by showing both runtime and configuration status through intuitive visual feedback."

**Bad:** "This PR implements a dual-status model with Runtime and Operational enums and refactors 10 node components."

---

## PR structure

````markdown
# Pull Request: [User-facing feature name]

**Linear Issue:** https://linear.app/hivemq/issue/[ISSUE-ID]/[slug]

---

## Description

[Opening paragraph — user value proposition]

The enhancement introduces:

- **[Key improvement]**: [User-facing description]
- **[Key improvement]**: [User-facing description]

### User Experience Improvements

**What users gain:**

- **[Benefit]**: [How it helps]
- **[Benefit]**: [How it helps]

### Technical Summary

- [High-level point, no code]
- [High-level point, no code]

---

## BEFORE

### Previous behavior

[One paragraph + 3–5 limitation bullets]

---

## AFTER

### New behavior

#### 1. [Scenario name]

![After - Scenario](./screenshots/after-[feature].png)

_Test: `cypress/e2e/[path]/[file].spec.cy.ts`_
_Screenshot: 1400×1016 viewport_

**Key Visual Elements:**

- **[Element]**: [What it shows]

**User Benefits:** [One paragraph]

---

## Test Coverage

- **[X]+ tests total, all passing ✅**
- **Unit tests**: [What they cover]
- **E2E tests**: [What they cover]

---

## Breaking Changes

None. / [List with compatibility notes]

---

## Accessibility

- ✅ [Key a11y feature]
- ✅ Tested with axe-core via `cy.checkAccessibility()`

---

## Documentation

- `[file]` — [What was added/updated]

---

## Reviewer Notes

**Focus areas:**

1. [What to review]
2. [What to review]

**Manual testing:**

1. [Step]
2. [Step]
3. Observe: [Expected result] ✅

**Quick test commands:**

```bash
pnpm cypress:run:e2e --spec "cypress/e2e/[path]/[file].spec.cy.ts"
```
````

```

---

## Submission checklist

- [ ] Title is user-focused, not technical
- [ ] Linear issue link is correct
- [ ] Description opens with user value proposition
- [ ] No code snippets in the description section
- [ ] AFTER section has screenshots with Cypress test captions
- [ ] Test coverage is summarized (count only, not exhaustive list)
- [ ] Breaking changes stated (or "None")
- [ ] Accessibility section present (UI changes)
- [ ] Reviewer notes provide clear testing steps
- [ ] No replication of Linear ticket content

---

## Never do this

- Open description with implementation details ("This PR refactors…")
- Include code snippets in the description (save for Files Changed / code review)
- Write vague benefits ("improved UX", "better performance")
- Skip screenshots for UI changes
- Replicate the entire Linear ticket
```
