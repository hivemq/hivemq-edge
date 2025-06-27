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
    cy.percySnapshot('Component: SchemaPanel')
  })
})
