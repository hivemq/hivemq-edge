/// <reference types="cypress" />

import NodePropertyDrawer from '@/modules/Workspace/components/drawers/NodePropertyDrawer.tsx'
import { MOCK_NODE_ADAPTER } from '@/__test-utils__/react-flow/nodes.ts'
import { Node } from 'reactflow'
import { Adapter, Bridge } from '@/api/__generated__'
import { NodeTypes } from '@/modules/Workspace/types.ts'

const mockNode: Node<Bridge | Adapter> = {
  position: { x: 0, y: 0 },
  id: 'adapter@fgffgf',
  type: NodeTypes.ADAPTER_NODE,
  data: MOCK_NODE_ADAPTER,
}

describe('NodePropertyDrawer', () => {
  beforeEach(() => {
    cy.viewport(800, 800)
    cy.intercept('/api/v1/metrics', []).as('getMetrics')
    cy.intercept('/api/v1/metrics/**', []).as('getMetricForX')
    cy.intercept('/api/v1/management/events?*', []).as('getEvents')
  })

  it('should render properly', () => {
    const onClose = cy.stub().as('onClose')
    const onEditEntity = cy.stub().as('onEditEntity')
    cy.mountWithProviders(
      <NodePropertyDrawer
        nodeId="adapter@fgffgf"
        selectedNode={mockNode}
        isOpen={true}
        onClose={onClose}
        onEditEntity={onEditEntity}
      />
    )

    // check the panel control
    cy.getByAriaLabel('Close').click()
    cy.get('@onClose').should('have.been.calledOnce')

    // check that the metrics is there
    cy.getByTestId('metrics-toggle').should('be.visible')

    // check that the event log is there
    cy.get('p').should('contain.text', 'The 5 most recent events for adapter idAdapter')
    cy.getByTestId('navigate-eventLog-filtered')
      .should('contain.text', 'Show more')
      .should('have.attr', 'href', '/event-logs?source=idAdapter')
    cy.get('tbody').find('tr').should('have.length', 5)

    // check that the controller is there
    cy.getByTestId('protocol-create-adapter').should('contain.text', 'Modify the adapter').click()
    cy.get('@onEditEntity').should('have.been.calledOnce')
    cy.getByTestId('device-action-start').should('exist')
  })

  it('should be accessible', () => {
    cy.injectAxe()
    cy.mountWithProviders(
      <NodePropertyDrawer
        nodeId={'adapter@fgffgf'}
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
    cy.percySnapshot('Component: NodePropertyDrawer')
  })
})
