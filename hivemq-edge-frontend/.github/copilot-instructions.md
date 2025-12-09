# AI Context and Task Documentation

## ⚠️ READ THIS FIRST: MANDATORY RULES

**[AI_MANDATORY_RULES.md](.github/AI_MANDATORY_RULES.md)** ← **READ THIS ENTIRE DOCUMENT BEFORE DOING ANYTHING**

This document contains rules that prevent you from wasting 1-2 hours on problems that take 15 minutes to solve when you follow guidelines.

**Common failures these rules prevent:**

- Not reading guidelines when user references them (wastes 1-2 hours)
- Running all tests instead of one test when debugging (wastes 1 hour)
- Not using HTML snapshots for Cypress debugging (wastes 1-2 hours)
- Skipping tests instead of fixing them (incomplete work)
- Ignoring repeated instructions (frustrates user)

**READ THE MANDATORY RULES NOW. IT TAKES 3 MINUTES AND SAVES HOURS.**

---

## Quick Reference for AI Agents

All task-related documentation is located in the `.tasks/` directory:

- **Active Tasks Index**: [.tasks/ACTIVE_TASKS.md](./.tasks/ACTIVE_TASKS.md)
- **AI Guidelines**: [.tasks/AUTONOMY_TEMPLATE.md](./.tasks/AUTONOMY_TEMPLATE.md)
- **Quick Start Guide**: [.tasks/QUICK_START.md](./.tasks/QUICK_START.md)

## Current Tasks

### 37542-code-coverage

Code coverage improvements for HiveMQ Edge Frontend

- **Status**: Active
- **Location**: `.tasks/37542-code-coverage/`
- **Progress**: 3 subtasks completed, 42 tests added, 11 files improved

## For AI Agents

When a user mentions a task:

1. Read `.tasks/ACTIVE_TASKS.md` to find the task
2. Navigate to `.tasks/{task-id}-{task-name}/`
3. Load context from TASK_BRIEF.md and TASK_SUMMARY.md
4. Review recent CONVERSATION_SUBTASK_N.md files

## Documentation Structure

```
.tasks/
├── ACTIVE_TASKS.md              # Master task index
├── AUTONOMY_TEMPLATE.md         # Work patterns & guidelines
└── {task-id}-{task-name}/
    ├── TASK_BRIEF.md
    ├── TASK_SUMMARY.md
    ├── CONVERSATION_SUBTASK_N.md
    └── SESSION_FEEDBACK.md
```

---

_This file provides quick context for AI assistants working in this codebase._

# Task Documentation & AI Context

This directory contains comprehensive task documentation, progress tracking, and AI work guidelines.

**When working on tasks, AI agents should consult this directory first.**

## Important Guidelines

- **DataHub Architecture**: [.tasks/DATAHUB_ARCHITECTURE.md](../.tasks/DATAHUB_ARCHITECTURE.md) - DataHub designer architecture, state management, E2E testing patterns
- **Design Guidelines**: [.tasks/DESIGN_GUIDELINES.md](../.tasks/DESIGN_GUIDELINES.md) - UI component patterns, button variants, styling conventions
- **Testing Guidelines**: [.tasks/TESTING_GUIDELINES.md](../.tasks/TESTING_GUIDELINES.md) - Mandatory accessibility testing, component test patterns

See [README.md](.tasks/README.md) for full documentation structure.
