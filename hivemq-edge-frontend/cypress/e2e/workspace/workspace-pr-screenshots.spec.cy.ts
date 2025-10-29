/// <reference types="cypress" />

import { MOCK_PROTOCOL_OPC_UA } from '@/__test-utils__/adapters'
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

    // Protocol types
    cy.intercept('/api/v1/management/protocol-adapters/types', {
      items: [MOCK_PROTOCOL_OPC_UA],
    }).as('getProtocols')

    loginPage.visit('/app/workspace')
    loginPage.loginButton.click()
    workspacePage.navLink.click()
  })

  describe('PR Screenshots - Status Visualization', () => {
    it('PR Screenshot 1: Healthy workspace with all systems operational', { tags: ['@percy'] }, () => {
      // Mock a complex workspace with multiple adapters and bridge
      // All entities showing ACTIVE status with operational data flow
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

      // Wait for React Flow to render
      cy.get('.react-flow__viewport', { timeout: 10000 }).should('be.visible')

      // Fit the workspace to show all entities
      workspacePage.toolbox.fit.click()

      // Wait for layout to settle
      cy.wait(1500)

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
        // Mock a comprehensive enterprise workspace with all entity types and mixed statuses
        cy.intercept('/api/v1/management/protocol-adapters/adapters', {
          items: [
            // ERROR adapter - shows red edges, error propagates to device nodes
            {
              ...mockAdapter_OPCUA,
              id: 'opcua-critical',
              status: {
                connection: Status.connection.ERROR,
                runtime: Status.runtime.STOPPED,
                message: 'Connection timeout: Unable to reach device at opc.tcp://192.168.1.100:4840',
              },
            },
            // ACTIVE operational adapter #1 - has mappings, shows green animated edges
            {
              ...mockAdapter_OPCUA,
              id: 'opcua-working',
              type: 'simulation',
              status: {
                connection: Status.connection.CONNECTED,
                runtime: Status.runtime.STARTED,
              },
            },
            // ACTIVE operational adapter #2 - different mappings, also animated
            {
              ...mockAdapter_OPCUA,
              id: 'opcua-working2',
              type: 'opcua',
              status: {
                connection: Status.connection.CONNECTED,
                runtime: Status.runtime.STARTED,
              },
            },
            // ACTIVE non-operational adapter - no mappings, shows green static edges
            {
              ...mockAdapter_OPCUA,
              id: 'modbus-idle',
              type: 'modbus',
              status: {
                connection: Status.connection.CONNECTED,
                runtime: Status.runtime.STARTED,
              },
            },
            // INACTIVE adapter - shows gray edges
            {
              ...mockAdapter_OPCUA,
              id: 's7-maintenance',
              type: 's7',
              status: {
                connection: Status.connection.DISCONNECTED,
                runtime: Status.runtime.STOPPED,
              },
            },
            // Another ERROR adapter to show multiple errors
            {
              ...mockAdapter_OPCUA,
              id: 'http-failed',
              type: 'http',
              status: {
                connection: Status.connection.ERROR,
                runtime: Status.runtime.STOPPED,
                message: 'HTTP 500: Internal server error',
              },
            },
          ],
        })

        // Mock mappings - varied operational states
        cy.intercept('/api/v1/management/protocol-adapters/adapters/opcua-critical/northboundMappings', {
          items: [], // ERROR adapter has no working mappings
        })

        cy.intercept('/api/v1/management/protocol-adapters/adapters/opcua-working/northboundMappings', {
          items: [
            { tagName: 'sensor-data', topic: 'production/data/stream' },
            { tagName: 'sensor-status', topic: 'production/status/monitor' },
          ], // Operational ACTIVE - will animate
        })

        cy.intercept('/api/v1/management/protocol-adapters/adapters/opcua-working2/northboundMappings', {
          items: [
            { tagName: 'temperature-sensor', topic: 'factory/temperature/zone1' },
            { tagName: 'pressure-sensor', topic: 'factory/pressure/zone1' },
            { tagName: 'flow-sensor', topic: 'factory/flow/line3' },
          ], // Different mappings - also operational ACTIVE
        })

        cy.intercept('/api/v1/management/protocol-adapters/adapters/modbus-idle/northboundMappings', {
          items: [], // ACTIVE but no mappings - green but not animated
        })

        cy.intercept('/api/v1/management/protocol-adapters/adapters/s7-maintenance/northboundMappings', {
          items: [], // INACTIVE - gray edges
        })

        cy.intercept('/api/v1/management/protocol-adapters/adapters/http-failed/northboundMappings', {
          items: [], // ERROR adapter
        })

        // Mock bridge with ACTIVE status and operational mappings
        cy.intercept('/api/v1/management/bridges', {
          items: [
            {
              ...mockBridge,
              id: 'backup-bridge',
              status: {
                connection: Status.connection.CONNECTED,
                runtime: Status.runtime.STARTED,
              },
              localSubscriptions: [{ filters: ['production/#'], destination: 'backup/production', maxQoS: 1 }],
              remoteSubscriptions: [{ filters: ['alerts/#'], destination: 'local/alerts', maxQoS: 0 }],
            },
          ],
        })

        // Mock Combiner connected to opcua-critical and opcua-working
        // Note: cy_interceptPulseWithMockDB already sets up combiners endpoint
        // This will override with our specific test data
        cy.intercept('/api/v1/management/combiners', {
          items: [
            {
              id: 'data-combiner-1',
              name: 'Production Data Aggregator',
              description: 'Combines critical and working adapter data',
              sources: {
                items: [
                  { type: 'ADAPTER', id: 'opcua-critical' },
                  { type: 'ADAPTER', id: 'opcua-working' },
                ],
              },
              mappings: {
                items: [
                  {
                    id: 'mapping-1',
                    sources: {
                      primary: { id: 'sensor-data', type: 'TAG' },
                      tags: ['sensor-data', 'sensor-status'],
                      topicFilters: [],
                    },
                    destination: { topic: 'combined/production/metrics' },
                    instructions: [],
                  },
                ],
              },
            },
          ],
        })

        // Wait for React Flow to render and all Pulse entities to load
        cy.get('.react-flow__viewport', { timeout: 10000 }).should('be.visible')

        // Wait for Pulse Agent node to appear (confirms Pulse is properly initialized)
        cy.get('[data-nodetype="PULSE_AGENT_NODE"]', { timeout: 10000 }).should('exist')

        // Fit the workspace to show all entities
        workspacePage.toolbox.fit.click()

        // Wait for layout to settle with all the complex entities
        cy.wait(2000)

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
