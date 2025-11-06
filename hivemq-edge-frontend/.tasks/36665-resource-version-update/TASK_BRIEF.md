# Task: 36665-resource-version-update

## Objective

Refactor the updating flow for resources, especially SCRIPT. Versions are not properly handled currently, leading to inconsistencies.

## Context

The Data Hub feature provides a visual designer for creating:

- **Data Policies**: Rules that apply data transformation on MQTT messages
- **Behavior Policies**: Rules that define behavior of MQTT clients

## Goal

- fix the management of version for resources when updating them

## secondary goals

- [x] The disabled state of the editors are not properly handled for both SCHEMA and SCRIPT (code editor, version)
- [x] The summary of changes in resources only show NEW or UPDATE, even when resources are not going to be modified
- [ ] Validating a minimalist behaviour policy shows an error on the node but a success on the report and publish properly
- [ ] when switching between policy on the canvas, the dry run report is not cleaned up (show report is still accessible and report still available)
- [?] when starting to edit the content of the Monaco Editor, the prompt first go to the end of the document, not the line who received the key inputs
- [x] changing a version doesn't load the content of the new one
