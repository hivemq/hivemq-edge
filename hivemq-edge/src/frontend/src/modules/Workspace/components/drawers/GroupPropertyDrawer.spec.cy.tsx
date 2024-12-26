/// <reference types="cypress" />

import { Node } from 'reactflow'
import { Group, NodeTypes } from '../../types.ts'
import GroupPropertyDrawer from './GroupPropertyDrawer.tsx'
import { MOCK_METRICS } from '@/api/hooks/useGetMetrics/__handlers__'
import { MetricList } from '@/api/__generated__'
import { MOCK_NODE_ADAPTER } from '@/__test-utils__/react-flow/nodes.ts'
import { MOCK_ADAPTER_ID, MOCK_ADAPTER_ID2 } from '@/__test-utils__/mocks.ts'

const mockNode: Node<Group> = {
  position: { x: 0, y: 0 },
  id: 'adapter@group',
  type: NodeTypes.CLUSTER_NODE,
  data: { childrenNodeIds: [MOCK_ADAPTER_ID, MOCK_ADAPTER_ID2], title: 'the group', isOpen: true },
}

const mockNodes: Node<NodeTypes.ADAPTER_NODE>[] = [
  {
    ...MOCK_NODE_ADAPTER,
    id: MOCK_ADAPTER_ID,
    data: { ...MOCK_NODE_ADAPTER.data, id: MOCK_ADAPTER_ID },
    position: { x: 0, y: 0 },
  },
  {
    ...MOCK_NODE_ADAPTER,
    id: MOCK_ADAPTER_ID2,
    data: { ...MOCK_NODE_ADAPTER.data, id: MOCK_ADAPTER_ID2 },
    position: { x: 0, y: 0 },
  },
]

describe('GroupPropertyDrawer', () => {
  beforeEach(() => {
    cy.viewport(800, 800)
    cy.intercept('/api/v1/metrics', { items: MOCK_METRICS } as MetricList)
    cy.intercept('/api/v1/metrics/**', []).as('getMetricForX')
  })

  it('should render properly', () => {
    const onClose = cy.stub().as('onClose')
    const onEditEntity = cy.stub()
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

    cy.get('dt').eq(0).should('contain.text', 'bridge-id-01')
    cy.get('dt').eq(1).should('contain.text', 'my-adapter')

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
