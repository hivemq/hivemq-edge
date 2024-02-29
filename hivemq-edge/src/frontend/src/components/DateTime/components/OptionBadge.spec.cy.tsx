/// <reference types="cypress" />

import OptionBadge from './OptionBadge.tsx'
import { MOCK_RANGE_OPTION } from '../utils/range-option.mocks.ts'

describe('OptionBadge', () => {
  beforeEach(() => {
    cy.viewport(800, 400)
  })

  it('should render properly', () => {
    cy.mountWithProviders(<OptionBadge data={MOCK_RANGE_OPTION} />)

    cy.getByTestId(`dateRange-option-badge-${MOCK_RANGE_OPTION.value}`)
      .should('contain.text', '1h')
      .should('have.attr', 'data-group', MOCK_RANGE_OPTION.colorScheme)
  })

  it('should render properly', () => {
    cy.mountWithProviders(<OptionBadge data={{ ...MOCK_RANGE_OPTION, duration: undefined }} />)

    cy.getByTestId(`dateRange-option-badge-${MOCK_RANGE_OPTION.value}`).should('not.exist')
  })
})
