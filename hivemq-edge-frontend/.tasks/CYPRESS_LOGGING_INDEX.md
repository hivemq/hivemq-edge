# Cypress Logging Documentation - Master Index

**Created:** November 11, 2025  
**Last Updated:** November 12, 2025 (Consolidated into CYPRESS_TESTING_GUIDELINES.md)  
**Purpose:** Master index for Cypress logging documentation

---

## ⚠️ CONSOLIDATION NOTICE

**As of November 12, 2025**, all Cypress logging documentation has been consolidated into a single comprehensive guide:

### 👉 **[CYPRESS_TESTING_GUIDELINES.md](./CYPRESS_TESTING_GUIDELINES.md)** - START HERE

This single document now contains:

- ✅ Logging configuration setup
- ✅ Debugging procedures
- ✅ Common mistakes and solutions
- ✅ Verification procedures
- ✅ All testing best practices

---

## Quick Links to Logging Topics

**In the consolidated document, logging is covered in:**

- **[Logging Configuration Section](./CYPRESS_TESTING_GUIDELINES.md#cypress-logging-configuration)** - Complete setup and debugging guide
- **[Common Logging Issues](./CYPRESS_TESTING_GUIDELINES.md#cypress-logging-configuration)** - Troubleshooting section
- **[Debugging with Cypress UI](./CYPRESS_TESTING_GUIDELINES.md#cypress-logging-configuration)** - Best practices for debugging

---

## Legacy Documentation

The following files have been consolidated and are no longer maintained:

- ❌ `CYPRESS_LOGGING_SETUP.md` → See CYPRESS_TESTING_GUIDELINES.md § Logging Configuration
- ❌ `CYPRESS_LOGGING_VERIFICATION.md` → See CYPRESS_TESTING_GUIDELINES.md § Logging Configuration
- ❌ `CYPRESS_BEST_PRACTICES.md` → See CYPRESS_TESTING_GUIDELINES.md
- ❌ `CYPRESS_TESTING_BEST_PRACTICES.md` → See CYPRESS_TESTING_GUIDELINES.md

**This index file is retained for historical reference.**

---

## 📚 Documentation Files (Legacy - Reference Only)

**File:** `.tasks/TESTING_GUIDELINES.md` (Section: Cypress Logging Configuration)

**Use this when:**

- Need comprehensive explanation
- Want to understand why things are configured this way
- Planning to change configuration
- Writing new testing documentation

**Contains:**

- Complete configuration details
- Debugging methods
- When to adjust settings
- Integration with other tools

---

### 4. Success Story

**File:** `.tasks/38111-workspace-operation-wizard/SESSION_LOGGING_SETUP_SUCCESS.md`

**Use this when:**

- Want to see real example of logging solving problems
- Need to understand the discovery process
- Explaining to others why this matters
- Learning from past mistakes

**Contains:**

- What was accomplished
- Key discoveries
- Before/after comparison
- Lessons learned

---

## 🚨 Critical Information

### The Problem We Solved

**Before proper logging:**

```
AssertionError: 1 accessibility violation was detected: expected 1 to equal 0
```

☝️ **USELESS!** Can't fix what you can't see!

**After proper logging:**

```
a11y error! scrollable-region-focusable on 1 Node
log scrollable-region-focusable, [.chakra-card__body], <div class="chakra-card__body css-1att3eq">
```

☝️ **PERFECT!** Exact problem, exact location, immediate fix!

---

### Current Configuration (TL;DR)

**Three things must be right:**

1. **cypress.config.ts:**

   ```typescript
   printLogsToConsole: 'always'
   retries: {
     runMode: 0
   }
   ```

2. **package.json:**

   ```json
   "cypress:run:component": "cypress run --component"  // NO -q flag
   ```

3. **cypress/support/component.ts:**
   ```typescript
   // ❌ DO NOT ADD: installLogsCollector()
   ```

---

### Common Mistakes

❌ **Mistake 1:** Adding `installLogsCollector()` to support file → Breaks all tests  
❌ **Mistake 2:** Using `-q` flag → Silences all output  
❌ **Mistake 3:** Setting `printLogsToConsole: 'never'` → Can't see failures  
❌ **Mistake 4:** Disabling accessibility rules instead of fixing bugs

---

## 🎯 Quick Start Guide

### For New AI Agent/Developer

**Step 1:** Verify configuration is correct

```bash
# Run verification script
grep "printLogsToConsole: 'always'" cypress.config.ts
grep '"cypress:run:component": "cypress run --component"' package.json
grep "retries: { runMode: 0" cypress.config.ts
```

**Step 2:** Run a test to verify logging works

```bash
pnpm cypress:run:component --spec "src/modules/Workspace/components/wizard/WizardSelectionPanel.spec.cy.tsx"
```

**Step 3:** If needed, open Cypress UI for detailed debugging

```bash
pnpm cypress:open:component
# Then: F12 → Console tab
```

**Step 4:** Reference documentation as needed

- Quick answers: `.tasks/CYPRESS_LOGGING_SETUP.md`
- Verification: `.tasks/CYPRESS_LOGGING_VERIFICATION.md`
- Full details: `.tasks/TESTING_GUIDELINES.md`

---

### For Debugging Accessibility Issues

**Step 1:** Run test in UI mode (not terminal)

```bash
pnpm cypress:open:component
```

**Step 2:** Open browser DevTools

- Press F12
- Go to Console tab

**Step 3:** Look for detailed output

- `a11y error!` messages
- Rule names (e.g., `color-contrast`, `scrollable-region-focusable`)
- Affected elements with CSS selectors
- DOM snapshots

**Step 4:** Fix the actual bug

- Don't disable the rule!
- Fix the component
- Test passes without exceptions

---

## 📖 Read This First (Priority Order)

**If you have 2 minutes:**
Read: `.tasks/CYPRESS_LOGGING_SETUP.md` → "Quick Reference" section

**If you have 5 minutes:**
Read: `.tasks/CYPRESS_LOGGING_VERIFICATION.md` → Run verification commands

**If you have 15 minutes:**
Read: `.tasks/CYPRESS_LOGGING_SETUP.md` completely

**If you have 30 minutes:**
Read: `.tasks/TESTING_GUIDELINES.md` → "Cypress Logging Configuration" section

**If you're curious:**
Read: `.tasks/38111-workspace-operation-wizard/SESSION_LOGGING_SETUP_SUCCESS.md` for the story

---

## 🔗 External References

- **cypress-terminal-report:** https://github.com/archfz/cypress-terminal-report
- **cypress-axe (accessibility):** https://github.com/component-driven/cypress-axe
- **Cypress Best Practices:** https://docs.cypress.io/guides/references/best-practices

---

## 📅 Update History

| Date       | Change                                       | Reason                             |
| ---------- | -------------------------------------------- | ---------------------------------- |
| 2025-11-11 | Initial configuration                        | Spent hours debugging without logs |
| 2025-11-11 | Created documentation suite                  | Ensure this never happens again    |
| 2025-11-11 | Fixed WizardSelectionPanel accessibility bug | scrollable-region-focusable        |
| 2025-11-11 | Verified 12 tests passing                    | Configuration confirmed working    |

---

## 💡 Key Takeaways

1. **Logging is not optional** - It's the difference between minutes and hours of debugging
2. **Configuration is multi-layered** - Need config file + package.json + support file coordination
3. **Documentation prevents pain** - Write it once, save hours later
4. **Fix bugs, don't hide them** - No accessibility rule exceptions unless absolutely necessary
5. **Verify, then trust** - Always run tests to confirm configuration works

---

**Bottom Line:** With proper logging configured and documented, every future developer/AI can debug efficiently!

---

## 🆘 Still Having Issues?

If logging still doesn't work after following all documentation:

1. Check `.tasks/CYPRESS_LOGGING_VERIFICATION.md` for verification steps
2. Check `.tasks/CYPRESS_LOGGING_SETUP.md` for troubleshooting
3. Check `.tasks/TESTING_GUIDELINES.md` for detailed explanation
4. Check git history for this file to see what changed
5. Ask the person who set this up (or read SESSION_LOGGING_SETUP_SUCCESS.md for context)

**Remember:** This configuration is TESTED and VERIFIED working as of Nov 11, 2025.
