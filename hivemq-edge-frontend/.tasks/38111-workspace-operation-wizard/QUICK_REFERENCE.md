# Quick Reference: Task 38111 Workspace Wizard

**Status:** 🎯 Planning Complete - Ready for Development

---

## 🚀 Start Here

**Next Action:** Subtask 1 - Wizard State Management

**File to Create:** `src/modules/Workspace/hooks/useWizardStore.ts`

---

## 📚 Essential Documents

| Document              | Purpose                                    | Location                                   |
| --------------------- | ------------------------------------------ | ------------------------------------------ |
| **TASK_PLAN.md**      | Complete implementation plan (20 subtasks) | `.tasks/38111-workspace-operation-wizard/` |
| **ARCHITECTURE.md**   | Technical decisions and patterns           | `.tasks/38111-workspace-operation-wizard/` |
| **I18N_STRUCTURE.md** | Translation keys and usage                 | `.tasks/38111-workspace-operation-wizard/` |
| **TASK_SUMMARY.md**   | Progress tracking                          | `.tasks/38111-workspace-operation-wizard/` |
| **Session Index**     | Work logs                                  | `.tasks-log/38111_00_SESSION_INDEX.md`     |

---

## ⚠️ Critical Rules

### i18n (NON-NEGOTIABLE)

❌ `t(\`workspace.wizard.\${type}.name\`)` **← NEVER!**

✅ `t('workspace.wizard.entityType.name', { context: type })` **← ALWAYS!**

### Testing (NON-NEGOTIABLE)

```typescript
// ✅ MUST be unskipped and passing
it('should be accessible', () => {
  cy.injectAxe()
  cy.mountWithProviders(<Component />)
  cy.checkAccessibility()  // NOT cy.checkA11y()
})

// ⏭️ MUST exist but skipped
it.skip('should render correctly', () => {
  // Test implementation
})
```

### Accessibility

❌ `<Select value={value}>` **← WRONG!**

✅ `<Select aria-label={t('key')} value={value}>` **← CORRECT!**

---

## 🏗️ Architecture at a Glance

### Four Components

1. **Trigger** → `CreateEntityButton` (in CanvasToolbar)
2. **Progress** → `WizardProgressBar` (React Flow Panel)
3. **Ghosts** → `GhostNode` (canvas preview)
4. **Config** → Drawer with form

### State: Zustand Store

```typescript
const { isActive, entityType, currentStep } = useWizardState()
const { startWizard, cancelWizard } = useWizardActions()
```

---

## 📊 Phase Strategy

**Phase 1:** Foundation + Adapter (Complete 1 flow end-to-end) ← **START HERE**

**Phase 2:** Other Entities (Bridge, Combiner, Asset Mapper, Group)

**Phase 3:** Integration Points (TAG, TOPIC FILTER, DATA MAPPING, DATA COMBINING)

**Phase 4:** Polish (Error handling, Keyboard, Docs)

---

## 🎯 Subtask 1 Checklist

- [ ] Define TypeScript interfaces
- [ ] Create Zustand store with devtools
- [ ] Implement core actions
  - [ ] startWizard
  - [ ] cancelWizard
  - [ ] nextStep
  - [ ] previousStep
  - [ ] completeWizard
- [ ] Create convenience hooks
  - [ ] useWizardState
  - [ ] useWizardActions
  - [ ] useWizardSelection
- [ ] Write accessibility test
- [ ] Update TASK_SUMMARY.md
- [ ] Create session log

---

## 🛠️ Commands Reference

### Testing

```bash
# Run component test
pnpm cypress:run:component --spec "path/to/Component.spec.cy.tsx"

# Run e2e test
pnpm cypress:run:e2e --spec "cypress/e2e/path/to/test.spec.cy.ts"
```

### Development

```bash
# Start dev server
pnpm dev

# Type check
pnpm type-check

# Lint
pnpm lint
```

---

## 📁 File Structure

```
src/modules/Workspace/components/wizard/
├── WizardOrchestrator.tsx              # Main coordinator
├── CreateEntityButton.tsx              # Trigger
├── entity-wizards/                     # Entity creation
│   ├── AdapterWizard.tsx               # Subtask 7
│   ├── BridgeWizard.tsx                # Subtask 8
│   ├── CombinerWizard.tsx              # Subtask 9
│   ├── AssetMapperWizard.tsx           # Subtask 10
│   └── GroupWizard.tsx                 # Subtask 11
├── integration-wizards/                # Integration points
│   ├── TagWizard.tsx                   # Subtask 12
│   ├── TopicFilterWizard.tsx           # Subtask 13
│   ├── DataMappingNorthWizard.tsx      # Subtask 14
│   ├── DataMappingSouthWizard.tsx      # Subtask 14
│   └── DataCombiningWizard.tsx         # Subtask 15
├── steps/                              # Reusable steps
│   ├── WizardProgressBar.tsx           # Subtask 4
│   ├── SelectionStep.tsx               # Subtask 17
│   └── ConfigurationStep.tsx           # Subtask 6
├── preview/                            # Ghost system
│   ├── GhostNode.tsx                   # Subtask 5
│   ├── GhostEdge.tsx                   # Subtask 5
│   └── GhostNodeRenderer.tsx           # Subtask 5
├── hooks/                              # State management
│   ├── useWizardStore.ts               # Subtask 1 ← START
│   ├── useWizardSelection.ts           # Subtask 17
│   └── useWizardKeyboard.ts            # Subtask 19
└── utils/                              # Utilities
    ├── wizardMetadata.ts               # Subtask 2
    ├── configurationPanelRouter.ts     # Subtask 6
    ├── selectionManager.ts             # Subtask 17
    └── wizardValidation.ts             # Subtask 18
```

---

## 💡 Tips

### Before Coding

- Read the full subtask description in TASK_PLAN.md
- Check ARCHITECTURE.md for patterns
- Reference I18N_STRUCTURE.md for keys

### While Coding

- Follow TypeScript strictly
- Add ARIA labels to all interactive elements
- Use plain string translation keys
- Create accessibility test first

### After Coding

- Run tests and verify passing
- Update TASK_SUMMARY.md
- Create session log
- Commit with clear message

---

## 🔗 Guidelines

- **I18n:** `.tasks/I18N_GUIDELINES.md`
- **Testing:** `.tasks/TESTING_GUIDELINES.md`
- **Reporting:** `.tasks/REPORTING_STRATEGY.md`
- **Design:** `.tasks/DESIGN_GUIDELINES.md`
- **Workspace:** `.tasks/WORKSPACE_TOPOLOGY.md`

---

## 📞 Quick Help

**Question:** How do I translate entity type names?

```typescript
t('workspace.wizard.entityType.name', { context: EntityType.ADAPTER })
```

**Question:** What test pattern to use?

```typescript
it('should be accessible', () => {
  cy.injectAxe()
  cy.mountWithProviders(<Component />)
  cy.checkAccessibility()
})

it.skip('other tests...', () => {})
```

**Question:** Where to store wizard state?

```typescript
// In Zustand store
const useWizardStore = create<WizardStore>()(...)
```

**Question:** How to update progress?

- Update TASK_SUMMARY.md checkboxes
- Create session log in .tasks-log/
- Update percentage in summary header

---

**Ready to start? Let's build this! 🚀**
