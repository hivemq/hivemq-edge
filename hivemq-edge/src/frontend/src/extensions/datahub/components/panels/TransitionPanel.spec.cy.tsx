/// <reference types="cypress" />

import { MockStoreWrapper } from '../../__test-utils__/MockStoreWrapper.tsx'
import { DataHubNodeType } from '../../types.ts'
import { getNodePayload } from '../../utils/node.utils.ts'
import { TransitionPanel } from '../panels/TransitionPanel.tsx'
import { Button } from '@chakra-ui/react'

const wrapper: React.JSXElementConstructor<{ children: React.ReactNode }> = ({ children }) => (
  <MockStoreWrapper
    config={{
      initialState: {
        nodes: [
          {
            id: '3',
            type: DataHubNodeType.TRANSITION,
            position: { x: 0, y: 0 },
            data: getNodePayload(DataHubNodeType.TRANSITION),
          },
        ],
      },
    }}
  >
    {children}
    <Button variant={'primary'} type="submit" form="datahub-node-form">
      SUBMIT{' '}
    </Button>
  </MockStoreWrapper>
)

describe('TransitionPanel', () => {
  beforeEach(() => {
    cy.viewport(800, 800)
  })

  it('should render the fields for a Validator', () => {
    cy.mountWithProviders(<TransitionPanel selectedNode={'3'} />, { wrapper })

    // first select
    cy.get('label#root_type-label').should('contain.text', 'Transition')
    cy.get('label#root_type-label + div').should('contain.text', 'Event.OnAny')
    cy.get('label#root_type-label + div').click()
    cy.get('label#root_type-label + div')
      .find("[role='listbox']")
      .find("[role='option']")
      .eq(0)
      .should('contain.text', 'Event.OnAny')
    cy.get('label#root_type-label + div').find("[role='listbox']").find("[role='option']").should('have.length', 6)
    cy.get('label#root_type-label + div').click()

    // first select
    cy.get('label#root_from-label').should('contain.text', 'From State')
    cy.get('label#root_from-label + div').should('contain.text', 'Any.*')
    cy.get('label#root_from-label + div').click()
    cy.get('label#root_from-label + div')
      .find("[role='listbox']")
      .find("[role='option']")
      .eq(0)
      .should('contain.text', 'Any.*')
    cy.get('label#root_from-label + div').find("[role='listbox']").find("[role='option']").should('have.length', 8)
    cy.get('label#root_from-label + div').click()

    // first select
    cy.get('label#root_to-label').should('contain.text', 'To State')
    cy.get('label#root_to-label + div').should('contain.text', 'Any.*')
    cy.get('label#root_to-label + div').click()
    cy.get('label#root_to-label + div')
      .find("[role='listbox']")
      .find("[role='option']")
      .eq(0)
      .should('contain.text', 'Any.*')
    cy.get('label#root_to-label + div').find("[role='listbox']").find("[role='option']").should('have.length', 8)
    cy.get('label#root_to-label + div').click()
  })

  it('should be accessible', () => {
    cy.injectAxe()
    cy.mountWithProviders(<TransitionPanel selectedNode={'3'} />, { wrapper })

    cy.checkAccessibility()
    cy.percySnapshot('Component: TransitionPanel')
  })
})
