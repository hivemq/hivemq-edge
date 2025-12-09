# Task 38512: Quick Reference

**Status:** 📋 Planning Complete - Ready for Implementation  
**Created:** December 8, 2025

---

## TL;DR

Add JavaScript validation to ScriptEditor using existing Monaco validation module. Replace disabled `new Function()` approach with safe static analysis.

**Time:** 2-3 hours | **Files:** 4 | **Lines:** ~40 added, ~10 removed

---

## Documentation Map

```
.tasks/38512-datahub-js-validation/
├── TASK_DESCRIPTION.md          ← Start here (problem statement)
├── TASK_SUMMARY.md              ← Quick overview & decisions
├── IMPLEMENTATION_PLAN.md       ← Detailed step-by-step guide
├── ARCHITECTURE_DIAGRAM.md      ← Visual flow diagrams
└── QUICK_REFERENCE.md           ← This file
```

**Read Order:**

1. TASK_SUMMARY.md (5 min) - Get the big picture
2. IMPLEMENTATION_PLAN.md (10 min) - Understand the approach
3. ARCHITECTURE_DIAGRAM.md (optional) - See visual flows

---

## The Problem

```typescript
// ScriptEditor.tsx line 142
// TODO[NVL] This is prone to code injection attacks
// try {
//   new Function(sourceCode)  // UNSAFE - Executes code
// } catch (e) {
//   errors.sourceCode?.addError((e as SyntaxError).message)
// }
```

Users can save invalid JavaScript. No syntax checking.

---

## The Solution

```typescript
// Use existing validation module (task 38053)
const { validate, isReady } = useJavaScriptValidation()
const [jsValidationError, setJsValidationError] = useState<string | null>(null)

useEffect(() => {
  if (!formData?.sourceCode || !isReady) return

  const timeoutId = setTimeout(async () => {
    const result = await validate(formData.sourceCode)
    setJsValidationError(result.isValid ? null : formatValidationError(result.errors[0]))
  }, 500)

  return () => clearTimeout(timeoutId)
}, [formData?.sourceCode, validate, isReady])

// In customValidate:
if (jsValidationError) {
  errors.sourceCode?.addError(jsValidationError)
}
```

Real-time validation, no security risk, minimal code.

---

## Files to Modify

| File                     | Changes                                  | LOC      |
| ------------------------ | ---------------------------------------- | -------- |
| ScriptEditor.tsx         | Add validation hook, effect, integration | +30, -10 |
| ScriptEditor.spec.cy.tsx | Enable skipped test, add 6 test cases    | +100     |
| validation/README.md     | Update RJSF example                      | +30      |
| TASK_COMPLETE.md         | New completion summary                   | +50      |

---

## Key Decisions

| Question              | Decision             | Rationale                                          |
| --------------------- | -------------------- | -------------------------------------------------- |
| Where to validate?    | ScriptEditor only    | Scripts created/edited here, designer is read-only |
| Debounce timing?      | Fixed 500ms          | Proven pattern, balances UX & performance          |
| Show how many errors? | First error only     | Consistent with RJSF, Monaco shows inline anyway   |
| Monaco unavailable?   | Graceful degradation | Better UX than blocking form                       |

---

## Test Checklist

- [ ] Invalid JS shows error message
- [ ] Save button disabled for invalid JS
- [ ] Valid JS passes without errors
- [ ] Error clears when code fixed
- [ ] Debounce works (only validates after 500ms pause)
- [ ] Monaco not ready doesn't break form
- [ ] Warnings don't block save

---

## Implementation Checklist

### Phase 1: Code (1.5h)

- [ ] Import `useJavaScriptValidation`, `formatValidationError`
- [ ] Add `jsValidationError` state
- [ ] Add validation hook initialization
- [ ] Implement debounced validation effect
- [ ] Update `customValidate` function
- [ ] Remove commented-out `new Function()` code
- [ ] Run `get_errors` to verify no TypeScript errors

### Phase 2: Tests (1h)

- [ ] Enable skipped test (line 99)
- [ ] Implement invalid JS test
- [ ] Add valid JS test
- [ ] Add error clearing test
- [ ] Add debounce test
- [ ] Add Monaco unavailable test
- [ ] Add warnings test
- [ ] Run tests, verify >80% coverage

### Phase 3: Docs (0.5h)

- [ ] Update validation README
- [ ] Create TASK_COMPLETE.md
- [ ] Update TASK_DESCRIPTION.md status

---

## Commands

```bash
# Run component tests
pnpm cypress:component:run --spec "src/extensions/datahub/components/editors/ScriptEditor.spec.cy.tsx"

# Run validation unit tests (already passing)
pnpm vitest run src/extensions/datahub/components/forms/monaco/validation/

# Check TypeScript errors
# (use get_errors tool in IDE)

# Coverage report
pnpm cypress:component:coverage
```

---

## Success Criteria

1. ✅ Invalid JS shows error, blocks save
2. ✅ Valid JS passes without errors
3. ✅ Real-time feedback (debounced)
4. ✅ No security concerns
5. ✅ Tests pass (>80% coverage)
6. ✅ Minimal code changes
7. ✅ Graceful degradation

---

## Related Documentation

- **Monaco Validation:** `src/extensions/datahub/components/forms/monaco/validation/README.md`
- **RJSF Patterns:** `.tasks/RJSF_GUIDELINES.md`
- **Monaco Testing:** `.tasks/MONACO_TESTING_GUIDE.md`
- **DataHub Architecture:** `.tasks/DATAHUB_ARCHITECTURE.md`

---

## Quick Links

### Implementation

- ScriptEditor: `src/extensions/datahub/components/editors/ScriptEditor.tsx`
- Validation Module: `src/extensions/datahub/components/forms/monaco/validation/`
- CodeEditor Widget: `src/extensions/datahub/components/forms/CodeEditor.tsx`

### Tests

- Component Tests: `src/extensions/datahub/components/editors/ScriptEditor.spec.cy.tsx`
- Validation Tests: `src/extensions/datahub/components/forms/monaco/validation/*.spec.ts`

### Related Tasks

- 38053-monaco-configuration (Monaco setup)
- 37937-datahub-resource-edit-flow (ScriptEditor implementation)

---

## Notes

- Validation module already exists and tested (21 unit tests)
- This is integration work, not new development
- Follow patterns from validation README
- Test as you code (TDD approach)
- Keep documentation updates minimal

---

## Contact Points

If stuck, reference these:

1. Validation README - RJSF integration example
2. ScriptEditor existing code - customValidate pattern
3. RJSF_GUIDELINES.md - validation patterns
4. IMPLEMENTATION_PLAN.md - detailed steps

---

**Ready to start? → Open IMPLEMENTATION_PLAN.md**
