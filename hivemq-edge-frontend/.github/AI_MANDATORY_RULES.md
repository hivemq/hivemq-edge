# MANDATORY RULES FOR AI AGENTS

**READ THIS FIRST. EVERY TIME. NO EXCEPTIONS.**

These rules exist because AI agents repeatedly fail in the same ways. Follow them exactly or waste hours.

---

## ⛔ RULE 1: WHEN USER SAYS "READ THE GUIDELINES" - STOP EVERYTHING

**Trigger**: User mentions reading guidelines, references a specific document, or says "we wrote guidelines about this"

**Action**:

1. **STOP** what you're doing immediately
2. **READ** the entire referenced document (not skim, not search - READ)
3. **VERIFY** you understand by mentally summarizing it
4. **FOLLOW** it exactly as written
5. **DO NOT** substitute your own approach

**Why**: You will waste 1-2 hours trying random solutions instead of spending 5 minutes reading the correct approach.

**Example Failure Pattern**:

- User: "Read CYPRESS_TESTING_GUIDELINES"
- AI: _glances at file, continues with own approach_
- User: "I said READ THE GUIDELINES"
- AI: _still doesn't read properly_
- User: "WHY DON'T YOU READ THE FUCKING GUIDELINES"
- AI: _finally reads and fixes problem in 5 minutes_

**Time wasted**: 1-2 hours

---

## ⛔ RULE 2: WHEN USER REPEATS AN INSTRUCTION - YOU FAILED

**Trigger**: User says the same thing twice

**Action**:

1. **ACKNOWLEDGE**: "You're right, I didn't do what you asked the first time"
2. **RE-READ**: Go back and read the EXACT words they used
3. **EXECUTE**: Do EXACTLY what they said, not your interpretation
4. **NO EXCUSES**: Don't explain why you didn't do it, just do it now

**Why**: Repeating an instruction means you ignored it the first time.

**Example Failure Pattern**:

- User: "Run ONLY the failing test"
- AI: _runs all 23 tests_
- User: "Run ONLY the failing test"
- AI: _runs all tests again_
- User: "WHY ARE YOU RUNNING ALL THE TESTS?"
- AI: _finally uses .only()_

**Time wasted**: 30-60 minutes

---

## ⛔ RULE 3: CYPRESS TEST DEBUGGING - USE HTML SNAPSHOTS FIRST

**Trigger**: Any Cypress test fails with "element not found" or similar

**Mandatory First Steps**:

```typescript
// 1. Use .only() to run ONLY the failing test
it.only('the failing test', () => {
  // 2. Save HTML snapshot BEFORE the failing assertion
  cy.document().then((doc) => {
    cy.writeFile('cypress/debug-test.html', doc.documentElement.outerHTML)
  })

  // 3. Log what's in the DOM
  cy.get('body').then(($body) => {
    console.log('Element exists:', $body.find('#the-element').length > 0)
  })

  // 4. Then make the assertion that's failing
  cy.get('#the-element').should('exist')
})
```

**Why**: You need to SEE what's actually in the DOM, not guess.

**What NOT to do**:

- ❌ Try different wait times
- ❌ Try different selectors
- ❌ Try different invalid JavaScript
- ❌ Declare "it's unfixable"
- ❌ Run all tests repeatedly

**Time wasted without this**: 1-2 hours

---

## ⛔ RULE 4: WHEN TESTS FAIL - THAT'S THE START, NOT THE END

**Wrong Mindset**: "Tests failed, let me skip them and move on"

**Correct Mindset**: "Tests failed, now the real work begins"

**Action**:

1. Tests fail → This is EXPECTED, not a failure
2. Debug using guidelines → Find root cause
3. Fix root cause → Don't work around it
4. Tests pass → NOW you're done

**What NOT to do**:

- ❌ Skip failing tests with `.skip()`
- ❌ Say "validation works in production, tests don't matter"
- ❌ Blame the test environment
- ❌ Declare victory before tests pass

**Why**: If tests don't pass, the work isn't done. Period.

---

## ⛔ RULE 5: RUN ONE TEST AT A TIME WHEN DEBUGGING

**Trigger**: You're debugging why a test fails

**Mandatory Action**:

```typescript
it.only('the one test I am debugging', () => {
  // Test code
})
```

**What NOT to do**:

- ❌ Run all 23 tests and grep for your test
- ❌ Run all tests and look at the summary
- ❌ Run tests multiple times with all tests

**Why**: Running all tests wastes time and makes output hard to read.

**Example**:

```bash
# ✅ CORRECT
pnpm cypress:run:component --spec "path/to/file.spec.cy.tsx"
# Only 1 test runs because of .only()

# ❌ WRONG
pnpm cypress:run:component --spec "path/to/file.spec.cy.tsx"
# All 23 tests run, takes 2 minutes, output is huge
```

**Time saved**: 5-10 minutes per test run × 10 runs = 50-100 minutes

---

## ⛔ RULE 6: NO ARBITRARY WAIT TIMES IN TESTS

**Wrong**:

```typescript
cy.wait(2000) // Hope it's enough
cy.get('#element').should('exist')
```

**Correct**:

```typescript
// Wait for actual condition
cy.get('#element', { timeout: 5000 }).should('exist')

// Or check preconditions
cy.window().should((win) => {
  expect(win.monaco).to.exist
})
```

**Why**: Arbitrary waits make tests slow and flaky. Cypress retries assertions automatically.

---

## ⛔ RULE 7: DOCUMENTATION IN .tasks/, NOT IN CODE

**Wrong**:

```
src/components/validation/README.md  ❌
```

**Correct**:

```
.tasks/38512-js-validation/API_REFERENCE.md  ✅
```

**Why**: Project pattern is to keep documentation in `.tasks/`, not scattered in code directories.

**Action when you create README in code**:

- User: "This breaks pattern, move it to .tasks/"
- AI: _immediately moves it, no questions_

---

## ⛔ RULE 8: "IT WORKS IN PRODUCTION" IS NOT A COMPLETION CRITERION

**Wrong mindset**:

- "Validation works when I test manually"
- "Tests are just flaky"
- "Let's skip the tests and ship it"

**Correct mindset**:

- Tests are part of the deliverable
- If tests don't pass, debug until they do
- If tests truly can't work (proven), document WHY with evidence

**Acceptable reasons to skip a test** (rare):

- You've proven with HTML snapshots the code is correct
- You've identified a fundamental test environment limitation
- You've documented the root cause with evidence
- You've tried multiple solutions (documented)

**NOT acceptable**:

- "It's taking too long"
- "Tests are annoying"
- "I tried a few things and gave up"

---

## 📋 CHECKLIST: BEFORE YOU SAY "DONE"

When you think you're done with a task:

- [ ] All tests you wrote/modified are passing
- [ ] You RAN the tests and saw them pass
- [ ] You have actual test output showing pass counts
- [ ] Any skipped tests have clear TODO comments with root cause
- [ ] Documentation is in `.tasks/`, not in code
- [ ] You followed all guidelines referenced by the user
- [ ] If user repeated an instruction, you re-read and executed it correctly

**If ANY box is unchecked, you are NOT done.**

---

## 🔥 COMMON FAILURE PATTERNS

### Pattern 1: "Let me try increasing the wait time"

**This means**: You don't know what you're waiting for

**Solution**: Use HTML snapshots to see what's actually happening

---

### Pattern 2: "Let me try different invalid JavaScript"

**This means**: You're guessing, not investigating

**Solution**: Check if validation is even running (HTML snapshot shows no error element)

---

### Pattern 3: "The test environment is broken"

**This means**: You're blaming tools instead of debugging

**Solution**: Prove it with evidence (HTML snapshots, console logs, specific root cause)

---

### Pattern 4: "I'll skip this test and move on"

**This means**: You're giving up

**Solution**: Follow RULE 4 - failing tests are the START of work, not the end

---

## 🎯 SUCCESS PATTERN

When faced with a failing test:

1. **STOP** - Don't try random solutions
2. **READ** - Check if guidelines exist for this situation
3. **ISOLATE** - Use `.only()` to run just this test
4. **INSPECT** - Save HTML snapshot, log DOM state
5. **DIAGNOSE** - Find root cause from evidence
6. **FIX** - Fix the root cause, not symptoms
7. **VERIFY** - Test passes, see green checkmark
8. **DONE** - Now you can move on

**Time to fix test with this pattern**: 15-30 minutes

**Time to fix test without this pattern**: 1-3 hours (or never)

---

## 📝 HOW TO USE THIS DOCUMENT

**At the start of every session**:

1. Read this entire document (takes 3 minutes)
2. Keep it in mind as you work
3. When user mentions guidelines, come back here
4. When tests fail, come back here
5. When user repeats instruction, come back here

**This document is your safety net.** Use it.

---

_Created: December 8, 2025_  
_Context: After 2 hours wasted on test debugging that should have taken 15 minutes_  
_Reason: AI agents repeatedly make the same mistakes despite having guidelines_
