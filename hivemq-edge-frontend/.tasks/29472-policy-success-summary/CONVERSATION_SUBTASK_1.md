# Subtask 1: Design & UX Planning

**Started:** November 3, 2025  
**Status:** In Progress  
**Estimated Time:** 2-3 hours

---

## Objective

Finalize the visual design and user experience for the policy success summary, ensuring:

- Clear information hierarchy
- Consistent with existing design patterns
- Accessibility considerations
- User-friendly approach to technical information

---

## Design Decisions

### 1. Overall Layout Strategy

**Progressive Disclosure Approach:**

- Start with simple success confirmation
- Expand to show policy details
- Further expand for resources
- Optionally show JSON for power users

**Rationale:** Users need different levels of detail. Casual users want quick confirmation, while power users want full transparency.

### 2. Component Hierarchy

```
┌─────────────────────────────────────────────────────────────┐
│ [SUCCESS ALERT]                                    ← Level 1│
│ Simple, reassuring message                                   │
└─────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────┐
│ [POLICY OVERVIEW CARD]                             ← Level 2│
│ Essential policy information at a glance                     │
│ - Status badge (New/Update)                                  │
│ - Policy type and ID                                         │
│ - Key characteristics                                        │
└─────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────┐
│ [RESOURCES BREAKDOWN]                              ← Level 3│
│ Collapsible sections for schemas and scripts                │
│ - Default: Collapsed (show counts)                           │
│ - Expanded: Full details per resource                        │
└─────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────┐
│ [JSON PAYLOAD VIEW]                                ← Level 4│
│ Advanced technical view (optional)                           │
│ - Default: Collapsed                                         │
│ - Expanded: Tabbed syntax-highlighted JSON                   │
└─────────────────────────────────────────────────────────────┘
```

### 3. Status Badge Design

**New (Draft):**

- Color: Blue (`colorScheme="blue"`)
- Icon: `LuPlus` (Lucide React)
- Label: "New"
- Semantic meaning: Creating a new resource

**Update (Modified):**

- Color: Orange (`colorScheme="orange"`)
- Icon: `LuRefreshCw` (Lucide React)
- Label: "Update"
- Semantic meaning: Modifying existing resource

**Implementation:**

```tsx
<Badge colorScheme={isNew ? 'blue' : 'orange'} display="inline-flex" alignItems="center" gap={1}>
  <Icon as={isNew ? LuPlus : LuRefreshCw} boxSize="10px" />
  {isNew ? t('...status_new') : t('...status_update')}
</Badge>
```

### 4. Policy Overview Card Design

**Visual Structure:**

```
┌─────────────────────────────────────────────────────────────┐
│ Policy Details                              [Card Header]    │
├─────────────────────────────────────────────────────────────┤
│                                                               │
│  [🆕 New]  Data Policy                    [Policy Icon] 📊  │
│                                                               │
│  Policy ID                                                    │
│  my-transformation-policy                                     │
│                                                               │
│  Topic Filters (2)                                            │
│  • devices/+/temperature                                      │
│  • devices/+/humidity                                         │
│                                                               │
└─────────────────────────────────────────────────────────────┘
```

**Key Elements:**

- Status badge prominent at top
- Policy type with icon for visual recognition
- Policy ID in monospace font
- Topic filters/transitions as bulleted list
- Proper spacing for readability

**Chakra Components:**

- `Card` with `size="sm"`
- `CardHeader` with `Heading size="sm"`
- `CardBody` with `VStack` layout
- `HStack` for horizontal alignment
- `List` and `ListItem` for filters

### 5. Resources Breakdown Design

**Accordion Pattern (Following PolicyErrorReport):**

```
┌─────────────────────────────────────────────────────────────┐
│ ▼ Schemas (2)                               [Expanded]       │
│ ┌─────────────────────────────────────────────────────────┐ │
│ │  📄  temperature-schema                                 │ │
│ │      [🆕 New]  v1  •  JSON                              │ │
│ │                                                         │ │
│ │  📄  humidity-schema                                    │ │
│ │      [🔄 Update]  v2  •  JSON                           │ │
│ └─────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────┐
│ ▶ Scripts (1)                               [Collapsed]      │
└─────────────────────────────────────────────────────────────┘
```

**Interaction:**

- Default: Both sections expanded (users want to see what's being created)
- Click header to collapse/expand
- Count badge shows number of items even when collapsed

**Resource Item Layout:**

```
[Icon] Resource Name
       [Status Badge] version • type
```

**Empty State:**

```
┌─────────────────────────────────────────────────────────────┐
│ No additional resources required                             │
│ This policy doesn't create or modify any schemas or scripts  │
└─────────────────────────────────────────────────────────────┘
```

### 6. JSON Payload View Design

**Collapsed State (Default):**

```
┌─────────────────────────────────────────────────────────────┐
│ ▶ View JSON Payload                     [Copy All] [Button] │
└─────────────────────────────────────────────────────────────┘
```

**Expanded State:**

```
┌─────────────────────────────────────────────────────────────┐
│ ▼ Hide JSON Payload                     [Copy All] [Button] │
├─────────────────────────────────────────────────────────────┤
│                                                               │
│  [Policy] [Schemas (2)] [Scripts (1)]         [Tabs]         │
│                                                               │
│  ┌─────────────────────────────────────────┐  [Copy] [Btn]  │
│  │ {                                       │                 │
│  │   "id": "my-policy",                    │  Syntax         │
│  │   "matching": {                         │  Highlighted    │
│  │     "topicFilters": [...]               │  JSON           │
│  │   }                                     │                 │
│  │ }                                       │                 │
│  └─────────────────────────────────────────┘                 │
│                                                               │
│  📋 Simplified for readability                                │
│                                                               │
└─────────────────────────────────────────────────────────────┘
```

**Implementation Choice: Chakra Code Component (Start Simple)**

**Rationale:**

- Lighter weight than Monaco
- Faster to implement
- Sufficient for read-only JSON display
- Can upgrade to Monaco later if needed

**Code Component Setup:**

```tsx
import { Code } from '@chakra-ui/react'

;<Code
  display="block"
  whiteSpace="pre"
  p={4}
  borderRadius="md"
  fontSize="xs"
  maxH="400px"
  overflowY="auto"
  bg="gray.50"
>
  {JSON.stringify(payload, null, 2)}
</Code>
```

### 7. Color Palette (Following Design Guidelines)

**Status Colors:**

- New/Draft: `blue.500` → Badge, borders
- Update/Modified: `orange.500` → Badge, borders
- Success: `green.500` → Alert background

**Text Colors:**

- Primary: `gray.800` → Headings, labels
- Secondary: `gray.600` → Values, descriptions
- Tertiary: `gray.500` → Helper text, icons

**Background Colors:**

- Card: `white` (default)
- Code: `gray.50`
- Hover: `gray.100`

**Border Colors:**

- Default: `gray.200`
- Focus: `blue.500`

### 8. Spacing & Typography

**Spacing Scale (Chakra):**

- Between cards: `spacing={4}` (16px)
- Within cards: `spacing={3}` (12px)
- Between elements: `spacing={2}` (8px)

**Typography:**

- Card headers: `<Heading size="sm">` (14px, semibold)
- Section titles: `<Text fontWeight="medium">` (12px, medium)
- Body text: `<Text fontSize="sm">` (14px, normal)
- Code/IDs: `<Text fontSize="xs" fontFamily="mono">` (12px, monospace)

### 9. Accessibility Requirements

**Keyboard Navigation:**

- Tab through all interactive elements
- Enter/Space to expand accordions
- Escape to close drawer
- Arrow keys within accordions

**Screen Reader Support:**

```tsx
// Accordion
<AccordionButton
  aria-label={`Schemas section, ${count} items`}
  aria-expanded={isExpanded}
>
  Schemas ({count})
</AccordionButton>

// Collapsible JSON
<Button
  onClick={toggle}
  aria-expanded={isExpanded}
  aria-controls="json-payload-content"
>
  {isExpanded ? 'Hide' : 'View'} JSON Payload
</Button>

// Copy button with feedback
<Button onClick={handleCopy} aria-label="Copy JSON to clipboard">
  <Icon as={copied ? LuCheck : LuCopy} />
  {copied ? 'Copied' : 'Copy'}
</Button>

// Live region for copy feedback
<Box
  role="status"
  aria-live="polite"
  aria-atomic="true"
  position="absolute"
  left="-10000px"
>
  {copied && "JSON copied to clipboard"}
</Box>
```

**Color Contrast:**

- All text meets WCAG AA (4.5:1 minimum)
- Status badges have sufficient contrast
- Focus indicators visible (Chakra default)

**ARIA Labels:**

- All icons have text labels or aria-labels
- Expandable sections have proper ARIA states
- Live regions for dynamic content updates

### 10. Responsive Behavior

**Desktop (> 768px):**

- Drawer width: `500px` (Chakra "sm")
- Full feature set
- Hover states visible
- Code display with scrolling

**Mobile (< 768px):**

- Full-width drawer
- Touch-friendly tap targets (min 44px)
- Larger spacing for readability
- May hide JSON view (too complex for mobile)

---

## Design Approval Checklist

Before implementation, confirm:

- [ ] Layout hierarchy makes sense
- [ ] Status badge colors and icons are appropriate
- [ ] Policy overview shows the right information
- [ ] Resources breakdown follows error report pattern
- [ ] JSON view approach is acceptable (Code component, not Monaco initially)
- [ ] Color palette matches design guidelines
- [ ] Accessibility requirements are comprehensive
- [ ] Responsive behavior is acceptable

---

## Implementation Notes

### Component Structure

```
PolicySummaryReport.tsx (Enhanced)
├─ Success Alert (existing, keep simple)
├─ PolicyOverview.tsx (NEW)
│  ├─ Status Badge
│  ├─ Policy Icon
│  ├─ Policy ID
│  └─ Key Details (topic filters/transitions)
├─ ResourcesBreakdown.tsx (NEW)
│  ├─ Accordion (allowMultiple, defaultIndex=[0,1])
│  ├─ Schemas Section
│  │  └─ Resource items with badges
│  └─ Scripts Section
│     └─ Resource items with badges
└─ PolicyJsonView.tsx (NEW)
   ├─ Collapsible trigger
   ├─ Tabs (Policy, Schemas, Scripts)
   └─ Code display with copy button
```

### Testing Requirements (Per Guidelines)

**For Each Component, Create:**

1. **Component Test File** (`*.spec.cy.tsx`)

   - Rendering tests
   - Interaction tests (expand/collapse, copy)
   - Different data scenarios
   - **Accessibility tests (MANDATORY)**

2. **Test Coverage:**

   - PolicyOverview.spec.cy.tsx
   - ResourcesBreakdown.spec.cy.tsx
   - PolicyJsonView.spec.cy.tsx
   - PolicySummaryReport.spec.cy.tsx (update existing)

3. **Accessibility Tests Each Component Must Have:**

   ```typescript
   it('should be accessible', () => {
     cy.checkA11y('[data-testid="component-root"]')
   })

   it('should support keyboard navigation', () => {
     // Tab through elements
     // Enter/Space to interact
     // Verify focus management
   })

   it('should have proper ARIA labels', () => {
     // Check aria-label, aria-expanded, role attributes
   })
   ```

### Mock Data Needed

Create in `src/extensions/datahub/__test-utils__/`:

```typescript
// mock-validation-reports.ts

export const MOCK_SUCCESS_REPORT_DATA_POLICY: DryRunResults[] = [
  // Per-node items
  { node: mockTopicFilterNode, data: {...}, error: undefined },
  { node: mockValidatorNode, data: {...}, error: undefined },
  { node: mockSchemaNode, data: mockSchema, error: undefined },

  // Final summary item
  {
    node: mockPolicyNode,
    data: mockDataPolicy,
    error: undefined,
    resources: [
      { node: mockSchemaNode, data: mockSchema, error: undefined }
    ]
  }
]

export const MOCK_SUCCESS_REPORT_BEHAVIOR_POLICY: DryRunResults[] = [...]
export const MOCK_SUCCESS_REPORT_NO_RESOURCES: DryRunResults[] = [...]
export const MOCK_SUCCESS_REPORT_MANY_RESOURCES: DryRunResults[] = [...]
```

---

## Design Questions for Review

### 1. JSON View Approach

**Option A: Start with Chakra Code (Recommended)**

- ✅ Simple implementation
- ✅ Lightweight
- ✅ Sufficient for MVP
- ❌ No advanced features (find, collapse)

**Option B: Use Monaco Editor**

- ✅ Rich features
- ✅ Better syntax highlighting
- ❌ Heavier bundle size
- ❌ More complex setup

**Recommendation:** Start with Code component, upgrade to Monaco if users request it.

### 2. Resources Accordion Default State

**Option A: Both sections expanded**

- ✅ Users see everything immediately
- ❌ More scrolling required

**Option B: Both sections collapsed**

- ✅ Cleaner initial view
- ❌ Users must click to see details

**Recommendation:** Expanded (users want to review before publishing)

### 3. JSON Simplification

Should we filter out internal fields from JSON?

**Fields to potentially omit:**

- `__typename`
- `createdAt`
- `lastUpdatedAt`
- Internal IDs

**Recommendation:** Show complete JSON initially, add simplification toggle later if needed.

---

## Next Steps

**After Design Approval:**

1. ✅ Create mock validation reports (`__test-utils__`)
2. ✅ Implement Subtask 2: Data extraction utilities
3. ✅ Implement Subtask 3: PolicyOverview component + tests
4. ✅ Implement Subtask 4: ResourcesBreakdown component + tests
5. ✅ Implement Subtask 5: PolicyJsonView component + tests
6. ✅ Integrate all into PolicySummaryReport

**Each step includes:**

- Implementation
- Component tests with accessibility tests
- Manual verification

---

## Status

**Status:** ✅ COMPLETE

**Design Approved:** November 3, 2025

**Decisions:**

- ✅ Layout hierarchy approved
- ✅ Status badge colors: Blue (new) / Orange (update) - **Made configurable**
- ✅ Start with Chakra Code component for JSON
- ✅ Resources accordion expanded by default
- ✅ Progressive disclosure approach confirmed

**Next:** Proceed to Subtask 2 (Data Extraction Utilities)
