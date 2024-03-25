/// <reference types="cypress" />

import { Button } from '@chakra-ui/react'

import { MockStoreWrapper } from '@datahub/__test-utils__/MockStoreWrapper.tsx'
import { DataHubNodeType } from '@datahub/types.ts'
import { getNodePayload } from '@datahub/utils/node.utils.ts'
import { SchemaPanel } from '@datahub/designer/schema/SchemaPanel.tsx'

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
      SUBMIT{' '}
    </Button>
  </MockStoreWrapper>
)

describe('SchemaPanel', () => {
  beforeEach(() => {
    cy.viewport(800, 800)
    cy.intercept('/api/v1/data-hub/schemas', { items: [] }).as('getSchemas')
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
  })

  it.skip('should control the editing flow', () => {
    cy.mountWithProviders(<SchemaPanel selectedNode="3" />, { wrapper })

    // TODO[NVL] Need to be done
    // cy.get('label#root_type-label + div').click()
    // cy.get('label#root_type-label + div')
    //   .find("[role='listbox']")
    //   .find("[role='option']")
    //   .eq(0)
    //   .should('contain.text', 'JSON')
    // cy.get('label#root_type-label + div')
    //   .find("[role='listbox']")
    //   .find("[role='option']")
    //   .eq(1)
    //   .should('contain.text', 'PROTOBUF')
    // cy.get('label#root_type-label + div').find("[role='listbox']").find("[role='option']").should('have.length', 2)
    // cy.get('label#root_type-label + div').click()
    //
    // cy.get('label#root_version-label').should('contain.text', 'version')
    // cy.get('label#root_version-label + div').should('contain.text', 'DRAFT')
    //
    // cy.get('section div').should('have.attr', 'data-mode-id', 'json')
  })

  it('should be accessible', () => {
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
