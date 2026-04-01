/// <reference types="cypress" />

import { Route, Routes } from 'react-router-dom'
import type { Node } from '@xyflow/react'

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

const getWrapperWith = (initialNodes?: Node[], routePath = '/node/:nodeId') => {
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
          <Route path={routePath} element={children} />
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

  describe('Enable All Metadata button', () => {
    const adapterNodes = [{ ...MOCK_NODE_ADAPTER, position: { x: 0, y: 0 } }]

    it('should show the button for northbound mappings with includeMetadata disabled', () => {
      cy.intercept('/api/v1/management/protocol-adapters/adapters/*/northboundMappings', {
        items: [{ ...MOCK_NORTHBOUND_MAPPING, includeMetadata: false }],
      }).as('getMappings')

      cy.mountWithProviders(<AdapterMappingManager type={MappingType.NORTHBOUND} />, {
        routerProps: { initialEntries: [`/adapter/idAdapter`] },
        wrapper: getWrapperWith(adapterNodes, '/adapter/:adapterId'),
      })

      cy.wait('@getMappings')
      cy.get('button').contains('Enable All Metadata').should('be.visible').and('not.be.disabled')
    })

    it('should disable the button when all mappings already have includeMetadata enabled', () => {
      cy.intercept('/api/v1/management/protocol-adapters/adapters/*/northboundMappings', {
        items: [{ ...MOCK_NORTHBOUND_MAPPING, includeMetadata: true }],
      }).as('getMappings')

      cy.mountWithProviders(<AdapterMappingManager type={MappingType.NORTHBOUND} />, {
        routerProps: { initialEntries: [`/adapter/idAdapter`] },
        wrapper: getWrapperWith(adapterNodes, '/adapter/:adapterId'),
      })

      cy.wait('@getMappings')
      cy.get('button').contains('Enable All Metadata').should('be.disabled')
    })

    it('should not show the button for southbound mappings', () => {
      cy.intercept('/api/v1/management/protocol-adapters/adapters/*/southboundMappings', {
        items: [MOCK_SOUTHBOUND_MAPPING],
      }).as('getMappings')

      cy.mountWithProviders(<AdapterMappingManager type={MappingType.SOUTHBOUND} />, {
        routerProps: { initialEntries: [`/adapter/idAdapter`] },
        wrapper: getWrapperWith(adapterNodes, '/adapter/:adapterId'),
      })

      cy.get('button').contains('Enable All Metadata').should('not.exist')
    })

    it('should send update request with includeMetadata enabled on all items when clicked', () => {
      const mappingWithoutMetadata = { ...MOCK_NORTHBOUND_MAPPING, includeMetadata: false }
      cy.intercept('GET', '/api/v1/management/protocol-adapters/adapters/*/northboundMappings', {
        items: [mappingWithoutMetadata],
      }).as('getMappings')
      cy.intercept('PUT', '/api/v1/management/protocol-adapters/adapters/*/northboundMappings', (req) => {
        expect(req.body.items).to.have.length(1)
        expect(req.body.items[0].includeMetadata).to.equal(true)
        req.reply({ statusCode: 200, body: {} })
      }).as('updateMappings')

      cy.mountWithProviders(<AdapterMappingManager type={MappingType.NORTHBOUND} />, {
        routerProps: { initialEntries: [`/adapter/idAdapter`] },
        wrapper: getWrapperWith(adapterNodes, '/adapter/:adapterId'),
      })

      cy.wait('@getMappings')
      cy.get('button').contains('Enable All Metadata').click()
      cy.wait('@updateMappings')
    })

    it('should handle multiple mappings with mixed includeMetadata states', () => {
      const mappings = [
        { ...MOCK_NORTHBOUND_MAPPING, tagName: 'tag1', includeMetadata: false },
        { ...MOCK_NORTHBOUND_MAPPING, tagName: 'tag2', includeMetadata: true },
        { ...MOCK_NORTHBOUND_MAPPING, tagName: 'tag3', includeMetadata: false },
      ]
      cy.intercept('GET', '/api/v1/management/protocol-adapters/adapters/*/northboundMappings', {
        items: mappings,
      }).as('getMappings')
      cy.intercept('PUT', '/api/v1/management/protocol-adapters/adapters/*/northboundMappings', (req) => {
        expect(req.body.items).to.have.length(3)
        expect(req.body.items.every((item: { includeMetadata: boolean }) => item.includeMetadata === true)).to.be.true
        req.reply({ statusCode: 200, body: {} })
      }).as('updateMappings')

      cy.mountWithProviders(<AdapterMappingManager type={MappingType.NORTHBOUND} />, {
        routerProps: { initialEntries: [`/adapter/idAdapter`] },
        wrapper: getWrapperWith(adapterNodes, '/adapter/:adapterId'),
      })

      cy.wait('@getMappings')
      cy.get('button').contains('Enable All Metadata').should('not.be.disabled').click()
      cy.wait('@updateMappings')
    })
  })
})
