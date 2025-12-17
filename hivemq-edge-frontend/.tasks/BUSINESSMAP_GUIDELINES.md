# BusinessMap Card Creation Guidelines

This document outlines the process and design decisions for creating tickets in BusinessMap (Board 57: Edge).

## Table of Contents

- [Card Format Requirements](#card-format-requirements)
- [Card Structure Template](#card-structure-template)
- [Card Creation Process](#card-creation-process)
- [Design Decisions](#design-decisions)
- [Common Parameters](#common-parameters)
- [Known Limitations](#known-limitations)

---

## Card Format Requirements

### HTML Format (NOT Markdown)

BusinessMap cards use **HTML format**, not Markdown. The API will display raw Markdown as a single line of unformatted text.

**✅ Correct:**

```html
<p><strong>EXPECTED BEHAVIOR</strong></p>
<ul>
  <li>Item 1</li>
  <li>Item 2</li>
</ul>
```

**❌ Incorrect:**

```markdown
**EXPECTED BEHAVIOR**

- Item 1
- Item 2
```

### HTML Elements Used

| Element                  | Purpose         | Example                                         |
| ------------------------ | --------------- | ----------------------------------------------- |
| `<p>`                    | Paragraphs      | `<p>This is a paragraph</p>`                    |
| `<strong>`               | Bold text       | `<strong>IMPORTANT</strong>`                    |
| `<code>`                 | Inline code     | `<code>fileName.ts</code>`                      |
| `<pre><code>`            | Code blocks     | `<pre><code>function example() {}</code></pre>` |
| `<ul>`                   | Unordered lists | `<ul><li>Item</li></ul>`                        |
| `<ul class="todo-list">` | Checkbox lists  | See below                                       |

### Checkbox Lists (Todo Lists)

BusinessMap uses a specific structure for checkboxes:

```html
<ul class="todo-list">
  <li>
    <label class="todo-list__label"
      ><input type="checkbox" disabled="disabled" /><span class="todo-list__label__description"
        >Task description here</span
      ></label
    >
  </li>
</ul>
```

**Key points:**

- Use `class="todo-list"` on the `<ul>` element
- Nest structure: `<li>` → `<label class="todo-list__label">` → `<input>` + `<span>`
- Always use `disabled="disabled"` on checkboxes (users check them in UI)
- Put task description in `<span class="todo-list__label__description">`

---

## Card Structure Template

### Bug - Broker Template

This is the standard template we use (type_id: 62):

```
EXPECTED BEHAVIOR
- [ ] Checkbox list of acceptance criteria
- [ ] Each criterion clearly stated

ACTUAL BEHAVIOR
- Problem description
- Location: file paths
- Code examples
- Impact statements

PROPOSED SOLUTIONS
- Detailed fixes with code examples
- Multiple approaches where applicable
- Recommended approach indicated

CUSTOMER IMPACT
- Clear impact statements per priority level
- Separate Critical/High/Medium/Low impacts

AFFECTED VERSIONS
- Version information
- When issue was introduced

RELATED ISSUES
- Links to parent card
- Links to related tickets
- Links to analysis documents

DoR (Definition of Ready)
- Complete DoR checklist from Bug - Broker template

Please fill the following points after development

PRs
- [ ] Checkbox list of implementation tasks
- [ ] Verification steps

ACCEPTANCE TEST RESULTS
(To be filled after implementation)

RELEASE NOTE SUGGESTIONS
- Ready-to-use release notes
- Categorized by type (Bug Fix, Improvements, etc.)
```

### Content Guidelines

1. **EXPECTED BEHAVIOR**

   - Use checkbox lists for acceptance criteria
   - Start each criterion with priority level: `F-C1 (CRITICAL):`, `B-H2 (HIGH):`, etc.
   - Use MUST/SHOULD language for clarity

2. **ACTUAL BEHAVIOR**

   - Provide file locations: `src/path/to/file.ts:123`
   - Include code examples showing the problem
   - Use emoji indicators: ✅ (correct), ❌ (incorrect)
   - State impact clearly

3. **PROPOSED SOLUTIONS**

   - Provide complete code examples
   - Include estimated effort: "(5 minutes)", "(20 minutes)"
   - Mark recommended approach: "(RECOMMENDED)"

4. **CUSTOMER IMPACT**

   - Group by severity: Critical, High, Medium, Low
   - Use consistent formatting with colored text for Critical: `<strong style="color:hsl(0,75%,60%);">Critical:</strong>`
   - Explain consequences, not just descriptions

5. **PRs Section**
   - Include standard reminder: `<span style="color:hsl(0,75%,60%);">Please fill the following points after development</span>`
   - Use checkboxes for implementation tasks
   - Include verification steps (typecheck, test, build)

---

## Card Creation Process

### 1. Prepare Card Content

Convert ticket document from Markdown to HTML:

```typescript
// Example conversion
const markdown = `## EXPECTED BEHAVIOR
- Item 1`

const html = `<p><strong>EXPECTED BEHAVIOR</strong></p>
<ul>
<li>Item 1</li>
</ul>`
```

### 2. Create the Card

Use `mcp__businessmap__create_card` with required parameters:

```typescript
{
  title: "[Prefix] Clear Descriptive Title",
  column_id: 2925,  // Landing zone
  lane_id: 345,     // Default Swimlane
  priority: 3,      // 1=Low, 2=Medium, 3=High
  type_id: 62,      // Bug - Broker
  description: htmlContent
}
```

### 3. Link to Parent Card

After creation, link to parent using `mcp__businessmap__add_card_parent`:

```typescript
{
  card_id: newCardId,
  parent_card_id: parentCardId
}
```

### 4. Add Tags (Manual)

**Note:** API tag operations don't work reliably. Add tags manually in BusinessMap UI.

**Required tags for Edge board:**

- All cards: `hivemq-edge` (tag_id: 656)
- Frontend cards: `UI` (tag_id: 349)

---

## Design Decisions

### Title Prefixes

Use consistent prefixes to categorize tickets:

| Prefix             | Usage                   | Example                                     |
| ------------------ | ----------------------- | ------------------------------------------- |
| `[Component Name]` | Component-specific bugs | `[Databases Adapter]`, `[OPC-UA]`           |
| `[Feature Area]`   | Feature improvements    | `[Schema Validation]`, `[Widget Specs]`     |
| `[Code Cleanup]`   | Code quality            | `[Code Cleanup] Remove Orphaned Components` |
| `[RJSF Validator]` | Form validation         | `[RJSF Validator] Add HOSTNAME Format`      |
| `[Mocks Sync]`     | Test infrastructure     | `[Mocks Sync] Regenerate Adapter Mocks`     |
| `[UI Polish]`      | UX improvements         | `[UI Polish] Improve Field Titles`          |

### Priority Levels

| Priority | Value | Usage                                           |
| -------- | ----- | ----------------------------------------------- |
| Low      | 1     | Code cleanup, minor UX improvements             |
| Medium   | 2     | Important improvements, missing features        |
| High     | 3     | Critical bugs, security issues, blocking issues |

**Guidelines:**

- Use High (3) for Critical or High severity issues
- Use Medium (2) for Medium severity issues
- Use Low (1) for Low severity issues or nice-to-haves

### Issue Labeling

Use consistent issue IDs throughout documentation:

**Backend Issues:**

- `B-C1`, `B-C2`, ... (Critical)
- `B-H1`, `B-H2`, ... (High)
- `B-M1`, `B-M2`, ... (Medium)
- `B-L1`, `B-L2`, ... (Low)

**Frontend Issues:**

- `F-C1`, `F-C2`, ... (Critical)
- `F-H1`, `F-H2`, ... (High)
- `F-M1`, `F-M2`, ... (Medium)
- `F-L1`, `F-L2`, ... (Low)

**Widget Specification Issues:**

- `WS-C1`, `WS-C2`, ... (Critical - Security)
- `WS-H1`, `WS-H2`, ... (High - UX)
- `WS-M1`, `WS-M2`, ... (Medium - Consistency)

### Linking Strategy

```
Parent Analysis Card (Investigation)
    ↓ parent link
    ├── Implementation Ticket 1
    ├── Implementation Ticket 2
    ├── Implementation Ticket 3
    └── Implementation Ticket N
```

- All implementation tickets link to parent as "parent"
- Use relative links between related tickets if needed
- Document links in RELATED ISSUES section

---

## Common Parameters

### Board 57 (Edge) - Landing Zone

```typescript
const EDGE_BOARD = {
  board_id: 57,
  workflow_id: 232, // Development Workflow
  column_id: 2925, // Landing Zone
  lane_id: 345, // Default Swimlane
  type_id: 62, // Bug - Broker
}
```

### Card Type IDs (Board 57)

| Type          | ID  | Usage                      |
| ------------- | --- | -------------------------- |
| Bug - Broker  | 62  | Standard bug/issue tickets |
| Investigation | 122 | Research/analysis tasks    |
| Feature       | 59  | New feature requests       |
| Test          | 132 | Testing tasks              |
| Documentation | 131 | Documentation updates      |

### Column IDs (Development Workflow)

| Column               | ID   | Description                |
| -------------------- | ---- | -------------------------- |
| Icebox               | 2994 | Low priority backlog       |
| Landing Zone         | 2925 | New tickets triage         |
| Backlog              | 1805 | Prioritized backlog        |
| Requested (Selected) | 1806 | Ready for development      |
| Development          | 2983 | In progress                |
| Ready for Release    | 2930 | Complete, awaiting release |
| Released             | 2104 | Released to production     |

---

## Known Limitations

### Tag Operations Don't Work via API

**Problem:** The `tag_ids_to_add` and `tag_ids_to_remove` parameters in `update_card` don't actually apply tags.

**Workaround:** Add tags manually in BusinessMap UI after card creation.

**Required Tags:**

- `hivemq-edge` (656) - All Edge board tickets
- `UI` (349) - Frontend-related tickets

### No Direct Tag Listing

The API doesn't provide a way to list available tags. Tag IDs must be discovered by:

1. Manually adding tags to a test card
2. Reading the card to see tag_ids
3. Recording the mapping

---

## Example: Creating a Bug Fix Card

### Step 1: Prepare Content

```typescript
const cardContent = `<p><strong>EXPECTED BEHAVIOR</strong></p>
<ul class="todo-list">
<li><label class="todo-list__label"><input type="checkbox" disabled="disabled"><span class="todo-list__label__description"><strong>B-C1 (CRITICAL):</strong> Function MUST return correct value</span></label></li>
</ul>

<p><strong>ACTUAL BEHAVIOR</strong></p>
<p><strong>Location:</strong> <code>src/utils/helper.ts:42</code></p>
<pre><code>function getValue() {
  return wrongValue;  // ❌ Bug
}</code></pre>

<p><strong>Impact:</strong> Critical functionality broken</p>

<p><strong>PROPOSED SOLUTIONS</strong></p>
<pre><code>function getValue() {
  return correctValue;  // ✅ Fixed
}</code></pre>

<p><strong>CUSTOMER IMPACT</strong></p>
<p><strong style="color:hsl(0,75%,60%);">Critical:</strong> Feature completely broken</p>

<p><strong>RELATED ISSUES</strong></p>
<ul>
<li>Card 38628 (parent analysis)</li>
</ul>`
```

### Step 2: Create Card

```typescript
mcp__businessmap__create_card({
  title: '[Component] Fix Critical Bug in getValue()',
  column_id: 2925,
  lane_id: 345,
  priority: 3,
  type_id: 62,
  description: cardContent,
})
```

### Step 3: Link to Parent

```typescript
mcp__businessmap__add_card_parent({
  card_id: 38XXX,  // New card ID
  parent_card_id: 38628
});
```

### Step 4: Add Tags Manually

In BusinessMap UI:

1. Open card 38XXX
2. Add tag: `hivemq-edge`
3. If frontend: Add tag: `UI`

---

## Checklist for Card Creation

- [ ] Content converted from Markdown to HTML
- [ ] Used proper checkbox structure for todo lists
- [ ] Included all required sections (Expected, Actual, Proposed, Impact, etc.)
- [ ] Added issue IDs (B-C1, F-M2, etc.) to checkboxes
- [ ] Specified file locations with line numbers where applicable
- [ ] Included code examples in `<pre><code>` blocks
- [ ] Set correct priority (1/2/3)
- [ ] Created card in Landing zone (column_id: 2925)
- [ ] Linked to parent card using `add_card_parent`
- [ ] Added tags manually in BusinessMap UI
- [ ] Verified card displays correctly in BusinessMap

---

## Version History

| Version | Date       | Changes                                              |
| ------- | ---------- | ---------------------------------------------------- |
| 1.0     | 2025-12-17 | Initial guidelines based on task 38658 card creation |

---

## Related Documentation

- `.tasks/38658-adapter-jsonschema-review/` - Example analysis and ticket creation
- `.tasks/TICKETS_BACKEND_SUMMARY.md` - Backend tickets overview
- `.tasks/ACTIVE_TASKS.md` - Current active tasks
