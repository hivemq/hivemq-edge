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

<!-- No completed tasks yet -->

---

**Last Updated:** October 23, 2025
