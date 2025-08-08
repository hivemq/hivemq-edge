import PulsePage from '@/modules/Pulse/PulsePage.tsx'

describe('PulsePage', () => {
  beforeEach(() => {
    cy.viewport(800, 800)
  })

  it('should render properly', () => {
    cy.intercept('/api/v1/frontend/capabilities', { items: [] })

    cy.mountWithProviders(<PulsePage />)

    cy.getByTestId('page-container-header').within(() => {
      cy.get('h1').should('have.text', 'Assets')
      cy.get('h1 + p').should(
        'have.text',
        'Manage Assets and their data mappings to stream live data from Edge to Pulse and other external infrastructure.'
      )
    })

    cy.get('h2').should(
      'have.text',
      'Pulse Assets are not yet available for HiveMQ Edge. Please contact us for more information.'
    )
  })

  it('should be accessible', () => {
    cy.intercept('/api/v1/frontend/capabilities', { items: [] })

    cy.injectAxe()

    cy.mountWithProviders(<PulsePage />)

    cy.checkAccessibility()
  })
})
