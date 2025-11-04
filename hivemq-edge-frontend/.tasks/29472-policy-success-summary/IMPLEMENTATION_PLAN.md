# Task 29472: Implementation Plan

**Task:** Policy Success Summary  
**Created:** November 3, 2025  
**Status:** Planning

---

## ⚠️ CRITICAL: Testing Requirements

**MANDATORY FOR EVERY COMPONENT (Per .tasks/AUTONOMY_TEMPLATE.md):**

1. **Component Test File** (`*.spec.cy.tsx`) MUST be created with each component
2. **Accessibility Tests** are MANDATORY for every component
3. **Tests MUST PASS** before declaring subtask complete
4. **Never declare complete without running tests**

**Test Command Pattern:**

```bash
pnpm cypress:run:component --spec "path/to/Component.spec.cy.tsx"
```

**Required Tests Per Component:**

- ✅ Rendering with various props
- ✅ User interactions (clicks, keyboard)
- ✅ Accessibility (`cy.checkA11y()`)
- ✅ ARIA labels and roles
- ✅ Keyboard navigation
- ✅ Edge cases

**E2E Tests:** Will be done in Subtask 9 (Integration & E2E Testing)

---

## Overview

Transform the simple success alert into a comprehensive summary that provides users with clear visibility into what will be published, including the policy overview, resource breakdown, and optional JSON view.

---

## Subtask Breakdown

### Subtask 1: Design & UX Planning 🎨

**Goal:** Design the user experience for the success summary

**Deliverables:**

1. Visual hierarchy and layout design
2. Information architecture (what to show and how)
3. JSON display strategy (collapsible, syntax-highlighted)
4. Consistency with existing error report pattern

**Design Considerations:**

**Layout Structure:**

```
Success Alert (keep existing green alert)
  └─ "Policy is valid and ready to publish"

Policy Overview Card
  ├─ Badge: [New] or [Update]
  ├─ Type: Data Policy / Behavior Policy
  ├─ ID: policy-id
  └─ Key Details:
     ├─ Data Policy: Topic Filters
     └─ Behavior Policy: Transitions

Resources Card (Accordion)
  ├─ Schemas (expandable)
  │  └─ List of schemas with status badges
  └─ Scripts (expandable)
     └─ List of scripts with status badges

JSON View (Optional, Collapsible)
  ├─ Collapse/Expand toggle
  └─ Syntax-highlighted code display
     ├─ Policy JSON
     └─ Copy button
```

**Status Badge Colors:**

- 🆕 New (Draft) → Blue badge (`blue.500`)
- 🔄 Update (Modified) → Orange badge (`orange.500`)
- ✅ Published → Green badge (`green.500`)

**JSON Display Strategy:**

- Use Monaco Editor (already in codebase) OR Chakra Code component
- Collapsible by default (don't overwhelm users)
- Read-only view with copy functionality
- Consider showing "simplified" JSON (omit internal fields)

**Accessibility:**

- Proper heading hierarchy
- ARIA labels for expandable sections
- Keyboard navigation support
- Screen reader announcements

**Files to Reference:**

- `.tasks/DESIGN_GUIDELINES.md` - Button variants, modal patterns
- `PolicyErrorReport.tsx` - Accordion pattern reference
- `src/extensions/datahub/components/controls/CodeEditor.tsx` - Code display patterns

---

### Subtask 2: Data Extraction Utilities 🔧

**Goal:** Create utility functions to extract and format data from the validation report

**Location:** `src/extensions/datahub/utils/policy-summary.utils.ts` (new file)

**IMPORTANT:** The report array contains one item per designer node, PLUS a final summary item with the complete policy validation. We extract from this FINAL item.

**Functions to Implement:**

```typescript
// Extract policy information from the FINAL summary item
export interface PolicySummary {
  id: string
  type: DataHubNodeType.DATA_POLICY | DataHubNodeType.BEHAVIOR_POLICY
  isNew: boolean // true = creating, false = updating
  topicFilters?: string[] // for data policies
  transitions?: string[] // for behavior policies
}

export function extractPolicySummary(
  report: DryRunResults<unknown, never>[],
  designerStatus: DesignerStatus
): PolicySummary | undefined {
  // Get the final summary item (last in array)
  const finalSummary = [...report].pop()
  if (!finalSummary?.data) return undefined

  // Extract from finalSummary.data (complete policy JSON)
  // ...
}

// Extract resource information from the FINAL summary item's resources array
export interface ResourceSummary {
  id: string
  version: number | ResourceWorkingVersion
  type: 'SCHEMA' | 'FUNCTION'
  isNew: boolean
  metadata: {
    schemaType?: 'JSON' | 'PROTOBUF'
    functionType?: string
  }
}

export function extractResourcesSummary(report: DryRunResults<unknown, never>[]): ResourceSummary[] {
  // Get the final summary item (last in array)
  const finalSummary = [...report].pop()
  if (!finalSummary?.resources) return []

  // Extract from finalSummary.resources (all schemas + scripts)
  // ...
}

// Format policy JSON for display - use the FINAL summary item
export interface PolicyPayload {
  policy: object
  resources: {
    schemas: object[]
    scripts: object[]
  }
}

export function extractPolicyPayload(report: DryRunResults<unknown, never>[]): PolicyPayload | undefined {
  // Get the final summary item (last in array)
  const finalSummary = [...report].pop()
  if (!finalSummary) return undefined

  return {
    policy: finalSummary.data, // Complete policy JSON
    resources: {
      schemas: finalSummary.resources?.filter((r) => r.node.type === DataHubNodeType.SCHEMA).map((r) => r.data) || [],
      scripts: finalSummary.resources?.filter((r) => r.node.type === DataHubNodeType.FUNCTION).map((r) => r.data) || [],
    },
  }
}

// Helper to group resources by type
export function groupResourcesByType(resources: ResourceSummary[]): {
  schemas: ResourceSummary[]
  scripts: ResourceSummary[]
}
```

**Key Implementation Notes:**

1. **Always use the final item:** `const finalSummary = [...report].pop()`
2. **Policy data is in:** `finalSummary.data`
3. **All resources are in:** `finalSummary.resources[]`
4. **Don't iterate per-node items** - they're for error reporting only

**Testing:**

- Unit tests for each utility function
- Test with various report structures
- Edge cases: empty resources, mixed versions

**Files to Create:**

- `src/extensions/datahub/utils/policy-summary.utils.ts`
- `src/extensions/datahub/utils/policy-summary.utils.spec.ts`

---

### Subtask 3: Component - Policy Overview Card 📋

**Goal:** Create a component to display policy summary information

**Location:** `src/extensions/datahub/components/helpers/PolicyOverview.tsx` (new file)

**Component Structure:**

```typescript
interface PolicyOverviewProps {
  summary: PolicySummary
}

export const PolicyOverview: FC<PolicyOverviewProps> = ({ summary }) => {
  // Display policy type, ID, status badge, key details
}
```

**Visual Elements:**

- Chakra `Card` with `CardHeader` and `CardBody`
- Status `Badge` (New/Update)
- Icon for policy type (reuse from `NodeIcon.tsx`)
- Key-value pairs for policy details
- Translated labels using i18n

**Styling:**

- Follow existing card patterns in codebase
- Use theme colors from design guidelines
- Responsive layout

**Testing:**

- Component test for rendering
- Test with Data Policy props
- Test with Behavior Policy props
- Accessibility tests (per guidelines)

**Files to Create:**

- `src/extensions/datahub/components/helpers/PolicyOverview.tsx`
- `src/extensions/datahub/components/helpers/PolicyOverview.spec.cy.tsx`

---

### Subtask 4: Component - Resources Breakdown 📦

**Goal:** Create an accordion component to display schemas and scripts

**Location:** `src/extensions/datahub/components/helpers/ResourcesBreakdown.tsx` (new file)

**Component Structure:**

```typescript
interface ResourcesBreakdownProps {
  resources: ResourceSummary[]
}

export const ResourcesBreakdown: FC<ResourcesBreakdownProps> = ({ resources }) => {
  const { schemas, scripts } = groupResourcesByType(resources)

  // Render accordion with:
  // - Schemas section (count badge)
  // - Scripts section (count badge)
  // Each item shows: name, version, status badge, type
}
```

**Visual Pattern:**

- Follow `PolicyErrorReport` accordion style
- Use Chakra `Accordion`, `AccordionItem`, `AccordionButton`, `AccordionPanel`
- Show count in section headers: "Schemas (2)", "Scripts (1)"
- Each resource as a list item with metadata
- Status badges per resource

**Empty State:**

- Show message when no resources: "No additional resources required"

**Testing:**

- Component test with various resource combinations
- Test empty state
- Test accordion expansion/collapse
- Accessibility tests

**Files to Create:**

- `src/extensions/datahub/components/helpers/ResourcesBreakdown.tsx`
- `src/extensions/datahub/components/helpers/ResourcesBreakdown.spec.cy.tsx`

---

### Subtask 5: Component - JSON Payload View (Optional) 💻

**Goal:** Create a collapsible JSON view with syntax highlighting

**Location:** `src/extensions/datahub/components/helpers/PolicyJsonView.tsx` (new file)

**Component Structure:**

```typescript
interface PolicyJsonViewProps {
  payload: PolicyPayload
}

export const PolicyJsonView: FC<PolicyJsonViewProps> = ({ payload }) => {
  const [isExpanded, setIsExpanded] = useState(false)

  // Render:
  // - Collapse/Expand button
  // - Syntax-highlighted JSON (when expanded)
  // - Copy to clipboard button
}
```

**Implementation Options:**

**Option A: Monaco Editor (Recommended)**

- Already used in codebase for code editing
- Full syntax highlighting
- Read-only mode
- Reference: `src/extensions/datahub/components/forms/CodeEditor.tsx`

**Option B: Chakra Code Component**

- Simpler, lightweight
- Use with `react-syntax-highlighter`
- Good for smaller JSON payloads

**Features:**

- Collapsed by default
- Toggle button with icon (ChevronDown/ChevronUp)
- Format JSON with proper indentation
- Copy button with success toast
- Max height with scroll for large payloads

**User-Friendly JSON:**

- Consider filtering out internal fields (`__typename`, etc.)
- Pretty-print with 2-space indentation
- Show in separate tabs: Policy | Schemas | Scripts

**Testing:**

- Component test for rendering
- Test expand/collapse
- Test copy functionality
- Accessibility (keyboard navigation)

**Files to Create:**

- `src/extensions/datahub/components/helpers/PolicyJsonView.tsx`
- `src/extensions/datahub/components/helpers/PolicyJsonView.spec.cy.tsx`

---

### Subtask 6: Enhanced PolicySummaryReport Component 🎯

**Goal:** Update PolicySummaryReport to use new components and display comprehensive summary

**Location:** `src/extensions/datahub/components/helpers/PolicySummaryReport.tsx` (existing file)

**Current Implementation:**

```typescript
// Simple alert with title and description
<Alert status={alertStatus}>
  <AlertIcon />
  <AlertTitle>...</AlertTitle>
  <AlertDescription>...</AlertDescription>
</Alert>
```

**Enhanced Implementation:**

```typescript
interface PolicySummaryReportProps {
  status: PolicyDryRunStatus
  report?: DryRunResults<unknown, never>[] // ADD THIS
  designerStatus?: DesignerStatus // ADD THIS
}

export const PolicySummaryReport: FC<PolicySummaryReportProps> = ({
  status,
  report,
  designerStatus
}) => {
  // Extract data using utilities
  const policySummary = extractPolicySummary(report, designerStatus)
  const resources = extractResourcesSummary(report)
  const payload = extractPolicyPayload(report)

  // Render based on status
  if (status !== PolicyDryRunStatus.SUCCESS) {
    return <SimpleAlert /> // Keep existing for non-success
  }

  return (
    <VStack spacing={4} align="stretch">
      <Alert status="success">...</Alert>

      {policySummary && <PolicyOverview summary={policySummary} />}

      {resources.length > 0 && <ResourcesBreakdown resources={resources} />}

      {payload && <PolicyJsonView payload={payload} />}
    </VStack>
  )
}
```

**Changes:**

- Add optional props for report and designerStatus
- Conditionally render detailed summary only on SUCCESS
- Maintain backward compatibility (existing props still work)
- Use VStack for vertical spacing

**Testing:**

- Update existing tests
- Add tests for success state with report
- Test without report (backward compatibility)
- Test each section rendering

**Files to Update:**

- `src/extensions/datahub/components/helpers/PolicySummaryReport.tsx`
- `src/extensions/datahub/components/helpers/PolicySummaryReport.spec.cy.tsx`

---

### Subtask 7: Update DryRunPanelController 🔌

**Goal:** Pass report data to PolicySummaryReport

**Location:** `src/extensions/datahub/components/controls/DryRunPanelController.tsx`

**Changes:**

```typescript
// Before:
<PolicySummaryReport status={status} />

// After:
const { status, nodes } = useDataHubDraftStore()
<PolicySummaryReport
  status={status}
  report={report}
  designerStatus={status}
/>
```

**Additional Considerations:**

- Ensure report is available from `usePolicyChecksStore`
- Designer status from `useDataHubDraftStore`
- No changes to drawer structure

**Testing:**

- Update component tests
- Test data flow from stores to component
- Integration test with full validation flow

**Files to Update:**

- `src/extensions/datahub/components/controls/DryRunPanelController.tsx`
- `src/extensions/datahub/components/controls/DryRunPanelController.spec.cy.tsx`

---

### Subtask 8: Internationalization (i18n) 🌍

**Goal:** Add translation keys for new UI elements

**Location:** `src/extensions/datahub/locales/en/translation.json`

**Keys to Add:**

```json
{
  "workspace": {
    "dryRun": {
      "report": {
        "success": {
          "details": {
            "header": "What will be published",
            "policy": {
              "title": "Policy Details",
              "id": "Policy ID",
              "type": "Type",
              "status_new": "New Policy",
              "status_update": "Update Existing Policy",
              "topicFilters": "Topic Filters",
              "transitions": "Transitions"
            },
            "resources": {
              "title": "Resources",
              "empty": "No additional resources required",
              "schemas": "Schemas",
              "schemas_count_one": "Schema ({{count}})",
              "schemas_count_other": "Schemas ({{count}})",
              "scripts": "Scripts",
              "scripts_count_one": "Script ({{count}})",
              "scripts_count_other": "Scripts ({{count}})",
              "version": "Version",
              "type": "Type",
              "status_new": "New",
              "status_update": "Update"
            },
            "json": {
              "title": "JSON Payload",
              "expand": "Show JSON",
              "collapse": "Hide JSON",
              "copy": "Copy to clipboard",
              "copied": "Copied to clipboard",
              "tabs": {
                "policy": "Policy",
                "schemas": "Schemas",
                "scripts": "Scripts"
              }
            }
          }
        }
      }
    }
  }
}
```

**Testing:**

- Verify all keys are used in components
- Test pluralization rules
- Check translation context switching

**Files to Update:**

- `src/extensions/datahub/locales/en/translation.json`

---

### Subtask 9: Integration & E2E Testing 🧪

**Goal:** Comprehensive testing of the feature

**Test Coverage:**

**Unit Tests:**

- ✅ Utility functions (subtask 2)
- ✅ Individual components (subtasks 3-5)
- ✅ Updated components (subtasks 6-7)

**Component Tests:**

- Full PolicySummaryReport with real data
- Different policy types (Data/Behavior)
- Various resource combinations
- Empty states

**Integration Tests:**

- Full validation flow → success summary display
- Click through from designer to published policy
- Error → fix → success flow

**Accessibility Tests (Mandatory per guidelines):**

- Screen reader announcements
- Keyboard navigation through summary
- Focus management in accordion
- ARIA labels and roles
- Color contrast ratios

**Test Data:**
Create mock reports in `__test-utils__`:

```typescript
// Mock validation reports
export const MOCK_SUCCESS_REPORT_DATA_POLICY = [...]
export const MOCK_SUCCESS_REPORT_BEHAVIOR_POLICY = [...]
export const MOCK_SUCCESS_REPORT_WITH_RESOURCES = [...]
export const MOCK_SUCCESS_REPORT_NO_RESOURCES = [...]
```

**Files to Create/Update:**

- `src/extensions/datahub/__test-utils__/mock-validation-reports.ts`
- Component test files (created in subtasks)
- Integration test: `cypress/e2e/datahub/policy-validation-success.spec.cy.ts`

---

### Subtask 10: Documentation & Polish ✨

**Goal:** Final documentation and code quality

**Deliverables:**

1. **Code Documentation:**

   - JSDoc comments for all utilities
   - Component prop documentation
   - Complex logic explanation

2. **User Documentation:**

   - Update `src/extensions/datahub/README.md`
   - Document new success summary feature
   - Include screenshots (if applicable)

3. **Task Documentation:**

   - Final `TASK_SUMMARY.md`
   - Lessons learned
   - Future improvements

4. **Code Quality:**

   - Run linter and fix issues
   - Type safety checks
   - Remove console.logs
   - Code review checklist

5. **Performance:**
   - Check bundle size impact
   - Lazy loading considerations
   - Memoization where needed

**Files to Update:**

- `src/extensions/datahub/README.md`
- `.tasks/29472-policy-success-summary/TASK_SUMMARY.md`
- Various code files (documentation comments)

---

## Dependencies Between Subtasks

```
Subtask 1 (Design) → All other subtasks
Subtask 2 (Utils) → Subtasks 3, 4, 5, 6
Subtask 3, 4, 5 (Components) → Subtask 6
Subtask 6 (Enhanced Report) → Subtask 7
Subtask 7 (Controller) → Subtask 9
Subtask 8 (i18n) → Can run in parallel with 3-7
Subtask 9 (Testing) → After 2-7
Subtask 10 (Documentation) → Final step
```

---

## Testing Strategy (Per Guidelines)

**CRITICAL RULE:** Never declare a subtask complete without running tests!

**For each subtask involving code changes:**

1. Write tests FIRST (TDD approach when possible)
2. Implement the feature
3. Run component tests: `pnpm cypress:run:component --spec "path/to/Component.spec.cy.tsx"`
4. Verify ALL tests pass
5. Check for accessibility issues
6. Only then declare subtask complete

**Test Commands:**

```bash
# Component tests
pnpm cypress:run:component --spec "src/extensions/datahub/components/helpers/*.spec.cy.tsx"

# E2E tests
pnpm cypress:run:e2e --spec "cypress/e2e/datahub/policy-validation-success.spec.cy.ts"

# Unit tests
pnpm vitest run src/extensions/datahub/utils/policy-summary.utils.spec.ts
```

---

## Success Criteria

**Feature Complete When:**

✅ Users see comprehensive policy overview on validation success  
✅ Resource breakdown clearly shows what will be created/modified  
✅ JSON view provides detailed payload information (optional, collapsible)  
✅ All components follow design guidelines (buttons, colors, patterns)  
✅ All accessibility tests pass  
✅ All unit/component/integration tests pass  
✅ i18n keys properly implemented  
✅ Documentation updated  
✅ Code reviewed and polished

---

## Timeline Estimate

- **Subtask 1:** 2-3 hours (design & planning)
- **Subtask 2:** 3-4 hours (utilities + tests)
- **Subtask 3:** 2-3 hours (component + tests)
- **Subtask 4:** 2-3 hours (component + tests)
- **Subtask 5:** 3-4 hours (component + tests)
- **Subtask 6:** 2-3 hours (update + tests)
- **Subtask 7:** 1-2 hours (integration)
- **Subtask 8:** 1-2 hours (translations)
- **Subtask 9:** 4-5 hours (comprehensive testing)
- **Subtask 10:** 2-3 hours (documentation)

**Total Estimate:** 22-32 hours

---

## Notes for Implementation

1. **Start with Subtask 1** - Get design approval before coding
2. **Build incrementally** - Each component can be developed and tested independently
3. **Test continuously** - Don't accumulate testing debt
4. **Consider performance** - Large JSON payloads need efficient rendering
5. **Mobile responsive** - Drawer appears on mobile, ensure good UX
6. **Accessibility first** - Per guidelines, this is mandatory
7. **Follow patterns** - Reuse existing patterns from PolicyErrorReport

---

## Future Enhancements (Out of Scope)

- Export summary as PDF
- Email summary to users
- Compare with previous version (for updates)
- Inline editing from summary view
- Resource dependency graph visualization
