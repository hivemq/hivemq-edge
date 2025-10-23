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
})
