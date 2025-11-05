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

### 37542-code-coverage

Code coverage improvements for HiveMQ Edge Frontend

- **Status**: Active
- **Location**: `.tasks/37542-code-coverage/`
- **Progress**: 3 subtasks completed, 42 tests added, 11 files improved

### 38018-domain-ontology-visualisation

Data visualization enhancements for integration point connectivity

- **Status**: Active (Discovery Phase)
- **Location**: `.tasks/38018-domain-ontology-visualisation/`
- **Progress**: Task structure created, domain model documented, current implementation analyzed
- **Next**: Performance benchmarking and enhancement implementation

---

## Completed Tasks

_(Move tasks here when completed)_

---

## Task Status Legend

- 🚀 **Active**: Currently being worked on
- 📋 **Planning**: Initial planning/design phase
- ⏸️ **Paused**: Temporarily on hold
- ✅ **Completed**: Finished and merged
- ❌ **Cancelled**: No longer needed

---

**Last Updated**: November 4, 2025
