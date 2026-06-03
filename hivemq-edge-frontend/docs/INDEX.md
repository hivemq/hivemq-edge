# HiveMQ Edge Frontend - Documentation Index

**Last Updated:** 2026-02-17

---

## 📖 About This Documentation

This directory contains permanent project documentation for developers (human and AI agents).

**See:** [README.md](./README.md) for structure and contribution guidelines.

---

## 🚀 Quick Start

**New to the project?**
1. [Technical Stack](./technical/TECHNICAL_STACK.md) - Understand what we use
2. [Onboarding Guide](./guides/ONBOARDING.md) - Get set up
3. [Architecture Overview](./architecture/OVERVIEW.md) - Understand how it works

**Working on a specific area?**
- **Testing:** [Testing Guide](./guides/TESTING_GUIDE.md)
- **UI Components:** [Design Guide](./guides/DESIGN_GUIDE.md)
- **DataHub:** [DataHub Architecture](./architecture/DATAHUB_ARCHITECTURE.md)
- **Workspace:** [Workspace Architecture](./architecture/WORKSPACE_ARCHITECTURE.md)
- **API Integration:** [React Query Patterns](./api/REACT_QUERY_PATTERNS.md)

---

## 📚 Documentation Sections

### Technical Reference

Complete technical reference for the application stack, dependencies, and infrastructure.

| Document | Description | Status |
|----------|-------------|--------|
| [Technical Stack](./technical/TECHNICAL_STACK.md) | Core toolchain, dependencies, scripts, CI/CD | ✅ Complete |
| [Dependency Management](./technical/DEPENDENCY_MANAGEMENT.md) | Laissez-faire update policy, Renovate, Snyk workflows, version pinning, planned migrations | ✅ Complete |
| [Build & Deployment](./technical/BUILD_AND_DEPLOYMENT.md) | Local dev/build, Gradle production packaging, Jenkins, staging (ephemeral environments) | ✅ Complete |
| [Configuration](./technical/CONFIGURATION.md) | Env files, feature flags, third-party keys (Heap, Sentry, Percy, SonarCloud, Snyk) | ✅ Complete |
| [External Services](./technical/EXTERNAL_SERVICES.md) | Master reference for all external dashboards — URLs, login, ownership status | ✅ Complete |
| [Reference Materials](./technical/REFERENCE_MATERIALS.md) | Catalogue of external design artefacts — 14 Miro boards across workspace, DataHub, Pulse, domain modelling | ✅ Complete |

---

### Architecture

High-level architecture documentation explaining how the application is structured and why.

| Document | Description | Status |
|----------|-------------|--------|
| [Overview](./architecture/OVERVIEW.md) | High-level architecture, design principles, technology rationale, known gaps | ✅ Complete |
| [Data Flow](./architecture/DATA_FLOW.md) | REST API client stack, React Query cache, polling, mutations, invalidation pattern, client state hierarchy | ✅ Complete |
| [State Management](./architecture/STATE_MANAGEMENT.md) | State layer hierarchy, six Zustand stores, localStorage gaps, workspace technical debt | ✅ Complete |
| [DataHub Architecture](./architecture/DATAHUB_ARCHITECTURE.md) | DataHub extension design and implementation | ✅ Complete |
| [Workspace Architecture](./architecture/WORKSPACE_ARCHITECTURE.md) | React Flow canvas and workspace patterns | ✅ Complete |
| [Protocol Adapter Architecture](./architecture/PROTOCOL_ADAPTER_ARCHITECTURE.md) | Backend-driven adapter configuration with RJSF | ✅ Complete |
| [Testing Architecture](./architecture/TESTING_ARCHITECTURE.md) | 7-layer testing pyramid, tools, CI/CD, accessibility | ✅ Complete |
| [Domain Model](./architecture/DOMAIN_MODEL.md) | Entity model (TAG, TOPIC, TOPIC FILTER, COMBINER, BRIDGE, ASSET MAPPER), transformation flows, topic filter as MQTT pattern matcher | ✅ Complete |

---

### Guides

Practical how-to guides for common development tasks and patterns.

| Document | Description | Status |
|----------|-------------|--------|
| [Onboarding Guide](./guides/ONBOARDING.md) | Prerequisites (Node 24, pnpm 11), repository setup, `.env.local` configuration, first run, access | ✅ Complete |
| [Testing Guide](./guides/TESTING_GUIDE.md) | Testing patterns, E2E structure, Page Object pattern, `cy_interceptCoreE2E`, accessibility | ✅ Complete |
| [Design Guide](./guides/DESIGN_GUIDE.md) | UI component patterns and button variants | ✅ Complete |
| [Cypress Guide](./guides/CYPRESS_GUIDE.md) | Cypress rules, selectors, custom commands, Page Objects reference, debugging | ✅ Complete |
| [RJSF Guide](./guides/RJSF_GUIDE.md) | Complete RJSF integration guide with all forms | ✅ Complete |
| [Workspace Testing Guide](./guides/WORKSPACE_TESTING_GUIDE.md) | React Flow testing, WorkspacePage and WizardPage selector reference | ✅ Complete |
| [Internationalization Guide](./guides/I18N_GUIDE.md) | i18n patterns and translation workflow | ✅ Complete |
| [User-Facing Documentation](./guides/USER_FACING_DOCUMENTATION.md) | External docs, PR descriptions, feature announcements, screenshot generation | ✅ Complete |

---

### API Reference

Documentation for API integration, data fetching, and mocking.

| Document | Description | Status |
|----------|-------------|--------|
| [OpenAPI Integration](./api/OPENAPI_INTEGRATION.md) | openapi-typescript-codegen, HiveMqClient structure, regeneration workflow | ✅ Complete |
| [React Query Patterns](./api/REACT_QUERY_PATTERNS.md) | Query/mutation patterns, caching strategy, optimistic updates, infinite queries | ✅ Complete |
| [MSW API Mocking](./api/MSW_MOCKING.md) | Handler organization, testing patterns, error simulation, Cypress integration | ✅ Complete |

---

### Analysis

Technical analysis documentation for quality assurance, technical debt, and migration decisions.

| Document | Description | Status |
|----------|-------------|--------|
| [Adapter Schema Analysis 2025](./analysis/ADAPTER_SCHEMA_ANALYSIS_2025.md) | Comprehensive adapter schema review — 28 issues (task 38658) | ✅ Complete |
| [OpenAPI Quality Review](./analysis/OPENAPI_QUALITY_REVIEW.md) | OpenAPI spec audit — 29 issues, structural defects, agentic readiness, Data Hub spec vs. reality | ✅ Complete |
| [Chakra UI v3 Migration Analysis](./analysis/CHAKRA_V3_MIGRATION.md) | Cost-benefit analysis, 10-14 week phased plan, RJSF dependency risk assessment | ✅ Complete |
| [Problem Detail Analysis](./analysis/PROBLEM_DETAIL_ANALYSIS.md) | RFC 9457 gap analysis, error handling inventory, 4-phase migration plan for typed error UX | ✅ Complete |

---

### Walkthroughs

In-depth explanations of complex features from both interaction design and technical perspectives.

| Document | Description | Status |
|----------|-------------|--------|
| [RJSF Combiner Walkthrough](./walkthroughs/RJSF_COMBINER.md) | How we transformed flat forms into interactive UX | ✅ Complete |
| [Domain Ontology Visualization](./walkthroughs/DOMAIN_ONTOLOGY.md) | Five existing visualization approaches, two Phase 1 implementations, deferred work | ✅ Complete |

---

## 🔍 Finding Information

### By Topic

**Build & Development:**
- [Technical Stack](./technical/TECHNICAL_STACK.md) - Core toolchain, dependencies
- [Build & Deployment](./technical/BUILD_AND_DEPLOYMENT.md) - CI/CD pipeline

**Testing:**
- [Testing Guide](./guides/TESTING_GUIDE.md) - General testing patterns
- [Cypress Guide](./guides/CYPRESS_GUIDE.md) - Cypress-specific patterns
- [Workspace Testing Guide](./guides/WORKSPACE_TESTING_GUIDE.md) - React Flow testing
- [Testing Architecture](./architecture/TESTING_ARCHITECTURE.md) - Strategy overview

**UI Development:**
- [Design Guide](./guides/DESIGN_GUIDE.md) - Component patterns, button variants
- [RJSF Guide](./guides/RJSF_GUIDE.md) - Dynamic form generation
- [RJSF Combiner Walkthrough](./walkthroughs/RJSF_COMBINER.md) - Complex form UX implementation

**State & Data:**
- [State Management](./architecture/STATE_MANAGEMENT.md) - Zustand + React Query
- [React Query Patterns](./api/REACT_QUERY_PATTERNS.md) - API data fetching
- [Data Flow](./architecture/DATA_FLOW.md) - End-to-end data flow

**Specific Features:**
- [DataHub Architecture](./architecture/DATAHUB_ARCHITECTURE.md) - DataHub extension
- [Workspace Architecture](./architecture/WORKSPACE_ARCHITECTURE.md) - Workspace canvas

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

**Last Review:** 2026-02-17
**Next Review:** 2026-05-17 (Quarterly)
