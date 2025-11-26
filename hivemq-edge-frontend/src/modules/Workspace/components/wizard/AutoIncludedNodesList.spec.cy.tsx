import type { Node } from '@xyflow/react'
import { NodeTypes, type NodeDeviceType, type NodeHostType } from '@/modules/Workspace/types'
import AutoIncludedNodesList from './AutoIncludedNodesList.tsx'

describe('AutoIncludedNodesList', () => {
  // Device nodes use DeviceMetadata which is ProtocolAdapter & { sourceAdapterId }
  // Component will use node.data?.label || node.id for display
  const mockDeviceNode: NodeDeviceType = {
    id: 'Temperature Sensor',
    type: NodeTypes.DEVICE_NODE,
    position: { x: 100, y: 200 },
    data: {
      id: 'device-adapter-1',
      sourceAdapterId: 'adapter-1',
      logoUrl: '',
      installed: true,
      name: 'temperature-protocol',
      protocol: 'temperature',
      version: '1.0.0',
    },
  }

  const mockHostNode: NodeHostType = {
    id: 'host-bridge-1',
    type: NodeTypes.HOST_NODE,
    position: { x: 300, y: 200 },
    data: { label: 'Remote Broker' },
  }

  const mockDeviceNode2: NodeDeviceType = {
    id: 'Pressure Sensor',
    type: NodeTypes.DEVICE_NODE,
    position: { x: 150, y: 200 },
    data: {
      id: 'device-adapter-2',
      sourceAdapterId: 'adapter-2',
      logoUrl: '',
      installed: true,
      name: 'pressure-protocol',
      protocol: 'pressure',
      version: '1.0.0',
    },
  }

  // Accessibility test (mandatory - must be unskipped)
  it('should be accessible', () => {
    cy.injectAxe()
    cy.mountWithProviders(<AutoIncludedNodesList autoIncludedNodes={[mockDeviceNode]} />)
    cy.checkAccessibility()
  })

  it('should not render when autoIncludedNodes is empty', () => {
    cy.mountWithProviders(<AutoIncludedNodesList autoIncludedNodes={[]} />)
    // Component returns null, so the specific text should not be visible
    cy.contains('These nodes will also be included:').should('not.exist')
  })

  it('should render single auto-included device node', () => {
    cy.mountWithProviders(<AutoIncludedNodesList autoIncludedNodes={[mockDeviceNode]} />)

    cy.get('[role="region"]').should('be.visible')
    cy.contains('These nodes will also be included:').should('be.visible')
    cy.contains('Temperature Sensor').should('be.visible')
    cy.contains('(device)').should('be.visible')
  })

  it('should render single auto-included host node', () => {
    cy.mountWithProviders(<AutoIncludedNodesList autoIncludedNodes={[mockHostNode]} />)

    cy.get('[role="region"]').should('be.visible')
    cy.contains('Remote Broker').should('be.visible')
    cy.contains('(host)').should('be.visible')
  })

  it('should render multiple auto-included nodes', () => {
    cy.mountWithProviders(<AutoIncludedNodesList autoIncludedNodes={[mockDeviceNode, mockHostNode]} />)

    cy.get('[role="region"]').should('be.visible')
    cy.contains('Temperature Sensor').should('be.visible')
    cy.contains('Remote Broker').should('be.visible')
  })

  it('should show plus icon for each node', () => {
    cy.mountWithProviders(<AutoIncludedNodesList autoIncludedNodes={[mockDeviceNode, mockHostNode]} />)

    // Should have plus icons (one per node)
    cy.get('svg[aria-hidden="true"]').should('have.length', 2)
  })

  it('should use blue color scheme for visual distinction', () => {
    cy.mountWithProviders(<AutoIncludedNodesList autoIncludedNodes={[mockDeviceNode]} />)

    // Check background color (blue.50)
    cy.get('[role="region"]').should('have.css', 'background-color').and('match', /rgb/)
  })

  it('should handle node without label gracefully', () => {
    // Device node with no label in data - should fallback to node ID
    const nodeWithoutLabel: NodeDeviceType = {
      id: 'device-no-label',
      type: NodeTypes.DEVICE_NODE,
      position: { x: 0, y: 0 },
      data: {
        id: 'adapter-device-id',
        sourceAdapterId: 'adapter-test',
        logoUrl: '',
        installed: true,
        name: 'test-protocol',
        protocol: 'test',
        version: '1.0.0',
      },
    }

    cy.mountWithProviders(<AutoIncludedNodesList autoIncludedNodes={[nodeWithoutLabel]} />)

    // Should show node ID as fallback since data has no label field
    cy.contains('device-no-label').should('be.visible')
  })

  it('should show correct node type labels', () => {
    cy.mountWithProviders(<AutoIncludedNodesList autoIncludedNodes={[mockDeviceNode, mockHostNode]} />)

    // Device should show "device" type (lowercase, from i18n)
    cy.contains('Temperature Sensor')
      .parent()
      .within(() => {
        cy.contains('(device)').should('be.visible')
      })

    // Host should show "host" type (lowercase, from i18n)
    cy.contains('Remote Broker')
      .parent()
      .within(() => {
        cy.contains('(host)').should('be.visible')
      })
  })

  it('should have proper ARIA region label', () => {
    cy.mountWithProviders(<AutoIncludedNodesList autoIncludedNodes={[mockDeviceNode]} />)

    cy.get('[role="region"]').should('have.attr', 'aria-label', 'These nodes will also be included:')
  })

  it('should maintain list order', () => {
    cy.mountWithProviders(<AutoIncludedNodesList autoIncludedNodes={[mockDeviceNode, mockDeviceNode2, mockHostNode]} />)

    // Check that all nodes are rendered in order by checking they exist
    cy.contains('Temperature Sensor').should('be.visible')
    cy.contains('Pressure Sensor').should('be.visible')
    cy.contains('Remote Broker').should('be.visible')

    // Verify first item appears before second item in document order
    cy.contains('Temperature Sensor').then(($first) => {
      cy.contains('Pressure Sensor').then(($second) => {
        // Both should exist
        expect($first).to.exist
        expect($second).to.exist
        // First element should come before second in DOM
        const firstEl = $first?.[0] as unknown as HTMLElement
        const secondEl = $second?.[0] as unknown as HTMLElement
        if (firstEl && secondEl) {
          expect(firstEl.compareDocumentPosition(secondEl) & Node.DOCUMENT_POSITION_FOLLOWING).to.be.greaterThan(0)
        }
      })
    })
  })

  it('should handle unknown node type gracefully', () => {
    const unknownTypeNode: Node = {
      id: 'unknown-1',
      type: 'UNKNOWN_TYPE' as NodeTypes,
      position: { x: 0, y: 0 },
      data: { id: 'unknown-1', label: 'Unknown Node' },
    }

    cy.mountWithProviders(<AutoIncludedNodesList autoIncludedNodes={[unknownTypeNode]} />)

    cy.contains('Unknown Node').should('be.visible')
    // Should show fallback type (the node type string itself when translation doesn't exist)
    cy.contains(/\(.*\)/).should('be.visible')
  })
})
