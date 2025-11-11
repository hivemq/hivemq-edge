/// <reference types="cypress" />

import { Button } from '@chakra-ui/react'

import { MockStoreWrapper } from '@datahub/__test-utils__/MockStoreWrapper.tsx'
import { DataHubNodeType, SchemaType } from '@datahub/types.ts'
import { getNodePayload } from '@datahub/utils/node.utils.ts'
import { mockScript } from '@datahub/api/hooks/DataHubScriptsService/__handlers__'
import { FunctionPanel } from '@datahub/designer/script/FunctionPanel.tsx'

const wrapper: React.JSXElementConstructor<{ children: React.ReactNode }> = ({ children }) => (
  <MockStoreWrapper
    config={{
      initialState: {
        nodes: [
          {
            id: '3',
            type: DataHubNodeType.FUNCTION,
            position: { x: 0, y: 0 },
            data: getNodePayload(DataHubNodeType.FUNCTION),
          },
        ],
      },
    }}
  >
    {children}
    <Button variant="primary" type="submit" form="datahub-node-form" mt={4}>
      SUBMIT
    </Button>
  </MockStoreWrapper>
)

describe('FunctionPanel', () => {
  beforeEach(() => {
    cy.viewport(800, 800)
    cy.intercept('/api/v1/data-hub/scripts', { items: [{ ...mockScript, type: SchemaType.PROTOBUF }] })

    // Ignore Monaco worker loading errors
    cy.on('uncaught:exception', (err) => {
      return !(err.message.includes('importScripts') || err.message.includes('worker'))
    })
  })

  it('should render loading and error states', () => {
    const onFormError = cy.stub().as('onFormError')
    cy.intercept('/api/v1/data-hub/scripts', { statusCode: 404 }).as('getScripts')
    cy.mountWithProviders(<FunctionPanel selectedNode="3" onFormError={onFormError} />, { wrapper })
    cy.getByTestId('loading-spinner').should('be.visible')

    cy.wait('@getScripts')
    cy.get('[role="alert"]')
      .should('be.visible')
      .should('have.attr', 'data-status', 'error')
      .should('have.text', 'Not Found')

    cy.get('@onFormError').should('have.been.calledWithErrorMessage', 'Not Found')
  })

  it('should render the fields for a Function node', () => {
    cy.mountWithProviders(<FunctionPanel selectedNode="3" />, { wrapper })

    cy.get('label#root_name-label').should('contain.text', 'Name')
    cy.get('label#root_name-label + div').should('contain.text', 'Select...')
    cy.get('label#root_name-label').should('have.attr', 'data-invalid')

    cy.get('label#root_version-label').should('contain.text', 'Version')
    cy.get('label#root_version-label + div').should('contain.text', '')

    cy.get('label#root_description-label').should('contain.text', 'Description')
    cy.get('input#root_description').should('contain.text', '')
    cy.get('input#root_description').should('have.attr', 'placeholder', 'A short description for this version')

    cy.get('label#root_sourceCode-label').should('contain.text', 'Source')
    cy.get('div#root_sourceCode').should('contain.html', '&nbsp;*&nbsp;@param&nbsp;{Object}&nbsp;publish')
  })

  it('should control the editing flow', () => {
    cy.mountWithProviders(<FunctionPanel selectedNode="3" />, { wrapper })

    cy.get('#root_name-label + div').should('contain.text', 'Select...')
    cy.get('#root_version-label + div').should('contain.text', '')

    // create a draft
    cy.get('#root_name-label + div').click()
    cy.get('#root_name-label + div').type('new-schema')
    cy.get('#root_name-label + div').find('[role="option"]').as('optionList')
    cy.get('@optionList').eq(0).click()

    cy.get('#root_name-label + div').should('contain.text', 'new-schema')
    cy.get('#root_version-label + div').should('contain.text', 'DRAFT')
    cy.get('div#root_sourceCode').should('contain.html', '&nbsp;*&nbsp;@param&nbsp;{Object}&nbsp;publish')
    cy.get('div#root_sourceCode').find('div.monaco-mouse-cursor-text')
    // TODO[NVL] this doesn't work
    // cy.get('@editor').click()
    // cy.get('@editor').type('{command}a rr', { delay: 50, waitForAnimations: true })
    // cy.get('#root_schemaSource-label + div').should('contain.text', 'rr')

    // select an existing schema
    cy.get('#root_name-label + div').click()
    cy.get('#root_name-label + div').type('my-script')
    cy.get('#root_name-label + div').find('[role="option"]').as('optionList')
    cy.get('@optionList').eq(0).click()

    cy.get('#root_name-label + div').should('contain.text', 'my-script-id')
    cy.get('#root_version-label + div').should('contain.text', '1')
  })

  it('should be accessible', () => {
    cy.injectAxe()
    cy.mountWithProviders(<FunctionPanel selectedNode="3" />, { wrapper })

    cy.checkAccessibility()
  })
})

describe('FunctionPanel - Form field cascading updates', () => {
  beforeEach(() => {
    cy.viewport(800, 800)
  })

  it('should update version, description, and sourceCode when selecting an existing script by name', () => {
    const scriptWithMultipleVersions = {
      ...mockScript,
      version: 2,
      description: 'Version 2 description',
    }

    cy.intercept('/api/v1/data-hub/scripts', {
      items: [mockScript, scriptWithMultipleVersions],
    })

    cy.mountWithProviders(<FunctionPanel selectedNode="3" />, { wrapper })

    // Initial state
    cy.get('#root_name-label + div').should('contain.text', 'Select...')
    cy.get('#root_version-label + div').should('contain.text', '')

    // Select an existing script
    cy.get('#root_name-label + div').click()
    cy.get('#root_name-label + div').type('my-script')
    cy.get('#root_name-label + div').find('[role="option"]').eq(0).click()

    // Verify all fields are populated from the latest version
    cy.get('#root_name-label + div').should('contain.text', 'my-script-id')
    cy.get('#root_version-label + div').should('contain.text', '2')
    cy.get('input#root_description').should('have.value', 'Version 2 description')
    cy.get('div#root_sourceCode').should('exist')
  })

  it('should set version to DRAFT and load template when creating a new script', () => {
    cy.intercept('/api/v1/data-hub/scripts', { items: [mockScript] })

    cy.mountWithProviders(<FunctionPanel selectedNode="3" />, { wrapper })

    // Create a new script
    cy.get('#root_name-label + div').click()
    cy.get('#root_name-label + div').type('brand-new-script')
    cy.get('#root_name-label + div').find('[role="option"]').eq(0).click()

    // Verify DRAFT state
    cy.get('#root_name-label + div').should('contain.text', 'brand-new-script')
    cy.get('#root_version-label + div').should('contain.text', 'DRAFT')
    cy.get('div#root_sourceCode').should('contain.html', '&nbsp;*&nbsp;@param&nbsp;{Object}&nbsp;publish')
  })

  it('should update description and sourceCode when changing version without triggering cascade', () => {
    const version1 = {
      ...mockScript,
      version: 1,
      description: 'Version 1 description',
      source: btoa('function v1() { return 1; }'),
    }
    const version2 = {
      ...mockScript,
      version: 2,
      description: 'Version 2 description',
      source: btoa('function v2() { return 2; }'),
    }

    cy.intercept('/api/v1/data-hub/scripts', {
      items: [version1, version2],
    })

    cy.mountWithProviders(<FunctionPanel selectedNode="3" />, { wrapper })

    // Select the script (should load latest version 2)
    cy.get('#root_name-label + div').click()
    cy.get('#root_name-label + div').type('my-script')
    cy.get('#root_name-label + div').find('[role="option"]').eq(0).click()

    cy.get('#root_version-label + div').should('contain.text', '2')
    cy.get('input#root_description').should('have.value', 'Version 2 description')

    // Change to version 1
    cy.get('#root_version-label + div').click()
    cy.get('[role="option"]').contains('1').click()

    // Verify version change updates description and sourceCode without cascade
    cy.get('#root_version-label + div').should('contain.text', '1')
    cy.get('input#root_description').should('have.value', 'Version 1 description')
    cy.get('div#root_sourceCode').invoke('text').should('include', 'v1')
    cy.get('div#root_sourceCode').invoke('text').should('include', 'return')

    // Change back to version 2
    cy.get('#root_version-label + div').click()
    cy.get('[role="option"]').contains('2').click()

    cy.get('#root_version-label + div').should('contain.text', '2')
    cy.get('input#root_description').should('have.value', 'Version 2 description')
    cy.get('div#root_sourceCode').invoke('text').should('include', 'v2')
    cy.get('div#root_sourceCode').invoke('text').should('include', 'return')
  })

  it('should mark version as MODIFIED when editing description of a loaded script', () => {
    cy.intercept('/api/v1/data-hub/scripts', { items: [mockScript] })

    cy.mountWithProviders(<FunctionPanel selectedNode="3" />, { wrapper })

    // Load existing script
    cy.get('#root_name-label + div').click()
    cy.get('#root_name-label + div').type('my-script')
    cy.get('#root_name-label + div').find('[role="option"]').eq(0).click()

    cy.get('#root_version-label + div').should('contain.text', '1')

    // Edit description
    cy.get('input#root_description').clear()
    cy.get('input#root_description').type('Modified description')

    // Verify version changes to MODIFIED without cascade
    cy.get('#root_version-label + div').should('contain.text', 'MODIFIED')
    cy.get('input#root_description').should('have.value', 'Modified description')
  })

  it('should not trigger cascade when switching between scripts', () => {
    const script1 = {
      id: 'script-1',
      version: 1,
      description: 'Script 1 description',
      source: btoa('function script1() {}'),
      createdAt: mockScript.createdAt,
      functionType: mockScript.functionType,
    }
    const script2 = {
      id: 'script-2',
      version: 1,
      description: 'Script 2 description',
      source: btoa('function script2() {}'),
      createdAt: mockScript.createdAt,
      functionType: mockScript.functionType,
    }

    cy.intercept('/api/v1/data-hub/scripts', {
      items: [script1, script2],
    })

    cy.mountWithProviders(<FunctionPanel selectedNode="3" />, { wrapper })

    // Select first script
    cy.get('#root_name-label + div').click()
    cy.get('#root_name-label + div').type('script-1')
    cy.get('#root_name-label + div').find('[role="option"]').eq(0).click()

    cy.get('#root_name-label + div').should('contain.text', 'script-1')
    cy.get('#root_version-label + div').should('contain.text', '1')
    cy.get('input#root_description').should('have.value', 'Script 1 description')

    // Switch to second script - use the clear button in the select
    cy.get('#root_name-label + div').click()
    cy.get('#root_name-label + div').type('{selectall}script-2')
    cy.get('#root_name-label + div').find('[role="option"]').eq(0).click()

    // Verify clean switch without cascade
    cy.get('#root_name-label + div').should('contain.text', 'script-2')
    cy.get('#root_version-label + div').should('contain.text', '1')
    cy.get('input#root_description').should('have.value', 'Script 2 description')
    cy.get('div#root_sourceCode').invoke('text').should('include', 'script2')
  })

  it('should handle rapid field changes without cascade loops', () => {
    const multiVersionScript = {
      ...mockScript,
      version: 3,
      description: 'Version 3',
    }

    cy.intercept('/api/v1/data-hub/scripts', {
      items: [mockScript, { ...mockScript, version: 2, description: 'Version 2' }, multiVersionScript],
    })

    cy.mountWithProviders(<FunctionPanel selectedNode="3" />, { wrapper })

    // Load script
    cy.get('#root_name-label + div').click()
    cy.get('#root_name-label + div').type('my-script')
    cy.get('#root_name-label + div').find('[role="option"]').eq(0).click()

    // Rapid version changes
    cy.get('#root_version-label + div').click()
    cy.get('[role="option"]').contains('2').click()
    cy.get('#root_version-label + div').should('contain.text', '2')

    cy.get('#root_version-label + div').click()
    cy.get('[role="option"]').contains('1').click()
    cy.get('#root_version-label + div').should('contain.text', '1')

    cy.get('#root_version-label + div').click()
    cy.get('[role="option"]').contains('3').click()
    cy.get('#root_version-label + div').should('contain.text', '3')

    // Verify final state is stable
    cy.get('input#root_description').should('have.value', 'Version 3')
  })

  it('should maintain MODIFIED state when editing after version change', () => {
    const version1 = { ...mockScript, version: 1, description: 'V1' }
    const version2 = { ...mockScript, version: 2, description: 'V2' }

    cy.intercept('/api/v1/data-hub/scripts', { items: [version1, version2] })

    cy.mountWithProviders(<FunctionPanel selectedNode="3" />, { wrapper })

    // Load script
    cy.get('#root_name-label + div').click()
    cy.get('#root_name-label + div').type('my-script')
    cy.get('#root_name-label + div').find('[role="option"]').eq(0).click()

    // Change version
    cy.get('#root_version-label + div').click()
    cy.get('[role="option"]').contains('1').click()
    cy.get('#root_version-label + div').should('contain.text', '1')

    // Edit description
    cy.get('input#root_description').clear()
    cy.get('input#root_description').type('Custom modification')

    // Verify MODIFIED without reverting to original version
    cy.get('#root_version-label + div').should('contain.text', 'MODIFIED')
    cy.get('input#root_description').should('have.value', 'Custom modification')
  })
})
