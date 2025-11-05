# Ephemeral Session Logs

**⚠️ IMPORTANT: These files are NOT committed to git and may be cleaned up periodically.**

---

## Purpose

This directory contains detailed session-by-session logs that provide context and quick reference during active development. Unlike the permanent documentation in `.tasks/`, these logs are:

- ❌ **NOT committed to git** (excluded in `.gitignore`)
- ⚠️ **May be cleaned up** after 30 days
- ✅ **Detailed session records** with troubleshooting context
- ✅ **Quick reference** during development

---

## Naming Convention

```
{TASK_NUMBER}_{INDEX}_{DESCRIPTIVE_NAME}.md
```

**Examples:**
- `99999_00_SESSION_INDEX.md` - Master index for a session
- `99999_01_Discovery_Context.md` - Detailed discovery session
- `99999_02_Network_Graph_Implementation.md` - Specific implementation

**Components:**
1. **TASK_NUMBER** - Task/issue number (e.g., `99999`)
2. **INDEX** - Sequential number (00 = SESSION_INDEX, 01+ = topics)
3. **DESCRIPTIVE_NAME** - Clear description in PascalCase

---

## Current Sessions

### Task 99999 - Domain Ontology Visualisation
- `99999_00_SESSION_INDEX.md` - Session overview and summary
- `99999_01_Discovery_Context.md` - Initial discovery and analysis

---

## How to Use

### Finding Logs for a Task
```bash
ls .tasks-log/99999_*
```

### Searching Content
```bash
grep -r "React Flow" .tasks-log/
```

### Viewing Session Index
```bash
cat .tasks-log/99999_00_SESSION_INDEX.md
```

---

## Permanent Documentation

For long-term documentation that IS committed to git, see `.tasks/`:
- `.tasks/ACTIVE_TASKS.md` - Master task index
- `.tasks/{task-id}-{task-name}/` - Task-specific documentation

---

## Maintenance

Session logs should be cleaned up periodically:

**Keep:**
- This README.md (always in git)
- Recent session logs (last 1-2 weeks)
- Logs for active/ongoing work

**Remove:**
- Completed session logs older than 30 days
- Duplicate information (if moved to permanent docs)

---

**For more information, see:** `.tasks/REPORTING_STRATEGY.md`

