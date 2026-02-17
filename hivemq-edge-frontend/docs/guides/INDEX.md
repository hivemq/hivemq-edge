# Development Guides

This directory contains practical how-to guides for common development tasks and patterns.

## Documents

- **[ONBOARDING.md](./ONBOARDING.md)** - New developer onboarding
  - Prerequisites: Node.js 22, pnpm 10, NVM
  - Repository structure (monorepo context)
  - `.env.local` minimum configuration (required before first run)
  - Mock mode (running without the Java backend)
  - Running tests (Cypress + Vitest)
  - Getting access to external services
  - Recommended reading order
  - Day one checklist and troubleshooting

- **[TESTING_GUIDE.md](./TESTING_GUIDE.md)** - Testing patterns and requirements
  - Testing philosophy
  - Component test patterns
  - E2E test patterns
  - Accessibility testing (mandatory)
  - Coverage requirements

- **[DESIGN_GUIDE.md](./DESIGN_GUIDE.md)** - UI component patterns
  - Button variants (primary, outline, ghost, danger)
  - Chakra UI conventions
  - Color usage and theming
  - Responsive design patterns

- **[CYPRESS_GUIDE.md](./CYPRESS_GUIDE.md)** - Cypress testing reference
  - Critical rules
  - Selector strategy
  - Custom commands
  - Debugging techniques
  - Common patterns

- **[RJSF_GUIDE.md](./RJSF_GUIDE.md)** - Complete RJSF integration guide
  - All 12+ RJSF forms in the application
  - JSON Schema and UI Schema patterns
  - 18+ custom widgets inventory
  - Custom fields and templates
  - Validation and testing patterns
  - Common issues and solutions

- **[WORKSPACE_TESTING_GUIDE.md](./WORKSPACE_TESTING_GUIDE.md)** - Workspace-specific testing
  - React Flow component testing
  - Mock data for workspace tests
  - Node and edge testing
  - Canvas interaction testing

- **[I18N_GUIDE.md](./I18N_GUIDE.md)** - Internationalization patterns
  - i18next and react-i18next setup
  - Translation key naming conventions
  - Namespace structure and usage
  - Interpolation, context, and pluralization
  - Accessibility with i18n
  - Adding new translations
  - Testing i18n keys

- **[USER_FACING_DOCUMENTATION.md](./USER_FACING_DOCUMENTATION.md)** - User-facing documentation guide
  - Documentation layers (external docs, PR descriptions, feature announcements)
  - External HiveMQ docs URLs and update responsibilities
  - PR description template and principles
  - Feature announcement structure (What / How / Why / Looking Ahead)
  - Screenshot generation philosophy and rules
  - Two types of screenshot specs (permanent documentation vs. per-PR)
  - Step-by-step screenshot workflow with code templates
  - Common failures and fixes

## Purpose

Guides provide:
- **Practical examples** of how to accomplish specific tasks
- **Step-by-step instructions** for common workflows
- **Best practices** and patterns to follow
- **Troubleshooting** for common issues

## Audience

- Developers actively working on the codebase
- AI agents implementing features
- New developers learning the patterns
- Anyone needing quick reference for "how to..."

## Relationship to Architecture

**Architecture docs** explain the big picture and why decisions were made.

**Guides** explain how to work within that architecture practically.

Example:
- Architecture: "We use Cypress for testing with accessibility checks"
- Guide: "How to write a Cypress component test with accessibility"

---

**See:** [Documentation Index](../INDEX.md) for complete table of contents
