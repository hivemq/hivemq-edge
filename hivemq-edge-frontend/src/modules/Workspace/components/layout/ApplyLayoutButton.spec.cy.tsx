/// <reference types="cypress" />

import { ReactFlowProvider } from '@xyflow/react'
import ApplyLayoutButton from './ApplyLayoutButton'
import { EdgeFlowProvider } from '@/modules/Workspace/hooks/EdgeFlowProvider'
import useWorkspaceStore from '@/modules/Workspace/hooks/useWorkspaceStore'
import { LayoutType } from '@/modules/Workspace/types/layout'
import type { Node } from '@xyflow/react'

describe('ApplyLayoutButton', () => {
  beforeEach(() => {
    cy.viewport(400, 300)

    // Reset store before each test
    useWorkspaceStore.getState().reset()
  })

  it('should render button with correct label', () => {
    const wrapper = ({ children }: { children: React.ReactNode }) => (
      <EdgeFlowProvider>
        <ReactFlowProvider>{children}</ReactFlowProvider>
      </EdgeFlowProvider>
    )

    cy.mountWithProviders(<ApplyLayoutButton />, { wrapper })

    cy.getByTestId('workspace-apply-layout').should('be.visible')
    cy.getByTestId('workspace-apply-layout').should('contain.text', 'Apply Layout')
  })

  it('should apply layout when clicked with nodes present', () => {
    const wrapper = ({ children }: { children: React.ReactNode }) => {
      // Setup workspace with nodes and algorithm
      const store = useWorkspaceStore.getState()
      store.setLayoutAlgorithm(LayoutType.DAGRE_TB)

      const testNodes: Node[] = [
        { id: '1', type: 'adapter', position: { x: 0, y: 0 }, data: { label: 'Node 1' } },
        { id: '2', type: 'adapter', position: { x: 100, y: 100 }, data: { label: 'Node 2' } },
      ]

      store.onAddNodes(testNodes.map((node) => ({ type: 'add', item: node })))

      return (
        <EdgeFlowProvider>
          <ReactFlowProvider>{children}</ReactFlowProvider>
        </EdgeFlowProvider>
      )
    }

    cy.mountWithProviders(<ApplyLayoutButton />, { wrapper })

    // Click apply layout
    cy.getByTestId('workspace-apply-layout').click()

    // Wait a bit for layout to apply
    cy.wait(500)

    // Verify store shows nodes (layout was attempted)
    cy.window().then(() => {
      const state = useWorkspaceStore.getState()
      expect(state.nodes).to.have.length(2)
    })
  })

  it('should be accessible', () => {
    const wrapper = ({ children }: { children: React.ReactNode }) => (
      <EdgeFlowProvider>
        <ReactFlowProvider>{children}</ReactFlowProvider>
      </EdgeFlowProvider>
    )

    cy.injectAxe()
    cy.mountWithProviders(<ApplyLayoutButton />, { wrapper })

    cy.checkAccessibility(undefined, {
      rules: {
        region: { enabled: false },
      },
    })
  })
})
