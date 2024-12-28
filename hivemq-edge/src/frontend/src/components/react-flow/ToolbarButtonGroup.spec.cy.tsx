import { ReactFlowProvider } from 'reactflow'
import { Icon } from '@chakra-ui/react'
import { LuAlbum, LuBaby } from 'react-icons/lu'

import IconButton from '@/components/Chakra/IconButton.tsx'
import ToolbarButtonGroup from '@/components/react-flow/ToolbarButtonGroup.tsx'

describe('ToolbarButtonGroup', () => {
  beforeEach(() => {
    cy.viewport(800, 250)
  })

  it('should renders properly', () => {
    cy.mountWithProviders(
      <ToolbarButtonGroup>
        <IconButton icon={<Icon as={LuAlbum} />} aria-label="first button" onClick={cy.stub().as('button1')} />
        <IconButton icon={<Icon as={LuBaby} />} aria-label="second button" onClick={cy.stub().as('button2')} />
      </ToolbarButtonGroup>,
      {
        wrapper: ({ children }) => <ReactFlowProvider>{children}</ReactFlowProvider>,
      }
    )

    cy.get('[role="group"]').should('have.attr', 'data-orientation', 'horizontal')
    cy.getByAriaLabel('second button').should('be.visible')
    cy.getByAriaLabel('first button').should('be.visible')
  })

  it('should renders props', () => {
    cy.mountWithProviders(
      <ToolbarButtonGroup orientation="horizontal">
        <IconButton icon={<Icon as={LuAlbum} />} aria-label="first button" onClick={cy.stub().as('button1')} />
        <IconButton icon={<Icon as={LuBaby} />} aria-label="second button" onClick={cy.stub().as('button2')} />
      </ToolbarButtonGroup>,
      {
        wrapper: ({ children }) => <ReactFlowProvider>{children}</ReactFlowProvider>,
      }
    )

    cy.get('[role="group"]').should('have.attr', 'data-orientation', 'horizontal')
    cy.getByAriaLabel('second button').should('be.visible')
    cy.getByAriaLabel('first button').should('be.visible')
  })

  it('should be accessible', () => {
    cy.injectAxe()
    cy.mountWithProviders(
      <ToolbarButtonGroup>
        <IconButton icon={<Icon as={LuAlbum} />} aria-label="first button" />
        <IconButton icon={<Icon as={LuBaby} />} aria-label="second button" />
      </ToolbarButtonGroup>,
      {
        wrapper: ({ children }) => <ReactFlowProvider>{children}</ReactFlowProvider>,
      }
    )
    cy.checkAccessibility()
    cy.percySnapshot('The login page on loading')
  })
})
