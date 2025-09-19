import { mockReactFlow } from '@/__test-utils__/react-flow/providers.tsx'
import { MOCK_NODE_ASSETS } from '@/__test-utils__/react-flow/nodes.ts'
import { MOCK_COMBINER_ASSET } from '@/api/hooks/useCombiners/__handlers__'
import { MOCK_CAPABILITIES } from '@/api/hooks/useFrontendServices/__handlers__'
import { MOCK_PULSE_ASSET_LIST } from '@/api/hooks/usePulse/__handlers__'

import NodeAssets from '@/modules/Workspace/components/nodes/NodeAssets.tsx'

describe('NodeAssets', () => {
  beforeEach(() => {
    cy.viewport(600, 400)
    cy.intercept('/api/v1/management/pulse/managed-assets', MOCK_PULSE_ASSET_LIST)
    cy.intercept('GET', '/api/v1/management/pulse/asset-mappers', { items: [MOCK_COMBINER_ASSET] })
    cy.intercept('/api/v1/frontend/capabilities', MOCK_CAPABILITIES)

    cy.intercept('/api/v1/management/protocol-adapters/types', { statusCode: 202, log: false })
    cy.intercept('/api/v1/management/combiners', { statusCode: 202, log: false })
  })

  it('should render properly', () => {
    cy.mountWithProviders(mockReactFlow(<NodeAssets {...MOCK_NODE_ASSETS} />))
    cy.getByTestId('assets-description').should('have.text', 'Asset Mapper')

    cy.getByTestId('topics-container').within(() => {
      cy.getByTestId('topic-wrapper').should('have.length', 1)
      cy.getByTestId('topic-wrapper').eq(0).should('have.text', 'test / topic / 2')
      cy.getByTestId('topic-wrapper').eq(0).find('svg').should('have.attr', 'aria-label', 'Topic')
    })
  })

  it('should be accessible', () => {
    cy.injectAxe()
    cy.mountWithProviders(mockReactFlow(<NodeAssets {...MOCK_NODE_ASSETS} />))
    cy.checkAccessibility()
  })
})
