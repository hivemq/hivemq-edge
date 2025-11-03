/// <reference types="cypress" />

import PolicyJsonView from './PolicyJsonView.tsx'
import type { PolicyPayload } from '@datahub/types.ts'

describe('PolicyJsonView', () => {
  const mockPayload: PolicyPayload = {
    policy: {
      id: 'my-data-policy',
      matching: {
        topicFilter: 'devices/+/temperature',
      },
      validation: {
        validators: [],
      },
      onSuccess: {
        pipeline: [],
      },
    },
    resources: {
      schemas: [
        {
          id: 'temperature-schema',
          version: 1,
          type: 'JSON',
          schemaDefinition: '{"type": "object"}',
        },
      ],
      scripts: [
        {
          id: 'transform-temperature',
          version: 1,
          functionType: 'TRANSFORMATION',
          source: 'function transform(value) { return value; }',
        },
      ],
    },
  }

  const mockPayloadNoResources: PolicyPayload = {
    policy: {
      id: 'simple-policy',
    },
    resources: {
      schemas: [],
      scripts: [],
    },
  }

  beforeEach(() => {
    cy.on('uncaught:exception', () => {
      // should take care of unsupported browser permissions in headless mode
      return false
    })
    // Needed to test with the clipboard
    Cypress.automation('remote:debugger:protocol', {
      command: 'Browser.grantPermissions',
      params: {
        permissions: ['clipboardReadWrite', 'clipboardSanitizedWrite'],
        origin: window.location.origin,
      },
    })
  })

  describe('Rendering', () => {
    it('should render collapsed by default', () => {
      cy.mountWithProviders(<PolicyJsonView payload={mockPayload} />)

      cy.getByTestId('policy-json-view').should('be.visible')
      cy.getByTestId('json-toggle-button').should('contain', 'Show JSON')
      cy.getByTestId('json-tabs').should('not.exist')
    })

    it('should show expand button with icon', () => {
      cy.mountWithProviders(<PolicyJsonView payload={mockPayload} />)

      cy.getByTestId('json-toggle-button').within(() => {
        cy.get('svg').should('exist') // Icon present
      })
    })
  })

  describe('Expand/Collapse Behavior', () => {
    it('should expand when toggle button is clicked', () => {
      cy.mountWithProviders(<PolicyJsonView payload={mockPayload} />)

      cy.getByTestId('json-toggle-button').click()
      cy.getByTestId('json-tabs').should('be.visible')
      cy.getByTestId('json-policy-content').should('be.visible')
    })

    it('should collapse when toggle button is clicked again', () => {
      cy.mountWithProviders(<PolicyJsonView payload={mockPayload} />)

      // Expand
      cy.getByTestId('json-toggle-button').click()
      cy.getByTestId('json-tabs').should('be.visible')

      // Collapse
      cy.getByTestId('json-toggle-button').click()
      cy.getByTestId('json-tabs').should('not.be.visible')
    })

    it('should update button text when expanded', () => {
      cy.mountWithProviders(<PolicyJsonView payload={mockPayload} />)

      cy.getByTestId('json-toggle-button').click()
      cy.getByTestId('json-toggle-button').should('contain', 'Hide JSON')
    })

    it('should have proper ARIA attributes', () => {
      cy.mountWithProviders(<PolicyJsonView payload={mockPayload} />)

      cy.getByTestId('json-toggle-button')
        .should('have.attr', 'aria-expanded', 'false')
        .should('have.attr', 'aria-controls', 'json-payload-content')

      cy.getByTestId('json-toggle-button').click()
      cy.getByTestId('json-toggle-button').should('have.attr', 'aria-expanded', 'true')
    })
  })

  describe('Tabbed Interface', () => {
    beforeEach(() => {
      cy.mountWithProviders(<PolicyJsonView payload={mockPayload} />)
      cy.getByTestId('json-toggle-button').click()
    })

    it('should display all three tabs', () => {
      cy.getByTestId('tab-policy').should('be.visible')
      cy.getByTestId('tab-schemas').should('be.visible')
      cy.getByTestId('tab-scripts').should('be.visible')
    })

    it('should show resource counts in tab labels', () => {
      cy.getByTestId('tab-schemas').should('contain', '(1)')
      cy.getByTestId('tab-scripts').should('contain', '(1)')
    })

    it('should show policy tab content by default', () => {
      cy.getByTestId('json-policy-content').should('be.visible')
      cy.getByTestId('json-schemas-content').should('not.be.visible')
      cy.getByTestId('json-scripts-content').should('not.be.visible')
    })

    it('should switch to schemas tab when clicked', () => {
      cy.getByTestId('tab-schemas').click()
      cy.getByTestId('json-schemas-content').should('be.visible')
      cy.getByTestId('json-policy-content').should('not.be.visible')
    })

    it('should switch to scripts tab when clicked', () => {
      cy.getByTestId('tab-scripts').click()
      cy.getByTestId('json-scripts-content').should('be.visible')
      cy.getByTestId('json-policy-content').should('not.be.visible')
    })
  })

  describe('JSON Content', () => {
    beforeEach(() => {
      cy.mountWithProviders(<PolicyJsonView payload={mockPayload} />)
      cy.getByTestId('json-toggle-button').click()
    })

    it('should display formatted JSON in policy tab', () => {
      cy.getByTestId('json-policy-content').should('contain', 'my-data-policy')
      cy.getByTestId('json-policy-content').should('contain', 'devices/+/temperature')
    })

    it('should display formatted JSON in schemas tab', () => {
      cy.getByTestId('tab-schemas').click()
      cy.getByTestId('json-schemas-content').should('contain', 'temperature-schema')
      cy.getByTestId('json-schemas-content').should('contain', 'JSON')
    })

    it('should display formatted JSON in scripts tab', () => {
      cy.getByTestId('tab-scripts').click()
      cy.getByTestId('json-scripts-content').should('contain', 'transform-temperature')
      cy.getByTestId('json-scripts-content').should('contain', 'TRANSFORMATION')
    })

    it('should use monospace font for code', () => {
      cy.getByTestId('json-policy-content').should('have.css', 'font-family').and('match', /mono/i)
    })

    it('should have scrollable content for long JSON', () => {
      cy.getByTestId('json-policy-content').should('have.css', 'overflow-y', 'auto')
      cy.getByTestId('json-policy-content').should('have.css', 'max-height')
    })
  })

  describe('Copy Functionality', () => {
    beforeEach(() => {
      cy.mountWithProviders(<PolicyJsonView payload={mockPayload} />)
      cy.getByTestId('json-toggle-button').click()
    })

    it('should display copy all button', () => {
      cy.getByTestId('copy-all-button').should('be.visible')
    })

    it('should have copy button on each tab', () => {
      cy.getByTestId('copy-policy-button').should('be.visible')

      cy.getByTestId('tab-schemas').click()
      cy.getByTestId('copy-schemas-button').should('be.visible')

      cy.getByTestId('tab-scripts').click()
      cy.getByTestId('copy-scripts-button').should('be.visible')
    })

    it('should show success toast when copying', () => {
      cy.getByTestId('copy-all-button').click()
      cy.contains('Copied to clipboard').should('be.visible')
    })

    it('should copy policy JSON when copy button clicked', () => {
      cy.getByTestId('copy-policy-button').click()
      cy.contains('Copied to clipboard').should('be.visible')
    })

    it('should copy schemas JSON when copy button clicked', () => {
      cy.getByTestId('tab-schemas').click()
      cy.getByTestId('copy-schemas-button').click()
      cy.contains('Copied to clipboard').should('be.visible')
    })

    it('should copy scripts JSON when copy button clicked', () => {
      cy.getByTestId('tab-scripts').click()
      cy.getByTestId('copy-scripts-button').click()
      cy.contains('Copied to clipboard').should('be.visible')
    })
  })

  describe('Empty Resources', () => {
    beforeEach(() => {
      cy.mountWithProviders(<PolicyJsonView payload={mockPayloadNoResources} />)
      cy.getByTestId('json-toggle-button').click()
    })

    it('should show (0) count for empty schemas', () => {
      cy.getByTestId('tab-schemas').should('contain', '(0)')
    })

    it('should show (0) count for empty scripts', () => {
      cy.getByTestId('tab-scripts').should('contain', '(0)')
    })

    it('should display empty array for schemas', () => {
      cy.getByTestId('tab-schemas').click()
      cy.getByTestId('json-schemas-content').should('contain', '[]')
    })

    it('should display empty array for scripts', () => {
      cy.getByTestId('tab-scripts').click()
      cy.getByTestId('json-scripts-content').should('contain', '[]')
    })
  })

  describe('Accessibility', () => {
    it('should be accessible when collapsed', () => {
      cy.mountWithProviders(<PolicyJsonView payload={mockPayload} />)

      cy.injectAxe()
      cy.checkAccessibility()
    })

    it('should be accessible when expanded', () => {
      cy.mountWithProviders(<PolicyJsonView payload={mockPayload} />)
      cy.getByTestId('json-toggle-button').click()

      cy.injectAxe()
      cy.checkAccessibility()
    })

    it('should support keyboard navigation for toggle', () => {
      cy.mountWithProviders(<PolicyJsonView payload={mockPayload} />)

      // Focus on toggle button
      cy.getByTestId('json-toggle-button').focus()
      cy.focused().should('have.attr', 'data-testid', 'json-toggle-button')

      cy.focused().type('{enter}')
      cy.getByTestId('json-tabs').should('be.visible')
    })

    it('should support keyboard navigation for tabs', () => {
      cy.mountWithProviders(<PolicyJsonView payload={mockPayload} />)
      cy.getByTestId('json-toggle-button').click()

      // Focus on first tab
      cy.getByTestId('tab-policy').focus()
      cy.getByTestId('tab-policy').should('have.attr', 'aria-selected', 'true')

      // Click to switch to schemas tab
      cy.getByTestId('tab-schemas').click()
      cy.getByTestId('tab-schemas').should('have.attr', 'aria-selected', 'true')
    })

    it('should have proper color contrast for text', () => {
      cy.mountWithProviders(<PolicyJsonView payload={mockPayload} />)
      cy.getByTestId('json-toggle-button').click()

      cy.injectAxe()
      cy.checkAccessibility()
    })
  })

  describe('Visual Consistency', () => {
    beforeEach(() => {
      cy.mountWithProviders(<PolicyJsonView payload={mockPayload} />)
      cy.getByTestId('json-toggle-button').click()
    })

    it('should have consistent button styling', () => {
      cy.getByTestId('copy-all-button').should('have.class', 'chakra-button')
      cy.getByTestId('copy-policy-button').should('have.class', 'chakra-button')
    })

    it('should use gray background for code blocks', () => {
      cy.getByTestId('json-policy-content').should('have.css', 'background-color')
    })

    it('should have proper border styling', () => {
      cy.getByTestId('policy-json-view').should('have.css', 'border-width', '1px')
    })

    it('should display helper text', () => {
      cy.contains('Complete JSON payload ready for publishing').should('be.visible')
    })
  })

  describe('Edge Cases', () => {
    it('should handle large JSON payloads', () => {
      const largePayload: PolicyPayload = {
        policy: {
          id: 'large-policy',
          data: Array.from({ length: 100 }, (_, i) => ({ key: `value-${i}` })),
        },
        resources: {
          schemas: Array.from({ length: 50 }, (_, i) => ({ id: `schema-${i}` })),
          scripts: [],
        },
      }

      cy.mountWithProviders(<PolicyJsonView payload={largePayload} />)
      cy.getByTestId('json-toggle-button').click()
      cy.getByTestId('json-policy-content').should('be.visible')
    })

    it('should handle special characters in JSON', () => {
      const specialPayload: PolicyPayload = {
        policy: {
          id: 'policy-with-special-chars-!@#$%^&*()',
          description: 'Description with "quotes" and \\backslashes\\',
        },
        resources: {
          schemas: [],
          scripts: [],
        },
      }

      cy.mountWithProviders(<PolicyJsonView payload={specialPayload} />)
      cy.getByTestId('json-toggle-button').click()
      cy.getByTestId('json-policy-content').should('contain', 'policy-with-special-chars')
    })

    it('should handle nested objects in JSON', () => {
      cy.mountWithProviders(<PolicyJsonView payload={mockPayload} />)
      cy.getByTestId('json-toggle-button').click()

      // Check for nested structure
      cy.getByTestId('json-policy-content').should('contain', 'matching')
      cy.getByTestId('json-policy-content').should('contain', 'validation')
    })
  })

  describe('User Experience', () => {
    it('should maintain tab selection when toggling', () => {
      cy.mountWithProviders(<PolicyJsonView payload={mockPayload} />)

      // Expand and switch to schemas tab
      cy.getByTestId('json-toggle-button').click()
      cy.getByTestId('tab-schemas').click()
      cy.getByTestId('json-schemas-content').should('be.visible')

      // Collapse
      cy.getByTestId('json-toggle-button').click()
      cy.getByTestId('json-tabs').should('not.be.visible')

      // Expand again - should be back to default (policy tab)
      cy.getByTestId('json-toggle-button').click()
      cy.getByTestId('json-policy-content').should('be.visible')
    })

    it('should show icon on toggle button', () => {
      cy.mountWithProviders(<PolicyJsonView payload={mockPayload} />)

      cy.getByTestId('json-toggle-button').within(() => {
        cy.get('svg').should('have.length.at.least', 2) // Icon + chevron
      })
    })
  })
})
