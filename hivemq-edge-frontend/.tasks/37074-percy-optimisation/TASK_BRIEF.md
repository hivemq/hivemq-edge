# Task Brief: Percy Optimization Strategy

**Task ID:** 37074  
**Task Name:** percy-optimisation  
**Status:** Active  
**Created:** October 22, 2025

---

## Objective

Optimize Percy visual regression testing strategy to maximize UI coverage while minimizing the number of snapshots taken, staying within monthly token limits.

## Context

### Current Situation

- Percy is integrated with Cypress for visual regression testing
- Previously tested individual components, but this consumed too many tokens
- Old component-level Percy tests have been removed
- Currently have 6 Percy snapshots across E2E tests:
  - Login page
  - Onboarding page
  - Adapters page (2 snapshots)
  - Bridges page
  - Workspace page
- Percy configuration: Testing at 2 widths (375px mobile, 1280px desktop)
- Monthly token limit is constraining testing strategy

### Problem Statement

Need a strategy that:

1. Maximizes visual coverage of the UI
2. Minimizes the number of individual snapshots
3. Leverages existing E2E test infrastructure
4. Catches visual regressions effectively
5. Stays within Percy token budget

## Proposed Strategy

### 1. **E2E Test Leverage (Recommended Primary Approach)**

Use E2E tests as the foundation for Percy snapshots because:

- E2E tests already navigate through complex user journeys
- They set up realistic application states with proper data
- One E2E test can cover multiple components in context
- Each snapshot captures a complete, functional page

**Benefits:**

- High component density per snapshot
- Tests real user scenarios, not isolated components
- Reduced setup overhead
- Natural integration with existing test suite

### 2. **Strategic Snapshot Placement**

Instead of testing every page, focus on:

#### High-Value Pages (Must capture)

- **Dashboard/Home pages** - Most frequently viewed
- **Complex data visualization pages** - Workspace, Metrics, Pulse
- **Form-heavy pages** - Protocol Adapter creation, Bridge configuration
- **Data Hub Designer** - Most complex UI component
- **Main listing pages** - Adapters, Bridges, Mappings (capture various states)

#### State Variations (Selective capture)

For key pages, capture different UI states:

- Empty state (no data)
- Loaded state (with data)
- Error states (where critical)
- Modal/drawer interactions (for complex forms)
- Different data types (e.g., different adapter types)

### 3. **Snapshot Optimization Techniques**

#### A. Compound Snapshots

Take snapshots at key points in E2E flows that show multiple components:

```typescript
// Example: One test, multiple meaningful snapshots
it('should handle adapter creation flow', () => {
  // Initial listing page shows: nav, sidebar, table, empty state
  cy.percySnapshot('Adapters - Empty State')

  // Click "Add" - shows: listing + modal with protocol picker
  adapterPage.addNewAdapter.click()
  cy.percySnapshot('Adapters - Protocol Selection')

  // Select protocol - shows: complex form with all fields
  selectProtocol('OPC-UA')
  cy.percySnapshot('Adapters - OPC-UA Configuration Form')

  // After creation - shows: listing with data, success toast
  cy.percySnapshot('Adapters - With Data')
})
```

#### B. Width Optimization

Consider reducing snapshot widths:

- Current: 375px (mobile) + 1280px (desktop) = 2 snapshots per call
- Option 1: Test only desktop (1280px) if mobile is not critical
- Option 2: Use 768px (tablet) + 1280px for broader coverage
- **Recommendation:** Keep current setup if mobile layout differs significantly

#### C. Selective Tagging

Use Cypress tags to control which tests run Percy:

```typescript
it('should render', { tags: ['@percy'] }, () => {
  cy.percySnapshot('Page Name')
})
```

This allows running full E2E suite without Percy, then targeted Percy runs.

### 4. **Coverage Mapping**

Create a coverage matrix to ensure all major UI patterns are tested:

| UI Pattern           | Test Location      | Snapshot Name          |
| -------------------- | ------------------ | ---------------------- |
| Navigation + Sidebar | Login/Home         | Page: Login            |
| Empty State Lists    | Adapters (initial) | Page: Adapters - Empty |
| Data Tables          | Bridges            | Page: Bridges          |
| Complex Forms        | Adapter Creation   | Adapters - OPC-UA Form |
| Visual Graphs        | Workspace          | Page: Workspace        |
| Modals/Drawers       | Throughout E2E     | Multiple               |
| Data Hub Designer    | Data Hub E2E       | DataHub - Designer     |
| Error States         | Validation tests   | Targeted snapshots     |

### 5. **Token Budget Management**

**Calculation Formula:**

```
Tokens per snapshot = widths × 1 token
Tokens per test run = number of snapshots × widths

Current: 6 snapshots × 2 widths = 12 tokens per run
```

**Recommended Target:** 15-20 strategic snapshots (30-40 tokens per run)

This gives comprehensive coverage while staying manageable:

- ~8 main page views (different pages/states)
- ~7 interaction states (modals, forms, different data)
- ~5 edge cases (errors, empty states, complex scenarios)

### 6. **Implementation Priority**

**Phase 1: Core Pages (Immediate)**

1. Login/Onboarding (already done ✓)
2. Workspace with complex visualization (already done ✓)
3. Adapters - full flow (partial ✓)
4. Bridges - full flow (already done ✓)
5. Data Hub Designer - key states

**Phase 2: Form Coverage** 6. Protocol adapter forms (different types) 7. Bridge configuration forms 8. Data Policy designer states 9. Mapping configuration

**Phase 3: Edge Cases** 10. Error states 11. Validation feedback 12. Different data scenarios 13. Permission states (if applicable)

## Key Principles

1. **One snapshot, multiple components** - Capture full pages, not isolated components
2. **Meaningful states** - Each snapshot should show a distinct UI state or interaction
3. **E2E flow integration** - Piggyback on existing test navigation
4. **Strategic, not exhaustive** - Focus on high-impact visual testing
5. **Document coverage** - Maintain a matrix of what's tested

## Success Metrics

- Visual coverage of all major UI sections
- Detection of visual regressions in key user journeys
- Staying within monthly token budget
- Minimal maintenance overhead
- Fast feedback on visual changes

## Resources

- Current E2E tests: `cypress/e2e/**/*.spec.cy.ts`
- Percy config: `.percy.js`
- Percy command: `pnpm cypress:percy`
- E2E test pages: `cypress/pages/`

---

## Next Steps

1. Audit existing E2E tests to identify best snapshot locations
2. Create coverage matrix document
3. Implement Phase 1 snapshots
4. Monitor token usage and adjust strategy
5. Document snapshot naming conventions
