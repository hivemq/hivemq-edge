# HiveMQ Edge Frontend - Design Guidelines

**Last Updated:** November 4, 2025

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
<Button colorScheme="blue">Save Changes</Button> // ❌ Wrong!
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
    <Button variant="primary">Use Existing</Button> {/* Primary action */}
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
  <ModalOverlay /> {/* No backdropFilter */}
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

## Card Component Pattern

**CRITICAL:** Use Chakra UI's `Card` component with proper semantic structure for panels, details views, and content sections with distinct header/body/footer regions.

### ✅ Correct Pattern: Card with Semantic Structure

```tsx
import { Card, CardHeader, CardBody, CardFooter, Heading, Text, Button } from '@chakra-ui/react'
;<Card size="sm">
  <CardHeader>
    <Heading size="sm">Title</Heading>
  </CardHeader>

  <CardBody>
    <Text>Content goes here</Text>
  </CardBody>

  <CardFooter>
    <Button>Action</Button>
  </CardFooter>
</Card>
```

### ❌ Incorrect Pattern: Manual Structure with VStack

```tsx
// DON'T manually create card-like structures
<VStack>
  <HStack>
    <Heading>Title</Heading>
  </HStack>
  <Box>Content</Box>
  <ButtonGroup>Actions</ButtonGroup>
</VStack>
```

### When to Use Card

**Use Card for:**

- Details panels (e.g., node details, configuration panels)
- Content sections with clear header/body/footer separation
- Dashboard widgets
- Info cards with actions
- Preview panels

**Don't use Card for:**

- Simple text groupings (use Box)
- List items (use ListItem)
- Table rows (use Tr)
- Navigation sections (use Box/VStack)

### Card Sizes

```tsx
<Card size="sm">   {/* Compact - for panels, sidebars */}
<Card size="md">   {/* Default - for main content */}
<Card size="lg">   {/* Large - for prominent sections */}
```

### Optional Sections

**CardHeader and CardFooter are optional:**

```tsx
// Body only - no header or footer
<Card>
  <CardBody>
    <Text>Simple content</Text>
  </CardBody>
</Card>

// Header and body only - no footer
<Card>
  <CardHeader>
    <Heading size="sm">Title</Heading>
  </CardHeader>
  <CardBody>
    <Text>Content</Text>
  </CardBody>
</Card>
```

### Benefits of Card Pattern

1. **Semantic HTML** - Proper content structure
2. **Consistent Styling** - Built-in spacing, borders, shadows
3. **Accessibility** - Screen reader friendly structure
4. **Theme Integration** - Respects theme customizations
5. **Responsive** - Adapts to different screen sizes

### Example: Details Panel

```tsx
const NetworkGraphDetailsPanel: FC<Props> = ({ node, onClose }) => (
  <Card size="sm">
    <CardHeader>
      <HStack justify="space-between">
        <Heading size="sm">Node Details</Heading>
        <IconButton icon={<CloseIcon />} onClick={onClose} />
      </HStack>
    </CardHeader>

    <CardBody>
      <VStack align="stretch" spacing={4}>
        <Badge>{node.type}</Badge>
        <Text>{node.label}</Text>
        {/* More content */}
      </VStack>
    </CardBody>

    <CardFooter>
      <ButtonGroup>
        <Button variant="outline">View Config</Button>
        <Button variant="primary">Apply Filter</Button>
      </ButtonGroup>
    </CardFooter>
  </Card>
)
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

---

## Form Controls Pattern

**CRITICAL:** Always wrap form inputs (Select, Input, Textarea, Checkbox, Radio, etc.) in `FormControl` with `FormLabel` for consistency, accessibility, and testability.

### ✅ Correct Pattern: FormControl + FormLabel

```tsx
import { FormControl, FormLabel, Input, Select } from '@chakra-ui/react'

<FormControl>
  <FormLabel>Username</FormLabel>
  <Input placeholder="Enter username" />
</FormControl>

<FormControl>
  <FormLabel>Start from:</FormLabel>
  <Select placeholder="Select an option">
    <option value="1">Option 1</option>
    <option value="2">Option 2</option>
  </Select>
</FormControl>

<FormControl>
  <FormLabel>Direction:</FormLabel>
  <RadioGroup value={value} onChange={setValue}>
    <Stack direction="row">
      <Radio value="1">Option 1</Radio>
      <Radio value="2">Option 2</Radio>
    </Stack>
  </RadioGroup>
</FormControl>
```

### ❌ Incorrect Pattern: Manual Labels

```tsx
// DON'T use separate Text/Box for labels
<Box>
  <Text fontSize="sm" fontWeight="medium" mb={2}>
    Username
  </Text>
  <Input placeholder="Enter username" />
</Box>

// DON'T use heading for labels
<VStack>
  <Heading size="sm">Start from:</Heading>
  <Select>...</Select>
</VStack>
```

### Benefits of FormControl Pattern

1. **Semantic HTML** - Proper `<label>` elements with `for` attribute
2. **Accessibility** - Screen readers associate labels with inputs
3. **Consistent Spacing** - Built-in Chakra spacing between label and input
4. **Testing** - Easy to select inputs by label text
   ```tsx
   // Test can use: screen.getByLabelText('Username')
   ```
5. **Validation States** - FormControl handles isInvalid, isRequired, isDisabled
6. **Helper Text** - FormHelperText and FormErrorMessage components

### With Helper Text and Error State

```tsx
<FormControl isInvalid={!!error} isRequired>
  <FormLabel>Email address</FormLabel>
  <Input type="email" />
  <FormHelperText>We'll never share your email.</FormHelperText>
  <FormErrorMessage>{error}</FormErrorMessage>
</FormControl>
```

### With chakra-react-select

```tsx
import { Select } from 'chakra-react-select'
;<FormControl>
  <FormLabel>Choose node:</FormLabel>
  <Select<NodeOption>
    placeholder="Select..."
    options={options}
    value={value}
    onChange={setValue}
    isClearable
    isSearchable
  />
</FormControl>
```

### Testing Benefits

**With FormControl + FormLabel:**

```tsx
// ✅ Consistent test pattern across all forms
const input = screen.getByLabelText('Username')
await userEvent.type(input, 'john')
```

**Without FormControl (manual labels):**

```tsx
// ❌ Brittle - must use data-testid or complex queries
const input = screen.getByTestId('username-input')
// or
const input = screen.getByPlaceholderText('Enter username')
```

### When to Use FormControl

**Always use for:**

- Text inputs (`Input`, `Textarea`)
- Selects (Chakra `Select`, `chakra-react-select`)
- Radio groups (`RadioGroup`)
- Checkbox groups
- Date pickers
- Number inputs
- Any user input field

**Don't use for:**

- Buttons (use directly)
- Display-only text
- Icons or badges
- Navigation elements

### Validation and States

```tsx
<FormControl isInvalid={!!error} isRequired isDisabled={isLoading}>
  <FormLabel>Required field</FormLabel>
  <Input />
  {error && <FormErrorMessage>{error}</FormErrorMessage>}
</FormControl>
```

**Props:**

- `isRequired` - Shows asterisk, sets aria-required
- `isInvalid` - Shows error state, sets aria-invalid
- `isDisabled` - Disables all child inputs
- `isReadOnly` - Makes inputs read-only

---

## Checklist for New Forms

When creating forms:

- [ ] All inputs wrapped in `FormControl`
- [ ] Each FormControl has a `FormLabel`
- [ ] Required fields use `isRequired` prop
- [ ] Validation uses `isInvalid` + `FormErrorMessage`
- [ ] Helper text uses `FormHelperText`
- [ ] No manual `Text` or `Box` used for labels
- [ ] Tests use `getByLabelText()` for inputs

---

**Last Updated:** November 4, 2025
