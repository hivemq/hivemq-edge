import { PulseAgentIcon } from '@/components/Icons/PulseAgentIcon.tsx'

describe('PulseAgentIcon', () => {
  beforeEach(() => {
    cy.viewport(100, 100)
  })

  it('should render properly ', () => {
    cy.injectAxe()
    cy.mountWithProviders(<PulseAgentIcon />)
    cy.get('svg').should('have.attr', 'aria-label', 'Pulse Agent')
    cy.checkAccessibility()
  })
})
