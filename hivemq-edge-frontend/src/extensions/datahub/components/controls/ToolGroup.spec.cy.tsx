import ToolGroup from '@datahub/components/controls/ToolGroup.tsx'
import { Button } from '@chakra-ui/react'

describe('ToolGroup', () => {
  beforeEach(() => {
    cy.viewport(800, 600)
  })

  it('should render the toolbox', () => {
    cy.mountWithProviders(
      <ToolGroup title="The title" id="my-id">
        <Button>Button 1</Button>
        <Button>Button 2</Button>
      </ToolGroup>
    )

    cy.getByTestId('toolbox-group-title').should('have.text', 'The title')
    cy.getByTestId('toolbox-group-container').find('button').should('have.length', 2)
  })

  it('should be accessible', () => {
    cy.injectAxe()
    cy.mountWithProviders(
      <ToolGroup title="The title" id="my-id">
        <Button>Button 1</Button>
        <Button>Button 2</Button>
      </ToolGroup>
    )

    cy.checkAccessibility()
  })
})
