import type { ReactElement } from 'react'
import { ReactFlowProvider } from '@xyflow/react'
import type { Edge, Node } from '@xyflow/react'

import { NodeTypes } from '@/modules/Workspace/types.ts'
import useWorkspaceStore from '@/modules/Workspace/hooks/useWorkspaceStore.ts'
import { EdgeFlowProvider } from '@/modules/Workspace/hooks/EdgeFlowProvider'
import ReloadWorkspaceButton from './ReloadWorkspaceButton.tsx'

const wrapper = ({ children }: { children: ReactElement }) => (
  <EdgeFlowProvider>
    <ReactFlowProvider>{children}</ReactFlowProvider>
  </EdgeFlowProvider>
)

const node = (id: string, type: NodeTypes, data: Record<string, unknown> = {}): Node => ({
  id,
  type,
  data,
  position: { x: 0, y: 0 },
})

/** Every list the graph is built from, answering empty unless a test overrides it. */
const interceptWorkspaceApis = () => {
  cy.intercept('/api/v1/frontend/capabilities', { statusCode: 200, body: { items: [] }, log: false })
  cy.intercept('/api/v1/management/protocol-adapters/adapters', { statusCode: 200, body: { items: [] }, log: false })
  cy.intercept('/api/v1/management/protocol-adapters/types', { statusCode: 200, body: { items: [] }, log: false })
  cy.intercept('/api/v1/management/bridges', { statusCode: 200, body: { items: [] }, log: false })
  cy.intercept('/api/v1/gateway/listeners', { statusCode: 200, body: { items: [] }, log: false })
  cy.intercept('/api/v1/management/combiners', { statusCode: 200, body: { items: [] }, log: false })
  cy.intercept('/api/v1/management/pulse/asset-mappers', { statusCode: 200, body: { items: [] }, log: false })
}

describe('ReloadWorkspaceButton', () => {
  beforeEach(() => {
    cy.viewport(900, 700)
    interceptWorkspaceApis()
    useWorkspaceStore.getState().reset()
  })

  it('should render the trigger', () => {
    cy.mountWithProviders(<ReloadWorkspaceButton builtNodes={[]} builtEdges={[]} />, { wrapper })

    cy.getByTestId('reload-workspace-trigger').should('be.visible').should('contain.text', 'Reload workspace')
  })

  it('should offer both choices, with the destructive one naming its cost', () => {
    cy.mountWithProviders(<ReloadWorkspaceButton builtNodes={[]} builtEdges={[]} />, { wrapper })

    cy.getByTestId('reload-workspace-dialog').should('not.exist')
    cy.getByTestId('reload-workspace-trigger').click()

    cy.getByTestId('reload-workspace-dialog').should('be.visible')
    cy.getByTestId('reload-workspace-confirm').should('contain.text', 'Reload')
    cy.getByTestId('reload-workspace-reset').should('contain.text', 'Reset everything')
    // The safe choice promises the user's arrangement survives.
    cy.getByTestId('reload-workspace-dialog').should('contain.text', 'groups, layout and panel preferences are kept')
    // The destructive one says it signs you out, so the login screen is expected.
    cy.getByTestId('reload-workspace-dialog').should('contain.text', 'you are signed out')
  })

  it('should refuse to reload while a configuration panel is open', () => {
    cy.mountWithProviders(<ReloadWorkspaceButton builtNodes={[]} builtEdges={[]} />, {
      wrapper,
      routerProps: { initialEntries: ['/workspace/adapter/opcua/my-adapter'] },
    })

    cy.getByTestId('reload-workspace-trigger').click()

    cy.getByTestId('reload-workspace-panel-warning').should('be.visible')
    cy.getByTestId('reload-workspace-confirm').should('not.exist')
    cy.getByTestId('reload-workspace-reset').should('not.exist')
  })

  it('should remove a ghost node and keep the position the user dragged to', () => {
    // The canvas holds an adapter the server still has (dragged to a custom spot) and a combiner it
    // no longer has — the exact shape of the reported bug.
    const dragged = { ...node('adapter@kept', NodeTypes.ADAPTER_NODE, { id: 'kept' }), position: { x: 321, y: 123 } }
    const ghost = node('combiner-gone', NodeTypes.COMBINER_NODE, { id: 'gone' })

    useWorkspaceStore.setState({ nodes: [dragged, ghost], edges: [] })

    // What the builder derives from the server: the adapter only, at its default position.
    const built: Node[] = [node('adapter@kept', NodeTypes.ADAPTER_NODE, { id: 'kept', name: 'fresh' })]
    const builtEdges: Edge[] = []

    cy.mountWithProviders(<ReloadWorkspaceButton builtNodes={built} builtEdges={builtEdges} />, { wrapper })

    cy.getByTestId('reload-workspace-trigger').click()
    cy.getByTestId('reload-workspace-confirm').click()

    cy.getByTestId('reload-workspace-dialog').should('not.exist')

    cy.wrap(null).should(() => {
      const { nodes } = useWorkspaceStore.getState()
      // The ghost is gone.
      expect(nodes.map((item) => item.id)).to.deep.equal(['adapter@kept'])
      // The user's position survived.
      expect(nodes[0].position).to.deep.equal({ x: 321, y: 123 })
      // The server's data landed.
      expect(nodes[0].data).to.deep.equal({ id: 'kept', name: 'fresh' })
    })
  })

  it('should change nothing when the server cannot be reached', () => {
    // One list fails; a reconcile on a partial result would delete every combiner from the workspace.
    cy.intercept('/api/v1/management/combiners', { statusCode: 500, body: {}, log: false })

    const ghost = node('combiner-gone', NodeTypes.COMBINER_NODE, { id: 'gone' })
    useWorkspaceStore.setState({ nodes: [ghost], edges: [] })

    cy.mountWithProviders(<ReloadWorkspaceButton builtNodes={[]} builtEdges={[]} />, { wrapper })

    cy.getByTestId('reload-workspace-trigger').click()
    cy.getByTestId('reload-workspace-confirm').click()

    // The reload retries with backoff before giving up, so allow for the full sequence.
    cy.get('[role="status"]', { timeout: 30000 }).should('contain.text', 'Nothing was changed')

    cy.wrap(null).should(() => {
      // Untouched, ghost and all — a wrong workspace is better than a wrongly emptied one.
      expect(useWorkspaceStore.getState().nodes.map((item) => item.id)).to.deep.equal(['combiner-gone'])
    })
  })
})
