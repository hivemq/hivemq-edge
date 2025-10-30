/// <reference types="cypress" />

import { ReactFlowProvider } from '@xyflow/react'
import LayoutOptionsDrawer from './LayoutOptionsDrawer'
import { EdgeFlowProvider } from '@/modules/Workspace/hooks/EdgeFlowProvider'
import useWorkspaceStore from '@/modules/Workspace/hooks/useWorkspaceStore'
import { LayoutType } from '@/modules/Workspace/types/layout'
import type { LayoutOptions } from '@/modules/Workspace/types/layout'
import config from '@/config'

describe('LayoutOptionsDrawer', () => {
  beforeEach(() => {
    cy.viewport(1200, 800)

    // Enable the feature flag for testing
    config.features.WORKSPACE_AUTO_LAYOUT = true

    // Reset store before each test
    useWorkspaceStore.getState().reset()
  })

  it('should open and close correctly', () => {
    const onClose = cy.stub().as('onClose')
    const testOptions: LayoutOptions = { animate: true }

    const wrapper = ({ children }: { children: React.ReactNode }) => (
      <EdgeFlowProvider>
        <ReactFlowProvider>{children}</ReactFlowProvider>
      </EdgeFlowProvider>
    )

    cy.mountWithProviders(
      <LayoutOptionsDrawer isOpen={true} onClose={onClose} algorithmType={LayoutType.DAGRE_TB} options={testOptions} />,
      { wrapper }
    )

    // Drawer should be visible
    cy.get('[role="dialog"]').should('be.visible')
    cy.contains('Layout Options').should('be.visible')

    // Close button should work
    cy.get('button[aria-label="Close"]').click()
    cy.get('@onClose').should('have.been.called')
  })

  it('should show manual layout message for manual layout', () => {
    const onClose = cy.stub()
    const testOptions: LayoutOptions = {}

    const wrapper = ({ children }: { children: React.ReactNode }) => (
      <EdgeFlowProvider>
        <ReactFlowProvider>{children}</ReactFlowProvider>
      </EdgeFlowProvider>
    )

    cy.mountWithProviders(
      <LayoutOptionsDrawer isOpen={true} onClose={onClose} algorithmType={LayoutType.MANUAL} options={testOptions} />,
      { wrapper }
    )

    cy.contains('Manual layout has no configurable options').should('be.visible')

    // Should not show footer buttons for manual layout
    cy.contains('button', 'Apply Options').should('not.exist')
  })

  it('should show "no algorithm" message when null', () => {
    const onClose = cy.stub()
    const testOptions: LayoutOptions = {}

    const wrapper = ({ children }: { children: React.ReactNode }) => (
      <EdgeFlowProvider>
        <ReactFlowProvider>{children}</ReactFlowProvider>
      </EdgeFlowProvider>
    )

    cy.mountWithProviders(
      <LayoutOptionsDrawer isOpen={true} onClose={onClose} algorithmType={null} options={testOptions} />,
      { wrapper }
    )

    cy.contains('Select a layout algorithm').should('be.visible')
  })

  it('should display Dagre options for Dagre algorithms', () => {
    const onClose = cy.stub()
    const testOptions: LayoutOptions = {}

    const wrapper = ({ children }: { children: React.ReactNode }) => (
      <EdgeFlowProvider>
        <ReactFlowProvider>{children}</ReactFlowProvider>
      </EdgeFlowProvider>
    )

    cy.mountWithProviders(
      <LayoutOptionsDrawer isOpen={true} onClose={onClose} algorithmType={LayoutType.DAGRE_TB} options={testOptions} />,
      { wrapper }
    )

    // Should show form
    cy.get('form#layout-options-form').should('exist')

    // Should show Apply Options button
    cy.contains('button', 'Apply Options').should('be.visible')
  })

  it('should display Cola options for Cola algorithms', () => {
    const onClose = cy.stub()
    const testOptions: LayoutOptions = {}

    const wrapper = ({ children }: { children: React.ReactNode }) => (
      <EdgeFlowProvider>
        <ReactFlowProvider>{children}</ReactFlowProvider>
      </EdgeFlowProvider>
    )

    cy.mountWithProviders(
      <LayoutOptionsDrawer
        isOpen={true}
        onClose={onClose}
        algorithmType={LayoutType.COLA_FORCE}
        options={testOptions}
      />,
      { wrapper }
    )

    // Should show form
    cy.get('form#layout-options-form').should('exist')
    cy.contains('button', 'Apply Options').should('be.visible')
  })

  it('should display Radial Hub options for Radial Hub', () => {
    const onClose = cy.stub()
    const testOptions: LayoutOptions = {}

    const wrapper = ({ children }: { children: React.ReactNode }) => (
      <EdgeFlowProvider>
        <ReactFlowProvider>{children}</ReactFlowProvider>
      </EdgeFlowProvider>
    )

    cy.mountWithProviders(
      <LayoutOptionsDrawer
        isOpen={true}
        onClose={onClose}
        algorithmType={LayoutType.RADIAL_HUB}
        options={testOptions}
      />,
      { wrapper }
    )

    // Should show form
    cy.get('form#layout-options-form').should('exist')
    cy.contains('button', 'Apply Options').should('be.visible')
  })

  it('should close drawer on cancel', () => {
    const onClose = cy.stub().as('onClose')
    const testOptions: LayoutOptions = {}

    const wrapper = ({ children }: { children: React.ReactNode }) => (
      <EdgeFlowProvider>
        <ReactFlowProvider>{children}</ReactFlowProvider>
      </EdgeFlowProvider>
    )

    cy.mountWithProviders(
      <LayoutOptionsDrawer isOpen={true} onClose={onClose} algorithmType={LayoutType.DAGRE_TB} options={testOptions} />,
      { wrapper }
    )

    cy.contains('button', 'Cancel').click()
    cy.get('@onClose').should('have.been.called')
  })

  it('should be accessible', () => {
    const onClose = cy.stub()
    const testOptions: LayoutOptions = {}

    const wrapper = ({ children }: { children: React.ReactNode }) => (
      <EdgeFlowProvider>
        <ReactFlowProvider>{children}</ReactFlowProvider>
      </EdgeFlowProvider>
    )

    cy.injectAxe()
    cy.mountWithProviders(
      <LayoutOptionsDrawer isOpen={true} onClose={onClose} algorithmType={LayoutType.DAGRE_TB} options={testOptions} />,
      { wrapper }
    )

    cy.checkAccessibility(undefined, {
      rules: {
        // Chakra UI drawer may have color contrast issues
        'color-contrast': { enabled: false },
      },
    })
  })
})
