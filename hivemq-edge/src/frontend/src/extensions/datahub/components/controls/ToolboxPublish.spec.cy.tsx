/// <reference types="cypress" />

import { ToolboxPublish } from '@datahub/components/controls/ToolboxPublish.tsx'

describe('ToolboxPublish', () => {
  beforeEach(() => {
    cy.viewport(800, 250)
  })

  it('should renders properly', () => {
    cy.mountWithProviders(<ToolboxPublish />)
    cy.get('button').should('have.text', 'Publish')
  })
})
