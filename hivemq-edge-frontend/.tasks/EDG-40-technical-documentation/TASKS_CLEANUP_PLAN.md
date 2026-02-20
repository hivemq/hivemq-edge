# .tasks Root Cleanup Plan

**Date:** 2026-02-20
**Context:** Post EDG-40 — docs/ is now complete (30 docs, all ✅). Review and clean up bloated .tasks root.

---

## Disposition Summary

| Action | Count | Files |
|--------|-------|-------|
| Delete (safe) | 6 | See below |
| Delete (spot-check, docs assumed complete) | 5 | See below |
| Convert to skills | 3 | `debug-cypress`, `trace-error-messages`, `create-pr` |
| Migrate content to docs/ then delete | 3 | `WORKSPACE_TOPOLOGY`, `REACT_FLOW_BEST_PRACTICES`, `CODE_COMMENTS_GUIDELINES` |
| Keep | 14 | Operational context, templates, no docs/ equivalent |

---

## Delete — safe (no verification needed)

| File | Reason |
|------|--------|
| `CYPRESS_LOGGING_INDEX.md` | Self-declared deprecated — explicitly redirects to `CYPRESS_TESTING_GUIDELINES.md` |
| `LINEAR_MIGRATION_GUIDE.md` | Migration to Linear complete (Feb 2026); `DEFAULT_BOARD.md` covers what remains |
| `RESOURCE_USAGE_TEMPLATE.md` | Thin (~170 lines) — duplicates `AI_OPTIMIZATION_GUIDE.md` |
| `TESTING_GUIDELINES.md` | Hub doc that re-lists guides superseded by `docs/guides/TESTING_GUIDE.md` |
| `RJSF_DOCUMENTATION_PLAN.md` | EDG-40 complete — the plan was fulfilled, no further operational use |
| `WEBSTORM_CONFIG.md` | 60-line IDE note; useful info belongs in CLAUDE.md or onboarding, not a standalone file |

---

## Delete — after spot-check (docs assumed complete)

Working assumption: docs/ coverage is sufficient; new additions require a genuine gap.

| File | Superseded by |
|------|--------------|
| `RJSF_GUIDELINES.md` | `docs/guides/RJSF_GUIDE.md` |
| `RJSF_WIDGET_DESIGN_AND_TESTING.md` | `docs/guides/RJSF_GUIDE.md` |
| `I18N_GUIDELINES.md` | `docs/guides/I18N_GUIDE.md` |
| `CYPRESS_TESTING_GUIDELINES.md` | `docs/guides/CYPRESS_GUIDE.md` |
| `WORKSPACE_TESTING_GUIDELINES.md` | `docs/guides/WORKSPACE_TESTING_GUIDE.md` |

---

## Convert to skills

| File | Skill name | Description |
|------|-----------|-------------|
| `CYPRESS_STOP_AND_INVESTIGATE.md` | `debug-cypress` | Systematic checklist for debugging Cypress test failures |
| `ERROR_MESSAGE_TRACING_PATTERN.md` | `trace-error-messages` | Full-stack error tracing: OpenAPI → React Query → MSW → Component → Test |
| `PULL_REQUEST_TEMPLATE.md` | `create-pr` | PR creation with user-centric framing, screenshot requirements, structured description |

---

## Migrate content to docs/ then delete

| File | Destination |
|------|------------|
| `WORKSPACE_TOPOLOGY.md` | `docs/architecture/WORKSPACE_ARCHITECTURE.md` or `DOMAIN_MODEL.md` |
| `REACT_FLOW_BEST_PRACTICES.md` | `docs/architecture/WORKSPACE_ARCHITECTURE.md` |
| `CODE_COMMENTS_GUIDELINES.md` | `docs/guides/ONBOARDING.md` or new `docs/guides/CODE_STYLE.md` |

---

## Keep as-is (valid operational .tasks context)

These serve a purpose that docs/ does not cover — AI operational context, process frameworks, or unique reference with no docs/ equivalent.

- `README.md` — directory index
- `DEFAULT_BOARD.md` — Linear team config
- `AUTONOMY_TEMPLATE.md` — core agent behavior framework
- `AI_AGENT_CYPRESS_COMPLETE_GUIDE.md` — AI-specific Cypress file access (not in docs/)
- `AI_OPTIMIZATION_GUIDE.md` — user token optimization
- `HOW_TO_MAKE_AI_FOLLOW_GUIDELINES.md` — meta-guide on rule enforcement
- `DESIGN_GUIDELINES.md` — **mandatory preflight in CLAUDE.md — do not move**
- `REPORTING_STRATEGY.md` — documentation tier strategy (.tasks vs .tasks-log)
- `QUICK_START.md` — task resumption guide
- `USER_DOCUMENTATION_GUIDELINE.md` — unique user-facing doc template
- `PULL_REQUEST_SCREENSHOTS_GUIDE.md` — operational reference for PR screenshots
- `REACT_SELECT_TESTING_PATTERNS.md` — no docs/ equivalent, specialized
- `MONACO_TESTING_GUIDE.md` — no docs/ equivalent, specialized
- `PARALLEL_EXECUTION_TEMPLATE.md` — multi-agent orchestration framework
