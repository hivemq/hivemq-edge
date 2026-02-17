# Task XXXXX: Frontend Chakra V3 Migration \- Investigation Prompts

This document contains all user prompts that triggered investigation documents for this task.

---

## Prompt 1: Initial Investigation (December 12, 2025\)

**Triggered Documents:**

- `REPORT_FEATURE_DISTRIBUTION.md`
- `REPORT_OPENAPI_MIGRATION.md`
- `REPORT_RJSF_MIGRATION.md`

We are starting a new task, XXXXX-frontend-chakrav3, preparing a migration of the frontend UI, from Chakra v2 to v3.

To do that there are a few investigations I'd like you to run, to try to quantify the complexity of the task.

The issues to investigate and report on are, to start with, the following:

- We have two significant features that have their own identity in the frontend: the Workspace and the DataHub. How much code, React components, ui tests does they each represent, compared to the whole app?
- We are using an OpenAPI stub generator, to create types and axios-based handlers, that is deprecated (`ferdikoomen/openapi-typescript-codegen`). We need to migrate to `hey-api/openapi-ts` (or maybe `fabien0102/openapi-codegen`) to be future-proof and have proper support for Problem Detail. What is the impact in terms of code, name changes, type changes and infrastructure (e.g. use with React Query)
- We are using RJSF (`rjsf-team/react-jsonschema-form`) to manage specs-based forms in many parts of the UI. The current version is now deprecated. The new version will provide support for ChakraV3 but is likely to create issues with all the custom components (widgets, templates, etc.) we created. What is the current spread of ALL custom components across the app and the impact of their migration in terms of code, tests, etc.

Can you generate structured reports, with qualitative and quantitative information, on these points? There will be more aspects to look at as we progress on this task.

---

## Prompt 2: UX Paradigm Migration Analysis (December 12, 2025\)

**Triggered Document:**

- `UX_PARADIGM_ANALYSIS.md`

A new aspect to look at: migrating to a different UX paradigm for the visually intensive features.

As you already noted, both Workspace and DataHub rely on React Flow for the core UX of the features. It creates certain requirements on interactivity and design, e.g. drag-and-drop, task-based actions, etc.

There is a new drive to implement a more "traditional" UX, especially for first-time users. This new UX can be described as "resource" listings plus editing functionalities, as implemented for example with RJSF out of the relevant APIs (with the additional custom widgets).

I'd like you to consider the implication of adding support for such a UX paradigm in the frontend, from an engineering point of view, as well as design and product management. An analysis of the potential gains for switching (rather than extending) to the new paradigm would be welcome as well.

---

## Prompt 3: Accessibility Challenge (December 12, 2025\)

**Triggered Update:**

- `UX_PARADIGM_ANALYSIS.md` (Section 3.3 revised)

I'd like to challenge you on the current support for accessibility with React Flow (section 3.3 Accessibility Improvements). Could you consider what React Flow is stating about the current support, in their documentation [https://reactflow.dev/learn/advanced-use/accessibility](https://reactflow.dev/learn/advanced-use/accessibility) or their code [https://github.com/xyflow/xyflow](https://github.com/xyflow/xyflow)? Maybe also by checking the tests we might have in the code base (we have Deque-based a11y tests in all components)

---

## Prompt 4: OpenAPI Quality Analysis (December 12, 2025\)

**Triggered Document:**

- `OPENAPI_QUALITY_ANALYSIS.md`

Another line of enquiry: the quality of the OpenAPI specs that is feeding the frontend. It's residing in a directory parallel the frontend code, main README attached.

I'm interested in a qualitative (maybe quantitative) assessment of the quality, descriptive power, completeness and type-safe aspect of it. Two areas are of concerns, in relation to the point before about new paradigm: DataHub and Workspace.

For context, you might want to pay attention to the layers added in the frontend for DataHub to specify aspects that were not provided from the OpenAPI (and its generated stubs)

---

## Prompt 5: Rebuild Proposal Evaluation (December 12, 2025\)

**Triggered Document:**

- `PROPOSAL_REBUILD_ANALYSIS.md`

I'd like to make the following proposal:

- Drop both ReactFlow paradigms from Edge
- Switch to a RJSF-driven approach of CRUD endpoints
- Do not migrate to ChakraV3 but rebuild the Edge application from scratch, with a ChakraV3 core and local design artefacts

Could you balance such proposal with arguments for and against from the analysis you did over the issues?

---

## Prompt 6: Chakra V3 Migration Quantification (December 12, 2025\)

**Triggered Document:**

- `CHAKRA_V3_MIGRATION_ANALYSIS.md`

Let's try a quantification of the migration to ChakraV3, especially given your proposal for a hybrid approach.

ChakraUI has a few documents to help the migration, both that could help you to create an overview of the cost, timeline and plan for such migration:

- the migration documentation, [https://chakra-ui.com/llms-v3-migration.txt](https://chakra-ui.com/llms-v3-migration.txt)
- the full documentation, [https://chakra-ui.com/llms-components.txt](https://chakra-ui.com/llms-components.txt)

---

## Document Index

| Prompt   | Documents Generated                                                                         |
| :------- | :------------------------------------------------------------------------------------------ |
| Prompt 1 | `REPORT_FEATURE_DISTRIBUTION.md`, `REPORT_OPENAPI_MIGRATION.md`, `REPORT_RJSF_MIGRATION.md` |
| Prompt 2 | `UX_PARADIGM_ANALYSIS.md`                                                                   |
| Prompt 3 | `UX_PARADIGM_ANALYSIS.md` (Section 3.3 update)                                              |
| Prompt 4 | `OPENAPI_QUALITY_ANALYSIS.md`                                                               |
| Prompt 5 | `PROPOSAL_REBUILD_ANALYSIS.md`                                                              |
| Prompt 6 | `CHAKRA_V3_MIGRATION_ANALYSIS.md`                                                           |

---

**Last Updated:** December 12, 2025
