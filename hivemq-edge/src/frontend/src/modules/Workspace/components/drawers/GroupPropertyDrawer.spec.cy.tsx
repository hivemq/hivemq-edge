/// <reference types="cypress" />

import { Node } from 'reactflow'
import { Group, NodeTypes } from '../../types.ts'
import GroupPropertyDrawer from './GroupPropertyDrawer.tsx'
import { MOCK_METRICS } from '@/api/hooks/useGetMetrics/__handlers__'
import { MetricList } from '@/api/__generated__'
import { MOCK_NODE_ADAPTER, MOCK_NODE_BRIDGE } from '@/__test-utils__/react-flow/nodes.ts'

const mockNode: Node<Group> = {
  position: { x: 0, y: 0 },
  id: 'adapter@fgffgf',
  type: NodeTypes.CLUSTER_NODE,
  data: { childrenNodeIds: ['bridge-id-01', 'my-adapter'], title: 'dfdf', isOpen: true },
}

const mockNodes: Node<NodeTypes.BRIDGE_NODE | NodeTypes.ADAPTER_NODE>[] = [
  { ...MOCK_NODE_BRIDGE, id: 'bridge-id-01', position: { x: 0, y: 0 } },
  { ...MOCK_NODE_ADAPTER, id: 'my-adapter', position: { x: 0, y: 0 } },
]

describe('GroupPropertyDrawer', () => {
  beforeEach(() => {
    cy.viewport(800, 800)
    cy.intercept('/api/v1/metrics', [{ items: MOCK_METRICS } as MetricList]).as('getMetrics')
    cy.intercept('/api/v1/metrics/**', []).as('getMetricForX')
    // cy.intercept('/api/v1/management/events?*', []).as('getEvents')
  })

  it('should render properly', () => {
    const onClose = cy.stub().as('onClose')
    const onEditEntity = cy.stub().as('onEditEntity')
    cy.mountWithProviders(
      <GroupPropertyDrawer
        nodeId="adapter@fgffgf"
        nodes={mockNodes}
        selectedNode={mockNode}
        isOpen={true}
        onClose={onClose}
        onEditEntity={onEditEntity}
      />
    )

    cy.wait('@getMetricForX')

    cy.getByTestId('group-panel-title').should('contain.text', 'Group Observability')
    cy.getByTestId('group-panel-keys').find('p').eq(0).should('contain.text', 'adapter: bridge-id-01')
    cy.getByTestId('group-panel-keys').find('p').eq(1).should('contain.text', 'adapter: my-adapter')

    cy.get('dt').eq(0).should('contain.text', 'bridge-id-01')
    cy.get('dt').eq(1).should('contain.text', 'my-adapter')

    // check the panel control
    cy.getByAriaLabel('Close').click()
    cy.get('@onClose').should('have.been.calledOnce')

    // check that the metrics is there
    cy.getByTestId('metrics-toggle').should('be.visible')
  })

  it('should be accessible', () => {
    cy.injectAxe()
    cy.mountWithProviders(
      <GroupPropertyDrawer
        nodeId="adapter@fgffgf"
        nodes={mockNodes}
        selectedNode={mockNode}
        isOpen={true}
        onClose={cy.stub()}
        onEditEntity={cy.stub()}
      />
    )

    cy.checkAccessibility()
    cy.percySnapshot('Component: GroupPropertyDrawer')
  })
})
