/// <reference types="cypress" />

import ProtocolAdapters from '@/modules/ProtocolAdapters/components/panels/ProtocolAdapters.tsx'
import { mockAdapter, mockProtocolAdapter } from '@/api/hooks/useProtocolAdapters/__handlers__'
import { mockAdapterConnectionStatus } from '@/api/hooks/useConnection/__handlers__'

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
    cy.wait('@getAdapters')
    cy.wait('@getProtocols')
    cy.wait('@getStatus')

    cy.checkAccessibility(undefined, {
      rules: {
        // TODO[16486] Font too small. See https://hivemq.kanbanize.com/ctrl_board/57/cards/16486/details/
        'color-contrast': { enabled: false },
      },
    })
    cy.percySnapshot('Component: ProtocolAdapters')
  })
})
