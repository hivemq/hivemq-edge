/// <reference types="cypress" />

import ProtocolAdapters from '@/modules/ProtocolAdapters/components/panels/ProtocolAdapters.tsx'
import { mockAdapter, mockProtocolAdapter } from '@/api/hooks/useProtocolAdapters/__handlers__'
import { mockAdapterConnectionStatus } from '@/api/hooks/useConnection/__handlers__'
import { MOCK_ADAPTER_ID } from '@/__test-utils__/mocks.ts'

describe('ProtocolAdapters', () => {
  beforeEach(() => {
    cy.viewport(800, 900)
    cy.intercept('/api/v1/management/protocol-adapters/types', { items: [mockProtocolAdapter] }).as('getProtocols')
    cy.intercept('api/v1/management/protocol-adapters/adapters', { items: [mockAdapter] }).as('getAdapters')
    cy.intercept('api/v1/management/protocol-adapters/status', { items: [mockAdapterConnectionStatus] }).as('getStatus')
  })

  it('should be accessible', () => {
    cy.injectAxe()
    cy.mountWithProviders(<ProtocolAdapters />)

    cy.wait('@getAdapters').then(() => {
      cy.get('tbody').find('tr').should('have.length', 1)
      cy.get('tbody').find('tr').find('td').eq(0).should('contain.text', MOCK_ADAPTER_ID)
      cy.checkAccessibility()
      cy.percySnapshot('Component: ProtocolAdapters')
    })
  })
})
