# ARIA Attributes for Collapsible Toolbar - Reference Guide

**Task:** 37943-toolbar-search-filter  
**Pattern:** WAI-ARIA Disclosure (Show/Hide)

---

## Overview

This document provides the specific ARIA attributes required for the collapsible toolbar to meet WCAG 2.1 Level AA accessibility standards.

**Reference:** [WAI-ARIA Disclosure Pattern](https://www.w3.org/WAI/ARIA/apg/patterns/disclosure/)

---

## Required ARIA Attributes

### 1. Toggle Button

The button that expands/collapses the toolbar MUST have:

```tsx
<IconButton
  aria-label="Expand workspace toolbar" // or "Collapse workspace toolbar"
  aria-expanded={isExpanded} // "true" or "false"
  aria-controls="workspace-toolbar-content"
  onClick={toggleExpanded}
/>
```

**Attributes:**

- `aria-label`: Descriptive label for screen readers
- `aria-expanded`: Boolean indicating current state ("true" | "false")
- `aria-controls`: ID of the controlled content element

**Note:** The `aria-label` should change based on state:

- Collapsed: "Expand workspace toolbar" or "Show workspace tools"
- Expanded: "Collapse workspace toolbar" or "Hide workspace tools"

---

### 2. Content Container

The container with sections MUST have:

```tsx
<Box id="workspace-toolbar-content" role="group" aria-label="Workspace toolbar">
  {/* Sections */}
</Box>
```

**Attributes:**

- `id`: Unique identifier matching `aria-controls` on button
- `role`: Either "group" or "region" (region preferred for major sections)
- `aria-label`: Descriptive name for the entire toolbar content

---

### 3. Section 1: Search & Filter

```tsx
<Box role="region" aria-label="Search and filter controls">
  <SearchEntities />
  <DrawerFilterToolbox />
</Box>
```

**Attributes:**

- `role="region"`: Identifies as a significant page section
- `aria-label`: Describes the section purpose

**Why region?** Sections with `role="region"` appear in screen reader landmark navigation, making them easier to find.

---

### 4. Section 2: Layout Controls

```tsx
<Box role="region" aria-label="Layout controls">
  <LayoutSelector />
  <ApplyLayoutButton />
  <LayoutPresetsManager />
  {/* Settings button */}
</Box>
```

**Attributes:**

- `role="region"`: Identifies as a significant page section
- `aria-label`: Describes the section purpose

---

### 5. Panel (React Flow)

The outer Panel component should have:

```tsx
<Panel position="top-left" data-testid="workspace-toolbar" role="complementary" aria-label="Workspace tools">
  {/* Content */}
</Panel>
```

**Attributes:**

- `role="complementary"`: Landmark role for supporting content
- `aria-label`: High-level description of panel purpose
- `data-testid`: For testing

---

## Complete Example

```tsx
import { useState } from 'react'
import { Box, IconButton, Icon, Divider } from '@chakra-ui/react'
import { ChevronRightIcon, ChevronLeftIcon } from '@chakra-ui/icons'
import Panel from '@/components/react-flow/Panel'

const WorkspaceToolbar = () => {
  const [isExpanded, setIsExpanded] = useState(false)

  return (
    <Panel position="top-left" data-testid="workspace-toolbar" role="complementary" aria-label="Workspace tools">
      {/* Collapsed State: Toggle Button */}
      {!isExpanded && (
        <IconButton
          aria-label="Expand workspace toolbar"
          aria-expanded="false"
          aria-controls="workspace-toolbar-content"
          icon={<Icon as={ChevronRightIcon} />}
          onClick={() => setIsExpanded(true)}
        />
      )}

      {/* Expanded State: Content */}
      {isExpanded && (
        <Box id="workspace-toolbar-content" role="group" aria-label="Workspace toolbar">
          {/* Section 1: Search & Filter */}
          <Box role="region" aria-label="Search and filter controls">
            <SearchEntities />
            <DrawerFilterToolbox />
          </Box>

          <Divider my={2} />

          {/* Section 2: Layout Controls */}
          <Box role="region" aria-label="Layout controls">
            <LayoutSelector />
            <ApplyLayoutButton />
            <LayoutPresetsManager />
            {/* Settings button */}
          </Box>

          {/* Collapse Button */}
          <IconButton
            aria-label="Collapse workspace toolbar"
            aria-expanded="true"
            aria-controls="workspace-toolbar-content"
            icon={<Icon as={ChevronLeftIcon} />}
            onClick={() => setIsExpanded(false)}
          />
        </Box>
      )}
    </Panel>
  )
}
```

---

## Testing ARIA Attributes

### Manual Testing

1. **Screen Reader Testing:**

   - Use VoiceOver (Mac) or NVDA (Windows)
   - Tab to toggle button
   - Verify state announcement ("expanded" or "collapsed")
   - Navigate to regions (use landmarks menu)
   - Verify region labels are announced

2. **Keyboard Testing:**
   - Tab: Navigate through toolbar
   - Enter/Space: Activate toggle button
   - Verify focus management

### Automated Testing (Cypress)

```tsx
it('should have correct ARIA attributes in collapsed state', () => {
  cy.mountWithProviders(<WorkspaceToolbar />)

  // Toggle button
  cy.get('button[aria-controls="workspace-toolbar-content"]')
    .should('have.attr', 'aria-expanded', 'false')
    .should('have.attr', 'aria-label')
    .and('match', /expand/i)

  // Content should not be visible
  cy.get('#workspace-toolbar-content').should('not.exist')
})

it('should have correct ARIA attributes in expanded state', () => {
  cy.mountWithProviders(<WorkspaceToolbar />)

  // Expand
  cy.get('button[aria-controls="workspace-toolbar-content"]').click()

  // Toggle button
  cy.get('button[aria-controls="workspace-toolbar-content"]')
    .should('have.attr', 'aria-expanded', 'true')
    .should('have.attr', 'aria-label')
    .and('match', /collapse/i)

  // Content exists
  cy.get('#workspace-toolbar-content').should('exist').should('have.attr', 'role', 'group')

  // Sections have proper roles
  cy.get('[role="region"]').should('have.length', 2)
  cy.get('[role="region"]').first().should('have.attr', 'aria-label', 'Search and filter controls')
  cy.get('[role="region"]').last().should('have.attr', 'aria-label', 'Layout controls')
})

it('should be accessible', () => {
  cy.injectAxe()
  cy.mountWithProviders(<WorkspaceToolbar />)

  // Test collapsed state
  cy.checkAccessibility()

  // Expand
  cy.get('button[aria-controls="workspace-toolbar-content"]').click()

  // Test expanded state
  cy.checkAccessibility()
})
```

---

## Common Mistakes to Avoid

### ❌ WRONG: Missing aria-expanded

```tsx
<IconButton aria-label="Toggle toolbar" onClick={toggle} />
```

**Problem:** Screen readers can't announce current state.

---

### ❌ WRONG: aria-expanded as boolean instead of string

```tsx
<IconButton
  aria-expanded={true} // ❌ Wrong type
/>
```

**Problem:** React will convert to string "true" but TypeScript will complain.

**Fix:**

```tsx
<IconButton
  aria-expanded={isExpanded ? 'true' : 'false'} // ✅ Correct
  // or
  aria-expanded={String(isExpanded)} // ✅ Also correct
/>
```

---

### ❌ WRONG: No aria-controls

```tsx
<IconButton aria-label="Toggle toolbar" aria-expanded="false" onClick={toggle} />
```

**Problem:** No relationship between button and content.

---

### ❌ WRONG: No id on content

```tsx
<Box>{/* Content */}</Box>
```

**Problem:** `aria-controls` can't point to anything.

---

### ❌ WRONG: Using aria-hidden instead of conditional rendering

```tsx
<Box aria-hidden={!isExpanded}>{/* Always rendered but hidden */}</Box>
```

**Problem:** Content is still in the DOM and can be reached by screen readers.

**Better:**

```tsx
{
  isExpanded && <Box id="workspace-toolbar-content">{/* Only rendered when expanded */}</Box>
}
```

---

## WCAG Success Criteria Met

| Criterion                    | Level | How We Meet It                   |
| ---------------------------- | ----- | -------------------------------- |
| 1.3.1 Info and Relationships | A     | Semantic HTML + ARIA roles       |
| 2.1.1 Keyboard               | A     | All controls keyboard accessible |
| 2.4.6 Headings and Labels    | AA    | Descriptive aria-labels          |
| 4.1.2 Name, Role, Value      | A     | Proper ARIA attributes           |

---

## Resources

- [WAI-ARIA Disclosure Pattern](https://www.w3.org/WAI/ARIA/apg/patterns/disclosure/)
- [ARIA: button role](https://developer.mozilla.org/en-US/docs/Web/Accessibility/ARIA/Roles/button_role)
- [ARIA: region role](https://developer.mozilla.org/en-US/docs/Web/Accessibility/ARIA/Roles/region_role)
- [aria-expanded](https://developer.mozilla.org/en-US/docs/Web/Accessibility/ARIA/Attributes/aria-expanded)
- [aria-controls](https://developer.mozilla.org/en-US/docs/Web/Accessibility/ARIA/Attributes/aria-controls)

---

**Created:** October 31, 2025  
**Last Updated:** October 31, 2025
