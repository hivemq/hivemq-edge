---
title: "Documentation Quality Review — Run 1"
date: "2026-02-17"
skill: "technical-doc-writer v1.0"
guidelines: "HiveMQ Technical Documentation Writing Guidelines v1.0"
scope: "docs/ (39 files)"
status: "Fixes applied"
---

# Documentation Quality Review — Run 1

**Date:** 2026-02-17
**Skill:** `technical-doc-writer` v1.0
**Guidelines:** HiveMQ Technical Documentation Writing Guidelines v1.0
**Scope:** `docs/` — 39 files reviewed

---

## Summary

| Category | Critical | Advisory |
|----------|----------|----------|
| Future "will" in prose | 8 | — |
| `e.g.` (use "for example") | 12 | 6 |
| Modal verbs in prose | 4 | — |
| Passive voice (prose) | 4 | 6 |
| Optional plural `(s)` | 1 | — |
| Sentence length (>25 words) | — | ~20 |
| Frontmatter gaps | 0 | 1 |
| **Total** | **29** | **~33** |

### Fixes applied in this run

| # | File | Fix |
|---|------|-----|
| 1 | `ONBOARDING.md` | 4 × "will" → simple present |
| 2 | `USER_FACING_DOCUMENTATION.md` | 2 × "will" → simple present |
| 3 | `REFERENCE_MATERIALS.md` | "Board(s)" → "Boards" |
| 4 | `STATE_MANAGEMENT.md` | `e.g.` → "for example" |
| 5 | `TESTING_ARCHITECTURE.md` | `e.g.` → "for example"; "should also use" → "must also use" |
| 6 | `PROTOCOL_ADAPTER_ARCHITECTURE.md` | 2 × `e.g.` → "for example" |
| 7 | `DATAHUB_ARCHITECTURE.md` | `e.g.` → "for example" |
| 8 | `DESIGN_GUIDE.md` | `e.g.` → "for example" |
| 9 | `USER_FACING_DOCUMENTATION.md` | `e.g.` → "for example" |
| 10 | `RJSF_GUIDE.md` | `e.g.` → "for example" |
| 11 | `RJSF_COMBINER.md` | `e.g.` → "for example" |
| 12 | `I18N_GUIDE.md` | `e.g.` → "for example" |

### Deferred (advisory, not fixed in this run)

- Long "Why:" sentences in `OVERVIEW.md` — defensible length for explanation blocks
- Passive voice in `DATA_FLOW.md` — technical passive, not egregious
- `e.g.` in analysis docs (`OPENAPI_QUALITY_REVIEW`, `CHAKRA_V3_MIGRATION`, `PROBLEM_DETAIL_ANALYSIS`) — lower priority
- `e.g.` in table cells in `REFERENCE_MATERIALS.md` and `I18N_GUIDE.md` — advisory

---

## Key Finding: Skill Signal-to-Noise Problem

The grep-based pattern scan for modal verbs (`should`, `could`, `would`, `may`, `might`) returned **259 matches across 29 files**. After filtering, only ~4 were genuine prose violations.

**Root cause:** Cypress `.should('be.visible')` assertions inside fenced code blocks account for ~95% of matches. The skill cannot distinguish code from prose using simple grep.

### Impact on skill accuracy

| Pattern | Raw hits | False positives | Real issues |
|---------|----------|-----------------|-------------|
| Modal verbs | 259 | ~250 (Cypress `.should()`) | ~9 |
| Passive voice | 43 | ~25 (code/tables) | ~18 |
| `e.g.` | 31 | ~9 (in code blocks) | ~22 |
| "will" in prose | 21 | ~13 (pipeline descriptions, code) | ~8 |

---

## Skill Calibration: Required Improvements

Two changes needed before the next run produces reliable results.

### Problem 1: Code-block exclusion

The skill currently runs grep against the full file, including fenced code blocks. This makes the modal verb pattern nearly useless for this codebase.

**Recommended fix:** Add a pre-processing step that strips fenced code blocks before running pattern scans. One approach:

```bash
# Strip fenced code blocks from file, output prose-only content
awk '/^```/{f=!f; next} !f' "$file" | grep -n "\b(should|could)\b"
```

This is a bash pre-processing step — the skill currently only has `Glob, Grep, Read` in `allowed-tools`. To add this capability, the skill needs `Bash` added to `allowed-tools`, or the stripping logic needs to happen inside the AI's read-then-analyze pass.

**Alternative:** During the Read phase, the AI reviewer should mentally skip any content between fenced code fences. Document this explicitly in the skill as a mandatory calibration step.

### Problem 2: `e.g.` inside inline code

Patterns like `` (e.g., `my-opcua-adapter`) `` should flag the outer `e.g.`. But `` `// e.g. see config` `` inside a backtick span should not. Current grep cannot distinguish these without context.

**Recommended fix:** Accept that the Read-based review will catch these correctly — the AI reads with full context. Reserve grep only for high-confidence zero-false-positive patterns (`please`, `click on`, `Board(s)`, `an HiveMQ`, `a MQTT`).

---

## Recommended Skill Architecture Change

Based on this run, the most effective pattern for `technical-doc-writer` is a **hybrid approach**:

1. **Grep phase** — run only the patterns with zero/near-zero false positives in this codebase:
   - `\bplease\b`
   - `click (on|at)\b`
   - `\ban HiveMQ\b`
   - `\ba (MQTT|HTML|SQL)\b`
   - `\w+\(s\)` optional plurals
   - Non-descriptive links (`[click here]`, `[here]`)

2. **Read phase** — for each substantive prose file, the AI reads the full document and applies all Tier 1/Tier 2 rules with full context, including:
   - Modal verbs (with code-block awareness)
   - Future "will"
   - `e.g.`
   - Passive voice
   - Sentence length

3. **Skip code-heavy files from deep read** — files where >60% of content is code blocks (CYPRESS_GUIDE.md, TESTING_GUIDE.md, WORKSPACE_TESTING_GUIDE.md) — grep for structural issues only.

---

## Next Review

Suggested cadence: run before each significant PR that touches `docs/`.

Files most likely to accumulate issues:
- New architecture documents (passive voice, `e.g.` drift)
- Guides with code examples (false positive noise — use grep-skip mode)
- Any document with step-by-step instructions (watch for "will" and "please")
