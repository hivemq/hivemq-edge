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
        onSubmit={cy.stub()}
        onClose={cy.stub().as('onClose')}
      />
    )
    cy.get('@onClose').should('not.have.been.called')
    cy.get('.chakra-modal__close-btn').click()
    cy.get('@onClose').should('have.been.called')
  })

  // TODO[NVL] Change in behavior? To investigate and redesign the validation tests
  it.skip('should not close the panel when clicking submit on an invalid form', () => {
    cy.mountWithProviders(
      <AdapterInstanceDrawer
        adapterType={mockProtocolAdapter.id}
        isOpen={true}
        isSubmitting={false}
        onSubmit={cy.stub().as('onSubmit')}
        onClose={cy.stub()}
      />
    )
    cy.get('button[type="submit"]').click({ force: true })
    cy.get('@onSubmit').should('have.been.called')
  })

  it('should close the panel when clicking submit on a valid form', () => {
    cy.mountWithProviders(
      <AdapterInstanceDrawer
        adapterType={mockProtocolAdapter.id}
        isNewAdapter={true}
        isOpen={true}
        isSubmitting={false}
        onSubmit={cy.stub().as('onSubmit')}
        onClose={cy.stub()}
      />
    )

    cy.get('#root_id').type('a new identifier')

    cy.get('@onSubmit').should('not.have.been.called')
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
        onSubmit={cy.stub()}
        onClose={cy.stub()}
      />
    )
    cy.checkAccessibility()
    cy.percySnapshot('Component: AdapterInstanceDrawer')
  })

  describe('Custom Templates', () => {
    it('should render expandable array items', () => {
      cy.mountWithProviders(
        <AdapterInstanceDrawer
          adapterType={mockProtocolAdapter.id}
          isNewAdapter={true}
          isOpen={true}
          isSubmitting={false}
          onSubmit={cy.stub()}
          onClose={cy.stub()}
        />
      )

      cy.get("[role='tab']").eq(1).as('subscription')
      cy.get('@subscription').should('have.text', 'Subscription')
      cy.get('@subscription').click()
      cy.get('button').contains('Add Item').click()

      cy.get('[role="listitem"]').eq(0).as('firstItem')
      cy.get('@firstItem').find('h5').should('have.text', 'Subscriptions')
      cy.get('@firstItem').find('label').eq(0).should('contain.text', 'Destination Topic')
      cy.get('@firstItem').find('button[aria-label="Collapse Item"]').click()
      cy.get('@firstItem').find('h5').should('have.text', 'subscriptions-0')
      cy.get('@firstItem').find('label').should('not.exist')
    })
  })
})
