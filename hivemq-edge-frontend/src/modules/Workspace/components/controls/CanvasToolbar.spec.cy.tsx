import { ReactFlowProvider } from '@xyflow/react'
import CanvasToolbar from '@/modules/Workspace/components/controls/CanvasToolbar.tsx'

describe('CanvasToolbar', () => {
  beforeEach(() => {
    cy.viewport(800, 250)
  })

  it('should renders properly', () => {
    cy.mountWithProviders(<CanvasToolbar />, {
      wrapper: ({ children }: { children: JSX.Element }) => <ReactFlowProvider>{children}</ReactFlowProvider>,
    })

    cy.getByTestId('content-toolbar').should('have.attr', 'aria-label', 'Search & Filter toolbar')
    cy.getByTestId('content-toolbar').within(() => {
      cy.getByTestId('toolbox-search').should('be.visible')
      cy.getByTestId('toolbox-filter').should('be.visible')
    })
  })
})
