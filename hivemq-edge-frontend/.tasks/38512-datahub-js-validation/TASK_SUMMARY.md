# Task 38512: DataHub JavaScript Validation - Summary

**Created:** December 8, 2025  
**Status:** 📋 Planning Complete - Ready for Implementation  
**Version:** 1.0

---

## Quick Summary

Add JavaScript syntax validation to DataHub ScriptEditor using Monaco Editor's existing validation module, replacing the disabled security-risky `new Function()` approach.

---

## Problem Statement

**Current State:**

- ScriptEditor has JavaScript syntax validation commented out (line 142) due to security concerns with `new Function()`
- Users can save invalid JavaScript without any warning
- No real-time feedback for syntax errors
- TODO comment indicates intention to use Monaco validation instead

**Security Context:**

- Previous approach used `new Function(sourceCode)` which is vulnerable to code injection
- Approach was correctly disabled but left validation gap

---

## Solution Approach

**Use Existing Monaco Validation Module:**

- Module already exists at `src/extensions/datahub/components/forms/monaco/validation/`
- Provides `useJavaScriptValidation()` hook
- Uses Monaco's TypeScript language service (Web Worker) - 100% safe, no code execution
- Already tested (21 unit tests, 100% passing)
- Documented with RJSF integration examples

**Integration Point:**

- Add validation hook to ScriptEditor component
- Implement debounced real-time validation (500ms)
- Integrate with RJSF `customValidate` function
- Show errors inline using existing RJSF error display

---

## Key Decisions

### 1. Validation Location

**✅ ScriptEditor only** (not designer)

- Scripts are created/edited in ScriptEditor
- Designer (FunctionPanelSimplified) only selects existing scripts (read-only)
- Validate at source prevents invalid scripts from being saved

### 2. Debounce Timing

**✅ Fixed 500ms**

- Proven pattern from validation module documentation
- Balance between responsiveness and performance
- No configuration needed (no user requirement)

### 3. Error Display

**✅ First error only**

- Consistent with RJSF patterns throughout codebase
- Monaco editor shows all errors inline anyway
- Prevents error message clutter

### 4. Graceful Degradation

**✅ Allow form use if Monaco unavailable**

- Monaco loads from CDN (could fail)
- Better UX to allow fallback than block form
- Validation is enhancement, not critical requirement

---

## Implementation Scope

### Code Changes (Minimal)

1. **ScriptEditor.tsx**: Add ~30 lines (validation hook, effect, integration)
2. **ScriptEditor.spec.cy.tsx**: Add ~100 lines (6 test cases)
3. Remove ~10 lines (commented-out `new Function()` code)

### Testing

- Enable 1 skipped test
- Add 5 new test cases
- Target: >80% coverage for modified code

### Documentation

- Update validation README with real-world example
- Create TASK_COMPLETE.md

**Total Files:** 4 (3 modified, 1 new)

---

## Success Criteria

1. ✅ JavaScript syntax errors detected in real-time
2. ✅ Invalid scripts cannot be saved
3. ✅ Clear error messages displayed
4. ✅ No security concerns (Monaco static analysis only)
5. ✅ No performance impact (debounced)
6. ✅ Comprehensive test coverage (>80%)
7. ✅ Graceful handling if Monaco unavailable

---

## Related Tasks

- **38053-monaco-configuration**: Monaco Editor setup with TypeScript validation
- **37937-datahub-resource-edit-flow**: ScriptEditor implementation and resource management
- **37542-code-coverage**: Test coverage tracking

---

## Technical Context

### Monaco Validation Architecture

```
User Types JS Code
      ↓
CodeEditor Widget (Monaco)
      ↓ (value change)
RJSF Form State
      ↓ (formData.sourceCode)
useEffect (debounced 500ms)
      ↓
useJavaScriptValidation()
      ↓
validateJavaScript(monaco, code)
      ↓
TypeScript Service (Web Worker)
      ↓
Markers (errors/warnings)
      ↓
ValidationResult
      ↓
jsValidationError state
      ↓
customValidate function
      ↓
RJSF Error Display
```

### Security Model

**Safe Approach (Monaco):**

- Static analysis only (no execution)
- TypeScript language service in Web Worker
- Industry standard (same as VS Code)
- No access to application context

**Unsafe Approach (Disabled):**

- `new Function(code)` executes code
- Access to closure variables
- Potential code injection vector
- Correctly removed from codebase

---

## Dependencies

### Required (Already Available)

- ✅ Monaco Editor (@monaco-editor/react v4.7.0)
- ✅ Monaco validation module (task 38053)
- ✅ ScriptEditor component (task 37937)
- ✅ RJSF integration (@rjsf/chakra-ui)

### No New Dependencies

- Uses existing packages
- No additional npm installs needed

---

## Timeline Estimate

**Total: 2-3 hours**

| Phase | Task                | Duration  |
| ----- | ------------------- | --------- |
| 1     | Code implementation | 0.5 hours |
| 2     | Test implementation | 1.0 hours |
| 3     | Documentation       | 0.5 hours |
| 4     | Manual testing      | 1.0 hours |

---

## Risk Assessment

### Low Risk ✅

**Why:**

1. Validation module already exists and tested
2. Integration pattern proven (documented in README)
3. No new dependencies
4. Minimal code changes
5. No breaking changes to existing functionality
6. Security concerns already addressed (Monaco is safe)

**Mitigation:**

- Comprehensive test coverage
- Graceful degradation if Monaco fails
- Follow proven patterns from validation docs

---

## Next Steps

1. Review IMPLEMENTATION_PLAN.md for detailed step-by-step guide
2. Begin Phase 1: Core Integration
3. Implement tests during development (TDD approach)
4. Verify all tests pass
5. Update documentation
6. Create TASK_COMPLETE.md

---

## Notes

- This is a straightforward integration task, not a research task
- Solution already exists and is proven
- Main work is wiring existing pieces together
- Test coverage is critical (enable skipped test + add comprehensive cases)
- Documentation should reference ScriptEditor as canonical example

---

## References

- Validation Module: `src/extensions/datahub/components/forms/monaco/validation/`
- Validation README: `src/extensions/datahub/components/forms/monaco/validation/README.md`
- ScriptEditor: `src/extensions/datahub/components/editors/ScriptEditor.tsx`
- Monaco Config: `.tasks/38053-monaco-configuration/`
- Resource Flow: `.tasks/37937-datahub-resource-edit-flow/`
- RJSF Guidelines: `.tasks/RJSF_GUIDELINES.md`
- Monaco Testing: `.tasks/MONACO_TESTING_GUIDE.md`
