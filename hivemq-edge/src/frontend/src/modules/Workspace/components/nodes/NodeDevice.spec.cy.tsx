import { MOCK_NODE_DEVICE } from '@/__test-utils__/react-flow/nodes.ts'
import { mockReactFlow } from '@/__test-utils__/react-flow/providers.tsx'

import { NodeDevice } from '@/modules/Workspace/components/nodes/index.ts'

describe('NodeDevice', () => {
  beforeEach(() => {
    cy.viewport(400, 400)
  })

  it.only('should render properly', () => {
    cy.mountWithProviders(mockReactFlow(<NodeDevice {...MOCK_NODE_DEVICE} />))

    cy.getByTestId('device-description')
      .should('have.text', 'Simulation')
      .find('svg')
      .should('have.attr', 'data-type', 'INDUSTRIAL')

    cy.getByTestId('device-capabilities').find('svg').as('capabilities').should('have.length', 2)
    cy.get('@capabilities').eq(0).should('have.attr', 'data-type', 'READ')
    cy.get('@capabilities').eq(1).should('have.attr', 'data-type', 'DISCOVER')
  })

  it('should be accessible', () => {
    cy.injectAxe()
    cy.mountWithProviders(mockReactFlow(<NodeDevice {...MOCK_NODE_DEVICE} />))
    cy.checkAccessibility()
    cy.percySnapshot('Component: NodeDevice')
  })
})
