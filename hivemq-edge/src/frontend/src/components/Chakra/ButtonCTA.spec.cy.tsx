/// <reference types="cypress" />

import ButtonCTA from './ButtonCTA.tsx'
import { hexToRgb } from '@/__test-utils__/color.ts'

const MOCK_TITLE = 'Primary CTA Button'

describe('ButtonCTA', () => {
  beforeEach(() => {
    cy.viewport(400, 150)
  })

  it('should be accessible', () => {
    cy.injectAxe()
    cy.mountWithProviders(<ButtonCTA>{MOCK_TITLE}</ButtonCTA>)
    cy.checkAccessibility()
    cy.percySnapshot('Component: Primary CTA Button')
  })

  it('should render properly', () => {
    cy.mountWithProviders(<ButtonCTA data-testid={'button-cta'}>{MOCK_TITLE}</ButtonCTA>)

    // This might be an overkill; rely solely on Percy ?
    cy.getByTestId('button-cta').then((e) => {
      // @ts-ignore
      const win = cy.state('window')
      const styles = win.getComputedStyle(e[0])
      cy.wrap(e).should('have.css', 'font-weight', styles.getPropertyValue('--chakra-fontWeights-semibold'))
      cy.wrap(e).should(
        'have.css',
        'background-color',
        hexToRgb(styles.getPropertyValue('--chakra-colors-yellow-400'), true)
      )
    })
  })

  it('should be clickable', () => {
    const mockOnClick = cy.stub().as('setName')

    cy.mountWithProviders(
      <ButtonCTA data-testid={'button-cta'} onClick={mockOnClick}>
        {MOCK_TITLE}
      </ButtonCTA>
    )

    cy.getByTestId('button-cta').click()
    cy.get('@setName').should('have.been.calledOnce')
  })
})
