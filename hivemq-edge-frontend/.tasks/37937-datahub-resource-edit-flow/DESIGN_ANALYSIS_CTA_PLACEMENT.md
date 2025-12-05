# Design Analysis: CTA Placement in DataHub

**Date:** November 26, 2025  
**Task:** 37937-datahub-resource-edit-flow  
**Status:** Design Decision Needed

---

## 🎨 Problem Statement

After implementing resource editors (schemas and scripts), we now have **three equal creation actions** competing for user attention, but the UI doesn't reflect this new reality.

### Current Situation

**Page Structure:**

```
DataHubPage (top-level)
├─ Header with "Create New Policy" (Primary CTA - positioned top-right)
└─ DataHubListings (Tabs)
   ├─ Policies Tab
   ├─ Schemas Tab (NEW: "Create New Schema" in footer, Secondary)
   └─ Scripts Tab (NEW: "Create New Script" in footer, Secondary)
```

**Historical Context:**

- **Before refactor**: Only policies were editable → Single primary CTA made sense
- **After refactor**: All three resource types are now editable → Need to reconsider CTA strategy

---

## ❌ Problems with Current Design

### 1. **Contextual Mismatch**

The top-level "Create New Policy" CTA is **always visible** regardless of which tab is active. This creates confusion:

- When viewing Schemas tab: "Why does the page say 'Create Policy'?"
- When viewing Scripts tab: Same confusion
- **User mental model broken**: The primary action doesn't match the current context

### 2. **Visual Hierarchy Issues**

- **Before refactor**: One primary CTA made sense (only policies were editable)
- **After refactor**: Three equal creation actions compete for attention
- **Current state**: Primary CTA is divorced from content (sits above tabs, refers to different tab)

### 3. **Discoverability Problems**

- Table footer buttons are **less discoverable** than top-level CTAs
- Users scanning for "How do I create a schema?" won't naturally look at table footer
- Footer position implies "less important action"
- New users may not discover schema/script creation easily

### 4. **Inconsistency Across Tabs**

- Policies: Primary CTA at page level
- Schemas/Scripts: Secondary buttons in footer
- **Inconsistent mental model** per tab
- Users need to learn different patterns for each resource type

---

## 📐 Design Principles to Consider

### From DESIGN_GUIDELINES.md:

1. **Only ONE primary button per page/view**
2. **Button hierarchy matters**: Primary > Secondary > Tertiary
3. **Context determines importance**

### UX Best Practices:

1. **Progressive Disclosure**: Most important actions should be most visible
2. **Consistency**: Similar actions should have similar treatments
3. **Context Awareness**: UI should reflect current user context
4. **Scannability**: Users should find actions where they expect them
5. **Principle of Least Surprise**: UI should behave as users expect

---

## 📊 Three Design Options

### Option A: Tab-Aware Primary CTA ⭐ **(Recommended)**

**Change the top-level CTA to be context-aware based on active tab:**

```tsx
// DataHubPage.tsx or DataHubListings.tsx
const [activeTab, setActiveTab] = useState(0)

const ctaConfig = {
  0: { label: 'Create New Policy', action: handleCreatePolicy },
  1: { label: 'Create New Schema', action: handleCreateSchema },
  2: { label: 'Create New Script', action: handleCreateScript }
}

// In PageContainer cta prop:
<Button variant="primary" onClick={ctaConfig[activeTab].action}>
  {ctaConfig[activeTab].label}
</Button>
```

#### Implementation Approaches

**Approach A1: Move CTA to Tab Level**

```tsx
<TabPanel>
  <Flex justifyContent="space-between" mb={3}>
    <Text>{description}</Text>
    <Button variant="primary" onClick={handleCreate}>
      Create New {resourceType}
    </Button>
  </Flex>
  <Table />
</TabPanel>
```

**Approach A2: Keep CTA at Page Level with Tab State**

```tsx
<PageContainer
  cta={
    <Button variant="primary" onClick={currentTabCreateAction}>
      {currentTabCreateLabel}
    </Button>
  }
>
  <Tabs onChange={(index) => setActiveTab(index)}>{/* tabs content */}</Tabs>
</PageContainer>
```

#### Pros:

- ✅ Maintains single primary CTA pattern (respects design guidelines)
- ✅ Context-aware: CTA always matches active tab content
- ✅ Maximum discoverability (top-right, prominent position)
- ✅ Consistent pattern across all tabs (equal treatment)
- ✅ Clear visual hierarchy maintained
- ✅ Aligns with user mental model: "I'm viewing schemas → I can create a schema"

#### Cons:

- ⚠️ Requires state management between page and tabs component
- ⚠️ CTA label changes when switching tabs (could be slightly jarring)
- ⚠️ Need to handle draft policy state edge case

#### Best for:

Users who need clear, obvious creation actions with consistent patterns

---

### Option B: Multiple CTAs (Three Primary Buttons)

**Show all three creation buttons at page level:**

```tsx
// Top of page or in toolbar above tabs
<HStack spacing={3}>
  <Button variant="primary" size="sm">
    Create Policy
  </Button>
  <Button variant="primary" size="sm">
    Create Schema
  </Button>
  <Button variant="primary" size="sm">
    Create Script
  </Button>
</HStack>
```

#### Pros:

- ✅ All actions always visible (no context switching)
- ✅ Fast access to any creation action from anywhere
- ✅ No state management complexity
- ✅ Clear at a glance what can be created

#### Cons:

- ❌ **Violates "one primary CTA" design guideline**
- ❌ Creates visual noise (three competing primary buttons)
- ❌ Unclear which is "most important" action
- ❌ Takes up significant visual real estate
- ❌ May overwhelm new users
- ❌ Primary button loses meaning if everything is primary

#### Best for:

Power users who frequently switch between creating different resource types (edge case)

---

### Option C: Keep Current (Footer Buttons) + Improve Discoverability

**Keep footer buttons but add better signposting and styling:**

```tsx
// 1. Empty state when no items (improve discoverability for first-time users)
<EmptyState
  icon={<LuFileCode />}
  title="No schemas yet"
  description="Schemas define the structure of your data"
  action={
    <Button variant="primary" onClick={handleCreateNew}>
      Create Your First Schema
    </Button>
  }
/>

// 2. More prominent footer styling
<Button
  leftIcon={<LuPlus />}
  variant="secondary"
  size="md"  // Larger than current 'sm'
  width="full" // Full width in footer cell for better visibility
  onClick={handleCreateNew}
>
  Create New Schema
</Button>
```

#### Pros:

- ✅ Maintains clean header area
- ✅ Actions contextualized within their tables
- ✅ No state management complexity
- ✅ Policy CTA remains primary (reflects system hierarchy: Policies > Resources)
- ✅ Minimal code changes required

#### Cons:

- ⚠️ Still less discoverable than top-level CTA
- ⚠️ Empty state needed for first-time users (adds complexity)
- ⚠️ Inconsistent with policy creation pattern (different treatment)
- ⚠️ Users may miss footer buttons when scanning page
- ⚠️ Doesn't solve the contextual mismatch problem

#### Best for:

Systems where there's a clear hierarchy (Policies are primary workflow, resources are secondary)

---

## 🎯 Recommendation: **Option A - Tab-Aware Primary CTA**

### Why This is the Best Solution

1. **Respects User Context**: CTA label and action always matches what user is currently viewing
2. **Maintains Design Principles**: Preserves "one primary button per view" guideline
3. **Best Discoverability**: Top-right position is standard for primary page actions
4. **Consistent User Experience**: All three resource types get equal, consistent treatment
5. **Aligns with User Mental Model**: "I'm looking at schemas → the obvious action is to create a schema"
6. **Future-Proof**: Easy to add more resource types if needed

### Key Insight

> **Now that all three resource types are editable, they deserve equal treatment in the UI.**
>
> The primary CTA should represent **"the most important action in THIS context"** - not "the most important action overall".

Context matters in UI design. When a user is viewing the Schemas tab, schema creation _is_ the most important action for that context.

---

## 🔧 Implementation Strategy

### Phase 1: Move CTA Logic to Tab-Aware Component

**Option 1: CTA at Tab Panel Level (Simpler)**

```tsx
// In DataHubListings.tsx - each TabPanel
<TabPanel>
  <Flex justifyContent="space-between" alignItems="center" mb={3}>
    <Text>{t('Listings.tabs.schema.description')}</Text>
    <Button variant="primary" onClick={handleCreateSchema}>
      {t('Listings.schema.action.create')}
    </Button>
  </Flex>
  <SchemaTable onDeleteItem={handleOnDelete} />
</TabPanel>
```

**Option 2: CTA at Page Level with Tab State (More Complex)**

```tsx
// DataHubPage would need to:
// 1. Track active tab via state lifting
// 2. Pass down create handlers
// 3. Conditionally render CTA based on tab
```

### Phase 2: Remove Footer Buttons

- Remove `footer:` property from Actions column in SchemaTable
- Remove `footer:` property from Actions column in ScriptTable
- Remove `handleCreateNew` from column dependencies

### Phase 3: Handle Edge Cases

- **Draft policy exists**: Show draft warning when appropriate
- **No permissions**: Disable CTA if user can't create resources
- **Loading state**: Show skeleton button while data loads

### Phase 4: Update Tests

- Update SchemaTable tests: button no longer in footer
- Update ScriptTable tests: button no longer in footer
- Add tests for new CTA positioning

---

## 🔄 Alternative: Hybrid Approach (If Tab-Aware is Too Complex)

If implementing tab-aware CTA is deemed too complex or risky:

### Compromise Solution:

- **Keep Policy CTA at page level** (it's arguably the "main" feature)
- **Add prominent CTAs above each table** (not in footer, but below description)
- Accept slight inconsistency since Policies are the primary workflow

```tsx
<TabPanel>
  <Text mb={3}>{description}</Text>
  <Flex justifyContent="flex-end" mb={3}>
    <Button variant="primary" onClick={handleCreate}>
      {t('Listings.schema.action.create')}
    </Button>
  </Flex>
  <SchemaTable />
</TabPanel>
```

**Trade-offs:**

- ✅ Better than footer buttons (more discoverable)
- ✅ Simpler implementation
- ⚠️ Still have multiple primary buttons visible at once
- ⚠️ Doesn't fully solve the contextual mismatch

---

## 📋 Decision Checklist

Before implementing, consider:

- [ ] How often do users switch between tabs vs. staying in one?
- [ ] Is there a clear workflow hierarchy (Policies > Schemas/Scripts)?
- [ ] How important is consistency across all three resource types?
- [ ] What's the development effort vs. UX improvement trade-off?
- [ ] Have we validated this with users or stakeholders?

---

## 🚦 Next Steps

1. **Decide on approach**: A (tab-aware), B (multiple), C (keep current + improve), or Hybrid
2. **Prototype**: Create quick mockup or POC of chosen approach
3. **Validate**: Get feedback from team/stakeholders
4. **Implement**: Make changes to DataHubPage/DataHubListings
5. **Test**: Update all affected tests
6. **Document**: Update DESIGN_GUIDELINES.md with pattern decision

---

## 📚 Related Guidelines

- **DESIGN_GUIDELINES.md**: Button variants and hierarchy
- **DATAHUB_ARCHITECTURE.md**: DataHub structure and patterns
- **UX Principles**: Progressive disclosure, consistency, context awareness

---

**Status:** Awaiting decision on which option to implement  
**Recommendation:** Option A - Tab-Aware Primary CTA  
**Last Updated:** November 26, 2025
