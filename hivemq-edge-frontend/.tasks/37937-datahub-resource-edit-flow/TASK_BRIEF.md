# Task: 37937-datahub-resource-edit-flow

## Objective

Refactor the DataHub resource (schemas and scripts) creation and editing flow to separate resource management from policy configuration, simplifying the UI and improving maintainability.

## Context

The current DataHub designer integrates resource creation/editing directly into policy configuration through node panels (SchemaPanel and FunctionPanel). This approach has several issues:

1. **Complexity**: The RJSF-based editors in SchemaPanel and FunctionPanel have become overly complex, handling multiple states (creating new, loading existing, modifying versions) with intricate conditional logic
2. **User Confusion**: Users must create/edit resources within the policy designer context, which mixes two conceptually distinct operations
3. **Error Prone**: The current implementation uses complex state management with programmatic update flags to prevent cascading onChange events
4. **Inconsistent UX**: Resources can only be managed from within a policy, not as standalone entities

### Current Architecture

**Resource Creation/Editing (Current):**

- Location: Within policy designer, via SchemaPanel/FunctionPanel
- Triggered by: Selecting a schema/function node in the designer
- Complexity: High - handles creation, selection, version management, and type switching in one component
- State: Complex programmatic update refs to prevent cascading changes

**Node Configuration (Current):**

- Same panel handles both resource CRUD and node configuration
- Mixes resource-level concerns (schema definition, script source) with node-level concerns (which resource/version to use)

### Desired Architecture

**Resource Creation/Editing (New):**

- Location: DataHub main page, from Schema/Script tables
- Triggered by: "Create New" button or "Edit" action on resource row
- Complexity: Simple - focused only on resource CRUD
- State: Clean, dedicated to resource editing only

**Node Configuration (New):**

- Location: Within policy designer, via simplified node panels
- Purpose: Only select existing resource by name + version
- Complexity: Minimal - dropdown for name, dropdown for version
- State: Simple - just track selected resource reference

## Problem Statement

The current implementation violates separation of concerns by combining:

1. Resource lifecycle management (create, edit, version)
2. Policy node configuration (which resource to use)

This leads to:

- 200+ line panel components with complex conditional logic
- Difficult-to-maintain state management
- Poor user experience with unclear mental models
- Unnecessary coupling between resource management and policy design

## Goals

### Primary Goals

1. **Separate Resource Management**: Move schema/script creation and editing to the main DataHub page
2. **Simplify Node Configuration**: Reduce node panels to simple resource selection by name + version
3. **Improve UX**: Provide clear, distinct flows for "managing resources" vs "using resources in policies"
4. **Reduce Complexity**: Eliminate complex conditional logic from panel components

### Secondary Goals

1. **Reusability**: Create common components for both schema and script management (they follow similar patterns)
2. **Maintainability**: Clear separation of concerns makes code easier to understand and modify
3. **Consistency**: Use existing patterns (RJSF in side drawer, JSON-Schema validation)
4. **Publishing Flow**: Simplify policy publishing since resources will already exist

## Scope

### In Scope

**Phase 1: Resource Management UI**

- Add "Create New" buttons to Schema and Script tables
- Add "Edit" action to resource table rows
- Implement resource editing drawer (RJSF form)
- Support both schemas (JSON/Protobuf) and scripts (JavaScript)

**Phase 2: Simplified Node Configuration**

- Redesign SchemaPanel for simple resource selection
- Redesign FunctionPanel for simple resource selection
- Update SchemaNode and FunctionNode to display selection
- Remove complex resource creation logic from panels

**Phase 3: Publishing Flow Updates**

- Review ToolbarPublish to handle pre-existing resources
- Remove resource publishing logic (resources already published)
- Update dry-run validation to use resource references
- Ensure backward compatibility with existing policies

**Phase 4: Testing & Documentation**

- Component tests for new resource editors
- Component tests for simplified node panels
- E2E tests for complete resource → policy flow
- Update user documentation

### Out of Scope

- Changing the underlying API contracts
- Modifying policy types or node types
- Changing the overall designer architecture
- Bulk resource operations
- Resource versioning strategy changes

## Architecture Considerations

### Current Component Structure

```
DataHub Page
├─ DataHubListings (tabs)
   ├─ PolicyTable
   ├─ SchemaTable (read-only list)
   └─ ScriptTable (read-only list)

Policy Designer
├─ SchemaNode
│  └─ SchemaPanel (complex: create + edit + select + version)
└─ FunctionNode
   └─ FunctionPanel (complex: create + edit + select + version)
```

### Target Component Structure

```
DataHub Page
├─ DataHubListings (tabs)
   ├─ PolicyTable
   ├─ SchemaTable
   │  ├─ Create New Button → SchemaEditor (drawer)
   │  └─ Edit Action → SchemaEditor (drawer)
   └─ ScriptTable
      ├─ Create New Button → ScriptEditor (drawer)
      └─ Edit Action → ScriptEditor (drawer)

Policy Designer
├─ SchemaNode
│  └─ SchemaPanel (simple: select name + version only)
└─ FunctionNode
   └─ FunctionPanel (simple: select name + version only)
```

### Key Design Decisions

1. **Resource Editors Use Drawers**: Consistent with existing patterns (side drawer with RJSF form)
2. **Common Resource Editor Component**: Both schemas and scripts share similar structure, create reusable base component
3. **Node Panels Use Simple Selects**: Name dropdown + version dropdown, no embedded resource editing
4. **Resources Must Exist Before Use**: Nodes can only reference published resources
5. **Validation Still in Designer**: Dry-run validation happens during policy check, validates resource references exist

## User Stories

### Story 1: Data Architect Creates Schema

**As a** data architect  
**I want to** create and manage schemas independently from policies  
**So that I can** build a library of reusable data structures

**Acceptance Criteria:**

- Can click "Create New" on Schema table
- Drawer opens with schema editor (RJSF form)
- Can define schema name, type (JSON/Protobuf), version, definition
- Can save schema without creating a policy
- Schema appears in Schema table after creation

### Story 2: Developer Edits Script

**As a** developer  
**I want to** edit an existing script version  
**So that I can** fix bugs or add features

**Acceptance Criteria:**

- Can click "Edit" on a script row
- Drawer opens with script editor pre-populated
- Can modify script source code
- Can save as new version or update existing
- Changes reflected in Script table

### Story 3: Policy Designer Uses Schema

**As a** policy designer  
**I want to** add a schema node to my policy  
**So that I can** validate message structures

**Acceptance Criteria:**

- Can add schema node to policy canvas
- Panel shows dropdown of available schema names
- Selecting name loads available versions
- Can select specific version
- Node displays selected schema name + version
- Dry-run validates schema reference exists

### Story 4: Policy Publishing

**As a** policy designer  
**I want to** publish a policy that uses schemas and scripts  
**So that I can** activate data processing

**Acceptance Criteria:**

- All referenced resources must exist before policy check
- Dry-run validates all resource references
- Publishing only publishes the policy (not resources)
- Policy references resources by name + version
- Clear error if referenced resource doesn't exist

## Acceptance Criteria

### Functional Requirements

1. **Resource Management**

   - [ ] Can create new schemas from Schema table
   - [ ] Can edit existing schemas from Schema table
   - [ ] Can create new scripts from Script table
   - [ ] Can edit existing scripts from Script table
   - [ ] Resource editors use RJSF with JSON-Schema validation
   - [ ] Resource editors open in side drawer
   - [ ] Can save schemas/scripts independently

2. **Node Configuration**

   - [ ] Schema panel shows name dropdown (all schemas)
   - [ ] Schema panel shows version dropdown (versions for selected name)
   - [ ] Function panel shows name dropdown (all scripts)
   - [ ] Function panel shows version dropdown (versions for selected name)
   - [ ] Node displays selected resource name + version
   - [ ] No resource creation UI in node panels

3. **Publishing Flow**

   - [ ] Dry-run validates all resource references exist
   - [ ] Publish button disabled if resource references invalid
   - [ ] Policy publish only publishes policy (not resources)
   - [ ] Clear error messages for missing resources
   - [ ] Success message shows policy published

4. **Backward Compatibility**
   - [ ] Existing policies load correctly
   - [ ] Can edit existing policies
   - [ ] Can publish existing policies
   - [ ] Resource references preserved

### Non-Functional Requirements

1. **Performance**

   - Resource editors load in < 500ms
   - Node panels load in < 200ms
   - No impact on designer canvas performance

2. **Accessibility**

   - All components WCAG 2.1 AA compliant
   - Keyboard navigation works throughout
   - Screen reader announcements for state changes
   - Proper ARIA labels and roles

3. **Maintainability**
   - Clear separation of concerns
   - Reusable components for similar patterns
   - Comprehensive test coverage (>80%)
   - Clear documentation

## Implementation Guidelines

### Must Follow

1. **DATAHUB_ARCHITECTURE.md**: Understand state management, dry-run flow, publishing process
2. **DESIGN_GUIDELINES.md**: Use proper button variants, consistent styling
3. **TESTING_GUIDELINES.md**: Mandatory accessibility testing for all components
4. **I18N_GUIDELINES.md**: Proper translation key structure
5. **AUTONOMY_TEMPLATE.md**: Task reporting and progress tracking

### Technology Stack

- **UI Framework**: React 18 with TypeScript
- **Styling**: Chakra UI v2
- **Forms**: RJSF (React JSON Schema Form)
- **State**: Zustand stores (existing pattern)
- **Data Fetching**: TanStack Query (React Query)
- **Testing**: Vitest + Cypress component/E2E tests

### Code Locations

- **DataHub Main**: `src/extensions/datahub/components/pages/`
- **Schema Table**: `src/extensions/datahub/components/pages/SchemaTable.tsx`
- **Script Table**: `src/extensions/datahub/components/pages/ScriptTable.tsx`
- **Schema Node**: `src/extensions/datahub/designer/schema/SchemaNode.tsx`
- **Schema Panel**: `src/extensions/datahub/designer/schema/SchemaPanel.tsx`
- **Function Node**: `src/extensions/datahub/designer/script/FunctionNode.tsx`
- **Function Panel**: `src/extensions/datahub/designer/script/FunctionPanel.tsx`
- **Publishing**: `src/extensions/datahub/components/toolbar/ToolbarPublish.tsx`

## Success Metrics

### Quantitative

- Reduce SchemaPanel/FunctionPanel LOC by >50%
- Achieve >80% test coverage for new components
- Zero regression in existing policy functionality
- Resource creation time < 30 seconds

### Qualitative

- Developers report easier maintenance
- Users understand separation of resource vs policy
- Code reviews highlight improved clarity
- Reduced bug reports related to resource management

## Risks & Mitigations

### Risk 1: Breaking Existing Policies

**Impact**: High  
**Probability**: Medium  
**Mitigation**:

- Comprehensive E2E tests for backward compatibility
- Gradual rollout with feature flag
- Data migration script if needed

### Risk 2: User Confusion During Transition

**Impact**: Medium  
**Probability**: High  
**Mitigation**:

- Clear documentation and migration guide
- In-app guidance/tooltips
- Phased rollout with feedback collection

### Risk 3: Complex State Migration

**Impact**: Medium  
**Probability**: Low  
**Mitigation**:

- Keep existing stores, add new ones as needed
- Gradual refactoring approach
- Thorough testing of state transitions

## Timeline Estimate

**Total Duration**: 2-3 weeks

- **Phase 1**: Resource Management UI (4-5 days)
- **Phase 2**: Simplified Node Configuration (3-4 days)
- **Phase 3**: Publishing Flow Updates (2-3 days)
- **Phase 4**: Testing & Documentation (2-3 days)

## Dependencies

- None (self-contained refactoring)

## Related Tasks

- None currently
- Future: Resource import/export (out of scope)
- Future: Resource templates/library (out of scope)

## References

### Documentation

- `.tasks/DATAHUB_ARCHITECTURE.md` - Core architecture understanding
- `.tasks/DESIGN_GUIDELINES.md` - UI patterns and standards
- `.tasks/TESTING_GUIDELINES.md` - Test requirements
- `.tasks/I18N_GUIDELINES.md` - Translation patterns
- `.tasks/AUTONOMY_TEMPLATE.md` - Task reporting guidelines

### Existing Components

- `SchemaPanel.tsx` - Current complex implementation
- `FunctionPanel.tsx` - Current complex implementation
- `SchemaTable.tsx` - Target for "Create" button
- `ScriptTable.tsx` - Target for "Create" button
- `ToolbarPublish.tsx` - Publishing logic to update

### Related Code

- `useDataHubDraftStore.ts` - Designer state
- `usePolicyChecksStore.ts` - Validation state
- `useGetAllSchemas.ts` / `useGetAllScripts.ts` - Data fetching
- `datahubRJSFWidgets.tsx` - RJSF custom widgets

---

**Document Version**: 1.0  
**Created**: November 26, 2025  
**Last Updated**: November 26, 2025  
**Status**: Ready for Planning
