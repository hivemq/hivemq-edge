# HiveMQ Edge Frontend Documentation

This directory contains the permanent project documentation for the HiveMQ Edge Frontend.

## Purpose

**`./docs/` - Permanent Project Documentation**
- Architecture, technical reference, onboarding guides
- Audience: Developers (human and AI agents)
- Version-controlled, curated, maintained
- Stable reference material

**Distinction from `.tasks/`:**
- `.tasks/` contains task-specific work (briefs, plans, analysis)
- `./docs/` contains permanent project knowledge

## Navigation

Start with [INDEX.md](./INDEX.md) for the complete table of contents.

## Structure

```
docs/
├── INDEX.md                    # Master table of contents
├── README.md                   # This file
│
├── technical/                  # Technical reference
│   ├── TECHNICAL_STACK.md     # Dependencies, tooling, scripts
│   ├── DEPENDENCY_MANAGEMENT.md
│   ├── BUILD_AND_DEPLOYMENT.md
│   └── CONFIGURATION.md
│
├── architecture/               # Architecture documentation
│   ├── OVERVIEW.md
│   ├── DATA_FLOW.md
│   ├── STATE_MANAGEMENT.md
│   ├── DATAHUB_ARCHITECTURE.md
│   ├── WORKSPACE_ARCHITECTURE.md
│   └── TESTING_ARCHITECTURE.md
│
├── guides/                     # How-to guides
│   ├── ONBOARDING.md
│   ├── TESTING_GUIDE.md
│   ├── DESIGN_GUIDE.md
│   ├── CYPRESS_GUIDE.md
│   ├── RJSF_GUIDE.md
│   ├── WORKSPACE_TESTING_GUIDE.md
│   └── I18N_GUIDE.md
│
└── api/                        # API documentation
    ├── OPENAPI_INTEGRATION.md
    ├── REACT_QUERY_PATTERNS.md
    └── MSW_MOCKING.md
```

## Contributing to Documentation

### When to Add/Update Documentation

**Add to `./docs/` when:**
- Documenting architecture decisions
- Creating permanent reference material
- Writing onboarding/guide content
- Explaining core patterns and practices

**Add to `.tasks/` when:**
- Working on a specific task/feature
- Creating implementation plans
- Documenting task-specific analysis
- Writing migration guides for active work

### Documentation Standards

**All documentation must follow these rules:**
- Use UPPERCASE_WITH_UNDERSCORES.md naming
- All diagrams must use Mermaid (not ASCII art)
- Mermaid diagrams must use WCAG AA compliant colors (4.5:1 contrast minimum)
- Include "Last Updated" date at top of document
- Link to related documents

**See:** `.github/AI_MANDATORY_RULES.md` RULE 9 for diagram requirements

## Maintenance

**Review Frequency:** Quarterly or when major changes occur

**Document Owners:**
- Technical reference: Development team
- Architecture: Tech leads
- Guides: Development team with review from leads

---

**Last Updated:** 2026-02-16
