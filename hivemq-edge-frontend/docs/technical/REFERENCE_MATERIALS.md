---
title: "Reference Materials"
author: "Edge Frontend Team"
last_updated: "2026-02-17"
purpose: "Catalogue of external design and planning artefacts — Miro boards, design files, and other reference materials relevant to the Edge frontend"
audience: "Developers and AI agents needing context beyond the codebase itself"
maintained_at: "docs/technical/REFERENCE_MATERIALS.md"
---

# Reference Materials

A catalogue of external design and planning artefacts that provide context, rationale, and design intent behind the frontend codebase. These are not maintained in the repository, but are authoritative sources for understanding architectural decisions and UX thinking.

**Access:** Miro boards require a Miro account. Most boards are owned by and shared from Nicolas Van Labeke's workspace. See [External Services](./EXTERNAL_SERVICES.md) for login details.

---

## Table of Contents

- [Miro Boards](#miro-boards)
  - [Workspace, Topology & Visual Design](#workspace-topology--visual-design)
  - [Pulse, Asset Mapping & Data Modelling](#pulse-asset-mapping--data-modelling)
  - [DataHub Designer & Edge Integration](#datahub-designer--edge-integration)
  - [Domain Modelling, Patterns & Practices](#domain-modelling-patterns--practices)
- [Figma Boards](#figma-boards)

- [Board-to-Documentation Map](#board-to-documentation-map)

---

## Miro Boards

14 boards covering UX exploration, architecture decisions, domain modelling, and engineering practices. They vary in purpose: some are active design references, others document decisions already implemented, and some capture explorations that have since been superseded.

### Workspace, Topology & Visual Design

Boards covering the topology canvas, node/edge design, visual identity, and workspace interaction patterns.

| Board | URL | Summary |
|-------|-----|---------|
| **Edge \| Workspace deconstructed** | [Open](https://miro.com/app/board/uXjVJkiIoj4=) | Deconstructs current and planned workspace UX: wizard flows for creating adapters/bridges/mappings, integration points, and linked epics/unresolved issues — groups, search/filtering, device metadata, workspace persistence. |
| **Edge Workspace: Metrics, Observability and Dynamic Layout** | [Open](https://miro.com/app/board/uXjVNJL2wTw=) | Concept for contextual observability: per-node and grouped metrics, dynamic/manual grouping, and user-defined dashboards backed by curated Edge metrics. |
| **Edge \| Alternative Workspace as ETL Designer** | [Open](https://miro.com/app/board/uXjVIFl3nyY=) | Explores treating the workspace as an ETL canvas — how adapters, devices, combiners, Pulse agents, asset mappers, bridges, and DataHub mappers could align in a unified model. |
| **Edge – Protocol and Adapter Visual Identity** | [Open](https://miro.com/app/board/uXjVNEM9gFw=) | Analyses inconsistencies in how protocols, adapters, groups, bridges, gateways, and events are visually represented across workspace, panels, and logs. Argues for a coherent entity design system. |

**See:** [Workspace Architecture](../architecture/WORKSPACE_ARCHITECTURE.md)

---

### Pulse, Asset Mapping & Data Modelling

Boards covering Pulse integration, asset mapper UX, tag browsing, and bulk subscription creation.

| Board | URL | Summary |
|-------|-----|---------|
| **Pulse in Edge UX** | [Open](https://miro.com/app/board/uXjVJaC7WgA=) | Full UX and API story for Pulse-in-Edge: managed assets and asset mappers, their schemas and mappings, workspace representation, status propagation, activation tokens, and notification patterns. |
| **Edge Tag Browsing, OPC UA – Bleecker Project Plan** | [Open](https://miro.com/app/board/uXjVGPgLIrM=) | OPC UA tag browsing and batch tag creation: discovery APIs, tag-schema metadata, RJSF-based data-point selector, and CSV/Excel-driven bulk upload and validation flow. |
| **Edge: Bulk Creation of Subscriptions** | [Open](https://miro.com/app/board/uXjVKDwB5QM=) | Compares UX options for bulk subscription creation: editable in-app data grid vs. file-upload-driven approach, both constrained to required fields only. |

**See:** [Workspace Architecture](../architecture/WORKSPACE_ARCHITECTURE.md)

---

### DataHub Designer & Edge Integration

Boards covering the policy designer canvas, resource handling, and FSM behaviour models.

| Board | URL | Summary |
|-------|-----|---------|
| **DataHub – Edge Integration** | [Open](https://miro.com/app/board/uXjVN7kkrF8=) | High-level concept for integrating DataHub's policy canvas with Edge: topic filters, behaviour/data policies, schemas/scripts, RJSF-based node editors, OpenAPI constraints, capabilities, and frontend toolchain assumptions. |
| **DataHub Designer – Resource Handling Revamp** | [Open](https://miro.com/app/board/uXjVJnfJQVg=) | Compares "Plan A" (resource editing tied to policy nodes) vs "Plan B" (central resource editor) for schemas/scripts: flows for creating new versions, read-only vs editable states, pros and cons, state-management complexity. |
| **DataHub Designer – FSM User-Facing Strings** | [Open](https://miro.com/app/board/uXjVGKMvdqc=) | Behaviour-model FSMs: correct event/guard naming, ordering of transitions, UX copy for models like `Publish.quota` / `Publish.duplicate`, and aligning visuals and labels with the OpenAPI-backed JSON schema. |

**See:** [DataHub Architecture](../architecture/DATAHUB_ARCHITECTURE.md)

---

### Domain Modelling, Patterns & Practices

Boards covering domain entity relationships, engineering practices, and cross-cutting UX patterns.

| Board | URL | Summary |
|-------|-----|---------|
| **Edge Domain Ontology** | [Open](https://miro.com/app/board/uXjVLpkTwW8=) | Domain map for Edge: adapters, devices, tags, topic filters, combiners, groups, and data-combining mappings. Includes JSON-schema shapes and API sketches for combiners and mapping instructions. Side-by-side comparison of data flow with and without `TopicFilter`. |
| **Date and Time: Display and Filtering** | [Open](https://miro.com/app/board/uXjVNRLUwS0=) | Reference for date/time UX: comparisons of rule-based time filters, relative time pickers, range pickers, and timestamped table filtering patterns (e.g. DataDog-style timeframe picker). |
| **Frontend Testing Pyramid** | [Open](https://miro.com/app/board/uXjVMv__p30=) | Frontend testing strategy: layers from unit/logic to interaction and acceptance, visual regression and accessibility, mapped to tools like Cypress, Percy, Axe, Lighthouse, and CI integration. |
| **Edge: Quick Retrospective** | [Open](https://miro.com/app/board/uXjVM5BD3rM=) | Retrospective board: documentation gaps, OpenAPI spec maintenance, test coverage trade-offs, and calls for better project-management tooling. |

**See:** [Testing Architecture](../architecture/TESTING_ARCHITECTURE.md) (testing pyramid), [Protocol Adapter Architecture](../architecture/PROTOCOL_ADAPTER_ARCHITECTURE.md) (domain entities)

---

## Figma Boards

 Board                                   | URL | Summary                                                                                |
|-----------------------------------------|-----|----------------------------------------------------------------------------------------|
| **Original Bridge designs**             | [Open](https://www.figma.com/file/PMoMzXRmKLnlIalZxlOS1M/Edge?type=design&node-id=241%3A114538&t=6dIRdUn8zXnjPxmP-1) | First design proposal for the Bridge creation - and original design artefacts for Edge |
| **Definitive Bridge designs **          | [Open](https://www.figma.com/file/PMoMzXRmKLnlIalZxlOS1M/Edge?type=design&node-id=607%3A1102408&mode=design&t=ivBgHbTpTV4ryqyQ-1) | Definitive version of the design                                                       |
| **Edge Data Models + Domain mapping  ** | [Open](https://www.figma.com/design/goCjhfUWBP3Fc3e73BaWD0/Edge---Data-Models---Mapping?m=auto&t=lmUJ6TL3bwQHQj97-6) | First draft of the contemporary Edge                                                   |
| **Edge inventory 2024.9  **             | [Open](https://www.figma.com/design/GUsY1u8yDtIl7OE9XApfY1/EDGE?node-id=3-18&p=f&t=lmUJ6TL3bwQHQj97-0) | Edge review v2024.9                                                                    |
| **Edge inventory 2024.9  **             | [Open](https://www.figma.com/design/goCjhfUWBP3Fc3e73BaWD0/Edge-Initiatives?node-id=960-282183&t=nucffe3DJCrYEkDe-4) | Edge review v2025.1                                                                  |


## Board-to-Documentation Map

Quick reference for finding the board most relevant to a given documentation area.

| Documentation Area | Most Relevant Boards |
|--------------------|------------------------|
| [Workspace Architecture](../architecture/WORKSPACE_ARCHITECTURE.md) | Workspace deconstructed, Alternative Workspace as ETL Designer |
| [DataHub Architecture](../architecture/DATAHUB_ARCHITECTURE.md) | DataHub – Edge Integration, Resource Handling Revamp, FSM User-Facing Strings |
| [Testing Architecture](../architecture/TESTING_ARCHITECTURE.md) | Frontend Testing Pyramid |
| [State Management](../architecture/STATE_MANAGEMENT.md) | Workspace deconstructed (persistence issues) |
| Protocol adapters / tag browsing | Edge Tag Browsing / Bleecker, Protocol and Adapter Visual Identity |
| Metrics / observability | Edge Workspace: Metrics, Observability and Dynamic Layout |
| Pulse / asset mapping | Pulse in Edge UX |
| Domain entity model | Edge Domain Ontology |
| Bulk operations | Edge: Bulk Creation of Subscriptions |
