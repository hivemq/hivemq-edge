/**
 * Cypress Component Tests for WizardAdapterForm
 * Pattern from WizardProtocolSelector & WizardBridgeForm (both 100% passing)
 */

import { Drawer, DrawerOverlay, DrawerContent } from '@chakra-ui/react'
import WizardAdapterForm from './WizardAdapterForm'
import { mockProtocolAdapter } from '@/api/hooks/useProtocolAdapters/__handlers__'

describe('WizardAdapterForm', () => {
  let mockOnSubmit: ReturnType<typeof cy.stub>
  let mockOnBack: ReturnType<typeof cy.stub>

  beforeEach(() => {
    mockOnSubmit = cy.stub().as('onSubmit')
    mockOnBack = cy.stub().as('onBack')

    // Intercept APIs
    cy.intercept('GET', '/api/v1/management/protocol-adapters/types', { items: [mockProtocolAdapter] }).as(
      'getAdapterTypes'
    )
    cy.intercept('GET', '/api/v1/management/protocol-adapters/adapters', { items: [] }).as('getAdapters')
  })

  const mountComponent = (protocolId = 'simulation') => {
    const Wrapper = () => (
      <Drawer isOpen={true} onClose={() => {}} placement="right" size="lg">
        <DrawerOverlay />
        <DrawerContent>
          <WizardAdapterForm protocolId={protocolId} onSubmit={mockOnSubmit} onBack={mockOnBack} />
        </DrawerContent>
      </Drawer>
    )
    return cy.mountWithProviders(<Wrapper />)
  }

  describe('Rendering', () => {
    it('should render drawer with protocol name', () => {
      mountComponent()

      cy.wait('@getAdapterTypes')
      cy.wait('@getAdapters')

      // Should show adapter name/title
      cy.contains(/simulated/i).should('be.visible')
    })

    it('should render form', () => {
      mountComponent()

      cy.wait('@getAdapterTypes')
      cy.wait('@getAdapters')

      cy.get('#wizard-adapter-form').should('exist')
    })

    it('should render back button', () => {
      mountComponent()

      cy.wait('@getAdapterTypes')
      cy.wait('@getAdapters')

      cy.contains('button', /back/i).should('be.visible')
    })

    it('should render submit button', () => {
      mountComponent()

      cy.wait('@getAdapterTypes')
      cy.wait('@getAdapters')

      cy.contains('button', /submit|create/i).should('be.visible')
    })
  })

  describe('Loading State', () => {
    it('should show loader while fetching protocol types', () => {
      cy.intercept('GET', '/api/v1/management/protocol-adapters/types', {
        delay: 1000,
        body: { items: [mockProtocolAdapter] },
      }).as('getAdapterTypesDelayed')

      mountComponent()

      cy.getByTestId('loading-spinner').should('be.visible')
    })
  })

  describe('Form Interaction', () => {
    it('should call onBack when back button is clicked', () => {
      mountComponent()

      cy.wait('@getAdapterTypes')
      cy.wait('@getAdapters')

      cy.contains('button', /back/i).click()

      cy.get('@onBack').should('have.been.calledOnce')
    })

    it('should have submit button connected to form', () => {
      mountComponent()

      cy.wait('@getAdapterTypes')
      cy.wait('@getAdapters')

      cy.contains('button', /submit|create/i).should('have.attr', 'form', 'wizard-adapter-form')
    })

    it('should render adapter ID field', () => {
      mountComponent()

      cy.wait('@getAdapterTypes')
      cy.wait('@getAdapters')

      // RJSF uses #root_id pattern
      cy.get('#root_id').should('exist')
    })
  })

  describe('Close Button', () => {
    it('should have close button in header', () => {
      mountComponent()

      cy.wait('@getAdapterTypes')
      cy.wait('@getAdapters')

      cy.get('button[aria-label="Close"]').should('be.visible')
    })

    it('should call onBack when close button is clicked', () => {
      mountComponent()

      cy.wait('@getAdapterTypes')
      cy.wait('@getAdapters')

      cy.get('button[aria-label="Close"]').click()

      cy.get('@onBack').should('have.been.calledOnce')
    })
  })

  describe('Accessibility', () => {
    it('should be accessible', () => {
      mountComponent()

      cy.wait('@getAdapterTypes')
      cy.wait('@getAdapters')

      cy.injectAxe()
      cy.checkAccessibility()
    })

    it('should have proper dialog role', () => {
      mountComponent()

      cy.wait('@getAdapterTypes')
      cy.wait('@getAdapters')

      cy.get('[role="dialog"]').should('exist')
    })
  })

  describe('Edge Cases', () => {
    it('should handle undefined protocolId', () => {
      mountComponent(undefined)

      cy.wait('@getAdapterTypes')
      cy.wait('@getAdapters')

      // Should show loader or error state
      cy.get('[role="dialog"]').should('be.visible')
    })

    it('should handle unknown protocolId', () => {
      mountComponent('unknown-protocol')

      cy.wait('@getAdapterTypes')
      cy.wait('@getAdapters')

      // Should handle gracefully
      cy.get('[role="dialog"]').should('be.visible')
    })
  })
})
