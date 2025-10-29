# Active Tasks Index

**📍 LOCATION: All task files are in the `.tasks/` directory**

This file helps identify and resume work on tasks across different conversation threads.

## How to Use This File

**To resume work on a task in a new conversation:**

Simply tell the AI agent:

> "We're working on task **{task-id}**" or "Continue work on **{task-id}**"

The agent will **automatically look in the `.tasks/` directory** and:

1. Read this index file (`.tasks/ACTIVE_TASKS.md`)
2. Find the task directory under `.tasks/{task-id}-{task-name}/`
3. Read the TASK_BRIEF.md for context
4. Review TASK_SUMMARY.md for completed subtasks
5. Check the latest CONVERSATION_SUBTASK_N.md files for recent work
6. Be ready to continue where you left off

**Remember:** All task documentation lives in `.tasks/` - never at the project root.

---

## Active Tasks

### 33168-duplicate-combiner

**Status:** Completed ✅  
**Directory:** `.tasks/33168-duplicate-combiner/`  
**Objective:** Improve duplicate combiner detection UX and refactor ContextualToolbar for better testability  
**Completed Subtasks:** 3 (All Phases Complete)  
**Last Updated:** October 24, 2025

**Summary:**

- ✅ Phase 1: Extracted 5 utility functions with 29 unit tests
- ✅ Phase 2: Created modal dialog UI with 15 component tests
- ✅ Phase 3: Added 12 E2E tests + 2 Percy snapshots
- **Total: 56 tests, all passing**

**Quick Start:**

- Read: [TASK_BRIEF.md](.tasks/33168-duplicate-combiner/TASK_BRIEF.md)
- Review: [TASK_SUMMARY.md](.tasks/33168-duplicate-combiner/TASK_SUMMARY.md)
- Recent work: [CONVERSATION_SUBTASK_3.md](.tasks/33168-duplicate-combiner/CONVERSATION_SUBTASK_3.md)

---

### 37542-code-coverage

**Status:** Active  
**Directory:** `.tasks/37542-code-coverage/`  
**Objective:** Improve code coverage for the HiveMQ Edge Frontend codebase  
**Completed Subtasks:** 3  
**Last Updated:** October 17, 2025

**Quick Start:**

- Read: [TASK_BRIEF.md](.tasks/37542-code-coverage/TASK_BRIEF.md)
- Review: [TASK_SUMMARY.md](.tasks/37542-code-coverage/TASK_SUMMARY.md)
- Recent work: [CONVERSATION_SUBTASK_3.md](.tasks/37542-code-coverage/CONVERSATION_SUBTASK_3.md)

---

### 37074-percy-optimisation

**Status:** Active  
**Directory:** `.tasks/37074-percy-optimisation/`  
**Objective:** Optimize Percy visual regression testing strategy to maximize UI coverage while minimizing token usage  
**Completed Subtasks:** 2  
**Last Updated:** October 22, 2025

**Quick Start:**

- Read: [TASK_BRIEF.md](.tasks/37074-percy-optimisation/TASK_BRIEF.md)
- Review: [TASK_SUMMARY.md](.tasks/37074-percy-optimisation/TASK_SUMMARY.md)
- Recent work: [CONVERSATION_SUBTASK_2.md](.tasks/37074-percy-optimisation/CONVERSATION_SUBTASK_2.md)
- Coverage matrix: [COVERAGE_MATRIX.md](.tasks/37074-percy-optimisation/COVERAGE_MATRIX.md)

---

### 36015-sonarcloud-decoration

**Status:** Active  
**Directory:** `.tasks/36015-sonarcloud-decoration/`  
**Objective:** Configure and optimize SonarCloud decoration for PR code quality feedback  
**Completed Subtasks:** 1  
**Last Updated:** October 24, 2025

**Quick Start:**

- Read: [TASK_BRIEF.md](.tasks/36015-sonarcloud-decoration/TASK_BRIEF.md)
- Review: [TASK_SUMMARY.md](.tasks/36015-sonarcloud-decoration/TASK_SUMMARY.md)
- Recent work: [CONVERSATION_SUBTASK_1.md](.tasks/36015-sonarcloud-decoration/CONVERSATION_SUBTASK_1.md)

---

### 38000-cypress-module

**Status:** Completed ✅  
**Directory:** `.tasks/38000-cypress-module/`  
**Objective:** Fix intermittent Cypress test failures caused by dynamic module import errors  
**Completed Subtasks:** 1  
**Last Updated:** October 24, 2025

**Summary:**

- ✅ Investigated intermittent "Failed to fetch dynamically imported module" errors
- ✅ Configured Vite to pre-bundle Cypress dependencies and disable HMR during tests
- ✅ Enhanced Cypress configuration with increased timeouts and environment coordination
- **Result: Eliminated race conditions between Vite dev server and Cypress module loading**

**Quick Start:**

- Read: [TASK_BRIEF.md](.tasks/38000-cypress-module/TASK_BRIEF.md)
- Review: [TASK_SUMMARY.md](.tasks/38000-cypress-module/TASK_SUMMARY.md)
- Implementation details: [CONVERSATION_SUBTASK_1.md](.tasks/38000-cypress-module/CONVERSATION_SUBTASK_1.md)
- Learnings: [SESSION_FEEDBACK.md](.tasks/38000-cypress-module/SESSION_FEEDBACK.md)

---

## Task Template Entry

When adding a new task, copy this template:

```markdown
### {task-id}-{task-name}

**Status:** Active | Paused | Completed
**Directory:** `.tasks/{task-id}-{task-name}/`
**Objective:** Brief description of the task goal
**Completed Subtasks:** N
**Last Updated:** YYYY-MM-DD

**Quick Start:**

- Read: [TASK_BRIEF.md](.tasks/{task-id}-{task-name}/TASK_BRIEF.md)
- Review: [TASK_SUMMARY.md](.tasks/{task-id}-{task-name}/TASK_SUMMARY.md)
- Recent work: [CONVERSATION_SUBTASK_N.md](.tasks/{task-id}-{task-name}/CONVERSATION_SUBTASK_N.md)
```

---

## Completed Tasks Archive

When a task is fully completed, move its entry here:

### 37884-typescript-errors

**Status:** Completed ✅  
**Directory:** `.tasks/37884-typescript-errors/`  
**Objective:** Fix all latent TypeScript errors in the web app  
**Completed Subtasks:** 5 (all subtasks)  
**Completed Date:** October 28, 2025

**Summary:**

- ✅ Fixed 24 TypeScript errors across 17 files
- ✅ Subtask 1: Hook return type + test mock properties (16 errors)
- ✅ Subtask 2: React Flow node types in tests (5 errors)
- ✅ Subtask 3: Generic type constraints (3 errors)
- ✅ Subtask 4: Runtime type safety (1 error)
- ✅ Subtask 5: DataHub & component errors (8 errors)
- **Result: 0 TypeScript errors - verified by user with `npm run build:tsc`**

**Quick Start:**

- Read: [TASK_BRIEF.md](.tasks/37884-typescript-errors/TASK_BRIEF.md)
- Review: [TASK_SUMMARY.md](.tasks/37884-typescript-errors/TASK_SUMMARY.md)
- Complete: [TASK_COMPLETE.md](.tasks/37884-typescript-errors/TASK_COMPLETE.md)
- Subtasks: [CONVERSATION_SUBTASK_1.md](.tasks/37884-typescript-errors/CONVERSATION_SUBTASK_1.md) through [CONVERSATION_SUBTASK_5.md](.tasks/37884-typescript-errors/CONVERSATION_SUBTASK_5.md)

---

**Last Updated:** October 28, 2025
