# Task 38943: Complete Mapping Ownership

Complete the ownership tracking started in task 38936 by addressing the remaining ambiguity in `sources.tags[]` and `sources.topicFilters[]` arrays, and refactoring the fragile query structure.

## What's Fixed (38936)

- ✅ `sources.primary` has scope
- ✅ `instructions[].sourceRef` has scope
- ✅ Validation enforces scope integrity
- ✅ Operational status uses scope for matching

## What's Broken (This Task)

- ❌ `sources.tags[]` - string array, no ownership
- ❌ `sources.topicFilters[]` - string array, no ownership
- ❌ Query/Entity relationship - fragile index-based pairing
- ❌ Backend reconstructs from instructions - frontend arrays redundant

## Objectives:

- assess the exact nature of the ambiguity and the gaps in tag ownership
- investigate the react lifecycle of the query and the entity relationship involved in the UX flow
- identify the best ways to address the ambiguity. Let's make sure to quantify the efforts required for every possible solution. The following criteria apply:
  - backend changes (like API) is extremely high cost and MUST not be considered as viable option
  - backward compatibility MUST be preserved
  - frontend changes MUST be proportionate to the effort required
  - solution MUST be clean, i.e. not using partial workarounds like reconstructing the array
