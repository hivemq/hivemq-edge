import { DataIdentifierReference } from '@/api/__generated__'
import { CombinerOptionContent } from './CombinerOptionContent'

describe('CombinerOptionContent', () => {
  beforeEach(() => {
    cy.viewport(400, 200)
  })

  it('should render the label and type badge for a tag', () => {
    cy.mountWithProviders(<CombinerOptionContent label="boiler/temperature" type={DataIdentifierReference.type.TAG} />)

    cy.contains('boiler/temperature').should('exist')
    cy.get('p').should('contain.text', 'Tag')
  })

  it('should render ownership string (adapterId :: label) when adapterId is provided', () => {
    cy.mountWithProviders(
      <CombinerOptionContent
        label="boiler/temperature"
        type={DataIdentifierReference.type.TAG}
        adapterId="muc-opc-server"
      />
    )

    cy.contains('muc-opc-server :: boiler/temperature').should('exist')
  })

  it('should render plain label (no ownership string) when adapterId is absent', () => {
    cy.mountWithProviders(
      <CombinerOptionContent label="my/topic/+/temp" type={DataIdentifierReference.type.TOPIC_FILTER} />
    )

    cy.get('p').should('contain.text', 'Topic Filter')
    cy.get('p').should('have.length', 2) // label + type badge only (topic filter has no icon)
    cy.get('p').should('not.contain.text', '::')
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
    cy.mountWithProviders(<CombinerOptionContent label="boiler/temperature" type={DataIdentifierReference.type.TAG} />)

    cy.get('p').should('not.contain.text', 'Celsius')
    cy.get('p').should('have.length', 2) // ownership text + type badge (icon is an svg, not a p)
  })

  it('should render the correct type badge for topic filters', () => {
    cy.mountWithProviders(
      <CombinerOptionContent label="my/topic/+/temp" type={DataIdentifierReference.type.TOPIC_FILTER} />
    )

    cy.get('p').should('contain.text', 'Topic Filter')
    cy.get('p').should('not.contain.text', 'Tag')
  })
})
