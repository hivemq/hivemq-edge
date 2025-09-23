import { Text } from '@chakra-ui/react'
import ExpandableDrawer from '@/components/ExpandableDrawer/ExpandableDrawer.tsx'

describe('ExpandableDrawer', () => {
  beforeEach(() => {
    cy.viewport(800, 250)
  })

  it('should render', () => {
    const onClose = cy.stub().as('onClose')
    cy.mountWithProviders(
      <ExpandableDrawer header="The title" isOpen={true} onClose={onClose}>
        <Text data-testid="content">Test</Text>
      </ExpandableDrawer>
    )

    cy.get('@onClose').should('not.have.been.called')

    cy.get('header').should('have.text', 'The title')
    cy.getByTestId('content').should('be.visible').should('have.text', 'Test')
    cy.get('[role="dialog"]').should('have.attr', 'aria-label', 'The title')
    cy.getByAriaLabel('Close').should('be.visible').click()
    cy.get('@onClose').should('have.been.called')

    cy.getByAriaLabel('Expand').should('be.visible').should('have.attr', 'data-expanded', 'false')
    cy.getByAriaLabel('Expand').click()
    cy.getByAriaLabel('Shrink').should('have.attr', 'data-expanded', 'true')
  })
})
