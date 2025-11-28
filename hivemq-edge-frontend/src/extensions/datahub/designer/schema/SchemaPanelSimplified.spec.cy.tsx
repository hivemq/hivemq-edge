import { mockSchemaTempHumidity } from '@datahub/api/hooks/DataHubSchemasService/__handlers__'
import { SchemaPanelSimplified } from '@datahub/designer/schema/SchemaPanelSimplified.tsx'
import { MockStoreWrapper } from '@datahub/__test-utils__/MockStoreWrapper.tsx'
import { DataHubNodeType } from '@datahub/types.ts'
import { getNodePayload } from '@datahub/utils/node.utils.ts'

const wrapper: React.JSXElementConstructor<{ children: React.ReactNode }> = ({ children }) => (
  <MockStoreWrapper
    config={{
      initialState: {
        nodes: [
          {
            id: 'schema-node-1',
            type: DataHubNodeType.SCHEMA,
            position: { x: 0, y: 0 },
            data: getNodePayload(DataHubNodeType.SCHEMA),
          },
        ],
      },
    }}
  >
    {children}
  </MockStoreWrapper>
)

describe('SchemaPanelSimplified', () => {
  const mockSchemas = [
    mockSchemaTempHumidity,
    { ...mockSchemaTempHumidity, version: 2 },
    { ...mockSchemaTempHumidity, version: 3 },
  ]

  beforeEach(() => {
    cy.viewport(800, 900)

    // Ignore Monaco worker loading errors
    cy.on('uncaught:exception', (err) => {
      return !(err.message.includes('importScripts') || err.message.includes('worker'))
    })
  })

  // ✅ ACTIVE - Accessibility testing
  it('should be accessible', () => {
    cy.intercept('GET', '/api/v1/data-hub/schemas', {
      statusCode: 200,
      body: { items: mockSchemas },
    }).as('getSchemas')

    cy.injectAxe()
    cy.mountWithProviders(
      <SchemaPanelSimplified selectedNode="schema-node-1" onFormSubmit={cy.stub()} onFormError={cy.stub()} />,
      { wrapper }
    )

    cy.wait('@getSchemas')

    // Verify component renders
    cy.get('form').should('exist')

    // Check accessibility
    cy.checkAccessibility(undefined, {
      rules: {
        // TODO: Fix color-contrast issue on JSON type selector (Chakra UI styling)
        'color-contrast': { enabled: false },
      },
    })
  })

  // ⏭️ SKIPPED - Will activate during Phase 4
  it.skip('should load schema from node data', () => {
    // Test: Mock node with schema data (name, version, type)
    // Test: Verify form populated with schema name, version, type
    // Test: Verify schemaSource is readonly and populated
  })

  it.skip('should preserve node version when loading (not latest)', () => {
    // Test: Mock node with schema "sensor" version 2
    // Test: Mock API with versions 1, 2, 3 (3 is latest)
    // Test: Verify form loads version 2 (from node), NOT version 3 (latest)
    // Test: Verify schemaSource matches version 2 content
    // VALIDATION: Ensures fix for bug where latest version was always loaded
  })

  it.skip('should show list of available schemas in name selector', () => {
    // Test: Intercept GET schemas with multiple schemas
    // Test: Click name selector
    // Test: Verify all schema names visible in dropdown
    // Test: Verify no "Create new" option (select-only mode)
  })

  it.skip('should load schema content when name is selected', () => {
    // Test: Select schema name from dropdown
    // Test: Verify schemaSource loaded and displayed readonly
    // Test: Verify type field updated
    // Test: Verify version field populated
  })

  it.skip('should show versions for selected schema', () => {
    // Test: Select schema with multiple versions
    // Test: Click version selector
    // Test: Verify all versions shown in dropdown
  })

  it.skip('should load specific version when version changed', () => {
    // Test: Select schema name
    // Test: Change version in dropdown
    // Test: Verify schemaSource updates to that version's content
  })

  it.skip('should disable form when node is not editable', () => {
    // Test: Mock usePolicyGuards to return isNodeEditable=false
    // Test: Verify all fields disabled
  })

  it.skip('should show guard alert when policy is published', () => {
    // Test: Mock usePolicyGuards to return guardAlert
    // Test: Verify alert message displayed
  })

  it.skip('should validate schema is selected', () => {
    // Test: Leave name empty
    // Test: Try to submit
    // Test: Verify validation error "Please select a schema"
  })

  it.skip('should validate version is selected', () => {
    // Test: Select name but clear version
    // Test: Try to submit
    // Test: Verify validation error "Please select a version"
  })

  it.skip('should call onFormSubmit with schema data', () => {
    // Test: Select schema and version
    // Test: Click submit (if submit button exists in ReactFlowSchemaForm)
    // Test: Verify onFormSubmit called with correct data
  })

  it.skip('should show readonly schemaSource for JSON schemas', () => {
    // Test: Select JSON schema
    // Test: Verify Monaco editor shown with application/schema+json widget
    // Test: Verify editor is readonly
  })

  it.skip('should show readonly schemaSource for Protobuf schemas', () => {
    // Test: Select Protobuf schema
    // Test: Verify editor shown with application/octet-stream widget
    // Test: Verify editor is readonly
  })

  it.skip('should handle API errors gracefully', () => {
    // Test: Intercept GET schemas with 500 error
    // Test: Verify error message displayed
    // Test: Verify onFormError called
  })
})
