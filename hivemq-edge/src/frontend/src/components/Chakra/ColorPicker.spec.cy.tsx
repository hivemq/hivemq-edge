/// <reference types="cypress" />

import { ColorPicker } from '@/components/Chakra/ColorPicker.tsx'

const mockColorScheme = 'green'
const mockDdefaultColorSchemes = [
  'gray',
  'red',
  'orange',
  'yellow',
  'green',
  'blue',
  'cyan',
  'purple',
  'pink',
  'whatsapp',
]

describe('ColorPicker', () => {
  beforeEach(() => {
    cy.viewport(300, 500)
  })

  it('should render properly', () => {
    const onChange = cy.stub().as('onChange')
    cy.mountWithProviders(
      <ColorPicker
        colorScheme={mockColorScheme}
        onChange={onChange}
        colorSchemes={mockDdefaultColorSchemes}
        ml={'75px'}
      />
    )

    cy.getByTestId('colorPicker-trigger').should('have.attr', 'data-color-scheme', mockColorScheme)
    cy.getByTestId('colorPicker-trigger').click()
    cy.getByTestId('colorPicker-popover').should('be.visible')
    cy.getByTestId('colorPicker-sample').should('have.text', mockColorScheme)
    cy.get("[data-testId^='colorPicker-selector']")
      .should('have.length', 10)
      .eq(5)
      .should('have.attr', 'data-color-scheme', 'blue')
      .click()

    cy.get('@onChange').should('have.been.calledWith', 'blue')
    cy.getByTestId('colorPicker-sample').should('have.text', 'blue')
    cy.getByTestId('colorPicker-trigger').should('have.attr', 'data-color-scheme', 'blue')

    // cy.getByTestId('colorPicker-trigger').click()
    // cy.getByTestId('colorPicker-popover').should('not.be.visible')
  })

  // it('should be accessible', () => {
  //   cy.injectAxe()
  //   cy.mountWithProviders(<ColorPicker colorScheme={mockColorScheme} onChange={cy.stub()} ml={'75px'} />)
  //
  //   cy.getByTestId('colorPicker-trigger').click()
  //   cy.checkAccessibility()
  //   cy.percySnapshot('Component: ColorPicker')
  // })
})
