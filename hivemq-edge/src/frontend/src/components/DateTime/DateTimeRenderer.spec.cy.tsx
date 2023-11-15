/// <reference types="cypress" />

import DateTimeRenderer from '@/components/DateTime/DateTimeRenderer.tsx'
import { MOCK_DATE_TIME_NOW } from './utils/range-option.mocks.ts'

describe('DateTimeRenderer', () => {
  beforeEach(() => {
    cy.viewport(800, 400)
    cy.window().then((win) => {
      Object.defineProperty(win.navigator, 'language', { value: 'en-GB' })
    })
  })

  it('should render properly', () => {
    const now = MOCK_DATE_TIME_NOW.plus({ day: 2 }).toJSDate()

    cy.clock(now)
    cy.mountWithProviders(<DateTimeRenderer date={MOCK_DATE_TIME_NOW} isApprox />)
    cy.getByTestId('date-time-approx').should('contain.text', '2 days ago')
  })

  it('should open the tooltip when mouseover', () => {
    const now = MOCK_DATE_TIME_NOW.plus({ day: 2 }).toJSDate()

    // cy.clock(now)
    cy.mountWithProviders(<DateTimeRenderer date={MOCK_DATE_TIME_NOW} isApprox />)

    // mouseover doesn't trigger the tooltip; force a click
    cy.get('p').click({ force: true })
    cy.clock(now)
    cy.getByTestId('date-time-tooltip').should('contain.text', '10 November 2023 at 00:00:00.000')
  })

  it('should render the plain date properly', () => {
    const now = MOCK_DATE_TIME_NOW.plus({ day: 2 }).toJSDate()

    cy.clock(now)
    cy.mountWithProviders(<DateTimeRenderer date={MOCK_DATE_TIME_NOW} />)
    cy.getByTestId('date-time-approx').should('not.exist')
    cy.getByTestId('date-time-full').should('contain.text', 'Friday, 10 November 2023 at 00:00:00.000')
  })
})
