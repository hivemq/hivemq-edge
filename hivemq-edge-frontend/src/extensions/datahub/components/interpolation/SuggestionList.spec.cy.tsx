/// <reference types="cypress" />

import type { MentionNodeAttrs, SuggestionListProps } from '@datahub/components/interpolation/SuggestionList.tsx'
import SuggestionList from '@datahub/components/interpolation/SuggestionList.tsx'

const mockItems: MentionNodeAttrs[] = [
  {
    id: '0',
    label: 'clientId',
  },
  {
    id: '1',
    label: 'policyId',
  },
]

const mockSuggestionListProps: SuggestionListProps = {
  // @ts-ignore
  editor: undefined,
}

describe('SuggestionList', () => {
  beforeEach(() => {
    cy.viewport(400, 600)
  })

  it('should render the list of variables', () => {
    cy.mountWithProviders(<SuggestionList {...mockSuggestionListProps} items={mockItems} />)
    cy.getByTestId('interpolation-container').should('be.visible')
    cy.getByTestId('interpolation-container').find('button').as('getVariables')
    cy.get('@getVariables').should('have.length', 2)
    cy.get('@getVariables').eq(0).should('contain.text', 'clientId')
    cy.get('@getVariables').eq(1).should('contain.text', 'policyId')
  })

  it('should select the variable', () => {
    cy.mountWithProviders(
      <SuggestionList {...mockSuggestionListProps} items={mockItems} command={cy.stub().as('onInsert')} />
    )
    cy.getByTestId('interpolation-container').find('button').as('getVariables')
    cy.get('@getVariables').should('have.length', 2)
    cy.get('@getVariables').eq(0).click()
    cy.get('@onInsert').should('be.calledWith', {
      id: '0',
      label: 'clientId',
    })
  })

  it('should be accessible', () => {
    cy.injectAxe()

    cy.mountWithProviders(<SuggestionList {...mockSuggestionListProps} items={mockItems} />)

    cy.checkAccessibility()
  })
})
