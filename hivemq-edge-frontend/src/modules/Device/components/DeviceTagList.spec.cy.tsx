/// <reference types="cypress" />

import {
  MOCK_DEVICE_TAG_JSON_SCHEMA_OPCUA,
  MOCK_DEVICE_TAGS,
  mockAdapter_OPCUA,
  mockProtocolAdapter_OPCUA,
} from '@/api/hooks/useProtocolAdapters/__handlers__'
import DeviceTagList from '@/modules/Device/components/DeviceTagList.tsx'
import { MockAdapterType } from '@/__test-utils__/adapters/types.ts'

describe('DeviceTagList', () => {
  beforeEach(() => {
    cy.viewport(800, 800)
    cy.intercept('/api/v1/management/protocol-adapters/types', { items: [mockProtocolAdapter_OPCUA] })
    cy.intercept('/api/v1/management/protocol-adapters/adapters', { items: [mockAdapter_OPCUA] })
    cy.intercept('/api/v1/management/protocol-adapters/tag-schemas/**', { statusCode: 203, log: false })
    cy.intercept('/api/v1/management/protocol-adapters/adapters/*/tags?*', { statusCode: 203, log: false })
  })

  it('should render the errors', () => {
    cy.intercept('/api/v1/management/protocol-adapters/adapters/*/tags?*', { statusCode: 404 })

    cy.mountWithProviders(<DeviceTagList adapter={mockAdapter_OPCUA} />)
    cy.getByTestId('loading-spinner').should('be.visible')
    cy.get('[role="alert"]').should('have.attr', 'data-status', 'error').should('contain.text', 'Cannot load the tags')
  })

  it('should render an empty list', () => {
    cy.intercept('/api/v1/management/protocol-adapters/adapters/*/tags', { items: [] })
    cy.intercept('/api/v1/management/protocol-adapters/tag-schemas/opcua', MOCK_DEVICE_TAG_JSON_SCHEMA_OPCUA)

    cy.mountWithProviders(<DeviceTagList adapter={mockAdapter_OPCUA} />)
    cy.getByTestId('loading-spinner').should('be.visible')

    cy.get('table[aria-label="List of tags"]').within(() => {
      cy.get('[role="alert"]')
        .should('have.attr', 'data-status', 'info')
        .should('contain.text', 'There is no tag created yet')
    })
  })

  it('should render properly', () => {
    cy.intercept('/api/v1/management/protocol-adapters/adapters/*/tags', {
      items: MOCK_DEVICE_TAGS('opcua-1', MockAdapterType.OPC_UA),
    })
    cy.intercept('/api/v1/management/protocol-adapters/tag-schemas/opcua', MOCK_DEVICE_TAG_JSON_SCHEMA_OPCUA)

    cy.mountWithProviders(<DeviceTagList adapter={mockAdapter_OPCUA} />)
    cy.getByTestId('loading-spinner').should('be.visible')

    cy.get('h2').should('have.text', 'List of tags')

    cy.get('table[aria-label="List of tags"]').within(() => {
      cy.get('tbody tr').should('have.length', 2)
    })
  })

  it('should be accessible', () => {
    cy.intercept('/api/v1/management/protocol-adapters/adapters/*/tags', {
      items: MOCK_DEVICE_TAGS('opcua-1', MockAdapterType.OPC_UA),
    })
    cy.intercept('/api/v1/management/protocol-adapters/tag-schemas/opcua', MOCK_DEVICE_TAG_JSON_SCHEMA_OPCUA)

    cy.mountWithProviders(<DeviceTagList adapter={mockAdapter_OPCUA} />)
    cy.injectAxe()
    cy.checkAccessibility()
  })
})
