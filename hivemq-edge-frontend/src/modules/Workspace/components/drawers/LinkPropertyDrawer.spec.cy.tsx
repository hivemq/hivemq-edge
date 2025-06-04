import type { Node } from '@xyflow/react'

import { MOCK_NODE_ADAPTER } from '@/__test-utils__/react-flow/nodes.ts'
import type { Adapter, Bridge } from '@/api/__generated__'
import { NodeTypes } from '@/modules/Workspace/types.ts'

import LinkPropertyDrawer from './LinkPropertyDrawer'

const mockNode: Node<Bridge | Adapter> = {
  position: { x: 0, y: 0 },
  id: 'adapter@fgffgf',
  type: NodeTypes.ADAPTER_NODE,
  data: MOCK_NODE_ADAPTER,
}

describe('NodePropertyDrawer', () => {
  beforeEach(() => {
    cy.viewport(800, 800)
    cy.intercept('/api/v1/management/protocol-adapters/types', { statusCode: 404 })
    cy.intercept('/api/v1/metrics', [])
    cy.intercept('/api/v1/metrics/**', [])
    cy.intercept('/api/v1/management/events?*', [])
  })

  it('should render properly', () => {
    const onClose = cy.stub().as('onClose')
    const onEditEntity = cy.stub().as('onEditEntity')
    cy.mountWithProviders(
      <LinkPropertyDrawer
        nodeId="adapter@fgffgf"
        selectedNode={mockNode}
        isOpen={true}
        onClose={onClose}
        onEditEntity={onEditEntity}
      />
    )

    // check the panel control
    cy.get('@onClose').should('not.have.been.called')
    cy.getByAriaLabel('Close').click()
    cy.get('@onClose').should('have.been.calledOnce')

    // check that the metrics is there
    cy.getByTestId('metrics-toggle').should('be.visible')
  })

  it('should be accessible', () => {
    cy.injectAxe()
    cy.mountWithProviders(
      <LinkPropertyDrawer
        nodeId="adapter@fgffgf"
        selectedNode={mockNode}
        isOpen={true}
        onClose={cy.stub()}
        onEditEntity={cy.stub()}
      />
    )

    cy.checkAccessibility(undefined, {
      rules: {
        // TODO[#17700] Color-contrast totally wrong; fix and test the component
        'color-contrast': { enabled: false },
      },
    })
    cy.percySnapshot('Component: LinkPropertyDrawer')
  })
})
