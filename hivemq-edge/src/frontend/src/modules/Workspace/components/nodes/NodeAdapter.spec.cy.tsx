import { mockReactFlow } from '@/__test-utils__/react-flow/providers.tsx'
import { CustomNodeTesting } from '@/__test-utils__/react-flow/CustomNodeTesting.tsx'
import { MOCK_NODE_ADAPTER } from '@/__test-utils__/react-flow/nodes.ts'
import { MOCK_ADAPTER_ID } from '@/__test-utils__/mocks.ts'

import { mockProtocolAdapter } from '@/api/hooks/useProtocolAdapters/__handlers__'
import { formatTopicString } from '@/components/MQTT/topic-utils.ts'
import { NodeTypes } from '@/modules/Workspace/types.ts'

import NodeAdapter from './NodeAdapter.tsx'
import {
  MOCK_NORTHBOUND_MAPPING,
  MOCK_SOUTHBOUND_MAPPING,
} from '@/api/hooks/useProtocolAdapters/__handlers__/mapping.mocks.ts'

describe('NodeAdapter', () => {
  beforeEach(() => {
    cy.viewport(600, 400)
    cy.intercept('/api/v1/management/protocol-adapters/types', {
      items: [
        mockProtocolAdapter,
        {
          ...mockProtocolAdapter,
          id: 'opc-ua-client',
          capabilities: ['READ', 'DISCOVER', 'WRITE'],
        },
      ],
    })

    cy.intercept('/api/v1/management/protocol-adapters/adapters/*/northboundMappings', {
      items: [MOCK_NORTHBOUND_MAPPING],
    }).as('getNorthboundMappings')

    cy.intercept('/api/v1/management/protocol-adapters/adapters/*/southboundMappings', {
      items: [MOCK_SOUTHBOUND_MAPPING],
    }).as('getSouthboundMappings')
  })

  it('should render properly', () => {
    cy.mountWithProviders(mockReactFlow(<NodeAdapter {...MOCK_NODE_ADAPTER} />))

    cy.getByTestId('adapter-node-name').should('contain', MOCK_ADAPTER_ID)
    cy.getByTestId('connection-status').should('contain.text', 'Connected')
    cy.getByTestId('topics-container').should('be.visible').should('contain.text', formatTopicString('my/topic'))
  })

  it('should render the selected adapter properly', () => {
    cy.mountWithProviders(
      <CustomNodeTesting
        nodes={[{ ...MOCK_NODE_ADAPTER, position: { x: 50, y: 100 }, selected: true }]}
        nodeTypes={{ [NodeTypes.ADAPTER_NODE]: NodeAdapter }}
      />
    )
    cy.getByTestId('adapter-node-name').should('contain', MOCK_ADAPTER_ID)
    cy.getByTestId('node-adapter-toolbar-northbound').should('have.attr', 'aria-label', 'Edit Northbound mappings')
    cy.getByTestId('node-group-toolbar-group').should('have.attr', 'aria-label', 'Group the selected adapters')
    cy.getByTestId('node-group-toolbar-panel').should('have.attr', 'aria-label', 'Open the overview panel')
  })

  it('should render the toolbar for bi-directional adapter', () => {
    cy.mountWithProviders(
      <CustomNodeTesting
        nodes={[
          {
            ...MOCK_NODE_ADAPTER,
            position: { x: 50, y: 100 },
            selected: true,
            data: { ...MOCK_NODE_ADAPTER.data, type: 'opc-ua-client' },
          },
        ]}
        nodeTypes={{ [NodeTypes.ADAPTER_NODE]: NodeAdapter }}
      />
    )
    cy.getByTestId('adapter-node-name').should('contain', MOCK_ADAPTER_ID)
    cy.getByTestId('node-adapter-toolbar-northbound').should('have.attr', 'aria-label', 'Edit Northbound mappings')
    cy.getByTestId('node-adapter-toolbar-southbound').should('have.attr', 'aria-label', 'Edit Southbound mappings')
    cy.getByTestId('node-group-toolbar-group').should('have.attr', 'aria-label', 'Group the selected adapters')
    cy.getByTestId('node-group-toolbar-panel').should('have.attr', 'aria-label', 'Open the overview panel')
  })

  it('should render the toolbar for multiple selected', () => {
    cy.mountWithProviders(
      <CustomNodeTesting
        nodes={[
          {
            ...MOCK_NODE_ADAPTER,
            position: { x: 50, y: 100 },
            selected: true,
          },
          {
            ...MOCK_NODE_ADAPTER,
            id: 'idAdapter2',
            position: { x: 50, y: 100 },
            selected: true,
            hidden: true,
          },
          {
            ...MOCK_NODE_ADAPTER,
            id: 'idAdapter3',
            position: { x: 50, y: 100 },
            selected: true,
            hidden: true,
          },
        ]}
        nodeTypes={{ [NodeTypes.ADAPTER_NODE]: NodeAdapter }}
      />
    )
    cy.getByTestId('adapter-node-name').should('contain', MOCK_ADAPTER_ID)
    cy.get('[role="toolbar"][aria-label="Node toolbar"]').should('be.visible')
    cy.getByTestId('toolbar-title').should('have.text', '3 entities selected')
    // cy.getByTestId('node-adapter-toolbar-northbound').should('have.attr', 'aria-label', 'Edit Northbound mappings')
    cy.getByTestId('node-group-toolbar-group').should('have.attr', 'aria-label', 'Group the selected adapters')
    cy.getByTestId('node-group-toolbar-combiner').should(
      'have.attr',
      'aria-label',
      'Create a data combiner from selection'
    )
  })

  it('should render the toolbar properly', () => {
    cy.mountWithProviders(
      <CustomNodeTesting
        nodes={[
          {
            ...MOCK_NODE_ADAPTER,
            position: { x: 50, y: 100 },
            selected: true,
            data: { ...MOCK_NODE_ADAPTER.data, type: 'opc-ua-client' },
          },
        ]}
        nodeTypes={{ [NodeTypes.ADAPTER_NODE]: NodeAdapter }}
      />
    )

    cy.getByTestId('test-navigate-pathname').should('have.text', '/')
    cy.getByTestId('node-group-toolbar-panel').click()
    cy.getByTestId('test-navigate-pathname').should(
      'have.text',
      `/workspace/node/adapter/opc-ua-client/${MOCK_NODE_ADAPTER.id}`
    )

    cy.getByTestId('node-adapter-toolbar-southbound').click()
    cy.getByTestId('test-navigate-pathname').should(
      'have.text',
      `/workspace/node/adapter/opc-ua-client/${MOCK_NODE_ADAPTER.id}/southbound`
    )

    cy.getByTestId('node-adapter-toolbar-northbound').click()
    cy.getByTestId('test-navigate-pathname').should(
      'have.text',
      `/workspace/node/adapter/opc-ua-client/${MOCK_NODE_ADAPTER.id}/northbound`
    )
  })

  it('should be accessible', () => {
    cy.injectAxe()
    cy.mountWithProviders(mockReactFlow(<NodeAdapter {...MOCK_NODE_ADAPTER} />))

    cy.checkAccessibility()
    cy.percySnapshot('Component: NodeAdapter')
  })
})
