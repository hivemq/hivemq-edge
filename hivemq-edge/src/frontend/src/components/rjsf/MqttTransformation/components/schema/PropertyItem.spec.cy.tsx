/// <reference types="cypress" />

import type { FlatJSONSchema7 } from '@/components/rjsf/MqttTransformation/utils/json-schema.utils.ts'
import PropertyItem from './PropertyItem.tsx'
import { List } from '@chakra-ui/react'

const MOCK_PROPERTY: FlatJSONSchema7 = {
  description: undefined,
  path: [],
  title: 'Billing address',
  type: 'object',
}

const wrapper: React.JSXElementConstructor<{ children: React.ReactNode }> = ({ children }) => {
  return <List>{children}</List>
}

describe('PropertyItem', () => {
  beforeEach(() => {
    cy.viewport(800, 900)
  })

  it('should render properly', () => {
    cy.mountWithProviders(<PropertyItem property={MOCK_PROPERTY} isDraggable={false} />, { wrapper })

    cy.get('ul li').eq(0).should('have.text', 'Billing address').should('have.attr', 'data-type', 'object')
  })

  it('should render draggable', () => {
    cy.mountWithProviders(<PropertyItem property={MOCK_PROPERTY} isDraggable={true} />, { wrapper })

    cy.get('ul li').eq(0).should('have.text', 'Billing address').should('have.attr', 'draggable', 'true')
  })

  it('should render tooltip', () => {
    cy.mountWithProviders(
      <PropertyItem property={{ ...MOCK_PROPERTY, path: ['root', 'branch'] }} hasTooltip={true} />,
      { wrapper }
    )

    cy.get('ul li')
      .eq(0)
      .should('have.text', 'Billing address')
      .should('have.attr', 'data-path', 'root.branch.Billing address')
    cy.get('ul li span').click()
    cy.get('ul li span').should('have.attr', 'aria-describedby')
  })

  it('should be accessible ', () => {
    cy.injectAxe()

    cy.mountWithProviders(<PropertyItem property={MOCK_PROPERTY} isDraggable={false} />, { wrapper })

    cy.checkAccessibility()
  })
})
