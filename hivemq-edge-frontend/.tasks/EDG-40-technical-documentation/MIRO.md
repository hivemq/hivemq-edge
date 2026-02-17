---
title: 'HiveMQ – Nicolas Van Labeke: Miro Boards Overview'
author: 'Nicolas Van Labeke'
date: 2026-02-17
---

# Titled Miro boards (with summaries)

## Edge workspace, topology & visual design

- **[Edge | Workspace deconstructed](https://miro.com/app/board/uXjVJkiIoj4=)**  
  Deconstructs the current and planned workspace UX: wizard flows for creating adapters/bridges/mappings, integration points, and linked epics/unresolved issues (groups, search/filtering, device metadata, workspace persistence).

- **[Edge Workspace: Metrics, Observability and Dynamic layout](https://miro.com/app/board/uXjVNJL2wTw=)**  
  Concept for contextual observability: per-node and grouped metrics, dynamic/manual grouping, and user-defined dashboards backed by curated Edge metrics.

- **[Edge | Alternative Workspace as ETL Designer](https://miro.com/app/board/uXjVIFl3nyY=)**  
  Explores treating the workspace as an ETL canvas, showing how adapters, devices, combiners, Pulse agents, asset mappers, bridges, and DataHub mappers could align in a unified model.

- **[Edge – Protocol and Adapter visual identity](https://miro.com/app/board/uXjVNEM9gFw=)**  
  Analyses inconsistencies in how protocols, adapters, groups, bridges, gateways, and events are visually represented across workspace, panels, and logs, and argues for a coherent entity design system.

## Pulse, asset mapping & Edge-side data modelling

- **[Pulse in Edge UX](https://miro.com/app/board/uXjVJaC7WgA=)**  
  Full UX + API story for Pulse-in-Edge: managed assets and asset mappers, their schemas and mappings, workspace representation, status propagation, activation tokens, and notification/TODO patterns.

- **[Edge Tag Browsing, OPC UA – Bleecker Project Plan](https://miro.com/app/board/uXjVGPgLIrM=)**  
  OPC UA tag browsing and batch tag creation: discovery APIs, tag-schema metadata, RJSF-based data-point selector, and CSV/Excel-driven bulk upload and validation flow.

- **[Edge: Bulk creation of subscriptions](https://miro.com/app/board/uXjVKDwB5QM=)**  
  Compares UX options for bulk subscription creation: editable in-app data grid vs file-upload driven approach, both constrained to required fields only.

## DataHub Designer & Edge integration

- **[DataHub – Edge integration](https://miro.com/app/board/uXjVN7kkrF8=)**  
  High-level concept for integrating DataHub’s policy canvas with Edge: topic filters, behavior/data policies, schemas/scripts, RJSF-based node editors, OpenAPI constraints, capabilities, and frontend toolchain assumptions.

- **[Datahub Designer – Resource handling revamp](https://miro.com/app/board/uXjVJnfJQVg=)**  
  Compares “Plan A” (resource editing tied to policy nodes) vs “Plan B” (central resource editor) for schemas/scripts: flows for creating new versions, read-only vs editable states, pros/cons and state-management complexity.

- **[DataHub Designer – FSM user-facing strings](https://miro.com/app/board/uXjVGKMvdqc=)**  
  Focuses on behaviour-model FSMs: correct event/guard naming, ordering of transitions, UX copy for models like `Publish.quota` / `Publish.duplicate`, and aligning visuals and labels with the OpenAPI-backed JSON schema.

## Domain modelling, patterns & practices

- **[Edge Domain Ontology](https://miro.com/app/board/uXjVLpkTwW8=)**  
  Domain map for Edge: adapters, devices, tags, topic filters, combiners, groups and data-combining mappings; includes JSON-schema shapes and API sketches for combiners and mapping instructions.

- **[Date and Time: display and filtering](https://miro.com/app/board/uXjVNRLUwS0=)**  
  Reference board for date/time UX: comparisons of rule-based time filters, relative time pickers, range pickers and timestamped table filtering patterns (e.g. DataDog-style timeframe picker).

- **[Frontend Testing Pyramid](https://miro.com/app/board/uXjVMv__p30=)**  
  Frontend testing strategy: layers from unit/logic to interaction & acceptance, visual regression and accessibility, mapped to tools like Cypress, Percy, Axe, Lighthouse and CI integration.

- **[Edge: Quick Retrospective](https://miro.com/app/board/uXjVM5BD3rM=)**  
  Retro board for Edge work: “Stop/Continue/Invent/Act” sections capturing documentation gaps, OpenAPI spec maintenance, test coverage trade-offs, and calls for better project-management tooling and history of contributions.
