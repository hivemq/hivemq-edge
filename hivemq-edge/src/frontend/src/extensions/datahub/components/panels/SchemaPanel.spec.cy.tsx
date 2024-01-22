/// <reference types="cypress" />

import { MockStoreWrapper } from '../../__test-utils__/react-flow.mocks.tsx'
import { DataHubNodeType } from '../../types.ts'
import { getNodePayload } from '../../utils/node.utils.ts'
import { SchemaPanel } from '../panels/SchemaPanel.tsx'

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
  </MockStoreWrapper>
)

describe('SchemaPanel', () => {
  beforeEach(() => {
    cy.viewport(800, 800)
  })

  it('should render the fields for a Validator', () => {
    cy.mountWithProviders(<SchemaPanel selectedNode={'3'} />, { wrapper })

    // first select
    cy.get('label#root_type-label').should('contain.text', 'Schema')
    cy.get('label#root_type-label + div').should('contain.text', 'JSON')
    cy.get('label#root_type-label + div').click()
    cy.get('label#root_type-label + div')
      .find("[role='listbox']")
      .find("[role='option']")
      .eq(0)
      .should('contain.text', 'JSON')
    cy.get('label#root_type-label + div')
      .find("[role='listbox']")
      .find("[role='option']")
      .eq(1)
      .should('contain.text', 'PROTOBUF')
    cy.get('label#root_type-label + div').find("[role='listbox']").find("[role='option']").should('have.length', 2)
    cy.get('label#root_type-label + div').click()

    cy.get('label#root_version-label').should('contain.text', 'version')
    cy.get('label#root_version-label + input').should('have.value', '1')

    cy.get('section div').should('have.attr', 'data-mode-id', 'json')
  })

  it('should be accessible', () => {
    cy.injectAxe()
    cy.mountWithProviders(<SchemaPanel selectedNode={'3'} />, { wrapper })

    cy.checkAccessibility()
    cy.percySnapshot('Component: SchemaPanel')
  })
})
