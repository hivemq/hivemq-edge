# .tasks/ Directory - Task Documentation Central

## 📍 This is Where ALL Task Files Live

**Golden Rule:** Every task-related file, document, history, and summary belongs in this `.tasks/` directory.

## Directory Purpose

This directory contains:

- ✅ Task registry and index
- ✅ AI work guidelines and patterns
- ✅ Individual task subdirectories
- ✅ Task briefs, summaries, and conversation histories
- ✅ Feedback and retrospectives
- ✅ Integration guides for your dev tools

## Quick Links

### For Getting Started

- **[QUICK_START.md](./QUICK_START.md)** - How to resume tasks in new conversations
- **[WEBSTORM_SETUP.md](./WEBSTORM_SETUP.md)** - Configure your IDE for task work

### For AI Agents

- **[ACTIVE_TASKS.md](./ACTIVE_TASKS.md)** - START HERE: Registry of all tasks
- **[AUTONOMY_TEMPLATE.md](./AUTONOMY_TEMPLATE.md)** - Work patterns and best practices

## Why Everything Goes Here

1. **Single Source of Truth** - No confusion about where task files live
2. **Easy Navigation** - AI agents know exactly where to look
3. **Cross-Conversation Continuity** - Resume work seamlessly in new threads
4. **Organization** - Keep project root clean, tasks separate
5. **Version Control** - All task history tracked in git

## Structure Overview

```
.tasks/                                ← YOU ARE HERE
│
├── README.md                          ← This file
├── ACTIVE_TASKS.md                    ← Master index of all tasks
├── AUTONOMY_TEMPLATE.md               ← AI guidelines & best practices
├── CODE_COMMENTS_GUIDELINES.md        ← Rules for code comments & documentation
├── CYPRESS_TESTING_GUIDELINES.md      ← ⭐ CONSOLIDATED Cypress testing reference (Nov 12, 2025)
├── CYPRESS_LOGGING_INDEX.md           ← Master index (updated Nov 12, 2025 to reference consolidated doc)
├── DATAHUB_ARCHITECTURE.md            ← DataHub architecture & E2E testing guide
├── DESIGN_GUIDELINES.md               ← UI/UX design patterns & standards
├── ERROR_MESSAGE_TRACING_PATTERN.md   ← Full-Stack Error Tracing guidelines
├── I18N_GUIDELINES.md                 ← Internationalization guidelines
├── MONACO_TESTING_GUIDE.md            ← Monaco Editor Testing Guide for Cypress
├── QUICK_START.md                     ← User guide for resuming work
├── TESTING_GUIDELINES.md              ← Comprehensive testing standards (now references Cypress doc)
├── USER_DOCUMENTATION_GUIDELINE.md    ← Guide for creating end-user documentation
├── WEBSTORM_SETUP.md                  ← IDE configuration guide
├── WEBSTORM_TEMPLATES.md              ← Live templates for WebStorm
├── WORKSPACE_TESTING_GUIDELINES.md    ← Workspace-specific testing patterns
├── FOR_CONSIDERATION.md               ← Future improvements
│
└── {task-id}-{task-name}/             ← Individual task directories
    ├── TASK_BRIEF.md
    ├── TASK_SUMMARY.md
    ├── CONVERSATION_SUBTASK_1.md
    ├── CONVERSATION_SUBTASK_N.md
    ├── SESSION_FEEDBACK.md
    └── assets/                       ← Screenshots, diagrams, etc.
```

**Deleted Files (Consolidated Nov 12, 2025):**

- ❌ CYPRESS_BEST_PRACTICES.md → See CYPRESS_TESTING_GUIDELINES.md
- ❌ CYPRESS_TESTING_BEST_PRACTICES.md → See CYPRESS_TESTING_GUIDELINES.md
- ❌ CYPRESS_LOGGING_SETUP.md → See CYPRESS_TESTING_GUIDELINES.md
- ❌ CYPRESS_LOGGING_VERIFICATION.md → See CYPRESS_TESTING_GUIDELINES.md

## For AI Agents

When a user mentions working on a task:

1. ✅ Read `.tasks/ACTIVE_TASKS.md` first
2. ✅ Navigate to `.tasks/{task-id}-{task-name}/`
3. ✅ Load task context from files in that directory
4. ✅ Never look for task files at project root

## For Users

### Starting a New Task

```bash
# Create task directory matching Linear issue
mkdir -p .tasks/EDG-40-technical-documentation
```

### Resuming Work in New Conversation

Just say: "We're working on task EDG-40" or "Let's continue with EDG-40"

The AI knows to look in `.tasks/EDG-40-*/` automatically.

See [QUICK_START.md](./QUICK_START.md) for details.

## Integration with Your Tools

This task system integrates with:

- ✅ **Linear** - Task tracking and issue management
- ✅ **Figma** - Reference designs in TASK_BRIEF
- ✅ **MIRO** - Link architecture diagrams
- ✅ **Google Docs** - Reference detailed specs
- ✅ **WebStorm** - Live templates and external tools
- ✅ **Git** - Commit messages and PR templates reference tasks

## Current Tasks

Check [ACTIVE_TASKS.md](./ACTIVE_TASKS.md) for the list of active tasks.

---

**Last Updated:** February 16, 2026 (Linear Migration)
