import { MOCK_ADAPTER_OPC_UA, MOCK_PROTOCOL_OPC_UA } from '@/__test-utils__/adapters/opc-ua.ts'
import { MOCK_PROTOCOL_S7 } from '@/__test-utils__/adapters/s7.ts'
import { MOCK_ADAPTER_SIMULATION, MOCK_PROTOCOL_SIMULATION } from '@/__test-utils__/adapters/simulation.ts'
import { MOCK_NODE_ADAPTER, MOCK_NODE_COMBINER, MOCK_NODE_DEVICE } from '@/__test-utils__/react-flow/nodes.ts'
import { ReactFlowTesting } from '@/__test-utils__/react-flow/ReactFlowTesting.tsx'
import { mockBridge } from '@/api/hooks/useGetBridges/__handlers__'
import { MOCK_DEVICE_TAG_ADDRESS_MODBUS } from '@/api/hooks/useProtocolAdapters/__handlers__'
import {
  MOCK_NORTHBOUND_MAPPING,
  MOCK_SOUTHBOUND_MAPPING,
} from '@/api/hooks/useProtocolAdapters/__handlers__/mapping.mocks.ts'
import { MOCK_TOPIC_FILTER } from '@/api/hooks/useTopicFilters/__handlers__'
import DrawerFilterToolbox from '@/modules/Workspace/components/filters/DrawerFilterToolbox.tsx'
import type { Edge, Node } from '@xyflow/react'

const getWrapperWith = (initialNodes?: Node[], initialEdges?: Edge[]) => {
  const Wrapper: React.JSXElementConstructor<{ children: React.ReactNode }> = ({ children }) => {
    return (
      <ReactFlowTesting
        config={{
          initialState: {
            nodes: initialNodes,
            edges: initialEdges,
          },
        }}
      >
        {children}
      </ReactFlowTesting>
    )
  }

  return Wrapper
}

const cy_localStorage_should = (chainer: 'equal', value: unknown) => {
  cy.getAllLocalStorage().then((localStorage) => {
    const storage = Object.values(localStorage)[0]['edge.workspace.filter']
    cy.wrap(storage).should(chainer, JSON.stringify(value))
  })
}

describe('DrawerFilterToolbox', () => {
  beforeEach(() => {
    cy.viewport(800, 800)

    cy.intercept('/api/v1/management/protocol-adapters/types', {
      items: [MOCK_PROTOCOL_OPC_UA, MOCK_PROTOCOL_SIMULATION, MOCK_PROTOCOL_S7],
    })
    cy.intercept('/api/v1/management/protocol-adapters/adapters', {
      items: [MOCK_ADAPTER_OPC_UA, { ...MOCK_ADAPTER_OPC_UA, id: 'opcua-boiler' }, MOCK_ADAPTER_SIMULATION],
    })

    cy.intercept('/api/v1/management/protocol-adapters/northboundMappings', {
      items: [MOCK_NORTHBOUND_MAPPING],
    })
    cy.intercept('/api/v1/management/protocol-adapters/southboundMappings', { items: [MOCK_SOUTHBOUND_MAPPING] })
    cy.intercept('/api/v1/management/protocol-adapters/tags', {
      items: [{ name: 'test/tag1', definition: MOCK_DEVICE_TAG_ADDRESS_MODBUS }],
    })
    cy.intercept('/api/v1/management/topic-filters', {
      items: [MOCK_TOPIC_FILTER],
    })
    cy.intercept('/api/v1/management/bridges', { items: [mockBridge] })
  })

  it('should render properly', () => {
    cy.mountWithProviders(<DrawerFilterToolbox />, {
      wrapper: getWrapperWith([
        { ...MOCK_NODE_ADAPTER, position: { x: 50, y: 100 } },
        { ...MOCK_NODE_DEVICE, position: { x: 50, y: 100 } },
        { ...MOCK_NODE_COMBINER, position: { x: 50, y: 100 } },
      ]),
    })

    cy.get('[role="dialog"]#chakra-modal-filter-workspace').should('not.exist')
    cy.getByTestId('toolbox-filter').within(() => {
      cy.getByTestId('toolbox-filter-open').should('have.text', 'Filter Workspace')
      cy.getByTestId('toolbox-filter-clearAll').should('have.text', 'Clear filters').should('be.disabled')

      cy.getByTestId('toolbox-filter-open').click()
    })

    cy.get('[role="dialog"]#chakra-modal-filter-workspace').should('be.visible')

    cy.get('[role="dialog"]#chakra-modal-filter-workspace').within(() => {
      cy.get('header').should('have.text', 'Filter Workspace')
      cy.getByTestId('workspace-quick-filters-container').should('be.visible')

      // this is not enough; check integration of wrapper and criteria
      cy.getByTestId('workspace-filter-selection-container').should('be.visible')
      cy.getByTestId('workspace-filter-entities-container').should('exist')
      cy.getByTestId('workspace-filter-protocols-container').should('exist')
      cy.getByTestId('workspace-filter-topic-container').should('exist')
      cy.getByTestId('workspace-filter-status-container').should('exist')
      cy.getByTestId('workspace-filter-options-container').should('exist')

      cy.get('footer').within(() => {
        cy.getByTestId('filter-apply').should('be.visible')
        cy.getByTestId('filter-clearAll').should('be.visible')
      })

      cy.getByAriaLabel('Close').click()
    })

    cy.get('[role="dialog"]#chakra-modal-filter-workspace').should('not.exist')
  })

  it('should store the filters', () => {
    cy.mountWithProviders(<DrawerFilterToolbox />, {
      wrapper: getWrapperWith([
        { ...MOCK_NODE_ADAPTER, position: { x: 50, y: 100 } },
        { ...MOCK_NODE_DEVICE, position: { x: 50, y: 100 } },
        { ...MOCK_NODE_COMBINER, position: { x: 50, y: 100 } },
      ]),
    })

    cy.getByTestId('toolbox-filter-open').click()

    cy.get('[role="dialog"]#chakra-modal-filter-workspace').should('be.visible')
    cy_localStorage_should('equal', {
      options: {
        isLiveUpdate: false,
        joinOperator: 'OR',
      },
    })

    cy.getByTestId('workspace-filter-entities-container').within(() => {
      cy.get('label').should('not.have.attr', 'data-checked')
    })
    cy.getByTestId('workspace-filter-status-container').within(() => {
      cy.get('label').should('not.have.attr', 'data-checked')
    })

    cy.get('[role="group"] #workspace-filter-entities-trigger').type('adapt{enter}')
    cy.get('[role="group"] #workspace-filter-status-trigger').type('connec{enter}')

    cy.getByTestId('workspace-filter-entities-container').within(() => {
      cy.get('label').should('have.attr', 'data-checked')
    })
    cy.getByTestId('workspace-filter-status-container').within(() => {
      cy.get('label').should('have.attr', 'data-checked')
    })

    cy_localStorage_should('equal', {
      options: {
        isLiveUpdate: false,
        joinOperator: 'OR',
      },
      entities: { isActive: true, filter: [{ label: 'Adapters', value: 'ADAPTER_NODE' }] },
      status: { isActive: true, filter: [{ label: 'Connected', status: 'CONNECTED' }] },
    })
  })

  it('should clear the filters', () => {
    const onClearFilters = cy.stub().as('onClearFilters')
    cy.mountWithProviders(<DrawerFilterToolbox onClearFilters={onClearFilters} />, {
      wrapper: getWrapperWith([
        { ...MOCK_NODE_ADAPTER, position: { x: 50, y: 100 } },
        { ...MOCK_NODE_DEVICE, position: { x: 50, y: 100 } },
        { ...MOCK_NODE_COMBINER, position: { x: 50, y: 100 } },
      ]),
    })

    cy.getByTestId('toolbox-filter-clearAll').should('be.disabled')
    cy.get('@onClearFilters').should('not.have.been.called')
    cy.getByTestId('toolbox-filter-open').click()
    cy.get('[role="group"] #workspace-filter-entities-trigger').type('adapt{enter}')
    cy.get('[role="group"] #workspace-filter-status-trigger').type('connec{enter}')

    cy.getByAriaLabel('Close').click()

    cy.getByTestId('toolbox-filter-clearAll').should('not.be.disabled')
    cy.getByTestId('toolbox-filter-clearAll').click()
    cy.get('@onClearFilters').should('have.been.called')
    cy.getByTestId('toolbox-filter-clearAll').should('be.disabled')

    cy.getByTestId('toolbox-filter-open').click()
    cy.get('[role="group"] #workspace-filter-entities-trigger').type('adapt{enter}')
    cy.get('[role="group"] #workspace-filter-status-trigger').type('connec{enter}')

    cy.getByTestId('filter-clearAll').click()
    cy.get('@onClearFilters').should('have.been.called')
    cy.getByTestId('toolbox-filter-clearAll').should('be.disabled')
  })

  it('should apply the filters', () => {
    const onApplyFilters = cy.stub().as('onApplyFilters')
    cy.mountWithProviders(<DrawerFilterToolbox onApplyFilters={onApplyFilters} />, {
      wrapper: getWrapperWith([
        { ...MOCK_NODE_ADAPTER, position: { x: 50, y: 100 } },
        { ...MOCK_NODE_DEVICE, position: { x: 50, y: 100 } },
        { ...MOCK_NODE_COMBINER, position: { x: 50, y: 100 } },
      ]),
    })

    cy.getByTestId('toolbox-filter-clearAll').should('be.disabled')
    cy.get('@onApplyFilters').should('not.have.been.called')

    cy.getByTestId('toolbox-filter-open').click()
    cy.get('[role="group"] #workspace-filter-entities-trigger').type('adapt{enter}')
    cy.get('@onApplyFilters').should('have.been.calledWith', {
      options: {
        isLiveUpdate: false,
        joinOperator: 'OR',
      },
      entities: {
        isActive: true,
        filter: [
          {
            label: 'Adapters',
            value: 'ADAPTER_NODE',
          },
        ],
      },
    })

    cy.get('[role="group"] #workspace-filter-status-trigger').type('connec{enter}')
    cy.get('@onApplyFilters').should('have.been.calledWith', {
      options: {
        isLiveUpdate: false,
        joinOperator: 'OR',
      },
      entities: {
        isActive: true,
        filter: [
          {
            label: 'Adapters',
            value: 'ADAPTER_NODE',
          },
        ],
      },
      status: {
        isActive: true,
        filter: [
          {
            label: 'Connected',
            status: 'CONNECTED',
          },
        ],
      },
    })

    cy.getByTestId('filter-apply').click()
    cy.get('@onApplyFilters').should('have.been.called')
  })

  it('should be accessible', () => {
    cy.injectAxe()
    cy.mountWithProviders(<DrawerFilterToolbox />, { wrapper: getWrapperWith() })

    cy.getByTestId('toolbox-filter-open').click()

    cy.checkAccessibility()
  })
})
