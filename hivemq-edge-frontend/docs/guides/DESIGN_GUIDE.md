---
title: "Design Guide"
author: "Edge Frontend Team"
last_updated: "2026-02-16"
purpose: "UI component patterns and design conventions"
audience: "Developers implementing UI components"
maintained_at: "docs/guides/DESIGN_GUIDE.md"
---

# Design Guide

---

## Table of Contents

- [Button Variants](#button-variants)
- [Modal Patterns](#modal-patterns)
- [Color Usage](#color-usage)
- [Accessibility](#accessibility)

---

## Button Variants

### Use Custom Variants, Not colorScheme

**❌ Wrong:**
```tsx
<Button colorScheme="blue">Save Changes</Button>
```

**✅ Correct:**
```tsx
<Button variant="primary">Save Changes</Button>
```

### Available Variants

| Variant | Purpose | Example |
|---------|---------|---------|
| `primary` | Main call-to-action | Save, Submit, Confirm |
| `outline` | Secondary actions | Create New, Edit |
| `ghost` | Tertiary/subtle actions | Cancel, Close, Back |
| `danger` | Destructive actions | Delete, Remove |

### Button Hierarchy

**In forms/modals, order actions left-to-right by importance:**

```tsx
<ModalFooter>
  <HStack spacing={3}>
    <Button variant="ghost">Cancel</Button>        {/* Least important */}
    <Button variant="outline">Create New</Button>  {/* Alternative */}
    <Button variant="primary">Use Existing</Button> {/* Primary action, rightmost */}
  </HStack>
</ModalFooter>
```

**Rules:**
1. Tertiary (`ghost`) - Cancel, Close, Back (leftmost)
2. Secondary (`outline`) - Alternative actions (middle)
3. Primary (`primary`) - Main/recommended action (rightmost)

### Why This Matters

✅ **Consistency** - All primary buttons follow the theme
✅ **Theming** - Custom variants respect theme configuration
✅ **Maintainability** - Styling changes are centralized
✅ **Accessibility** - Variants have proper contrast ratios

---

## Modal Patterns

### Icons and Colors by Intent

Choose modal appearance based on severity and intent:

#### Information (Blue)

**Use when:** Providing helpful information, suggestions, non-blocking notifications

```tsx
import { LuLightbulb, LuInfo } from 'react-icons/lu'

<LuLightbulb color={theme.colors.blue[500]} size={24} />  // Suggestion/tip
<LuInfo color={theme.colors.blue[500]} size={24} />       // General info
```

**Examples:**
- Suggesting alternatives
- Notifying about duplicates
- Providing tips
- Guiding decisions

**Color:** `blue[500]` (matches Chakra's "info" toast)

#### Warning (Orange)

**Use when:** User needs caution but can proceed

```tsx
import { LuAlertTriangle, LuAlertCircle } from 'react-icons/lu'

<LuAlertTriangle color={theme.colors.orange[500]} size={24} />  // Warning
<LuAlertCircle color={theme.colors.orange[500]} size={24} />    // Caution
```

**Examples:**
- Actions with unexpected consequences
- Settings affecting system behavior
- Non-critical issues needing attention

**Color:** `orange[500]` (matches Chakra's "warning" toast)

#### Error/Danger (Red)

**Use when:** Errors, destructive actions, blocking issues

```tsx
import { LuAlertOctagon, LuXCircle } from 'react-icons/lu'

<LuAlertOctagon color={theme.colors.red[500]} size={24} />  // Error/danger
<LuXCircle color={theme.colors.red[500]} size={24} />       // Blocking error
```

**Examples:**
- Destructive actions (delete, remove)
- Critical errors blocking progress
- Actions that cannot be undone

**Color:** `red[500]` (matches Chakra's "error" toast)

### Decision Guidelines

Ask yourself:

1. **Is this blocking the user?** → Red (error)
2. **Could this cause problems?** → Orange (warning)
3. **Is this helpful information?** → Blue (info)

**Don't alarm users unnecessarily.** The tone should match the severity.

### Backdrop Blur

**Do NOT blur** when modal references underlying content:

```tsx
// No blur - user needs to see canvas below
<Modal isOpen={isOpen} onClose={onClose}>
  <ModalOverlay /> {/* No backdropFilter */}
  {/* Modal explains something visible on canvas */}
</Modal>
```

**Use cases for clear overlay:**
- Modal references visible content (for example, canvas node)
- Animated transitions show underlying elements
- User needs to verify something below modal

**DO blur** when modal needs exclusive focus:

```tsx
// Blur - complex form needs concentration
<Modal isOpen={isOpen} onClose={onClose}>
  <ModalOverlay backdropFilter="blur(4px)" />
  {/* Complex form or critical decision */}
</Modal>
```

**Use cases for blurred overlay:**
- Complex forms requiring concentration
- Destructive confirmations
- Critical decisions needing careful reading
- No relationship to underlying content

### Modal Positioning

**When showing underlying context, position modal to maximize visibility:**

```tsx
<ModalContent
  containerProps={{
    justifyContent: 'flex-start',  // Top of screen
    alignItems: 'flex-start',      // Left side
    pt: 20,  // Space from top
    pl: 20,  // Space from left
  }}
>
```

---

## Color Usage

### Semantic Colors

Use theme colors consistently:

| Color | Purpose | Theme Path |
|-------|---------|------------|
| Blue | Information, primary actions | `colors.blue[500]` |
| Orange | Warnings, caution | `colors.orange[500]` |
| Red | Errors, destructive actions | `colors.red[500]` |
| Green | Success, active states | `colors.green[500]` |
| Gray | Neutral, inactive states | `colors.gray[500]` |

### Status Colors

**Workspace status system:**

| Status | Color | Visual |
|--------|-------|--------|
| ACTIVE | Green | `colors.status.connected[500]` |
| ERROR | Red | `colors.status.error[500]` |
| INACTIVE | Gray | `colors.status.disconnected[500]` |

### Accessing Theme

```tsx
import { useTheme } from '@chakra-ui/react'

const Component = () => {
  const theme = useTheme()

  return (
    <Icon color={theme.colors.blue[500]} />
  )
}
```

---

## Accessibility

### Color Contrast

**All color combinations must meet WCAG AA standards:**
- Normal text: 4.5:1 contrast ratio
- Large text: 3:1 contrast ratio
- UI components: 3:1 contrast ratio

**Custom variants already meet these requirements.**

### Color + Icon Redundancy

**Never rely on color alone:**

```tsx
// ✅ Good - Color + icon
<LuAlertTriangle color={theme.colors.orange[500]} />
<Text>Warning: This action cannot be undone</Text>

// ❌ Bad - Color only
<Text color="orange.500">Warning: This action cannot be undone</Text>
```

### Focus Indicators

**All interactive elements must have visible focus:**

- Buttons: Focus ring (handled by Chakra)
- Inputs: Border change (handled by Chakra)
- Custom components: Add `_focus` styles

### Keyboard Navigation

**All modals and dialogs must support:**
- `Escape` to close
- `Tab` to navigate
- `Enter` to confirm (when appropriate)

**Chakra Modal handles this automatically.**

---

## Component Checklist

### Creating New Components

- [ ] Use `variant="primary"` for main actions
- [ ] Use `variant="outline"` for secondary actions
- [ ] Use `variant="ghost"` for cancel/close
- [ ] Use `variant="danger"` for destructive actions
- [ ] Button hierarchy follows left-to-right importance
- [ ] Primary button is rightmost in button groups
- [ ] Modal icon color matches intent (blue/orange/red)
- [ ] Modal backdrop blur only when needed
- [ ] All text has sufficient color contrast
- [ ] Interactive elements have focus indicators
- [ ] Color is not the only indicator of state

---

## Related Documentation

**Architecture:**
- [Workspace Architecture](../architecture/WORKSPACE_ARCHITECTURE.md)
- [DataHub Architecture](../architecture/DATAHUB_ARCHITECTURE.md)

**Guides:**
- [Testing Guide](./TESTING_GUIDE.md)
- [Cypress Guide](./CYPRESS_GUIDE.md)

**Technical:**
- [Technical Stack](../technical/TECHNICAL_STACK.md)
