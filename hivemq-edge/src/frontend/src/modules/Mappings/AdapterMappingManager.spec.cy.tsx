/// <reference types="cypress" />

import { Route, Routes } from 'react-router-dom'
import { Node } from 'reactflow'

import { ReactFlowTesting } from '@/__test-utils__/react-flow/ReactFlowTesting.tsx'
import { MOCK_NODE_ADAPTER } from '@/__test-utils__/react-flow/nodes.ts'
import { mockAdapter, mockProtocolAdapter } from '@/api/hooks/useProtocolAdapters/__handlers__'
import useWorkspaceStore from '@/modules/Workspace/hooks/useWorkspaceStore.ts'
import AdapterMappingManager from '@/modules/Mappings/AdapterMappingManager.tsx'
import { MappingType } from '@/modules/Mappings/types.ts'
import {
  MOCK_NORTHBOUND_MAPPING,
  MOCK_SOUTHBOUND_MAPPING,
} from '@/api/hooks/useProtocolAdapters/__handlers__/mapping.mocks.ts'

const getWrapperWith = (initialNodes?: Node[]) => {
  const Wrapper: React.JSXElementConstructor<{ children: React.ReactNode }> = ({ children }) => {
    const { nodes } = useWorkspaceStore()
    return (
      <ReactFlowTesting
        config={{
          initialState: {
            nodes: initialNodes,
          },
        }}
        showDashboard={true}
        dashboard={<div data-testid="data-length">{nodes.length}</div>}
      >
        <Routes>
          <Route path="/node/:nodeId" element={children}></Route>
        </Routes>
      </ReactFlowTesting>
    )
  }

  return Wrapper
}

describe('AdapterMappingManager', () => {
  beforeEach(() => {
    cy.viewport(800, 800)
    cy.intercept('/api/v1/management/protocol-adapters/types', { items: [mockProtocolAdapter] }).as('getProtocol')
    cy.intercept('/api/v1/management/protocol-adapters/adapters', { items: [mockAdapter] }).as('getAdapter')
    cy.intercept('/api/v1/management/bridges', { items: [] })
  })

  it('should render the drawer', () => {
    cy.mountWithProviders(<AdapterMappingManager type={MappingType.NORTHBOUND} />, {
      routerProps: { initialEntries: [`/node/wrong-adapter`] },
      wrapper: getWrapperWith(),
    })

    cy.get('[role="dialog"]').should('be.visible')

    cy.get('header').should('contain.text', 'Manage Northbound mappings')
    cy.get('[role="dialog"]').find('button').as('dialog-buttons').should('have.length', 3)
    cy.get('@dialog-buttons').eq(0).should('have.attr', 'aria-label', 'Close')
    cy.get('@dialog-buttons').eq(1).should('have.attr', 'aria-label', 'Shrink')
    cy.get('@dialog-buttons').eq(2).should('have.text', 'Submit')

    cy.get('@dialog-buttons').eq(1).click()
    cy.get('@dialog-buttons').eq(1).should('have.attr', 'aria-label', 'Expand')
    cy.get('@dialog-buttons').eq(0).click()
    cy.get('[role="dialog"]').should('not.exist')
  })

  it('should render error properly', () => {
    cy.mountWithProviders(<AdapterMappingManager type={MappingType.NORTHBOUND} />, {
      routerProps: { initialEntries: [`/node/wrong-adapter`] },
      wrapper: getWrapperWith(),
    })

    cy.get('[role="dialog"]').should('be.visible')

    cy.get('[role="alert"]').should('be.visible')
    cy.get('[role="alert"] span').should('have.attr', 'data-status', 'error')
    cy.get('[role="alert"] div div')
      .should('have.attr', 'data-status', 'error')
      .should('contain.text', 'We cannot load your adapters for the time being. Please try again later')
  })

  it('should render Northbound properly', () => {
    cy.intercept('/api/v1/management/protocol-adapters/adapters/*/northboundMappings', {
      items: [MOCK_NORTHBOUND_MAPPING],
    }).as('getMappings')

    cy.mountWithProviders(<AdapterMappingManager type={MappingType.NORTHBOUND} />, {
      routerProps: { initialEntries: [`/node/idAdapter`] },
      wrapper: getWrapperWith([{ ...MOCK_NODE_ADAPTER, position: { x: 0, y: 0 } }]),
    })

    cy.getByTestId('data-length').should('contain.text', '1')
    cy.get('header').should('contain.text', 'Manage Northbound mappings')
  })

  it('should render Southbound properly', () => {
    cy.intercept('/api/v1/management/protocol-adapters/adapters/*/southboundMappings', {
      items: [MOCK_SOUTHBOUND_MAPPING],
    }).as('getMappings')

    cy.mountWithProviders(<AdapterMappingManager type={MappingType.SOUTHBOUND} />, {
      routerProps: { initialEntries: [`/node/idAdapter`] },
      wrapper: getWrapperWith([{ ...MOCK_NODE_ADAPTER, position: { x: 0, y: 0 } }]),
    })
    cy.get('header').should('contain.text', 'Manage Southbound mappings')

    cy.get('[role="alert"]').should('be.visible')
  })
})
