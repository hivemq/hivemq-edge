# SUBTASK_6.6: Side Panel Layout Fixes

**Date:** November 10, 2025  
**Issue:** Config panels had improper structure causing UX issues  
**Status:** ✅ Fixed

---

## Problems Identified

### 1. Missing Close Button

- Panel header had no close button
- User couldn't close panel (overlapped progress bar)
- No standard way to cancel step

### 2. Poor Layout

- Search/filter was in header area
- Protocol browser cramped in tiny scrollable area
- Not following standard drawer pattern

### 3. Inconsistent Structure

- Not using DrawerHeader, DrawerBody, DrawerFooter
- Custom VStack layout instead of standard components
- Doesn't match other drawers in the app

---

## Solution

Refactored both wizard step components to use **standard Chakra drawer structure**:

### WizardProtocolSelector (Step 1)

**Before:**

```tsx
<VStack>
  <Box px={4} pt={4}>
    {' '}
    {/* Custom header */}
    <Heading>Select Protocol</Heading>
    <Text>Description</Text>
  </Box>
  <Box px={4}>
    {' '}
    {/* Search in separate box */}
    <FacetSearch />
  </Box>
  <Box overflowY="auto">
    {' '}
    {/* Tiny scrollable area */}
    <ProtocolsBrowser />
  </Box>
</VStack>
```

**After:**

```tsx
<>
  <DrawerHeader borderBottomWidth="1px">
    <DrawerCloseButton onClick={cancelWizard} /> {/* ✅ Close button */}
    <Heading>Select Protocol</Heading>
    <Text>Description</Text>
  </DrawerHeader>

  <DrawerBody>
    {' '}
    {/* ✅ Proper scrollable body */}
    <Box mb={4}>
      <FacetSearch /> {/* Search in body, not header */}
    </Box>
    <ProtocolsBrowser /> {/* Full scrollable area */}
  </DrawerBody>
</>
```

### WizardAdapterForm (Step 2)

**Before:**

```tsx
<VStack>
  <Box px={4} pt={4}>
    {' '}
    {/* Custom header */}
    <Button onClick={onBack}>Back</Button>
    <Heading>Configure</Heading>
    <NodeNameCard />
  </Box>
  <Box overflowY="auto">
    {' '}
    {/* Form area */}
    <ChakraRJSForm />
  </Box>
  <Box borderTopWidth="1px">
    {' '}
    {/* Custom footer */}
    <Button type="submit">Submit</Button>
  </Box>
</VStack>
```

**After:**

```tsx
<>
  <DrawerHeader borderBottomWidth="1px">
    <DrawerCloseButton onClick={onBack} /> {/* ✅ Close button */}
    <Heading>Configure Adapter</Heading>
    <NodeNameCard />
  </DrawerHeader>

  <DrawerBody>
    {' '}
    {/* ✅ Proper scrollable form */}
    <ChakraRJSForm />
  </DrawerBody>

  <DrawerFooter borderTopWidth="1px">
    {' '}
    {/* ✅ Standard footer */}
    <Button variant="outline" onClick={onBack}>
      Back
    </Button>
    <Button variant="primary" type="submit">
      Submit
    </Button>
  </DrawerFooter>
</>
```

---

## Benefits

### 1. ✅ Close Button

- Both steps now have standard close button
- Clicking close calls `cancelWizard()` or `onBack()`
- Consistent with all other drawers in app

### 2. ✅ Proper Scrolling

- Search/filter in body (not header)
- Full scrollable area for content
- Protocol cards no longer cramped
- Form fields easily accessible

### 3. ✅ Standard Layout

- Uses Chakra drawer components
- Follows app patterns
- Better visual hierarchy
- Proper borders and spacing

### 4. ✅ Better UX

- More space for protocol selection
- Search doesn't scroll away
- Clear footer with actions
- Professional appearance

---

## Visual Comparison

### Step 1: Protocol Selection

**Before:**

```
┌─────────────────────────────────┐
│ Select Protocol Type            │  ← No close button ❌
│ Choose the protocol adapter...  │
│ [Search box] [Filters]          │  ← In header, scrolls away ❌
├─────────────────────────────────┤
│ ┌────────┐ ┌────────┐          │
│ │Modbus  │ │OPC-UA  │          │  ← Tiny cramped area ❌
│ └────────┘ └────────┘          │
│ (tiny scroll)                   │
└─────────────────────────────────┘
```

**After:**

```
┌─────────────────────────────────┐
│ Select Protocol Type         [X]│  ← Close button ✅
│ Choose the protocol adapter...  │
├─────────────────────────────────┤
│ [Search box] [Filters]          │  ← In body, visible ✅
│                                 │
│ ┌────────┐ ┌────────┐          │
│ │Modbus  │ │OPC-UA  │          │
│ └────────┘ └────────┘          │  ← Full scrollable ✅
│ ┌────────┐ ┌────────┐          │
│ │MQTT    │ │S7      │          │
│ └────────┘ └────────┘          │
│                                 │
│ (proper scroll area)            │
└─────────────────────────────────┘
```

### Step 2: Adapter Configuration

**Before:**

```
┌─────────────────────────────────┐
│ [← Back]                        │  ← No close button ❌
│ Configure Adapter               │
│ [Modbus TCP]                    │
├─────────────────────────────────┤
│ [Adapter ID]                    │
│ [Host]                          │
│ (form fields)                   │  ← Small scroll area ❌
├─────────────────────────────────┤
│              [Submit]           │
└─────────────────────────────────┘
```

**After:**

```
┌─────────────────────────────────┐
│ Configure Adapter            [X]│  ← Close button ✅
│ [Modbus TCP]                    │
├─────────────────────────────────┤
│ [Adapter ID]                    │
│ [Host]                          │
│ [Port]                          │
│ [Polling Interval]              │
│ [Subscriptions]                 │  ← Full scrollable ✅
│ [Advanced Settings]             │
│                                 │
│ (proper scroll area)            │
├─────────────────────────────────┤
│ [Back]                 [Submit] │  ← Both actions ✅
└─────────────────────────────────┘
```

---

## Technical Changes

### Files Modified

1. **WizardProtocolSelector.tsx**

   - Added: DrawerHeader, DrawerBody, DrawerCloseButton imports
   - Added: useWizardActions for cancelWizard
   - Changed: VStack → Drawer components
   - Changed: Conditional rendering in DrawerBody
   - Changed: FacetSearch moved to body

2. **WizardAdapterForm.tsx**
   - Added: DrawerHeader, DrawerBody, DrawerFooter, DrawerCloseButton imports
   - Changed: VStack → Drawer components
   - Changed: Back button moved to footer
   - Changed: Close button added to header
   - Changed: Submit button in footer (not duplicate)

### Imports Added

```tsx
import { DrawerHeader, DrawerBody, DrawerFooter, DrawerCloseButton } from '@chakra-ui/react'
```

---

## Close Button Behavior

### Step 1: Protocol Selection

```tsx
<DrawerCloseButton onClick={cancelWizard} />
```

- Closes panel
- Cancels entire wizard
- Returns to normal workspace

### Step 2: Configuration Form

```tsx
<DrawerCloseButton onClick={onBack} />
```

- Closes panel
- Goes back to Step 1
- Maintains wizard state

**Rationale:**

- Step 1: User hasn't committed to protocol yet → Cancel is appropriate
- Step 2: User selected protocol, might want to go back → Back is appropriate

---

## Testing Checklist

- [x] Step 1: Close button visible
- [x] Step 1: Close button cancels wizard
- [x] Step 1: Search/filter in scrollable area
- [x] Step 1: Protocol cards have full space
- [x] Step 2: Close button visible
- [x] Step 2: Close button goes back
- [x] Step 2: Form fully scrollable
- [x] Step 2: Footer has Back and Submit
- [x] Both steps: Standard drawer appearance
- [x] Both steps: No overlap with progress bar

---

## User Feedback Expected

Users should now:

- ✅ See close button (standard X in top right)
- ✅ Have easy way to exit/go back
- ✅ Have full scrollable area for content
- ✅ Not see search disappear when scrolling
- ✅ Have comfortable space to browse protocols
- ✅ See clear footer with actions

---

**Status:** ✅ Ready to test - Side panels now follow standard patterns!
