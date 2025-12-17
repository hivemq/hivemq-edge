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

### 38658-adapter-jsonschema-review

**Status:** ✅ Phase 1 & 2 Complete - Research, Analysis & Backend Comparison  
**Directory:** `.tasks/38658-adapter-jsonschema-review/`  
**Description:** Cross-functional research task analyzing protocol adapter JSON-Schema and UI-Schema configurations  
**Type:** Research / Analysis

**Key Documents:**

- [TASK_BRIEF.md](./38658-adapter-jsonschema-review/TASK_BRIEF.md) - Task objectives and scope
- [ADAPTER_INVENTORY.md](./38658-adapter-jsonschema-review/ADAPTER_INVENTORY.md) - Complete adapter inventory
- [SCHEMA_ANALYSIS.md](./38658-adapter-jsonschema-review/SCHEMA_ANALYSIS.md) - Detailed schema analysis
- [REMEDIATION_REPORT.md](./38658-adapter-jsonschema-review/REMEDIATION_REPORT.md) - Issues and fixes (v2.0)
- [BACKEND_COMPARISON.md](./38658-adapter-jsonschema-review/BACKEND_COMPARISON.md) - Backend vs Frontend comparison
- [TASK_SUMMARY.md](./38658-adapter-jsonschema-review/TASK_SUMMARY.md) - Progress tracking

**Backend Modules Analyzed:** 7 modules, 7 UI schemas, 4 Java config files

**Findings:**

- 🔴 **CRITICAL:** File adapter tag schema is complete copy of HTTP adapter (wrong fields, wrong protocolId)
- 🔴 **HIGH:** Databases adapter backend bug - port field has invalid string constraints on Integer
- 🟡 Modbus UI schema `id.ui:disabled` mismatch (backend=true, frontend=false)
- 10 total issues identified

**Critical Issues:**

- ❌ File adapter tag schema has wrong `protocolId`
- ❌ Databases adapter has invalid port field constraints

**Next Steps:** Apply critical fixes or continue analysis

---

### 37937-datahub-resource-edit-flow

**Status:** ✅ Phase 2 Complete (Resource Editors + Simplified Panels)  
**Directory:** `.tasks/37937-datahub-resource-edit-flow/`  
**Description:** Refactor DataHub resource creation/editing flow - separate resource management from policy configuration  
**Next Phase:** Phase 3 - Publishing Flow Updates

**Key Documents:**

- [TASK_BRIEF.md](./37937-datahub-resource-edit-flow/TASK_BRIEF.md) - Requirements and specifications
- [TASK_PLAN.md](./37937-datahub-resource-edit-flow/TASK_PLAN.md) - 12 subtask implementation plan
- [TASK_SUMMARY.md](./37937-datahub-resource-edit-flow/TASK_SUMMARY.md) - Progress tracking
- [PHASE_2_COMPLETE.md](./37937-datahub-resource-edit-flow/PHASE_2_COMPLETE.md) - Phase 2 completion summary

**Completed:**

- ✅ Phase 1: Resource editor infrastructure (SchemaEditor, ScriptEditor) - 66 tests
- ✅ Phase 2: Simplified node configuration panels - 87 tests
- ✅ Extra: Complete protobuf messageType support with encode/decode utilities
- ✅ Documentation: Updated RJSF_GUIDELINES.md with Widgets vs Fields patterns

**Progress:** Phase 1 (5/5 ✅), Phase 2 (4/4 ✅), Phase 3 (0/3), Phase 4 (0/1)

---

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

### 37937-datahub-resource-edit-flow

**Status:** 📋 Planning Complete - Ready for Implementation  
**Directory:** `.tasks/37937-datahub-resource-edit-flow/`  
**Description:** Refactor DataHub resource (schemas and scripts) creation and editing flow  
**Progress:** 0/12 subtasks, Planning phase complete

**Key Documents:**

- [TASK_BRIEF.md](./37937-datahub-resource-edit-flow/TASK_BRIEF.md) - Requirements and context
- [TASK_PLAN.md](./37937-datahub-resource-edit-flow/TASK_PLAN.md) - 12 subtask implementation plan (4 phases)
- [TASK_SUMMARY.md](./37937-datahub-resource-edit-flow/TASK_SUMMARY.md) - Progress tracking
- [QUICK_REFERENCE.md](./37937-datahub-resource-edit-flow/QUICK_REFERENCE.md) - Quick start guide

**Objective:**

Separate DataHub resource management from policy node configuration:

- Move schema/script creation to DataHub main page (from tables)
- Simplify node panels to resource selection only (name + version dropdowns)
- Remove resource publishing from policy publish flow
- Reduce panel complexity by >50% LOC

**Planned Phases:**

**Phase 1: Resource Editor Infrastructure (Days 1-5)**

- [ ] Subtask 1.1: ResourceEditorDrawer base component
- [ ] Subtask 1.2: SchemaEditor implementation
- [ ] Subtask 1.3: ScriptEditor implementation
- [ ] Subtask 1.4: Integration with SchemaTable
- [ ] Subtask 1.5: Integration with ScriptTable

**Phase 2: Simplified Node Configuration (Days 6-9)**

- [ ] Subtask 2.1: ResourceSelector component
- [ ] Subtask 2.2: SchemaPanelSimplified (<100 LOC)
- [ ] Subtask 2.3: FunctionPanelSimplified (<100 LOC)
- [ ] Subtask 2.4: Node display updates

**Phase 3: Publishing Flow Updates (Days 10-12)**

- [ ] Subtask 3.1: Dry-run validation updates
- [ ] Subtask 3.2: Publishing logic refactor
- [ ] Subtask 3.3: PolicySummaryReport updates

**Phase 4: Testing & Documentation (Days 13-15)**

- [ ] Subtask 4.1: Component test coverage (>80%)
- [ ] Subtask 4.2: E2E test scenarios
- [ ] Subtask 4.3: Documentation & migration guide

**Key Requirements:**

- Resource CRUD from DataHub main page (Schema/Script tables)
- Node panels: simple selection only (no creation)
- > 50% LOC reduction in SchemaPanel/FunctionPanel
- Backward compatibility with existing policies
- Feature flag for gradual rollout

**Next Action:** Review task plan and begin Subtask 1.1 (ResourceEditorDrawer)

---

### 37542-code-coverage

**Status:** 🔄 Active  
**Directory:** `.tasks/37542-code-coverage/`  
**Description:** Code coverage improvements for HiveMQ Edge Frontend  
**Progress:** 3 subtasks completed, 42 tests added, 11 files improved

---

### XXXXX-frontend-chakrav3

**Status:** 📋 Investigation Phase  
**Directory:** `.tasks/XXXXX-frontend-chakrav3/`  
**Description:** Chakra UI v2 to v3 migration preparation and execution  
**Progress:** Investigation complete, planning phase

**Key Documents:**

- [TASK_BRIEF.md](./XXXXX-frontend-chakrav3/TASK_BRIEF.md) - Main overview and summary
- [REPORT_FEATURE_DISTRIBUTION.md](./XXXXX-frontend-chakrav3/REPORT_FEATURE_DISTRIBUTION.md) - Workspace vs DataHub vs Core analysis
- [REPORT_OPENAPI_MIGRATION.md](./XXXXX-frontend-chakrav3/REPORT_OPENAPI_MIGRATION.md) - OpenAPI generator migration analysis
- [REPORT_RJSF_MIGRATION.md](./XXXXX-frontend-chakrav3/REPORT_RJSF_MIGRATION.md) - RJSF custom components analysis

**Investigation Summary:**

| Area      | Files  | Impact                       |
| --------- | ------ | ---------------------------- |
| Workspace | ~200   | High (React Flow + Chakra)   |
| DataHub   | ~321   | High (Policy Designer)       |
| OpenAPI   | ~210+  | Critical (complete rewrite)  |
| RJSF      | ~100+  | Critical (major refactoring) |
| Core App  | ~1,095 | Medium                       |

**Next Steps:**

1. [ ] Create detailed migration plan with phases
2. [ ] Identify Chakra v2 → v3 breaking changes
3. [ ] Evaluate OpenAPI generator with POC
4. [ ] Monitor `@rjsf/chakra-ui` for v3 support
5. [ ] Estimate effort for each migration phase

---

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

**Last Updated:** December 12, 2025  
**Maintained By:** AI Agents & Development Team
