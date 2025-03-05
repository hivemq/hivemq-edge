import { Route, Routes, useLocation } from 'react-router-dom'
import type { Node } from 'reactflow'

import { ReactFlowTesting } from '@/__test-utils__/react-flow/ReactFlowTesting.tsx'
import { MOCK_DEFAULT_NODE, MOCK_NODE_COMBINER } from '@/__test-utils__/react-flow/nodes.ts'
import { MockAdapterType } from '@/__test-utils__/adapters/types'
import { MOCK_DEVICE_TAGS, mockAdapter, mockProtocolAdapter_OPCUA } from '@/api/hooks/useProtocolAdapters/__handlers__'
import { MOCK_TOPIC_FILTER } from '@/api/hooks/useTopicFilters/__handlers__'
import { mockEmptyCombiner } from '@/api/hooks/useCombiners/__handlers__'

import { NodeTypes } from '@/modules/Workspace/types'
import CombinerMappingManager from './CombinerMappingManager'

const getWrapperWith = (initialNodes?: Node[]) => {
  const Wrapper: React.JSXElementConstructor<{ children: React.ReactNode }> = ({ children }) => {
    const { pathname } = useLocation()

    return (
      <ReactFlowTesting
        config={{
          initialState: {
            nodes: initialNodes,
          },
        }}
        showDashboard={true}
        dashboard={<div data-testid="data-pathname">{pathname}</div>}
      >
        <Routes>
          <Route path="/node/:combinerId" element={children}></Route>
        </Routes>
      </ReactFlowTesting>
    )
  }

  return Wrapper
}

describe('CombinerMappingManager', () => {
  beforeEach(() => {
    cy.viewport(800, 800)
  })

  it('should render the drawer', () => {
    cy.mountWithProviders(<CombinerMappingManager />, {
      routerProps: { initialEntries: [`/node/wrong-adapter`] },
      wrapper: getWrapperWith(),
    })

    cy.get('[role="dialog"]').should('be.visible')

    cy.get('header').should('contain.text', 'Manage Data combining mappings')
    cy.get('[role="dialog"]').find('button').as('dialog-buttons').should('have.length', 1)
    cy.get('@dialog-buttons').eq(0).should('have.attr', 'aria-label', 'Close')

    cy.get('@dialog-buttons').eq(0).click()
    cy.get('[role="dialog"]').should('not.exist')
  })

  it('should render error properly', () => {
    cy.mountWithProviders(<CombinerMappingManager />, {
      routerProps: { initialEntries: [`/node/wrong-adapter`] },
      wrapper: getWrapperWith(),
    })

    cy.get('[role="dialog"]').should('be.visible')

    cy.get('[role="alert"]').should('be.visible')
    cy.get('[role="alert"] span').should('have.attr', 'data-status', 'error')
    cy.get('[role="alert"] div div')
      .should('have.attr', 'data-status', 'error')
      .should('contain.text', 'There was a problem loading the data')
  })

  it('should render data combining properly', () => {
    cy.mountWithProviders(<CombinerMappingManager />, {
      routerProps: { initialEntries: [`/node/idCombiner`] },
      wrapper: getWrapperWith([{ ...MOCK_NODE_COMBINER, position: { x: 0, y: 0 } }]),
    })
    cy.get('header').should('contain.text', 'Manage Data combining mappings')

    cy.getByTestId('node-type-icon').should('exist').should('have.attr', 'data-nodeicon', NodeTypes.COMBINER_NODE)
    cy.getByTestId('node-name').should('contain.text', 'my-combiner')
    cy.getByTestId('node-description').should('contain.text', 'Data Combiner')

    cy.get('[role="tablist"] [role="tab"]').should('have.length', 3)
    cy.get('[role="tablist"] [role="tab"]').eq(0).should('have.text', 'Configuration')
    cy.get('[role="tablist"] [role="tab"]').eq(1).should('have.text', 'Sources')
    cy.get('[role="tablist"] [role="tab"]').eq(2).should('have.text', 'Mappings')

    // TODO[TEST] More tests. But we need a strategy for testing OpenAPI/JSONSchema/RJSF forms
  })

  it('should render the toolbar properly', () => {
    cy.intercept('DELETE', '/api/v1/management/combiners/**', { deleted: 'the combiner' }).as('delete')
    cy.mountWithProviders(<CombinerMappingManager />, {
      routerProps: { initialEntries: [`/node/idCombiner`] },
      wrapper: getWrapperWith([{ ...MOCK_NODE_COMBINER, position: { x: 0, y: 0 } }]),
    })

    // eslint-disable-next-line cypress/no-unnecessary-waiting
    cy.wait(500)

    cy.getByTestId('data-pathname').should('have.text', '/node/idCombiner')

    cy.get('footer').within(() => {
      cy.get('button').eq(0).should('have.text', 'Delete')
      cy.get('button').eq(1).should('have.text', 'Submit')

      cy.get('button').eq(0).click()
    })

    cy.get('section[role="alertdialog"]').should('be.visible')
    cy.get('section[role="alertdialog"]').within(() => {
      cy.get('footer button').eq(1).click()
    })

    cy.wait('@delete')
    cy.get('[role="dialog"]').should('not.exist')
    cy.getByTestId('data-pathname').should('have.text', '/workspace')

    cy.get('[role="status"]').should('contain.text', 'Delete the combiner')
    cy.get('[role="status"]').should('contain.text', "We've successfully deleted the combiner for you.")
    cy.get('[role="status"] > div').should('have.attr', 'data-status', 'success')
  })

  it('should publish properly', () => {
    cy.intercept('/api/v1/management/protocol-adapters/adapters/my-adapter/tags', {
      items: MOCK_DEVICE_TAGS('my-adapter', MockAdapterType.OPC_UA),
    }).as('getTags1')
    cy.intercept('/api/v1/management/protocol-adapters/adapters/my-other-adapter/tags', {
      items: MOCK_DEVICE_TAGS('my-other-adapter', MockAdapterType.OPC_UA),
    }).as('getTags2')
    cy.intercept('/api/v1/management/topic-filters', {
      items: [
        MOCK_TOPIC_FILTER,
        {
          topicFilter: 'another/filter',
          description: 'This is a topic filter',
        },
      ],
    }).as('getTopicFilters')
    cy.intercept('/api/v1/management/protocol-adapters/types', { items: [mockProtocolAdapter_OPCUA] }).as(
      'getProtocols'
    )
    cy.intercept('api/v1/management/protocol-adapters/adapters', {
      items: [
        {
          ...mockAdapter,
          id: 'my-adapter',
          type: 'opcua',
        },
        {
          ...mockAdapter,
          id: 'my-other-adapter',
          type: 'opcua',
        },
      ],
    }).as('getAdapters')
    cy.intercept('PUT', 'api/v1/management/combiners/**', { updated: 'the combiner' }).as('update')

    cy.mountWithProviders(<CombinerMappingManager />, {
      routerProps: { initialEntries: [`/node/idCombiner`] },
      wrapper: getWrapperWith([
        {
          id: 'idCombiner',
          type: NodeTypes.COMBINER_NODE,
          data: mockEmptyCombiner,
          ...MOCK_DEFAULT_NODE,
          position: { x: 0, y: 0 },
        },
      ]),
    })

    // eslint-disable-next-line cypress/no-unnecessary-waiting
    cy.wait(500)

    cy.get('footer').within(() => {
      cy.get('button').eq(1).click()
    })

    cy.wait('@update')
    cy.get('[role="dialog"]').should('not.exist')
    cy.getByTestId('data-pathname').should('have.text', '/workspace')

    cy.get('[role="status"]').should('contain.text', 'Update the combiner')
    cy.get('[role="status"]').should('contain.text', "We've successfully updated the combiner for you.")
    cy.get('[role="status"] > div').should('have.attr', 'data-status', 'success')
  })

  it('should be accessible', () => {
    cy.injectAxe()
    cy.mountWithProviders(<CombinerMappingManager />, {
      routerProps: { initialEntries: [`/node/idCombiner`] },
      wrapper: getWrapperWith([{ ...MOCK_NODE_COMBINER, position: { x: 0, y: 0 } }]),
    })

    cy.checkAccessibility()

    cy.get('[role="tablist"] [role="tab"]').eq(0).should('have.text', 'Configuration')
    cy.checkAccessibility()

    cy.get('[role="tablist"] [role="tab"]').eq(1).should('have.text', 'Sources')
    cy.get('[role="tablist"] [role="tab"]').eq(1).click()
    cy.checkAccessibility()

    cy.get('[role="tablist"] [role="tab"]').eq(2).should('have.text', 'Mappings')
    cy.get('[role="tablist"] [role="tab"]').eq(2).click()
    cy.checkAccessibility()
  })
})
