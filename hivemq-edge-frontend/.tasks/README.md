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

### For AI Agents

- **[AUTONOMY_TEMPLATE.md](./AUTONOMY_TEMPLATE.md)** - Work patterns and best practices
- **[QUICK_START.md](./QUICK_START.md)** - How to resume tasks in new conversations

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
├── AUTONOMY_TEMPLATE.md               ← AI agent work patterns and best practices
├── AI_OPTIMIZATION_GUIDE.md           ← Token and context optimisation for AI agents
├── DEFAULT_BOARD.md                   ← Linear team configuration
├── HOW_TO_MAKE_AI_FOLLOW_GUIDELINES.md ← Meta-guide on rule enforcement
├── PARALLEL_EXECUTION_TEMPLATE.md     ← Multi-agent orchestration framework
├── QUICK_START.md                     ← Resuming tasks in a new conversation
├── REPORTING_STRATEGY.md              ← Documentation tier strategy (.tasks vs docs/)
│
└── {task-id}-{task-name}/             ← Individual task directories
    ├── TASK_BRIEF.md
    ├── TASK_SUMMARY.md
    ├── CONVERSATION_SUBTASK_1.md
    └── assets/                        ← Screenshots, diagrams, etc.
```

Reference documentation (guides, architecture, API patterns) lives in `docs/`.
Skills and agents live in `.claude/skills/` and `.claude/agents/`.

## For AI Agents

When a user mentions working on a task:

1. ✅ Navigate to `.tasks/{task-id}-{task-name}/`
2. ✅ Load task context from files in that directory
3. ✅ Never look for task files at project root

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

Active tasks are tracked in Linear. See [DEFAULT_BOARD.md](./DEFAULT_BOARD.md) for team configuration.

---

**Last Updated:** February 16, 2026 (Linear Migration)
