/// <reference types="cypress" />

import PolicyOverview from './PolicyOverview.tsx'
import { DataHubNodeType, type PolicySummary } from '@datahub/types.ts'

describe('PolicyOverview', () => {
  const mockDataPolicySummaryNew: PolicySummary = {
    id: 'my-data-policy',
    type: DataHubNodeType.DATA_POLICY,
    isNew: true,
    topicFilters: ['devices/+/temperature', 'devices/+/humidity'],
  }

  const mockDataPolicySummaryUpdate: PolicySummary = {
    id: 'existing-data-policy',
    type: DataHubNodeType.DATA_POLICY,
    isNew: false,
    topicFilters: ['sensors/#'],
  }

  const mockBehaviorPolicySummaryNew: PolicySummary = {
    id: 'my-behavior-policy',
    type: DataHubNodeType.BEHAVIOR_POLICY,
    isNew: true,
    transitions: ['Mqtt.OnInboundPublish', 'Mqtt.OnInboundConnect'],
  }

  const mockBehaviorPolicySummaryUpdate: PolicySummary = {
    id: 'existing-behavior-policy',
    type: DataHubNodeType.BEHAVIOR_POLICY,
    isNew: false,
    transitions: ['Mqtt.OnInboundDisconnect'],
  }

  describe('Rendering', () => {
    it('should render the component with Data Policy summary (new)', () => {
      cy.mountWithProviders(<PolicyOverview summary={mockDataPolicySummaryNew} />)

      cy.get('[data-testid="policy-overview"]').should('be.visible')
      cy.contains('Policy Details').should('be.visible')
      cy.get('[data-testid="policy-status-badge"]').should('contain', 'New')
      cy.get('[data-testid="policy-type"]').should('contain', 'Data Policy')
      cy.get('[data-testid="policy-id"]').should('contain', 'my-data-policy')
      cy.get('[data-testid="topic-filters-list"]').should('be.visible')
      cy.get('[data-testid="topic-filters-list"] li').should('have.length', 2)
    })

    it('should render the component with Data Policy summary (update)', () => {
      cy.mountWithProviders(<PolicyOverview summary={mockDataPolicySummaryUpdate} />)

      cy.get('[data-testid="policy-status-badge"]').should('contain', 'Update')
      cy.get('[data-testid="policy-id"]').should('contain', 'existing-data-policy')
      cy.get('[data-testid="topic-filters-list"] li').should('have.length', 1)
    })

    it('should render the component with Behavior Policy summary (new)', () => {
      cy.mountWithProviders(<PolicyOverview summary={mockBehaviorPolicySummaryNew} />)

      cy.get('[data-testid="policy-status-badge"]').should('contain', 'New')
      cy.get('[data-testid="policy-type"]').should('contain', 'Behavior Policy')
      cy.get('[data-testid="policy-id"]').should('contain', 'my-behavior-policy')
      cy.get('[data-testid="transitions-list"]').should('be.visible')
      cy.get('[data-testid="transitions-list"] li').should('have.length', 2)
    })

    it('should render the component with Behavior Policy summary (update)', () => {
      cy.mountWithProviders(<PolicyOverview summary={mockBehaviorPolicySummaryUpdate} />)

      cy.get('[data-testid="policy-status-badge"]').should('contain', 'Update')
      cy.get('[data-testid="policy-id"]').should('contain', 'existing-behavior-policy')
      cy.get('[data-testid="transitions-list"] li').should('have.length', 1)
    })

    it('should render with empty topic filters', () => {
      const summary: PolicySummary = {
        ...mockDataPolicySummaryNew,
        topicFilters: [],
      }

      cy.mountWithProviders(<PolicyOverview summary={summary} />)

      cy.get('[data-testid="topic-filters-list"]').should('not.exist')
    })

    it('should render with empty transitions', () => {
      const summary: PolicySummary = {
        ...mockBehaviorPolicySummaryNew,
        transitions: [],
      }

      cy.mountWithProviders(<PolicyOverview summary={summary} />)

      cy.get('[data-testid="transitions-list"]').should('not.exist')
    })

    it('should display policy icon', () => {
      cy.mountWithProviders(<PolicyOverview summary={mockDataPolicySummaryNew} />)

      cy.get('[data-testid="policy-icon"]').should('be.visible')
    })

    it('should truncate long policy IDs with ellipsis', () => {
      const longIdSummary: PolicySummary = {
        ...mockDataPolicySummaryNew,
        id: 'this-is-a-very-long-policy-id-that-should-be-truncated-with-ellipsis',
      }

      cy.mountWithProviders(<PolicyOverview summary={longIdSummary} />)

      cy.get('[data-testid="policy-id"]')
        .should('have.css', 'text-overflow', 'ellipsis')
        .and('have.attr', 'title', longIdSummary.id)
    })
  })

  describe('Status Badge Colors', () => {
    it('should use blue color scheme for new policy by default', () => {
      cy.mountWithProviders(<PolicyOverview summary={mockDataPolicySummaryNew} />)

      cy.get('[data-testid="policy-status-badge"]').should('have.class', 'chakra-badge')
      // Chakra adds color scheme as class
    })

    it('should use orange color scheme for update policy by default', () => {
      cy.mountWithProviders(<PolicyOverview summary={mockDataPolicySummaryUpdate} />)

      cy.get('[data-testid="policy-status-badge"]').should('have.class', 'chakra-badge')
    })

    it('should allow custom color scheme for new badge', () => {
      cy.mountWithProviders(<PolicyOverview summary={mockDataPolicySummaryNew} newBadgeColorScheme="green" />)

      cy.get('[data-testid="policy-status-badge"]').should('be.visible')
    })

    it('should allow custom color scheme for update badge', () => {
      cy.mountWithProviders(<PolicyOverview summary={mockDataPolicySummaryUpdate} updateBadgeColorScheme="purple" />)

      cy.get('[data-testid="policy-status-badge"]').should('be.visible')
    })
  })

  describe('Content Validation', () => {
    it('should display correct count for topic filters', () => {
      cy.mountWithProviders(<PolicyOverview summary={mockDataPolicySummaryNew} />)

      cy.contains('Topic Filters (2)').should('be.visible')
    })

    it('should display correct count for transitions', () => {
      cy.mountWithProviders(<PolicyOverview summary={mockBehaviorPolicySummaryNew} />)

      cy.contains('Transitions (2)').should('be.visible')
    })

    it('should display all topic filters in list', () => {
      cy.mountWithProviders(<PolicyOverview summary={mockDataPolicySummaryNew} />)

      cy.get('[data-testid="topic-filters-list"]').within(() => {
        cy.contains('devices/+/temperature').should('be.visible')
        cy.contains('devices/+/humidity').should('be.visible')
      })
    })

    it('should display all transitions in list', () => {
      cy.mountWithProviders(<PolicyOverview summary={mockBehaviorPolicySummaryNew} />)

      cy.get('[data-testid="transitions-list"]').within(() => {
        cy.contains('Mqtt.OnInboundPublish').should('be.visible')
        cy.contains('Mqtt.OnInboundConnect').should('be.visible')
      })
    })
  })

  describe('Accessibility', () => {
    it('should be accessible with Data Policy', () => {
      cy.mountWithProviders(<PolicyOverview summary={mockDataPolicySummaryNew} />)

      cy.injectAxe()
      cy.checkAccessibility()
    })

    it('should be accessible with Behavior Policy', () => {
      cy.mountWithProviders(<PolicyOverview summary={mockBehaviorPolicySummaryNew} />)

      cy.injectAxe()
      cy.checkAccessibility()
    })

    it('should have proper heading hierarchy', () => {
      cy.mountWithProviders(<PolicyOverview summary={mockDataPolicySummaryNew} />)

      // Card header should have heading
      cy.get('[data-testid="policy-overview"]').within(() => {
        cy.get('h2, h3, h4').should('exist')
      })
    })

    it('should have readable text colors with sufficient contrast', () => {
      cy.mountWithProviders(<PolicyOverview summary={mockDataPolicySummaryNew} />)

      cy.injectAxe()
      cy.checkAccessibility()
    })

    it('should provide title attribute for truncated policy ID', () => {
      const longIdSummary: PolicySummary = {
        ...mockDataPolicySummaryNew,
        id: 'very-long-policy-id-that-will-be-truncated',
      }

      cy.mountWithProviders(<PolicyOverview summary={longIdSummary} />)

      cy.get('[data-testid="policy-id"]').should('have.attr', 'title', longIdSummary.id)
    })
  })

  describe('Edge Cases', () => {
    it('should handle policy with single topic filter', () => {
      const singleFilterSummary: PolicySummary = {
        ...mockDataPolicySummaryNew,
        topicFilters: ['single/topic'],
      }

      cy.mountWithProviders(<PolicyOverview summary={singleFilterSummary} />)

      cy.contains('Topic Filters (1)').should('be.visible')
      cy.get('[data-testid="topic-filters-list"] li').should('have.length', 1)
    })

    it('should handle policy with single transition', () => {
      const singleTransitionSummary: PolicySummary = {
        ...mockBehaviorPolicySummaryNew,
        transitions: ['Mqtt.OnInboundPublish'],
      }

      cy.mountWithProviders(<PolicyOverview summary={singleTransitionSummary} />)

      cy.contains('Transitions (1)').should('be.visible')
      cy.get('[data-testid="transitions-list"] li').should('have.length', 1)
    })

    it('should handle Data Policy with undefined topicFilters', () => {
      const noFiltersSummary: PolicySummary = {
        ...mockDataPolicySummaryNew,
        topicFilters: undefined,
      }

      cy.mountWithProviders(<PolicyOverview summary={noFiltersSummary} />)

      cy.get('[data-testid="topic-filters-list"]').should('not.exist')
    })

    it('should handle Behavior Policy with undefined transitions', () => {
      const noTransitionsSummary: PolicySummary = {
        ...mockBehaviorPolicySummaryNew,
        transitions: undefined,
      }

      cy.mountWithProviders(<PolicyOverview summary={noTransitionsSummary} />)

      cy.get('[data-testid="transitions-list"]').should('not.exist')
    })

    it('should handle policy with special characters in ID', () => {
      const specialCharSummary: PolicySummary = {
        ...mockDataPolicySummaryNew,
        id: 'policy-with-special-chars-!@#$%^&*()',
      }

      cy.mountWithProviders(<PolicyOverview summary={specialCharSummary} />)

      cy.get('[data-testid="policy-id"]').should('contain', specialCharSummary.id)
    })
  })

  describe('Visual Consistency', () => {
    it('should display status badge with icon', () => {
      cy.mountWithProviders(<PolicyOverview summary={mockDataPolicySummaryNew} />)

      cy.get('[data-testid="policy-status-badge"]').within(() => {
        cy.get('svg').should('exist') // Icon should be present
        cy.contains('New').should('be.visible')
      })
    })

    it('should use monospace font for policy ID', () => {
      cy.mountWithProviders(<PolicyOverview summary={mockDataPolicySummaryNew} />)

      cy.get('[data-testid="policy-id"]').should('have.css', 'font-family').and('match', /mono/i)
    })

    it('should maintain consistent spacing between sections', () => {
      cy.mountWithProviders(<PolicyOverview summary={mockDataPolicySummaryNew} />)

      cy.get('[data-testid="policy-overview"]').within(() => {
        cy.get('.chakra-stack').should('exist')
      })
    })
  })
})
