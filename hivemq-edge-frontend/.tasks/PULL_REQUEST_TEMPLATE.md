# Pull Request: Duplicate Combiner Detection Enhancement

**Kanban Ticket:** https://businessmap.io/c/57/33168

---

## Description

# Pull Request Template & Guidelines

**Purpose:** This template provides structure and guidelines for creating effective, UX-focused pull request descriptions.

---

## Quick Start

1. Copy the [Basic Template](#basic-template) section below
2. Replace placeholders with your content
3. Follow the [Writing Guidelines](#writing-guidelines)
4. Include screenshots from Cypress E2E tests
5. Focus on **user benefits**, not technical implementation details

---

## Writing Guidelines

### Audience & Tone

**Primary Audience:** Product managers, designers, and other developers (not just technical reviewers)

**Tone:**

- ✅ **User-centric**: Start with what users gain
- ✅ **Clear & concise**: Avoid unnecessary technical jargon
- ✅ **Visual**: Use screenshots and examples liberally
- ✅ **Action-oriented**: Focus on benefits and outcomes
- ❌ **Avoid**: Code snippets in description (save for "Files Changed")
- ❌ **Avoid**: Replicating GitHub ticket details
- ❌ **Avoid**: Overly technical implementation minutiae

### Description Section

**Purpose:** Explain the "what" and "why" from a user perspective.

**Structure:**

1. **Opening paragraph**: Transform statement - what changes for users?
2. **Enhancement list**: 3-5 bullet points of key improvements
3. **User benefits subsection**: What users gain (not how it works)
4. **Technical summary**: Brief 3-5 bullet points for developers

**Example (Good):**

```markdown
This PR transforms how users understand the state of their data pipelines.
Previously, users could only see if nodes were connected. Now, users can
instantly see both runtime status (is it connected?) and operational status
(is it configured?) through intuitive visual feedback.
```

**Example (Bad - Too Technical):**

```markdown
This PR implements a dual-status model with Runtime and Operational enums,
refactors 10 node components to use the new NodeStatusModel interface, and
adds edge-specific status computation using React Flow hooks.
```

### BEFORE/AFTER Sections

**Purpose:** Show the transformation visually and concisely.

**BEFORE - Structure:**

- Brief description of limitations
- 3-5 bullet points of problems/limitations
- Optional: Screenshot (if available)
- No need for lengthy explanations

**AFTER - Structure:**

- Subsections for each major improvement (2-4 subsections)
- **Screenshot first**, explanation second
- Screenshot captions should reference Cypress test files
- Include viewport size in captions (e.g., "1400x1016")
- **Key Visual Elements** bullet list after each screenshot
- **User Benefits** paragraph explaining value

**Screenshot Guidelines:**

- Use Cypress E2E tests to capture consistent screenshots
- Reference the test file in caption: `_Test: cypress/e2e/[path]/[file].spec.cy.ts_`
- Include scenario description: `_Scenario: User creates adapter with multiple combiners_`
- Use consistent viewport: 1400x1016 (or document if different)
- Save in `.tasks/[task-id]/screenshots/` directory
- Use descriptive filenames: `after-modal-empty-state.png`

### Visual Language Guide (Optional)

**When to include:** For features with visual/UI changes

**Structure:**

- Table format for clarity
- Three columns: Visual Element | Meaning | User Action
- Use emojis for quick recognition (🟢🔴🟡⚡🚫)
- Include "Combined Status Examples" table showing interactions

### Test Coverage Section

**Keep it high-level:**

- Total count (e.g., "74+ tests, all passing ✅")
- Breakdown by type (unit, integration, E2E)
- Brief description of what each type covers
- Mention visual regression tools (Percy, Cypress screenshots)

**Avoid:**

- Listing every test file
- Showing test code
- Overly detailed test descriptions

### Files Changed Section

**Structure:**

- **Summary table** with counts (Created/Modified/Total)
- **Key Files** organized by category
- Brief one-line descriptions
- File paths for easy navigation

**Keep concise:**

- Group similar files (e.g., "All 10 node types")
- Don't list every single file
- Focus on most important/changed files

### Standard Sections

Always include these sections (brief is fine):

1. **Breaking Changes** - List or state "None"
2. **Performance Impact** - Positive improvements or "No impact"
3. **Accessibility** - Key a11y considerations (if UI changes)
4. **Documentation** - What docs were added/updated

### Reviewer Notes Section

**Purpose:** Help reviewers know what to focus on and how to test.

**Structure:**

1. **Focus areas** (4-5 bullet points)
2. **Manual testing suggestions** (step-by-step)
3. **Quick test commands** (code block)

**Be specific:**

- What should reviewers pay attention to?
- How can they reproduce key scenarios?
- What commands run the relevant tests?

---

## Basic Template

Copy and customize this template:

````markdown
# Pull Request: [Feature Name]

**Kanban Ticket:** https://businessmap.io/c/57/[TICKET_ID]

---

## Description

[Opening paragraph explaining the transformation for users - what changes and why it matters]

The enhancement introduces:

- **[Key improvement 1]**: [Brief explanation]
- **[Key improvement 2]**: [Brief explanation]
- **[Key improvement 3]**: [Brief explanation]

### User Experience Improvements

**What users gain:**

- **[Benefit 1]**: [How it helps users]
- **[Benefit 2]**: [How it helps users]
- **[Benefit 3]**: [How it helps users]

### Technical Summary

**Implementation highlights:**

- [Technical point 1]
- [Technical point 2]
- [Technical point 3]

---

## BEFORE

### Previous Behavior - [Brief Title]

The old implementation [brief description of what it did]:

**Limitations:**

- [Limitation 1]
- [Limitation 2]
- [Limitation 3]

![Before - Description](./screenshots/before-[feature].png)

_[Optional caption providing context]_

---

## AFTER

### New Behavior - [Brief Title]

The new implementation [brief description of improvement]:

#### 1. [Scenario Name]

[Brief description of this scenario]

![After - Scenario 1](./screenshots/after-scenario-1.png)

_Test: `cypress/e2e/[path]/[file].spec.cy.ts` - "[test name]"_  
_Screenshot: 1400x1016 viewport showing [what's shown]_

**Key Visual Elements:**

- **[Element 1]**: [What it shows/indicates]
- **[Element 2]**: [What it shows/indicates]

**User Benefits:**

- [Benefit explanation for this scenario]

#### 2. [Second Scenario Name]

[Repeat structure for additional scenarios]

---

## Visual Language Guide

[Include if there are visual/UI changes]

### What the [Elements] Mean

| Visual Element | Meaning         | User Action  |
| -------------- | --------------- | ------------ |
| [Element 1]    | [What it means] | [What to do] |
| [Element 2]    | [What it means] | [What to do] |

---

## Test Coverage

### Comprehensive Testing

- **[X]+ tests total, all passing ✅**
- **Unit tests**: [What they cover]
- **Integration tests**: [What they cover]
- **E2E tests**: [What they cover]

### Visual Regression

- [Tool used] screenshots/snapshots
- Consistent viewport ([dimensions])
- Tests cover [scenarios]

---

## Files Changed

### Summary

- **Created**: [X] new files
- **Modified**: [X] existing files
- **Total**: [X] files changed

### Key Files

**[Category 1]:**

1. `path/to/file1.ts` - [Brief description]
2. `path/to/file2.ts` - [Brief description]

**[Category 2]:**

- [Group description] ([count] files)

---

## Breaking Changes

[List changes or state "None. All changes are backward compatible:"]

- ✅ [Compatibility point 1]
- ✅ [Compatibility point 2]

---

## Performance Impact

[State impact - usually "Positive improvements:" or "No impact"]

- ✅ [Performance point 1]
- ✅ [Performance point 2]

---

## Accessibility

[If UI changes, list a11y considerations]

- ✅ [A11y feature 1]
- ✅ [A11y feature 2]
- ✅ [Testing method used]

---

## Documentation

[What documentation was created/updated]

- `[file]` - [Description]
- [Other docs]

---

## Future Enhancements (Optional)

[Optional improvements that could be done later]

- [ ] [Enhancement 1]
- [ ] [Enhancement 2]

---

## Reviewer Notes

**Focus areas for review:**

1. **[Area 1]**: [What to check]
2. **[Area 2]**: [What to check]
3. **[Area 3]**: [What to check]

**Manual testing suggestions:**

1. [Step 1]
2. [Step 2]
3. [Step 3]
4. Observe: [Expected result] ✅

**Quick test commands:**

```bash
# [Description]
[command]

# [Description]
[command]
```
````

---

## Migration Notes

[If relevant, notes for users and developers]

**For users:**

- [Migration note 1]

**For developers:**

- [Migration note 1]

````

---

## Section-by-Section Guide

### 1. Title & Kanban Link

```markdown
# Pull Request: [Concise Feature Name]

**Kanban Ticket:** https://businessmap.io/c/57/[TICKET_ID]
````

- Use the board number (57 is default)
- Use the ticket ID from the task
- Keep title short and descriptive (not technical)

### 2. Description - Opening

**Goal:** Hook readers with the user value proposition.

**Formula:**

```
This PR [transforms/enhances/improves] how users [do something].
Previously, users [old limitation]. Now, users [new capability]
through [approach].
```

**Good Examples:**

- "This PR transforms how users understand the state of their data pipelines."
- "This PR enhances the user experience when attempting to create a combiner."
- "This PR improves visibility into configuration completeness across the workspace."

**Bad Examples:**

- "This PR refactors the status system to use a dual-status model." ❌
- "This PR implements new React Flow hooks for better performance." ❌

### 3. Description - Enhancement List

**Goal:** Quick scan of what's new.

**Rules:**

- 3-5 bullet points maximum
- Bold the key phrase
- Keep each point to one line if possible
- Focus on user-facing features

**Template:**

```markdown
The enhancement introduces:

- **[Feature]**: [User-facing description]
- **[Feature]**: [User-facing description]
```

### 4. Description - User Benefits

**Goal:** Explicitly state what users gain.

**Structure:**

- Subsection with "What users gain:"
- 3-5 bullet points
- Bold the benefit category
- Explain the value concisely

**Template:**

```markdown
### User Experience Improvements

**What users gain:**

- **[Benefit category]**: [How it helps]
```

### 5. Description - Technical Summary

**Goal:** Brief technical overview for developers.

**Rules:**

- Keep to 3-5 bullet points
- High-level only
- No code snippets here
- Save details for "Files Changed" section

### 6. BEFORE Section

**Goal:** Establish the problem/limitation context.

**Keep brief:**

- One paragraph description
- 3-5 limitation bullet points
- Optional screenshot (if available)
- Don't dwell on the old way

### 7. AFTER Section

**Goal:** Show the transformation with visuals and benefits.

**Structure:**

- Multiple subsections (numbered)
- Each subsection = one scenario/aspect
- Screenshot first, then explanation
- Always include "User Benefits" paragraph

**Screenshot Requirements:**

- Must reference Cypress test file
- Include viewport dimensions
- Descriptive filename
- Stored in `.tasks/[task-id]/screenshots/`

**Caption Template:**

```markdown
_Test: `cypress/e2e/[path]/[file].spec.cy.ts` - "[test name]"_  
_Screenshot: 1400x1016 viewport showing [description]_
```

### 8. Visual Language Guide

**When to include:**

- Any UI/visual changes
- New visual conventions
- Status indicators, colors, animations

**Format:**

- Use tables for clarity
- Three columns: Element | Meaning | User Action
- Use emojis for quick visual scanning
- Include "Combined" examples table if relevant

### 9. Test Coverage

**Keep high-level:**

```markdown
### Comprehensive Testing

- **[X]+ tests total, all passing ✅**
- **Unit tests**: [Brief description]
- **Integration tests**: [Brief description]
- **E2E tests**: [Brief description]

### Visual Regression

- [Method/tool]
- [Coverage summary]
```

### 10. Files Changed

**Summary table first:**

```markdown
### Summary

- **Created**: [X] new files
- **Modified**: [X] existing files
- **Total**: [X] files changed
```

**Then organize by category:**

- Group similar files
- Use clear category names
- Brief one-line descriptions
- File paths for navigation

### 11. Standard Sections

**Breaking Changes:**

- State "None" if truly none
- Otherwise list with clear explanations

**Performance Impact:**

- Usually "Positive improvements" or "No impact"
- Brief bullet points

**Accessibility:**

- Include for any UI changes
- List key a11y features implemented
- Mention testing tools used (axe-core, etc.)

**Documentation:**

- List created/updated docs
- Brief description of each

### 12. Reviewer Notes

**Most important section for reviewers!**

**Focus areas:** What reviewers should pay attention to
**Manual testing:** Step-by-step reproduction steps
**Quick test commands:** Copy-paste commands to run tests

**Be specific:**

```markdown
**Manual testing suggestions:**

1. [Specific action]
2. [Specific action]
3. [Specific action]
4. Observe: [Expected result] ✅
```

---

## Common Mistakes to Avoid

### ❌ Too Technical

**Bad:**

```markdown
This PR implements a NodeStatusModel interface with Runtime and Operational
enums, refactors 10 node components to use React.useMemo for status
computation, and adds per-edge operational status logic using the target
node's statusModel.operational property.
```

**Good:**

```markdown
This PR transforms how users see their workspace status. Users can now
instantly see both if nodes are connected (runtime) and if they're
configured correctly (operational) through color and animation.
```

### ❌ Replicating GitHub Ticket

**Bad:**

- Copying entire ticket description
- Listing all acceptance criteria
- Including internal planning notes
- Lengthy requirement specifications

**Good:**

- Brief user-focused description
- Link to ticket for details
- Focus on what was actually built

### ❌ Missing Visuals

**Bad:**

- All text, no screenshots
- Generic descriptions of UI changes
- "The modal looks better now"

**Good:**

- Screenshot for every UI change
- Annotated with captions
- Referenced test file for reproducibility

### ❌ Code Dumps

**Bad:**

- Long code snippets in description
- Showing implementation details
- Technical architecture diagrams

**Good:**

- Save code for "Files Changed" section
- Focus on user-facing behavior
- Use simple diagrams if needed (tables work well)

### ❌ Vague Benefits

**Bad:**

- "Improved UX"
- "Better performance"
- "Enhanced usability"

**Good:**

- "Faster troubleshooting: Immediately identify which connections need attention"
- "Reduced configuration errors: Visual feedback prevents incomplete setups"

---

## Checklist Before Submitting

Use this checklist to ensure your PR description is complete:

- [ ] Title is concise and user-focused (not technical)
- [ ] Kanban ticket link included with correct board and ID
- [ ] Description starts with user value proposition
- [ ] Benefits section explicitly states what users gain
- [ ] Technical summary is brief (3-5 bullets)
- [ ] BEFORE section establishes context (not too long)
- [ ] AFTER section has screenshots with proper captions
- [ ] Screenshots reference Cypress test files
- [ ] Visual language guide included (if UI changes)
- [ ] Test coverage is summarized (not exhaustive)
- [ ] Files changed section is organized and concise
- [ ] Breaking changes are clearly stated (or "None")
- [ ] Reviewer notes provide clear testing instructions
- [ ] No code snippets in description section
- [ ] No replication of GitHub ticket content
- [ ] Tone is user-centric, not developer-centric

---

## Examples

### Good PR Titles

✅ "Enhanced Workspace Status Visualization"
✅ "Duplicate Combiner Detection with Modal Dialog"
✅ "Improved Configuration Validation Feedback"

### Bad PR Titles

❌ "Refactor Status System to Use Dual-Status Model"
❌ "Implement NodeStatusModel Interface Across Components"
❌ "Add React Flow Hook Optimizations"

### Good Opening Paragraphs

✅ "This PR transforms how users understand their data pipelines by showing both runtime and configuration status through intuitive color and animation."

✅ "This PR enhances the duplicate detection experience by replacing a brief toast with a comprehensive modal that shows existing combiner details and clear action choices."

### Bad Opening Paragraphs

❌ "This PR implements a comprehensive dual-status model (Runtime + Operational) for the HiveMQ Edge Workspace with fine-grained per-edge operational status."

❌ "This PR refactors the ContextualToolbar component to extract 5 utility functions and creates a new modal component with full keyboard navigation support."

---

## Questions?

**For PR description questions:**

- Review this template
- Look at recent PRs for examples
- Ask in team chat

**For technical implementation questions:**

- Those belong in code comments
- Or in architecture documentation
- Not in the PR description

---

**Remember:** The PR description is for understanding **user value and testing the change**, not for explaining **how the code works**. Save implementation details for code review discussions and documentation.
