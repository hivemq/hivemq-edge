/// <reference types="cypress" />

import { ToolbarPublish } from '@datahub/components/toolbar/ToolbarPublish.tsx'

describe('ToolbarPublish', () => {
  beforeEach(() => {
    cy.viewport(800, 250)
  })

  it('should renders properly', () => {
    cy.mountWithProviders(<ToolbarPublish />)
    cy.get('button').should('have.text', 'Publish')
  })
})
