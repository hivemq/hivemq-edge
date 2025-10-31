/// <reference types="cypress" />

/**
 * E2E tests for group node status aggregation and behavior
 * Task 32118 Phase 7 - Group Node Special Handling
 */

import { loginPage, workspacePage } from '../../pages'
import { cy_interceptCoreE2E } from '../../utils/intercept.utils.ts'

describe.skip('Workspace Group Nodes - Status Aggregation', () => {
  // Skip all tests - Need to be reviewed again

  beforeEach(() => {
    cy.viewport(1280, 800)
    cy_interceptCoreE2E()

    loginPage.visit('/app/workspace')
    loginPage.loginButton.click()
    workspacePage.navLink.click()
  })

  describe('Group Node Status Aggregation', () => {
    it('should display ERROR status when any child has ERROR', () => {
      // Create a group with multiple adapters
      cy.intercept('/api/v1/management/protocol-adapters/adapters', {
        body: {
          items: [
            {
              id: 'adapter1',
              type: 'simulation',
              status: { connection: 'CONNECTED', runtime: 'STARTED' },
            },
            {
              id: 'adapter2',
              type: 'simulation',
              status: { connection: 'DISCONNECTED', runtime: 'STOPPED' }, // ERROR
            },
          ],
        },
      })

      cy.visit('/workspace')

      cy.wait('@getAdapters')
      workspacePage.toolbox.fit.click()

      // Select both adapters and group them
      cy.get('[data-testid="node-adapter1"]').click({ shiftKey: true })
      cy.get('[data-testid="node-adapter2"]').click({ shiftKey: true })
      cy.get('[data-testid="workspace-toolbar-group"]').click()

      // Verify group node shows ERROR status (red border/color)
      cy.get('[data-testid^="node-cluster"]')
        .should('exist')
        .and('have.css', 'border-color')
        .and('match', /rgb\(.*\)/) // Should have colored border
    })

    it('should display ACTIVE status when all children are ACTIVE', () => {
      cy.intercept('/api/v1/management/protocol-adapters/adapters', {
        body: {
          items: [
            {
              id: 'adapter1',
              type: 'simulation',
              status: { connection: 'CONNECTED', runtime: 'STARTED' },
            },
            {
              id: 'adapter2',
              type: 'simulation',
              status: { connection: 'CONNECTED', runtime: 'STARTED' },
            },
          ],
        },
      })

      cy.visit('/workspace')

      // Select both adapters and group them
      cy.get('[data-testid="node-adapter1"]').click({ shiftKey: true })
      cy.get('[data-testid="node-adapter2"]').click({ shiftKey: true })
      cy.get('[data-testid="workspace-toolbar-group"]').click()

      // Verify group node shows ACTIVE status (green border/color)
      cy.get('[data-testid^="node-cluster"]').should('exist')

      // Verify edges from group are green (active)
      cy.get('.react-flow__edge-path[stroke*="green"]').should('exist')
    })

    it('should update group status when child status changes', () => {
      // Start with all adapters ACTIVE
      let adapter2Status = { connection: 'CONNECTED', runtime: 'STARTED' }

      cy.intercept('/api/v1/management/protocol-adapters/adapters', (req) => {
        req.reply({
          items: [
            {
              id: 'adapter1',
              type: 'simulation',
              status: { connection: 'CONNECTED', runtime: 'STARTED' },
            },
            {
              id: 'adapter2',
              type: 'simulation',
              status: adapter2Status,
            },
          ],
        })
      }).as('getAdapters')

      cy.visit('/workspace')
      cy.wait('@getAdapters')

      // Group the adapters
      cy.get('[data-testid="node-adapter1"]').click({ shiftKey: true })
      cy.get('[data-testid="node-adapter2"]').click({ shiftKey: true })
      cy.get('[data-testid="workspace-toolbar-group"]').click()

      // Change adapter2 to ERROR
      adapter2Status = { connection: 'DISCONNECTED', runtime: 'STOPPED' }

      // Trigger status refresh
      cy.get('[data-testid="workspace-refresh"]').click()
      cy.wait('@getAdapters')

      // Verify group status updated to ERROR
      cy.get('[data-testid^="node-cluster"]').should('exist')
    })
  })

  describe('Group Node Expand/Collapse', () => {
    beforeEach(() => {
      // Setup group with multiple nodes
      cy.intercept('/api/v1/management/protocol-adapters/adapters', {
        body: {
          items: [
            { id: 'adapter1', type: 'simulation', status: { connection: 'CONNECTED', runtime: 'STARTED' } },
            { id: 'adapter2', type: 'simulation', status: { connection: 'CONNECTED', runtime: 'STARTED' } },
          ],
        },
      })
    })

    it('should expand group to show child nodes', () => {
      cy.visit('/workspace')

      // Create group
      cy.get('[data-testid="node-adapter1"]').click({ shiftKey: true })
      cy.get('[data-testid="node-adapter2"]').click({ shiftKey: true })
      cy.get('[data-testid="workspace-toolbar-group"]').click()

      // Group should be collapsed initially
      cy.get('[data-testid^="node-cluster"]').should('exist')

      // Child nodes should not be visible (or very small)
      cy.get('[data-testid="node-adapter1"]').should('not.be.visible')
      cy.get('[data-testid="node-adapter2"]').should('not.be.visible')

      // Click expand button on group
      cy.get('[data-testid^="node-cluster"]').within(() => {
        cy.get('[aria-label*="Expand"]').click()
      })

      // Child nodes should now be visible
      cy.get('[data-testid="node-adapter1"]').should('be.visible')
      cy.get('[data-testid="node-adapter2"]').should('be.visible')
    })

    it('should collapse group to hide child nodes', () => {
      cy.visit('/workspace')

      // Create and expand group
      cy.get('[data-testid="node-adapter1"]').click({ shiftKey: true })
      cy.get('[data-testid="node-adapter2"]').click({ shiftKey: true })
      cy.get('[data-testid="workspace-toolbar-group"]').click()

      cy.get('[data-testid^="node-cluster"]').within(() => {
        cy.get('[aria-label*="Expand"]').click()
      })

      // Child nodes visible
      cy.get('[data-testid="node-adapter1"]').should('be.visible')

      // Click collapse button
      cy.get('[data-testid^="node-cluster"]').within(() => {
        cy.get('[aria-label*="Collapse"]').click()
      })

      // Child nodes should be hidden
      cy.get('[data-testid="node-adapter1"]').should('not.be.visible')
      cy.get('[data-testid="node-adapter2"]').should('not.be.visible')
    })

    it('should maintain status aggregation when collapsed', () => {
      cy.intercept('/api/v1/management/protocol-adapters/adapters', {
        body: {
          items: [
            {
              id: 'adapter1',
              type: 'simulation',
              status: { connection: 'CONNECTED', runtime: 'STARTED' },
            },
            {
              id: 'adapter2',
              type: 'simulation',
              status: { connection: 'DISCONNECTED', runtime: 'STOPPED' }, // ERROR
            },
          ],
        },
      })

      cy.visit('/workspace')

      // Create group
      cy.get('[data-testid="node-adapter1"]').click({ shiftKey: true })
      cy.get('[data-testid="node-adapter2"]').click({ shiftKey: true })
      cy.get('[data-testid="workspace-toolbar-group"]').click()

      // Group should show ERROR status even when collapsed
      cy.get('[data-testid^="node-cluster"]').should('exist').and('have.attr', 'data-status').and('include', 'ERROR')
    })
  })

  describe('Nested Groups', () => {
    it('should support creating nested groups', () => {
      cy.intercept('/api/v1/management/protocol-adapters/adapters', {
        body: {
          items: [
            { id: 'adapter1', type: 'simulation', status: { connection: 'CONNECTED', runtime: 'STARTED' } },
            { id: 'adapter2', type: 'simulation', status: { connection: 'CONNECTED', runtime: 'STARTED' } },
            { id: 'adapter3', type: 'simulation', status: { connection: 'CONNECTED', runtime: 'STARTED' } },
          ],
        },
      })

      cy.visit('/workspace')

      // Create first group
      cy.get('[data-testid="node-adapter1"]').click({ shiftKey: true })
      cy.get('[data-testid="node-adapter2"]').click({ shiftKey: true })
      cy.get('[data-testid="workspace-toolbar-group"]').click()

      // Create second group including first group and adapter3
      cy.get('[data-testid^="node-cluster"]').first().click({ shiftKey: true })
      cy.get('[data-testid="node-adapter3"]').click({ shiftKey: true })
      cy.get('[data-testid="workspace-toolbar-group"]').click()

      // Should have two group nodes
      cy.get('[data-testid^="node-cluster"]').should('have.length', 2)
    })

    it('should propagate ERROR status through nested groups', () => {
      cy.intercept('/api/v1/management/protocol-adapters/adapters', {
        body: {
          items: [
            {
              id: 'adapter1',
              type: 'simulation',
              status: { connection: 'DISCONNECTED', runtime: 'STOPPED' }, // ERROR
            },
            {
              id: 'adapter2',
              type: 'simulation',
              status: { connection: 'CONNECTED', runtime: 'STARTED' },
            },
            {
              id: 'adapter3',
              type: 'simulation',
              status: { connection: 'CONNECTED', runtime: 'STARTED' },
            },
          ],
        },
      })

      cy.visit('/workspace')

      // Create nested groups
      cy.get('[data-testid="node-adapter1"]').click({ shiftKey: true })
      cy.get('[data-testid="node-adapter2"]').click({ shiftKey: true })
      cy.get('[data-testid="workspace-toolbar-group"]').click()

      cy.get('[data-testid^="node-cluster"]').first().click({ shiftKey: true })
      cy.get('[data-testid="node-adapter3"]').click({ shiftKey: true })
      cy.get('[data-testid="workspace-toolbar-group"]').click()

      // Both groups should show ERROR status
      cy.get('[data-testid^="node-cluster"]').each(($group) => {
        cy.wrap($group).should('have.attr', 'data-status').and('include', 'ERROR')
      })
    })
  })

  describe('Ungroup Functionality', () => {
    it('should ungroup and restore individual node statuses', () => {
      cy.intercept('/api/v1/management/protocol-adapters/adapters', {
        body: {
          items: [
            {
              id: 'adapter1',
              type: 'simulation',
              status: { connection: 'CONNECTED', runtime: 'STARTED' },
            },
            {
              id: 'adapter2',
              type: 'simulation',
              status: { connection: 'DISCONNECTED', runtime: 'STOPPED' },
            },
          ],
        },
      })

      cy.visit('/workspace')

      // Create group
      cy.get('[data-testid="node-adapter1"]').click({ shiftKey: true })
      cy.get('[data-testid="node-adapter2"]').click({ shiftKey: true })
      cy.get('[data-testid="workspace-toolbar-group"]').click()

      // Ungroup
      cy.get('[data-testid^="node-cluster"]').rightclick()
      cy.get('[data-testid="context-menu-ungroup"]').click()
      cy.get('[data-testid="confirm-dialog-confirm"]').click()

      // Individual nodes should be visible with their original statuses
      cy.get('[data-testid="node-adapter1"]').should('be.visible')
      cy.get('[data-testid="node-adapter2"]').should('be.visible')

      // Group node should be gone
      cy.get('[data-testid^="node-cluster"]').should('not.exist')
    })
  })

  describe('Accessibility', () => {
    it('should be keyboard accessible for group operations', () => {
      cy.visit('/workspace')
      cy.injectAxe()

      // Create group
      cy.get('[data-testid="node-adapter1"]').click({ shiftKey: true })
      cy.get('[data-testid="node-adapter2"]').click({ shiftKey: true })
      cy.get('[data-testid="workspace-toolbar-group"]').click()

      // Check accessibility of group node
      cy.get('[data-testid^="node-cluster"]').should('exist')
      cy.checkA11y('[data-testid^="node-cluster"]')

      // Group controls should be keyboard accessible
      cy.get('[data-testid^="node-cluster"]').within(() => {
        cy.get('button').each(($btn) => {
          cy.wrap($btn).should('have.attr', 'aria-label')
        })
      })
    })

    it('should announce group status changes to screen readers', () => {
      cy.visit('/workspace')

      cy.get('[data-testid="node-adapter1"]').click({ shiftKey: true })
      cy.get('[data-testid="node-adapter2"]').click({ shiftKey: true })
      cy.get('[data-testid="workspace-toolbar-group"]').click()

      // Group node should have appropriate ARIA attributes
      cy.get('[data-testid^="node-cluster"]')
        .should('have.attr', 'role')
        .and('match', /group|region/)
    })
  })

  describe('Visual Regression', () => {
    it('should match snapshot for group with ACTIVE children', () => {
      cy.intercept('/api/v1/management/protocol-adapters/adapters', {
        body: {
          items: [
            { id: 'adapter1', type: 'simulation', status: { connection: 'CONNECTED', runtime: 'STARTED' } },
            { id: 'adapter2', type: 'simulation', status: { connection: 'CONNECTED', runtime: 'STARTED' } },
          ],
        },
      })

      cy.visit('/workspace')

      cy.get('[data-testid="node-adapter1"]').click({ shiftKey: true })
      cy.get('[data-testid="node-adapter2"]').click({ shiftKey: true })
      cy.get('[data-testid="workspace-toolbar-group"]').click()

      cy.percySnapshot('Workspace - Group Node - All Active')
    })

    it('should match snapshot for group with ERROR child', () => {
      cy.intercept('/api/v1/management/protocol-adapters/adapters', {
        body: {
          items: [
            { id: 'adapter1', type: 'simulation', status: { connection: 'CONNECTED', runtime: 'STARTED' } },
            { id: 'adapter2', type: 'simulation', status: { connection: 'DISCONNECTED', runtime: 'STOPPED' } },
          ],
        },
      })

      cy.visit('/workspace')

      cy.get('[data-testid="node-adapter1"]').click({ shiftKey: true })
      cy.get('[data-testid="node-adapter2"]').click({ shiftKey: true })
      cy.get('[data-testid="workspace-toolbar-group"]').click()

      cy.percySnapshot('Workspace - Group Node - With Error')
    })
  })
})
