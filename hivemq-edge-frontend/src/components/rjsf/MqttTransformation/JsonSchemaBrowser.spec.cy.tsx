import { DataIdentifierReference } from '@/api/__generated__'
import type { DataReference } from '@/api/hooks/useDomainModel/useGetCombinedDataSchemas'
import JsonSchemaBrowser from '@/components/rjsf/MqttTransformation/JsonSchemaBrowser.tsx'
import { MOCK_MQTT_SCHEMA_PLAIN, MOCK_MQTT_SCHEMA_REFS } from '@/__test-utils__/rjsf/schema.mocks.ts'

const MOCK_SCHEMA_TITLED = { ...MOCK_MQTT_SCHEMA_PLAIN, title: 'temperature' }

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

  describe('TAG heading with isTagShown', () => {
    it('should show ownership string in heading when scope is set', () => {
      const ref: DataReference = { id: 'temperature', type: DataIdentifierReference.type.TAG, scope: 'opcua-adapter' }
      cy.mountWithProviders(<JsonSchemaBrowser schema={MOCK_SCHEMA_TITLED} dataReference={ref} isTagShown />)

      cy.getByTestId('topic-wrapper').should('contain.text', 'opcua-adapter :: temperature')
    })

    it('should show plain tag name in heading when scope is null', () => {
      const ref: DataReference = { id: 'temperature', type: DataIdentifierReference.type.TAG, scope: null }
      cy.mountWithProviders(<JsonSchemaBrowser schema={MOCK_SCHEMA_TITLED} dataReference={ref} isTagShown />)

      cy.getByTestId('topic-wrapper').should('contain.text', 'temperature')
      cy.getByTestId('topic-wrapper').should('not.contain.text', '::')
    })

    it('should show topic filter id in heading for TOPIC_FILTER type', () => {
      const ref: DataReference = {
        id: 'factory/+/sensors',
        type: DataIdentifierReference.type.TOPIC_FILTER,
        scope: null,
      }
      cy.mountWithProviders(<JsonSchemaBrowser schema={MOCK_SCHEMA_TITLED} dataReference={ref} isTagShown />)

      cy.getByTestId('topic-wrapper').should('be.visible')
      cy.getByTestId('topic-wrapper')
        .invoke('text')
        .then((text) => {
          expect(text).to.include('sensors')
          expect(text).not.to.include('::')
        })
    })
  })

  it('should be accessible', () => {
    cy.injectAxe()
    cy.mountWithProviders(<JsonSchemaBrowser schema={MOCK_MQTT_SCHEMA_REFS} />)

    cy.checkAccessibility()
  })
})
