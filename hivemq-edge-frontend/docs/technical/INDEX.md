# Technical Reference

This directory contains technical reference documentation about the stack, dependencies, build processes, and infrastructure.

## Documents

- **[TECHNICAL_STACK.md](./TECHNICAL_STACK.md)** - Complete technical stack reference
  - Core toolchain (Vite, TypeScript, pnpm, Node.js)
  - Dependencies organized by category
  - CI/CD pipeline documentation
  - Scripts reference
  - Version information and dependency management

- **[DEPENDENCY_MANAGEMENT.md](./DEPENDENCY_MANAGEMENT.md)** - Dependency update policy
  - Update schedule and procedures
  - Security patch policy
  - Major version upgrade approval process
  - Testing requirements before updates

- **[BUILD_AND_DEPLOYMENT.md](./BUILD_AND_DEPLOYMENT.md)** - Build and deployment procedures
  - Local build process
  - CI/CD pipeline details
  - Deployment workflows
  - Environment-specific builds

- **[CONFIGURATION.md](./CONFIGURATION.md)** - Configuration reference
  - Environment variables
  - Config file documentation
  - Feature flags
  - Runtime configuration

- **[EXTERNAL_SERVICES.md](./EXTERNAL_SERVICES.md)** - External services master reference
  - All external dashboards with URLs and login methods
  - Ownership and access status tracking (bus-factor risk)
  - Monitoring (Heap, Sentry, Percy), code quality (SonarCloud, Snyk), CI/CD (Jenkins)
  - Issue tracking (Linear, Kanbanize), design tools (Figma, Miro)

- **[REFERENCE_MATERIALS.md](./REFERENCE_MATERIALS.md)** - External design and planning artefacts
  - 14 Miro boards catalogued across 4 thematic categories
  - Workspace/topology design, Pulse/asset mapping, DataHub designer, domain modelling
  - Board-to-documentation map for quick cross-referencing

## Purpose

These documents provide definitive reference material for:
- Understanding what technologies we use and why
- Setting up development environment
- Understanding build and deployment processes
- Managing dependencies and versions

## Audience

- New developers onboarding to the project
- Existing developers needing reference material
- AI agents working on the codebase
- DevOps configuring CI/CD

---

**See:** [Documentation Index](../INDEX.md) for complete table of contents
