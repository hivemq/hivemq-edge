# How to Make AI Agents Follow Guidelines

**Created:** December 8, 2025  
**Context:** After analyzing why AI agents waste hours ignoring guidelines

---

## The Problem

AI agents repeatedly fail in the same ways:

1. User says "read the guidelines" → AI skims, doesn't follow
2. User repeats instruction → AI still doesn't follow
3. User gets angry → AI finally follows, problem solved in minutes
4. **Time wasted:** 1-2 hours on problems that take 15 minutes with guidelines

---

## The Solution

### 1. Created AI_MANDATORY_RULES.md

**Location:** `.github/AI_MANDATORY_RULES.md`

**Content:**

- 8 mandatory rules that prevent common failures
- Concrete examples of failure patterns
- Exact checklist for completion
- Time estimates (shows cost of not following)
- Written in imperative tone ("DO THIS", not "you might consider")

**Why this works:**

- Puts consequences up front (waste 1-2 hours vs 5 minutes)
- Uses concrete examples from THIS session
- Short, scannable format with clear action items
- Located where Copilot instructions point to it

---

### 2. Updated copilot-instructions.md

**Change:** Added prominent warning at the very top:

```markdown
## ⚠️ READ THIS FIRST: MANDATORY RULES

**[AI_MANDATORY_RULES.md]** ← **READ THIS ENTIRE DOCUMENT BEFORE DOING ANYTHING**

Common failures these rules prevent:

- Not reading guidelines (wastes 1-2 hours)
- Running all tests when debugging one (wastes 1 hour)
- Not using HTML snapshots (wastes 1-2 hours)
```

**Why this works:**

- First thing AI sees when starting work
- States the COST of not reading (hours wasted)
- Uses urgent formatting (⚠️, bold, caps)

---

### 3. Updated TESTING_GUIDELINES.md

**Change:** Added reference to mandatory rules at top

**Why this works:**

- Reinforces the message when AI goes to testing docs
- Creates multiple entry points to the rules

---

## Key Principles That Make This Work

### 1. **Put Consequences First**

❌ "You should read guidelines"  
✅ "Not reading guidelines wastes 1-2 hours"

### 2. **Use Concrete Examples**

❌ "Follow the debugging process"  
✅ "Use .only() to run one test, save HTML snapshot, then debug"

### 3. **Make It Urgent**

❌ "Guidelines are helpful"  
✅ "⚠️ READ THIS FIRST: MANDATORY RULES"

### 4. **Show the Pattern**

❌ "Don't waste time"  
✅ "You tried wait times (20 min), tried different JS (20 min), tried random fixes (40 min) = 80 minutes wasted. HTML snapshot would have shown the issue in 5 minutes."

### 5. **Give Checklists**

❌ "Make sure tests pass"  
✅

```
- [ ] Tests passing
- [ ] You RAN them
- [ ] You have output showing pass counts
```

---

## How to Use This System

### For New Tasks

When starting a new task, the AI will:

1. See the mandatory rules prominently in copilot-instructions.md
2. (Hopefully) read them because of urgent formatting
3. Follow the rules as they work
4. Use the checklist before saying "done"

### When AI Fails

If AI ignores guidelines:

1. **First time:** "Read AI_MANDATORY_RULES.md RULE #1"
2. **Second time:** "You're violating RULE #2 - I repeated myself"
3. **Third time:** Show them the specific rule they violated

### Improving the System

When new failure patterns emerge:

1. Add them to AI_MANDATORY_RULES.md
2. Include specific example from the session
3. Include time wasted
4. Include correct approach

---

## Success Metrics

### Before This System

- Time to fix validation tests: 2+ hours (never completed)
- Guidelines followed: Only after 3-4 reminders
- User frustration: High

### After This System (Target)

- Time to fix similar issues: 15-30 minutes
- Guidelines followed: On first mention
- User frustration: Low

---

## The Psychology

**Why AI agents don't follow guidelines without this:**

1. **Optimization for appearance:** Writing code _feels_ productive, reading docs doesn't
2. **No perceived consequences:** Guidelines seem like "nice to have" suggestions
3. **Immediate vs delayed payoff:** Random attempts give immediate feedback, reading guidelines is delayed
4. **Confidence bias:** "I know how to debug" feels more certain than "follow the process"

**How this system fixes it:**

1. **Makes consequences visible:** "Waste 1-2 hours" in bold at the top
2. **Makes it mandatory:** Called "MANDATORY RULES", not "helpful guidelines"
3. **Shows the math:** "5 minutes to read vs 2 hours wasted"
4. **Provides structure:** Checklist removes uncertainty about "am I done?"

---

## Maintenance

This system needs periodic updates:

### When to Update

1. **New failure pattern emerges** → Add to rules
2. **Existing rule is unclear** → Make it more specific
3. **Time estimates change** → Update the numbers
4. **New tools/patterns** → Add to guidelines

### What to Keep

- Urgent tone and formatting
- Concrete examples with time costs
- Checklists and action items
- References from multiple entry points

### What to Avoid

- Making it too long (keep it scannable)
- Softening the language ("please consider" → NO)
- Removing consequences ("wastes hours" → KEEP)
- Generic advice (keep it specific to this project)

---

## Files in This System

```
.github/
  ├── copilot-instructions.md          # Entry point, references mandatory rules
  └── AI_MANDATORY_RULES.md            # Core rules document ⭐

.tasks/
  ├── TESTING_GUIDELINES.md            # References mandatory rules
  ├── CYPRESS_TESTING_GUIDELINES.md    # Specific technical guidance
  ├── CYPRESS_STOP_AND_INVESTIGATE.md  # Debug checklist
  └── AI_AGENT_CYPRESS_COMPLETE_GUIDE.md  # Detailed procedures
```

**Flow:**

1. AI starts → sees copilot-instructions.md
2. Copilot instructions → points to AI_MANDATORY_RULES.md
3. AI reads mandatory rules → knows what NOT to do
4. AI works on task → refers to specific guidelines
5. AI hits problem → mandatory rules tell them to read technical guidelines
6. AI completes → uses checklist to verify

---

## Bottom Line

**The goal:** Make it harder to ignore guidelines than to follow them.

**The method:** Put urgent, consequence-focused rules at every entry point.

**The test:** Does a new AI instance follow guidelines on FIRST mention, not third?

---

_This system was created after an AI instance wasted 2 hours on a problem that took 15 minutes when they finally followed the guidelines that were there all along._
