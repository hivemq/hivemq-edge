# Claude Code — Project Instructions

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

---

# ⛔ STOP - MANDATORY PREFLIGHT CHECKLIST ⛔

**YOU MUST COMPLETE THIS CHECKLIST AT THE START OF EVERY CONVERSATION. NO EXCEPTIONS.**

## 🚨 BLOCKING REQUIREMENT: Read This Document FIRST

**DO NOT proceed with ANY user request until you have used the Read tool to read the document below.**

### Required Action:

**USE THE READ TOOL** to read `.github/AI_MANDATORY_RULES.md`

- Contains critical rules that prevent wasting 1-2 hours on common mistakes
- Not optional. Not negotiable. READ IT.

### ⚠️ Verification Checklist

Before responding to the user's first request, verify:

- [ ] I used the Read tool on `.github/AI_MANDATORY_RULES.md` (not skimmed — READ)
- [ ] I understand the key rules in that document
- [ ] I will NOT skip this to "appear productive"

### 🔥 Consequences of Skipping This

**If you skip this preflight:**

- You will violate critical rules and break tests
- User will have to repeat instructions 3-4 times
- User will waste their time fixing your avoidable mistakes

## Why This Section Exists

Past AI instances have treated mandatory reading as "optional context" and started work immediately to "appear productive", wasting hours on problems the guidelines would have prevented.

**READ THE DOCUMENT FIRST.**

---

## Build & Development Commands

```bash
# Development
pnpm dev                  # Start dev server (port 3000, proxies /api to :8080)
pnpm build                # TypeScript check + Vite build
pnpm build:tsc            # TypeScript check only

# Linting
pnpm lint:eslint          # ESLint check
pnpm lint:prettier        # Prettier check
pnpm lint:all             # Both ESLint and Prettier

# Testing - Cypress
pnpm cypress:open:component   # Open Cypress component tests (interactive)
pnpm cypress:open:e2e         # Open Cypress E2E tests (interactive)
pnpm cypress:run:component    # Run all component tests headlessly
pnpm cypress:run:e2e          # Run all E2E tests headlessly

# Run specific test file
pnpm cypress:run:component --spec "src/path/to/Component.spec.cy.tsx"
pnpm cypress:run:e2e --spec "cypress/e2e/path/to/test.spec.cy.ts"

# Testing - Vitest (unit tests)
pnpm test                 # Run Vitest in watch mode
pnpm test:coverage        # Run with coverage report

# Code generation
pnpm dev:openAPI          # Generate API client from OpenAPI spec
```

## Architecture Overview

### Directory Structure

```
src/
├── api/                  # API layer
│   ├── __generated__/    # Auto-generated OpenAPI client (HiveMqClient)
│   ├── hooks/            # React Query hooks for API calls
│   └── schemas/          # JSON schemas for forms
├── components/           # Shared UI components
├── extensions/datahub/   # DataHub feature module (self-contained)
├── modules/              # Feature modules
│   ├── App/              # Router and main app shell
│   ├── Workspace/        # React Flow canvas for device topology
│   ├── ProtocolAdapters/ # Protocol adapter configuration
│   ├── Bridges/          # MQTT bridge configuration
│   └── ...               # Other feature modules
├── __test-utils__/       # Test utilities, MSW mocks
├── config/               # App configuration
└── hooks/                # Shared React hooks
```

### Key Technologies

- **React 18** with TypeScript
- **Vite** for bundling
- **Chakra UI v2** for components (use `variant="primary"` not `colorScheme`)
- **React Router v6** for routing
- **TanStack React Query** for server state
- **React Flow (@xyflow/react)** for canvas/node graphs
- **RJSF (@rjsf/chakra-ui)** for JSON Schema forms
- **Zustand** for client state
- **MSW** for API mocking in tests
- **Cypress** for component and E2E tests
- **i18next** for internationalization

### Path Aliases

```typescript
@/        → src/
@datahub/ → src/extensions/datahub/
@cypr/    → cypress/
```

## Documentation and agentic context

### Skills

Workflow skills live in `.claude/skills/`. Invoke with `/skill-name` or Claude invokes automatically based on context:

- `debug-cypress` — Systematic Cypress failure investigation
- `trace-error-messages` — Trace error text through the full stack
- `create-pr` — Write user-centric pull request descriptions
- `create-blogpost` — Write feature announcements and release notes
- `capture-screenshots` — Generate UI screenshots for PRs, docs, and blog posts
- `technical-doc-review` — Review and improve technical documentation

### Agents

Specialized sub-agents live in `.claude/agents/`. Claude delegates to them automatically:

- `agent-ui-engineer` — UI implementation with design system rules baked in
- `agent-e2e-tester` — Cypress component and E2E test writing, debugging, and review

### Task directory structure

Tasks follow the Linear workflow. When a user mentions a task (e.g., "EDG-40"), check `.tasks/{linear-id}-{task-name}/` for context.

**Pattern**: `.tasks/{project-id}-{issue-number}-{short-description}/`

**Examples**:

- Linear issue `EDG-40` → Directory: `.tasks/EDG-40-technical-documentation/`
- Linear issue `EDG-38` → Directory: `.tasks/EDG-38-readonly-schemas/`

**Branch Naming**: Git branches may use slashes (e.g., `feat/EDG-40/technical-documentation`), but task directories use hyphens throughout.

### Linear MCP — state name pitfall

The **Edge** team has two states with `type: backlog`: `Icebox` and `Backlog`. When creating or
updating issues, passing `state: "Backlog"` by name is unreliable — the MCP resolves by type and
may pick `Icebox` first.

**Always use the state ID directly:**

| State   | ID                                       |
| ------- | ---------------------------------------- |
| Backlog | `24b6629e-9193-4e4c-8dd7-0f3f5a720a1e`  |
| Icebox  | `a1273fce-4b0f-45d1-ba11-2ca668859aac`  |

```typescript
// ✅ Reliable
mcp__linear__save_issue({ state: "24b6629e-9193-4e4c-8dd7-0f3f5a720a1e", ... })

// ❌ Ambiguous — may land in Icebox instead
mcp__linear__save_issue({ state: "Backlog", ... })
```
