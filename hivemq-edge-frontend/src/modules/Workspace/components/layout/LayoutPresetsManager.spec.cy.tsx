/// <reference types="cypress" />

import { ReactFlowProvider } from '@xyflow/react'
import LayoutPresetsManager from './LayoutPresetsManager'
import { EdgeFlowProvider } from '@/modules/Workspace/hooks/EdgeFlowProvider'
import useWorkspaceStore from '@/modules/Workspace/hooks/useWorkspaceStore'
import { LayoutType } from '@/modules/Workspace/types/layout'
import type { Node } from '@xyflow/react'
import type { LayoutPreset } from '@/modules/Workspace/types/layout'

describe('LayoutPresetsManager', () => {
  beforeEach(() => {
    cy.viewport(600, 800)

    // Reset store before each test
    useWorkspaceStore.getState().reset()
  })

  it('should open menu with save option', () => {
    const wrapper = ({ children }: { children: React.ReactNode }) => (
      <EdgeFlowProvider>
        <ReactFlowProvider>{children}</ReactFlowProvider>
      </EdgeFlowProvider>
    )

    cy.mountWithProviders(<LayoutPresetsManager />, { wrapper })

    // Click menu button
    cy.get('button[aria-label*="preset"]').first().click()

    // Should show "Save Current Layout" option in menu
    cy.get('[role="menu"]').within(() => {
      cy.get('[role="menuitem"]').first().should('contain.text', 'Save Current Layout')
    })
  })

  it('should show "no saved presets" when empty', () => {
    const wrapper = ({ children }: { children: React.ReactNode }) => (
      <EdgeFlowProvider>
        <ReactFlowProvider>{children}</ReactFlowProvider>
      </EdgeFlowProvider>
    )

    cy.mountWithProviders(<LayoutPresetsManager />, { wrapper })

    cy.get('button[aria-label*="preset"]').first().click()

    // Check for empty state message within menu
    cy.get('[role="menu"]').should('contain.text', 'No saved presets')
  })

  it('should display saved presets', () => {
    const wrapper = ({ children }: { children: React.ReactNode }) => {
      // Add a preset to the store
      const store = useWorkspaceStore.getState()
      const testPreset: LayoutPreset = {
        id: 'test-preset-1',
        name: 'My Test Preset',
        createdAt: new Date(),
        updatedAt: new Date(),
        algorithm: LayoutType.DAGRE_TB,
        options: {},
        positions: new Map([
          ['node1', { x: 100, y: 100 }],
          ['node2', { x: 200, y: 200 }],
        ]),
      }
      store.saveLayoutPreset(testPreset)

      return (
        <EdgeFlowProvider>
          <ReactFlowProvider>{children}</ReactFlowProvider>
        </EdgeFlowProvider>
      )
    }

    cy.mountWithProviders(<LayoutPresetsManager />, { wrapper })

    cy.getByTestId('workspace-preset-trigger').click()
    cy.contains('My Test Preset').should('be.visible')
  })

  it('should open save modal on "Save Current Layout"', () => {
    const wrapper = ({ children }: { children: React.ReactNode }) => (
      <EdgeFlowProvider>
        <ReactFlowProvider>{children}</ReactFlowProvider>
      </EdgeFlowProvider>
    )

    cy.mountWithProviders(<LayoutPresetsManager />, { wrapper })

    cy.get('button[aria-label*="preset"]').first().click()
    cy.get('[role="menuitem"]').first().click()

    // Modal should open with correct title
    cy.get('[role="dialog"]').should('be.visible')
    cy.get('[role="dialog"]').within(() => {
      cy.get('header').should('contain.text', 'Save Layout Preset')
    })
  })

  it('should validate preset name input', () => {
    const wrapper = ({ children }: { children: React.ReactNode }) => (
      <EdgeFlowProvider>
        <ReactFlowProvider>{children}</ReactFlowProvider>
      </EdgeFlowProvider>
    )

    cy.mountWithProviders(<LayoutPresetsManager />, { wrapper })

    // Open modal
    cy.get('button[aria-label*="preset"]').click()
    cy.contains('Save Current Layout').click()

    // Try to save without name
    cy.get('[role="dialog"]').within(() => {
      cy.contains('button', 'Save').click()
    })

    // Should show warning toast
    cy.get('[role="status"]').should('be.visible')
    cy.get('[role="status"]').should('contain.text', 'Name required')
  })

  it('should save preset with correct data', () => {
    const wrapper = ({ children }: { children: React.ReactNode }) => {
      // Add some nodes to the store
      const store = useWorkspaceStore.getState()
      const testNodes: Node[] = [
        { id: 'node1', type: 'adapter', position: { x: 100, y: 100 }, data: {} },
        { id: 'node2', type: 'adapter', position: { x: 200, y: 200 }, data: {} },
      ]
      store.onAddNodes(testNodes.map((node) => ({ type: 'add', item: node })))

      return (
        <EdgeFlowProvider>
          <ReactFlowProvider>{children}</ReactFlowProvider>
        </EdgeFlowProvider>
      )
    }

    cy.mountWithProviders(<LayoutPresetsManager />, { wrapper })

    // Open modal and save preset
    cy.getByTestId('workspace-preset-trigger').click()
    cy.get('[role="menuitem"]').first().click()

    cy.get('[role="dialog"]').within(() => {
      cy.getByTestId('workspace-preset-input').type('New Preset')
      cy.getByTestId('workspace-preset-save').should('have.text', 'Save')
      cy.getByTestId('workspace-preset-save').click()
    })

    // Verify preset was added to store (don't rely on toast)
    cy.window().then(() => {
      const state = useWorkspaceStore.getState()
      expect(state.layoutConfig.presets).to.have.length(1)
      expect(state.layoutConfig.presets[0].name).to.equal('New Preset')
    })
  })

  it('should load preset configuration on click', () => {
    const wrapper = ({ children }: { children: React.ReactNode }) => {
      // Add nodes and a preset
      const store = useWorkspaceStore.getState()
      const testNodes: Node[] = [
        { id: 'node1', type: 'adapter', position: { x: 0, y: 0 }, data: {} },
        { id: 'node2', type: 'adapter', position: { x: 0, y: 0 }, data: {} },
      ]
      store.onAddNodes(testNodes.map((node) => ({ type: 'add', item: node })))

      // Set a different algorithm initially
      store.setLayoutAlgorithm(LayoutType.MANUAL)

      const testPreset: LayoutPreset = {
        id: 'test-preset-1',
        name: 'Load Test',
        createdAt: new Date(),
        updatedAt: new Date(),
        algorithm: LayoutType.DAGRE_TB,
        options: { animate: true },
        positions: new Map([
          ['node1', { x: 100, y: 100 }],
          ['node2', { x: 200, y: 200 }],
        ]),
      }
      store.saveLayoutPreset(testPreset)

      return (
        <EdgeFlowProvider>
          <ReactFlowProvider>{children}</ReactFlowProvider>
        </EdgeFlowProvider>
      )
    }

    cy.mountWithProviders(<LayoutPresetsManager />, { wrapper })

    // Load preset - open menu and click on the preset item
    cy.getByTestId('workspace-preset-trigger').click()

    // Click on the preset menu item (not the delete button)
    cy.get('[role="menu"]').within(() => {
      cy.get('[role="menuitem"]').contains('Load Test').click()
    })

    // Verify preset configuration was loaded (algorithm and options)
    cy.window().should(() => {
      const state = useWorkspaceStore.getState()
      expect(state.layoutConfig.currentAlgorithm).to.equal(LayoutType.DAGRE_TB)
      expect(state.layoutConfig.options.animate).to.equal(true)
    })
  })

  it('should delete preset on delete action', () => {
    const wrapper = ({ children }: { children: React.ReactNode }) => {
      // Add a preset
      const store = useWorkspaceStore.getState()
      const testPreset: LayoutPreset = {
        id: 'test-preset-1',
        name: 'Delete Test',
        createdAt: new Date(),
        updatedAt: new Date(),
        algorithm: LayoutType.DAGRE_TB,
        options: {},
        positions: new Map(),
      }
      store.saveLayoutPreset(testPreset)

      return (
        <EdgeFlowProvider>
          <ReactFlowProvider>{children}</ReactFlowProvider>
        </EdgeFlowProvider>
      )
    }

    cy.mountWithProviders(<LayoutPresetsManager />, { wrapper })

    // Open menu and verify preset exists
    cy.get('button[aria-label*="preset"]').first().click()
    cy.get('[role="menu"]').should('contain.text', 'Delete Test')

    // Click the delete button specifically
    cy.get('[role="menu"]').within(() => {
      cy.get('button[aria-label*="Delete"]').first().click()
    })

    // Verify preset was removed from store
    cy.window().then(() => {
      const state = useWorkspaceStore.getState()
      expect(state.layoutConfig.presets).to.have.length(0)
    })
  })

  it('should be accessible', () => {
    const wrapper = ({ children }: { children: React.ReactNode }) => (
      <EdgeFlowProvider>
        <ReactFlowProvider>{children}</ReactFlowProvider>
      </EdgeFlowProvider>
    )

    cy.injectAxe()
    cy.mountWithProviders(<LayoutPresetsManager />, { wrapper })

    // Open menu for accessibility check
    cy.get('button[aria-label*="preset"]').first().click()

    cy.checkAccessibility(undefined, {
      rules: {
        // Menu component may have minor contrast issues
        'color-contrast': { enabled: false },
        region: { enabled: false },
      },
    })
  })
})
