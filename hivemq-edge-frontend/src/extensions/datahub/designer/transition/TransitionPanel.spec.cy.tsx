/// <reference types="cypress" />

import { Button } from '@chakra-ui/react'

import { MockStoreWrapper } from '@datahub/__test-utils__/MockStoreWrapper.tsx'
import { DataHubNodeType } from '@datahub/types.ts'
import { getNodePayload } from '@datahub/utils/node.utils.ts'

import { TransitionPanel } from './TransitionPanel.tsx'

const wrapper: React.JSXElementConstructor<{ children: React.ReactNode }> = ({ children }) => (
  <MockStoreWrapper
    config={{
      initialState: {
        nodes: [
          {
            id: '1',
            type: DataHubNodeType.BEHAVIOR_POLICY,
            position: { x: 0, y: 0 },
            data: { model: 'Publish.quota' },
          },
          {
            id: '3',
            type: DataHubNodeType.TRANSITION,
            position: { x: 0, y: 0 },
            data: getNodePayload(DataHubNodeType.TRANSITION),
          },
        ],
        edges: [{ id: '1-3', source: '1', target: '3' }],
      },
    }}
  >
    {children}
    <Button variant="primary" type="submit" form="datahub-node-form">
      SUBMIT{' '}
    </Button>
  </MockStoreWrapper>
)

describe('TransitionPanel', () => {
  beforeEach(() => {
    cy.viewport(800, 800)
  })

  it('should render the fields for a Validator', () => {
    cy.mountWithProviders(<TransitionPanel selectedNode="3" />, { wrapper })

    cy.get('label#root_model-label').should('contain.text', 'model')
    cy.get('label#root_model-label + input').should('have.value', 'Publish.quota')

    cy.get('label#root_event-label').should('contain.text', 'event')
    cy.get('label#root_event-label + div').should('contain.text', 'Select...')
    cy.get('label#root_event-label + div').click()

    cy.get('div#react-select-root_event-listbox').find('[role="option"]').as('optionList')
    cy.get('@optionList').should('have.length', 8)
    cy.get('@optionList').eq(0).should('contain.text', 'Mqtt.OnInboundConnect')
  })

  it('should be accessible', () => {
    cy.injectAxe()
    cy.mountWithProviders(<TransitionPanel selectedNode="3" />, { wrapper })

    cy.checkAccessibility()
    cy.percySnapshot('Component: TransitionPanel')
  })
})
