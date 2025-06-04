/// <reference types="cypress" />

import NodeIcon from './NodeIcon'
import { DataHubNodeType } from '../../types.ts'

describe('NodeIcon', () => {
  beforeEach(() => {
    cy.viewport(800, 250)
  })

  it('should renders properly', () => {
    cy.mountWithProviders(<NodeIcon type={DataHubNodeType.DATA_POLICY} />)
    cy.get('svg').should('have.attr', 'aria-label', 'Data Policy')
  })

  it('should renders even an unknown type ', () => {
    cy.mountWithProviders(<NodeIcon type={undefined} />)
    cy.get('svg').should('have.attr', 'aria-label', 'Unknown type')
  })
})
