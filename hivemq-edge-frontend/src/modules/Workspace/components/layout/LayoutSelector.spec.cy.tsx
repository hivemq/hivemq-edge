/// <reference types="cypress" />

import { ReactFlowProvider } from '@xyflow/react'
import LayoutSelector from './LayoutSelector'
import { EdgeFlowProvider } from '@/modules/Workspace/hooks/EdgeFlowProvider'
import useWorkspaceStore from '@/modules/Workspace/hooks/useWorkspaceStore'
import { LayoutType } from '@/modules/Workspace/types/layout'

describe('LayoutSelector', () => {
  beforeEach(() => {
    cy.viewport(400, 300)
  })

  it('should render with all available algorithms', () => {
    const wrapper = ({ children }: { children: React.ReactNode }) => (
      <EdgeFlowProvider>
        <ReactFlowProvider>{children}</ReactFlowProvider>
      </EdgeFlowProvider>
    )

    cy.mountWithProviders(<LayoutSelector />, { wrapper })

    cy.getByTestId('workspace-layout-selector').should('be.visible')
    cy.getByTestId('workspace-layout-selector').find('option').should('have.length.gt', 3)
  })

  it('should show current algorithm as selected', () => {
    const wrapper = ({ children }: { children: React.ReactNode }) => {
      // Set algorithm before rendering
      useWorkspaceStore.getState().setLayoutAlgorithm(LayoutType.DAGRE_TB)

      return (
        <EdgeFlowProvider>
          <ReactFlowProvider>{children}</ReactFlowProvider>
        </EdgeFlowProvider>
      )
    }

    cy.mountWithProviders(<LayoutSelector />, { wrapper })

    cy.getByTestId('workspace-layout-selector').should('have.value', LayoutType.DAGRE_TB)
  })

  it('should call setAlgorithm when selection changes', () => {
    const wrapper = ({ children }: { children: React.ReactNode }) => (
      <EdgeFlowProvider>
        <ReactFlowProvider>{children}</ReactFlowProvider>
      </EdgeFlowProvider>
    )

    cy.mountWithProviders(<LayoutSelector />, { wrapper })

    // Change selection
    cy.getByTestId('workspace-layout-selector').select(LayoutType.DAGRE_LR)

    // Verify store was updated
    cy.window().then(() => {
      const state = useWorkspaceStore.getState()
      expect(state.layoutConfig.currentAlgorithm).to.equal(LayoutType.DAGRE_LR)
    })
  })

  it('should be accessible', () => {
    const wrapper = ({ children }: { children: React.ReactNode }) => (
      <EdgeFlowProvider>
        <ReactFlowProvider>{children}</ReactFlowProvider>
      </EdgeFlowProvider>
    )

    cy.injectAxe()
    cy.mountWithProviders(<LayoutSelector />, { wrapper })

    cy.checkAccessibility(undefined, {
      rules: {
        // React Flow context may have some violations
        region: { enabled: false },
      },
    })
  })
})
