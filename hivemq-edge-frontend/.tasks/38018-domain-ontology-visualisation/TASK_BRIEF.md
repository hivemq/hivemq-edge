# Task 38018: Domain Ontology Visualisation

## Overview

Enhance and extend the existing domain ontology visualization system to better represent connectivity and transformations between integration points in the HiveMQ Edge workspace.

## Background

Building on task 25337 (workspace entities and auto-layout), this task focuses on improving visualizations for:

- **Integration Points**: TAG (DEVICE), TOPIC (ADAPTER), TOPIC FILTER (EDGE)
- **Transformations**: Northbound mapping, Southbound mapping, Combiners, Bridge subscriptions
- **Relationships**: How data flows through the transformation pipeline

### Current Implementation (Proof of Concept)

The system already has a `DomainOntologyManager` component accessible via the Edge node side panel with:

- **Sankey Diagram** (ConceptFlow) - Data flow visualization
- **Chord Diagram** (RelationMatrix) - Relationship matrix
- **Sunburst Chart** (ConceptWheel) - Hierarchical exploration
- **Cluster View** (AdapterCluster) - Grouped visualization
- **Edge Bundling** (experimental) - Connection patterns

**Hook**: `useGetDomainOntology` aggregates all integration points and transformation flows

## Objectives

1. **Analyze** existing visualization implementations for strengths/weaknesses
2. **Enhance** current visualizations with improved interactivity and clarity
3. **Document** the domain model and transformation flows comprehensively
4. **Optimize** performance for large datasets (>100 integration points)
5. **Extend** with new visualization types based on data science insights

## Current Architecture

### Data Sources

```typescript
useGetDomainOntology() returns:
- tags: DomainTag[] (device data points)
- topicFilters: TopicFilter[] (edge subscriptions)
- northMappings: NorthboundMapping[] (TAG → TOPIC)
- southMappings: SouthboundMapping[] (TOPIC FILTER → TAG)
- bridgeSubscriptions: { topics, topicFilters, mappings }
```

### Existing Visualizations

1. **Sankey Flow** (`useGetSankeyData`) - Shows TAG → TOPIC → FILTER flows
2. **Chord Matrix** (`useGetChordMatrixData`) - Adjacency matrix with directionality
3. **Sunburst** (`useGetSunburstData`) - Hierarchical topic structure
4. **Cluster** (`useGetClusterData`) - Adapter grouping
5. **Edge Bundling** (experimental) - Hierarchical edge bundling

### UI Location

- **Access**: Edge node → Select → "Open side panel" button
- **Component**: `EdgePropertyDrawer` → `DomainOntologyManager`
- **Layout**: Tabbed interface with 4-5 visualization types

## Technical Considerations

- Leverage existing Nivo charts infrastructure
- Integrate with `useGetDomainOntology` hook
- Follow DataHub architecture patterns
- React-based implementation with TypeScript
- Performance for large workspaces (>100 entities)
- Accessibility requirements (WCAG compliance)
- E2E testing following project guidelines

## Success Criteria

- [ ] Comprehensive documentation of domain model
- [ ] Analysis of existing visualization strengths/weaknesses
- [ ] Performance benchmarks for current implementations
- [ ] Recommendations for improvements
- [ ] Implementation of at least 2 enhancements
- [ ] Full E2E test coverage for new features
- [ ] Accessibility compliance

## Research Questions

1. How do users currently interact with domain ontology visualizations?
2. What insights are most valuable for understanding integration topology?
3. Which visualization types are most effective for different use cases?
4. What performance bottlenecks exist in current implementations?
5. How can we better represent bidirectional transformations?

## Status

**Active** - Discovery phase, analyzing current implementation
