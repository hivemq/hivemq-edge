/// <reference types="cypress" />

import AdapterTypeSummary from '@/modules/ProtocolAdapters/components/adapters/AdapterTypeSummary.tsx'
import { mockProtocolAdapter } from '@/api/hooks/useProtocolAdapters/__handlers__'

describe('AdapterTypeSummary', () => {
  beforeEach(() => {
    cy.viewport(800, 900)
  })

  it('should render properly', () => {
    cy.mountWithProviders(<AdapterTypeSummary adapter={mockProtocolAdapter} />)

    cy.getByTestId('protocol-name').should('contain.text', 'Simulated Edge Device')
    cy.getByTestId('protocol-version').should('contain.text', '1.0.0')
    cy.getByTestId('protocol-type').should('contain.text', 'Simulation Server')
    cy.getByTestId('protocol-author').should('contain.text', 'HiveMQ')
    cy.getByTestId('protocol-description').should('contain.text', 'Simulates traffic from an edge device')

    cy.get('a').each(($a) => {
      const documentation = $a.text()
      expect($a, documentation).to.have.attr('href').not.contain('undefined')
    })
  })

  it('should render the in-text search highlight', () => {
    cy.mountWithProviders(<AdapterTypeSummary adapter={mockProtocolAdapter} searchQuery={'traffic from an edge'} />)

    cy.getByTestId('protocol-description').find('mark').should('contain.text', 'traffic from an edge')
  })

  it('should render the in-text search highlight', () => {
    cy.mountWithProviders(<AdapterTypeSummary adapter={mockProtocolAdapter} searchQuery={'Simulated Edge'} />)

    cy.getByTestId('protocol-name').find('mark').should('contain.text', 'Server Protocol')
  })

  it('should be accessible', () => {
    cy.injectAxe()
    cy.mountWithProviders(<AdapterTypeSummary adapter={mockProtocolAdapter} searchQuery={'traffic from an edge'} />)
    cy.checkAccessibility()
    cy.percySnapshot('Component: AdapterTypeSummary')
  })
})
