# Task 29472: Policy Success Summary

**Created:** November 3, 2025  
**Status:** Planning  
**Priority:** Medium

## Context

The Data Hub feature provides a visual designer for creating:

- **Data Policies**: Rules that apply data transformation on MQTT messages
- **Behavior Policies**: Rules that define behavior of MQTT clients

The designer serves two roles:

1. Graph-based editor for policies using configuration entities in an execution flow
2. Conversion between API-based definitions and frontend representations

## Current State

Policy publishing is a two-step process:

1. **Validation**: Checks policy validity and produces a report
2. **Publishing**: Uses the validation report to publish the policy

**Current behavior:**

- ❌ When errors exist → Detailed side panel shows errors node-by-node with context
- ✅ When policy is valid → Only a simple success message is shown

## Problem Statement

Users get minimal feedback when their policy is valid. They need:

- An overview of what will be created/modified
- Visibility into resources being created/modified
- Understanding of the impact before publishing

## Goals

Provide a comprehensive success summary when validation passes:

1. **Policy Overview**: Summary of the policy being created/modified
2. **Resource Overview**: List of resources being created/modified (schemas, scripts)
3. **JSON View** (optional): Show the JSON encoding in a user-friendly way

## Technical Context

### Key Files & Locations

**Designer Implementation:**

- `src/extensions/datahub/components/pages/PolicyEditor.tsx` - React Flow editor
- `src/extensions/datahub/components/pages/PolicyEditorLoader.tsx` - Loader wrapper

**Validation & Publishing:**

- `src/extensions/datahub/components/controls/DryRunPanelController.tsx` - Validation panel
- `src/extensions/datahub/components/toolbar/ToolbarPublish.tsx` - Publishing logic
- `src/extensions/datahub/hooks/usePolicyChecksStore.ts` - Validation state
- `src/extensions/datahub/hooks/usePolicyDryRun.ts` - Dry run hook

**Current Success Display:**

- `src/extensions/datahub/components/helpers/PolicySummaryReport.tsx` - Simple success alert
- `src/extensions/datahub/components/helpers/PolicyErrorReport.tsx` - Error accordion

**Building Blocks:**

- `src/extensions/datahub/designer/` - Subfolders for each policy element type
  - Each contains: React Flow node, side drawer, JSON Schema, UI Schema, utilities

**API & Schemas:**

- `src/extensions/datahub/api/` - API schemas, endpoints, React Query hooks

**Types:**

- `src/extensions/datahub/types.ts` - Core types including `PolicyDryRunStatus`, `DryRunResults`

## Acceptance Criteria

1. ✅ Create an actionable plan breaking down the task into reviewable subtasks
2. ✅ Design a user-friendly success summary (senior designer approach)
3. ✅ Carefully consider JSON payload display (avoid user-unfriendly raw JSON)
4. ✅ Document understanding of the DataHub designer and store architecture

## Success Metrics

- Users understand what will be published before clicking "Publish"
- Clear visibility into new vs. modified resources
- JSON representation is comprehensible (if shown)
- Consistent with existing error report UX patterns

## Related Documentation

- Design Guidelines: `.tasks/DESIGN_GUIDELINES.md`
- Testing Guidelines: `.tasks/TESTING_GUIDELINES.md`
- React Flow Best Practices: `.tasks/REACT_FLOW_BEST_PRACTICES.md`
- RJSF Guidelines: `.tasks/RJSF_GUIDELINES.md`
