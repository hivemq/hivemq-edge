import NodeDatahubToolbar from '@datahub/components/toolbar/NodeDatahubToolbar.tsx'
import { mockReactFlow } from '@/__test-utils__/react-flow/providers.tsx'

describe('NodeDatahubToolbar', () => {
  beforeEach(() => {
    cy.viewport(400, 400)
  })

  it('should render the toolbar', () => {
    const mockOnEdit = cy.stub().as('editNode')
    const mockOnDelete = cy.stub().as('deleteNode')
    const mockOnCopy = cy.stub().as('copyNode')

    cy.mountWithProviders(
      mockReactFlow(<NodeDatahubToolbar onEdit={mockOnEdit} onCopy={mockOnCopy} onDelete={mockOnDelete} />)
    )
    cy.get('@editNode').should('not.have.been.called')
    cy.get('@deleteNode').should('not.have.been.called')
    cy.get('@copyNode').should('not.have.been.called')

    cy.getByTestId('node-toolbar-edit').should('have.attr', 'aria-label', 'Edit')
    cy.getByTestId('node-toolbar-edit').click()
    cy.get('@editNode').should('have.been.called')

    cy.getByTestId('node-toolbar-copy').should('have.attr', 'aria-label', 'Copy')
    cy.getByTestId('node-toolbar-copy').click()
    cy.get('@copyNode').should('have.been.called')

    cy.getByTestId('node-toolbar-delete').should('have.attr', 'aria-label', 'Delete')
    cy.getByTestId('node-toolbar-delete').click()
    cy.get('@deleteNode').should('have.been.called')
  })

  it('should be accessible', () => {
    cy.injectAxe()
    cy.mountWithProviders(mockReactFlow(<NodeDatahubToolbar />))
    cy.checkAccessibility()
    cy.percySnapshot('Component: DataHub - NodeDatahubToolbar')
  })
})
