import { DataIdentifierReference } from '@/api/__generated__'
import { CombinerOptionContent } from './CombinerOptionContent'

describe('CombinerOptionContent', () => {
  beforeEach(() => {
    cy.viewport(400, 200)
  })

  it('should render the label and type badge', () => {
    cy.mountWithProviders(
      <CombinerOptionContent label="boiler/temperature" type={DataIdentifierReference.type.TAG} />
    )

    cy.get('p').should('contain.text', 'boiler/temperature')
    cy.get('p').should('contain.text', 'Tag')
  })

  it('should render the adapter name when adapterId is provided', () => {
    cy.mountWithProviders(
      <CombinerOptionContent
        label="boiler/temperature"
        type={DataIdentifierReference.type.TAG}
        adapterId="muc-opc-server"
      />
    )

    cy.get('p').should('contain.text', 'muc-opc-server')
  })

  it('should not render the adapter name when adapterId is absent', () => {
    cy.mountWithProviders(
      <CombinerOptionContent label="my/topic/+/temp" type={DataIdentifierReference.type.TOPIC_FILTER} />
    )

    cy.get('p').should('contain.text', 'Topic Filter')
    cy.get('p').should('have.length', 2) // label + type badge only
  })

  it('should render the description when provided', () => {
    cy.mountWithProviders(
      <CombinerOptionContent
        label="boiler/temperature"
        type={DataIdentifierReference.type.TAG}
        adapterId="muc-opc-server"
        description="Current boiler temperature in Celsius"
      />
    )

    cy.get('p').should('contain.text', 'Current boiler temperature in Celsius')
  })

  it('should not render a description element when description is absent', () => {
    cy.mountWithProviders(
      <CombinerOptionContent label="boiler/temperature" type={DataIdentifierReference.type.TAG} />
    )

    cy.get('p').should('not.contain.text', 'Celsius')
    cy.get('p').should('have.length', 2) // label + type badge only
  })

  it('should render the correct type badge for topic filters', () => {
    cy.mountWithProviders(
      <CombinerOptionContent label="my/topic/+/temp" type={DataIdentifierReference.type.TOPIC_FILTER} />
    )

    cy.get('p').should('contain.text', 'Topic Filter')
    cy.get('p').should('not.contain.text', 'Tag')
  })
})
