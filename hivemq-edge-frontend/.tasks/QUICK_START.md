# Quick Start Guide: Resuming Work Across Conversations

**📍 IMPORTANT: All task files are located in the `.tasks/` directory**

## For Users: How to Resume a Task

When starting a new conversation to continue work on a task, simply say:

### Option 1: Simple Task Reference
```
We're working on task 37542-code-coverage
```

### Option 2: Even Simpler
```
Continue task 37542
```

### Option 3: With Context Request
```
We're working on task 37542. Please review the progress so far.
```

That's it! The AI will automatically:
- Look in the `.tasks/` directory
- Find the task in `.tasks/ACTIVE_TASKS.md`
- Load all task files from `.tasks/{task-id}-{task-name}/`
- Review completed subtasks
- Be ready to continue

**Key Point:** You don't need to tell the AI where to find files - they're always in `.tasks/`

---

## What the AI Will Do

The AI agent will automatically:

1. ✅ Read `.tasks/ACTIVE_TASKS.md` to locate your task
2. ✅ Load `.tasks/37542-code-coverage/TASK_BRIEF.md` for context
3. ✅ Review `.tasks/37542-code-coverage/TASK_SUMMARY.md` for progress
4. ✅ Check the latest conversation files if needed
5. ✅ Summarize what's been done and ask what's next

---

## Example Conversation Starter

**You:** "We're working on task 37542-code-coverage"

**AI Response:** "I can see we're working on code coverage improvements for the HiveMQ Edge Frontend. You've completed 3 subtasks so far, covering 11 files with 42 new test cases. The last work was on DomainOntology hooks. What would you like to work on next?"

---

## Tips for Effective Task Resumption

### ✅ DO:
- Mention the task ID (e.g., "37542") or full name ("37542-code-coverage")
- Let the AI load the context first before diving into work
- Ask for a summary if you need a refresher

### ❌ DON'T:
- Explain the entire history yourself (it's in the docs!)
- Assume the AI remembers previous conversations
- Skip mentioning which task you're on

---

## File Structure Reference

**All task files are organized under `.tasks/` directory:**

```
.tasks/                                ← Everything is here!
├── ACTIVE_TASKS.md                    # ← Index of all tasks
├── AUTONOMY_TEMPLATE.md               # ← Work patterns & guidelines
├── QUICK_START.md                     # ← This file
└── 37542-code-coverage/               # ← Your specific task
    ├── TASK_BRIEF.md                  # Task overview
    ├── TASK_SUMMARY.md                # All subtasks & progress
    ├── CONVERSATION_SUBTASK_1.md      # Conversation history
    ├── CONVERSATION_SUBTASK_2.md      # Conversation history
    ├── CONVERSATION_SUBTASK_3.md      # Conversation history
    └── SESSION_FEEDBACK.md            # Learnings & feedback
```

**Never at project root** - all task files are under `.tasks/`

---

## Current Active Tasks

Check `.tasks/ACTIVE_TASKS.md` for the full list, but here's what's currently active:

- **37542-code-coverage** - Code coverage improvements (3 subtasks completed)

---

**Last Updated:** October 17, 2025
