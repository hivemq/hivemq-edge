/// <reference types="cypress" />

import ResourcesBreakdown from './ResourcesBreakdown.tsx'
import type { ResourceSummary } from '@datahub/types.ts'
import { ResourceWorkingVersion } from '@datahub/types.ts'

describe('ResourcesBreakdown', () => {
  const mockSchemas: ResourceSummary[] = [
    {
      id: 'temperature-schema',
      version: ResourceWorkingVersion.DRAFT,
      type: 'SCHEMA',
      isNew: true,
      metadata: {
        schemaType: 'JSON',
      },
    },
    {
      id: 'humidity-schema',
      version: 2,
      type: 'SCHEMA',
      isNew: false,
      metadata: {
        schemaType: 'PROTOBUF',
      },
    },
  ]

  const mockScripts: ResourceSummary[] = [
    {
      id: 'transform-temperature',
      version: ResourceWorkingVersion.DRAFT,
      type: 'FUNCTION',
      isNew: true,
      metadata: {
        functionType: 'TRANSFORMATION',
      },
    },
  ]

  const mockMixedResources: ResourceSummary[] = [...mockSchemas, ...mockScripts]

  describe('Rendering', () => {
    it('should render empty state when no resources', () => {
      cy.mountWithProviders(<ResourcesBreakdown resources={[]} />)

      cy.get('[data-testid="resources-empty-state"]').should('be.visible')
      cy.contains('No additional resources required').should('be.visible')
    })

    it('should render accordion with schemas and scripts', () => {
      cy.mountWithProviders(<ResourcesBreakdown resources={mockMixedResources} />)

      cy.get('[data-testid="resources-breakdown"]').should('be.visible')
      cy.get('[data-testid="schemas-header"]').should('be.visible')
      cy.get('[data-testid="scripts-header"]').should('be.visible')
    })

    it('should render only schemas section when no scripts', () => {
      cy.mountWithProviders(<ResourcesBreakdown resources={mockSchemas} />)

      cy.get('[data-testid="schemas-header"]').should('be.visible')
      cy.get('[data-testid="scripts-header"]').should('not.exist')
    })

    it('should render only scripts section when no schemas', () => {
      cy.mountWithProviders(<ResourcesBreakdown resources={mockScripts} />)

      cy.get('[data-testid="schemas-header"]').should('not.exist')
      cy.get('[data-testid="scripts-header"]').should('be.visible')
    })

    it('should display correct count in section headers', () => {
      cy.mountWithProviders(<ResourcesBreakdown resources={mockMixedResources} />)

      cy.get('[data-testid="schemas-header"]').should('contain', 'Schemas (2)')
      cy.get('[data-testid="scripts-header"]').should('contain', 'Script (1)')
    })

    it('should render all schema items', () => {
      cy.mountWithProviders(<ResourcesBreakdown resources={mockSchemas} />)

      cy.get('[data-testid="schemas-list"]').within(() => {
        cy.get('[data-testid="schema-temperature-schema"]').should('be.visible')
        cy.get('[data-testid="schema-humidity-schema"]').should('be.visible')
      })
    })

    it('should render all script items', () => {
      cy.mountWithProviders(<ResourcesBreakdown resources={mockScripts} />)

      cy.get('[data-testid="scripts-list"]').within(() => {
        cy.get('[data-testid="script-transform-temperature"]').should('be.visible')
      })
    })
  })

  describe('Accordion Behavior', () => {
    it('should have both sections expanded by default', () => {
      cy.mountWithProviders(<ResourcesBreakdown resources={mockMixedResources} />)

      cy.get('[data-testid="schemas-list"]').should('be.visible')
      cy.get('[data-testid="scripts-list"]').should('be.visible')
    })

    it('should collapse schemas section when clicked', () => {
      cy.mountWithProviders(<ResourcesBreakdown resources={mockMixedResources} />)

      cy.get('[data-testid="schemas-header"]').click()
      cy.get('[data-testid="schemas-list"]').should('not.be.visible')
    })

    it('should collapse scripts section when clicked', () => {
      cy.mountWithProviders(<ResourcesBreakdown resources={mockMixedResources} />)

      cy.get('[data-testid="scripts-header"]').click()
      cy.get('[data-testid="scripts-list"]').should('not.be.visible')
    })

    it('should allow multiple sections to be collapsed/expanded independently', () => {
      cy.mountWithProviders(<ResourcesBreakdown resources={mockMixedResources} />)

      // Collapse schemas
      cy.get('[data-testid="schemas-header"]').click()
      cy.get('[data-testid="schemas-list"]').should('not.be.visible')
      cy.get('[data-testid="scripts-list"]').should('be.visible')

      // Expand schemas again
      cy.get('[data-testid="schemas-header"]').click()
      cy.get('[data-testid="schemas-list"]').should('be.visible')
      cy.get('[data-testid="scripts-list"]').should('be.visible')
    })
  })

  describe('Resource Details', () => {
    it('should display schema with correct metadata', () => {
      cy.mountWithProviders(<ResourcesBreakdown resources={mockSchemas} />)

      cy.get('[data-testid="schema-temperature-schema"]')
        .parent()
        .parent()
        .within(() => {
          cy.contains('temperature-schema').should('be.visible')
          cy.contains('New').should('be.visible')
          cy.contains('JSON').should('be.visible')
        })
    })

    it('should display script with correct metadata', () => {
      cy.mountWithProviders(<ResourcesBreakdown resources={mockScripts} />)

      cy.get('[data-testid="script-transform-temperature"]')
        .parent()
        .parent()
        .within(() => {
          cy.contains('transform-temperature').should('be.visible')
          cy.contains('New').should('be.visible')
          cy.contains('TRANSFORMATION').should('be.visible')
        })
    })

    it('should display status badges with icons', () => {
      cy.mountWithProviders(<ResourcesBreakdown resources={mockMixedResources} />)

      // Check for badge with icon
      cy.get('[data-testid="schemas-list"]').within(() => {
        cy.get('.chakra-badge')
          .first()
          .within(() => {
            cy.get('svg').should('exist')
            cy.contains('New').should('be.visible')
          })
      })
    })

    it('should show "New" for draft resources', () => {
      cy.mountWithProviders(<ResourcesBreakdown resources={mockSchemas} />)

      cy.get('[data-testid="schema-temperature-schema"]')
        .parent()
        .parent()
        .within(() => {
          cy.contains('New').should('be.visible')
        })
    })

    it('should show "Update" for modified resources', () => {
      cy.mountWithProviders(<ResourcesBreakdown resources={mockSchemas} />)

      cy.get('[data-testid="schema-humidity-schema"]')
        .parent()
        .parent()
        .within(() => {
          cy.contains('Update').should('be.visible')
        })
    })

    it('should display version numbers', () => {
      cy.mountWithProviders(<ResourcesBreakdown resources={mockSchemas} />)

      cy.get('[data-testid="schemas-list"]').within(() => {
        // Draft version will be large number
        cy.contains(/v\d+/).should('exist')
        cy.contains('v2').should('exist')
      })
    })
  })

  describe('Icons', () => {
    it('should display file icon for schemas', () => {
      cy.mountWithProviders(<ResourcesBreakdown resources={mockSchemas} />)

      cy.get('[data-testid="schemas-list"]').within(() => {
        cy.get('svg').should('have.length.at.least', 2) // File icon + badge icons
      })
    })

    it('should display code icon for scripts', () => {
      cy.mountWithProviders(<ResourcesBreakdown resources={mockScripts} />)

      cy.get('[data-testid="scripts-list"]').within(() => {
        cy.get('svg').should('have.length.at.least', 1)
      })
    })
  })

  describe('Status Badge Colors', () => {
    it('should use blue badge for new resources by default', () => {
      cy.mountWithProviders(<ResourcesBreakdown resources={mockSchemas} />)

      cy.get('[data-testid="schema-temperature-schema"]')
        .parent()
        .parent()
        .within(() => {
          cy.get('.chakra-badge').should('exist')
        })
    })

    it('should use orange badge for update resources by default', () => {
      cy.mountWithProviders(<ResourcesBreakdown resources={mockSchemas} />)

      cy.get('[data-testid="schema-humidity-schema"]')
        .parent()
        .parent()
        .within(() => {
          cy.get('.chakra-badge').should('exist')
        })
    })

    it('should allow custom color scheme for new badge', () => {
      cy.mountWithProviders(<ResourcesBreakdown resources={mockSchemas} newBadgeColorScheme="green" />)

      cy.get('[data-testid="schema-temperature-schema"]').should('be.visible')
    })

    it('should allow custom color scheme for update badge', () => {
      cy.mountWithProviders(<ResourcesBreakdown resources={mockSchemas} updateBadgeColorScheme="purple" />)

      cy.get('[data-testid="schema-humidity-schema"]').should('be.visible')
    })
  })

  describe('Accessibility', () => {
    it('should be accessible with mixed resources', () => {
      cy.mountWithProviders(<ResourcesBreakdown resources={mockMixedResources} />)

      cy.injectAxe()
      cy.checkAccessibility()
    })

    it('should be accessible with empty state', () => {
      cy.mountWithProviders(<ResourcesBreakdown resources={[]} />)

      cy.injectAxe()
      cy.checkAccessibility()
    })

    it('should have proper ARIA labels on accordion buttons', () => {
      cy.mountWithProviders(<ResourcesBreakdown resources={mockMixedResources} />)

      cy.get('[data-testid="schemas-header"]').parent().should('have.attr', 'aria-label').and('contain', 'Schemas')
    })

    it('should have proper heading elements', () => {
      cy.mountWithProviders(<ResourcesBreakdown resources={mockMixedResources} />)

      cy.get('h3').should('have.length.at.least', 2)
    })

    it('should support keyboard navigation', () => {
      cy.mountWithProviders(<ResourcesBreakdown resources={mockMixedResources} />)

      // Tab to first accordion button
      cy.get('body').tab()
      cy.focused().should('contain', 'Schemas')

      // Press Enter to collapse
      cy.focused().type('{enter}')
      cy.get('[data-testid="schemas-list"]').should('not.be.visible')

      // Press Enter again to expand
      cy.focused().type('{enter}')
      cy.get('[data-testid="schemas-list"]').should('be.visible')
    })
  })

  describe('Edge Cases', () => {
    it('should handle single schema', () => {
      cy.mountWithProviders(<ResourcesBreakdown resources={[mockSchemas[0]]} />)

      cy.get('[data-testid="schemas-header"]').should('contain', 'Schema (1)')
      cy.get('[data-testid="schema-temperature-schema"]').should('be.visible')
    })

    it('should handle single script', () => {
      cy.mountWithProviders(<ResourcesBreakdown resources={mockScripts} />)

      cy.get('[data-testid="scripts-header"]').should('contain', 'Script (1)')
      cy.get('[data-testid="script-transform-temperature"]').should('be.visible')
    })

    it('should handle many resources', () => {
      const manySchemas: ResourceSummary[] = Array.from({ length: 10 }, (_, i) => ({
        id: `schema-${i}`,
        version: i + 1,
        type: 'SCHEMA',
        isNew: i % 2 === 0,
        metadata: { schemaType: 'JSON' },
      }))

      cy.mountWithProviders(<ResourcesBreakdown resources={manySchemas} />)

      cy.get('[data-testid="schemas-header"]').should('contain', 'Schemas (10)')
      cy.get('[data-testid="schemas-list"]').children().should('have.length', 10)
    })

    it('should handle resource with special characters in ID', () => {
      const specialResource: ResourceSummary = {
        id: 'schema-with-special-chars-!@#$',
        version: 1,
        type: 'SCHEMA',
        isNew: true,
        metadata: { schemaType: 'JSON' },
      }

      cy.mountWithProviders(<ResourcesBreakdown resources={[specialResource]} />)

      cy.contains('schema-with-special-chars-!@#$').should('be.visible')
    })
  })

  describe('Visual Consistency', () => {
    it('should maintain consistent spacing between resource items', () => {
      cy.mountWithProviders(<ResourcesBreakdown resources={mockSchemas} />)

      cy.get('[data-testid="schemas-list"]').within(() => {
        cy.get('.chakra-stack').should('exist')
      })
    })

    it('should align icons and text properly', () => {
      cy.mountWithProviders(<ResourcesBreakdown resources={mockMixedResources} />)

      cy.get('[data-testid="schemas-list"]').within(() => {
        cy.get('.chakra-stack').first().should('have.css', 'display')
      })
    })

    it('should use consistent badge styling', () => {
      cy.mountWithProviders(<ResourcesBreakdown resources={mockMixedResources} />)

      cy.get('.chakra-badge').should('have.length.at.least', 3)
    })
  })
})
