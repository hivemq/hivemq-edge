# CONVERSATION SUBTASK 2 - Code Completion UI Blocking Issue

**Task ID:** 38053  
**Task Name:** monaco-configuration  
**Date Started:** November 6, 2025  
**Status:** ✅ SOLVED - Commit Character Issue Fixed

---

## REAL Root Cause (Found via User Diagnostic)

**NOT z-index, NOT Drawer, NOT React Flow!**

**Actual Issue:** `acceptSuggestionOnCommitCharacter: true` was causing SPACE and `:` to commit suggestions when they shouldn't.

### User's Key Observations:

1. "works when there is something to complete" → Suggestions work fine
2. "typing SPACE doesn't work after 'type'" → SPACE was committing suggestion
3. "In Cypress, space shows dropdown" → Timing difference, same underlying issue
4. "Two quick spaces add . then clean remaining characters" → Snippet issue

All symptoms point to **commit character settings**, not UI/z-index issues!

---

## Problem Statement

**Symptoms:**

- Code completion triggers on typing
- Typing SPACE doesn't always show completion widget but blocks UI
- Results in unfriendly/laggy typing experience
- Works better in Cypress headed mode tests
- Issue occurs in production app with Chakra UI Drawer + overlay

**Context:**

- Editor embedded in Chakra UI Drawer
- Drawer has overlay mode enabled
- Likely z-index or focus/event handling conflict

---

## Root Cause Analysis

### Likely Culprits

#### 1. **Z-Index Conflicts** (Most Likely)

Monaco's suggest widget has a default z-index that may be lower than:

- Chakra Drawer overlay (`z-index: 1400` by default)
- Drawer content (`z-index: 1401`)
- Modal backdrop

**Result:** Widget renders but is hidden behind overlay, causing:

- Widget exists (blocking typing)
- Widget not visible (appears broken)

#### 2. **Focus Trapping**

Chakra Drawer uses focus trap to keep focus inside:

- Monaco tries to focus suggest widget
- Focus trap prevents it
- Event handlers still fire but UI doesn't update

#### 3. **Event Propagation Issues**

- Drawer overlay may capture keyboard events
- SPACE key might be handled by Drawer/overlay first
- Monaco doesn't receive clean event

#### 4. **React Portal Conflicts**

Monaco suggest widget uses DOM portal:

- Default portal target may be outside Drawer
- Gets blocked by overlay
- Works in tests because no overlay

#### 5. **Timing/Race Conditions**

- Monaco initialization vs Drawer animation
- Suggest widget positioning calculation during transition
- RAF (RequestAnimationFrame) timing issues

---

## Diagnostic Steps

### Step 1: Check Z-Index

Inspect suggest widget z-index vs Drawer:

```javascript
// In browser console
const suggestWidget = document.querySelector('.suggest-widget')
console.log('Suggest widget z-index:', window.getComputedStyle(suggestWidget).zIndex)

const drawer = document.querySelector('[role="dialog"]')
console.log('Drawer z-index:', window.getComputedStyle(drawer).zIndex)
```

### Step 2: Check DOM Hierarchy

```javascript
// Where is suggest widget mounted?
const suggestWidget = document.querySelector('.suggest-widget')
console.log('Widget parent:', suggestWidget?.parentElement)
console.log('Is inside drawer?', drawer?.contains(suggestWidget))
```

### Step 3: Monitor Events

```javascript
// Add event listener to see if SPACE is being captured
document.addEventListener(
  'keydown',
  (e) => {
    if (e.key === ' ') {
      console.log('SPACE pressed, target:', e.target, 'currentTarget:', e.currentTarget)
    }
  },
  true
) // Capture phase
```

---

## Proposed Solutions

### Solution 1: Increase Monaco Z-Index (Quick Fix)

**Why:** Monaco suggest widget needs to be above Drawer overlay

**Implementation:**

```typescript
// In CodeEditor.tsx or global CSS
const editorOptions = {
  ...baseOptions,
  // Force suggest widget above Chakra Drawer
  suggest: {
    ...baseOptions.suggest,
  }
}

// Add global CSS
.monaco-editor .suggest-widget {
  z-index: 1500 !important; /* Above Chakra Drawer (1400) */
}

.monaco-editor .parameter-hints-widget {
  z-index: 1500 !important;
}

.monaco-editor .monaco-hover {
  z-index: 1500 !important;
}
```

### Solution 2: Disable Focus Trap for Monaco (Medium)

**Why:** Focus trap may interfere with Monaco's internal focus management

**Implementation:**

```tsx
// In Drawer component
<Drawer
  isOpen={isOpen}
  placement="right"
  onClose={onClose}
  trapFocus={false}  // ← Disable focus trap
  blockScrollOnMount={false}  // May also help
>
```

### Solution 3: Custom Portal Target (Advanced)

**Why:** Mount Monaco widgets inside Drawer to avoid overlay issues

**Implementation:**

```typescript
// Create portal container inside Drawer
const MonacoPortalContainer = () => {
  const portalRef = useRef<HTMLDivElement>(null)

  useEffect(() => {
    if (portalRef.current) {
      // Tell Monaco to use this as portal target
      // This requires Monaco editor instance
      const editor = editorRef.current
      if (editor) {
        // Monaco doesn't have direct API for this
        // Need to use CSS or DOM manipulation
      }
    }
  }, [])

  return <div ref={portalRef} style={{ position: 'relative', zIndex: 1 }} />
}
```

### Solution 4: Adjust Suggest Trigger Settings (Configuration)

**Why:** Make suggestions more aggressive to work around timing issues

**Implementation:**

```typescript
const editorOptions = {
  ...baseOptions,
  quickSuggestions: {
    other: true,
    comments: false,
    strings: true,
  },
  quickSuggestionsDelay: 0, // Immediate
  suggestOnTriggerCharacters: true,
  acceptSuggestionOnCommitCharacter: true,
  acceptSuggestionOnEnter: 'on',
  suggestSelection: 'first',
  tabCompletion: 'on',
  wordBasedSuggestions: 'matchingDocuments',
  // Key ones for SPACE issue
  suggest: {
    showKeywords: true,
    showSnippets: true,
    showWords: true,
  },
}
```

### Solution 5: Event Handling Override (Advanced)

**Why:** Ensure Monaco gets clean keyboard events

**Implementation:**

```typescript
useEffect(() => {
  const editor = editorRef.current
  if (!editor) return

  // Ensure editor container gets events
  const editorDom = editor.getDomNode()
  if (!editorDom) return

  // Stop event propagation to Drawer
  const handleKeyDown = (e: KeyboardEvent) => {
    if (e.key === ' ' && e.target === editorDom) {
      e.stopPropagation()
      // Let Monaco handle it
    }
  }

  editorDom.addEventListener('keydown', handleKeyDown, { capture: true })

  return () => {
    editorDom.removeEventListener('keydown', handleKeyDown, { capture: true })
  }
}, [editorRef.current])
```

---

## Trial Plan

### Trial 1: Z-Index Fix (5 minutes)

1. Add global CSS for Monaco widgets
2. Test in Drawer
3. Check if suggest widget is now visible

**Expected:** Widget appears above overlay

### Trial 2: Disable Focus Trap (2 minutes)

1. Add `trapFocus={false}` to Drawer
2. Test typing with SPACE
3. Check if responsiveness improves

**Expected:** Better event handling

### Trial 3: Combine 1 + 2 (2 minutes)

1. Keep z-index CSS
2. Keep trapFocus={false}
3. Test together

**Expected:** Full functionality

### Trial 4: Adjust Timing (if needed)

1. Set `quickSuggestionsDelay: 0`
2. Add `suggestSelection: 'first'`
3. Test responsiveness

### Trial 5: Event Override (if still issues)

1. Add custom keydown handler
2. Stop propagation for SPACE
3. Test typing flow

---

## Testing Checklist

For each trial, test:

- [ ] Type normal characters - smooth?
- [ ] Type SPACE - suggest widget appears?
- [ ] Type SPACE - UI doesn't freeze?
- [ ] Arrow keys work in suggest widget?
- [ ] ESC dismisses suggest widget?
- [ ] TAB accepts suggestion?
- [ ] ENTER accepts suggestion?
- [ ] Click outside dismisses widget?
- [ ] Drawer still closes normally?
- [ ] Overlay still works?

---

## Implementation Order

1. **Start with Trial 1** (Z-Index) - Most likely fix
2. **Try Trial 2** (Focus Trap) - Common issue
3. **Combine if needed** (Trial 3)
4. **Only if still broken:** Try Trials 4-5

---

## Files to Modify

1. `CodeEditor.tsx` - Add editor options
2. Global CSS or `monacoConfig.ts` - Add z-index styles
3. Drawer component (where editor is used) - Add trapFocus={false}

---

## Expected Outcome

✅ Suggest widget visible when typing
✅ SPACE key shows suggestions without blocking
✅ Smooth typing experience
✅ No conflicts with Drawer/overlay
✅ Works same as in tests

---

## Implementation Results

### ✅ Fixes Applied

1. **Z-Index CSS Override** - `monaco/monaco-overlay.css`

   - All Monaco widgets forced to z-index: 1500
   - Above Chakra Drawer (1400) and Modal (1400)
   - Includes: suggest widget, parameter hints, hover, context menu, find widget

2. **Fixed Overflow Widgets** - `monacoConfig.ts`

   - Added `fixedOverflowWidgets: true`
   - Uses fixed positioning (better for modal contexts)

3. **Optimized Timing** - `monacoConfig.ts`

   - `quickSuggestionsDelay: 50` (reduce blocking)
   - `suggestSelection: 'first'` (auto-select)
   - Better suggestion timing

4. **Diagnostic Logging** - `CodeEditor.tsx`

   - Development mode logging
   - Reports drawer/modal context
   - Shows z-index values
   - Helps debug issues

5. **Drawer Test Suite** - `CodeEditor.Drawer.spec.cy.tsx`
   - 4 tests for drawer context
   - Verifies z-index hierarchy
   - Tests typing responsiveness
   - Focus trap scenarios

### Files Modified

- ✅ Created `monaco/monaco-overlay.css`
- ✅ Modified `CodeEditor.tsx`
- ✅ Modified `monacoConfig.ts`
- ✅ Created `CodeEditor.Drawer.spec.cy.tsx`
- ✅ Created `DRAWER_DEBUG_GUIDE.md`
- ✅ Created `SUBTASK_2_FIXES.md`

### Testing Instructions

1. Open app in dev mode with browser console
2. Navigate to editor in drawer
3. Type to trigger suggestions
4. Check for `[Monaco]` logs in console
5. Verify suggest widget visible above overlay
6. Test SPACE key - should not block

### Alternative Options (If Needed)

- Disable focus trap: `<Drawer trapFocus={false}>`
- Delay editor mount after animation
- Use Portal outside drawer

---

**Status:** Ready for testing
**Confidence:** High (z-index is most common issue)
**Next:** User testing and feedback
