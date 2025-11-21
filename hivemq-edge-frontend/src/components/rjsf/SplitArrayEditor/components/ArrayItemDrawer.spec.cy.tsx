/// <reference types="cypress" />

import { MOCK_DEVICE_TAG_ADDRESS_MODBUS } from '@/api/hooks/useProtocolAdapters/__handlers__'
import ArrayItemDrawer from '@/components/rjsf/SplitArrayEditor/components/ArrayItemDrawer.tsx'
import { Button } from '@chakra-ui/react'

describe('ArrayItemDrawer', () => {
  beforeEach(() => {
    cy.viewport(800, 800)
    cy.intercept('/api/v1/management/protocol-adapters/tags', {
      items: [{ name: 'test/tag1', definition: MOCK_DEVICE_TAG_ADDRESS_MODBUS }],
    })
  })

  it('should render properly', () => {
    const onSubmit = cy.stub().as('onSubmit')
    cy.injectAxe()
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
    cy.checkAccessibility()
  })
})
