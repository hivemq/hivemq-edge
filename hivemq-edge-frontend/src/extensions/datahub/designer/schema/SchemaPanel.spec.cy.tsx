/// <reference types="cypress" />

import { Button } from '@chakra-ui/react'

import { MockStoreWrapper } from '@datahub/__test-utils__/MockStoreWrapper.tsx'
import { DataHubNodeType, SchemaType } from '@datahub/types.ts'
import { getNodePayload } from '@datahub/utils/node.utils.ts'
import { SchemaPanel } from '@datahub/designer/schema/SchemaPanel.tsx'
import { mockSchemaTempHumidity } from '@datahub/api/hooks/DataHubSchemasService/__handlers__'

const wrapper: React.JSXElementConstructor<{ children: React.ReactNode }> = ({ children }) => (
  <MockStoreWrapper
    config={{
      initialState: {
        nodes: [
          {
            id: '3',
            type: DataHubNodeType.SCHEMA,
            position: { x: 0, y: 0 },
            data: getNodePayload(DataHubNodeType.SCHEMA),
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

describe('SchemaPanel', () => {
  beforeEach(() => {
    cy.viewport(800, 800)
    cy.intercept('/api/v1/data-hub/schemas', { items: [{ ...mockSchemaTempHumidity, type: SchemaType.PROTOBUF }] })
  })

  it('should render the fields for a Validator', () => {
    cy.mountWithProviders(<SchemaPanel selectedNode="3" />, { wrapper })

    cy.get('label#root_name-label').should('contain.text', 'Name')
    cy.get('label#root_name-label + div').should('contain.text', 'Select...')
    cy.get('label#root_name-label').should('have.attr', 'data-invalid')

    cy.get('label#root_type-label').should('contain.text', 'Schema')
    cy.get('label#root_type-label + div').should('contain.text', 'JSON')

    cy.get('label#root_version-label').should('contain.text', 'Version')
    cy.get('label#root_version-label + div').should('contain.text', '')

    cy.get('label#root_schemaSource-label').should('contain.text', 'schemaSource')
  })

  it('should control the editing flow', () => {
    cy.mountWithProviders(<SchemaPanel selectedNode="3" />, { wrapper })

    cy.get('#root_name-label + div').should('contain.text', 'Select...')
    cy.get('#root_type-label + div').should('contain.text', 'JSON')
    cy.get('#root_version-label + div').should('contain.text', '')

    // create a draft
    cy.get('#root_name-label + div').click()
    cy.get('#root_name-label + div').type('new-schema')
    cy.get('#root_name-label + div').find('[role="option"]').as('optionList')
    cy.get('@optionList').eq(0).click()

    cy.get('#root_name-label + div').should('contain.text', 'new-schema')
    cy.get('#root_type-label + div').should('contain.text', 'JSON')
    cy.get('#root_version-label + div').should('contain.text', 'DRAFT')
    cy.get('#root_schemaSource-label + div').should('contain.text', '"title":""')
    cy.get('#root_schemaSource-label + div').find('div.monaco-mouse-cursor-text').first().as('editor')
    cy.get('@editor').click()
    cy.get('@editor').type('{command}a rr', { delay: 50, waitForAnimations: true })
    cy.get('#root_schemaSource-label + div').should('contain.text', 'rr')

    // select an existing schema
    cy.get('#root_name-label + div').click()
    cy.get('#root_name-label + div').type('my-schema')
    cy.get('#root_name-label + div').find('[role="option"]').as('optionList')
    cy.get('@optionList').eq(0).click()

    cy.get('#root_name-label + div').should('contain.text', 'my-schema-id')
    cy.get('#root_type-label + div').should('contain.text', 'PROTOBUF')
    cy.get('#root_version-label + div').should('contain.text', '1')

    // modify the schema#
    // TODO[NVL] Triggering edit in Monaco not working
    // cy.get('@editor').type('this is fun')
    // cy.get('#root_name-label + div').should('contain.text', 'my-schema-id')
    // cy.get('#root_type-label + div').should('contain.text', 'PROTOBUF')
    // cy.get('#root_type-label + div input').should('be.disabled')
    // cy.get('#root_version-label + div').should('contain.text', 'MODIFIED')
    // cy.get('#root_version-label + div input').should('be.disabled')
  })

  // TODO[NVL] Weird import worker error
  it.skip('should be accessible', () => {
    cy.injectAxe()
    cy.mountWithProviders(<SchemaPanel selectedNode="3" />, { wrapper })

    cy.checkAccessibility(undefined, {
      rules: {
        // TODO[a11y] False positive with the react-select [?]
        'color-contrast': { enabled: false },
      },
    })
    cy.percySnapshot('Component: SchemaPanel')
  })
})
