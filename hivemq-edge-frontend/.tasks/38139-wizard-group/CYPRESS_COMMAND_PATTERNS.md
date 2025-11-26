# Cypress Command Patterns - Learnings from Subtask 7

**Date:** November 24, 2025  
**Context:** E2E test implementation for Group Wizard

---

## 🎯 Key Takeaways (TL;DR)

1. **NEVER use `rm -rf cypress/videos`** - Cypress automatically overwrites videos
2. **`rm` triggers approval every time** - It's not in the approved commands list
3. **Use `| head` instead of `| tail`** - Streams output immediately, no buffering
4. **Drawer/modal overlays block background UI by design** - Look for controls inside the drawer, never use `{force: true}`

---

## Issue Summary

During implementation of E2E tests, we discovered command patterns that cause terminal stalling and require unnecessary user approval despite being pre-approved.

---

## 🔍 Root Cause Analysis: Why Approval Was Needed

### The Mystery

Commands kept requiring approval despite having general command approval granted.

### The Culprit: `rm -rf cypress/videos`

**Discovered:** The `rm` command is NOT in the default list of approved commands. Any command chain containing `rm` triggers approval, even if the rest of the command is approved.

**Why this matters:**

- AI agent assumed videos directory needed manual cleanup
- Added `rm -rf cypress/videos &&` to every test run
- This single addition forced manual approval every single time
- Completely broke automated testing workflow

**The Solution:**

- **Don't use `rm` at all** - Cypress manages its own output directories
- Videos are automatically overwritten on each run
- No manual cleanup needed or beneficial

### Verification

Running tests **without** deleting videos:

```bash
pnpm cypress:run:e2e --spec "cypress/e2e/workspace/wizard/wizard-create-group.spec.cy.ts"
```

✅ Works perfectly  
✅ Videos overwritten automatically  
✅ No approval needed  
✅ Faster execution (no time wasted deleting directories)

---

## ❌ Problematic Pattern: Using `rm` and Piping with `tail`

### The Command

```bash
cd /Users/nicolas/IdeaProjects/hivemq-edge/hivemq-edge-frontend && rm -rf cypress/videos && pnpm cypress:run:e2e --spec "cypress/e2e/workspace/wizard/wizard-create-group.spec.cy.ts" 2>&1 | tail -80
```

### Problems

1. **User Approval Required** ⚠️ **ROOT CAUSE**

   - `rm` command is **NOT** in the default list of approved commands
   - Every command with `rm -rf` requires manual approval
   - This is why you kept seeing approval prompts despite having general command approval
   - Completely breaks automated workflow

2. **Unnecessary Operation**

   - **Cypress automatically overwrites videos** from previous runs
   - Each test creates a video with the same name, replacing the old one
   - Deleting the directory provides **NO benefit**
   - Wastes time waiting for directory deletion
   - **Verified:** Tests run perfectly fine without deleting videos directory

3. **Output Buffering** (Secondary Issue)

   - The `| tail -80` pipe waits for the entire command to complete before showing output
   - No streaming output visible during test execution
   - Appears to be "stalling" when actually running fine

4. **Limited Context**
   - Only shows last 80 lines
   - Miss important error context that occurred earlier in the run
   - Hard to see test progression (which test is running, how many passed/failed so far)

---

## ✅ Recommended Patterns: No `rm`, Efficient Output

**CRITICAL:** Never use `rm -rf cypress/videos` - it's unnecessary and triggers approval every time!

### Option 1: Use `head` for Initial Context (Preferred for Quick Checks)

```bash
cd /Users/nicolas/IdeaProjects/hivemq-edge/hivemq-edge-frontend && pnpm cypress:run:e2e --spec "cypress/e2e/workspace/wizard/wizard-create-group.spec.cy.ts" 2>&1 | head -100
```

**Benefits:**

- ✅ **No approval needed** - no `rm` command
- ✅ Cypress automatically manages videos - no cleanup needed
- Shows test execution start immediately
- Captures test names, describe blocks, and early errors
- Streams output (no buffering delay)

**When to Use:**

- Quick verification that test is running
- Checking test structure/organization
- Seeing if tests start correctly

### Option 2: Use `grep` for Specific Info

```bash
cd /Users/nicolas/IdeaProjects/hivemq-edge/hivemq-edge-frontend && pnpm cypress:run:e2e --spec "cypress/e2e/workspace/wizard/wizard-create-group.spec.cy.ts" 2>&1 | grep -E "(passing|failing|✓|✗)"
```

**Benefits:**

- Only shows pass/fail results
- Very concise output
- No buffering issues

**When to Use:**

- Just want to know final pass/fail status
- Running multiple test files

### Option 3: No Piping (Best for Debugging)

```bash
cd /Users/nicolas/IdeaProjects/hivemq-edge/hivemq-edge-frontend && pnpm cypress:run:e2e --spec "cypress/e2e/workspace/wizard/wizard-create-group.spec.cy.ts"
```

**Benefits:**

- See ALL output in real-time
- No buffering
- Full error stack traces
- Test progression visible

**Drawbacks:**

- Very verbose
- May exceed terminal buffer limits

**When to Use:**

- Debugging failing tests
- Need full error context
- Single test file execution

### Option 4: Output to File Then Read

```bash
cd /Users/nicolas/IdeaProjects/hivemq-edge/hivemq-edge-frontend && pnpm cypress:run:e2e --spec "cypress/e2e/workspace/wizard/wizard-create-group.spec.cy.ts" > /tmp/cypress-test.log 2>&1 && tail -100 /tmp/cypress-test.log
```

**Benefits:**

- Captures full output to file
- Can review any part of output after completion
- No streaming needed

**Drawbacks:**

- No real-time feedback
- Extra step to read file

**When to Use:**

- Long-running test suites
- Need to review full output later
- Debugging intermittent issues

---

## 🎯 Best Practice Recommendations

### For Iterative Test Development (Using `.only`)

```bash
# Simple, clean output - just shows pass/fail
pnpm cypress:run:e2e --spec "cypress/e2e/workspace/wizard/wizard-create-group.spec.cy.ts"
```

**Why:**

- Only one test runs (`.only`)
- Output is short and clear
- No need for filtering
- See errors immediately

### For Running Full Test Suite

```bash
# Show first 150 lines (covers describe blocks and early tests)
pnpm cypress:run:e2e --spec "cypress/e2e/workspace/wizard/wizard-create-group.spec.cy.ts" | head -150
```

**Why:**

- See test organization
- Catch early failures
- Streaming output (no delay)

### For Checking Final Results Only

```bash
# Just the summary
pnpm cypress:run:e2e --spec "cypress/e2e/workspace/wizard/wizard-create-group.spec.cy.ts" 2>&1 | grep -A 20 "passing"
```

**Why:**

- Minimal output
- Just need to know pass/fail count
- No intermediate noise

---

## 🔍 Why Terminal Commands Appear to Stall

### Root Causes

1. **Pipe Buffering**

   - Commands like `tail` wait for complete input before processing
   - Cypress generates ~1000+ lines of output for complex tests
   - No output appears until test completes

2. **Approval Dialog Timing**

   - Command approval dialog shows before execution starts
   - If command takes 30+ seconds to run, user thinks it's stuck
   - No feedback that test is actually running

3. **Silent Progress**
   - Without streaming output, can't see test progression
   - Can't tell if stuck in beforeEach, in test, or hanging

### Solutions

1. **Avoid tail/tail -n patterns** - Use `head` instead
2. **Use grep for targeted output** - Show only what matters
3. **For debugging, no pipes** - See everything in real-time
4. **Consider background execution** - For very long test runs

---

## 📝 Key Learnings

### Drawer/Modal Overlay Pattern

**Context:** When debugging "element covered by footer" errors in E2E tests

**The Pattern:**

- Drawers/modals overlay the main UI with `<DrawerOverlay>`
- Background elements (toolbars, progress bars) become non-interactive BY DESIGN
- Navigation controls are self-contained within the drawer/modal

**How to Recognize:**

```
CypressError: element is being covered by another element:
  <footer class="chakra-modal__footer">...</footer>
```

**Investigation Steps:**

1. ✅ Error mentions "modal/drawer footer/overlay" - this is the clue
2. ✅ Check if overlay UI component is open (Drawer, Modal, etc.)
3. ✅ Find component source code (e.g., `WizardGroupForm.tsx`)
4. ✅ Look for controls inside `<DrawerFooter>` or `<ModalFooter>`
5. ✅ Update page object to use correct test IDs
6. ❌ NEVER use `{force: true}` - this bypasses legitimate design patterns

**Real Example:**

```typescript
// ❌ Wrong - tries to click covered progress bar button
wizardPage.progressBar.backButton.click()

// ✅ Correct - clicks button inside drawer footer
wizardPage.groupConfig.backButton.click()
```

**Documentation Updated:** `.tasks/TESTING_GUIDELINES.md` - Section on "Understanding Drawer/Modal Overlay Patterns"

---

## 🚦 Decision Matrix: Which Command Pattern to Use

| Scenario                 | Command Pattern                | Reasoning                           |
| ------------------------ | ------------------------------ | ----------------------------------- |
| Single test with `.only` | No pipes                       | Short output, need full errors      |
| Multiple passing tests   | `\| head -150`                 | See structure, avoid tail buffering |
| Just checking pass/fail  | `\| grep "(passing\|failing)"` | Minimal, targeted output            |
| Debugging failures       | No pipes                       | Need full stack traces              |
| Very long test suite     | `> file && tail file`          | Capture all, review after           |
| Checking test exists     | `\| head -50`                  | Quick verification                  |

---

## 🤖 Quick Reference for AI Agents

When running Cypress E2E tests:

```bash
# ✅ DO THIS - No approval needed
pnpm cypress:run:e2e --spec "cypress/e2e/path/to/test.spec.cy.ts" 2>&1 | head -100

# ❌ NEVER DO THIS - Requires approval every time
rm -rf cypress/videos && pnpm cypress:run:e2e --spec "..." 2>&1 | tail -80
```

**Why:**

- `rm` is not an approved command → triggers manual approval
- Cypress manages videos automatically → `rm` is unnecessary
- `head` streams output → `tail` buffers and appears to stall

---

## 📋 Action Items

- [x] Document drawer/modal pattern in TESTING_GUIDELINES.md
- [x] Identify root cause of approval issues (`rm` command)
- [x] Verify Cypress works without deleting videos
- [x] Document recommended command patterns
- [ ] Update AI_AGENT_CYPRESS_COMPLETE_GUIDE.md with these findings
- [ ] Consider adding npm script for filtered Cypress output
- [ ] Review other test suites for similar patterns
