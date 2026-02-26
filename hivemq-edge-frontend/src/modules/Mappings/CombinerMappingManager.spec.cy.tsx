import { MOCK_CAPABILITY_PULSE_ASSETS } from '@/api/hooks/useFrontendServices/__handlers__'
import { Route, Routes, useLocation, useNavigate } from 'react-router-dom'
import type { Node } from '@xyflow/react'
import { Button } from '@chakra-ui/react'

import { ReactFlowTesting } from '@/__test-utils__/react-flow/ReactFlowTesting.tsx'
import { MOCK_DEFAULT_NODE, MOCK_NODE_COMBINER } from '@/__test-utils__/react-flow/nodes.ts'
import { MockAdapterType } from '@/__test-utils__/adapters/types'
import { MOCK_DEVICE_TAGS, mockAdapter, mockProtocolAdapter_OPCUA } from '@/api/hooks/useProtocolAdapters/__handlers__'
import { MOCK_TOPIC_FILTER } from '@/api/hooks/useTopicFilters/__handlers__'
import { mockEmptyCombiner } from '@/api/hooks/useCombiners/__handlers__'

import { EntityType } from '@/api/__generated__'
import { IdStubs, NodeTypes } from '@/modules/Workspace/types'
import CombinerMappingManager from './CombinerMappingManager'

const INITIAL_ENTRY = '/workspace'
const getWrapperWith = (nodeId: string, initialNodes?: Node[]) => {
  const Wrapper: React.JSXElementConstructor<{ children: React.ReactNode }> = ({ children }) => {
    const { pathname } = useLocation()
    const navigate = useNavigate()

    return (
      <ReactFlowTesting
        config={{
          initialState: {
            nodes: initialNodes,
          },
        }}
        showDashboard={true}
        showReactFlowElements={true}
        dashboard={<div data-testid="data-pathname">{pathname}</div>}
      >
        <Routes>
          <Route
            path={INITIAL_ENTRY}
            element={
              <Button data-testid="test-reactflow-trigger-button" onClick={() => navigate(`/node/${nodeId}`)}>
                Open Mapper
              </Button>
            }
          />
          <Route path="/node/:combinerId" element={<div>{children}</div>} />
        </Routes>
      </ReactFlowTesting>
    )
  }

  return Wrapper
}

describe('CombinerMappingManager', () => {
  // All test skipped due to issues with catching the error at mount time
  beforeEach(() => {
    cy.viewport(800, 800)
    cy.intercept('/api/v1/management/topic-filters', { statusCode: 203, log: false })
    cy.intercept('/api/v1/management/protocol-adapters/types', { statusCode: 203, log: false })
    cy.intercept('/api/v1/management/protocol-adapters/adapters', { statusCode: 203, log: false })
    cy.intercept('/api/v1/management/protocol-adapters/adapters/**/tags', { statusCode: 203, log: false })

    cy.intercept('/api/v1/frontend/capabilities', { items: [MOCK_CAPABILITY_PULSE_ASSETS] })
    cy.intercept('/api/v1/management/pulse/managed-assets', { statusCode: 203 })
  })

  it('should render error properly', () => {
    let caughtError: Error | null = null
    cy.on('uncaught:exception', (err) => {
      caughtError = err
      return false // Prevent Cypress from failing
    })

    cy.mountWithProviders(<CombinerMappingManager />, {
      routerProps: { initialEntries: [INITIAL_ENTRY] },
      wrapper: getWrapperWith('wrongNode', [{ ...MOCK_NODE_COMBINER, position: { x: 0, y: 0 } }]),
    })

    // Must mimic the click on the ReactFlow node to open the drawer
    cy.getByTestId('test-reactflow-trigger-button').click()

    cy.wrap(null).then(() => {
      expect(caughtError).to.not.be.null
      expect(caughtError?.message).to.include('No combiner node found')
    })
  })

  it('should render the drawer', () => {
    cy.mountWithProviders(<CombinerMappingManager />, {
      routerProps: { initialEntries: [INITIAL_ENTRY] },
      wrapper: getWrapperWith('idCombiner', [{ ...MOCK_NODE_COMBINER, position: { x: 0, y: 0 } }]),
    })

    // Must mimic the click on the ReactFlow node to open the drawer
    cy.getByTestId('test-reactflow-trigger-button').click()

    cy.get('[role="dialog"]').should('be.visible')

    cy.get('header').should('contain.text', 'Manage Data combining mappings')
    cy.get('[role="dialog"]').find('button[aria-label="Close"]').as('dialog-close')

    cy.get('@dialog-close').click()
    cy.get('[role="dialog"]').should('not.exist')
    cy.getByTestId('data-pathname').should('have.text', INITIAL_ENTRY)
  })

  it('should render data combining properly', () => {
    cy.mountWithProviders(<CombinerMappingManager />, {
      routerProps: { initialEntries: [INITIAL_ENTRY] },
      wrapper: getWrapperWith('idCombiner', [{ ...MOCK_NODE_COMBINER, position: { x: 0, y: 0 } }]),
    })

    // Must mimic the click on the ReactFlow node to open the drawer
    cy.getByTestId('test-reactflow-trigger-button').click()

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
    let caughtError: Error | null = null
    cy.on('uncaught:exception', (err) => {
      caughtError = err
      return false // Prevent Cypress from failing
    })

    cy.intercept('DELETE', '/api/v1/management/combiners/**', { deleted: 'the combiner' }).as('delete')
    cy.mountWithProviders(<CombinerMappingManager />, {
      routerProps: { initialEntries: [INITIAL_ENTRY] },
      wrapper: getWrapperWith('idCombiner', [{ ...MOCK_NODE_COMBINER, position: { x: 0, y: 0 } }]),
    })

    // Must mimic the click on the ReactFlow node to open the drawer
    cy.getByTestId('test-reactflow-trigger-button').click()
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
    cy.wrap(null).then(() => {
      expect(caughtError).to.not.be.null
      expect(caughtError?.message).to.include('No combiner node found')
    })
    cy.wait('@delete')
    cy.get('[role="dialog"]').should('not.exist')
    cy.getByTestId('data-pathname').should('have.text', '/workspace')

    cy.get('[role="status"]').should('contain.text', 'Delete the combiner')
    cy.get('[role="status"]').should('contain.text', "We've successfully deleted the combiner for you.")
    cy.get('[role="status"] > div').should('have.attr', 'data-status', 'success')
    return
  })

  it('should publish properly', () => {
    cy.intercept('/api/v1/management/protocol-adapters/adapters/my-adapter/tags', {
      items: MOCK_DEVICE_TAGS('my-adapter', MockAdapterType.OPC_UA),
    })
    cy.intercept('/api/v1/management/protocol-adapters/adapters/my-other-adapter/tags', {
      items: MOCK_DEVICE_TAGS('my-other-adapter', MockAdapterType.OPC_UA),
    })
    cy.intercept('/api/v1/management/protocol-adapters/writing-schema/**', { body: {} })
    cy.intercept('/api/v1/management/topic-filters', {
      items: [
        MOCK_TOPIC_FILTER,
        {
          topicFilter: 'another/filter',
          description: 'This is a topic filter',
        },
      ],
    })
    cy.intercept('/api/v1/management/protocol-adapters/types', { items: [mockProtocolAdapter_OPCUA] })
    cy.intercept('/api/v1/management/protocol-adapters/adapters', {
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
    })
    cy.intercept('PUT', '/api/v1/management/combiners/**', { updated: 'the combiner' }).as('update')

    cy.mountWithProviders(<CombinerMappingManager />, {
      routerProps: { initialEntries: [INITIAL_ENTRY] },
      wrapper: getWrapperWith('idCombiner', [
        {
          id: 'idCombiner',
          type: NodeTypes.COMBINER_NODE,
          data: {
            ...mockEmptyCombiner,
            sources: {
              items: [
                { type: EntityType.ADAPTER, id: 'my-adapter' },
                { id: IdStubs.EDGE_NODE, type: EntityType.EDGE_BROKER },
              ],
            },
          },
          ...MOCK_DEFAULT_NODE,
          position: { x: 0, y: 0 },
        },
      ]),
    })
    // Must mimic the click on the ReactFlow node to open the drawer
    cy.getByTestId('test-reactflow-trigger-button').click()
    cy.getByTestId('data-pathname').should('have.text', '/node/idCombiner')

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
      routerProps: { initialEntries: [INITIAL_ENTRY] },
      wrapper: getWrapperWith('idCombiner', [{ ...MOCK_NODE_COMBINER, position: { x: 0, y: 0 } }]),
    })
    // Must mimic the click on the ReactFlow node to open the drawer
    cy.getByTestId('test-reactflow-trigger-button').click()
    cy.getByTestId('data-pathname').should('have.text', '/node/idCombiner')

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
