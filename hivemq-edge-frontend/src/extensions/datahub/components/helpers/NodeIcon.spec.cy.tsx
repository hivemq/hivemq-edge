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

  it('should forward data-testid prop', () => {
    cy.mountWithProviders(<NodeIcon type={DataHubNodeType.DATA_POLICY} data-testid="test-icon" />)
    cy.get('[data-testid="test-icon"]').should('exist')
    cy.get('[data-testid="test-icon"]').should('have.attr', 'aria-label', 'Data Policy')
  })

  it('should forward other Icon props like color', () => {
    cy.mountWithProviders(<NodeIcon type={DataHubNodeType.SCHEMA} color="red.500" data-testid="colored-icon" />)
    cy.get('[data-testid="colored-icon"]').should('exist')
  })
})
