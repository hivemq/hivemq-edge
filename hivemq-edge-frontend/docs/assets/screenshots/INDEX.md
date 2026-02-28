# Screenshot Index

**Last Updated:** 2026-02-16

This directory contains screenshots organized by feature/domain for use across multiple documentation files.

**Organization:** Screenshots are grouped by what they show, not where they're used. This enables reuse across architecture docs, guides, and technical references.

**Viewport Standard:** All E2E screenshots use HD (1280x720) for consistency.

---

## Directory Structure

```
screenshots/
├── datahub/         # DataHub extension screenshots
├── workspace/       # Workspace/topology screenshots
├── adapters/        # Protocol adapter screenshots
├── bridges/         # Bridge configuration screenshots
├── ui-components/   # Shared UI component screenshots
├── development/     # Development tools/processes
└── common/          # Common UI elements/patterns
```

---

## DataHub

**Total:** 5 screenshots

| Screenshot | Description | Used In | Test Source |
|------------|-------------|---------|-------------|
| `datahub-designer-canvas-empty.png` | Empty policy designer canvas with toolbox and toolbar | **docs/architecture/DATAHUB_ARCHITECTURE.md** (Figure 1) | `cypress/e2e/datahub/datahub-documentation-screenshots.spec.cy.ts` |
| `datahub-schema-table-empty-state.png` | Schema table with "No schemas found" message | **docs/architecture/DATAHUB_ARCHITECTURE.md** (Figure 2) | `cypress/e2e/datahub/datahub-documentation-screenshots.spec.cy.ts` |
| `datahub-schema-table-with-data.png` | Schema table with single example schema entry | **docs/architecture/DATAHUB_ARCHITECTURE.md** (Figure 3) | `cypress/e2e/datahub/datahub-documentation-screenshots.spec.cy.ts` |
| `datahub-policy-table-empty-state.png` | Policy table with "No policies found" message | _Available for use_ | `cypress/e2e/datahub/datahub-documentation-screenshots.spec.cy.ts` |
| `datahub-script-table-empty-state.png` | Script table with "No scripts found" message | _Available for use_ | `cypress/e2e/datahub/datahub-documentation-screenshots.spec.cy.ts` |

**Next Steps:**
- Add screenshots to `docs/architecture/DATAHUB_ARCHITECTURE.md`
- Create `designer-canvas-with-nodes.png` with example policy nodes
- Create validation workflow screenshots

---

## Combiner

**Total:** 9 screenshots

| Screenshot | Description | Used In | Test Source |
|------------|-------------|---------|-------------|
| `combiner-tabs-navigation.png` | Combiner form drawer showing tab navigation (Configuration, Sources, Mappings) | **docs/walkthroughs/RJSF_COMBINER.md** | `cypress/e2e/mappings/combiner-documentation-screenshots.spec.cy.ts` |
| `combiner-mapping-table.png` | Mapping table with 3 example mappings showing summaries | **docs/walkthroughs/RJSF_COMBINER.md** | `src/modules/Mappings/combiner/DataCombiningTableField.spec.cy.tsx` |
| `combiner-empty-state.png` | Empty mapping table with "No data received yet" message | **docs/walkthroughs/RJSF_COMBINER.md** | `src/modules/Mappings/combiner/DataCombiningTableField.spec.cy.tsx` |
| `combiner-primary-select.png` | Primary data key selector dropdown showing tag and topic filter options | **docs/walkthroughs/RJSF_COMBINER.md** | `src/modules/Mappings/combiner/PrimarySelect.spec.cy.tsx` |
| `combiner-native-form-flat.png` | Native RJSF form showing flat vertical list of all fields | **docs/walkthroughs/RJSF_COMBINER.md** | `src/modules/Mappings/CombinerMappingManager.spec.cy.tsx` |
| `combiner-mapping-drawer.png` | Mappings tab with empty table (drawer closed) | **docs/walkthroughs/RJSF_COMBINER.md** | `cypress/e2e/mappings/combiner-documentation-screenshots.spec.cy.ts` |
| `combiner-mapping-drawer-open.png` | Mapping editor drawer open showing sources, destination, schemas | **docs/walkthroughs/RJSF_COMBINER.md** | `cypress/e2e/mappings/combiner-documentation-screenshots.spec.cy.ts` |
| `combiner-entity-select.png` | Multi-select dropdown showing entity options with metadata (icon, description, tag count) | **docs/walkthroughs/RJSF_COMBINER.md** | `src/modules/Mappings/combiner/CombinedEntitySelect.spec.cy.tsx` |
| `combiner-editor-field.png` | Full editor field showing split layout (sources left, destination right) | **docs/walkthroughs/RJSF_COMBINER.md** | `src/modules/Mappings/combiner/DataCombiningEditorField.spec.cy.tsx` |

---

## Workspace

Currently empty. Screenshots will be added as tests are created.

**Planned:**
- `workspace-healthy-all-operational.png` - Healthy workspace view
- `workspace-layout-after-radial.png` - Workspace with radial layout applied
- `wizard-01-menu.png` - Workspace wizard menu
- `wizard-02-adapter-selection.png` - Adapter type selection

---

## Adapters

Currently empty.

---

## Bridges

Currently empty.

---

## UI Components

Currently empty.

**Planned:**
- `button-variants.png` - All button variants (primary, outline, ghost, danger)
- `form-states-example.png` - Form states (empty, filled, error, success)

---

## Development

Currently empty.

**Planned:**
- `cypress-test-runner.png` - Cypress component test runner

---

## Common

Currently empty.

---

## Usage Guidelines

### Referencing Screenshots

Use relative paths from documentation files:

```markdown
<!-- From docs/architecture/*.md -->
![Description](../assets/screenshots/datahub/screenshot-name.png)

<!-- From docs/guides/*.md -->
![Description](../assets/screenshots/workspace/screenshot-name.png)
```

### Reusing Screenshots

The same screenshot can be referenced in multiple documents:

```markdown
<!-- In docs/architecture/DATAHUB_ARCHITECTURE.md -->
![DataHub policy designer interface](../assets/screenshots/datahub/designer-canvas-empty.png)

<!-- In docs/guides/DATAHUB_GUIDE.md -->
![Start with a clean canvas](../assets/screenshots/datahub/designer-canvas-empty.png)
```

### Naming Convention

All screenshots follow: `{feature}-{state}-{description}.png`

Examples:
- `datahub-designer-canvas-empty.png`
- `workspace-wizard-01-menu.png`
- `schema-table-with-data.png`

### Quality Standards

- **Viewport:** HD (1280x720) for all E2E screenshots
- **Format:** PNG
- **State:** Clean UI, realistic data, no dev tools
- **Test:** Every screenshot must have a test that can regenerate it

---

## Maintenance

### Adding New Screenshots

1. Create or update test in `cypress/e2e/` or `src/`
2. Run test to generate screenshot in `cypress/screenshots/`
3. Copy to appropriate `docs/assets/screenshots/{domain}/` directory
4. Update this INDEX.md with entry
5. Reference in documentation with caption and alt text

### Updating Screenshots

When UI changes:
1. Re-run the test to regenerate screenshot
2. Copy updated screenshot to docs/assets/screenshots/
3. Verify all documentation references still accurate

### Review Schedule

- **After UI changes:** Re-run affected screenshot tests
- **Quarterly:** Review all screenshots for accuracy
- **Before releases:** Verify screenshot quality and relevance

---

**See Also:**
- [Screenshot Guidelines](../../../.tasks/EDG-40-technical-documentation/SCREENSHOT_GUIDELINES.md)
- [Documentation Acceptance Criteria](../../../.tasks/EDG-40-technical-documentation/DOCUMENTATION_ACCEPTANCE_CRITERIA.md)
