# Testing Patterns and Best Practices

## Cypress Test Organization

This project uses **two distinct types of Cypress tests**, each with specific purposes and conventions:

### 1. E2E Tests (End-to-End)

**Location:** `cypress/e2e/`

**Organization:** Tests are organized around pages or critical user paths/workflows

**API Mocking:** Uses `cy.intercept()` for API payload mocking

**Purpose:** Test complete user journeys and integration between multiple components

**Example structure:**
```
cypress/e2e/
├── adapters/
├── bridges/
├── datahub/
├── eventLog/
├── Login/
├── mappings/
└── pulse/
```

### 2. Component Tests

**Location:** Co-located with the component being tested (same directory)

**Naming Convention:** `{ComponentName}.spec.cy.tsx` - MUST match the component name exactly

**API Mocking:** Uses **MSW (Mock Service Worker)** for API payload mocking

**Purpose:** Test individual component behavior in isolation

**Example:**
```
src/modules/Workspace/components/filters/
├── FilterEntities.tsx
├── FilterEntities.spec.cy.tsx  ✅ Correct naming
├── FilterProtocol.tsx
├── FilterProtocol.spec.cy.tsx  ✅ Correct naming
```

## Key Differences Summary

| Aspect | E2E Tests | Component Tests |
|--------|-----------|-----------------|
| **Location** | `cypress/e2e/` | Co-located with component |
| **Naming** | Flexible | `{ComponentName}.spec.cy.tsx` |
| **API Mocking** | `cy.intercept()` | MSW |
| **Scope** | Full user flows | Individual components |
| **Organization** | By page/feature | By component hierarchy |

## Best Practices

### For Component Tests:
1. Always name test files to match the component: `FilterStatus.tsx` → `FilterStatus.spec.cy.tsx`
2. Use MSW handlers for API mocking
3. Test component in isolation
4. Use `cy.mountWithProviders()` for mounting with necessary context
5. Include accessibility tests with `cy.checkAccessibility()`

### For E2E Tests:
1. Organize by user flow or page
2. Use `cy.intercept()` for API mocking
3. Test realistic user scenarios
4. Focus on critical paths through the application

## Why This Matters

This distinction is important because:
- **Different mocking strategies** require different setup approaches
- **File location** helps determine the test's purpose and scope
- **Naming conventions** ensure tests are easy to find and maintain
- **Tooling** may rely on these conventions for test discovery and execution

---

**Last Updated:** October 23, 2025

