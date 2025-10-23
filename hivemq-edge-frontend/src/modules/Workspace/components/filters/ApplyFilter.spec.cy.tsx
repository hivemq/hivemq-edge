import { ApplyFilter } from '@/modules/Workspace/components/filters/index.ts'

describe('ApplyFilter', () => {
  beforeEach(() => {
    cy.viewport(800, 800)
  })

  it('should render properly', () => {
    cy.mountWithProviders(<ApplyFilter />)

    cy.getByTestId('filter-apply').should('have.text', 'Apply filters')
    cy.getByTestId('filter-clearAll').should('have.text', 'Clear filters')
  })

  it('should trigger actions', () => {
    const onSubmit = cy.stub().as('onSubmit')
    const onClear = cy.stub().as('onClear')
    cy.mountWithProviders(<ApplyFilter onClear={onClear} onSubmit={onSubmit} />)

    cy.get('@onSubmit').should('have.not.been.called')
    cy.get('@onClear').should('have.not.been.called')
    cy.getByTestId('filter-apply').click()
    cy.get('@onSubmit').should('have.been.called')
    cy.getByTestId('filter-clearAll').click()
    cy.get('@onClear').should('have.been.called')
  })

  it('should be accessible', () => {
    cy.injectAxe()
    cy.mountWithProviders(<ApplyFilter />)

    cy.checkAccessibility()
  })
})
