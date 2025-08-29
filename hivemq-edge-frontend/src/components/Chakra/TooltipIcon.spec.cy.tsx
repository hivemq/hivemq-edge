/// <reference types="cypress" />

import TooltipIcon from '@/components/Chakra/TooltipIcon.tsx'
import { HqPulseActivated } from '@/components/Icons'

describe('TooltipIcon', () => {
  beforeEach(() => {
    cy.viewport(400, 150)
  })

  it('should be accessible', () => {
    cy.injectAxe()
    cy.mountWithProviders(
      <TooltipIcon
        aria-label="the icon text"
        data-testid="my-icon-badge"
        as={HqPulseActivated}
        data-type="pulseStatus"
      />
    )
    cy.get('[role="tooltip"]').should('not.exist')
    cy.getByTestId('my-icon-badge').click()
    cy.get('[role="tooltip"]').should('contain.text', 'the icon text')
    cy.get('body').click()
    cy.get('[role="tooltip"]').should('not.exist')
    cy.getByTestId('my-icon-badge').realHover()
    cy.get('[role="tooltip"]').should('contain.text', 'the icon text')

    cy.checkAccessibility(undefined, {
      rules: {
        region: { enabled: false },
      },
    })
  })
})
