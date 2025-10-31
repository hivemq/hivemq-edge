# Parallel Task Execution Template

**Purpose:** Guide for orchestrating multiple AI agents working on independent subtasks simultaneously.

**Last Updated:** October 30, 2025

---

## Quick Start

1. Copy this template to your task directory: `.tasks/{task-id}-{task-name}/PARALLEL_EXECUTION_PLAN.md`
2. Fill in the subtask breakdown
3. Assign agents to independent subtasks
4. Define merge order
5. Create communication log
6. Execute in parallel
7. Merge according to plan

---

## Task Overview

**Task ID:** [e.g., 25337]  
**Task Name:** [e.g., workspace-auto-layout]  
**Total Subtasks:** [e.g., 4]  
**Parallel Groups:** [e.g., 2 groups of 2]  
**Estimated Time Savings:** [e.g., 40-50%]  
**Start Date:** [Date]  
**Target Completion:** [Date]

---

## Subtask Breakdown

### Subtask X.1 - [Name]

**Status:** ⏳ Not Started | 🔄 In Progress | ✅ Complete  
**Assigned Agent:** Agent-1  
**Branch:** `feature/{task-id}-subtask-1`  
**Priority:** P0 | P1 | P2  
**Estimated Duration:** [e.g., 2 hours]  
**Dependencies:** None | [List dependencies]

**Files to Modify:**

- `src/path/to/file1.ts`
- `src/path/to/file2.tsx`
- `cypress/e2e/test.spec.cy.ts`

**Deliverables:**

- [ ] [Specific deliverable 1]
- [ ] [Specific deliverable 2]
- [ ] Tests passing (100%)
- [ ] TypeScript errors = 0
- [ ] Prettier + lint run
- [ ] `CONVERSATION_SUBTASK_X_1.md` created
- [ ] Session log created: `{task}_X_1_{description}.md`

**Success Criteria:**

- [Specific measurable criterion 1]
- [Specific measurable criterion 2]

**Notes:**

- [Any special considerations]
- [Patterns to follow]
- [Dependencies to be aware of]

---

### Subtask X.2 - [Name]

**Status:** ⏳ Not Started | 🔄 In Progress | ✅ Complete  
**Assigned Agent:** Agent-2  
**Branch:** `feature/{task-id}-subtask-2`  
**Priority:** P0 | P1 | P2  
**Estimated Duration:** [e.g., 2 hours]  
**Dependencies:** None | [List dependencies]

**Files to Modify:**

- `src/path/to/file3.ts`
- `src/path/to/file4.tsx`

**Deliverables:**

- [ ] [Specific deliverable 1]
- [ ] [Specific deliverable 2]
- [ ] Tests passing (100%)
- [ ] TypeScript errors = 0
- [ ] Prettier + lint run
- [ ] `CONVERSATION_SUBTASK_X_2.md` created
- [ ] Session log created: `{task}_X_2_{description}.md`

**Success Criteria:**

- [Specific measurable criterion 1]
- [Specific measurable criterion 2]

**Notes:**

- [Any special considerations]

---

### Subtask X.3 - [Name]

**Status:** ⏳ Not Started | 🔄 In Progress | ✅ Complete  
**Assigned Agent:** Agent-3  
**Branch:** `feature/{task-id}-subtask-3`  
**Priority:** P0 | P1 | P2  
**Estimated Duration:** [e.g., 1.5 hours]  
**Dependencies:** None | [List dependencies]

**Files to Modify:**

- `src/path/to/file5.ts`
- `.tasks/{task-id}/documentation.md`

**Deliverables:**

- [ ] [Specific deliverable 1]
- [ ] [Specific deliverable 2]
- [ ] Tests passing (100%)
- [ ] TypeScript errors = 0
- [ ] Prettier + lint run
- [ ] `CONVERSATION_SUBTASK_X_3.md` created
- [ ] Session log created: `{task}_X_3_{description}.md`

**Success Criteria:**

- [Specific measurable criterion 1]

**Notes:**

- [Any special considerations]

---

## Independence Verification

**Checklist for ensuring subtasks can run in parallel:**

- [ ] **No file overlaps** - Each subtask modifies different files
- [ ] **No shared dependencies** - Each subtask can complete independently
- [ ] **Clear interfaces** - If subtasks interact, interfaces are defined upfront
- [ ] **Separate test files** - No test file is modified by multiple subtasks
- [ ] **No shared config** - package.json, tsconfig.json modifications are coordinated
- [ ] **Documentation split** - Each subtask has its own conversation/session logs

**Potential Conflicts Identified:**

- [List any files that might have conflicts]
- [List any shared state concerns]
- [List any integration points]

**Conflict Resolution Strategy:**

- [How conflicts will be resolved]
- [Who has priority if conflicts arise]

---

## Agent Assignment

### Agent 1

**Subtasks:** X.1, X.4  
**Focus Area:** [e.g., Component tests]  
**Required Context:**

- Read: `TESTING_GUIDELINES.md`
- Read: `CYPRESS_TESTING_BEST_PRACTICES.md`
- Read: Existing component test patterns

**Communication Channel:** `.tasks-log/{task}_X_AGENT1_LOG.md`

### Agent 2

**Subtasks:** X.2  
**Focus Area:** [e.g., E2E tests]  
**Required Context:**

- Read: `TESTING_GUIDELINES.md`
- Read: Existing E2E test patterns
- Read: Page object files

**Communication Channel:** `.tasks-log/{task}_X_AGENT2_LOG.md`

### Agent 3

**Subtasks:** X.3  
**Focus Area:** [e.g., Bug fixes]  
**Required Context:**

- Read: TypeScript configuration
- Read: Existing type definitions

**Communication Channel:** `.tasks-log/{task}_X_AGENT3_LOG.md`

---

## Merge Strategy

### Merge Order

**Critical:** Subtasks MUST be merged in this order to avoid conflicts and ensure dependencies are met.

1. **Subtask X.3** - [Name] (MERGE FIRST)

   - **Why first:** [e.g., Fixes foundational TypeScript issues]
   - **Agent:** Agent-3
   - **Branch:** `feature/{task-id}-subtask-3`
   - **Merge to:** `main`
   - **After merge:** All other agents rebase

2. **Subtask X.1** - [Name] (MERGE SECOND)

   - **Why second:** [e.g., Establishes test patterns for others]
   - **Agent:** Agent-1
   - **Branch:** `feature/{task-id}-subtask-1`
   - **Merge to:** `main`
   - **After merge:** Remaining agents rebase

3. **Subtask X.2** - [Name] (MERGE THIRD)
   - **Why third:** [e.g., Depends on patterns from X.1]
   - **Agent:** Agent-2
   - **Branch:** `feature/{task-id}-subtask-2`
   - **Merge to:** `main`
   - **After merge:** Final verification

### Merge Procedure

Each agent must follow this procedure before merging:

```bash
# 1. Ensure all deliverables complete
# 2. Run quality gates
npx prettier --write {all-modified-files}
npm run lint:all
npm test -- {relevant-tests}
npx tsc -b

# 3. Rebase on latest main
git checkout main
git pull origin main
git checkout feature/{task-id}-subtask-X
git rebase main

# 4. Resolve conflicts if any
# 5. Force push (if rebased)
git push origin feature/{task-id}-subtask-X --force-with-lease

# 6. Create PR
# 7. Wait for approval
# 8. Merge to main
# 9. Notify next agent in merge order
```

### Conflict Resolution Rules

**If merge conflicts occur:**

1. **First finisher has priority** - Agent who finished first gets to merge with minimal changes
2. **Later agents adapt** - Subsequent agents rebase and adapt their changes
3. **Communication required** - Use agent communication log to coordinate
4. **Shared files** - If conflict on shared file (e.g., TASK_SUMMARY.md), resolve via communication

**Blocking Issues:**

- If Agent 3 (first merge) is blocked, communicate immediately
- If Agent 3's changes are required by others, others must wait
- Use communication log to coordinate timing

---

## Communication Protocol

### Communication Log

**File:** `.tasks-log/{task}_X_AGENT_COMMUNICATION.md`

**Format:**

```markdown
# Task {ID} - Agent Communication Log

## Agent {N} → All ({Timestamp})

Message content here...

## Agent {N} → Agent {N} ({Timestamp})

Direct message content...
```

### Message Types

**🔔 Pattern Decision** - Architectural or pattern choice that affects others

```markdown
## Agent 1 → All (10:30 AM)

🔔 **Pattern Decision:** Component tests

Using pattern:

- cy.mountWithProviders()
- EdgeFlowProvider wrapper
- config.features.WORKSPACE_AUTO_LAYOUT = true

Please follow same pattern for consistency.
```

**❓ Question** - Need clarification or input

```markdown
## Agent 2 → Agent 1 (10:45 AM)

❓ **Question:** Should we use cy.wait() for animations?
```

**✅ Response** - Answer to a question

```markdown
## Agent 1 → Agent 2 (10:50 AM)

✅ **Response:** No! Use element visibility checks instead.
See CYPRESS_TESTING_BEST_PRACTICES.md
```

**🚀 Status Update** - Progress or completion

```markdown
## Agent 3 → All (11:00 AM)

🚀 **Status Update:** TypeScript fixes complete.
Merging to main. You may need to rebase your branches.
```

**⚠️ Blocker** - Issue that affects others

```markdown
## Agent 1 → All (11:15 AM)

⚠️ **Blocker:** Found critical bug in useLayoutEngine.ts
Pausing component tests until fix is merged.
```

**🔄 Merge Notice** - Branch merged, rebase required

```markdown
## Agent 3 → All (11:30 AM)

🔄 **Merge Notice:** Subtask X.3 merged to main.
Please rebase your branches:

git checkout main && git pull origin main
git checkout feature/{task-id}-subtask-{yours}
git rebase main
```

---

## Execution Timeline

### Phase 1: Setup (15 minutes)

- [ ] Create branches for all subtasks
- [ ] Create communication log file
- [ ] Each agent reviews their assigned subtasks
- [ ] Each agent reads required context documents
- [ ] All agents acknowledge ready to start

### Phase 2: Parallel Execution (Varies by subtask)

**Agent 1:** [Estimated time]

- [ ] Start subtask X.1
- [ ] Create conversation file
- [ ] Implement deliverables
- [ ] Run tests locally
- [ ] Run quality gates
- [ ] Create session log
- [ ] Push to branch

**Agent 2:** [Estimated time]

- [ ] Start subtask X.2
- [ ] Create conversation file
- [ ] Implement deliverables
- [ ] Run tests locally
- [ ] Run quality gates
- [ ] Create session log
- [ ] Push to branch

**Agent 3:** [Estimated time]

- [ ] Start subtask X.3
- [ ] Create conversation file
- [ ] Implement deliverables
- [ ] Run tests locally
- [ ] Run quality gates
- [ ] Create session log
- [ ] Push to branch

### Phase 3: Merge Sequence (30 minutes per merge)

- [ ] **Merge 1:** Agent 3 merges first
- [ ] Agents 1 & 2 rebase on new main
- [ ] **Merge 2:** Agent 1 merges second
- [ ] Agent 2 rebases on new main
- [ ] **Merge 3:** Agent 2 merges third
- [ ] Final verification on main

### Phase 4: Finalization (30 minutes)

- [ ] Update TASK_SUMMARY.md (one agent, coordinator)
- [ ] Run full test suite on main
- [ ] Verify all deliverables complete
- [ ] Create PARALLEL_EXECUTION_REPORT.md
- [ ] Archive communication logs

---

## Quality Gates

### Individual Agent Quality Gates

Each agent MUST complete before declaring their subtask done:

- [ ] All deliverables completed
- [ ] All tests passing (100% success rate)
- [ ] TypeScript compilation clean (0 errors)
- [ ] No implicit `any` types introduced
- [ ] **⚠️ CRITICAL:** `npx prettier --write` run on ALL modified files
- [ ] **⚠️ CRITICAL:** `npm run lint:all` passing (0 errors)
- [ ] Conversation file created: `CONVERSATION_SUBTASK_X_N.md`
- [ ] Session log created: `.tasks-log/{task}_X_N_{description}.md`
- [ ] All files properly formatted
- [ ] Branch pushed to remote

### Coordination Quality Gates

Before merging:

- [ ] All agents have finished their assigned subtasks
- [ ] Communication log reviewed by all agents
- [ ] Merge order confirmed
- [ ] First agent ready to merge

After each merge:

- [ ] Merge successful (no conflicts)
- [ ] Main branch tests passing
- [ ] Other agents notified
- [ ] Other agents rebased successfully

Final quality gate:

- [ ] All subtasks merged to main
- [ ] Full test suite passing
- [ ] All documentation updated
- [ ] TASK_SUMMARY.md updated with all subtasks
- [ ] No regression introduced

---

## Coordination Checklist

### Before Starting

- [ ] All subtasks are truly independent (no file overlap)
- [ ] Each agent has their assignment clear
- [ ] Branches created for all subtasks
- [ ] Communication log initialized
- [ ] Required context documents identified
- [ ] Merge order defined and communicated
- [ ] All agents ready to start

### During Execution

- [ ] Each agent updates communication log with progress
- [ ] Pattern decisions communicated immediately
- [ ] Questions resolved quickly via communication log
- [ ] Blockers escalated immediately
- [ ] Each agent runs tests locally before pushing

### After First Completion

- [ ] First agent runs full quality gates
- [ ] First agent creates PR
- [ ] Other agents notified to prepare for rebase
- [ ] First agent merges
- [ ] All other agents rebase immediately

### After All Completions

- [ ] All agents merged in order
- [ ] Full test suite passing on main
- [ ] TASK_SUMMARY.md updated (by coordinator)
- [ ] Execution report created
- [ ] Lessons learned documented

---

## Best Practices

### DO ✅

- ✅ **Assign non-overlapping files** - No two agents modify same file
- ✅ **Communicate pattern decisions** - Use communication log for important choices
- ✅ **Run quality gates individually** - Each agent prettier + lint their own changes
- ✅ **Follow merge order strictly** - Don't merge out of order
- ✅ **Rebase immediately** - When prior agent merges, rebase right away
- ✅ **Use session logs** - Document work in `.tasks-log/` directory
- ✅ **Test locally** - Don't push broken code
- ✅ **Update conversation files** - Keep individual subtask documentation
- ✅ **Coordinate timing** - Communicate when starting/finishing
- ✅ **Read others' logs** - Stay aware of decisions made by other agents

### DON'T ❌

- ❌ **Don't edit same files** - Guaranteed merge conflicts
- ❌ **Don't modify TASK_SUMMARY.md in parallel** - Sequential only
- ❌ **Don't skip quality gates** - Prettier + lint are mandatory
- ❌ **Don't merge out of order** - Follow the defined merge sequence
- ❌ **Don't work in silence** - Use communication log
- ❌ **Don't assume** - Ask questions in communication log
- ❌ **Don't skip rebasing** - Must rebase after each merge
- ❌ **Don't push untested code** - Verify locally first
- ❌ **Don't ignore blockers** - Communicate immediately
- ❌ **Don't work on shared config files** - Coordinate changes

---

## Common Challenges & Solutions

### Challenge 1: Shared Configuration Files

**Problem:** Multiple agents need to modify `package.json` or `tsconfig.json`

**Solution:**

1. Assign config changes to ONE agent only
2. That agent merges FIRST
3. Other agents rebase and adapt

### Challenge 2: TASK_SUMMARY.md Conflicts

**Problem:** All agents want to update the summary

**Solution:**

1. Designate ONE agent as "coordinator"
2. All others complete their subtasks
3. Coordinator updates TASK_SUMMARY.md with ALL subtask info at end
4. Single atomic update

### Challenge 3: Pattern Inconsistency

**Problem:** Agents using different patterns for similar tasks

**Solution:**

1. First agent establishes pattern
2. Communicates pattern via communication log
3. Other agents follow the established pattern
4. Reference examples from first agent's code

### Challenge 4: Blocking Dependencies

**Problem:** Agent 2 realizes they depend on Agent 1's work

**Solution:**

1. Communicate blocker immediately
2. Adjust merge order if needed
3. Agent 2 pauses and works on different task
4. Resume after Agent 1 merges

### Challenge 5: Merge Conflicts

**Problem:** Despite best efforts, merge conflicts occur

**Solution:**

1. First finisher has priority
2. Later agents resolve conflicts by adapting their code
3. Use communication log to coordinate resolution
4. Test thoroughly after resolving conflicts

---

## Example: Task 25337 Parallel Execution

### Real-World Example

**Task:** 25337 - Workspace Auto-Layout Testing  
**Subtasks:** 4 independent testing subtasks

#### Subtask Breakdown

**Subtask 11.1 - Component Tests A (Agent 1)**

- Files: LayoutSelector.spec.cy.tsx, ApplyLayoutButton.spec.cy.tsx
- Duration: 2 hours
- Deliverables: 7 component tests

**Subtask 11.2 - Component Tests B (Agent 2)**

- Files: LayoutPresetsManager.spec.cy.tsx, LayoutOptionsDrawer.spec.cy.tsx
- Duration: 2.5 hours
- Deliverables: 17 component tests

**Subtask 11.3 - E2E Tests A (Agent 3)**

- Files: workspace-layout-basic.spec.cy.ts, workspace-layout-options.spec.cy.ts
- Duration: 2 hours
- Deliverables: 10 E2E tests

**Subtask 11.4 - E2E Tests B (Agent 4)**

- Files: workspace-layout-presets.spec.cy.ts, workspace-layout-shortcuts.spec.cy.ts
- Duration: 2 hours
- Deliverables: 11 E2E tests

#### Timeline

**Sequential:** 2 + 2.5 + 2 + 2 = **8.5 hours**  
**Parallel:** max(2, 2.5, 2, 2) = **2.5 hours**  
**Time Saved:** 6 hours (**70% reduction**)

#### Merge Order

1. Agent 1 (establishes component test patterns)
2. Agent 2 (follows patterns from Agent 1)
3. Agent 3 (establishes E2E test patterns)
4. Agent 4 (follows patterns from Agent 3)

---

## Post-Execution Report Template

After completing parallel execution, create: `.tasks-log/{task}_X_PARALLEL_EXECUTION_REPORT.md`

```markdown
# Task {ID} - Parallel Execution Report

## Summary

- **Task ID:** {ID}
- **Task Name:** {Name}
- **Execution Date:** {Date}
- **Total Subtasks:** {N}
- **Agents Used:** {N}
- **Success:** ✅ Yes | ❌ No (with issues)

## Timeline

- **Start Time:** {Time}
- **First Completion:** {Time} (Agent {N})
- **Last Completion:** {Time} (Agent {N})
- **Total Duration:** {Hours}
- **Estimated Sequential Time:** {Hours}
- **Time Saved:** {Hours} ({Percentage}%)

## Subtask Results

### Subtask X.1

- **Agent:** Agent-1
- **Status:** ✅ Complete
- **Duration:** {Hours}
- **Files Modified:** {Count}
- **Tests Added:** {Count}
- **Issues Encountered:** None | [Description]

### Subtask X.2

- **Agent:** Agent-2
- **Status:** ✅ Complete
- **Duration:** {Hours}
- **Files Modified:** {Count}
- **Tests Added:** {Count}
- **Issues Encountered:** None | [Description]

## Merge Statistics

- **Total Merges:** {N}
- **Merge Conflicts:** {N}
- **Conflict Resolution Time:** {Hours}
- **Rebases Required:** {N}

## Communication Effectiveness

- **Messages Exchanged:** {N}
- **Pattern Decisions:** {N}
- **Questions Asked:** {N}
- **Blockers Encountered:** {N}
- **Blockers Resolved:** {N}

## Quality Metrics

- **Tests Added:** {N}
- **Tests Passing:** {N}/{N} ({Percentage}%)
- **TypeScript Errors:** {N}
- **Lint Errors:** {N}
- **Files Formatted:** {N}

## Lessons Learned

### What Worked Well

- [Thing that worked well]
- [Another success]

### What Could Be Improved

- [Area for improvement]
- [Another challenge]

### Recommendations for Future

- [Recommendation 1]
- [Recommendation 2]

## Conclusion

[Overall assessment of the parallel execution experiment]
```

---

## Tools & Scripts

### Quick Branch Setup

```bash
#!/bin/bash
# create-parallel-branches.sh

TASK_ID=$1
SUBTASKS=("subtask-1" "subtask-2" "subtask-3")

for subtask in "${SUBTASKS[@]}"; do
  git checkout -b "feature/${TASK_ID}-${subtask}"
  git push -u origin "feature/${TASK_ID}-${subtask}"
  git checkout main
done

echo "✅ Created ${#SUBTASKS[@]} branches"
```

### Rebase All Branches

```bash
#!/bin/bash
# rebase-all-branches.sh

TASK_ID=$1
SUBTASKS=("subtask-1" "subtask-2" "subtask-3")

git checkout main
git pull origin main

for subtask in "${SUBTASKS[@]}"; do
  echo "🔄 Rebasing feature/${TASK_ID}-${subtask}..."
  git checkout "feature/${TASK_ID}-${subtask}"
  git rebase main
  if [ $? -eq 0 ]; then
    echo "✅ Rebase successful for ${subtask}"
  else
    echo "❌ Rebase failed for ${subtask} - resolve conflicts"
    exit 1
  fi
done

git checkout main
echo "✅ All branches rebased"
```

### Quality Gate Script

```bash
#!/bin/bash
# quality-gate.sh

echo "🎯 Running Quality Gates..."

# Format all modified files
echo "📝 Running prettier..."
npx prettier --write $(git diff --name-only --cached)

# Run lint
echo "🔍 Running lint..."
npm run lint:all

# Check TypeScript
echo "📘 Checking TypeScript..."
npx tsc -b

# Run tests
echo "🧪 Running tests..."
npm test

if [ $? -eq 0 ]; then
  echo "✅ All quality gates passed!"
else
  echo "❌ Quality gates failed - fix errors before pushing"
  exit 1
fi
```

---

## FAQ

**Q: How many agents can work in parallel?**  
A: As many as you have independent subtasks. Typical range: 2-4 agents.

**Q: What if agents finish at very different times?**  
A: That's fine. First finisher merges first, others rebase when ready.

**Q: Can I add more agents mid-execution?**  
A: Yes, but ensure they have clear assignment and understand the communication protocol.

**Q: What if we discover a dependency mid-execution?**  
A: Communicate via log, adjust merge order, coordinate the dependency resolution.

**Q: Should all agents wait for first agent to finish before starting?**  
A: No! All agents start simultaneously. Only merging is sequential.

**Q: How do I handle emergency changes (hotfixes)?**  
A: Pause parallel work, merge hotfix to main, all agents rebase, then resume.

**Q: What if TASK_SUMMARY.md needs updates from all agents?**  
A: Designate one "coordinator" agent who updates summary at the end with input from all.

**Q: Can parallel execution work for documentation tasks?**  
A: Yes! Different sections of documentation can be written in parallel.

---

## References

- AUTONOMY_TEMPLATE.md - General AI agent guidelines
- TESTING_GUIDELINES.md - Testing patterns and practices
- REPORTING_STRATEGY.md - Documentation standards
- Git branching best practices

---

**Use this template for any task with 3+ independent subtasks to maximize efficiency!**
