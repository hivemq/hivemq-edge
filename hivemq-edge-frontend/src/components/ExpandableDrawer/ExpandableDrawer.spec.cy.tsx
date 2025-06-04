import { Text } from '@chakra-ui/react'
import ExpandableDrawer from '@/components/ExpandableDrawer/ExpandableDrawer.tsx'

describe('ExpandableDrawer', () => {
  beforeEach(() => {
    cy.viewport(800, 250)
  })

  it('should render', () => {
    cy.mountWithProviders(
      <ExpandableDrawer header="The title" isOpen={true} onClose={cy.stub}>
        <Text data-testid="content">Test</Text>
      </ExpandableDrawer>
    )

    cy.get('header').should('have.text', 'The title')
    cy.getByTestId('content').should('be.visible')
  })
})
