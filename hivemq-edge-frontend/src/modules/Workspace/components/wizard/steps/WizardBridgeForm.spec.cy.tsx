/**
 * Cypress Component Tests for WizardBridgeForm
 * Pattern copied from WizardProtocolSelector (25/25 passing)
 */

import { Drawer, DrawerOverlay, DrawerContent } from '@chakra-ui/react'
import WizardBridgeForm from './WizardBridgeForm'

describe('WizardBridgeForm', () => {
  let mockOnSubmit: ReturnType<typeof cy.stub>
  let mockOnBack: ReturnType<typeof cy.stub>

  beforeEach(() => {
    mockOnSubmit = cy.stub().as('onSubmit')
    mockOnBack = cy.stub().as('onBack')

    // Intercept bridges list API
    cy.intercept('GET', '/api/v1/management/bridges', { items: [] }).as('getBridges')
  })

  const mountComponent = () => {
    // Component uses DrawerHeader/Body/Footer - needs Drawer wrapper
    const Wrapper = () => (
      <Drawer isOpen={true} onClose={() => {}} placement="right" size="lg">
        <DrawerOverlay />
        <DrawerContent>
          <WizardBridgeForm onSubmit={mockOnSubmit} onBack={mockOnBack} />
        </DrawerContent>
      </Drawer>
    )
    return cy.mountWithProviders(<Wrapper />)
  }

  describe('Rendering', () => {
    it('should render drawer with title', () => {
      mountComponent()

      cy.wait('@getBridges')

      // Check for translated title
      cy.contains(/configure.*bridge/i).should('be.visible')
    })

    it('should render form', () => {
      mountComponent()

      cy.wait('@getBridges')

      // Form should exist with correct id
      cy.get('#wizard-bridge-form').should('exist')
    })

    it('should render back button', () => {
      mountComponent()

      cy.wait('@getBridges')

      cy.contains('button', /back/i).should('be.visible')
    })

    it('should render submit button', () => {
      mountComponent()

      cy.wait('@getBridges')

      cy.contains('button', /submit|create/i).should('be.visible')
    })
  })

  describe('Form Interaction', () => {
    it('should call onBack when back button is clicked', () => {
      mountComponent()

      cy.wait('@getBridges')

      cy.contains('button', /back/i).click()

      cy.get('@onBack').should('have.been.calledOnce')
    })

    it('should have submit button connected to form', () => {
      mountComponent()

      cy.wait('@getBridges')

      // Submit button should have form attribute
      cy.contains('button', /submit|create/i).should('have.attr', 'form', 'wizard-bridge-form')
    })

    it('should render bridge ID field', () => {
      mountComponent()

      cy.wait('@getBridges')

      // Bridge form should have ID field (RJSF uses id attribute pattern root_fieldname)
      cy.get('#root_id').should('exist')
    })
  })

  describe('Close Button', () => {
    it('should have close button in header', () => {
      mountComponent()

      cy.wait('@getBridges')

      cy.get('button[aria-label="Close"]').should('be.visible')
    })

    it('should call onBack when close button is clicked', () => {
      mountComponent()

      cy.wait('@getBridges')

      cy.get('button[aria-label="Close"]').click()

      cy.get('@onBack').should('have.been.calledOnce')
    })
  })

  describe('Accessibility', () => {
    it('should be accessible', () => {
      mountComponent()

      cy.wait('@getBridges')

      cy.injectAxe()
      cy.checkAccessibility()
    })

    it('should have proper dialog role', () => {
      mountComponent()

      cy.wait('@getBridges')

      cy.get('[role="dialog"]').should('exist')
    })
  })
})
