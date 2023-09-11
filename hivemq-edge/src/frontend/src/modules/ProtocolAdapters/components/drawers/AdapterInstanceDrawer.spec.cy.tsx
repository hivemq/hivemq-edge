/// <reference types="cypress" />

import { mockAdapter, mockProtocolAdapter } from '@/api/hooks/useProtocolAdapters/__handlers__'

import AdapterInstanceDrawer from './AdapterInstanceDrawer.tsx'

describe('AdapterInstanceDrawer', () => {
  beforeEach(() => {
    cy.viewport(800, 900)
    cy.intercept('/api/v1/management/protocol-adapters/types', { items: [mockProtocolAdapter] })
    cy.intercept('api/v1/management/protocol-adapters/adapters', { items: [mockAdapter] })
  })

  it('should close panel on clicking close icon', () => {
    cy.mountWithProviders(
      <AdapterInstanceDrawer
        adapterType={mockProtocolAdapter.id}
        isOpen={true}
        isSubmitting={false}
        onSubmit={cy.stub().as('onSubmit')}
        onClose={cy.stub().as('onClose')}
      />
    )
    cy.get('.chakra-modal__close-btn').click()
    cy.get('@onClose').should('have.been.called')
  })

  it('should not close the panel when clicking submit on an invalid form', () => {
    cy.mountWithProviders(
      <AdapterInstanceDrawer
        adapterType={mockProtocolAdapter.id}
        isOpen={true}
        isSubmitting={false}
        onSubmit={cy.stub().as('onSubmit')}
        onClose={cy.stub().as('onClose')}
      />
    )
    cy.get('button[type="submit"]').click()
    cy.get('@onSubmit').should('not.have.been.called')
  })

  it('should close the panel when clicking submit on a valid form', () => {
    cy.mountWithProviders(
      <AdapterInstanceDrawer
        adapterType={mockProtocolAdapter.id}
        isNewAdapter={true}
        isOpen={true}
        isSubmitting={false}
        onSubmit={cy.stub().as('onSubmit')}
        onClose={cy.stub().as('onClose')}
      />
    )

    cy.get('#root_id').type('a new identifier')

    cy.get('button[type="submit"]').click()
    cy.get('@onSubmit').should('have.been.called')
  })

  it('should be accessible', () => {
    cy.injectAxe()
    cy.mountWithProviders(
      <AdapterInstanceDrawer
        adapterType={mockProtocolAdapter.id}
        isNewAdapter={true}
        isOpen={true}
        isSubmitting={false}
        onSubmit={cy.stub().as('onSubmit')}
        onClose={cy.stub().as('onClose')}
      />
    )
    cy.checkAccessibility()
    cy.percySnapshot('Component: AdapterInstanceDrawer')
  })
})
