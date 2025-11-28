import { mockScript } from '@datahub/api/hooks/DataHubScriptsService/__handlers__'
import { FunctionPanelSimplified } from '@datahub/designer/script/FunctionPanelSimplified.tsx'
import { MockStoreWrapper } from '@datahub/__test-utils__/MockStoreWrapper.tsx'
import { DataHubNodeType } from '@datahub/types.ts'
import { getNodePayload } from '@datahub/utils/node.utils.ts'

const wrapper: React.JSXElementConstructor<{ children: React.ReactNode }> = ({ children }) => (
  <MockStoreWrapper
    config={{
      initialState: {
        nodes: [
          {
            id: 'function-node-1',
            type: DataHubNodeType.FUNCTION,
            position: { x: 0, y: 0 },
            data: getNodePayload(DataHubNodeType.FUNCTION),
          },
        ],
      },
    }}
  >
    {children}
  </MockStoreWrapper>
)

describe('FunctionPanelSimplified', () => {
  const mockScripts = [mockScript, { ...mockScript, version: 2 }, { ...mockScript, version: 3 }]

  beforeEach(() => {
    cy.viewport(800, 900)

    // Ignore Monaco worker loading errors
    cy.on('uncaught:exception', (err) => {
      return !(err.message.includes('importScripts') || err.message.includes('worker'))
    })
  })

  // ✅ ACTIVE - Accessibility testing
  it('should be accessible', () => {
    cy.intercept('GET', '/api/v1/data-hub/scripts*', {
      statusCode: 200,
      body: { items: mockScripts },
    }).as('getScripts')

    cy.injectAxe()
    cy.mountWithProviders(
      <FunctionPanelSimplified selectedNode="function-node-1" onFormSubmit={cy.stub()} onFormError={cy.stub()} />,
      { wrapper }
    )

    cy.wait('@getScripts')

    // Verify component renders
    cy.get('form').should('exist')

    // Check accessibility
    cy.checkAccessibility(undefined, {
      rules: {
        // TODO: Fix color-contrast issue (Chakra UI styling)
        'color-contrast': { enabled: false },
      },
    })
  })

  // ⏭️ SKIPPED - Will activate during Phase 4
  it.skip('should load script from node data', () => {
    // Test: Mock node with script data (name, version, description)
    // Test: Verify form populated with script name, version, description
    // Test: Verify sourceCode is readonly and populated
  })

  it.skip('should preserve node version when loading (not latest)', () => {
    // Test: Mock node with script "processor" version 2
    // Test: Mock API with versions 1, 2, 3 (3 is latest)
    // Test: Verify form loads version 2 (from node), NOT version 3 (latest)
    // Test: Verify sourceCode matches version 2 content
    // VALIDATION: Ensures fix for bug where latest version was always loaded
  })

  it.skip('should show list of available scripts in name selector', () => {
    // Test: Intercept GET scripts with multiple scripts
    // Test: Click name selector
    // Test: Verify all script names visible in dropdown
    // Test: Verify no "Create new" option (select-only mode)
  })

  it.skip('should load script content when name is selected', () => {
    // Test: Select script name from dropdown
    // Test: Verify sourceCode loaded and displayed readonly
    // Test: Verify description loaded
    // Test: Verify version field populated
  })

  it.skip('should show versions for selected script', () => {
    // Test: Select script with multiple versions
    // Test: Click version selector
    // Test: Verify all versions shown in dropdown
  })

  it.skip('should load specific version when version changed', () => {
    // Test: Select script name
    // Test: Change version in dropdown
    // Test: Verify sourceCode updates to that version's content
    // Test: Verify description updates
  })

  it.skip('should disable form when node is not editable', () => {
    // Test: Mock usePolicyGuards to return isNodeEditable=false
    // Test: Verify all fields disabled
  })

  it.skip('should show guard alert when policy is published', () => {
    // Test: Mock usePolicyGuards to return guardAlert
    // Test: Verify alert message displayed
  })

  it.skip('should validate script is selected', () => {
    // Test: Leave name empty
    // Test: Try to submit
    // Test: Verify validation error "Please select a script"
  })

  it.skip('should validate version is selected', () => {
    // Test: Select name but clear version
    // Test: Try to submit
    // Test: Verify validation error "Please select a version"
  })

  it.skip('should call onFormSubmit with script data', () => {
    // Test: Select script and version
    // Test: Click submit (if submit button exists in ReactFlowSchemaForm)
    // Test: Verify onFormSubmit called with correct data
  })

  it.skip('should show readonly sourceCode with JavaScript editor', () => {
    // Test: Select script
    // Test: Verify Monaco editor shown with text/javascript widget
    // Test: Verify editor is readonly
  })

  it.skip('should show readonly description field', () => {
    // Test: Select script with description
    // Test: Verify description field populated
    // Test: Verify field is readonly
  })

  it.skip('should handle API errors gracefully', () => {
    // Test: Intercept GET scripts with 500 error
    // Test: Verify error message displayed
    // Test: Verify onFormError called
  })
})
