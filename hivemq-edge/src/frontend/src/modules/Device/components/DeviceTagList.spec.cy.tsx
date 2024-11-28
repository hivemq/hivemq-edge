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
    cy.intercept('/api/v1/management/protocol-adapters/types', { items: [mockProtocolAdapter_OPCUA] }).as(
      'getProtocols'
    )
    cy.intercept('/api/v1/management/protocol-adapters/adapters', { items: [mockAdapter_OPCUA] }).as('getAdapters')
  })

  it('should render the errors', () => {
    cy.intercept('/api/v1/management/protocol-adapters/adapters/*/tags?*', { statusCode: 404 }).as('getTags')

    cy.mountWithProviders(<DeviceTagList adapter={mockAdapter_OPCUA} />)
    cy.getByTestId('loading-spinner').should('be.visible')
    cy.get('[role="alert"').should('have.attr', 'data-status', 'error').should('contain.text', 'Cannot load the tags')
    cy.getByAriaLabel('Edit tags').should('be.visible').should('be.disabled')
  })

  it('should render an empty list', () => {
    cy.intercept('/api/v1/management/protocol-adapters/adapters/*/tags', { items: [] }).as('getTags')
    cy.intercept('/api/v1/management/protocol-adapters/tagschemas/opcua', MOCK_DEVICE_TAG_JSON_SCHEMA_OPCUA)

    cy.mountWithProviders(<DeviceTagList adapter={mockAdapter_OPCUA} />)
    cy.getByTestId('loading-spinner').should('be.visible')

    cy.get('[role="alert"')
      .should('have.attr', 'data-status', 'info')
      .should('contain.text', 'There is no tag created yet')

    cy.getByAriaLabel('Edit tags').should('be.visible').should('not.be.disabled')
  })

  it('should render properly', () => {
    cy.intercept('/api/v1/management/protocol-adapters/adapters/*/tags', {
      items: MOCK_DEVICE_TAGS('opcua-1', MockAdapterType.OPC_UA),
    }).as('getTags')
    cy.intercept('/api/v1/management/protocol-adapters/tagschemas/opcua', MOCK_DEVICE_TAG_JSON_SCHEMA_OPCUA)

    cy.mountWithProviders(<DeviceTagList adapter={mockAdapter_OPCUA} />)
    cy.getByTestId('loading-spinner').should('be.visible')

    cy.get('h2').should('have.text', 'List of Device Tags')
    cy.getByAriaLabel('Edit tags').should('be.visible').should('not.be.disabled')

    cy.getByTestId('device-tags-list').should('be.visible')
    cy.getByTestId('device-tags-list').find('li').should('have.length', 2)
  })

  it('should be accessible', () => {
    cy.intercept('/api/v1/management/protocol-adapters/adapters/*/tags', {
      items: MOCK_DEVICE_TAGS('opcua-1', MockAdapterType.OPC_UA),
    }).as('getTags')
    cy.intercept('/api/v1/management/protocol-adapters/tagschemas/opcua', MOCK_DEVICE_TAG_JSON_SCHEMA_OPCUA)

    cy.mountWithProviders(<DeviceTagList adapter={mockAdapter_OPCUA} />)
    cy.injectAxe()
    cy.checkAccessibility()
  })
})
