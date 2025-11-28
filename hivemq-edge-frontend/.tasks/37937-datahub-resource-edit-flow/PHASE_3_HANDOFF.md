# Phase 3 Handoff: Publishing Flow Investigation Required

**Date:** November 27, 2025  
**Task:** 37937-datahub-resource-edit-flow  
**Current Status:** Phase 2 Complete, Phase 3 Ready  
**Progress:** 9/15 subtasks (60%)

---

## ✅ What's Complete

### Phase 1: Resource Editor Infrastructure ✅

- SchemaEditor & ScriptEditor (full CRUD)
- Integrated with Schema/Script tables
- 26 active tests passing

### Phase 2: Simplified Node Configuration ✅

- SchemaPanelSimplified & FunctionPanelSimplified
- Wired up via editors.config.tsx
- Select-only (no creation in designer)
- 2 active tests passing

---

## 🔍 Phase 3: Publishing Flow Updates

### Goal

Update policy validation and publishing to work with the new architecture where resources are created separately from policies.

### Subtasks (3 days estimated)

**3.1: Dry-Run Validation Updates** (1 day)

- Verify resource references resolve to actual resources
- Show clear errors if schema/script not found
- May need to update `usePolicyDryRun` or validation logic

**3.2: Publishing Logic Refactor** (1 day)

- Review `ToolbarPublish.tsx`
- Ensure resources are NOT published with policy
- Policy should only reference resources by name + version

**3.3: PolicySummaryReport Updates** (1 day)

- Show referenced resources in summary
- Display resource name + version
- Warn if resources missing

---

## ⚠️ Investigation Required BEFORE Starting Phase 3

### Critical Questions to Answer

From ARCHITECTURE.md v1.1:

**1. How are schemas referenced in policy payloads?**

- Open browser DevTools → Network tab
- Create a policy with a schema node
- Inspect the policy payload (POST/PUT request)
- Look at `DataPolicyValidator.arguments` structure
- Document: How is the schema identified? By name? ID? Version?

**2. Does ToolbarPublish already separate resource publishing?**

- Read `ToolbarPublish.tsx` implementation
- Check if resources are already published separately
- If yes: Phase 3 may be mostly validation updates
- If no: Need to refactor publishing flow

**3. What does dry-run validation check?**

- Read `usePolicyDryRun.ts` implementation
- Check what validations are performed
- Document: Does it validate resource references?
- If yes: May just need error messages
- If no: Need to add validation logic

**4. What's in PolicySummaryReport?**

- Read `PolicySummaryReport.tsx`
- Check what information is displayed
- Document: Are resources shown? How?
- Plan: What needs to be added/changed?

---

## 📋 Investigation Checklist

Before writing ANY code for Phase 3:

- [ ] Inspect policy payload in network tab (create policy with schema node)
- [ ] Document schema reference structure in payload
- [ ] Read `ToolbarPublish.tsx` (all ~250 lines)
- [ ] Document current publishing flow (resources published? how?)
- [ ] Read `usePolicyDryRun.ts`
- [ ] Document current validation logic (resource checks? what?)
- [ ] Read `PolicySummaryReport.tsx`
- [ ] Document current summary display (resources shown? how?)
- [ ] Update ARCHITECTURE.md with findings
- [ ] Create Phase 3 implementation plan based on findings

**⚠️ DO NOT skip this investigation!** Making assumptions about API contracts or current behavior will lead to wasted effort and potential bugs.

---

## 📁 Key Files for Phase 3

### Files to Read (Investigation)

1. `src/extensions/datahub/components/toolbar/ToolbarPublish.tsx` (~250 LOC)

   - Understand publishing flow
   - Check if resources published separately

2. `src/extensions/datahub/hooks/usePolicyDryRun.ts`

   - Understand validation logic
   - Check resource reference validation

3. `src/extensions/datahub/components/helpers/PolicySummaryReport.tsx`

   - Understand summary display
   - Check resource display logic

4. `src/extensions/datahub/api/hooks/*` (mutation hooks)
   - Check createSchema, createScript, createPolicy APIs
   - Understand API contracts

### Files Likely to Modify (Implementation)

1. `src/extensions/datahub/components/toolbar/ToolbarPublish.tsx`

   - May need to remove resource publishing logic
   - Or may just need validation updates

2. `src/extensions/datahub/hooks/usePolicyDryRun.ts`

   - May need to add resource validation
   - Or may just need error messages

3. `src/extensions/datahub/components/helpers/PolicySummaryReport.tsx`

   - May need to add resource display
   - Or may just need formatting updates

4. `src/extensions/datahub/locales/en/datahub.json`
   - Add error messages for missing resources
   - Add summary labels for resources

---

## 🎯 Expected Outcomes

### Scenario 1: Resources Already Separate (Best Case)

**If investigation reveals:**

- ToolbarPublish already publishes resources separately ✅
- Dry-run validation already checks resource references ✅
- Policy payload already uses resource name + version ✅

**Then Phase 3 is mostly:**

- Add better error messages
- Update PolicySummaryReport display
- Maybe add validation edge cases
- **Estimated:** 1-2 days instead of 3

### Scenario 2: Publishing Needs Refactor (Moderate Case)

**If investigation reveals:**

- ToolbarPublish publishes resources WITH policy ❌
- Resources need to be pre-published ❌
- But API contracts already support this ✅

**Then Phase 3 involves:**

- Refactor ToolbarPublish to require pre-published resources
- Add validation for resource existence
- Update PolicySummaryReport
- **Estimated:** 3 days as planned

### Scenario 3: API Changes Required (Worst Case)

**If investigation reveals:**

- API doesn't support separate resource publishing ❌
- API contracts need to change ❌

**Then:**

- This is OUT OF SCOPE (backend changes needed)
- May need to revert some Phase 2 changes
- Or add temporary "publish resources on demand" logic
- **Estimated:** Need to discuss with team

---

## 🚨 Red Flags to Watch For

### During Investigation

1. **API contract assumptions in ARCHITECTURE.md that are wrong**

   - If schema references work differently than documented
   - If resources can't be published separately
   - If validation is backend-only

2. **Circular dependencies**

   - If policy needs resources to validate
   - If resources need policy context to create
   - If dry-run requires both to exist

3. **Versioning complexities**
   - If policy locks to specific version (good!)
   - If policy uses "latest" version (problematic!)
   - If version updates break existing policies (bad!)

### During Implementation

1. **Breaking existing policies**

   - Test loading old policies
   - Test editing old policies
   - Test publishing old policies
   - Ensure backward compatibility

2. **Race conditions**

   - Resource deleted while policy references it
   - Resource version changed while policy uses it
   - Multiple policies referencing same resource

3. **Error handling**
   - Clear messages for missing resources
   - Recovery options (create resource? change reference?)
   - Validation failures during publish

---

## 📊 Success Criteria for Phase 3

### Functional Requirements

- [ ] Policies reference resources by name + version
- [ ] Dry-run validates resource references exist
- [ ] Publishing only publishes policy (not resources)
- [ ] Clear errors if resource not found
- [ ] PolicySummaryReport shows referenced resources
- [ ] Backward compatible with existing policies

### Non-Functional Requirements

- [ ] No API contract changes required
- [ ] No breaking changes to existing flows
- [ ] Clear error messages guide users to fix
- [ ] Performance not degraded

### Testing Requirements

- [ ] Component tests for validation logic
- [ ] Integration tests for publishing flow
- [ ] E2E tests for complete flow (create resource → create policy → publish)
- [ ] Backward compatibility tests (load/edit/publish old policies)

---

## 🎓 Lessons from Phases 1 & 2

### What Worked Well

1. **Investigation before implementation**

   - Understanding existing architecture first
   - Reading guidelines before coding
   - Using grep/file_search to find examples

2. **Incremental approach**

   - Build components independently
   - Test each component before integration
   - Wire up at the end (minimal changes)

3. **Configuration over modification**
   - Made ResourceNameCreatableSelect configurable (not duplicated)
   - Used panel registry for wiring (not modifying nodes)
   - Kept changes localized

### What to Avoid

1. **Assumptions about API contracts**

   - Don't assume payload structure
   - Don't assume backend behavior
   - Always verify in network tab or code

2. **Over-engineering**

   - Phase 1 skipped ResourceEditorDrawer base (not needed)
   - Subtask 2.4 was just config changes (no node modifications)
   - Keep it simple!

3. **Rushing to code**
   - Investigation phase is crucial
   - Understanding before implementing
   - Reading docs before writing

---

## 🚀 Next Agent Instructions

### Step 1: Investigation (1-2 hours)

1. **Read the publishing flow**

   ```bash
   # Open these files and understand the flow
   - src/extensions/datahub/components/toolbar/ToolbarPublish.tsx
   - src/extensions/datahub/hooks/usePolicyDryRun.ts
   - src/extensions/datahub/components/helpers/PolicySummaryReport.tsx
   ```

2. **Inspect actual policy payloads**

   ```
   1. Start dev server: pnpm dev
   2. Open browser → DataHub → Policy Designer
   3. Open DevTools → Network tab
   4. Create policy with schema node
   5. Save/publish policy
   6. Inspect POST/PUT request payload
   7. Document structure (especially schema references)
   ```

3. **Document findings**
   ```
   Update ARCHITECTURE.md with:
   - Actual policy payload structure (with example)
   - Current publishing flow (step-by-step)
   - Current validation logic (what's checked?)
   - Current summary display (what's shown?)
   - What needs to change for Phase 3
   ```

### Step 2: Planning (30 min)

Based on investigation findings:

1. Confirm which files need changes
2. Identify specific functions to modify
3. Plan validation logic (if needed)
4. Plan error messages (i18n keys)
5. Update Phase 3 estimates if needed

### Step 3: Implementation (2-3 days)

Follow the plan from Step 2:

1. Make changes incrementally
2. Test each change
3. Add tests for new logic
4. Update i18n keys
5. Verify backward compatibility

### Step 4: Verification (1 hour)

1. Run component tests
2. Run E2E tests (if any)
3. Manual testing:
   - Create resource → Create policy → Publish ✅
   - Create policy with missing resource → Validation error ✅
   - Load old policy → Still works ✅
4. Document results

---

## 📚 Reference Documents

**Task Documentation:**

- `.tasks/37937-datahub-resource-edit-flow/TASK_BRIEF.md` - Original requirements
- `.tasks/37937-datahub-resource-edit-flow/TASK_SUMMARY.md` - Progress tracking
- `.tasks/37937-datahub-resource-edit-flow/ARCHITECTURE.md` - Technical design (needs update!)
- `.tasks/37937-datahub-resource-edit-flow/VISUAL_SUMMARY.md` - What we built
- `.tasks/37937-datahub-resource-edit-flow/SESSION_6_PHASE2_COMPLETE.md` - Phase 2 summary

**Project Guidelines:**

- `.tasks/DATAHUB_ARCHITECTURE.md` - Core DataHub architecture
- `.tasks/TESTING_GUIDELINES.md` - Test requirements
- `.tasks/DESIGN_GUIDELINES.md` - UI patterns

---

**Status:** Phase 2 Complete, Investigation Phase Required  
**Next:** Read code + inspect payloads BEFORE coding  
**Estimated Phase 3 Duration:** 2-3 days (after investigation)  
**Last Updated:** November 27, 2025
