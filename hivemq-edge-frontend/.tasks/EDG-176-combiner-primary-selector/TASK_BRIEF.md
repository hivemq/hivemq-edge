# EDG-176 — Task Brief

## Linear Issue

https://linear.app/hivemq/issue/EDG-176/combiner-mapping-ui-needs-to-the-implement-the-trigger-primary

## Context

This task is a follow-up to EDG-35 (Visual Rendering of Tags in Data Combinings, completed PR #1407) and ticket 38943. EDG-35 updated the sources selector, the selected sources list, and the right-side mapping column to render tags with scope (adapter ownership). One component was deliberately left out and flagged at the time: the **primary (trigger) selector** in the combining editor.

## Problem

`PrimarySelect` still renders options and the selected value as plain strings. Every other selector in the combining editor now uses the badge-based rendering pattern with scope/ownership info.

**Current behaviour:**

- Dropdown options: plain text labels (e.g. `my/tag/t1`, `my/topic/+/temp`)
- Selected value: plain text inside the react-select control
- No visual differentiation between TAG and TOPIC_FILTER options
- No scope/adapter ownership shown

**Target behaviour (matching `CombinedEntitySelect`):**

- Dropdown options: label + adapterId in gray + type badge, description below
- Selected value: `PLCTag` or `TopicFilter` badge with scope-formatted label (`adapter :: tag`)
- Consistent visual language with the sources selector directly above it

## Objectives

1. Options match `CombinedEntitySelect` style — type badge, scope in gray, description
2. Selected value rendered as a scoped badge (PLCTag / TopicFilter)
3. Maximum reuse of existing components: `PLCTag`, `TopicFilter`, `formatOwnershipString`, `chakraComponents`

## Files in Scope

| File                                                      | Change              |
| --------------------------------------------------------- | ------------------- |
| `src/modules/Mappings/combiner/PrimarySelect.tsx`         | Main implementation |
| `src/modules/Mappings/combiner/PrimarySelect.spec.cy.tsx` | Test updates        |
