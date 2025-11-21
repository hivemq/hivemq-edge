# Task 38111: Workspace Operation Wizard - Planning Complete

**Date:** November 10, 2025  
**Status:** 🎯 Ready for Development

---

## 📋 Executive Summary

Task 38111 planning is **complete**. We have a comprehensive plan to implement a wizard system for creating entities and integration points directly in the workspace.

### What Was Accomplished

✅ **5 Planning Documents Created:**

1. **TASK_BRIEF.md** - Requirements and acceptance criteria (already existed)
2. **TASK_PLAN.md** - Detailed 20-subtask implementation plan (NEW)
3. **TASK_SUMMARY.md** - Progress tracking document (NEW)
4. **ARCHITECTURE.md** - Technical architecture decisions (NEW)
5. **I18N_STRUCTURE.md** - Translation keys and patterns (NEW)

✅ **Session Log System Established:**

- `.tasks-log/38111_00_SESSION_INDEX.md` created
- Template for future sessions documented
- Naming convention established

✅ **All Guidelines Reviewed:**

- ✅ I18N_GUIDELINES.md - Plain strings, context usage
- ✅ TESTING_GUIDELINES.md - Accessibility first, pragmatic approach
- ✅ REPORTING_STRATEGY.md - Two-tier documentation
- ✅ DESIGN_GUIDELINES.md - Button variants, modal patterns
- ✅ WORKSPACE_TOPOLOGY.md - Node types and connections

---

## 🎨 Design Approach

### Four-Component Architecture

```
1. TRIGGER        → CreateEntityButton in CanvasToolbar
2. PROGRESS BAR   → React Flow Panel showing current step
3. GHOST NODES    → Visual preview on canvas
4. CONFIG PANEL   → Side drawer with forms
```

### Phased Implementation

**Phase 1: Foundation & Adapter** (7 subtasks)

- Core wizard system
- State management (Zustand)
- UI components (trigger, progress, ghost nodes)
- Complete adapter creation flow

**Phase 2: Entity Wizards** (4 subtasks)

- Bridge, Combiner, Asset Mapper, Group

**Phase 3: Integration Points** (4 subtasks)

- TAG, TOPIC FILTER, DATA MAPPING, DATA COMBINING

**Phase 4: Polish** (5 subtasks)

- Orchestrator, Selection system, Error handling, Keyboard/A11y, Docs

---

## 🔑 Key Decisions

### 1. State Management: Zustand

**Why:** Already used in workspace, better performance, no prop drilling

### 2. Testing: Accessibility First

**Strategy:** All components have tests, but only accessibility tests unskipped initially

- ✅ Ensures accessibility from day one
- ✅ Enables rapid development
- ✅ Tests documented and ready to unskip later

### 3. i18n: Context-Based with Plain Strings

**Pattern:** `t('workspace.wizard.entityType.name', { context: 'ADAPTER' })`

- ❌ NEVER: `t(\`workspace.wizard.\${type}.name\`)` (template literals forbidden)
- ✅ ALWAYS: Plain string keys with context parameter

### 4. Ghost Nodes: Layout Engine Integration

**Approach:** Reuse existing layout algorithms for positioning

- Automatic collision avoidance
- Consistent with manual placement
- Visual distinction (50% opacity, dashed border)

### 5. Form Reusability: Minimal Adaptation

**Strategy:** Add optional `wizardContext` prop to existing forms

- No code duplication
- Forms remain independently usable
- Validation logic unchanged

---

## 📊 Implementation Breakdown

### Subtask Distribution

| Phase   | Subtasks | Focus Area           |
| ------- | -------- | -------------------- |
| Phase 1 | 1-7      | Foundation & Adapter |
| Phase 2 | 8-11     | Other Entities       |
| Phase 3 | 12-15    | Integration Points   |
| Phase 4 | 16-20    | Polish & Enhancement |

### Estimated Timeline

- **Phase 1:** 2-3 weeks
- **Phase 2:** 1.5-2 weeks
- **Phase 3:** 1.5-2 weeks
- **Phase 4:** 1-1.5 weeks
- **Total:** 6-9 weeks

---

## 🎯 Next Steps

### Immediate Next Action

**Start Subtask 1: Wizard State Management & Types**

**Deliverables:**

1. Create `src/modules/Workspace/hooks/useWizardStore.ts`
2. Define TypeScript interfaces for wizard state
3. Implement Zustand store with actions
4. Create convenience hooks
5. Add basic test file (accessibility test only, unskipped)

**Files to Create:**

```
src/modules/Workspace/
└── hooks/
    ├── useWizardStore.ts
    └── useWizardStore.spec.cy.tsx (test)
```

### Before Starting Development

1. **Review all planning documents:**

   - [ ] TASK_BRIEF.md - Understand requirements
   - [ ] TASK_PLAN.md - Review full plan
   - [ ] ARCHITECTURE.md - Understand technical approach
   - [ ] I18N_STRUCTURE.md - Know translation patterns

2. **Set up development environment:**

   - [ ] Branch: `feature/38111-workspace-wizard`
   - [ ] Clean workspace
   - [ ] Dependencies up to date

3. **Reference materials ready:**
   - [ ] Guidelines documents bookmarked
   - [ ] Existing workspace code reviewed
   - [ ] React Flow docs accessible

---

## 📚 Document Locations

### Permanent Documentation (Git)

```
.tasks/38111-workspace-operation-wizard/
├── TASK_BRIEF.md          # Requirements
├── TASK_PLAN.md           # Implementation plan
├── TASK_SUMMARY.md        # Progress tracker
├── ARCHITECTURE.md        # Technical decisions
└── I18N_STRUCTURE.md      # Translation keys
```

### Session Logs (Local Only)

```
.tasks-log/
└── 38111_00_SESSION_INDEX.md   # Session index
```

### Guidelines (Reference)

```
.tasks/
├── I18N_GUIDELINES.md
├── TESTING_GUIDELINES.md
├── REPORTING_STRATEGY.md
├── DESIGN_GUIDELINES.md
└── WORKSPACE_TOPOLOGY.md
```

---

## 🚨 Critical Reminders

### i18n Rules (NON-NEGOTIABLE)

❌ **NEVER:**

```typescript
t(`workspace.wizard.${type}.name`) // Template literals FORBIDDEN
```

✅ **ALWAYS:**

```typescript
t('workspace.wizard.entityType.name', { context: type }) // Plain strings with context
```

### Testing Rules (NON-NEGOTIABLE)

✅ **Every component MUST have:**

- Accessibility test (UNSKIPPED, must pass)
- Other tests (SKIPPED initially)

```typescript
it('should be accessible', () => {
  cy.injectAxe()
  cy.mountWithProviders(<Component />)
  cy.checkAccessibility()  // NOT cy.checkA11y()
})

it.skip('should render correctly', () => {
  // Skipped during initial development
})
```

### Select Components (ACCESSIBILITY)

❌ **WRONG:**

```tsx
<Select value={value}>  {/* Missing accessible name */}
```

✅ **CORRECT:**

```tsx
<Select aria-label={t('key.ariaLabel')} value={value}>
```

---

## 🎨 Design Principles

1. **Progressive Disclosure** - Show complexity only when needed
2. **Visual Feedback** - Ghost nodes, progress bar, selections
3. **Accessibility First** - Keyboard nav, screen readers, ARIA
4. **Reusability** - Leverage existing components
5. **Extensibility** - Easy to add new entity types
6. **Consistency** - Unified creation experience

---

## 📈 Success Metrics

### Quantitative

- Completion rate: % of started wizards completed
- Time to create: Average duration
- Error rate: % with errors
- Accessibility: 100% of tests passing

### Qualitative

- User feedback surveys
- Usability testing observations
- Developer feedback on adding wizards
- Support ticket reduction

---

## ⚠️ Risk Management

| Risk                 | Severity  | Mitigation                        |
| -------------------- | --------- | --------------------------------- |
| Complexity Creep     | 🔴 High   | Strict step limits, user testing  |
| Form Integration     | 🔴 High   | Minimal changes, thorough testing |
| Ghost Node Confusion | 🔴 High   | Clear visuals, labels, tooltips   |
| Performance Impact   | 🟡 Medium | Limit ghosts, optimize rendering  |
| Accessibility Gaps   | 🟡 Medium | Mandatory tests, expert review    |

---

## 🎓 Key Learnings for AI Agents

### When to Update Documentation

**TASK_SUMMARY.md:**

- After each subtask completion
- When changing phase
- Weekly progress updates

**Session Logs (.tasks-log/):**

- After each work session
- When solving significant issues
- When making architectural decisions

**CONVERSATION_SUBTASK_N.md:**

- For detailed subtask discussions
- Complex problem-solving
- Design debates

### Documentation Strategy

**Permanent (Git):**

- Architecture decisions
- Major milestones
- Long-term reference
- Team-reviewed content

**Ephemeral (Local):**

- Daily work logs
- Debugging notes
- Quick references
- Session summaries

---

## ✅ Planning Checklist

- [x] Requirements understood (TASK_BRIEF.md)
- [x] Implementation plan created (TASK_PLAN.md)
- [x] Architecture designed (ARCHITECTURE.md)
- [x] i18n structure defined (I18N_STRUCTURE.md)
- [x] Progress tracker ready (TASK_SUMMARY.md)
- [x] Session logging established
- [x] Guidelines reviewed
- [x] Testing strategy defined
- [x] Risk assessment complete
- [x] Timeline estimated

---

## 🚀 Ready to Start!

All planning is complete. The task is well-defined, documented, and ready for implementation.

**First Development Session:**

- Focus: Subtask 1 - Wizard State Management
- Estimated Time: 2-3 hours
- Key Deliverable: Working Zustand store with tests

**Remember:**

- Follow I18N_GUIDELINES.md strictly (plain strings!)
- Create accessibility tests for every component
- Update TASK_SUMMARY.md after completion
- Create session log for the work
- Have fun building! 🎉

---

**END OF PLANNING DOCUMENT**
