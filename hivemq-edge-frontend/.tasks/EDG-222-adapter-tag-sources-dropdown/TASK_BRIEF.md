# EDG-222 — Task Brief

## Linear Issue

- **ID**: EDG-222
- **Title**: adapter :: tag should be shown in the sources dropdown list
- **Priority**: High
- **Status**: In Progress
- **Project**: Tagname Scoping (aka Namespace Normalisation)

## Related Issues

- **EDG-35** — Visual Rendering of Tags in Data Combinings (Mappings) — PR #1407 merged
- **EDG-176** — Combiner Mapping UI: trigger (primary) selector with scope — PR #1439 merged

## Problem Statement

In the Combiner Mapping editor, when the user opens the **sources dropdown** (`CombinedEntitySelect`),
the available tags are listed without the `adapter :: tag` visual format. Instead, the tag name is
shown as plain text and the adapter name appears separately in grey to the right.

**Expected**: Tags in the dropdown options list rendered as `[ PLCTag icon | adapter :: tag ]`
**Actual**: Tags rendered as plain `tag name` with adapter shown separately in grey

## Context

This is a follow-up bug fix on EDG-35 (which established the `adapter :: tag` visual language) and
EDG-176 (which fixed the **primary** selector, but left the **sources** option list unfixed).

The display pattern from EDG-35:

- When adapter context IS implied → `[ ICON | TemperatureWhite ]`
- When adapter context is NOT implied → `[ ICON | Odette :: TemperatureWhite ]`

In the sources dropdown, the user selects tags from **multiple adapters**, so adapter context is
**not implied** — the `adapter :: tag` format is required.

## Scope

Only the **dropdown options list** is affected. The selected chips (MultiValueContainer) already
show the correct `adapter :: tag` format since EDG-35/EDG-176.
