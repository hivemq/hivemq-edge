---
name: create-blogpost
description: >
  Use this skill when writing user-facing documentation for a blog post, release notes,
  or feature announcement. Invoke automatically when: the user asks to "write a blog post",
  "write release notes", "document this feature for users", or "write a feature announcement".
  Produces structured ~500-word feature write-ups in plain language for non-technical readers
  (DevOps engineers, system administrators — not frontend developers).
argument-hint: '[linear-issue-id or feature-name]'
disable-model-invocation: false
user-invocable: true
allowed-tools: Bash, Read, Glob, Grep
---

# Create Blog Post

Structured approach for writing user-facing feature documentation: blog posts, release notes,
and feature announcements.

**Rule: Write for users, not developers. Describe what changes for the user — not how the code works.**

---

## When to invoke

Invoke this skill when:

- The user asks to write a blog post, release notes, or a feature announcement
- The user says "document this feature for users" or "write a user-facing write-up"
- A PR or feature needs external communication material

---

## Document structure

A feature write-up has four sections in this order:

### Section 1 — What It Is

One sentence on what the feature is, followed by 3–7 bullet points listing the available
options or capabilities. Lead with the user benefit.

```markdown
### What It Is

HiveMQ Edge now includes **automatic layout algorithms** that organize your workspace nodes.

- **Dagre Vertical** — Top-to-bottom flow, ideal for sequential architectures
- **Radial Hub** — Edge node centered, connections radiating outward
- **Force-Directed** — Physics-based clustering that reveals natural groupings
```

**Target:** 75–150 words

### Section 2 — How It Works

3–6 numbered steps the user can follow immediately. One action per step. Bold the UI element
or action. Add a screenshot after the steps.

```markdown
### How It Works

1. **Open your workspace** and locate the Layout Controls in the toolbar
2. **Select an algorithm** from the dropdown menu
3. **Click Apply Layout** to reorganize your nodes instantly

![Workspace Layout Controls showing the algorithm selector](./screenshot-layout-controls.png)
```

**Screenshot:** Use the `capture-screenshots` skill (`--for blog`) to generate the image.
Alt text must describe what is visible in the screenshot, not just name the feature.

**Target:** 80–120 words

### Section 3 — How It Helps

3–4 subsections (H4), each covering one user benefit. One or two sentences each, with a
concrete example or use case.

```markdown
### How It Helps

#### Better Visualization

See your MQTT architecture clearly without manual positioning. Different layouts reveal
different aspects of your topology.

#### Faster Setup

Apply a layout in one click, then save it as a reusable preset for consistent workspaces.
```

**Target:** 100–150 words

### Section 4 — Looking Ahead (new or experimental features only)

State the current maturity, explain how user feedback drives improvements, set expectations.
Omit this section for stable, established features.

```markdown
### Looking Ahead

These layouts represent our **initial implementation**. We're actively collecting feedback
from real-world MQTT topologies to refine the algorithms. Consider them a starting point —
share your use case if you see improvement opportunities.
```

**Target:** 80–120 words

### Closing call-to-action

One or two sentences. Specific (name the feature), action-oriented, encouraging.

```markdown
---

**Try the new layouts in your workspace and discover which arrangement works best for your architecture.**
```

---

## Tone and voice

| Do                                           | Avoid                                                                    |
| -------------------------------------------- | ------------------------------------------------------------------------ |
| Second person: "You can", "Your workspace"   | First or third person narration                                          |
| Active voice: "Click the button"             | Passive voice: "The button can be clicked"                               |
| Specific benefits with examples              | Vague claims: "improved UX", "better performance"                        |
| Plain language                               | Technical jargon without definition (no "DAG", "constraint propagation") |
| Contractions: "You'll notice", "Don't worry" | Marketing hyperbole: "revolutionary", "game-changing"                    |

---

## Word count targets

| Section       | Target                  |
| ------------- | ----------------------- |
| Title         | <70 characters          |
| What It Is    | 75–150 words            |
| How It Works  | 80–120 words            |
| How It Helps  | 100–150 words           |
| Looking Ahead | 80–120 words (optional) |
| CTA           | 1–2 sentences           |
| **Total**     | **~500 words**          |

Target 500 words to leave room for other features when combining into a multi-feature announcement.

---

## Combining into a release post

When multiple features are documented independently, combine them:

```markdown
# HiveMQ Edge Release Notes — Q4 2025

## Feature 1: Workspace Auto-Layout: Organize Your MQTT Architecture Effortlessly

[Feature 1 content]

## Feature 2: Duplicate Combiner Detection: Clearer Configuration Feedback

[Feature 2 content]
```

Each feature's title becomes an H2. Each feature's internal sections become H3/H4 as written.

---

## Pre-publish checklist

- [ ] All four required sections present (What / How / Helps / Looking Ahead if experimental)
- [ ] Title: H2 with feature name and value proposition, under 70 characters
- [ ] ~500 words ±10%
- [ ] Second person, active voice throughout
- [ ] No technical jargon without explanation
- [ ] Screenshot included with descriptive alt text (if UI feature)
- [ ] Closing call-to-action present
- [ ] No code snippets in the body
- [ ] "Looking Ahead" present for new/experimental features

---

## Never do this

- Open with implementation details ("This PR refactors…", "We implemented a new algorithm…")
- Use passive voice as the default register
- List acceptance criteria or internal ticket notes
- Include code snippets in the body text
- Write vague benefits without concrete examples
