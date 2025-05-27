/// <reference types="cypress" />

import { MOCK_RANGE_OPTION } from '../utils/range-option.mocks.ts'
import OptionCommand from './OptionCommand.tsx'

describe('OptionBadge', () => {
  beforeEach(() => {
    cy.viewport(800, 400)
  })

  it('should render properly', () => {
    cy.mountWithProviders(<OptionCommand data={MOCK_RANGE_OPTION} />)

    cy.get('button').should('contain.text', MOCK_RANGE_OPTION.label).should('be.disabled')
  })
})
