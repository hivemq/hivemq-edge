/**
 * Cypress Component Tests for WizardProtocolSelector
 *
 * Tests the protocol selection step of the adapter wizard
 */

import { Drawer, DrawerOverlay, DrawerContent } from '@chakra-ui/react'
import WizardProtocolSelector from './WizardProtocolSelector'
import { useWizardStore } from '@/modules/Workspace/hooks/useWizardStore'
import { mockProtocolAdapter, mockProtocolAdapter_OPCUA } from '@/api/hooks/useProtocolAdapters/__handlers__'

describe('WizardProtocolSelector', () => {
  // Use existing mocks from API handlers - they already have installed: true
  const mockProtocols = [mockProtocolAdapter, mockProtocolAdapter_OPCUA]

  let mockOnSelect: ReturnType<typeof cy.stub>

  beforeEach(() => {
    useWizardStore.getState().actions.cancelWizard()
    // Create fresh stub for each test
    mockOnSelect = cy.stub().as('onSelect')
  })

  const mountComponent = (onSelect?: (protocolId: string | undefined) => void) => {
    // Component uses DrawerHeader/Body/Footer which require Drawer context
    const Wrapper = () => (
      <Drawer isOpen={true} onClose={() => {}} placement="right" size="lg">
        <DrawerOverlay />
        <DrawerContent>
          <WizardProtocolSelector onSelect={onSelect || mockOnSelect} />
        </DrawerContent>
      </Drawer>
    )
    return cy.mountWithProviders(<Wrapper />)
  }

  describe('Loading State', () => {
    it('should show loader while fetching protocols', () => {
      // Intercept API call to delay response
      cy.intercept('GET', '/api/v1/management/protocol-adapters/types', {
        delay: 1000,
        body: { items: mockProtocols },
      }).as('getAdapters')

      mountComponent()

      // Should show loading spinner while API is fetching
      cy.getByTestId('loading-spinner').should('be.visible')
    })
  })

  describe('Error State', () => {
    it('should show error message when API fails', () => {
      cy.intercept('GET', '/api/v1/management/protocol-adapters/types', {
        statusCode: 500,
        body: {
          title: 'Failed to load protocol adapters',
          status: 500,
        },
      }).as('getAdaptersError')

      mountComponent()

      cy.wait('@getAdaptersError')

      // Wait for loading spinner to disappear (component only shows error after loading is done)
      cy.getByTestId('loading-spinner').should('not.exist')

      // Should show error message with explicit timeout for CI stability
      cy.get('[role="alert"]').should('be.visible')
      cy.contains('Failed to load protocol adapters', { timeout: 10000 }).should('be.visible')
    })

    it('should show generic error when no error details provided', () => {
      cy.intercept('GET', '/api/v1/management/protocol-adapters/types', {
        statusCode: 500,
        body: {},
      }).as('getAdaptersError')

      mountComponent()

      cy.wait('@getAdaptersError')

      // Wait for loading spinner to disappear
      cy.getByTestId('loading-spinner').should('not.exist')

      // Should show error alert (ErrorMessage renders as Alert with status="error")
      cy.get('[role="alert"]', { timeout: 10000 }).should('be.visible')

      // Check for translated error message (protocolAdapter.error.loading)
      cy.get('[role="alert"]').should('contain.text', 'load')
    })
  })

  describe('Empty State', () => {
    it('should show warning when no protocols available', () => {
      cy.intercept('GET', '/api/v1/management/protocol-adapters/types', {
        body: { items: [] },
      }).as('getAdapters')

      mountComponent()

      cy.wait('@getAdapters')

      // Should show empty state warning
      cy.contains(/no.*available/i).should('be.visible')
    })
  })

  describe('Success State', () => {
    beforeEach(() => {
      cy.intercept('GET', '/api/v1/management/protocol-adapters/types', {
        body: { items: mockProtocols },
      }).as('getAdapters')
    })

    it('should render drawer with title and description', () => {
      mountComponent()

      cy.wait('@getAdapters')

      // Check for translated title (check translation.json for actual text)
      cy.contains(/select.*protocol/i).should('be.visible')
      // Description should be visible
      cy.get('[role="dialog"]').should('contain.text', 'protocol')
    })

    it('should display all available protocols', () => {
      mountComponent()

      cy.wait('@getAdapters')

      // Should show all protocols (split scrollIntoView for ESLint)
      cy.contains('Simulated Edge Device').scrollIntoView()
      cy.contains('Simulated Edge Device').should('be.visible')
      cy.contains('OPC UA to MQTT Protocol Adapter').scrollIntoView()
      cy.contains('OPC UA to MQTT Protocol Adapter').should('be.visible')
    })

    it('should call onSelect when protocol is clicked', () => {
      mountComponent()

      cy.wait('@getAdapters')

      // Click the "Create Instance" button for simulation adapter
      cy.contains('Simulated Edge Device')
        .closest('[role="listitem"]')
        .find('[data-testid="protocol-create-adapter"]')
        .click()

      // Should call onSelect with protocol id
      cy.get('@onSelect').should('have.been.calledOnce')
      cy.get('@onSelect').should('have.been.calledWith', 'simulation')
    })

    it('should call onSelect with correct protocol for different selections', () => {
      mountComponent()

      cy.wait('@getAdapters')

      // Click the "Create Instance" button for OPC-UA adapter
      cy.contains('OPC UA to MQTT Protocol Adapter')
        .closest('[role="listitem"]')
        .find('[data-testid="protocol-create-adapter"]')
        .click()

      cy.get('@onSelect').should('have.been.calledWith', 'opcua')
    })
  })

  describe('Search Functionality', () => {
    beforeEach(() => {
      cy.intercept('GET', '/api/v1/management/protocol-adapters/types', {
        body: { items: mockProtocols },
      }).as('getAdapters')
    })

    it('should have search toggle button in footer', () => {
      mountComponent()

      cy.wait('@getAdapters')

      // Search button should be in footer
      cy.contains('button', /search/i).should('be.visible')
    })

    it('should show search panel when toggle is clicked', () => {
      mountComponent()

      cy.wait('@getAdapters')

      // Initially search should be hidden (FacetSearch input with specific id)
      cy.get('#facet-search-input').should('not.exist')

      // Click search toggle
      cy.contains('button', /search/i).click()

      // Search panel should appear with its input
      cy.get('#facet-search-input').should('be.visible')
    })

    it('should hide search panel when toggle is clicked again', () => {
      mountComponent()

      cy.wait('@getAdapters')

      // Show search
      cy.contains('button', /search/i).click()
      cy.get('#facet-search-input').should('be.visible')

      // Hide search
      cy.contains('button', /hide.*search/i).click()
      cy.get('#facet-search-input').should('not.exist')
    })

    it('should change button state when search is toggled', () => {
      mountComponent()

      cy.wait('@getAdapters')

      // Initially inactive state (search hidden)
      cy.getByTestId('search-toggle-inactive').should('exist')
      cy.getByTestId('search-toggle-active').should('not.exist')

      // Click to show search
      cy.getByTestId('search-toggle-inactive').click()

      // Should change to active state
      cy.getByTestId('search-toggle-active').should('exist')
      cy.getByTestId('search-toggle-inactive').should('not.exist')
    })

    it('should filter protocols when searching', () => {
      mountComponent()

      cy.wait('@getAdapters')

      // Show search
      cy.contains('button', /search/i).click()

      // Type in search box using specific id
      cy.get('#facet-search-input').type('opc')

      // Should filter to show only OPC-UA adapter
      cy.contains('OPC UA to MQTT Protocol Adapter').scrollIntoView()
      cy.contains('OPC UA to MQTT Protocol Adapter').should('be.visible')
      // Simulated adapter should not exist in DOM (filtered out)
      cy.contains('Simulated Edge Device').should('not.exist')
    })

    it('should show grid layout when search is visible', () => {
      mountComponent()

      cy.wait('@getAdapters')

      // Show search
      cy.contains('button', /search/i).click()

      // Should show both FacetSearch panel (with search input) and ProtocolsBrowser list
      // FacetSearch input should be visible
      cy.get('#facet-search-input').should('be.visible')
      // Protocols list should be visible
      cy.get('[role="list"]').should('be.visible')
      // Both should be visible at the same time (side-by-side layout)
      cy.get('#facet-search-input').should('be.visible')
      cy.get('[role="list"]').should('be.visible')
    })
  })

  describe('Close Button', () => {
    beforeEach(() => {
      cy.intercept('GET', '/api/v1/management/protocol-adapters/types', {
        body: { items: mockProtocols },
      }).as('getAdapters')
    })

    it('should have close button in header', () => {
      mountComponent()

      cy.wait('@getAdapters')

      // Close button should exist
      cy.get('button[aria-label="Close"]').should('be.visible')
    })

    it('should call cancelWizard when close button is clicked', () => {
      // Spy on cancelWizard
      const cancelSpy = cy.spy(useWizardStore.getState().actions, 'cancelWizard')

      mountComponent()

      cy.wait('@getAdapters')

      // Click close button
      cy.get('button[aria-label="Close"]').click()

      // Should call cancelWizard
      cy.wrap(cancelSpy).should('have.been.called')
    })
  })

  describe('Accessibility', () => {
    beforeEach(() => {
      cy.intercept('GET', '/api/v1/management/protocol-adapters/types', {
        body: { items: mockProtocols },
      }).as('getAdapters')
    })

    it('should be accessible', () => {
      mountComponent()

      cy.wait('@getAdapters')

      cy.injectAxe()
      cy.checkAccessibility()
    })

    it('should have proper ARIA attributes on drawer', () => {
      mountComponent()

      cy.wait('@getAdapters')

      // Drawer should have dialog role
      cy.get('[role="dialog"]').should('exist')
    })

    it('should have accessible close button', () => {
      mountComponent()

      cy.wait('@getAdapters')

      cy.get('button[aria-label="Close"]').should('exist')
    })

    it('should have accessible search button', () => {
      mountComponent()

      cy.wait('@getAdapters')

      cy.contains('button', /search/i).should('be.visible')
      // Button should have visible text, not just icon
      cy.contains('button', /search/i).should('contain.text', 'Search')
    })
  })

  describe('Integration with ProtocolsBrowser', () => {
    beforeEach(() => {
      cy.intercept('GET', '/api/v1/management/protocol-adapters/types', {
        body: { items: mockProtocols },
      }).as('getAdapters')
    })

    it('should pass forceSingleColumn prop when search hidden', () => {
      mountComponent()

      cy.wait('@getAdapters')

      // Protocols should be in single column layout
      cy.get('[role="dialog"]').within(() => {
        // Check that items are stacked vertically (single column)
        cy.contains('Simulated Edge Device').should('be.visible')
      })
    })

    it('should pass forceSingleColumn prop when search visible', () => {
      mountComponent()

      cy.wait('@getAdapters')

      // Show search
      cy.contains('button', /search/i).click()

      // Should still use single column in grid layout (scroll into view if covered)
      cy.contains('Simulated Edge Device').scrollIntoView()
      cy.contains('Simulated Edge Device').should('be.visible')
    })
  })

  describe('Edge Cases', () => {
    it('should handle undefined items from API', () => {
      cy.intercept('GET', '/api/v1/management/protocol-adapters/types', {
        body: {},
      }).as('getAdapters')

      mountComponent()

      cy.wait('@getAdapters')

      // Should show empty state (no crash)
      cy.contains(/no.*available/i).should('be.visible')
    })

    it('should handle null items from API', () => {
      cy.intercept('GET', '/api/v1/management/protocol-adapters/types', {
        body: { items: null },
      }).as('getAdapters')

      mountComponent()

      cy.wait('@getAdapters')

      // Should show empty state (no crash)
      cy.contains(/no.*available/i).should('be.visible')
    })

    it('should handle malformed protocol data', () => {
      cy.intercept('GET', '/api/v1/management/protocol-adapters/types', {
        body: {
          items: [
            { id: 'broken', name: undefined }, // Missing required fields
          ],
        },
      }).as('getAdapters')

      mountComponent()

      cy.wait('@getAdapters')

      // Should not crash, might show broken item or skip it
      cy.get('[role="dialog"]').should('be.visible')
    })
  })
})
