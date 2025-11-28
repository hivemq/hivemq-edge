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

### 38111-workspace-operation-wizard

**Status:** ✅ Phase 1 Complete (Adapters, Bridges, Combiners, Asset Mappers)  
**Directory:** `.tasks/38111-workspace-operation-wizard/`  
**Description:** Interactive wizard system for creating entities directly in the workspace  
**Next Phase:** Integration Point Wizards (TAGs, Topic Filters, Data Mappings)

**Key Documents:**

- [TASK_BRIEF.md](./38111-workspace-operation-wizard/TASK_BRIEF.md) - Requirements and specifications
- [TASK_PLAN.md](./38111-workspace-operation-wizard/TASK_PLAN.md) - 20 subtask implementation plan
- [TASK_SUMMARY.md](./38111-workspace-operation-wizard/TASK_SUMMARY.md) - Progress tracking
- [ARCHITECTURE.md](./38111-workspace-operation-wizard/ARCHITECTURE.md) - Technical design decisions
- [WEB_PRODUCT_DOCUMENTATION.md](38139-wizard-group/WEB_PRODUCT_DOCUMENTATION.md) - User documentation

**Completed:**

- ✅ Wizard state management (Zustand store)
- ✅ Metadata registry for all wizard types
- ✅ Trigger button with dropdown menu
- ✅ Progress bar component
- ✅ Ghost node system (multi-node preview)
- ✅ Configuration panel integration
- ✅ Interactive selection system (Combiner, Asset Mapper)
- ✅ Adapter wizard (complete flow)
- ✅ Bridge wizard (complete flow)
- ✅ Combiner wizard (with selection)
- ✅ Asset Mapper wizard (with Pulse Agent requirement)

---

### 38139-wizard-group

**Status:** 📋 Planning Complete - Ready for Implementation  
**Directory:** `.tasks/38139-wizard-group/`  
**Description:** Group wizard implementation (continuation of task 38111)  
**Dependencies:** Task 38111 (Workspace Operation Wizard)

**Key Documents:**

- [TASK_BRIEF.md](./38139-wizard-group/TASK_BRIEF.md) - Requirements and constraints
- [TASK_PLAN.md](./38139-wizard-group/TASK_PLAN.md) - 8 subtask implementation plan (2-3 weeks)
- [QUICK_REFERENCE.md](./38139-wizard-group/QUICK_REFERENCE.md) - Quick start guide

**Planned Subtasks:**

**Phase 1: Foundation (Days 1-3)**

- [ ] Subtask 1: Group Selection Constraints (1-2 days)
- [ ] Subtask 2: Auto-Inclusion Visual Feedback (1 day)

**Phase 2: Ghost System (Days 4-7)**

- [ ] Subtask 3: Ghost Group Factory (2 days)
- [ ] Subtask 4: Ghost Group Renderer Enhancement (2 days)

**Phase 3: Configuration (Days 8-10)**

- [ ] Subtask 5: Configuration Panel (1.5 days)
- [ ] Subtask 6: Wizard Completion (2 days)

**Phase 4: Testing & Polish (Days 11-13)**

- [ ] Subtask 7: E2E Testing (1.5 days)
- [ ] Subtask 8: Documentation & Polish (1 day)

**Key Requirements:**

- Groups can select ADAPTER, BRIDGE, CLUSTER (group) nodes
- Nodes already in groups cannot be re-grouped
- DEVICE/HOST nodes auto-included (not directly selectable)
- Ghost group appears in Step 1 (preview), not Step 0 (selection)
- React Flow parent-child group implementation
- Reuse existing `createGroup` utility

**Next Action:** Begin Subtask 1 (Group Selection Constraints)

---

### 37542-code-coverage

**Status:** 🔄 Active  
**Directory:** `.tasks/37542-code-coverage/`  
**Description:** Code coverage improvements for HiveMQ Edge Frontend  
**Progress:** 3 subtasks completed, 42 tests added, 11 files improved

---

## Completed Tasks

### 37884-typescript-errors

**Status:** ✅ Complete  
**Description:** Fixed TypeScript errors across the codebase

### 36665-resource-version-update

**Status:** ✅ Complete  
**Description:** Updated resource version handling

### 37055-workspace-search

**Status:** ✅ Complete  
**Description:** Workspace search functionality

### 32118-workspace-status

**Status:** ✅ Complete  
**Description:** Workspace status indicators and propagation

### 25337-workspace-auto-layout

**Status:** ✅ Complete  
**Description:** Automatic layout algorithms for workspace

---

## Task Naming Convention

Tasks follow the pattern: `{task-id}-{kebab-case-description}`

Example: `38139-wizard-group`

---

## Task Documentation Structure

Each task directory contains:

```
.tasks/{task-id}-{task-name}/
├── TASK_BRIEF.md                   # Requirements and objectives
├── TASK_PLAN.md                    # Implementation plan with subtasks
├── TASK_SUMMARY.md                 # Progress tracking
├── QUICK_REFERENCE.md              # Quick start guide (optional)
├── ARCHITECTURE.md                 # Technical decisions (optional)
├── CONVERSATION_SUBTASK_N.md       # Session logs
└── SESSION_FEEDBACK.md             # Feedback and learnings
```

---

## Guidelines & References

All tasks should follow these guidelines located in `.tasks/`:

- **AUTONOMY_TEMPLATE.md** - Work patterns for AI agents
- **DATAHUB_ARCHITECTURE.md** - DataHub designer architecture
- **DESIGN_GUIDELINES.md** - UI component patterns
- **TESTING_GUIDELINES.md** - Mandatory accessibility testing
- **I18N_GUIDELINES.md** - Translation key structure
- **WORKSPACE_TOPOLOGY.md** - Node types and relationships
- **REACT_FLOW_BEST_PRACTICES.md** - React Flow patterns

---

**Last Updated:** November 21, 2025  
**Maintained By:** AI Agents & Development Team
