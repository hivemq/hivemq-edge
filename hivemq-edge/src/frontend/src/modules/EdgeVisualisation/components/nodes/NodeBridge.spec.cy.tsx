/// <reference types="cypress" />

import { MOCK_NODE_BRIDGE } from '@/__test-utils__/react-flow/nodes.ts'
import { mockReactFlow } from '@/__test-utils__/react-flow/providers.tsx'
import { MOCK_TOPIC_ACT1, MOCK_TOPIC_ALL } from '@/__test-utils__/react-flow/topics.ts'

import { mockProtocolAdapter } from '@/api/hooks/useProtocolAdapters/__handlers__'
import { formatTopicString } from '@/components/MQTT/topic-utils.ts'

import NodeBridge from './NodeBridge.tsx'

describe('NodeBridge', () => {
  beforeEach(() => {
    cy.viewport(400, 400)
    cy.intercept('/api/v1/management/protocol-adapters/types', { items: [mockProtocolAdapter] })
  })

  it('should render properly', () => {
    cy.mountWithProviders(mockReactFlow(<NodeBridge {...MOCK_NODE_BRIDGE} />))

    cy.getByTestId('bridge-node-name').should('contain', 'bridge-id-01')
    cy.getByTestId('connection-status').should('contain', 'Connected')
    // cy.getByTestId('topics-container')
    //   .should('be.visible')
    //   .should('contain.text', MOCK_TOPIC_REF1)
    //   .should('contain.text', MOCK_TOPIC_REF2)

    cy.getByTestId('topics-container').should('have.length', 2)
    cy.getByTestId('topics-container')
      .eq(0)
      .should('be.visible')
      .should('contain.text', formatTopicString(MOCK_TOPIC_ACT1))
    cy.getByTestId('topics-container')
      .eq(1)
      .should('be.visible')
      .should('contain.text', formatTopicString(MOCK_TOPIC_ALL))
  })

  it('should be accessible', () => {
    cy.injectAxe()
    cy.mountWithProviders(mockReactFlow(<NodeBridge {...MOCK_NODE_BRIDGE} />))
    cy.checkAccessibility()
    cy.percySnapshot('Component: NodeBridge')
  })
})
