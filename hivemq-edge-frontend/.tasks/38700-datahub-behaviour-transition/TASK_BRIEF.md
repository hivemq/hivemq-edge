# Task 38700: DataHub Behavior Policy - Enhanced Model Selection

## Overview

Improve the user-facing parts of the behavior policy model selection in DataHub. The JSON schema contains three behavior models (Mqtt.events, Publish.duplicate, Publish.quota), each with their own FSM (Finite State Machine) definition. Currently, the model selector is a basic dropdown with no descriptions or guidance.

## Context

### Current Architecture

- **Schema**: `src/extensions/datahub/api/__generated__/schemas/BehaviorPolicyData.json`

  - Contains 3 behavior models with embedded FSM metadata (states + transitions)
  - Uses JSON Schema `allOf`/`if`/`then` for conditional model-specific arguments

- **Form Panel**: `src/extensions/datahub/designer/behavior_policy/BehaviorPolicyPanel.tsx`

  - Renders policy form using RJSF (React JSON Schema Form)
  - Model selector is currently a plain enum dropdown

- **Transition Panel**: `src/extensions/datahub/designer/transition/TransitionPanel.tsx`
  - Reads parent policy's selected model to populate FSM-based transition selector
  - Shows FSM visualization below form

### Behavior Models

1. **Mqtt.events** - Event interception for debugging (no arguments required)
2. **Publish.duplicate** - Detects consecutive identical messages (no arguments)
3. **Publish.quota** - Tracks publish count with min/max thresholds (requires arguments)

## Subtask 1: Enhanced Model Selector with Descriptions/Previews

### Objective

Replace the basic model dropdown with an enhanced selector that provides:

- Rich model descriptions (what each model does)
- Visual FSM preview or summary
- Clear indication of which models require arguments
- Better UX for model comparison and selection

### Requirements

1. **Create Custom RJSF Widget**

   - Custom widget for the `model` field in BehaviorPolicyData schema
   - Display model name, description, and FSM summary (state count, transition count)
   - Visual indication of SUCCESS/FAILED terminal states
   - Show if model requires arguments

2. **UI Design**

   - Use Chakra UI components (RadioGroup or custom card-based selector)
   - Responsive layout
   - Clear visual hierarchy (model name → description → FSM info)
   - Accessible (ARIA labels, keyboard navigation)

3. **Integration**

   - Register widget in `datahubRJSFWidgets.tsx`
   - Update `BehaviorPolicySchema.ts` uiSchema to use custom widget
   - Extract descriptions from JSON schema metadata

4. **Testing**
   - Component test with all three models
   - Verify selection updates form data
   - Accessibility test (mandatory)
   - Test readonly/disabled states

### Implementation Notes

- DO NOT modify the JSON schema structure
- Keep "XXXXX DISCREPANCY" comments in place (under investigation separately)
- Extract metadata programmatically from schema definitions
- Follow RJSF widget patterns established in existing widgets (TransitionSelect, etc.)

### Files to Create/Modify

**Create:**

- `src/extensions/datahub/components/forms/BehaviorModelSelect.tsx` - Custom widget
- `src/extensions/datahub/components/forms/BehaviorModelSelect.spec.cy.tsx` - Tests

**Modify:**

- `src/extensions/datahub/designer/behavior_policy/BehaviorPolicySchema.ts` - Add widget to uiSchema
- `src/extensions/datahub/designer/datahubRJSFWidgets.tsx` - Register widget
- `src/extensions/datahub/locales/en/datahub.json` - Add i18n keys (if needed)

### Acceptance Criteria

- [ ] Model selector displays all three models with descriptions
- [ ] FSM summary (state/transition counts) visible for each model
- [ ] Visual indication of models requiring arguments
- [ ] Selection updates form data correctly
- [ ] Component tests pass (including accessibility)
- [ ] Follows existing DataHub patterns and styling
- [ ] i18n keys added for all user-facing text
- [ ] TypeScript compiles without errors
- [ ] ESLint and Prettier applied

## Status

**Started:** January 28, 2026
**Status:** In Progress
**Current Phase:** Subtask 1 - Enhanced Model Selector
