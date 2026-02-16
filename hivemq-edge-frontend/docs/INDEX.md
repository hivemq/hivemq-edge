# HiveMQ Edge Frontend - Documentation Index

**Last Updated:** 2026-02-13

---

## 📖 About This Documentation

This directory contains permanent project documentation for developers (human and AI agents).

**See:** [README.md](./README.md) for structure and contribution guidelines.

---

## 🚀 Quick Start

**New to the project?**
1. [Technical Stack](./technical/TECHNICAL_STACK.md) - Understand what we use
2. [Onboarding Guide](./guides/ONBOARDING.md) _(TODO)_ - Get set up
3. [Architecture Overview](./architecture/OVERVIEW.md) _(TODO)_ - Understand how it works

**Working on a specific area?**
- **Testing:** [Testing Guide](./guides/TESTING_GUIDE.md) _(TODO)_
- **UI Components:** [Design Guide](./guides/DESIGN_GUIDE.md) _(TODO)_
- **DataHub:** [DataHub Architecture](./architecture/DATAHUB_ARCHITECTURE.md)
- **Workspace:** [Workspace Architecture](./architecture/WORKSPACE_ARCHITECTURE.md)
- **API Integration:** [React Query Patterns](./api/REACT_QUERY_PATTERNS.md) _(TODO)_

---

## 📚 Documentation Sections

### Technical Reference

Complete technical reference for the application stack, dependencies, and infrastructure.

| Document | Description | Status |
|----------|-------------|--------|
| [Technical Stack](./technical/TECHNICAL_STACK.md) | Core toolchain, dependencies, scripts, CI/CD | ✅ Complete |
| [Dependency Management](./technical/DEPENDENCY_MANAGEMENT.md) | Update policy, deprecations, planned upgrades | 📝 TODO |
| [Build & Deployment](./technical/BUILD_AND_DEPLOYMENT.md) | Build process, deployment procedures | 📝 TODO |
| [Configuration](./technical/CONFIGURATION.md) | Environment variables, config files | 📝 TODO |

---

### Architecture

High-level architecture documentation explaining how the application is structured and why.

| Document | Description | Status |
|----------|-------------|--------|
| [Overview](./architecture/OVERVIEW.md) | High-level architecture and design principles | 📝 TODO |
| [Data Flow](./architecture/DATA_FLOW.md) | How data flows through the application | 📝 TODO |
| [State Management](./architecture/STATE_MANAGEMENT.md) | React Query + Zustand patterns | 📝 TODO |
| [DataHub Architecture](./architecture/DATAHUB_ARCHITECTURE.md) | DataHub extension design and implementation | ✅ Complete |
| [Workspace Architecture](./architecture/WORKSPACE_ARCHITECTURE.md) | React Flow canvas and workspace patterns | ✅ Complete |
| [Testing Architecture](./architecture/TESTING_ARCHITECTURE.md) | Testing strategy, pyramid, coverage approach | 📝 TODO |

---

### Guides

Practical how-to guides for common development tasks and patterns.

| Document | Description | Status |
|----------|-------------|--------|
| [Onboarding Guide](./guides/ONBOARDING.md) | Getting started for new developers | 📝 TODO |
| [Testing Guide](./guides/TESTING_GUIDE.md) | Testing patterns, accessibility, Cypress requirements | 📝 TODO |
| [Design Guide](./guides/DESIGN_GUIDE.md) | UI component patterns and button variants | 📝 TODO |
| [Cypress Guide](./guides/CYPRESS_GUIDE.md) | Comprehensive Cypress testing reference | 📝 TODO |
| [RJSF Guide](./guides/RJSF_GUIDE.md) | JSON Schema Form patterns and testing | 📝 TODO |
| [Workspace Testing Guide](./guides/WORKSPACE_TESTING_GUIDE.md) | Testing workspace/React Flow components | 📝 TODO |
| [Internationalization Guide](./guides/I18N_GUIDE.md) | i18n patterns and translation workflow | 📝 TODO |

---

### API Reference

Documentation for API integration, data fetching, and mocking.

| Document | Description | Status |
|----------|-------------|--------|
| [OpenAPI Integration](./api/OPENAPI_INTEGRATION.md) | How OpenAPI client generation works | 📝 TODO |
| [React Query Patterns](./api/REACT_QUERY_PATTERNS.md) | Query and mutation patterns, caching strategy | 📝 TODO |
| [MSW API Mocking](./api/MSW_MOCKING.md) | Mock Service Worker patterns for tests | 📝 TODO |

---

## 🔍 Finding Information

### By Topic

**Build & Development:**
- [Technical Stack](./technical/TECHNICAL_STACK.md) - Core toolchain, dependencies
- [Build & Deployment](./technical/BUILD_AND_DEPLOYMENT.md) _(TODO)_ - CI/CD pipeline

**Testing:**
- [Testing Guide](./guides/TESTING_GUIDE.md) _(TODO)_ - General testing patterns
- [Cypress Guide](./guides/CYPRESS_GUIDE.md) _(TODO)_ - Cypress-specific patterns
- [Testing Architecture](./architecture/TESTING_ARCHITECTURE.md) _(TODO)_ - Strategy overview

**UI Development:**
- [Design Guide](./guides/DESIGN_GUIDE.md) _(TODO)_ - Component patterns, button variants
- [RJSF Guide](./guides/RJSF_GUIDE.md) _(TODO)_ - Dynamic form generation

**State & Data:**
- [State Management](./architecture/STATE_MANAGEMENT.md) _(TODO)_ - Zustand + React Query
- [React Query Patterns](./api/REACT_QUERY_PATTERNS.md) _(TODO)_ - API data fetching
- [Data Flow](./architecture/DATA_FLOW.md) _(TODO)_ - End-to-end data flow

**Specific Features:**
- [DataHub Architecture](./architecture/DATAHUB_ARCHITECTURE.md) _(TODO)_ - DataHub extension
- [Workspace Architecture](./architecture/WORKSPACE_ARCHITECTURE.md) _(TODO)_ - Workspace canvas

---

## 📝 Status Legend

- ✅ **Complete** - Document is complete and up-to-date
- 🔄 **In Progress** - Document is being written
- 📝 **TODO** - Document planned but not started
- 🚧 **Needs Review** - Document exists but needs review/update
- ⚠️ **Outdated** - Document exists but may be outdated

---

## 🤝 Contributing

### Adding New Documentation

1. Determine if it's permanent documentation (`./docs/`) or task-specific (`.tasks/`)
2. Choose appropriate subdirectory: `technical/`, `architecture/`, `guides/`, or `api/`
3. Use UPPERCASE_WITH_UNDERSCORES.md naming convention
4. Add entry to this INDEX.md
5. Follow documentation standards in [README.md](./README.md)

### Updating Existing Documentation

1. Update the document
2. Update "Last Updated" date at top
3. Update status in this INDEX.md if needed
4. Ensure all internal links still work

### Diagrams

**All diagrams MUST use Mermaid with WCAG AA compliant colors.**

See `.github/AI_MANDATORY_RULES.md` RULE 9 for requirements.

---

## 📂 Related Documentation

**Task-Specific Documentation:** `.tasks/`
- Task briefs, implementation plans, migration guides
- Context for active development work

**Mandatory Rules:** `.github/AI_MANDATORY_RULES.md`
- Critical rules for AI agents and developers
- Testing requirements, documentation standards

**Project Root:** `CLAUDE.md`
- Repository guidance for Claude Code
- Links to mandatory reading

---

**Last Review:** 2026-02-13
**Next Review:** 2026-05-13 (Quarterly)
