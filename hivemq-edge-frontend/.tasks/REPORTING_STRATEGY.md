# AI Agent Reporting Strategy

**Last Updated:** October 30, 2025

---

## Overview

This document defines how AI agents should report work completed during sessions, including both permanent documentation (in git) and ephemeral session logs (local only).

---

## Two-Tier Documentation System

### Tier 1: Permanent Documentation (`.tasks/` - IN GIT)

**Purpose:** Long-term reference documentation that should be committed to the repository.

**Location:** `.tasks/` and `.tasks/{task-number}-{task-name}/`

**Files Include:**

- `TASK_BRIEF.md` - Initial task description and requirements
- `TASK_SUMMARY.md` - Overall task progress and status
- `CONVERSATION_SUBTASK_N.md` - Detailed subtask conversations
- `ARCHITECTURE.md` - Technical architecture decisions
- `PHASE_N_SUMMARY.md` - Major phase completions
- Feature-specific documentation (e.g., `LAYOUT_ALGORITHMS_REFERENCE.md`)

**Characteristics:**

- ✅ Committed to git
- ✅ Long-term reference
- ✅ Reviewed by team
- ✅ Part of project history

### Tier 2: Ephemeral Session Logs (`.tasks-log/` - LOCAL ONLY)

**Purpose:** Detailed session-by-session logs that provide context and quick reference but are not committed to git.

**Location:** `.tasks-log/`

**Naming Convention:**

```
{TASK_NUMBER}_{INDEX}_{DESCRIPTIVE_NAME}.md
```

**Examples:**

- `25337_00_SESSION_INDEX.md` - Master index for a session
- `25337_01_Subtask11_Testing_Complete.md` - Detailed summary
- `25337_02_Accessibility_Select_Fix.md` - Specific fix documentation
- `25337_03_CyWait_Removal.md` - Anti-pattern removal details

**Components of the Naming:**

1. **TASK_NUMBER**: The task/issue number (e.g., `25337`)
2. **INDEX**: Sequential number starting from 00 (e.g., `00`, `01`, `02`, `03`)
   - `00` is reserved for SESSION_INDEX
   - `01+` are specific topics/fixes/implementations
3. **DESCRIPTIVE_NAME**: Clear description using PascalCase or snake_case

**Characteristics:**

- ❌ NOT committed to git (in `.gitignore`)
- ⚠️ May be cleaned up periodically
- ✅ Detailed session records
- ✅ Quick reference during development
- ✅ Easy to follow (numbered sequentially)

---

## When to Use Each Tier

### Use Permanent Documentation (`.tasks/`) When:

- ✅ Documenting architectural decisions
- ✅ Recording major milestones or phase completions
- ✅ Providing long-term reference material
- ✅ Creating documentation that the team will review
- ✅ Establishing patterns or guidelines
- ✅ Tracking overall task progress

### Use Session Logs (`.tasks-log/`) When:

- ✅ Documenting a specific work session
- ✅ Recording detailed fix steps
- ✅ Providing troubleshooting context
- ✅ Creating quick reference summaries
- ✅ Logging incremental progress
- ✅ Documenting issues and their resolutions

---

## Session Log Structure

### Always Create a SESSION_INDEX (00)

Every work session should have a master index file:

**File:** `{TASK_NUMBER}_00_SESSION_INDEX.md`

**Contents:**

```markdown
# Task {NUMBER} - Session Index

**Session Date:** {DATE}
**Task:** {TASK_NAME} - {SUBTASK}
**Status:** ✅ COMPLETE / 🔄 IN PROGRESS

---

## Session Logs Created

### 📄 01 - {Title}

**File:** `{TASK}_01_{Name}.md`
Brief description...

### 📄 02 - {Title}

**File:** `{TASK}_02_{Name}.md`
Brief description...

---

## Quick Reference

- Key files created
- Major achievements
- Issues resolved

---

## Session Statistics

- Duration
- Files modified
- Tests created/fixed
- Documentation updated
```

### Individual Topic Logs (01+)

Each specific topic/fix/implementation gets its own numbered file:

**Structure:**

```markdown
# Task {NUMBER} - Session Log: {TOPIC}

**Date:** {DATE}
**Session:** {SUBTASK_NAME}
**Log #:** {INDEX}

---

## Issue/Topic

Description of what was addressed

## Solution/Implementation

How it was solved

## Key Details

Specific technical details, code examples

## Result

Final outcome and verification

---

**Key Takeaway:** Main lesson learned
```

---

## Git Configuration

### `.gitignore` Setup

```gitignore
# Exclude all session logs
.tasks-log/*

# But include the README
!.tasks-log/README.md
```

This ensures:

- ✅ `.tasks-log/README.md` is committed (explains the system)
- ❌ All session logs are excluded from git
- ⚠️ Users know these files may be cleaned up

### `.tasks-log/README.md`

This file **IS committed to git** and explains the ephemeral log system to other developers.

---

## AI Agent Workflow

### At Session Start

1. Check if `.tasks-log/` exists, create if needed
2. Verify `.gitignore` includes `.tasks-log/*` with `!.tasks-log/README.md`
3. Find next available index for the task (check existing `{TASK}_##_*` files)

### During Session

1. Create `{TASK}_00_SESSION_INDEX.md` at the start
2. For each significant fix/feature/issue, create `{TASK}_{NN}_{NAME}.md`
3. Update SESSION_INDEX as you create new logs

### At Session End

1. Finalize SESSION_INDEX with complete statistics
2. Update permanent documentation in `.tasks/` as needed
3. Remind user that session logs are ephemeral and may be cleaned up

---

## Example Session

**Task 25337 - Testing Implementation:**

```
.tasks-log/
├── README.md                              # In git - explains system
├── 25337_00_SESSION_INDEX.md             # Master index
├── 25337_01_Subtask11_Testing_Complete.md
├── 25337_02_Accessibility_Select_Fix.md
├── 25337_03_CyWait_Removal.md
├── 25337_04_File_Corruption_Recovery.md
└── 25337_05_TypeScript_Errors_Fixed.md
```

**Permanent documentation also updated:**

```
.tasks/25337-workspace-auto-layout/
├── CONVERSATION_SUBTASK_11.md            # In git
├── SUBTASK_11_TESTING_COMPLETE.md        # In git
└── ACCESSIBILITY_SELECT_FIX.md           # In git
```

---

## Best Practices

### DO:

- ✅ Use sequential numbering for easy following
- ✅ Create descriptive, searchable filenames
- ✅ Include code examples in session logs
- ✅ Add "Key Takeaway" sections for lessons learned
- ✅ Reference related files and documentation
- ✅ Update SESSION_INDEX throughout the session

### DON'T:

- ❌ Skip the SESSION_INDEX file
- ❌ Use ambiguous file names
- ❌ Mix permanent and ephemeral documentation
- ❌ Forget to update `.gitignore`
- ❌ Assume session logs will persist forever

---

## Maintenance

### Periodic Cleanup

Session logs in `.tasks-log/` should be periodically cleaned up:

**Keep:**

- README.md (always - it's in git)
- Recent session logs (last 1-2 weeks)
- Logs for active/ongoing work

**Remove:**

- Old completed session logs
- Duplicate information (if moved to permanent docs)
- Logs older than 30 days (unless specifically valuable)

### Searching Logs

Use task number prefix to find all logs for a task:

```bash
ls .tasks-log/25337_*
```

Use grep to search content:

```bash
grep -r "accessibility" .tasks-log/
```

---

## Summary

- **Permanent docs** (`.tasks/`) = Long-term, reviewed, in git
- **Session logs** (`.tasks-log/`) = Detailed, ephemeral, local only
- **Naming format**: `{TASK}_{INDEX}_{NAME}.md`
- **Always create**: SESSION_INDEX as `{TASK}_00_SESSION_INDEX.md`
- **Git**: README is committed, logs are excluded
- **Remember**: Session logs may be cleaned up!

---

**AI agents should use this two-tier system for all task documentation.**
