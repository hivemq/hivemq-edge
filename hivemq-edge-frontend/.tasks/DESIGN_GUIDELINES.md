# HiveMQ Edge Frontend - Design Guidelines

**Last Updated:** October 24, 2025

---

## UI Component Patterns

### Button Variants

**CRITICAL:** Always use Chakra UI's custom variants instead of `colorScheme` for primary actions.

#### ✅ Correct Pattern

```tsx
// Primary action button (main CTA)
<Button variant="primary">Save Changes</Button>

// Secondary action button
<Button variant="outline">Cancel</Button>

// Tertiary/subtle action
<Button variant="ghost">Close</Button>

// Destructive action
<Button variant="danger">Delete</Button>
```

#### ❌ Incorrect Pattern

```tsx
// DON'T use colorScheme for primary buttons
<Button colorScheme="blue">Save Changes</Button>  // ❌ Wrong!
```

#### Button Hierarchy in Forms/Modals

When presenting multiple actions, follow this hierarchy (left to right or based on importance):

1. **Tertiary** (`variant="ghost"`) - Cancel, Close, Back
2. **Secondary** (`variant="outline"`) - Alternative actions
3. **Primary** (`variant="primary"`) - Main/recommended action (rightmost position)

**Example:**
```tsx
<ModalFooter>
  <HStack spacing={3}>
    <Button variant="ghost">Cancel</Button>
    <Button variant="outline">Create New</Button>
    <Button variant="primary">Use Existing</Button>  {/* Primary action */}
  </HStack>
</ModalFooter>
```

---

## Why This Matters

1. **Consistency**: Using `variant="primary"` ensures all primary buttons follow the theme
2. **Theming**: Custom variants respect the application's theme configuration
3. **Maintainability**: Changes to primary button styling are centralized
4. **Accessibility**: Variants are configured with proper contrast ratios

---

## Modal Icons & Colors

### Information vs Alert/Warning

**CRITICAL:** Choose modal icons and colors based on the intent and severity of the message.

#### ✅ Information Modals (Helpful, Guiding)

Use when providing helpful information, suggestions, or non-blocking notifications:

```tsx
// Use blue color with informational icons
<LuLightbulb color={theme.colors.blue[500]} size={24} />  // Suggestion/tip
<LuInfo color={theme.colors.blue[500]} size={24} />       // General info
```

**When to use:**
- Suggesting alternatives or better options
- Providing helpful tips or recommendations
- Notifying about duplicates or existing items
- Guiding user decisions (not blocking them)

**Color:** `blue[500]` - Matches ChakraUI's "info" toast status

#### ⚠️ Warning Modals (Caution Required)

Use when user needs to be cautious but can proceed:

```tsx
// Use orange color with warning icons
<LuAlertTriangle color={theme.colors.orange[500]} size={24} />  // Warning
<LuAlertCircle color={theme.colors.orange[500]} size={24} />    // Caution
```

**When to use:**
- Actions that might have unexpected consequences
- Settings that could affect system behavior
- Non-critical issues that need attention

**Color:** `orange[500]` - Matches ChakraUI's "warning" toast status

#### 🛑 Error/Danger Modals (Destructive or Blocking)

Use for errors, destructive actions, or blocking issues:

```tsx
// Use red color with error/danger icons
<LuAlertOctagon color={theme.colors.red[500]} size={24} />  // Error/danger
<LuXCircle color={theme.colors.red[500]} size={24} />       // Blocking error
```

**When to use:**
- Destructive actions (delete, remove, permanently change)
- Critical errors that block user progress
- Actions that cannot be undone

**Color:** `red[500]` - Matches ChakraUI's "error" toast status

---

### Decision Guidelines

Ask yourself:

1. **Is this blocking the user?** → Red (error)
2. **Could this cause problems?** → Orange (warning)
3. **Is this helpful information?** → Blue (info)

**Example - Duplicate Combiner Modal:**
- **Wrong:** Orange triangle ❌ - Implies something is wrong/dangerous
- **Right:** Blue lightbulb ✅ - Helpful suggestion, user can still create new

The tone should match the severity. Don't alarm users unnecessarily.

---

## Modal Overlays

### When to Use Backdrop Blur

**CRITICAL:** Only blur the modal overlay when you want to focus attention exclusively on the modal content.

#### ❌ Do NOT Blur When Context Matters

If the modal references or interacts with underlying content, keep the overlay clear:

```tsx
// DON'T blur when showing related content below
<Modal isOpen={isOpen} onClose={onClose}>
  <ModalOverlay />  {/* No backdropFilter */}
  {/* Modal content */}
</Modal>
```

**Use cases for clear overlay:**
- Modal references an existing item on the canvas/page
- Animated transitions show underlying content (e.g., fitView to a node)
- User needs to identify or verify something below the modal
- Modal provides context about visible elements

**Example - Duplicate Combiner Modal:**
The canvas uses `fitView()` to animate and zoom to the existing combiner node. A blurred overlay would defeat this purpose by hiding the very thing we're trying to show the user.

#### ✅ DO Blur for Focus

Use backdrop blur when you want exclusive focus on the modal:

```tsx
// Blur when modal content needs full attention
<Modal isOpen={isOpen} onClose={onClose}>
  <ModalOverlay backdropFilter="blur(4px)" />
  {/* Complex form or critical decision */}
</Modal>
```

**Use cases for blurred overlay:**
- Complex forms requiring concentration
- Destructive confirmations where distraction is risky
- Critical decisions needing careful reading
- No relationship between modal and underlying content

---

### Modal Positioning

When showing underlying context, consider positioning the modal to maximize visibility:

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

This leaves more screen real estate visible for the underlying content.

---

## Why This Matters

1. **User Trust**: Over-alarming creates anxiety and alert fatigue
2. **Clarity**: Color and icon should immediately signal severity level
3. **Consistency**: Matches ChakraUI toast color conventions
4. **Accessibility**: Color + icon provides redundant signaling

---

## Custom Variants Available

Based on the codebase, these custom button variants are available:

- `primary` - Main call-to-action buttons
- `outline` - Secondary actions
- `ghost` - Tertiary/subtle actions  
- `danger` - Destructive actions (delete, remove, etc.)

**Note:** Standard Chakra variants like `solid`, `link` are also available but use custom variants when possible for consistency.

---

## Examples from Codebase

### ✅ Good Examples

**Login Form:**
```tsx
<Button variant="primary" type="submit">
  {t('login.action.submit')}
</Button>
```

**Bridge Editor:**
```tsx
<Button variant="primary" type="submit" form="bridge-form">
  {t('action.save')}
</Button>
```

**Modal Actions:**
```tsx
<Button variant="ghost" onClick={onClose}>Cancel</Button>
<Button variant="primary" onClick={onSubmit}>Confirm</Button>
```

---

## Checklist for New Components

When creating buttons in new components:

- [ ] Primary action uses `variant="primary"`
- [ ] Secondary actions use `variant="outline"` 
- [ ] Cancel/Close uses `variant="ghost"`
- [ ] Delete/Remove uses `variant="danger"`
- [ ] Button hierarchy follows left-to-right importance
- [ ] Primary button is positioned last (rightmost) in button groups
- [ ] No use of `colorScheme` for custom-styled buttons

---

## Related Files

This guideline affects components that use Chakra UI `<Button>` components, including:
- Modals and dialogs
- Forms and drawers
- Toolbars and action panels
- Page-level CTAs

---

## References

- Task 33168 - Duplicate Combiner Modal implementation
- Chakra UI theme configuration in `src/modules/Theme/`

