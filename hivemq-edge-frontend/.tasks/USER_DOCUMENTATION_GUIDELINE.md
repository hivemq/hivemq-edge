# User Documentation Guideline

## Purpose

This guideline provides a reusable template and best practices for creating end-user-focused feature documentation. Use this for blog posts, release notes, and feature announcements.

**Reference Implementation:** [25337 Workspace Auto-Layout User Documentation](./25337-workspace-auto-layout/USER_DOCUMENTATION.md)

---

## Document Structure

### Header Level 2 (##) - Feature Title

**Format:** `## [Feature Name]: [Brief Value Proposition]`

**Example:** `## Workspace Auto-Layout: Organize Your MQTT Architecture Effortlessly`

**Guidelines:**

- Lead with action verb when possible (Organize, Explore, Configure, etc.)
- Include the primary benefit or transformation the feature enables
- Keep under 70 characters for readability

---

### Section 1: What It Is (### Level 3)

**Purpose:** Define the feature clearly for users encountering it for the first time.

**Structure:**

1. **Opening sentence** - One sentence explaining what the feature is

   - Use present tense
   - Lead with the user benefit
   - Example: "HiveMQ Edge now includes **automatic layout algorithms** that intelligently organize your workspace nodes."

2. **Feature options/variants** - Bullet list of available choices/algorithms/modes
   - Format: `- **Option Name** - Brief description, best use case`
   - Keep descriptions to one line
   - Include 3-7 options depending on feature complexity
   - Add technical context only if it clarifies purpose, not implementation

**Word Count:** 75-150 words

**Example:**

```markdown
### What It Is

HiveMQ Edge now includes **automatic layout algorithms** that intelligently
organize your workspace nodes. Instead of manually positioning each element,
select a layout and let the workspace arrange your MQTT infrastructure in seconds.

The feature offers five professional algorithms, each optimized for different
topology patterns:

- **Dagre Vertical** - Clean top-to-bottom flow, perfect for sequential architectures
- **Dagre Horizontal** - Left-to-right organization, ideal for wide screens
- **Radial Hub** - EDGE node centered with connections radiating outward
- **Force-Directed** - Physics-based organic clustering that reveals natural relationships
- **Hierarchical Constraint** - Strict layer-based organization for formal structures
```

---

### Section 2: How It Works (### Level 3)

**Purpose:** Provide step-by-step instructions that users can follow immediately.

**Structure:**

1. **Numbered steps** (3-6 steps) - Simple, actionable instructions

   - Start with "Open," "Click," "Select," "Enter"
   - Bold the action or UI element: `**Open your workspace**`
   - One action per step
   - Include optional steps with "(optional)" label

2. **Performance/technical note** - Single sentence about speed or capability

   - Reassures users about responsiveness
   - Example: "All layouts execute instantly—even complex calculations complete in milliseconds..."

3. **Screenshot placeholder** - Add after steps complete

**Word Count:** 80-120 words (excluding screenshot)

**Screenshot Placeholder Format:**

```markdown
![Alt Text - Describe what the user will see](./screenshot-feature-name.png)
```

**Alt Text Guidelines:**

- Describe what the screenshot shows, not just "screenshot of feature"
- Include key UI elements visible
- Be specific: "Layout Controls showing algorithm dropdown and apply button" not "Layout controls"

**Example:**

```markdown
### How It Works

1. **Open your workspace** and locate the Layout Controls in the toolbar
2. **Select an algorithm** from the dropdown menu
3. **Click Apply Layout** to instantly reorganize your nodes
4. **Save as preset** (optional) to reuse the same arrangement across workspaces

All layouts execute instantly—even complex calculations complete in milliseconds,
so you can iterate freely and compare different arrangements.

![Workspace Layout Controls - Showing algorithm selector and layout options](./screenshot-workspace-layout-controls.png)
```

---

### Section 3: How It Helps (### Level 3)

**Purpose:** Articulate concrete user benefits. Use subheadings for different benefit categories.

**Structure:**

1. **3-4 subsections** (#### Level 4) - Each representing one key benefit
2. **Each subsection** contains:
   - Subheading: Action-oriented, user-focused
   - 1-2 sentences explaining the benefit
   - Concrete example or use case when relevant

**Benefit Categories (common examples):**

- Better Visualization / Clarity
- Faster Setup / Time Savings
- Explore / Discovery / Understanding
- Consistency / Reusability
- Performance / Efficiency
- Control / Flexibility

**Word Count:** 100-150 words total

**Example:**

```markdown
### How It Helps

#### Better Visualization

See your MQTT architecture's structure clearly without manual node positioning.
Different layouts reveal different aspects of your topology—from linear flows
to interconnected relationships.

#### Faster Setup

Stop spending time on layout tweaking. Apply a layout in one click, then save
it as a reusable preset for consistent workspace organization.

#### Explore Your Architecture

Try different layouts to understand your topology better. A force-directed layout
might reveal unexpected clusters, while a hierarchical view clarifies data flow patterns.
```

---

### Section 4: Looking Ahead / Future Direction (### Level 3)

**Purpose:** Set expectations about feature maturity and evolution.

**When to Include:**

- Features that are new/experimental
- Algorithms or approaches that may change
- Features collecting user feedback for improvements

**Structure:**

1. **State the current status** - "initial implementation," "experimental," "version 1"
2. **Explain feedback collection** - How user input drives improvements
3. **Set expectations for evolution** - This will improve/change/expand
4. **Call to action** - Invite specific feedback or use cases

**Tone Guidelines:**

- Transparent and honest about limitations
- Optimistic about future improvements
- Frame as "starting point" not "incomplete"
- Avoid phrases like "only," "just," or "limited"

**Word Count:** 80-120 words

**Example:**

```markdown
### Looking Ahead

The layout algorithms available today represent our **initial implementation**.
**We're actively collecting feedback from real-world MQTT topologies to
continuously improve these layouts.** As users deploy HiveMQ Edge with diverse
network architectures, we'll refine these algorithms to better match common patterns.

Consider these layouts as **starting points that will evolve** based on your
feedback. If you notice improvement opportunities with your specific topology,
please share your insights!
```

---

### Closing Call-to-Action

**Purpose:** Encourage trial and feedback.

**Format:** Standalone bold paragraph at end, after optional section divider `---`

**Guidelines:**

- Specific (mention the feature, not just "try it")
- Action-oriented
- Positive/encouraging tone
- Optional: Include feedback mechanism (email, link, survey)

**Word Count:** 1-2 sentences

**Example:**

```markdown
---

**Try the new layouts in your next workspace and discover which arrangement
works best for your architecture.**
```

---

## Screenshot Requirements

### When Screenshots Are Essential

**Always include a screenshot if:**

- Feature has UI controls (buttons, dropdowns, panels)
- Visual layout matters (workspace arrangement, design, positioning)
- User needs to locate elements on screen

**May skip screenshot if:**

- Feature is purely functional with no UI interaction
- Feature is visible system behavior (logging, data validation)

### How to Generate Screenshots from E2E Tests

#### Step 1: Locate Percy Snapshots in Tests

Search for Percy snapshot calls in your E2E test files:

```bash
grep -r "cy.percySnapshot" cypress/e2e/workspace/
```

Example output:

```
workspace-layout-accessibility.spec.cy.ts:128:    cy.percySnapshot('Workspace - Layout Controls Panel')
workspace-layout-accessibility.spec.cy.ts:191:    cy.percySnapshot('Workspace - After Dagre TB Layout')
```

#### Step 2: Run E2E Tests with Percy

**Prerequisite:** Percy CLI installed and configured

```bash
# Install Percy CLI if not already installed
npm install --save-dev @percy/cli

# Run specific E2E test file with Percy enabled
npx percy exec -- npm run cypress:run:e2e -- --spec "cypress/e2e/workspace/workspace-layout-accessibility.spec.cy.ts"
```

**Without Percy (manual screenshot):**
If Percy is not configured, take manual screenshots during test execution:

```bash
npm run cypress:run:e2e -- --spec "cypress/e2e/workspace/workspace-layout-basic.spec.cy.ts" --headed
```

Then manually screenshot the relevant test state and save the image.

#### Step 3: Save Screenshot to Task Directory

**Naming convention:** `screenshot-[feature-noun]-[descriptor].png`

**Examples:**

- `screenshot-workspace-layout-controls.png`
- `screenshot-layout-presets-manager.png`
- `screenshot-algorithm-selector.png`

**Location:** Same directory as USER_DOCUMENTATION.md

**Acceptable formats:**

- PNG (preferred - lossless)
- JPG (acceptable for photos)

**Size guidelines:**

- Max 1MB file size
- Width: 1200-1400px for readability
- Height: natural aspect ratio

#### Step 4: Verify Screenshot Display

After adding screenshot markdown:

```markdown
![Workspace Layout Controls - Showing algorithm selector and layout options](./screenshot-workspace-layout-controls.png)
```

**Verification checklist:**

- [ ] File exists in same directory as documentation
- [ ] Alt text is descriptive (not just "screenshot")
- [ ] Image displays clearly at intended size
- [ ] Screenshot shows the primary UI interaction point
- [ ] No sensitive data visible (credentials, internal IPs, etc.)

---

## Tone and Voice Guidelines

### Do's ✅

- **Use second person:** "You can," "Try," "Your workspace" (speaks directly to user)
- **Use active voice:** "Click the button" not "The button should be clicked"
- **Be specific:** "Save as a reusable preset" not "It can be saved"
- **Use action verbs:** Organize, Explore, Discover, Configure, Apply
- **Be encouraging:** "effortlessly," "instantly," "simply"
- **Use contractions:** "You'll notice," "Don't worry" (conversational)

### Don'ts ❌

- **Avoid technical jargon** unless defining it for users (no "DAG," "force simulation," "constraint propagation")
- **Don't assume previous knowledge** (explain what a "preset" or "algorithm" is first use)
- **Avoid multiple nested conditions:** Keep sentences simple
- **Don't use marketing hyperbole:** "revolutionary," "game-changing" (use benefits instead)
- **Avoid passive voice:** Not "Nodes will be organized" but "Organize your nodes"

### Word Count Targets

| Section            | Target         | Flexibility |
| ------------------ | -------------- | ----------- |
| Title              | <70 chars      | ±10%        |
| What It Is         | 75-150 words   | ±20%        |
| How It Works       | 80-120 words   | ±20%        |
| Screenshot caption | 1 line         | -           |
| How It Helps       | 100-150 words  | ±20%        |
| Looking Ahead      | 80-120 words   | ±20%        |
| CTA                | 1-2 sentences  | -           |
| **Total**          | **~500 words** | ±10%        |

**Rationale:** Blog posts work best at 500-800 words. This template targets 500 words to leave room for other features in a multi-feature announcement.

---

## Checklist: Before Publishing

- [ ] **Structure:** All 4 required sections present (What/How/Why/Looking Ahead)
- [ ] **Title:** Level 2 header with feature name and value proposition
- [ ] **Headings:** All sections use proper levels (### for main, #### for subsections)
- [ ] **Word count:** ~500 words ±10%
- [ ] **Tone:** Second person, active voice, encouraging
- [ ] **Specificity:** No vague claims; all benefits supported with examples
- [ ] **Screenshot:** Included with descriptive alt text (if UI feature)
- [ ] **No jargon:** Technical terms explained or avoided
- [ ] **CTA:** Clear call-to-action at end
- [ ] **Feedback section:** If experimental/new, "Looking Ahead" explains what will change
- [ ] **Links:** Task documentation or related docs linked if relevant
- [ ] **Formatting:** Bold for emphasis, bullets for lists, proper markdown

---

## File Location & Naming

### Standard Location

```
.tasks/{TASK_ID}-{TASK_NAME}/USER_DOCUMENTATION.md
```

**Example:**

```
.tasks/25337-workspace-auto-layout/USER_DOCUMENTATION.md
```

### Screenshot Location

```
.tasks/{TASK_ID}-{TASK_NAME}/screenshot-*.png
```

**Example:**

```
.tasks/25337-workspace-auto-layout/screenshot-workspace-layout-controls.png
```

### Integration into Blog Post

When multiple features are documented this way:

1. **Collect all USER_DOCUMENTATION.md files**
2. **Each section becomes a subsection** in the blog post
3. **Add a level 1 (#) header** at the very top of the blog post
4. **Section titles become level 2 (##)** headers (as already formatted)
5. **Update screenshot paths** to point to correct locations if reorganizing files

**Example blog post structure:**

```markdown
# HiveMQ Edge Release Notes - Q4 2025

## Feature 1: Workspace Auto-Layout

[Content from USER_DOCUMENTATION.md]

## Feature 2: Policy Success Summary

[Content from USER_DOCUMENTATION.md]

## Feature 3: Workspace Status

[Content from USER_DOCUMENTATION.md]
```

---

## Real Examples

### Workspace Auto-Layout

- **File:** [.tasks/25337-workspace-auto-layout/USER_DOCUMENTATION.md](./25337-workspace-auto-layout/USER_DOCUMENTATION.md)
- **Structure:** What It Is (5 algorithms) → How It Works (4 steps + screenshot) → How It Helps (3 benefits) → Looking Ahead (experimental feedback)
- **Word Count:** ~520 words
- **Screenshot:** Placeholder with guide for generation

---

## Common Pitfalls & How to Avoid Them

### Pitfall 1: Too Much Technical Detail

❌ "The dagre library uses a sugiyama-style hierarchical graph layout algorithm..."  
✅ "Clean top-to-bottom flow, perfect for sequential architectures"

**Fix:** Focus on what the user experiences, not how it works internally.

---

### Pitfall 2: Vague Benefits

❌ "Improved workspace organization"  
✅ "See your MQTT architecture's structure clearly without manual node positioning"

**Fix:** Show the before/after or give specific examples.

---

### Pitfall 3: Forgetting the "Looking Ahead" Section

❌ Missing entirely for experimental features

**Fix:** Always include for features that are new/experimental/might change. Sets expectations and invites feedback.

---

### Pitfall 4: Too Many Steps or Options

❌ "How It Works" with 8+ steps  
✅ "How It Works" with 3-5 steps

**Fix:** Combine related steps. Optional steps marked "(optional)".

---

### Pitfall 5: Screenshot Too Small or Unclear

❌ Generic screenshot of whole UI with small controls  
✅ Cropped screenshot showing just the layout controls in focus

**Fix:** Screenshot should focus on the feature being documented, sized for readability.

---

### Pitfall 6: Passive or Weak Language

❌ "Layouts can be saved as presets"  
✅ "Save layouts as presets to reuse them across workspaces"

**Fix:** Use active voice. Start sentences with the user action.

---

## Questions to Ask When Writing

**Before you start:**

1. Who is the primary user? (DevOps engineer, System Administrator, etc.)
2. What problem does this feature solve?
3. What's the one thing users need to know first?

**For each section:**

1. **What It Is:** Can a newcomer understand this without prior knowledge?
2. **How It Works:** Could a user follow these steps without help?
3. **How It Helps:** Does each benefit have a concrete example or context?
4. **Looking Ahead:** Is the maturity level clear? What feedback do you want?

---

## Version History

| Version | Date         | Changes                                                                            |
| ------- | ------------ | ---------------------------------------------------------------------------------- |
| 1.0     | Nov 12, 2025 | Initial guideline document based on 25337 workspace auto-layout user documentation |
