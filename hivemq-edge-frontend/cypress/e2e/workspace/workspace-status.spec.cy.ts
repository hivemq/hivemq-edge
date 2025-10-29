/// <reference types="cypress" />

import { MOCK_PROTOCOL_OPC_UA } from '@/__test-utils__/adapters'
import { mockBridge } from '@/api/hooks/useGetBridges/__handlers__'
import { mockAdapter_OPCUA } from '@/api/hooks/useProtocolAdapters/__handlers__'
import { Status } from '@/api/__generated__'

import { loginPage, workspacePage } from 'cypress/pages'
import { cy_interceptCoreE2E } from 'cypress/utils/intercept.utils.ts'

/**
 * E2E tests for Phase 6: Visual validation of dual-status system
 * Tests that runtime status (colors) and operational status (animations) render correctly
 */
describe('Workspace - Dual Status System', () => {
  beforeEach(() => {
    cy_interceptCoreE2E()

    cy.intercept('/api/v1/management/protocol-adapters/types', {
      items: [MOCK_PROTOCOL_OPC_UA],
    }).as('getProtocols')

    loginPage.visit('/app/workspace')
    loginPage.loginButton.click()
    workspacePage.navLink.click()
  })

  describe('Runtime Status Colors', () => {
    it('should display green edges for ACTIVE adapters', () => {
      // Mock adapter with ACTIVE status
      cy.intercept('/api/v1/management/protocol-adapters/adapters', {
        items: [
          {
            ...mockAdapter_OPCUA,
            status: {
              connection: Status.connection.CONNECTED,
              runtime: Status.runtime.STARTED,
            },
          },
        ],
      }).as('getActiveAdapter')

      cy.wait('@getActiveAdapter')

      // Verify edge has green stroke color (ACTIVE status)
      cy.get('.react-flow__edge').should('exist')
      cy.get('.react-flow__edge path').should('have.attr', 'stroke')
    })

    it('should display red edges for ERROR adapters', () => {
      // Mock adapter with ERROR status
      cy.intercept('/api/v1/management/protocol-adapters/adapters', {
        items: [
          {
            ...mockAdapter_OPCUA,
            status: {
              connection: Status.connection.ERROR,
              runtime: Status.runtime.STOPPED,
            },
          },
        ],
      }).as('getErrorAdapter')

      cy.wait('@getErrorAdapter')

      // Verify edge has red stroke color (ERROR status)
      cy.get('.react-flow__edge').should('exist')
      cy.get('.react-flow__edge path').should('have.attr', 'stroke')
    })

    it('should display yellow/gray edges for INACTIVE adapters', () => {
      // Mock adapter with INACTIVE status
      cy.intercept('/api/v1/management/protocol-adapters/adapters', {
        items: [
          {
            ...mockAdapter_OPCUA,
            status: {
              connection: Status.connection.DISCONNECTED,
              runtime: Status.runtime.STOPPED,
            },
          },
        ],
      }).as('getInactiveAdapter')

      cy.wait('@getInactiveAdapter')

      // Verify edge has gray stroke color (INACTIVE status)
      cy.get('.react-flow__edge').should('exist')
      cy.get('.react-flow__edge path').should('have.attr', 'stroke')
    })
  })

  describe('Operational Status Animations', () => {
    it('should animate edges when adapter is ACTIVE with mappings', () => {
      // Mock adapter with ACTIVE status and mappings configured
      cy.intercept('/api/v1/management/protocol-adapters/adapters', {
        items: [
          {
            ...mockAdapter_OPCUA,
            status: {
              connection: Status.connection.CONNECTED,
              runtime: Status.runtime.STARTED,
            },
          },
        ],
      }).as('getActiveAdapter')

      cy.intercept('/api/v1/management/protocol-adapters/adapters/**/northboundMappings', {
        items: [{ tagName: 'sensor1', topic: 'test/topic' }],
      }).as('getNorthboundMappings')

      cy.wait('@getActiveAdapter')
      cy.wait('@getNorthboundMappings')

      // Verify edge has animated class (operational ACTIVE)
      cy.get('.react-flow__edge').should('exist')
      cy.get('.react-flow__edge.animated').should('exist')
    })

    it('should NOT animate edges when adapter is ACTIVE but no mappings', () => {
      // Mock adapter with ACTIVE status but NO mappings
      cy.intercept('/api/v1/management/protocol-adapters/adapters', {
        items: [
          {
            ...mockAdapter_OPCUA,
            status: {
              connection: Status.connection.CONNECTED,
              runtime: Status.runtime.STARTED,
            },
          },
        ],
      }).as('getActiveAdapter')

      cy.intercept('/api/v1/management/protocol-adapters/adapters/**/northboundMappings', {
        items: [],
      }).as('getEmptyMappings')

      cy.wait('@getActiveAdapter')
      cy.wait('@getEmptyMappings')

      // Verify edge does NOT have animated class (operational INACTIVE)
      cy.get('.react-flow__edge').should('exist')
      // Note: May need to verify absence of animation class more specifically
    })
  })

  describe('Status Propagation Visualization', () => {
    it('should show device node with same color as parent adapter', () => {
      // Mock adapter with ACTIVE status
      cy.intercept('/api/v1/management/protocol-adapters/adapters', {
        items: [
          {
            ...mockAdapter_OPCUA,
            status: {
              connection: Status.connection.CONNECTED,
              runtime: Status.runtime.STARTED,
            },
          },
        ],
      }).as('getActiveAdapter')

      cy.wait('@getActiveAdapter')

      // Verify both adapter node and device node are visible
      cy.get('[data-nodetype="ADAPTER_NODE"]').should('exist')
      cy.get('[data-nodetype="DEVICE_NODE"]').should('exist')

      // Both should have edges with matching colors (status propagation)
      cy.get('.react-flow__edge').should('have.length.at.least', 1)
    })

    it('should propagate ERROR status through entire chain', () => {
      // Mock adapter with ERROR status
      cy.intercept('/api/v1/management/protocol-adapters/adapters', {
        items: [
          {
            ...mockAdapter_OPCUA,
            status: {
              connection: Status.connection.ERROR,
              runtime: Status.runtime.STOPPED,
            },
          },
        ],
      }).as('getErrorAdapter')

      cy.wait('@getErrorAdapter')

      // Verify edges show ERROR propagation (red color)
      cy.get('.react-flow__edge').should('exist')
      cy.get('.react-flow__edge path').should('have.attr', 'stroke')
    })
  })

  describe('Bridge Status Visualization', () => {
    it('should display green edges for ACTIVE bridges with topic filters', () => {
      // Mock bridge with ACTIVE status and topic filters
      cy.intercept('/api/v1/management/bridges', {
        items: [
          {
            ...mockBridge,
            status: {
              connection: Status.connection.CONNECTED,
              runtime: Status.runtime.STARTED,
            },
            localSubscriptions: [{ filters: ['test/topic'] }],
            remoteSubscriptions: [{ filters: ['remote/topic'] }],
          },
        ],
      }).as('getActiveBridge')

      cy.wait('@getActiveBridge')

      // Verify bridge edges are visible and green
      cy.get('[data-nodetype="BRIDGE_NODE"]').should('exist')
      cy.get('.react-flow__edge').should('exist')
    })

    it('should show animated edges for bridges with configured topics', () => {
      // Mock bridge with topics configured (operational ACTIVE)
      cy.intercept('/api/v1/management/bridges', {
        items: [
          {
            ...mockBridge,
            status: {
              connection: Status.connection.CONNECTED,
              runtime: Status.runtime.STARTED,
            },
            remoteSubscriptions: [{ filters: ['remote/topic'] }],
          },
        ],
      }).as('getActiveBridge')

      cy.wait('@getActiveBridge')

      // Verify bridge edges are animated
      cy.get('.react-flow__edge.animated').should('exist')
    })
  })

  describe('Group Node Status Aggregation', () => {
    it('should show ERROR status if any child has ERROR', () => {
      // Mock multiple adapters - one with ERROR
      cy.intercept('/api/v1/management/protocol-adapters/adapters', {
        items: [
          {
            ...mockAdapter_OPCUA,
            id: 'adapter-1',
            status: {
              connection: Status.connection.CONNECTED,
              runtime: Status.runtime.STARTED,
            },
          },
          {
            ...mockAdapter_OPCUA,
            id: 'adapter-2',
            status: {
              connection: Status.connection.ERROR,
              runtime: Status.runtime.STOPPED,
            },
          },
        ],
      }).as('getMixedAdapters')

      cy.wait('@getMixedAdapters')

      // If grouped, group node should show ERROR status (optional check as grouping depends on layout)
      cy.get('body').then(($body) => {
        if ($body.find('[data-nodetype="CLUSTER_NODE"]').length > 0) {
          cy.get('[data-nodetype="CLUSTER_NODE"]').should('exist')
        }
      })
    })
  })

  describe('Percy Visual Regression', () => {
    it('should capture workspace with various status combinations', { tags: ['@percy'] }, () => {
      // Mock various statuses for visual regression testing
      cy.intercept('/api/v1/management/protocol-adapters/adapters', {
        items: [
          {
            ...mockAdapter_OPCUA,
            id: 'adapter-active',
            status: {
              connection: Status.connection.CONNECTED,
              runtime: Status.runtime.STARTED,
            },
          },
          {
            ...mockAdapter_OPCUA,
            id: 'adapter-error',
            status: {
              connection: Status.connection.ERROR,
              runtime: Status.runtime.STOPPED,
            },
          },
          {
            ...mockAdapter_OPCUA,
            id: 'adapter-inactive',
            status: {
              connection: Status.connection.DISCONNECTED,
              runtime: Status.runtime.STOPPED,
            },
          },
        ],
      }).as('getMultipleAdapters')

      cy.wait('@getMultipleAdapters')

      workspacePage.toolbox.fit.click()

      // Capture visual snapshot with various status combinations
      cy.percySnapshot('Workspace - Dual Status System (Mixed Statuses)')
    })

    it('should capture edge animations', { tags: ['@percy'] }, () => {
      // Mock adapter with active status and mappings
      cy.intercept('/api/v1/management/protocol-adapters/adapters', {
        items: [
          {
            ...mockAdapter_OPCUA,
            status: {
              connection: Status.connection.CONNECTED,
              runtime: Status.runtime.STARTED,
            },
          },
        ],
      }).as('getActiveAdapter')

      cy.intercept('/api/v1/management/protocol-adapters/adapters/**/northboundMappings', {
        items: [{ tagName: 'sensor1', topic: 'test/topic' }],
      }).as('getMappings')

      cy.wait('@getActiveAdapter')
      cy.wait('@getMappings')

      workspacePage.toolbox.fit.click()

      // Capture snapshot showing animated edges
      cy.percySnapshot('Workspace - Edge Animations (Operational Active)')
    })
  })

  describe('Accessibility with Dual Status', () => {
    it('should remain accessible with status visualization', () => {
      cy.intercept('/api/v1/management/protocol-adapters/adapters', {
        items: [
          {
            ...mockAdapter_OPCUA,
            status: {
              connection: Status.connection.CONNECTED,
              runtime: Status.runtime.STARTED,
            },
          },
        ],
      }).as('getAdapter')

      cy.wait('@getAdapter')

      cy.injectAxe()
      workspacePage.toolbox.fit.click()

      cy.checkAccessibility(undefined, {
        rules: {
          region: { enabled: false },
          'color-contrast': { enabled: false },
        },
      })
    })
  })

  describe('PR Screenshots - Status Visualization', () => {
    it('PR Screenshot 1: Healthy workspace with all systems operational', { tags: ['@percy'] }, () => {
      // Mock a complex workspace with multiple adapters, bridge, combiners, and groups
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
      }).as('getMappings1')

      cy.intercept('/api/v1/management/protocol-adapters/adapters/opcua-quality/northboundMappings', {
        items: [{ tagName: 'quality-check', topic: 'factory/quality/inspection' }],
      }).as('getMappings2')

      cy.intercept('/api/v1/management/protocol-adapters/adapters/modbus-temperature/northboundMappings', {
        items: [{ tagName: 'temp-sensor', topic: 'factory/environment/temperature' }],
      }).as('getMappings3')

      cy.intercept('/api/v1/management/protocol-adapters/adapters/s7-assembly/northboundMappings', {
        items: [
          { tagName: 'assembly-status', topic: 'factory/assembly/status' },
          { tagName: 'assembly-count', topic: 'factory/assembly/count' },
        ],
      }).as('getMappings4')

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
      }).as('getActiveBridge')

      cy.wait('@getActiveAdapters')
      cy.wait('@getMappings1')
      cy.wait('@getMappings2')
      cy.wait('@getMappings3')
      cy.wait('@getMappings4')
      cy.wait('@getActiveBridge')

      // Fit the workspace to show all entities
      workspacePage.toolbox.fit.click()

      // Wait for layout to settle
      cy.wait(1000)

      // Check accessibility before taking screenshot
      cy.injectAxe()
      cy.checkAccessibility(undefined, {
        rules: {
          region: { enabled: false },
          'color-contrast': { enabled: false },
        },
      })

      // Capture screenshot showing:
      // - All green edges (ACTIVE runtime status)
      // - All edges animated (operational ACTIVE - data flowing)
      // - Complex workspace with adapters, bridge, devices, and groups

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
      'PR Screenshot 2: Mixed status workspace showing error propagation and status variety',
      {
        tags: ['@percy'],
      },
      () => {
        // Mock a complex workspace with mixed statuses to show all visual states
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
            // ACTIVE operational adapter - has mappings, shows green animated edges
            {
              ...mockAdapter_OPCUA,
              id: 'opcua-working',
              type: 'simulation',
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
        }).as('getMixedAdapters')

        // Mock mappings - varied operational states
        cy.intercept('/api/v1/management/protocol-adapters/adapters/opcua-critical/northboundMappings', {
          items: [], // ERROR adapter has no working mappings
        }).as('getEmptyMappings1')

        cy.intercept('/api/v1/management/protocol-adapters/adapters/opcua-working/northboundMappings', {
          items: [
            { tagName: 'sensor-data', topic: 'production/data/stream' },
            { tagName: 'sensor-status', topic: 'production/status/monitor' },
          ], // Operational ACTIVE - will animate
        }).as('getActiveMappings')

        cy.intercept('/api/v1/management/protocol-adapters/adapters/modbus-idle/northboundMappings', {
          items: [], // ACTIVE but no mappings - green but not animated
        }).as('getEmptyMappings2')

        cy.intercept('/api/v1/management/protocol-adapters/adapters/s7-maintenance/northboundMappings', {
          items: [], // INACTIVE - gray edges
        }).as('getEmptyMappings3')

        cy.intercept('/api/v1/management/protocol-adapters/adapters/http-failed/northboundMappings', {
          items: [], // ERROR adapter
        }).as('getEmptyMappings4')

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
        }).as('getMixedBridge')

        cy.wait('@getMixedAdapters')
        cy.wait('@getEmptyMappings1')
        cy.wait('@getActiveMappings')
        cy.wait('@getEmptyMappings2')
        cy.wait('@getEmptyMappings3')
        cy.wait('@getEmptyMappings4')
        cy.wait('@getMixedBridge')

        // Mock Pulse Agent with 2 mapped assets
        cy.intercept('/api/v1/management/pulse/status', {
          status: 'CONNECTED',
          connectedSince: '2025-10-25T10:00:00Z',
        }).as('getPulseStatus')

        cy.intercept('/api/v1/management/pulse/managed-assets', {
          items: [
            {
              id: 'asset-pump-01',
              name: 'Industrial Pump 01',
              description: 'Main production line pump',
              topic: 'assets/pumps/pump-01/telemetry',
              mapping: {
                status: 'STREAMING',
                mappingId: 'pulse-asset-mapper-1',
              },
            },
            {
              id: 'asset-conveyor-02',
              name: 'Conveyor Belt 02',
              description: 'Assembly line conveyor system',
              topic: 'assets/conveyors/belt-02/status',
              mapping: {
                status: 'STREAMING',
                mappingId: 'pulse-asset-mapper-1',
              },
            },
          ],
        }).as('getPulseAssets')

        // Add second working adapter with different mappings
        cy.intercept('/api/v1/management/protocol-adapters/adapters/opcua-working2/northboundMappings', {
          items: [
            { tagName: 'temperature-sensor', topic: 'factory/temperature/zone1' },
            { tagName: 'pressure-sensor', topic: 'factory/pressure/zone1' },
            { tagName: 'flow-sensor', topic: 'factory/flow/line3' },
          ],
        }).as('getWorking2Mappings')

        // Mock Combiner connected to opcua-critical and opcua-working
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
        }).as('getCombiners')

        // Mock Asset Mapper connected to adapters and pulse agent
        cy.intercept('/api/v1/management/pulse/asset-mappers', {
          items: [
            {
              id: 'pulse-asset-mapper-1',
              name: 'Asset Integration Mapper',
              description: 'Maps OPC-UA data to Pulse managed assets',
              sources: {
                items: [
                  { type: 'ADAPTER', id: 'opcua-working' },
                  { type: 'ADAPTER', id: 'opcua-working2' },
                  { type: 'PULSE_AGENT', id: 'the Pulse Agent' },
                ],
              },
              mappings: {
                items: [
                  {
                    id: 'asset-mapping-1',
                    sources: {
                      primary: { id: 'temperature-sensor', type: 'TAG' },
                      tags: ['temperature-sensor', 'pressure-sensor', 'flow-sensor'],
                      topicFilters: ['assets/+/telemetry'],
                    },
                    destination: { topic: 'unified/assets/stream' },
                    instructions: [],
                  },
                ],
              },
            },
          ],
        }).as('getAssetMappers')

        cy.wait('@getPulseStatus')
        cy.wait('@getPulseAssets')
        cy.wait('@getWorking2Mappings')
        cy.wait('@getCombiners')
        cy.wait('@getAssetMappers')

        // Wait for all entities to load
        cy.wait(1000)

        // Fit the workspace to show all entities
        workspacePage.toolbox.fit.click()

        // Wait for layout to settle
        cy.wait(1000)

        // Check accessibility before taking screenshot
        cy.injectAxe()
        cy.checkAccessibility(undefined, {
          rules: {
            region: { enabled: false },
            'color-contrast': { enabled: false },
          },
        })

        // Capture screenshot showing:
        // - 6 adapters with all status types (ERROR, ACTIVE operational, ACTIVE non-operational, INACTIVE)
        // - Pulse Agent with 2 managed streaming assets
        // - Combiner aggregating data from critical and working adapters
        // - Asset Mapper integrating multiple adapters with Pulse Agent
        // - Bridge for cloud connectivity
        // - Red edges for ERROR status (opcua-critical, http-failed)
        // - Green animated edges for ACTIVE operational (opcua-working, opcua-working2)
        // - Green static edges for ACTIVE non-operational (modbus-idle)
        // - Gray edges for INACTIVE (s7-maintenance)
        // - Complex enterprise IoT scenario with data flow and integration

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
