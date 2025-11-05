import NetworkGraphView from '@/modules/DomainOntology/components/NetworkGraphView.tsx'

describe('NetworkGraphView (POC)', () => {
  it('should be accessible', () => {
    cy.injectAxe()
    cy.mountWithProviders(<NetworkGraphView />)
    cy.checkAccessibility()
  })
})
