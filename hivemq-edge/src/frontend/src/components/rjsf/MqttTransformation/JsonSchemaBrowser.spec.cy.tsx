import JsonSchemaBrowser from '@/components/rjsf/MqttTransformation/JsonSchemaBrowser.tsx'
import { MOCK_MQTT_SCHEMA_REFS } from '@/__test-utils__/rjsf/schema.mocks.ts'

describe('JsonSchemaBrowser', () => {
  beforeEach(() => {
    cy.viewport(400, 400)
  })

  it('should render properly', () => {
    cy.mountWithProviders(<JsonSchemaBrowser schema={MOCK_MQTT_SCHEMA_REFS} />)

    cy.get('ul').find('li > div').as('metadata')
    cy.get('@metadata').should('have.length', 11)
    cy.get('@metadata').eq(0).should('have.text', 'Billing address').should('have.attr', 'data-type', 'object')
    cy.get('@metadata').eq(9).should('have.text', 'name').should('have.attr', 'data-type', 'string')
  })

  it('should be accessible', () => {
    cy.injectAxe()
    cy.mountWithProviders(<JsonSchemaBrowser schema={MOCK_MQTT_SCHEMA_REFS} />)

    cy.checkAccessibility()
    cy.percySnapshot('Component: JsonSchemaBrowser')
  })
})
