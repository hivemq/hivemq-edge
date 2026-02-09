# Widget-Level Validation Analysis: CodeEditor with errorSchema

**Date**: December 11, 2025  
**Context**: Using Monaco's `onDidChangeMarkers` to pass errorSchema via widget's `onChange`  
**Status**: ⚠️ Errors disappear when other fields change

---

## Current Implementation

### What You're Doing

In `CodeEditor.tsx`, you're using Monaco's marker change event to pass validation errors to RJSF:

```typescript
const handleEditorMount = (editor) => {
  const model = editor.getModel()

  if (monaco && model) {
    const syncMarkersToRjsf = () => {
      // Get Monaco's validation markers
      const markers = monaco.editor.getModelMarkers({ resource: model.uri })

      // Filter for errors only
      const errors = markers
        .filter((marker) => marker.severity === MarkerSeverity.Error)
        .map((m) => 'ERRORSCHEMA - ' + m.message)

      // Get current value
      const value = model.getValue()

      // Pass value + errorSchema to RJSF
      props.onChange(value, errors.length ? { __errors: errors } : undefined)
    }

    // Initial sync on mount
    syncMarkersToRjsf()

    // Sync when Monaco completes validation
    monaco.editor.onDidChangeMarkers((changedResources) => {
      const changed = changedResources.some((uri) => uri.toString() === model.uri.toString())
      if (!changed) return
      syncMarkersToRjsf()
    })
  }
}

const handleEditorChange = (value: string | undefined) => {
  isUserEditingRef.current = true

  // Only update the value; markers will adjust errorSchema
  props.onChange(value)

  setTimeout(() => {
    isUserEditingRef.current = false
  }, 100)
}
```

### What You're Observing

✅ **Works**: Errors display beneath the CodeEditor widget when you type invalid JavaScript

- Monaco validates → `onDidChangeMarkers` fires → `syncMarkersToRjsf()` called → errorSchema sent → errors display

❌ **Problem**: Errors disappear when you change a different widget (e.g., name field)

- Change name field → RJSF re-renders form → CodeEditor remounts → initial `syncMarkersToRjsf()` runs → markers not ready yet → errorSchema = undefined → errors cleared

---

## Root Cause Analysis

### The Error Lifecycle in RJSF

1. **Widget sends errorSchema via `onChange`**:

   ```typescript
   props.onChange(value, { __errors: ['error message'] })
   ```

2. **RJSF receives and stores errorSchema**:

   - RJSF stores this in its internal state
   - Associates it with this specific field (`sourceCode`)

3. **RJSF re-renders widget with errors**:

   - Passes errors back as `props.rawErrors`
   - Widget can display them inline

4. **But errorSchema is not persistent**:
   - RJSF expects errorSchema on EVERY `onChange` call
   - If widget calls `onChange(value, undefined)`, RJSF clears the errors
   - errorSchema doesn't persist across unrelated field changes

### Why Errors Disappear

**Sequence of events:**

```
1. User types invalid JS in CodeEditor
   ↓
2. Monaco validates (async, ~50-100ms)
   ↓
3. onDidChangeMarkers fires
   ↓
4. syncMarkersToRjsf() sends errorSchema
   props.onChange(value, { __errors: ['Cannot find name "x"'] })
   ↓
5. RJSF stores errorSchema, displays errors ✅
   ↓
6. User changes name field
   ↓
7. RJSF updates formData.name
   ↓
8. RJSF re-renders entire form (all widgets get new props)
   ↓
9. CodeEditor re-renders/remounts
   ↓
10. handleEditorMount runs again
    ↓
11. syncMarkersToRjsf() called immediately (initial sync)
    ↓
12. Problem: Monaco's markers not ready yet on remount!
    markers = [] (empty)
    ↓
13. props.onChange(value, undefined) // ← No errors!
    ↓
14. RJSF receives undefined errorSchema
    ↓
15. RJSF clears previous errors ❌
```

### The Core Issue

**Initial sync on mount happens BEFORE markers are ready:**

```typescript
const handleEditorMount = (editor) => {
  // ...setup code...

  if (monaco && model) {
    const syncMarkersToRjsf = () => {
      /* ... */
    }

    // ⚠️ PROBLEM: Called synchronously during mount
    syncMarkersToRjsf() // Markers might not be ready yet!

    // ✅ This works: Called when markers actually change
    monaco.editor.onDidChangeMarkers(() => {
      syncMarkersToRjsf()
    })
  }
}
```

**Timeline:**

- **T=0ms**: CodeEditor mounts → `syncMarkersToRjsf()` called → markers = [] → sends undefined
- **T=50-100ms**: Monaco validates code → markers ready → `onDidChangeMarkers` fires → sends errorSchema

But by then, RJSF has already received `undefined` and cleared the errors.

### Why `handleEditorChange` Doesn't Help

```typescript
const handleEditorChange = (value: string | undefined) => {
  // Only sends value, not errorSchema
  props.onChange(value)
}
```

When user types in CodeEditor:

- `handleEditorChange` sends new value
- BUT doesn't send errorSchema
- RJSF expects errorSchema with EVERY `onChange`
- Not sending errorSchema = implicitly sending undefined
- Result: errors cleared

---

## Why This Approach Struggles

### RJSF's Expectations

RJSF expects widgets to behave like this:

```typescript
// Every onChange call should include full state
const handleChange = (newValue) => {
  // Calculate errors synchronously
  const errors = validateSync(newValue)

  // Send both value AND errors together
  props.onChange(newValue, errors ? { __errors: [errors] } : undefined)
}
```

But Monaco validation is async:

```typescript
// Monaco's model - errors come later
const handleChange = (newValue) => {
  // Can't calculate errors synchronously!
  // Monaco validates in background (50-100ms)
  props.onChange(newValue /* ??? what to send here? */)
}

// Errors arrive later via event
monaco.editor.onDidChangeMarkers(() => {
  // By now, onChange was already called without errors
  // Need to call onChange AGAIN to send errors
})
```

### The Widget-Level Trap

Trying to manage async validation at the widget level creates issues:

1. **errorSchema is ephemeral** - not persisted by RJSF across re-renders
2. **Widget remounts frequently** - when ANY field changes
3. **Initial sync is unreliable** - markers not ready on mount
4. **Requires errorSchema on EVERY onChange** - can't just send it once

---

## Attempted Solutions & Why They Failed

### Attempt 1: Initial Sync on Mount

```typescript
// Initial sync
syncMarkersToRjsf()
```

**Problem**: Markers not ready yet on mount → sends undefined → clears errors

### Attempt 2: Only Sync on Marker Changes (Remove Initial Sync)

```typescript
// Don't call initial sync
// Only sync when markers change
monaco.editor.onDidChangeMarkers(() => {
  syncMarkersToRjsf()
})
```

**Problem**: If CodeEditor remounts with valid code, no marker change event fires → no sync → errors not sent

### Attempt 3: Send errorSchema in handleEditorChange

```typescript
const handleEditorChange = (value: string | undefined) => {
  // Try to send current errors
  const markers = monaco.editor.getModelMarkers({ resource: model.uri })
  const errors = extractErrors(markers)
  props.onChange(value, errors)
}
```

**Problem**:

- Markers lag behind user typing
- Shows "one step behind" errors
- Same async issue in different form

---

## Why Form-Level Validation is Correct

### The Right Approach: `customValidate` in Form

```typescript
// In ScriptEditor.tsx (form level)
const customValidate: CustomValidator<FunctionData> = (formData, errors) => {
  // Validate sourceCode synchronously
  const jsError = validateSourceCode(formData.sourceCode)

  if (jsError) {
    errors.sourceCode?.addError(jsError)
  }

  return errors
}

<Form customValidate={customValidate} ... />
```

**Why this is better:**

1. ✅ **RJSF calls it on every validation cycle** - not just when widget changes
2. ✅ **Runs when ANY field changes** - errors don't disappear
3. ✅ **Part of RJSF's validation lifecycle** - proper integration
4. ✅ **Errors persist correctly** - managed by RJSF, not widget state
5. ✅ **Works with form submission** - blocked if customValidate fails
6. ✅ **Consistent with other validations** - duplicate name check, required fields, etc.

### The Challenge: Synchronous Requirement

The ONLY issue with form-level validation is:

```typescript
// customValidate must be synchronous
const customValidate = (formData, errors) => {
  // Can't do this:
  // const result = await monacoValidate(formData.sourceCode) ❌

  // Must do this:
  const error = validateSync(formData.sourceCode) ✅

  if (error) {
    errors.sourceCode?.addError(error)
  }

  return errors
}
```

**Monaco validates asynchronously** → Need to make it synchronous or fast enough

---

## Solutions for Form-Level Async Validation

### Option A: TypeScript Compiler API (Synchronous)

Bundle TypeScript's compiler and run synchronous syntax validation:

```typescript
import ts from 'typescript'

const validateSync = (code: string): string | null => {
  const result = ts.transpileModule(code, {
    reportDiagnostics: true,
    compilerOptions: { target: ts.ScriptTarget.ES2015 },
  })

  if (result.diagnostics?.length) {
    return formatDiagnostic(result.diagnostics[0])
  }
  return null
}

// In customValidate (truly synchronous):
const error = validateSync(formData.sourceCode)
if (error) errors.sourceCode?.addError(error)
```

**Pros:**

- ✅ Truly synchronous - no async issues
- ✅ Fast (~10-20ms for syntax check)
- ✅ Works offline
- ✅ Reliable - no race conditions

**Cons:**

- ⚠️ Bundle size: ~500KB for TS compiler
- ⚠️ Less accurate than Monaco's full type checking

### Option B: Global Validation Cache + Re-trigger

Store validation results outside React, trigger re-validation when async completes:

```typescript
// validationCache.ts - Global singleton
class ValidationCache {
  private cache = new Map<string, string | null>()
  private onUpdate: (() => void) | null = null

  setCallback(cb: () => void) {
    this.onUpdate = cb
  }

  get(code: string): string | null {
    return this.cache.get(code) ?? null
  }

  async validate(code: string) {
    const result = await monacoValidate(code)
    this.cache.set(code, result)
    this.onUpdate?.() // Trigger form re-validation
  }
}

export const validationCache = new ValidationCache()

// In ScriptEditor:
useEffect(() => {
  validationCache.setCallback(() => {
    // Trigger RJSF re-validation
    setValidationTrigger((prev) => prev + 1)
  })
}, [])

// In customValidate:
const error = validationCache.get(formData.sourceCode)
if (error) errors.sourceCode?.addError(error)

// Trigger async validation (doesn't block)
validationCache.validate(formData.sourceCode)
```

**Pros:**

- ✅ Uses Monaco's full validation
- ✅ Cache persists across re-renders
- ✅ Accurate type checking

**Cons:**

- ⚠️ Still triggers form updates
- ⚠️ More complex state management
- ⚠️ Slight delay before errors appear

### Option C: Web Worker with Fast Cache

Run lightweight validation in a Web Worker you control:

```typescript
// validationWorker.ts
const worker = new Worker('validation-worker.js')
const cache = new Map<string, string | null>()

worker.onmessage = (e) => {
  cache.set(e.data.code, e.data.error)
  triggerFormValidation()
}

export const validateSync = (code: string): string | null => {
  const cached = cache.get(code)
  if (cached !== undefined) return cached

  // Trigger async validation
  worker.postMessage({ code })

  // Return null for now
  return null
}

// In customValidate:
const error = validateSync(formData.sourceCode)
if (error) errors.sourceCode?.addError(error)
```

**Pros:**

- ✅ You control validation timing
- ✅ Fast cache hits (instant)
- ✅ Doesn't block main thread

**Cons:**

- ⚠️ First validation might miss error
- ⚠️ Requires worker setup
- ⚠️ More complex architecture

---

## Recommendation

### For Your Situation

**Go with Option A: TypeScript Compiler API (Synchronous)**

**Why:**

1. Solves the sync/async problem completely
2. Simple implementation - no caching, no workers, no re-triggers
3. Bundle size acceptable for a code editor feature
4. Reliable and predictable
5. Works with RJSF's validation lifecycle naturally

**Implementation:**

```typescript
// validation/tsValidator.ts
import ts from 'typescript'

export const validateJavaScriptSync = (code: string): string | null => {
  try {
    const result = ts.transpileModule(code, {
      reportDiagnostics: true,
      compilerOptions: {
        target: ts.ScriptTarget.ES2015,
        module: ts.ModuleKind.ESNext,
        noEmit: true,
      },
    })

    const errors = result.diagnostics?.filter((d) => d.category === ts.DiagnosticCategory.Error)

    if (errors && errors.length > 0) {
      const error = errors[0]
      const line = error.file?.getLineAndCharacterOfPosition(error.start ?? 0)
      return `Line ${line?.line ?? 0}, Column ${line?.character ?? 0}: ${ts.flattenDiagnosticMessageText(
        error.messageText,
        '\n'
      )}`
    }

    return null
  } catch (e) {
    return 'Syntax error'
  }
}

// In ScriptEditor customValidate:
import { validateJavaScriptSync } from '@/validation/tsValidator'

const customValidate = (formData, errors) => {
  if (formData.sourceCode) {
    const error = validateJavaScriptSync(formData.sourceCode)
    if (error) {
      errors.sourceCode?.addError(error)
    }
  }
  return errors
}
```

---

## Why Widget-Level Validation Doesn't Work Here

### Summary of Issues

1. **errorSchema is not persistent** - cleared when widget remounts
2. **Widget remounts on any field change** - not just sourceCode changes
3. **Initial sync unreliable** - markers not ready on mount
4. **Monaco is inherently async** - can't make it synchronous at widget level
5. **RJSF expects errorSchema on every onChange** - can't send it just once
6. **Leads to "errors disappearing" bugs** - frustrating UX

### When Widget-Level Validation DOES Work

Widget-level validation with errorSchema works when:

- ✅ Validation is synchronous (e.g., regex, length checks)
- ✅ Widget doesn't remount frequently
- ✅ Errors can be calculated in `onChange` handler
- ✅ No external async dependencies

Example that WOULD work:

```typescript
const handleChange = (value: string) => {
  // Synchronous validation
  const errors = []
  if (value.length < 10) {
    errors.push('Must be at least 10 characters')
  }
  if (!/^[a-z]+$/.test(value)) {
    errors.push('Must contain only lowercase letters')
  }

  // Send value + errors together
  props.onChange(value, errors.length ? { __errors: errors } : undefined)
}
```

But Monaco validation doesn't fit this pattern.

---

## Conclusion

**Widget-level validation with errorSchema is the wrong pattern for async Monaco validation.**

**The right approach is form-level `customValidate` with synchronous validation.**

**To make Monaco validation synchronous, use TypeScript's compiler API directly.**

This resolves:

- ✅ Errors don't disappear on field changes
- ✅ Integrated with RJSF's validation lifecycle
- ✅ Consistent with other validations
- ✅ No async timing issues
- ✅ No complex state management
- ✅ Clean, maintainable code

The widget-level experiment was valuable for understanding RJSF's errorSchema mechanism, but it's not the right tool for this job.
