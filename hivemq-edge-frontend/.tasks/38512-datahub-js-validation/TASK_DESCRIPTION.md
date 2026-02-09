We are working on a new task, 38512-datahub-js-validation, to add validation of JS scripts in the DataHub.
The context is the Datahub designer, with RJSF and the Monaco Editor; familiarise yourself with the existing documents (DATAHUB_ARCHITECTURE, RJSF_GUIDELINES and MONACO_TESTING_GUIDE)

This issue can be described as below:

- The Datahub designer uses Monaco Editor to support editing Javascript code. As such. it includes code highlighting and validation (see tasks 38053-monaco-configuration)
- The editor is embeded in a RSJF-based form (in the list of scripts, see task 37937-datahub-resource-edit-flow), that doesn't implement any custom validation for the edited code. A previous attempt, using `new Function` was discarded because of the risk of code injection
- The datahub Designer itself doesn't include the editor for the code but uses a reference to the (now base64-bundled) code. It has basic validation for the existence of the referenced script but no validation for the content of the script.

The task is this:

- can we use any mechanism offered by Monaco to validate the Javascript code once we are outside of the loaded editor (e.g. in a RJSF custom validator)?

The requirements:

- minimise the refactoring of existing code
- do not offer solutions that are known to be caused of security concerns
- remember to add tests for every new file create (Cypress for component, Vitest for unit tests)

Get an understandign of the task and propose a plan of action. Please be mindful of the requirements for documentation and avoid sprawling too many documents

---

## Planning Complete ✅

**Date:** December 8, 2025

The task analysis and planning phase is complete. Three focused documents have been created:

1. **IMPLEMENTATION_PLAN.md** - Detailed step-by-step implementation guide
2. **TASK_SUMMARY.md** - Quick overview and key decisions
3. **ARCHITECTURE_DIAGRAM.md** - Visual flow diagrams and architecture

### Key Findings

**Solution Exists:** The validation module already exists and is fully tested (task 38053). This is an integration task, not a research task.

**Approach:** Use `useJavaScriptValidation()` hook in ScriptEditor with debounced real-time validation (500ms). Integrate with RJSF `customValidate` function.

**Security:** Monaco's TypeScript service uses static analysis in Web Worker (no code execution). This is safe and industry-standard.

**Scope:** Validation only in ScriptEditor (where scripts are created/edited). Designer only selects scripts (read-only), so no validation needed there.

**Changes:** Minimal refactoring (~40 lines added, ~10 removed) across 4 files.

**Tests:** Enable 1 skipped test, add 5 new test cases. Target >80% coverage.

**Timeline:** 2-3 hours total.

### Next Steps

1. Review IMPLEMENTATION_PLAN.md for detailed checklist
2. Begin Phase 1: Core Integration (ScriptEditor.tsx)
3. Implement tests in parallel (TDD approach)
4. Update documentation
5. Create TASK_COMPLETE.md when done
