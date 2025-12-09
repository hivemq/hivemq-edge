# Task 37937: Quick Reference Guide

**Status**: 📋 Planning Complete  
**Progress**: 0/12 subtasks  
**Duration**: 2-3 weeks

---

## 30-Second Summary

**What**: Separate DataHub resource management from policy configuration  
**Why**: Current panels are too complex (200+ LOC), mixing resource CRUD with node config  
**How**:

1. Move resource create/edit to DataHub main page
2. Simplify node panels to resource selection only (name + version)
3. Remove resource publishing from policy publish flow

**Impact**:

- 50%+ LOC reduction in panels
- Clearer UX (manage resources, then use them)
- Easier maintenance

---

## Quick Commands

```bash
# Navigate to task directory
cd .tasks/37937-datahub-resource-edit-flow

# Create feature branch
git checkout -b feature/37937-datahub-resource-edit-flow

# Run component tests
pnpm cypress:run:component --spec "src/extensions/datahub/components/editors/**/*.spec.cy.tsx"

# Run E2E tests
pnpm cypress:run:e2e --spec "cypress/e2e/datahub/resources*.spec.cy.ts"

# Check test coverage
pnpm test:coverage
```

---

## Key Files

### Documentation (Read First)

- `TASK_BRIEF.md` - Context, goals, requirements
- `TASK_PLAN.md` - 12 subtasks, detailed implementation
- `TASK_SUMMARY.md` - Progress tracking
- `../../DATAHUB_ARCHITECTURE.md` - Architecture deep-dive

### Current Implementation (Understand)

- `src/extensions/datahub/designer/schema/SchemaPanel.tsx` - Complex (200 LOC)
- `src/extensions/datahub/designer/script/FunctionPanel.tsx` - Complex (200 LOC)
- `src/extensions/datahub/components/pages/SchemaTable.tsx` - Target for create/edit
- `src/extensions/datahub/components/pages/ScriptTable.tsx` - Target for create/edit

### Will Create (Phase 1)

- `src/extensions/datahub/components/editors/ResourceEditorDrawer.tsx`
- `src/extensions/datahub/components/editors/SchemaEditor.tsx`
- `src/extensions/datahub/components/editors/ScriptEditor.tsx`
- `src/extensions/datahub/hooks/useResourceEditor.ts`

### Will Create (Phase 2)

- `src/extensions/datahub/components/helpers/ResourceSelector.tsx`
- `src/extensions/datahub/designer/schema/SchemaPanelSimplified.tsx`
- `src/extensions/datahub/designer/script/FunctionPanelSimplified.tsx`

---

## Implementation Phases

### Phase 1: Resource Management UI (Days 1-5)

**Goal**: Create/edit resources from DataHub main page

✅ **Subtask 1.1** (Day 1): ResourceEditorDrawer base component  
✅ **Subtask 1.2** (Day 2): SchemaEditor implementation  
✅ **Subtask 1.3** (Day 3): ScriptEditor implementation  
✅ **Subtask 1.4** (Day 4): Integration with SchemaTable  
✅ **Subtask 1.5** (Day 5): Integration with ScriptTable

**Checkpoint**: Users can create/edit schemas and scripts from tables

---

### Phase 2: Simplified Node Configuration (Days 6-9)

**Goal**: Replace complex panels with simple resource selection

✅ **Subtask 2.1** (Day 6): ResourceSelector component  
✅ **Subtask 2.2** (Day 7): SchemaPanelSimplified (<100 LOC)  
✅ **Subtask 2.3** (Day 8): FunctionPanelSimplified (<100 LOC)  
✅ **Subtask 2.4** (Day 9): Node display updates

**Checkpoint**: Panels simplified, only select resources by name+version

---

### Phase 3: Publishing Flow Updates (Days 10-12)

**Goal**: Remove resource publishing from policy publish

✅ **Subtask 3.1** (Day 10): Dry-run validation for resource references  
✅ **Subtask 3.2** (Day 11): Publishing logic refactor  
✅ **Subtask 3.3** (Day 12): PolicySummaryReport updates

**Checkpoint**: Policy publish only publishes policy, not resources

---

### Phase 4: Testing & Documentation (Days 13-15)

**Goal**: Comprehensive coverage and docs

✅ **Subtask 4.1** (Day 13): Component test coverage (>80%)  
✅ **Subtask 4.2** (Day 14): E2E test scenarios  
✅ **Subtask 4.3** (Day 15): Documentation & migration guide

**Checkpoint**: Ready for PR submission

---

## Design Decisions

### Resource Editors

- **Location**: Side drawers (consistent with existing patterns)
- **Form**: RJSF with JSON-Schema validation
- **Reusability**: Common base component for schemas and scripts
- **Access**: "Create New" button + "Edit" action in tables

### Node Panels

- **Complexity**: Simple dropdowns only (name + version)
- **No Creation**: Cannot create resources from panels
- **Display**: Clear resource name, version badge, existence indicator
- **Validation**: Check references exist during dry-run

### Publishing Flow

- **Resources First**: Must exist before use in policy
- **Policy Only**: Publish button only publishes policy
- **References**: Policy references resources by name + version
- **Validation**: Dry-run validates all references exist

---

## Component Architecture

```
DataHub Page
├─ SchemaTable
│  ├─ [+ Create New] → SchemaEditor (drawer)
│  └─ [⋮ Edit] → SchemaEditor (drawer)
├─ ScriptTable
│  ├─ [+ Create New] → ScriptEditor (drawer)
│  └─ [⋮ Edit] → ScriptEditor (drawer)

Policy Designer
├─ SchemaNode
│  └─ SchemaPanelSimplified
│     └─ ResourceSelector
│        ├─ Name: [dropdown]
│        └─ Version: [dropdown]
├─ FunctionNode
│  └─ FunctionPanelSimplified
│     └─ ResourceSelector
│        ├─ Name: [dropdown]
│        └─ Version: [dropdown]
```

---

## Code Metrics

### LOC Reduction Targets

| Component      | Before | After | Reduction |
| -------------- | ------ | ----- | --------- |
| SchemaPanel    | 200    | <100  | >50%      |
| FunctionPanel  | 200    | <100  | >50%      |
| ToolbarPublish | 250    | 150   | 40%       |

### Test Coverage Targets

- New components: >80%
- Modified components: >80%
- E2E: All critical paths

---

## User Stories

### Story 1: Create Schema Independently

```
As a data architect
I want to create schemas from the Schema table
So that I can build a library before designing policies

Flow:
1. Navigate to DataHub → Schemas tab
2. Click "Create New Schema"
3. Fill form (name, type, definition)
4. Save → Schema appears in table
5. Later: Use in policy by selecting from dropdown
```

### Story 2: Simplified Policy Design

```
As a policy designer
I want to select existing schemas in my policy
So that I can focus on policy logic, not resource management

Flow:
1. Open policy designer
2. Add schema node
3. Select schema name from dropdown
4. Select version from dropdown
5. Node displays: "schema1 (v2) ✓"
6. Continue with policy design
```

### Story 3: Clear Error Handling

```
As a policy designer
I want clear errors if a resource is missing
So that I know what to fix

Flow:
1. Policy references "deleted_schema"
2. Run dry-run validation
3. Error: "Schema 'deleted_schema' v1 not found"
4. Message: "Create this schema from the Schema table"
5. Cannot publish until fixed
```

---

## Testing Strategy

### Component Tests

- All user interactions
- Accessibility (keyboard, screen readers)
- Error/loading states
- Edge cases

### E2E Tests

1. Create schema → Use in policy
2. Edit schema → Update policy version
3. Missing resource → Error handling
4. Complete flow: Resource → Policy → Publish

### Manual Testing Checklist

- [ ] Create schema from table
- [ ] Edit schema from table
- [ ] Create script from table
- [ ] Edit script from table
- [ ] Select schema in policy
- [ ] Select script in policy
- [ ] Dry-run with valid references
- [ ] Dry-run with missing references
- [ ] Publish policy
- [ ] Backward compat with existing policies

---

## Common Issues & Solutions

### Issue: Complex State Management

**Solution**: Reuse existing Zustand stores, add new ones only if necessary

### Issue: Backward Compatibility

**Solution**: Feature flag, comprehensive E2E tests, keep old code during transition

### Issue: User Confusion

**Solution**: Clear documentation, in-app tooltips, migration guide

### Issue: Resource Version Conflicts

**Solution**: Version dropdown shows all available, clear indication of latest

---

## Feature Flag Configuration

```typescript
// config/features.ts
export const FEATURES = {
  DATAHUB_SIMPLIFIED_RESOURCES: import.meta.env.VITE_FEATURE_SIMPLIFIED_RESOURCES === 'true',
}

// .env.local (development)
VITE_FEATURE_SIMPLIFIED_RESOURCES = true
```

---

## Rollout Plan

1. **Week 1-2**: Development (flag OFF)
2. **Week 3**: Internal testing (flag ON dev)
3. **Week 4**: Beta release (flag ON staging)
4. **Week 5**: Production release (flag ON prod)
5. **Week 6+**: Cleanup (remove old code)

---

## Success Criteria

### Must Have

- [ ] Can create/edit schemas from table
- [ ] Can create/edit scripts from table
- [ ] Node panels simplified (<100 LOC)
- [ ] Publishing only publishes policy
- [ ] Test coverage >80%
- [ ] No regression in existing policies

### Nice to Have

- [ ] Inline resource preview in selector
- [ ] Resource usage analytics
- [ ] Bulk resource operations
- [ ] Resource templates

---

## Resources

### Guidelines

- [DATAHUB_ARCHITECTURE.md](../../DATAHUB_ARCHITECTURE.md) - Architecture
- [DESIGN_GUIDELINES.md](../../DESIGN_GUIDELINES.md) - UI patterns
- [TESTING_GUIDELINES.md](../../TESTING_GUIDELINES.md) - Test requirements
- [I18N_GUIDELINES.md](../../I18N_GUIDELINES.md) - Translations
- [AUTONOMY_TEMPLATE.md](../../AUTONOMY_TEMPLATE.md) - Reporting

### Related Tasks

- Task 38111: Workspace Wizard (pattern reference)
- Task 37542: Code Coverage (testing patterns)

### API Documentation

- Schema API: `src/api/hooks/DataHubSchemasService/`
- Script API: `src/api/hooks/DataHubScriptsService/`
- Policy API: `src/api/hooks/DataHubPolicyService/`

---

## Contact & Support

**Task Owner**: TBD  
**Reviewers**: TBD  
**Documentation**: `.tasks/37937-datahub-resource-edit-flow/`

---

**Last Updated**: November 26, 2025  
**Version**: 1.0
