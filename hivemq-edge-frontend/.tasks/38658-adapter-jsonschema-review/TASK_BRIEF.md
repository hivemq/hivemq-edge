# Task Brief: Adapter JSON-Schema & UI-Schema Review

## Task ID: 38658-adapter-jsonschema-review

## Objective

Conduct a comprehensive cross-functional research task analyzing all protocol adapter JSON-Schema and UI-Schema configurations in HiveMQ Edge to identify inconsistencies, missing fields, and improvement opportunities.

## Background

HiveMQ Edge uses React JSON Schema Form (RJSF) to dynamically render adapter configuration forms. Each protocol adapter defines:

1. **JSON Schema** - Defines the data structure, types, and validation rules
2. **UI Schema** - Controls how the form is rendered (widgets, ordering, help text)

These schemas are defined in both:

- **Frontend**: TypeScript mock data for testing
- **Backend**: Java classes with annotations

## Scope

### In Scope

- All protocol adapters (HTTP, MQTT, OPC-UA, Modbus, S7, ADS, EIP, File, Databases, Simulation)
- JSON Schema validation and completeness
- UI Schema field ordering and widget selection
- Cross-adapter consistency patterns
- Backend vs Frontend schema alignment
- Tag schema configurations

### Out of Scope

- Runtime adapter behavior
- Performance optimization
- New adapter development
- Backend code changes (analysis only)

## Deliverables

1. **ADAPTER_INVENTORY.md** - Complete catalog of all adapters with schema locations
2. **SCHEMA_ANALYSIS.md** - Detailed analysis of each adapter's schemas
3. **REMEDIATION_REPORT.md** - Prioritized list of issues with recommended fixes
4. **BACKEND_COMPARISON.md** - Backend vs Frontend schema comparison

## Success Criteria

- [ ] All adapters documented in inventory
- [ ] Schema analysis complete for all adapters
- [ ] Issues categorized by severity (Critical/High/Medium/Low)
- [ ] Actionable recommendations provided
- [ ] Backend schemas compared with frontend

## Analysis Timestamp

**Commit:** (to be recorded)
**Branch:** master
**Analyzed:** (to be recorded)

## Related Documents

- [RJSF_GUIDELINES.md](../../hivemq-edge/src/frontend/RJSF_GUIDELINES.md)
- [ADAPTER_INVENTORY.md](./ADAPTER_INVENTORY.md)
- [SCHEMA_ANALYSIS.md](./SCHEMA_ANALYSIS.md)
- [REMEDIATION_REPORT.md](./REMEDIATION_REPORT.md)
