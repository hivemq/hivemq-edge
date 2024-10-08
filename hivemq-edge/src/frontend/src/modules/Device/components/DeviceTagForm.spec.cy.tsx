/// <reference types="cypress" />

import DeviceTagForm from '@/modules/Device/components/DeviceTagForm.tsx'
import {
  MOCK_DEVICE_TAGS,
  mockAdapter_OPCUA,
  mockProtocolAdapter_OPCUA,
} from '@/api/hooks/useProtocolAdapters/__handlers__'
import { DomainTagList } from '@/api/__generated__'

describe('AdapterSubscriptionManager', () => {
  beforeEach(() => {
    cy.viewport(800, 800)
  })

  it('should render the errors', () => {
    cy.intercept('/api/v1/management/protocol-adapters/types', { statusCode: 404 })
    cy.intercept('/api/v1/management/protocol-adapters/adapters', { statusCode: 404 })
    cy.intercept('/api/v1/management/protocol-adapters/adapters/*/tags?*', { statusCode: 404 })

    cy.mountWithProviders(<DeviceTagForm adapterId="my-id" adapterType="my-type" />, {
      routerProps: { initialEntries: [`/node/wrong-adapter`] },
    })
    cy.getByTestId('loading-spinner').should('be.visible')

    cy.get('[role="alert"')
      .should('have.attr', 'data-status', 'error')
      .should('contain.text', 'We cannot load your adapters for the time being. Please try again later')
  })

  it.only('should render the form', () => {
    const mockProtocolAdapter_OPCUA_ID = mockProtocolAdapter_OPCUA?.id as string
    const mockAdapter_OPCUA_ID = mockAdapter_OPCUA.id as string
    cy.intercept('/api/v1/management/protocol-adapters/types', { items: [mockProtocolAdapter_OPCUA] }).as(
      'getProtocols'
    )
    cy.intercept('/api/v1/management/protocol-adapters/adapters', { items: [mockAdapter_OPCUA] }).as('getAdapters')
    const mockResponse: DomainTagList = {
      items: MOCK_DEVICE_TAGS(mockProtocolAdapter_OPCUA_ID, mockAdapter_OPCUA_ID),
    }
    cy.intercept('/api/v1/management/protocol-adapters/adapters/*/tags*', mockResponse).as('getTags')

    cy.mountWithProviders(<DeviceTagForm adapterId={mockAdapter_OPCUA_ID} adapterType={mockProtocolAdapter_OPCUA_ID} />)
    cy.getByTestId('loading-spinner').should('be.visible')

    cy.wait(['@getProtocols', '@getAdapters', '@getTags'])

    cy.get('#root_tags__title').should('contain.text', 'List of tags')
    cy.get('#root_tags__title + p').should('contain.text', 'The list of all tags defined in the device')
    cy.get('#root_tags__title + p + [role="list"]').as('tagList').should('be.visible')
    cy.get('@tagList').find('[role="listitem"]').should('have.length', 1)

    cy.get('@tagList').eq(0).find('[role="toolbar"] button').eq(0).should('have.attr', 'aria-label', 'Remove')
    cy.get('#root_tags_0__title').should('have.text', 'tags-0')

    cy.get('@tagList').eq(0).find('[role="group"] > label').eq(0).should('contain.text', 'Tag Name')
    cy.get('@tagList')
      .eq(0)
      .find('[role="group"] > input')
      .eq(0)
      .should('contain.value', `${mockProtocolAdapter_OPCUA_ID}/log/event`)

    cy.get('#root_tags__title + p + [role="list"] + div button').should('contain.text', 'Add Item')
  })
})
