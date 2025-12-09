# Task 38512: DataHub JavaScript Validation - Implementation Plan

**Created:** December 8, 2025  
**Status:** Ready for Implementation  
**Version:** 1.0

---

## Overview

Add JavaScript validation to DataHub ScriptEditor using Monaco Editor's existing validation capabilities, replacing the disabled `new Function()` approach with secure static analysis.

---

## Current State Analysis

### What Exists ✅

1. **Monaco Validation Module** (Task 38053)

   - Location: `src/extensions/datahub/components/forms/monaco/validation/`
   - Hook: `useJavaScriptValidation()`
   - Function: `validateJavaScript(monaco, code)`
   - Tests: 21 unit tests, 100% passing
   - Security: Uses TypeScript language service (Web Worker), no code execution

2. **Script Editor** (Task 37937)

   - Location: `src/extensions/datahub/components/editors/ScriptEditor.tsx`
   - Uses RJSF for form management
   - Has `customValidate` function for duplicate name checking
   - Has TODO comment (line 142) about replacing `new Function()` with Monaco validation

3. **Monaco Editor Widget**
   - Location: `src/extensions/datahub/components/forms/CodeEditor.tsx`
   - JavascriptEditor widget used in RJSF forms
   - Already configured with TypeScript validation

### What's Missing ❌

1. **JavaScript syntax validation in ScriptEditor**

   - Currently commented out due to security concerns
   - No real-time feedback for syntax errors
   - Users can save invalid JavaScript

2. **Test coverage for validation integration**
   - Skipped test at line 99 of `ScriptEditor.spec.cy.tsx`
   - No tests for validation error display

---

## Implementation Plan

### Phase 1: Core Integration (ScriptEditor)

#### 1.1 Add Validation Hook to ScriptEditor.tsx

**File:** `src/extensions/datahub/components/editors/ScriptEditor.tsx`

**Changes:**

1. Import validation hook and utilities:

```typescript
import { useJavaScriptValidation, formatValidationError } from '@datahub/components/forms/monaco/validation'
```

2. Add validation state (after line 51):

```typescript
const { validate: validateJS, isReady: isMonacoReady } = useJavaScriptValidation()
const [jsValidationError, setJsValidationError] = useState<string | null>(null)
```

3. Add debounced validation effect (after line 86):

```typescript
// Debounced JavaScript validation
useEffect(() => {
  const sourceCode = formData?.sourceCode
  if (!sourceCode || !isMonacoReady) {
    setJsValidationError(null)
    return
  }

  const timeoutId = setTimeout(async () => {
    const result = await validateJS(sourceCode)
    if (!result.isValid && result.errors.length > 0) {
      setJsValidationError(formatValidationError(result.errors[0]))
    } else {
      setJsValidationError(null)
    }
  }, 500) // 500ms debounce

  return () => clearTimeout(timeoutId)
}, [formData?.sourceCode, validateJS, isMonacoReady])
```

4. Update `customValidate` function (replace TODO at line 142):

```typescript
// Validate JavaScript syntax using Monaco (secure - no code execution)
if (jsValidationError) {
  errors.sourceCode?.addError(jsValidationError)
}
```

5. Remove old code:

- Delete commented-out `new Function()` validation (lines 138-145)

**Estimated LOC:** ~20 lines added, ~10 lines removed

---

#### 1.2 Update Tests

**File:** `src/extensions/datahub/components/editors/ScriptEditor.spec.cy.tsx`

**Changes:**

1. **Enable skipped test** (line 99):

   - Remove `.skip()`
   - Implement test to verify invalid JavaScript shows error
   - Test progression:
     1. Enter script name
     2. Enter invalid JavaScript in editor (e.g., `const x = {{{`)
     3. Wait for validation (cy.wait(600) to account for 500ms debounce)
     4. Verify error displays in `#root_sourceCode__error`
     5. Verify save button is disabled
     6. Fix syntax
     7. Verify error clears and save button enables

2. **Add new test cases:**
   - Valid JavaScript passes validation (no errors shown)
   - Warnings don't block save button
   - Monaco not ready gracefully degrades (no errors if Monaco unavailable)
   - Multiple rapid changes use debounce correctly
   - Validation error clears when code is fixed

**Estimated:** 5 new test cases, ~100 lines

---

### Phase 2: Documentation Updates

#### 2.1 Update Validation README

**File:** `src/extensions/datahub/components/forms/monaco/validation/README.md`

**Changes:**

1. Update RJSF example (line 179):

   - Replace generic example with actual ScriptEditor pattern
   - Add reference to ScriptEditor.tsx as implementation example
   - Clarify debounce timing recommendation (500ms)

2. Add "Real-world Example" section:
   - Link to ScriptEditor.tsx
   - Explain integration with RJSF customValidate
   - Show debounced validation pattern

**Estimated LOC:** ~30 lines added/updated

---

#### 2.2 Update Task Documentation

**File:** `.tasks/38512-datahub-js-validation/TASK_DESCRIPTION.md`

**Changes:**

- Add "Implementation Complete" section
- Document solution approach
- Reference key files

**File:** Create `.tasks/38512-datahub-js-validation/TASK_COMPLETE.md`

- Summary of changes
- Testing evidence
- Files modified list

---

## Testing Strategy

### Unit Tests (Vitest)

**Already Complete** ✅

- 21 unit tests in `javascriptValidator.spec.ts`
- 100% passing
- No additional unit tests needed

### Component Tests (Cypress)

**File:** `ScriptEditor.spec.cy.tsx`

| Test Case                | Purpose                                 | Assertions                            |
| ------------------------ | --------------------------------------- | ------------------------------------- |
| Invalid JS shows error   | Verify validation catches syntax errors | Error message displays, Save disabled |
| Valid JS passes          | Ensure valid code doesn't show errors   | No error message, Save enabled        |
| Validation clears on fix | Test error recovery                     | Error appears then disappears         |
| Debounce works           | Prevent excessive validation calls      | Only validates after 500ms pause      |
| Monaco not ready         | Graceful degradation                    | No errors if Monaco unavailable       |
| Warnings don't block     | Allow warnings through                  | Warning shown but Save enabled        |

**Expected Coverage:** >80% for modified code

---

## Design Decisions

### 1. Validation Scope

**Decision:** Validate only in ScriptEditor, not in FunctionPanelSimplified

**Rationale:**

- FunctionPanelSimplified is read-only (only selects existing scripts)
- Validation at source (ScriptEditor) prevents invalid scripts from being saved
- Simpler architecture (single validation point)
- No UX benefit to showing errors in designer when user can't edit there

**Alternative Considered:** Add read-only error display in designer

- **Rejected:** Adds complexity without clear user benefit

---

### 2. Debounce Timing

**Decision:** Fixed 500ms debounce

**Rationale:**

- Proven pattern from validation module README
- Balance between responsiveness and performance
- Typical user pause in typing
- Consistent with other real-time validation in codebase

**Alternative Considered:** Configurable debounce

- **Rejected:** No user requirement, adds unnecessary complexity

---

### 3. Error Display Strategy

**Decision:** Show first error only

**Rationale:**

- Consistent with RJSF patterns throughout codebase
- Monaco editor already shows all errors inline
- Prevents overwhelming user with error messages
- User fixes errors iteratively anyway

**Alternative Considered:** Show all errors

- **Rejected:** Clutters UI, Monaco already shows inline markers

---

### 4. Monaco Availability

**Decision:** Graceful degradation if Monaco not loaded

**Rationale:**

- Monaco loads asynchronously from CDN
- Better UX to allow form use than block on Monaco
- Validation is enhancement, not critical path
- isReady check prevents errors

**Implementation:** Check `isMonacoReady` before validation

---

## Edge Cases & Error Handling

### 1. Monaco CDN Failure

**Scenario:** Monaco fails to load from CDN

**Handling:**

- `isReady` remains false
- Validation is skipped
- User can still use form (TextArea fallback in CodeEditor)
- No validation errors shown

**Test:** "Monaco not ready" test case

---

### 2. Very Large Scripts

**Scenario:** User pastes 10,000+ line script

**Handling:**

- TypeScript service handles in Web Worker
- Debounce prevents rapid re-validation
- Typical validation < 500ms even for large scripts
- No UI blocking

**Test:** Already covered in validation unit tests

---

### 3. Rapid Typing

**Scenario:** User types continuously without pausing

**Handling:**

- Debounce clears previous timeout
- Validation only runs after 500ms pause
- No performance impact

**Test:** "Debounce works" test case

---

### 4. Form Data Race Conditions

**Scenario:** Validation completes after form data changes

**Handling:**

- Effect cleanup cancels pending timeouts
- New validation runs for new code
- Error state synchronized with form data

**Test:** Implicit in all test cases

---

## Implementation Checklist

- [ ] 1.1: Add validation hook to ScriptEditor.tsx
- [ ] 1.2: Update customValidate function
- [ ] 1.3: Remove old commented code
- [ ] 1.4: Run get_errors to verify no TypeScript errors
- [ ] 2.1: Enable skipped test and implement
- [ ] 2.2: Add 5 new test cases
- [ ] 2.3: Run Cypress tests and verify >80% coverage
- [ ] 3.1: Update validation README
- [ ] 3.2: Create TASK_COMPLETE.md
- [ ] 4.1: Manual testing - create invalid script
- [ ] 4.2: Manual testing - verify error display
- [ ] 4.3: Manual testing - verify debounce behavior

---

## Files Modified

### Primary Changes

1. `src/extensions/datahub/components/editors/ScriptEditor.tsx`
2. `src/extensions/datahub/components/editors/ScriptEditor.spec.cy.tsx`

### Documentation Updates

3. `src/extensions/datahub/components/forms/monaco/validation/README.md`
4. `.tasks/38512-datahub-js-validation/TASK_COMPLETE.md` (new)

**Total Files:** 4 (3 modified, 1 new)

---

## Success Criteria

1. ✅ ScriptEditor validates JavaScript syntax in real-time
2. ✅ Validation uses Monaco (no security concerns)
3. ✅ Invalid scripts cannot be saved
4. ✅ Error messages are clear and actionable
5. ✅ Tests cover all scenarios (>80% coverage)
6. ✅ No performance impact (debounced validation)
7. ✅ Graceful degradation if Monaco unavailable
8. ✅ Minimal code changes (existing validation module reused)

---

## Timeline

**Estimated:** 2-3 hours

- Phase 1: 1.5 hours (code + tests)
- Phase 2: 0.5 hours (documentation)
- Testing/Validation: 1 hour

---

## Notes

- Validation module already exists and is fully tested (21 unit tests)
- No new security concerns (Monaco uses static analysis)
- Minimal refactoring needed (add ~40 lines, remove ~10 lines)
- Pattern is proven (from validation README examples)
- Designer validation not needed (scripts selected only, not edited)
