# Missing Documentation Tracker

**Task:** EDG-40-technical-documentation
**Last Updated:** 2026-02-13

This document tracks all documentation referenced in `docs/` that has not yet been created.

---

## Status Legend

- 🔴 **Critical** - Blocks understanding of key features
- 🟡 **High** - Important for developers working in area
- 🟢 **Medium** - Nice to have, referenced but not blocking
- ⚪ **Low** - Optional, can be deferred

---

## Architecture Documents

| Document                                 | Status | Priority    | Referenced By                     | Notes                            |
| ---------------------------------------- | ------ | ----------- | --------------------------------- | -------------------------------- |
| `architecture/OVERVIEW.md`               | ❌     | 🔴 Critical | INDEX.md, DATAHUB_ARCHITECTURE.md | High-level architecture overview |
| `architecture/STATE_MANAGEMENT.md`       | ❌     | 🟡 High     | DATAHUB_ARCHITECTURE.md           | React Query + Zustand patterns   |
| `architecture/DATA_FLOW.md`              | ❌     | 🟢 Medium   | DATAHUB_ARCHITECTURE.md           | End-to-end data flow diagrams    |
| `architecture/WORKSPACE_ARCHITECTURE.md` | ❌     | 🟡 High     | INDEX.md                          | React Flow workspace patterns    |
| `architecture/TESTING_ARCHITECTURE.md`   | ❌     | 🟡 High     | INDEX.md, DATAHUB_ARCHITECTURE.md | Testing strategy and pyramid     |

---

## Guide Documents

| Document                            | Status | Priority    | Referenced By                     | Notes                                                |
| ----------------------------------- | ------ | ----------- | --------------------------------- | ---------------------------------------------------- |
| `guides/ONBOARDING.md`              | ❌     | 🔴 Critical | INDEX.md                          | New developer getting started                        |
| `guides/TESTING_GUIDE.md`           | ❌     | 🔴 Critical | INDEX.md, DATAHUB_ARCHITECTURE.md | Move from `.tasks/TESTING_GUIDELINES.md`             |
| `guides/DESIGN_GUIDE.md`            | ❌     | 🟡 High     | INDEX.md, DATAHUB_ARCHITECTURE.md | Move from `.tasks/DESIGN_GUIDELINES.md`              |
| `guides/CYPRESS_GUIDE.md`           | ❌     | 🟡 High     | INDEX.md, DATAHUB_ARCHITECTURE.md | Move from `.tasks/CYPRESS_TESTING_GUIDELINES.md`     |
| `guides/RJSF_GUIDE.md`              | ❌     | 🟢 Medium   | INDEX.md                          | Move from `.tasks/RJSF_WIDGET_DESIGN_AND_TESTING.md` |
| `guides/WORKSPACE_TESTING_GUIDE.md` | ❌     | 🟢 Medium   | INDEX.md                          | Move from `.tasks/WORKSPACE_TESTING_GUIDELINES.md`   |
| `guides/I18N_GUIDE.md`              | ❌     | 🟢 Medium   | INDEX.md, DATAHUB_ARCHITECTURE.md | Move from `.tasks/I18N_GUIDELINES.md`                |
| `guides/STATE_MANAGEMENT_GUIDE.md`  | ❌     | 🟡 High     | DATAHUB_ARCHITECTURE.md           | Practical state management patterns                  |

---

## API Documents

| Document                      | Status | Priority  | Referenced By                     | Notes                     |
| ----------------------------- | ------ | --------- | --------------------------------- | ------------------------- |
| `api/OPENAPI_INTEGRATION.md`  | ❌     | 🟡 High   | INDEX.md                          | How code generation works |
| `api/REACT_QUERY_PATTERNS.md` | ❌     | 🟡 High   | INDEX.md, DATAHUB_ARCHITECTURE.md | Query/mutation patterns   |
| `api/MSW_MOCKING.md`          | ❌     | 🟢 Medium | INDEX.md                          | API mocking for tests     |

---

## Technical Documents

| Document                             | Status | Priority  | Referenced By | Notes                            |
| ------------------------------------ | ------ | --------- | ------------- | -------------------------------- |
| `technical/DEPENDENCY_MANAGEMENT.md` | ❌     | 🟢 Medium | INDEX.md      | Update policy, deprecations      |
| `technical/BUILD_AND_DEPLOYMENT.md`  | ❌     | 🟢 Medium | INDEX.md      | Detailed build/deploy procedures |
| `technical/CONFIGURATION.md`         | ❌     | 🟢 Medium | INDEX.md      | Config files, env vars           |

---

## Completed Documents

| Document                               | Status | Completed  | Notes                              |
| -------------------------------------- | ------ | ---------- | ---------------------------------- |
| `INDEX.md`                             | ✅     | 2026-02-13 | Master table of contents           |
| `README.md`                            | ✅     | 2026-02-13 | Documentation overview             |
| `technical/TECHNICAL_STACK.md`         | ✅     | 2026-02-13 | Complete technical stack reference |
| `architecture/DATAHUB_ARCHITECTURE.md` | ✅     | 2026-02-13 | DataHub extension architecture     |

---

## Migration Tasks

### High Priority Moves (from .tasks/ to docs/)

1. **TESTING_GUIDELINES.md** → `docs/guides/TESTING_GUIDE.md`

   - Source: `.tasks/TESTING_GUIDELINES.md`
   - Action: Review, update, relocate
   - Update references in CLAUDE.md

2. **DESIGN_GUIDELINES.md** → `docs/guides/DESIGN_GUIDE.md`

   - Source: `.tasks/DESIGN_GUIDELINES.md`
   - Action: Review, update, relocate
   - Update references in CLAUDE.md

3. **CYPRESS_TESTING_GUIDELINES.md** → `docs/guides/CYPRESS_GUIDE.md`
   - Source: `.tasks/CYPRESS_TESTING_GUIDELINES.md`
   - Action: Review, update, relocate
   - Update references in CLAUDE.md

### Medium Priority Moves

4. **RJSF_WIDGET_DESIGN_AND_TESTING.md** → `docs/guides/RJSF_GUIDE.md`

   - Source: `.tasks/RJSF_WIDGET_DESIGN_AND_TESTING.md`
   - Action: Review, update, relocate

5. **WORKSPACE_TESTING_GUIDELINES.md** → `docs/guides/WORKSPACE_TESTING_GUIDE.md`

   - Source: `.tasks/WORKSPACE_TESTING_GUIDELINES.md`
   - Action: Review, update, relocate

6. **I18N_GUIDELINES.md** → `docs/guides/I18N_GUIDE.md`
   - Source: `.tasks/I18N_GUIDELINES.md`
   - Action: Review, update, relocate

---

## Creation Priority Order

### Phase 1: Critical Foundation (Immediate)

1. **guides/ONBOARDING.md**

   - New developer quick start
   - Environment setup
   - First tasks walkthrough
   - Links to key docs

2. **guides/TESTING_GUIDE.md** (migrate)

   - Core testing patterns
   - Referenced heavily by agents
   - Critical for quality

3. **architecture/OVERVIEW.md**
   - High-level system architecture
   - Component relationships
   - Technology decisions

### Phase 2: High-Value Guides (Next Sprint)

4. **guides/DESIGN_GUIDE.md** (migrate)

   - UI component patterns
   - Button variants
   - Accessibility

5. **guides/CYPRESS_GUIDE.md** (migrate)

   - Testing patterns
   - Page objects
   - Common pitfalls

6. **api/REACT_QUERY_PATTERNS.md**

   - Query/mutation patterns
   - Caching strategy
   - Error handling

7. **architecture/STATE_MANAGEMENT.md**
   - Zustand + React Query
   - When to use each
   - Common patterns

### Phase 3: Feature-Specific (As Needed)

8. **architecture/WORKSPACE_ARCHITECTURE.md**

   - React Flow integration
   - Layout algorithms
   - Testing patterns

9. **api/OPENAPI_INTEGRATION.md**

   - Code generation process
   - Customization
   - Migration guide

10. **architecture/TESTING_ARCHITECTURE.md**
    - Testing pyramid
    - Coverage strategy
    - Tooling overview

### Phase 4: Supporting Documentation (Lower Priority)

11. **guides/STATE_MANAGEMENT_GUIDE.md**

    - Practical patterns
    - Common use cases
    - Debugging

12. **guides/RJSF_GUIDE.md** (migrate)

    - Form patterns
    - Custom widgets
    - Testing

13. **technical/DEPENDENCY_MANAGEMENT.md**

    - Update policy
    - Security patches
    - Deprecations

14. **remaining guides and technical docs**

---

## Document Templates

When creating new documents, use these templates:

### Architecture Document Template

```markdown
# [Feature] Architecture

**Last Updated:** YYYY-MM-DD
**Purpose:** [Brief description]
**Audience:** [Developers/AI agents/Both]

---

## Overview

[High-level description with Mermaid diagram]

## Key Components

[Component descriptions]

## Data Flow

[Mermaid sequence diagrams]

## Implementation Details

[Code patterns, gotchas]

## Testing

[Testing approach]

## Related Documentation

[Links to docs/ only]

---

**Document Maintained By:** Development Team
**Last Review:** YYYY-MM-DD
**Next Review:** [Quarterly date]
```

### Guide Document Template

```markdown
# [Topic] Guide

**Last Updated:** YYYY-MM-DD
**Purpose:** Practical guide for [task]
**Audience:** [Developers/AI agents/Both]

---

## Quick Start

[Immediate actionable steps]

## Core Concepts

[Essential understanding]

## Common Patterns

[Copy-paste examples]

## Troubleshooting

[Common issues and solutions]

## Best Practices

[Do's and don'ts]

## Related Documentation

[Links to docs/ only]

---

**Last Review:** YYYY-MM-DD
```

---

## Tracking Metrics

**Total Documents Needed:** 18
**Completed:** 4 (22%)
**In Progress:** 0
**Not Started:** 14 (78%)

**By Priority:**

- 🔴 Critical: 3
- 🟡 High: 10
- 🟢 Medium: 5
- ⚪ Low: 0

---

## Next Actions

1. **Update INDEX.md** - Mark DATAHUB_ARCHITECTURE as complete ✅
2. **Create guides/ONBOARDING.md** - Critical for new developers
3. **Migrate TESTING_GUIDELINES.md** - Heavily referenced
4. **Create architecture/OVERVIEW.md** - Foundation document

---

**Last Updated:** 2026-02-13
**Review Frequency:** Weekly during active documentation phase
