# Reporting Strategy for Task Documentation

## Document Lifecycle

### 1. TASK_BRIEF.md

- **Created:** At task start (by user or AI)
- **Modified:** NEVER - this is the original requirement
- **Purpose:** Defines the objective, context, and acceptance criteria
- **Owner:** User

### 2. TASK_SUMMARY.md

- **Created:** At task start (by AI after analyzing TASK_BRIEF)
- **Modified:** By APPENDING sections at the bottom
- **Purpose:** Contains initial analysis, proposed subtasks, and progress tracking
- **Structure:**

  ```
  ## Overview
  ## Error Summary by File (initial analysis)
  ## Proposed Subtasks
  ## Execution Order

  --- APPEND NEW SECTIONS BELOW ---

  ## Progress Updates (append only)
  ### Update YYYY-MM-DD HH:MM
  - What was done
  - Current status
  ```

### 3. CONVERSATION_SUBTASK_N.md

- **Created:** When starting each subtask
- **Modified:** By APPENDING sections as work progresses
- **Purpose:** Detailed work log for a specific subtask
- **Naming:** N = 1, 2, 3, 4, 5... (sequential, matches subtask number from plan)
- **Structure:**

  ```
  # Subtask N: [Name] - [Status]

  ## Objective
  ## Files to Fix
  ## Work Log

  --- APPEND SECTIONS AS WORK PROGRESSES ---

  ## Additional Fixes (if needed)
  ## Issues Encountered
  ## Final Verification
  ## Status: COMPLETE ✅
  ```

### 4. TASK_COMPLETE.md

- **Created:** ONLY when ALL subtasks are verified complete by user
- **Modified:** NEVER
- **Purpose:** Final summary of all work done
- **Created by:** AI after user confirms success

## Rules

### DO:

✅ Create TASK_BRIEF and TASK_SUMMARY at start  
✅ Create CONVERSATION_SUBTASK_N for each subtask  
✅ APPEND to existing documents (don't rewrite)  
✅ Wait for user verification before creating TASK_COMPLETE  
✅ Keep subtask numbering consistent with the plan  
✅ Create additional specialized documents when needed (see below)

### DON'T:

❌ Create TASK_COMPLETE before user confirms all errors fixed  
❌ Create random documents like PHASE_3_COMPLETE, EXECUTIVE_SUMMARY, FINAL_VERIFICATION  
❌ Rewrite existing sections (only append)  
❌ Create ADDITIONAL_FIX as separate document (add to relevant CONVERSATION_SUBTASK_N)  
❌ Mark subtasks complete in TASK_SUMMARY without user verification

---

## Additional Documents (When Needed)

Beyond the core workflow documents, you SHOULD create additional specialized documents when appropriate:

### Resource & Metrics Documents

✅ **RESOURCE_USAGE.md** - Track API calls, token usage, costs  
✅ **COVERAGE_MATRIX.md** - Track test coverage progress  
✅ **PERFORMANCE_METRICS.md** - Track build times, test execution times  
✅ **ERROR_LOG.md** - Track recurring errors and solutions

### Deliverables & Handoff Documents

✅ **PULL_REQUEST.md** - PR description ready to copy/paste  
✅ **RELEASE_NOTES.md** - User-facing changes for this task  
✅ **MIGRATION_GUIDE.md** - Breaking changes and upgrade steps  
✅ **API_CHANGES.md** - Document API modifications

### Planning & Reference Documents

✅ **DESIGN_DECISIONS.md** - Major architectural choices and rationale  
✅ **ALTERNATIVES_CONSIDERED.md** - Options evaluated but not chosen  
✅ **TECHNICAL_DEBT.md** - Known issues and future improvements  
✅ **DEPENDENCIES.md** - External libraries added/removed

### Context & Learning Documents

✅ **LESSONS_LEARNED.md** - Insights from challenges encountered  
✅ **TESTING_STRATEGY.md** - Test approach for this specific task  
✅ **TROUBLESHOOTING.md** - Common issues and solutions

**Key Principle:** Create any document that adds value. The core workflow (TASK_BRIEF → TASK_SUMMARY → CONVERSATION_SUBTASK_N → TASK_COMPLETE) should not be polluted with completion documents before verification, but specialized documents are encouraged.

---

## Document Hierarchy

```
.tasks/{task-id}/
├── TASK_BRIEF.md              (immutable - original requirements)
├── TASK_SUMMARY.md            (append-only - plan + progress updates)
├── CONVERSATION_SUBTASK_1.md  (append-only - detailed work log)
├── CONVERSATION_SUBTASK_N.md  (append-only - detailed work log)
├── TASK_COMPLETE.md           (created ONLY after user verification)
│
└── Additional documents (create as needed):
    ├── PULL_REQUEST.md
    ├── RESOURCE_USAGE.md
    ├── COVERAGE_MATRIX.md
    ├── DESIGN_DECISIONS.md
    ├── LESSONS_LEARNED.md
    └── ...any other specialized documents
```

## Progress Tracking

Track progress by appending to TASK_SUMMARY.md:

```markdown
---

## Progress Update - October 28, 2025 14:30

### Completed

- ✅ Subtask 1: [description] - See CONVERSATION_SUBTASK_1.md
- ✅ Subtask 2: [description] - See CONVERSATION_SUBTASK_2.md

### In Progress

- 🔄 Subtask 3: [description] - See CONVERSATION_SUBTASK_3.md

### Remaining

- ⏳ Subtask 4: [description]
- ⏳ Subtask 5: [description]

### Error Count

- Start: 24 errors
- Fixed: 19 errors
- Remaining: 5 errors
```

## Verification Process

1. AI makes changes
2. AI asks user to verify: "Please run `npx tsc -b` and confirm the error count"
3. User provides actual results
4. If errors remain: Continue fixing, append to current CONVERSATION_SUBTASK_N
5. If all errors resolved: User says "all clear"
6. ONLY THEN: Create TASK_COMPLETE.md

## Anti-Patterns to Avoid

❌ Creating completion documents before user verification  
❌ Creating multiple summary documents (EXECUTIVE_SUMMARY, FINAL_VERIFICATION, etc.)  
❌ Claiming success without checking `tsc -b` output  
❌ Rewriting TASK_SUMMARY instead of appending  
❌ Creating PHASE_N documents (use CONVERSATION_SUBTASK_N instead)  
❌ Orphan documents like ADDITIONAL_FIX (merge into relevant subtask)

## Example Workflow

1. User creates TASK_BRIEF
2. AI analyzes and creates TASK_SUMMARY with plan
3. AI creates CONVERSATION_SUBTASK_1.md and starts work
4. AI appends fixes to CONVERSATION_SUBTASK_1.md
5. AI asks user to verify
6. User reports: "Still 5 errors"
7. AI appends to CONVERSATION_SUBTASK_1.md with additional fixes
8. AI asks user to verify again
9. User confirms: "Subtask 1 complete"
10. AI appends status to TASK_SUMMARY.md
11. AI creates CONVERSATION_SUBTASK_2.md
12. ... repeat for all subtasks ...
13. User confirms: "All errors resolved"
14. AI creates TASK_COMPLETE.md
15. Done!

---

**Key Principle:** Append, don't rewrite. Verify with user, don't assume.
