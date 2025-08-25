/// <reference types="cypress" />

import type { EntityReference } from '@/api/__generated__'
import { EntityType } from '@/api/__generated__'
import { mockBridge, mockBridgeId } from '@/api/hooks/useGetBridges/__handlers__'
import { EntityRenderer } from './EntityRenderer'
import { mockEntityReference } from '@/api/hooks/useCombiners/__handlers__'
import { mockAdapter, mockProtocolAdapter } from '@/api/hooks/useProtocolAdapters/__handlers__'
import { NodeTypes } from '@/modules/Workspace/types'

describe('EntityRenderer', () => {
  beforeEach(() => {
    cy.viewport(800, 800)
  })

  it('should render an error', () => {
    cy.intercept('/api/v1/management/protocol-adapters/types', { statusCode: 404 })
    cy.intercept('api/v1/management/protocol-adapters/adapters', { statusCode: 404 })
    cy.mountWithProviders(<EntityRenderer reference={mockEntityReference} />)

    cy.get('[role="alert"]').should('contain.text', 'This is not a valid reference to a Workspace entity')
    cy.get('[role="alert"]').should('have.attr', 'data-status', 'error')
  })

  it('should render an adapter', () => {
    cy.intercept('/api/v1/management/protocol-adapters/types', { items: [mockProtocolAdapter] })
    cy.intercept('api/v1/management/protocol-adapters/adapters', { items: [mockAdapter] })
    cy.mountWithProviders(<EntityRenderer reference={mockEntityReference} />)

    cy.getByTestId('node-type-icon').should('exist').should('have.attr', 'data-nodeicon', NodeTypes.ADAPTER_NODE)
    cy.getByTestId('node-name').should('have.text', 'my-adapter')
    cy.getByTestId('node-description').should('have.text', 'Simulated Edge Device')
  })

  it('should render an bridge', () => {
    cy.intercept('/api/v1/management/bridges/*', mockBridge)
    const mockBridgeEntityReference: EntityReference = {
      type: EntityType.BRIDGE,
      id: mockBridgeId,
    }
    cy.mountWithProviders(<EntityRenderer reference={mockBridgeEntityReference} />)

    cy.getByTestId('node-type-icon').should('exist').should('have.attr', 'data-nodeicon', NodeTypes.BRIDGE_NODE)
    cy.getByTestId('node-name').should('have.text', 'bridge-id-01')
  })

  it('should render an Edge broker', () => {
    const mockEdgeEntityReference: EntityReference = {
      type: EntityType.EDGE_BROKER,
      id: mockBridgeId,
    }
    cy.mountWithProviders(<EntityRenderer reference={mockEdgeEntityReference} />)

    cy.getByTestId('node-type-icon').should('exist').should('have.attr', 'data-nodeicon', NodeTypes.EDGE_NODE)
    cy.getByTestId('node-name').should('have.text', 'HiveMQ Edge')
    cy.getByTestId('node-description').should('have.text', 'Always added as the owner of the topic filters')
  })

  it('should render a Pulse Agent', () => {
    const mockPulseEntityReference: EntityReference = {
      // TODO[35769] This is a hack; PULSE_AGENT needs to be supported as a valid EntityType
      type: EntityType.DEVICE,
      id: mockBridgeId,
    }
    cy.mountWithProviders(<EntityRenderer reference={mockPulseEntityReference} />)

    cy.getByTestId('node-type-icon').should('exist').should('have.attr', 'data-nodeicon', NodeTypes.PULSE_NODE)
    cy.getByTestId('node-name').should('have.text', 'Pulse Agent')
    cy.getByTestId('node-description').should('have.text', 'Assets managed by the Pulse Agent')
  })

  it('should be accessible', () => {
    cy.intercept('/api/v1/management/protocol-adapters/types', { items: [mockProtocolAdapter] })
    cy.intercept('api/v1/management/protocol-adapters/adapters', { items: [mockAdapter] })
    cy.injectAxe()
    cy.mountWithProviders(<EntityRenderer reference={mockEntityReference} />)
    cy.checkAccessibility()
  })
})
