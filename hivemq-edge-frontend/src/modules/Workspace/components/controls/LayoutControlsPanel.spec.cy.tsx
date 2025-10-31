/// <reference types="cypress" />

import { ReactFlowProvider } from '@xyflow/react'
import LayoutControlsPanel from './LayoutControlsPanel'
import { EdgeFlowProvider } from '@/modules/Workspace/hooks/EdgeFlowProvider'
import useWorkspaceStore from '@/modules/Workspace/hooks/useWorkspaceStore'
import config from '@/config'

describe('LayoutControlsPanel', () => {
  beforeEach(() => {
    cy.viewport(800, 600)

    // Enable the feature flag for testing
    config.features.WORKSPACE_AUTO_LAYOUT = true

    // Reset store before each test
    useWorkspaceStore.getState().reset()
  })

  it('should render all child components', () => {
    const wrapper = ({ children }: { children: React.ReactNode }) => (
      <EdgeFlowProvider>
        <ReactFlowProvider>{children}</ReactFlowProvider>
      </EdgeFlowProvider>
    )

    cy.mountWithProviders(<LayoutControlsPanel />, { wrapper })

    // Panel should be visible
    cy.getByTestId('layout-controls-panel').should('be.visible')
  })

  it('should show LayoutSelector', () => {
    const wrapper = ({ children }: { children: React.ReactNode }) => (
      <EdgeFlowProvider>
        <ReactFlowProvider>{children}</ReactFlowProvider>
      </EdgeFlowProvider>
    )

    cy.mountWithProviders(<LayoutControlsPanel />, { wrapper })

    cy.getByTestId('workspace-layout-selector').should('be.visible')
  })

  it('should show ApplyLayoutButton', () => {
    const wrapper = ({ children }: { children: React.ReactNode }) => (
      <EdgeFlowProvider>
        <ReactFlowProvider>{children}</ReactFlowProvider>
      </EdgeFlowProvider>
    )

    cy.mountWithProviders(<LayoutControlsPanel />, { wrapper })

    cy.getByTestId('workspace-apply-layout').should('be.visible')
  })

  it('should show LayoutPresetsManager', () => {
    const wrapper = ({ children }: { children: React.ReactNode }) => (
      <EdgeFlowProvider>
        <ReactFlowProvider>{children}</ReactFlowProvider>
      </EdgeFlowProvider>
    )

    cy.mountWithProviders(<LayoutControlsPanel />, { wrapper })

    cy.get('button[aria-label*="preset"]').should('be.visible')
  })

  it('should show layout options button', () => {
    const wrapper = ({ children }: { children: React.ReactNode }) => (
      <EdgeFlowProvider>
        <ReactFlowProvider>{children}</ReactFlowProvider>
      </EdgeFlowProvider>
    )

    cy.mountWithProviders(<LayoutControlsPanel />, { wrapper })

    cy.get('button[aria-label="Layout options"]').should('be.visible')
  })

  it('should open options drawer when settings button clicked', () => {
    const wrapper = ({ children }: { children: React.ReactNode }) => (
      <EdgeFlowProvider>
        <ReactFlowProvider>{children}</ReactFlowProvider>
      </EdgeFlowProvider>
    )

    cy.mountWithProviders(<LayoutControlsPanel />, { wrapper })

    cy.get('button[aria-label="Layout options"]').click()

    // Drawer should open
    cy.get('[role="dialog"]').should('be.visible')
    cy.get('[role="dialog"]').within(() => {
      cy.get('header').should('contain.text', 'Layout Options')
    })
  })

  it('should be accessible', () => {
    const wrapper = ({ children }: { children: React.ReactNode }) => (
      <EdgeFlowProvider>
        <ReactFlowProvider>{children}</ReactFlowProvider>
      </EdgeFlowProvider>
    )

    cy.injectAxe()
    cy.mountWithProviders(<LayoutControlsPanel />, { wrapper })

    cy.checkAccessibility(undefined, {
      rules: {
        region: { enabled: false },
      },
    })
  })
})
