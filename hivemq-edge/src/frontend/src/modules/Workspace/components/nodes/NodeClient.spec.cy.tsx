import { MOCK_NODE_CLIENT } from '@/__test-utils__/react-flow/nodes.ts'
import { mockReactFlow } from '@/__test-utils__/react-flow/providers.tsx'

import { formatTopicString } from '@/components/MQTT/topic-utils.ts'

import { NodeClient } from '@/modules/Workspace/components/nodes/index.ts'

describe('NodeClient', () => {
  beforeEach(() => {
    cy.viewport(400, 400)
  })

  it('should render properly', () => {
    cy.mountWithProviders(mockReactFlow(<NodeClient {...MOCK_NODE_CLIENT} />))

    cy.getByTestId('client-node-name').should('contain', 'my-first-client')

    cy.getByTestId('topics-container').should('have.length', 1)
    cy.getByTestId('topics-container')
      .eq(0)
      .should('be.visible')
      .should('contain.text', formatTopicString('test/topic/1'))

    cy.get('div[data-handlepos]').should('be.visible').should('have.attr', 'data-handlepos', 'top')
  })

  it('should be accessible', () => {
    cy.injectAxe()
    cy.mountWithProviders(mockReactFlow(<NodeClient {...MOCK_NODE_CLIENT} />))
    cy.checkAccessibility()
    cy.percySnapshot('Component: NodeBridge')
  })
})
