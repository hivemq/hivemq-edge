/// <reference types="cypress" />

import { DataHubNodeType } from '@datahub/types.ts'
import ToolItem from './ToolItem.tsx'

describe('Tool', () => {
  beforeEach(() => {
    cy.viewport(800, 600)
  })

  it('should render function', () => {
    cy.mountWithProviders(<ToolItem nodeType={DataHubNodeType.FUNCTION} />)

    cy.get('button').should('have.attr', 'aria-label', 'JS Function')
    cy.get('button').should('have.attr', 'draggable', 'true')
  })

  it('should render data policy', () => {
    cy.mountWithProviders(<ToolItem nodeType={DataHubNodeType.DATA_POLICY} />)

    cy.get('button').should('have.attr', 'aria-label', 'Data Policy')
    cy.get('button').should('have.attr', 'draggable', 'true')
  })
})
