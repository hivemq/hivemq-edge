/// <reference types="cypress" />

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
    cy.intercept('/api/v1/management/protocol-adapters/types', { items: [mockProtocolAdapter] }).as('getProtocols')
    cy.intercept('api/v1/management/protocol-adapters/adapters', { items: [mockAdapter] }).as('getAdapters')

    cy.mountWithProviders(<EntityRenderer reference={mockEntityReference} />)

    cy.getByTestId('node-type-icon').should('exist').should('have.attr', 'data-nodeicon', NodeTypes.ADAPTER_NODE)
  })

  it('should be accessible', () => {
    cy.injectAxe()
    cy.mountWithProviders(<EntityRenderer reference={mockEntityReference} />)
    cy.checkAccessibility()
  })
})
