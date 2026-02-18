# Walkthroughs

This directory contains in-depth walkthroughs that explain complex features from both interaction design and technical perspectives.

## Documents

- **[RJSF_COMBINER.md](./RJSF_COMBINER.md)** - Combiner UX walkthrough
  - How we transformed flat RJSF forms into interactive UX
  - Detailed breakdown of 4 custom widgets
  - Data lifecycle through RJSF components
  - FormContext, validation, and scaffolding patterns
  - Testing strategy and common pitfalls

- **[DOMAIN_ONTOLOGY.md](./DOMAIN_ONTOLOGY.md)** - Domain ontology visualization walkthrough
  - Why the workspace canvas is not the answer for ontology visualization
  - Five existing visualization approaches (Sankey, Chord, Sunburst, Cluster, Edge Bundling)
  - Two Phase 1 implementations (Network Graph, Data Flow Tracer)
  - What was not activated and why
  - Lessons learned and deferred work

## Purpose

Walkthroughs provide:
- **Dual perspective**: Both UX rationale and technical implementation
- **Deep dives**: Comprehensive explanation of specific features
- **Decision context**: Why we designed things this way
- **Implementation details**: How data flows, what patterns we use
- **Learning resource**: For developers extending similar patterns

## Difference from Guides

**Guides** teach you "how to do X" with step-by-step instructions.

**Walkthroughs** explain "why and how we built X" with design rationale and architecture.

Example:
- **Guide**: "How to create an RJSF custom widget"
- **Walkthrough**: "How we designed the Combiner UX and implemented its custom widgets"

## Audience

- Developers maintaining or extending complex features
- UX designers understanding implementation constraints
- Technical leads evaluating design decisions
- AI agents implementing similar patterns
- New team members learning the codebase

## Relationship to Other Documentation

**Walkthroughs** complement architecture and guides:
- **Architecture docs** explain system-level structure
- **Walkthroughs** explain feature-level implementation
- **Guides** explain task-level procedures

Walkthroughs bridge the gap between high-level architecture and practical implementation.

---

**See:** [Documentation Index](../INDEX.md) for complete table of contents
