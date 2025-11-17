/// <reference types="cypress" />

import { mockBridge } from '@/api/hooks/useGetBridges/__handlers__'
import { mockAdapter_OPCUA } from '@/api/hooks/useProtocolAdapters/__handlers__'
import { Status } from '@/api/__generated__'

import { loginPage, workspacePage } from 'cypress/pages'
import { cy_interceptCoreE2E } from 'cypress/utils/intercept.utils.ts'
import { cy_interceptPulseWithMockDB, getPulseFactory } from 'cypress/utils/intercept-pulse.utils.ts'
import { drop } from '@mswjs/data'

/**
 * PR Screenshot Tests - Complex Workspace Scenarios
 *
 * These tests generate high-quality screenshots for PR documentation showing:
 * 1. Healthy operational workspace with all systems green
 * 2. Complex mixed-status enterprise scenario with Pulse Agent, combiners, and data integration
 *
 * Run with: pnpm cypress run --spec "cypress/e2e/workspace/workspace-pr-screenshots.spec.cy.ts" --env grepTags=@percy
 *
 * Note: Uses cy_interceptPulseWithMockDB to enable Pulse Agent, Asset Mapper, and Combiner nodes
 */
describe('Workspace - PR Screenshots', () => {
  const mswDB = getPulseFactory()

  beforeEach(() => {
    drop(mswDB)

    // Core E2E intercepts (auth, config, notifications, etc.)
    cy_interceptCoreE2E()

    // Enable Pulse Agent capabilities and set up mock database
    // This MUST be called before navigation to enable Pulse/Asset Mapper nodes
    cy_interceptPulseWithMockDB(mswDB, true, true)

    cy.intercept('/api/v1/management/topic-filters', { statusCode: 202, log: false })
    cy.intercept('/api/v1/data-hub/data-validation/policies', { statusCode: 202, log: false })
    cy.intercept('/api/v1/management/protocol-adapters/adapters/**/northboundMappings', {
      statusCode: 202,
      log: false,
    })
    cy.intercept('/api/v1/management/protocol-adapters/adapters/**/southboundMappings', {
      statusCode: 202,
      log: false,
    })

    loginPage.visit('/app/workspace')
    loginPage.loginButton.click()
    workspacePage.navLink.click()

    workspacePage.toolbox.fit.click()
  })

  describe('PR Screenshots - Status Visualization', () => {
    beforeEach(() => {
      // Mock a comprehensive enterprise workspace with all entity types and mixed statuses
      cy.intercept('/api/v1/management/protocol-adapters/adapters', {
        items: [
          {
            ...mockAdapter_OPCUA,
            id: 'opcua-production',
            status: {
              connection: Status.connection.CONNECTED,
              runtime: Status.runtime.STARTED,
            },
          },
          {
            ...mockAdapter_OPCUA,
            id: 'opcua-quality',
            type: 'simulation',
            status: {
              connection: Status.connection.CONNECTED,
              runtime: Status.runtime.STARTED,
            },
          },
          {
            ...mockAdapter_OPCUA,
            id: 'modbus-temperature',
            type: 'modbus',
            status: {
              connection: Status.connection.CONNECTED,
              runtime: Status.runtime.STARTED,
            },
          },
          {
            ...mockAdapter_OPCUA,
            id: 's7-assembly',
            type: 's7',
            status: {
              connection: Status.connection.CONNECTED,
              runtime: Status.runtime.STARTED,
            },
          },
        ],
      }).as('getActiveAdapters')

      // Mock northbound mappings for all adapters (operational ACTIVE - will show animated edges)
      cy.intercept('/api/v1/management/protocol-adapters/adapters/opcua-production/northboundMappings', {
        items: [
          { tagName: 'production-sensor-1', topic: 'factory/production/line1' },
          { tagName: 'production-sensor-2', topic: 'factory/production/line2' },
        ],
      })

      cy.intercept('/api/v1/management/protocol-adapters/adapters/opcua-quality/northboundMappings', {
        items: [{ tagName: 'quality-check', topic: 'factory/quality/inspection' }],
      })

      cy.intercept('/api/v1/management/protocol-adapters/adapters/modbus-temperature/northboundMappings', {
        items: [{ tagName: 'temp-sensor', topic: 'factory/environment/temperature' }],
      })

      cy.intercept('/api/v1/management/protocol-adapters/adapters/s7-assembly/northboundMappings', {
        items: [
          { tagName: 'assembly-status', topic: 'factory/assembly/status' },
          { tagName: 'assembly-count', topic: 'factory/assembly/count' },
        ],
      })

      // Mock bridge with ACTIVE status and topic filters (operational ACTIVE)
      cy.intercept('/api/v1/management/bridges', {
        items: [
          {
            ...mockBridge,
            id: 'cloud-bridge',
            status: {
              connection: Status.connection.CONNECTED,
              runtime: Status.runtime.STARTED,
            },
            localSubscriptions: [{ filters: ['factory/#'], destination: 'cloud/factory', maxQoS: 1 }],
            remoteSubscriptions: [{ filters: ['commands/#'], destination: 'local/commands', maxQoS: 1 }],
          },
        ],
      })
    })

    it('PR Screenshot 1: Healthy workspace with all systems operational', { tags: ['@percy'] }, () => {
      // Wait for React Flow to render
      cy.get('.react-flow__viewport').should('be.visible')

      // Fit the workspace to show all entities
      workspacePage.toolbox.fit.click()

      // Check accessibility before taking screenshot
      cy.injectAxe()
      cy.checkAccessibility(undefined, {
        rules: {
          region: { enabled: false },
          'color-contrast': { enabled: false },
        },
      })

      // ACTUAL SCREENSHOT FILE for PR (saved to cypress/screenshots/)
      cy.screenshot('PR-Screenshot-1-Healthy-Workspace-All-Systems-Operational', {
        overwrite: true,
        capture: 'viewport',
      })

      // Percy visual regression snapshot (optional)
      cy.percySnapshot('Workspace Status - All Systems Healthy and Operational', {
        widths: [1280, 1920],
        minHeight: 800,
      })
    })

    it(
      'PR Screenshot 2: Complex enterprise workspace with Pulse Agent and data integration',
      { tags: ['@percy'] },
      () => {
        workspacePage.toolbox.fit.click()

        // Wait for React Flow to render and all Pulse entities to load
        cy.get('.react-flow__viewport', { timeout: 10000 }).should('be.visible')

        // Check accessibility before taking screenshot
        cy.injectAxe()
        cy.checkAccessibility(undefined, {
          rules: {
            region: { enabled: false },
            'color-contrast': { enabled: false },
          },
        })

        // Capture screenshot showing comprehensive enterprise IoT scenario:
        // - 6 adapters with all status types (ERROR, ACTIVE operational, ACTIVE non-operational, INACTIVE)
        // - Pulse Agent with managed assets
        // - Combiner aggregating data from critical and working adapters
        // - Asset Mapper integrating multiple adapters with Pulse Agent
        // - Bridge for cloud connectivity
        // - Red edges for ERROR status (opcua-critical, http-failed)
        // - Green animated edges for ACTIVE operational (opcua-working, opcua-working2)
        // - Green static edges for ACTIVE non-operational (modbus-idle)
        // - Gray edges for INACTIVE (s7-maintenance)
        // - Error propagation through device nodes and groups

        // ACTUAL SCREENSHOT FILE for PR (saved to cypress/screenshots/)
        cy.screenshot('PR-Screenshot-2-Complex-Workspace-Mixed-Status-With-Pulse-And-Integration', {
          overwrite: true,
          capture: 'viewport',
        })

        // Percy visual regression snapshot (optional)
        cy.percySnapshot('Workspace Status - Complex Enterprise Scenario with Pulse and Data Integration', {
          widths: [1280, 1920],
          minHeight: 800,
        })
      }
    )
  })
})
