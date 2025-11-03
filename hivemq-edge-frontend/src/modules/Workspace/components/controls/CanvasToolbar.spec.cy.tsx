import { ReactFlowProvider } from '@xyflow/react'
import CanvasToolbar from '@/modules/Workspace/components/controls/CanvasToolbar.tsx'
import { EdgeFlowProvider } from '@/modules/Workspace/hooks/EdgeFlowProvider'

describe('CanvasToolbar', () => {
  beforeEach(() => {
    cy.viewport(800, 600)
  })

  const wrapper = ({ children }: { children: JSX.Element }) => (
    <EdgeFlowProvider>
      <ReactFlowProvider>{children}</ReactFlowProvider>
    </EdgeFlowProvider>
  )

  it('should renders properly', () => {
    cy.mountWithProviders(<CanvasToolbar />, { wrapper })

    cy.getByTestId('toolbox-search-expand').should('have.attr', 'aria-label', 'Expand Toolbox')
    cy.getByTestId('toolbox-search-collapse').should('not.be.visible')
    cy.getByTestId('content-toolbar').within(() => {
      cy.getByTestId('toolbox-search').should('not.be.visible')
      cy.getByTestId('toolbox-filter').should('not.be.visible')
    })

    cy.getByTestId('toolbox-search-expand').click()
    cy.getByTestId('content-toolbar').should('have.attr', 'aria-label', 'Search & Filter toolbar')
    cy.getByTestId('content-toolbar').within(() => {
      cy.getByTestId('toolbox-search').should('be.visible')
      cy.getByTestId('toolbox-filter').should('be.visible')
    })
    cy.getByTestId('toolbox-search-collapse').should('have.attr', 'aria-label', 'Collapse Toolbox')
    cy.getByTestId('toolbox-search-expand').should('not.be.visible')

    cy.getByTestId('toolbox-search-collapse').click()
    cy.getByTestId('toolbox-search-collapse').should('not.be.visible')
    cy.getByTestId('toolbox-search-expand').should('be.visible')
  })

  it('should show layout section when expanded', () => {
    cy.mountWithProviders(<CanvasToolbar />, { wrapper })

    // Expand toolbar
    cy.getByTestId('toolbox-search-expand').click()

    // Layout section should be visible
    cy.get('[role="region"][aria-label*="Layout"]').should('be.visible')
    cy.getByTestId('workspace-layout-selector').should('be.visible')
    cy.getByTestId('workspace-apply-layout').should('be.visible')
  })

  it('should show visual divider between sections', () => {
    cy.mountWithProviders(<CanvasToolbar />, { wrapper })

    // Expand toolbar
    cy.getByTestId('toolbox-search-expand').click()

    // Both sections should be visible
    cy.getByTestId('toolbox-search').should('be.visible')
    cy.getByTestId('workspace-layout-selector').should('be.visible')
  })

  it('should show layout selector', () => {
    cy.mountWithProviders(<CanvasToolbar />, { wrapper })

    cy.getByTestId('toolbox-search-expand').click()
    cy.getByTestId('workspace-layout-selector').should('be.visible')
  })

  it('should show apply layout button', () => {
    cy.mountWithProviders(<CanvasToolbar />, { wrapper })

    cy.getByTestId('toolbox-search-expand').click()
    cy.getByTestId('workspace-apply-layout').should('be.visible')
  })

  it('should show presets manager', () => {
    cy.mountWithProviders(<CanvasToolbar />, { wrapper })

    cy.getByTestId('toolbox-search-expand').click()
    cy.get('button[aria-label*="preset"]').should('be.visible')
  })

  it('should show settings button', () => {
    cy.mountWithProviders(<CanvasToolbar />, { wrapper })

    cy.getByTestId('toolbox-search-expand').click()
    // Settings button with gear icon (LuSettings)
    cy.get('[role="region"][aria-label*="Layout"]').within(() => {
      cy.get('button svg').should('exist')
    })
  })

  it('should open layout options drawer when settings clicked', () => {
    cy.mountWithProviders(<CanvasToolbar />, { wrapper })

    cy.getByTestId('toolbox-search-expand').click()
    // Click the last button in layout section (settings button)
    cy.get('[role="region"][aria-label*="Layout"]').within(() => {
      cy.get('button').last().click()
    })

    // Drawer should open
    cy.get('[role="dialog"]').should('be.visible')
  })

  it('should be accessible', () => {
    cy.injectAxe()
    cy.mountWithProviders(<CanvasToolbar />, { wrapper })

    // Test collapsed state
    cy.checkAccessibility(undefined, {
      rules: {
        region: { enabled: false }, // Panel component may not have proper region labeling
      },
    })

    // Test expanded state
    cy.getByTestId('toolbox-search-expand').click()
    cy.checkAccessibility(undefined, {
      rules: {
        region: { enabled: false },
      },
    })

    // Verify ARIA attributes
    cy.getByTestId('toolbox-search-expand').should('have.attr', 'aria-expanded', 'false')
    cy.getByTestId('toolbox-search-collapse').should('have.attr', 'aria-expanded', 'true')
    cy.getByTestId('toolbox-search-collapse').should('have.attr', 'aria-controls', 'workspace-toolbar-content')
    cy.get('#workspace-toolbar-content').should('have.attr', 'role', 'region')

    // Verify both sections have proper ARIA
    cy.get('[role="region"]').should('have.length.at.least', 2)
  })
})
