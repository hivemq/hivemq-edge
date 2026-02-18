---
name: technical-doc-review
description: Review technical documentation against the HiveMQ writing guidelines, producing a structured quality report with actionable findings per file
argument-hint: '[path/to/file.md | path/to/directory] [--summary-only]'
disable-model-invocation: false
user-invocable: true
allowed-tools: Glob, Grep, Read
---

# Technical Documentation Reviewer

Reviews Markdown documentation against the HiveMQ Technical Documentation Writing Guidelines (v1.0). Produces a structured report of findings organized by severity and category.

**Default target:** `docs/` — pass a specific file or directory to override.

---

## Scope Calibration

The HiveMQ guidelines were written for user-facing product documentation. This project's `docs/` directory contains internal developer documentation (architecture, guides, API patterns). Apply the rules at two tiers:

**Tier 1 — Always apply (flag as issues):**

- Passive voice constructions
- Modal verbs: `should`, `could`, `would`, `may`, `might`
- "will" used for future-tense narration (prefer simple present)
- `-ing` verb forms as main sentence verb in prose
- "please" in instructions
- Gender-specific pronouns (`he/she`, `his/her`, `he `, `she `)
- Ambiguous `this`/`it`/`these` as sentence-starting pronouns with unclear referent
- `e.g.` — use "for example" instead
- `click on` or `click at` — use "click" only
- Optional plural `(s)` forms: `topic(s)`, `adapter(s)`
- Incorrect indefinite article (`a MQTT`, `an HiveMQ`, `a SQL`)
- `noun(s)` style plurals
- Missing serial comma in three-item series
- More than three consecutive nouns (noun clusters)

**Tier 2 — Advisory (flag but do not count as errors):**

- Sentences likely exceeding 25 words (use line length > 180 characters as a proxy)
- Ambiguous `this`/`it` in mid-paragraph references
- Inconsistent terminology for the same concept within a document
- List items without parallel grammatical structure
- Non-descriptive link text ("click here", "this page", "see here")
- `vs.` vs. "versus" — use spelled-out form in prose, abbreviation only in short labels
- Acronyms not defined on first use (check for capitalized abbreviations)

**Out of scope for internal developer docs:**

- Click vs. Select distinction (no UI instructions in architecture docs)
- Localization / translation fitness
- ISO date format enforcement (already used)

---

## Workflow

### Phase 1: Discovery

1. If no argument provided, default target is `docs/`
2. Use Glob to find all `*.md` files in the target path
3. Exclude files that are obviously not prose documents (INDEX.md files are structural — flag less strictly)
4. Report file count before starting

### Phase 2: Per-File Analysis

For each file, run in this order:

#### 2.1 Structural Check (Read the file)

Check for:

- **Frontmatter**: Does the file start with a `---` YAML block? Does it include `title`, `author`, `last_updated`, `purpose`, `audience`, `maintained_at`? (INDEX.md files are exempt — they use `---` as a horizontal rule by design)
- **Section headers**: Do all H2/H3 headings use sentence case, not title case? (exception: proper nouns, acronyms)
- **Code blocks**: Do all fenced code blocks specify a language identifier?
- **Tables**: Do tables have a header row and separator row?

#### 2.2 Pattern Scan (Grep-based, automated)

Run these grep patterns against the file. **Exclude matches inside fenced code blocks** — the patterns below target prose only. Since grep cannot exclude code blocks natively, note matches that appear to be inside code blocks and skip them in the report.

```
# Modal verbs (outside code)
\b(should|could|would|may|might)\b

# Future "will" in prose
\bwill\b

# Passive voice indicators (common constructions)
\b(is|are|was|were|be|been|being)\s+(done|used|managed|handled|created|defined|built|triggered|called|returned|passed|sent|stored|loaded|generated|provided|configured|driven|persisted)\b

# "please" in instructions
\bplease\b

# click on / click at
click (on|at)\b

# e.g.
\be\.g\b|\be\.g\.\b

# Gender pronouns
\b(he|she|his|her|him)\b

# Optional plurals
\w+\(s\)

# Noun clusters (4+ consecutive capitalized/title-case words - heuristic)
([A-Z][a-z]+\s){3,}[A-Z][a-z]+

# Non-descriptive link text
\[click here\]|\[here\]|\[this page\]|\[this\]|\[see here\]

# Incorrect article before MQTT, HTML, SQL, HTTP, API (a MQTT, a HTML, a SQL, a HTTP, a API)
\ba (MQTT|HTML|SQL|HTTP|API)\b

# Incorrect article before HiveMQ (an HiveMQ)
\ban HiveMQ\b
```

#### 2.3 Sentence Length Heuristic

Grep for lines in prose sections (not code blocks, not table rows, not list items, not headings) that exceed 180 characters. These are candidates for sentences exceeding 25 words.

```
^.{181,}$
```

#### 2.4 Terminology Consistency Check (Read-based)

Scan the document for these known HiveMQ Edge terms and check for synonym drift:

| Canonical term       | Watch for non-canonical variants                          |
| -------------------- | --------------------------------------------------------- |
| `protocol adapter`   | "adapter", "protocol plugin", "connector"                 |
| `MQTT broker`        | "broker" (acceptable), "message broker" (avoid)           |
| `combiner`           | "data combiner", "combining node"                         |
| `northbound mapping` | "northbound subscription", "tag mapping"                  |
| `topic filter`       | "topic selector", "topic matcher"                         |
| `behavior policy`    | "behaviour policy" (note British/American split)          |
| `dry-run`            | "dry run", "dryrun"                                       |
| `React Query`        | "react query", "TanStack Query" (use React Query in docs) |
| `Zustand`            | "zustand" (capitalize in prose)                           |

### Phase 3: Generate Report

Output a structured Markdown report. Use this template:

---

```markdown
# Documentation Review Report

**Target:** {path}
**Files reviewed:** {count}
**Date:** {date}
**Guidelines version:** HiveMQ Technical Documentation Writing Guidelines v1.0

---

## Summary

| Category                | Critical | Advisory |
| ----------------------- | -------- | -------- |
| Passive voice           | {n}      | —        |
| Modal verbs             | {n}      | —        |
| Sentence length         | —        | {n}      |
| Terminology             | {n}      | {n}      |
| Structure / frontmatter | {n}      | {n}      |
| Other style             | {n}      | {n}      |
| **Total**               | **{n}**  | **{n}**  |

### Top 3 Issues Across All Files

{List the three most common issues with example count}

---

## File-by-File Findings

### {filename}

**Structural:**

- {finding or "✅ Frontmatter complete"}

**Critical issues:**

- Line {n}: `{offending text}` — {rule violated, suggested fix}

**Advisory notes:**

- Line {n}: {observation}

---
```

---

## Report Behaviour

- **`--summary-only`**: Print only the Summary section, skip per-file details
- If a file has no issues: Print `✅ {filename} — No issues found`
- If a finding is inside a code block (evident from context): skip it, do not report
- Report line numbers where feasible (grep output includes them)
- For passive voice: show the full sentence, not just the match, so the reviewer understands context
- Aim for actionable findings — do not flag things that are genuinely borderline

---

## Calibration Notes for AI Reviewers

**On passive voice:** Developer documentation legitimately uses some passive constructions when the agent is unknown or unimportant ("the node is computed from upstream status"). Flag only cases where active voice would be clearly more readable.

**On modal verbs:** `should` and `could` in technical constraints ("queries should use the provided query key") are functionally advisory-but-required and are worth flagging. `could` in exploratory language is fine. Use judgment.

**On sentence length:** Long sentences in tables or list items are acceptable. Only flag prose paragraphs.

**On frontmatter:** INDEX.md files in each directory are structural navigation — they do not require YAML frontmatter. Do not flag them.

**On terminology consistency:** Flag when a document uses two different terms for the same concept within the same file. Do not flag when different documents use slightly different terms — cross-document consistency is a separate concern.

---

## Reference: Writing Guidelines Summary

Derived from _HiveMQ Technical Documentation Writing Guidelines v1.0_.

| Rule            | Requirement                                                                          |
| --------------- | ------------------------------------------------------------------------------------ |
| Voice           | Active voice. "The broker disconnects the client." not "The client is disconnected." |
| Tense           | Simple present, imperative, infinitive. No future tense narration.                   |
| Modal verbs     | Avoid: should, could, would, may, might, will                                        |
| `-ing` forms    | Avoid as main verb. "To navigate" not "by navigating".                               |
| Sentence length | 25 words max for descriptions. 20 words for procedures.                              |
| Pronouns        | No gender-specific. Minimize "this", "it", "these" as subject.                       |
| Articles        | Based on pronunciation: "an MQTT", "a HiveMQ", "an SQL database".                    |
| Lists           | Parallel construction. Capitalize first word. Period if full sentences.              |
| Acronyms        | Define on first use. MQTT is exempt.                                                 |
| Terminology     | One term per concept. Consistent throughout.                                         |
| Commas          | Serial comma. Comma after introductory phrase.                                       |
| Courtesy        | No "please" in instructions.                                                         |
| Click           | Transitive: "Click Save." Not "Click on Save."                                       |
| Link text       | Descriptive. Not "click here", "this page".                                          |
| Noun clusters   | Maximum 3 consecutive nouns.                                                         |
| Hyphens         | Compound adjectives before noun: "drop-down list", "read-only memory".               |
