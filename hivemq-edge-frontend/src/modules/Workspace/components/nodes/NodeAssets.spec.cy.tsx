import { mockReactFlow } from '@/__test-utils__/react-flow/providers.tsx'
import { MOCK_NODE_ASSETS } from '@/__test-utils__/react-flow/nodes.ts'

import NodeAssets from '@/modules/Workspace/components/nodes/NodeAssets.tsx'

describe('NodeAssets', () => {
  beforeEach(() => {
    cy.viewport(600, 400)
  })

  it('should render properly', () => {
    cy.mountWithProviders(mockReactFlow(<NodeAssets {...MOCK_NODE_ASSETS} />))
    cy.getByTestId('assets-description').should('have.text', 'Asset Mapper')
  })

  it('should be accessible', () => {
    cy.injectAxe()
    cy.mountWithProviders(mockReactFlow(<NodeAssets {...MOCK_NODE_ASSETS} />))
    cy.checkAccessibility()
  })
})
