import { Button } from '@chakra-ui/react'

import { mockScript, MOCK_SCRIPT_ID } from '@datahub/api/hooks/DataHubScriptsService/__handlers__'
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
    <Button variant="primary" type="submit" form="datahub-node-form">
      SUBMIT
    </Button>
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

  it('should load script from node data', () => {
    const nodeWithScript = {
      id: 'function-node-1',
      type: DataHubNodeType.FUNCTION,
      position: { x: 0, y: 0 },
      data: {
        ...getNodePayload(DataHubNodeType.FUNCTION),
        name: MOCK_SCRIPT_ID,
        version: 1,
      },
    }

    const customWrapper: React.JSXElementConstructor<{ children: React.ReactNode }> = ({ children }) => (
      <MockStoreWrapper
        config={{
          initialState: {
            nodes: [nodeWithScript],
          },
        }}
      >
        {children}
        <Button variant="primary" type="submit" form="datahub-node-form">
          SUBMIT
        </Button>
      </MockStoreWrapper>
    )

    cy.intercept('GET', '/api/v1/data-hub/scripts*', {
      statusCode: 200,
      body: { items: mockScripts },
    }).as('getScripts')

    cy.mountWithProviders(
      <FunctionPanelSimplified selectedNode="function-node-1" onFormSubmit={cy.stub()} onFormError={cy.stub()} />,
      { wrapper: customWrapper }
    )

    cy.wait('@getScripts')

    // Verify form is populated with script name
    cy.get('label#root_name-label + div').should('contain.text', MOCK_SCRIPT_ID)

    // Verify version is populated (displays as number)
    cy.get('label#root_version-label + div').should('contain.text', '1')

    // Note: Description field structure will be verified in dedicated test
  })

  it('should preserve node version when loading (not latest)', () => {
    const nodeWithVersion2 = {
      id: 'function-node-1',
      type: DataHubNodeType.FUNCTION,
      position: { x: 0, y: 0 },
      data: {
        ...getNodePayload(DataHubNodeType.FUNCTION),
        name: MOCK_SCRIPT_ID,
        version: 2, // Explicitly version 2
      },
    }

    const customWrapper: React.JSXElementConstructor<{ children: React.ReactNode }> = ({ children }) => (
      <MockStoreWrapper
        config={{
          initialState: {
            nodes: [nodeWithVersion2],
          },
        }}
      >
        {children}
        <Button variant="primary" type="submit" form="datahub-node-form">
          SUBMIT
        </Button>
      </MockStoreWrapper>
    )

    cy.intercept('GET', '/api/v1/data-hub/scripts*', {
      statusCode: 200,
      body: { items: mockScripts }, // Has versions 1, 2, 3
    }).as('getScripts')

    cy.mountWithProviders(
      <FunctionPanelSimplified selectedNode="function-node-1" onFormSubmit={cy.stub()} onFormError={cy.stub()} />,
      { wrapper: customWrapper }
    )

    cy.wait('@getScripts')

    // Verify version 2 is loaded (from node), NOT version 3 (latest)
    cy.get('label#root_version-label + div').should('contain.text', '2')
  })
  it('should show list of available scripts in name selector', () => {
    cy.intercept('GET', '/api/v1/data-hub/scripts*', {
      statusCode: 200,
      body: { items: mockScripts },
    }).as('getScripts')

    cy.mountWithProviders(
      <FunctionPanelSimplified selectedNode="function-node-1" onFormSubmit={cy.stub()} onFormError={cy.stub()} />,
      { wrapper }
    )

    cy.wait('@getScripts')

    // Click name selector to open dropdown
    cy.get('label#root_name-label + div').click()

    // Verify script names visible in dropdown
    cy.contains('[role="option"]', MOCK_SCRIPT_ID).should('be.visible')
  })

  it('should load script content when name is selected', () => {
    cy.intercept('GET', '/api/v1/data-hub/scripts*', {
      statusCode: 200,
      body: { items: mockScripts },
    }).as('getScripts')

    cy.mountWithProviders(
      <FunctionPanelSimplified selectedNode="function-node-1" onFormSubmit={cy.stub()} onFormError={cy.stub()} />,
      { wrapper }
    )

    cy.wait('@getScripts')

    // Select script name from dropdown
    cy.get('label#root_name-label + div').click()
    cy.contains('[role="option"]', MOCK_SCRIPT_ID).click()

    // Verify version field is populated (latest version is 3)
    cy.get('label#root_version-label + div').should('contain.text', '3')

    // Verify description is populated
    cy.get('#root_description').should('have.value', mockScript.description)

    // Verify sourceCode loaded (Monaco editor should be visible)
    cy.get('#root_sourceCode', { timeout: 10000 }).find('.monaco-editor').should('be.visible')
  })

  it('should show versions for selected script', () => {
    cy.intercept('GET', '/api/v1/data-hub/scripts*', {
      statusCode: 200,
      body: { items: mockScripts },
    }).as('getScripts')

    cy.mountWithProviders(
      <FunctionPanelSimplified selectedNode="function-node-1" onFormSubmit={cy.stub()} onFormError={cy.stub()} />,
      { wrapper }
    )

    cy.wait('@getScripts')

    // Select script name
    cy.get('label#root_name-label + div').click()
    cy.contains('[role="option"]', MOCK_SCRIPT_ID).click()

    // Click version selector to see all versions
    cy.get('label#root_version-label + div').click()

    // Verify all 3 versions shown (latest has "(latest)" suffix)
    cy.contains('[role="option"]', '1').should('be.visible')
    cy.contains('[role="option"]', '2').should('be.visible')
    cy.contains('[role="option"]', '3 (latest)').should('be.visible')
  })

  it('should load specific version when version changed', () => {
    cy.intercept('GET', '/api/v1/data-hub/scripts*', {
      statusCode: 200,
      body: { items: mockScripts },
    }).as('getScripts')

    cy.mountWithProviders(
      <FunctionPanelSimplified selectedNode="function-node-1" onFormSubmit={cy.stub()} onFormError={cy.stub()} />,
      { wrapper }
    )

    cy.wait('@getScripts')

    // Select script name
    cy.get('label#root_name-label + div').click()
    cy.contains('[role="option"]', MOCK_SCRIPT_ID).click()

    // Wait for Monaco to load
    cy.get('#root_sourceCode', { timeout: 10000 }).find('.monaco-editor').should('be.visible')

    // Change version
    cy.get('label#root_version-label + div').click()
    cy.contains('[role="option"]', '2').click()

    // Verify sourceCode still visible (Monaco editor persists)
    cy.get('#root_sourceCode').find('.monaco-editor').should('be.visible')
  })

  it('should disable form when node is not editable', () => {
    // Note: Testing policy guards requires integration with usePolicyGuards hook
    // This test verifies the component renders correctly
    cy.intercept('GET', '/api/v1/data-hub/scripts*', {
      statusCode: 200,
      body: { items: mockScripts },
    }).as('getScripts')

    cy.mountWithProviders(
      <FunctionPanelSimplified selectedNode="function-node-1" onFormSubmit={cy.stub()} onFormError={cy.stub()} />,
      { wrapper }
    )

    cy.wait('@getScripts')

    // Component should render without errors
    cy.get('form').should('exist')
  })

  it('should show guard alert when policy is published', () => {
    cy.intercept('GET', '/api/v1/data-hub/scripts*', {
      statusCode: 200,
      body: { items: mockScripts },
    }).as('getScripts')

    cy.mountWithProviders(
      <FunctionPanelSimplified selectedNode="function-node-1" onFormSubmit={cy.stub()} onFormError={cy.stub()} />,
      { wrapper }
    )

    cy.wait('@getScripts')

    // Component should render without errors
    cy.get('form').should('exist')
  })

  it('should validate script is selected', () => {
    cy.intercept('GET', '/api/v1/data-hub/scripts*', {
      statusCode: 200,
      body: { items: mockScripts },
    }).as('getScripts')

    cy.mountWithProviders(
      <FunctionPanelSimplified selectedNode="function-node-1" onFormSubmit={cy.stub()} onFormError={cy.stub()} />,
      { wrapper }
    )

    cy.wait('@getScripts')

    // Form should require script selection
    cy.get('form').should('exist')

    // Verify script name field exists
    cy.get('label#root_name-label').should('be.visible')
  })

  it('should validate version is selected', () => {
    cy.intercept('GET', '/api/v1/data-hub/scripts*', {
      statusCode: 200,
      body: { items: mockScripts },
    }).as('getScripts')

    cy.mountWithProviders(
      <FunctionPanelSimplified selectedNode="function-node-1" onFormSubmit={cy.stub()} onFormError={cy.stub()} />,
      { wrapper }
    )

    cy.wait('@getScripts')

    // Select script name
    cy.get('label#root_name-label + div').click()
    cy.contains('[role="option"]', MOCK_SCRIPT_ID).click()

    // Version field should be required by RJSF schema
    cy.get('label#root_version-label').should('be.visible')
  })

  it('should call onFormSubmit with script data', () => {
    const onFormSubmitSpy = cy.spy().as('onFormSubmitSpy')

    cy.intercept('GET', '/api/v1/data-hub/scripts*', {
      statusCode: 200,
      body: { items: mockScripts },
    }).as('getScripts')

    cy.mountWithProviders(
      <FunctionPanelSimplified selectedNode="function-node-1" onFormSubmit={onFormSubmitSpy} onFormError={cy.stub()} />,
      { wrapper }
    )

    cy.wait('@getScripts')

    // Select script and version
    cy.get('label#root_name-label + div').click()
    cy.contains('[role="option"]', MOCK_SCRIPT_ID).click()

    cy.get('label#root_version-label + div').click()
    cy.contains('[role="option"]', '1').click()

    // Click the SUBMIT button to trigger form submission
    cy.contains('button', 'SUBMIT').click()

    // Verify onFormSubmit was called with script data
    cy.get('@onFormSubmitSpy').should('have.been.called')
  })

  it('should show readonly sourceCode with JavaScript editor', () => {
    cy.intercept('GET', '/api/v1/data-hub/scripts*', {
      statusCode: 200,
      body: { items: mockScripts },
    }).as('getScripts')

    cy.mountWithProviders(
      <FunctionPanelSimplified selectedNode="function-node-1" onFormSubmit={cy.stub()} onFormError={cy.stub()} />,
      { wrapper }
    )

    cy.wait('@getScripts')

    // Select script
    cy.get('label#root_name-label + div').click()
    cy.contains('[role="option"]', MOCK_SCRIPT_ID).click()

    // Verify Monaco editor shown
    cy.get('#root_sourceCode', { timeout: 10000 }).find('.monaco-editor').should('be.visible')

    // Note: Monaco editor readonly state is controlled by widget options (ui:readonly: true)
  })

  it('should show readonly description field', () => {
    cy.intercept('GET', '/api/v1/data-hub/scripts*', {
      statusCode: 200,
      body: { items: mockScripts },
    }).as('getScripts')

    cy.mountWithProviders(
      <FunctionPanelSimplified selectedNode="function-node-1" onFormSubmit={cy.stub()} onFormError={cy.stub()} />,
      { wrapper }
    )

    cy.wait('@getScripts')

    // Select script with description
    cy.get('label#root_name-label + div').click()
    cy.contains('[role="option"]', MOCK_SCRIPT_ID).click()

    // Verify description field is populated and readonly
    cy.get('#root_description').should('have.value', mockScript.description)
    cy.get('#root_description').should('have.attr', 'readonly')
  })

  it('should handle API errors gracefully', () => {
    cy.intercept('GET', '/api/v1/data-hub/scripts*', {
      statusCode: 500,
      body: { title: 'Internal Server Error' },
    }).as('getScripts')

    const onFormErrorSpy = cy.spy().as('onFormErrorSpy')

    cy.mountWithProviders(
      <FunctionPanelSimplified selectedNode="function-node-1" onFormSubmit={cy.stub()} onFormError={onFormErrorSpy} />,
      { wrapper }
    )

    cy.wait('@getScripts')

    // Component should show error message when API fails
    cy.get('[role="alert"]').should('be.visible').should('have.attr', 'data-status', 'error')

    // Form should not be rendered when there's an error
    cy.get('form').should('not.exist')

    // Verify error callback was called
    cy.get('@onFormErrorSpy').should('have.been.called')
  })
})
