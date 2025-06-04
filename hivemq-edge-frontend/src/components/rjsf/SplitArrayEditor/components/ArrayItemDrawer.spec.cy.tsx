/// <reference types="cypress" />

import ArrayItemDrawer from '@/components/rjsf/SplitArrayEditor/components/ArrayItemDrawer.tsx'
import { Button } from '@chakra-ui/react'

describe('ArrayItemDrawer', () => {
  beforeEach(() => {
    cy.viewport(800, 800)
  })

  it('should properly', () => {
    const onSubmit = cy.stub().as('onSubmit')
    cy.mountWithProviders(
      <ArrayItemDrawer
        header="test"
        context={{}}
        trigger={({ onOpen }) => (
          <Button data-testid="trigger" onClick={onOpen}>
            Click me!
          </Button>
        )}
        onSubmit={onSubmit}
      />,
      {
        routerProps: { initialEntries: [`/node/wrong-adapter`] },
      }
    )

    cy.get('[role="dialog"]').should('not.exist')
    cy.getByTestId('trigger').click()
    cy.get('[role="dialog"]').should('be.visible')
    cy.get('header').should('contain.text', 'test')
    cy.injectAxe()
    cy.checkAccessibility()
  })
})
