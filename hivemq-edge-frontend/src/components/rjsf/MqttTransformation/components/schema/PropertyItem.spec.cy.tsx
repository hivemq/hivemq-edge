/// <reference types="cypress" />

import type { FlatJSONSchema7 } from '@/components/rjsf/MqttTransformation/utils/json-schema.utils.ts'
import PropertyItem from './PropertyItem.tsx'

const MOCK_PROPERTY: FlatJSONSchema7 = {
  description: undefined,
  path: [],
  key: 'billing_address',
  title: 'Billing address',
  type: 'object',
}

describe('PropertyItem', () => {
  beforeEach(() => {
    cy.viewport(800, 900)
  })

  it('should render properly', () => {
    cy.mountWithProviders(<PropertyItem property={MOCK_PROPERTY} isDraggable={false} />)

    cy.getByAriaLabel('Property').should('have.text', 'Billing address').should('have.attr', 'data-type', 'object')
  })

  it('should render draggable', () => {
    cy.mountWithProviders(<PropertyItem property={MOCK_PROPERTY} isDraggable={true} />)

    cy.getByAriaLabel('Property')
      .should('have.attr', 'tabindex', '0')
      .should('have.text', 'Billing address')
      .should('have.attr', 'draggable', 'true')
  })

  it('should render tooltip', () => {
    cy.mountWithProviders(<PropertyItem property={{ ...MOCK_PROPERTY, path: ['root', 'branch'] }} hasTooltip={true} />)

    cy.getByAriaLabel('Property')
      .should('have.attr', 'data-path', 'root.branch.billing_address')
      .children()
      .should('have.text', 'Billing address')
    cy.getByTestId('property-type').click()
    cy.getByTestId('property-type').should('have.attr', 'aria-describedby')
    cy.getByTestId('property-name').click()
    cy.getByTestId('property-name').should('have.attr', 'aria-describedby')
  })

  it('should render examples properly', () => {
    cy.mountWithProviders(
      <PropertyItem property={{ ...MOCK_PROPERTY, examples: ['this is a sample'] }} isDraggable={false} hasExamples />
    )

    cy.getByAriaLabel('Property').should('have.attr', 'data-type', 'object')
    cy.getByTestId('property-name').should('have.text', 'Billing address')
    cy.getByTestId('property-example').should('have.text', 'this is a sample')
  })

  it('should render readonly indicator when property is readOnly', () => {
    cy.mountWithProviders(<PropertyItem property={{ ...MOCK_PROPERTY, readOnly: true }} isDraggable={false} />)

    cy.getByTestId('property-readonly').should('exist')
    cy.getByAriaLabel('Read-only property').should('exist')
  })

  it('should not render readonly indicator when property is not readOnly', () => {
    cy.mountWithProviders(<PropertyItem property={MOCK_PROPERTY} isDraggable={false} />)

    cy.getByTestId('property-readonly').should('not.exist')
  })

  it('should show tooltip on readonly indicator interaction', () => {
    cy.mountWithProviders(<PropertyItem property={{ ...MOCK_PROPERTY, readOnly: true }} isDraggable={false} />)

    cy.getByTestId('property-readonly').click()
    cy.getByTestId('property-readonly').should('have.attr', 'aria-describedby')
  })

  it('should be accessible', () => {
    cy.injectAxe()

    cy.mountWithProviders(<PropertyItem property={MOCK_PROPERTY} isDraggable={false} />)

    cy.checkAccessibility()
  })

  it('should be accessible with readonly property', () => {
    cy.injectAxe()

    cy.mountWithProviders(<PropertyItem property={{ ...MOCK_PROPERTY, readOnly: true }} isDraggable={false} />)

    cy.checkAccessibility()
  })
})
