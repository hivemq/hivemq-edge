/// <reference types="cypress" />

import TooltipBadge from '@/components/Chakra/TooltipBadge.tsx'

describe('TooltipBadge', () => {
  beforeEach(() => {
    cy.viewport(400, 150)
  })

  it('should be accessible', () => {
    cy.injectAxe()
    cy.mountWithProviders(
      <TooltipBadge aria-label="the text" data-testid="my-badge">
        the content
      </TooltipBadge>
    )
    cy.get('[role="tooltip"]').should('not.exist')
    cy.getByTestId('my-badge').click()
    cy.get('[role="tooltip"]').should('contain.text', 'the text')
    cy.get('body').click()
    cy.get('[role="tooltip"]').should('not.exist')
    cy.getByTestId('my-badge').realHover()
    cy.get('[role="tooltip"]').should('contain.text', 'the text')

    cy.checkAccessibility(undefined, {
      rules: {
        region: { enabled: false },
      },
    })
  })
})
