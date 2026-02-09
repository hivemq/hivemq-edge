# Task 38512: Validation Flow Architecture

## Data Flow Diagram

```
┌─────────────────────────────────────────────────────────────────┐
│                         User Types JS Code                       │
└──────────────────────────┬──────────────────────────────────────┘
                           │
                           ▼
┌─────────────────────────────────────────────────────────────────┐
│                  CodeEditor Widget (Monaco)                      │
│  src/extensions/datahub/components/forms/CodeEditor.tsx         │
│                                                                   │
│  - Renders Monaco Editor                                         │
│  - Provides inline syntax highlighting                           │
│  - Shows inline error markers                                    │
└──────────────────────────┬──────────────────────────────────────┘
                           │ onChange(value)
                           ▼
┌─────────────────────────────────────────────────────────────────┐
│                      RJSF Form State                             │
│  src/extensions/datahub/components/editors/ScriptEditor.tsx     │
│                                                                   │
│  formData = {                                                    │
│    name: "my-script",                                            │
│    sourceCode: "function transform(...) { ... }",               │
│    version: "DRAFT"                                              │
│  }                                                               │
└──────────────────────────┬──────────────────────────────────────┘
                           │ formData.sourceCode
                           ▼
┌─────────────────────────────────────────────────────────────────┐
│               useEffect (Debounced 500ms)                        │
│                                                                   │
│  useEffect(() => {                                               │
│    const timeoutId = setTimeout(async () => {                   │
│      const result = await validateJS(sourceCode)                │
│      setJsValidationError(...)                                  │
│    }, 500)                                                       │
│    return () => clearTimeout(timeoutId)                         │
│  }, [formData?.sourceCode])                                     │
└──────────────────────────┬──────────────────────────────────────┘
                           │ after 500ms pause
                           ▼
┌─────────────────────────────────────────────────────────────────┐
│              useJavaScriptValidation() Hook                      │
│  src/extensions/.../monaco/validation/useJavaScriptValidation.ts│
│                                                                   │
│  const { validate, isReady } = useJavaScriptValidation()        │
│                                                                   │
│  - Checks if Monaco is loaded (isReady)                         │
│  - Calls validateJavaScript(monaco, code)                       │
└──────────────────────────┬──────────────────────────────────────┘
                           │
                           ▼
┌─────────────────────────────────────────────────────────────────┐
│            validateJavaScript(monaco, code)                      │
│  src/extensions/.../monaco/validation/javascriptValidator.ts    │
│                                                                   │
│  1. Create temporary model                                       │
│  2. Wait for language service                                    │
│  3. Get markers (errors/warnings)                               │
│  4. Dispose model                                                │
│  5. Return ValidationResult                                      │
└──────────────────────────┬──────────────────────────────────────┘
                           │
                           ▼
┌─────────────────────────────────────────────────────────────────┐
│           TypeScript Service (Web Worker)                        │
│                                                                   │
│  - Runs in isolated Web Worker (no access to app)               │
│  - Performs static analysis only                                │
│  - No code execution                                             │
│  - Returns markers (line, column, message, severity)            │
└──────────────────────────┬──────────────────────────────────────┘
                           │ Markers
                           ▼
┌─────────────────────────────────────────────────────────────────┐
│                    ValidationResult                              │
│                                                                   │
│  {                                                               │
│    isValid: false,                                               │
│    errors: [                                                     │
│      {                                                           │
│        message: "';' expected.",                                 │
│        line: 3,                                                  │
│        column: 15,                                               │
│        severity: "error"                                         │
│      }                                                           │
│    ],                                                            │
│    warnings: []                                                  │
│  }                                                               │
└──────────────────────────┬──────────────────────────────────────┘
                           │ formatValidationError()
                           ▼
┌─────────────────────────────────────────────────────────────────┐
│                jsValidationError State                           │
│                                                                   │
│  "Line 3, Column 15: ';' expected."                             │
└──────────────────────────┬──────────────────────────────────────┘
                           │
                           ▼
┌─────────────────────────────────────────────────────────────────┐
│              customValidate Function (RJSF)                      │
│                                                                   │
│  const customValidate = (formData, errors) => {                 │
│    if (jsValidationError) {                                     │
│      errors.sourceCode?.addError(jsValidationError)             │
│    }                                                             │
│    return errors                                                 │
│  }                                                               │
└──────────────────────────┬──────────────────────────────────────┘
                           │
                           ▼
┌─────────────────────────────────────────────────────────────────┐
│                RJSF Error Display (ChakraUI)                     │
│                                                                   │
│  <FormControl isInvalid>                                         │
│    <FormErrorMessage id="root_sourceCode__error">              │
│      Line 3, Column 15: ';' expected.                           │
│    </FormErrorMessage>                                           │
│  </FormControl>                                                  │
│                                                                   │
│  [Save] button disabled (hasErrors = true)                      │
└─────────────────────────────────────────────────────────────────┘
```

## Component Interaction Diagram

```
┌──────────────────────────────────────────────────────────────────┐
│                      ScriptEditor.tsx                             │
│  ┌────────────────────────────────────────────────────────────┐  │
│  │  State                                                      │  │
│  │  - formData: FunctionData                                  │  │
│  │  - hasErrors: boolean                                      │  │
│  │  - jsValidationError: string | null  ◄──── NEW            │  │
│  └────────────────────────────────────────────────────────────┘  │
│                                                                    │
│  ┌────────────────────────────────────────────────────────────┐  │
│  │  Hooks                                                      │  │
│  │  - useGetAllScripts()                                      │  │
│  │  - useCreateScript()                                       │  │
│  │  - useJavaScriptValidation()  ◄──── NEW                   │  │
│  └────────────────────────────────────────────────────────────┘  │
│                                                                    │
│  ┌────────────────────────────────────────────────────────────┐  │
│  │  Effects                                                    │  │
│  │  - Initialize form data                                    │  │
│  │  - Track dirty state                                       │  │
│  │  - Debounced validation  ◄──── NEW                         │  │
│  └────────────────────────────────────────────────────────────┘  │
│                                                                    │
│  ┌────────────────────────────────────────────────────────────┐  │
│  │  Functions                                                  │  │
│  │  - handleChange()                                          │  │
│  │  - customValidate()  ◄──── UPDATED                         │  │
│  │  - handleSubmit()                                          │  │
│  └────────────────────────────────────────────────────────────┘  │
│                                                                    │
│  ┌────────────────────────────────────────────────────────────┐  │
│  │  Render                                                     │  │
│  │  <ReactFlowSchemaForm                                      │  │
│  │    schema={MOCK_FUNCTION_SCHEMA}                           │  │
│  │    formData={formData}                                     │  │
│  │    onChange={handleChange}                                 │  │
│  │    customValidate={customValidate}  ◄──── Uses validation │  │
│  │    widgets={datahubRJSFWidgets}                            │  │
│  │  >                                                          │  │
│  │    <CodeEditor widget for sourceCode field>               │  │
│  │  </ReactFlowSchemaForm>                                    │  │
│  └────────────────────────────────────────────────────────────┘  │
└──────────────────────────────────────────────────────────────────┘
```

## Validation State Machine

```
┌─────────────┐
│   Initial   │
│   State     │
└──────┬──────┘
       │ User opens ScriptEditor
       ▼
┌─────────────┐
│   Monaco    │
│  Loading    │
└──────┬──────┘
       │ isReady = true
       ▼
┌─────────────────────────────────────────────────────────────┐
│                    Ready State                              │
└──────┬──────────────────────────────────────┬──────────────┘
       │                                       │
       │ User types in editor                  │ No changes
       ▼                                       ▼
┌─────────────┐                         ┌──────────────┐
│  Debounce   │                         │   Valid      │
│   Period    │                         │   State      │
│  (500ms)    │                         │              │
└──────┬──────┘                         │ No errors    │
       │ Timeout expires                 └──────────────┘
       ▼
┌─────────────┐
│ Validating  │
└──────┬──────┘
       │ validateJS() completes
       │
       ├──► isValid = true ──────┐
       │                          ▼
       │                    ┌──────────────┐
       │                    │   Valid      │
       │                    │   State      │
       │                    │              │
       │                    │ No errors    │
       │                    │ Save enabled │
       │                    └──────────────┘
       │
       └──► isValid = false ─────┐
                                  ▼
                            ┌──────────────┐
                            │   Error      │
                            │   State      │
                            │              │
                            │ Error shown  │
                            │ Save disabled│
                            └──────┬───────┘
                                   │ User fixes code
                                   │
                                   ▼
                            ┌──────────────┐
                            │  Debounce    │
                            │   Period     │
                            └──────┬───────┘
                                   │
                                   ▼
                            (Validation cycle repeats)
```

## Security Comparison

```
┌─────────────────────────────────────────────────────────────┐
│              OLD APPROACH (Unsafe - Disabled)                │
├─────────────────────────────────────────────────────────────┤
│                                                               │
│   try {                                                       │
│     new Function(sourceCode)  ◄──── EXECUTES CODE           │
│   } catch (e) {                                              │
│     // Handle syntax error                                   │
│   }                                                           │
│                                                               │
│  ⚠️  Security Issues:                                        │
│  - Code is actually executed                                 │
│  - Access to closure variables                               │
│  - Potential code injection vector                           │
│  - Can affect application state                              │
│                                                               │
└─────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────┐
│              NEW APPROACH (Safe - Monaco)                     │
├─────────────────────────────────────────────────────────────┤
│                                                               │
│   const result = await validateJavaScript(                   │
│     monaco,                                                   │
│     sourceCode                                                │
│   )                                                           │
│                                                               │
│   Monaco Editor:                                              │
│   1. Create temporary model                                   │
│   2. TypeScript Service analyzes (Web Worker)                │
│   3. Return markers (errors/warnings)                         │
│   4. Dispose model                                            │
│                                                               │
│  ✅ Security Benefits:                                        │
│  - No code execution at all                                  │
│  - Static analysis only                                      │
│  - Isolated in Web Worker                                    │
│  - No access to application state                            │
│  - Industry standard (VS Code uses same approach)            │
│                                                               │
└─────────────────────────────────────────────────────────────┘
```

## Test Coverage Map

```
┌────────────────────────────────────────────────────────────────┐
│                   ScriptEditor.spec.cy.tsx                      │
├────────────────────────────────────────────────────────────────┤
│                                                                  │
│  Existing Tests (Pass)                                          │
│  ├─ renders drawer                                              │
│  ├─ shows create mode                                           │
│  ├─ handles form submission                                     │
│  ├─ validates duplicate names                                   │
│  └─ ... (other existing tests)                                  │
│                                                                  │
│  ──────────────────────────────────────────────────────────────│
│                                                                  │
│  NEW: Validation Tests                                          │
│  ├─ ✓ invalid JS shows error (enable skipped)                  │
│  ├─ ✓ valid JS passes validation                               │
│  ├─ ✓ validation clears when fixed                             │
│  ├─ ✓ debounce prevents excessive calls                        │
│  ├─ ✓ Monaco not ready gracefully degrades                     │
│  └─ ✓ warnings don't block save                                │
│                                                                  │
└────────────────────────────────────────────────────────────────┘

┌────────────────────────────────────────────────────────────────┐
│             javascriptValidator.spec.ts (Existing)              │
├────────────────────────────────────────────────────────────────┤
│                                                                  │
│  ✅ 21 unit tests (100% passing)                               │
│  ├─ validates syntax errors                                     │
│  ├─ handles valid code                                          │
│  ├─ detects missing semicolons                                  │
│  ├─ catches undefined variables                                 │
│  ├─ handles large scripts                                       │
│  ├─ disposes models correctly                                   │
│  └─ ... (comprehensive edge cases)                              │
│                                                                  │
│  No changes needed - validation module already tested          │
│                                                                  │
└────────────────────────────────────────────────────────────────┘
```

## Timeline Visualization

```
Phase 1: Core Integration (1.5 hours)
├─ Add validation hook (0.25h)
├─ Implement debounced effect (0.25h)
├─ Update customValidate (0.25h)
├─ Remove old code (0.25h)
└─ Test & verify (0.5h)

Phase 2: Test Implementation (1 hour)
├─ Enable skipped test (0.25h)
├─ Add 5 new test cases (0.5h)
└─ Run tests & verify coverage (0.25h)

Phase 3: Documentation (0.5 hours)
├─ Update validation README (0.25h)
└─ Create TASK_COMPLETE (0.25h)

Total: 3 hours
```
