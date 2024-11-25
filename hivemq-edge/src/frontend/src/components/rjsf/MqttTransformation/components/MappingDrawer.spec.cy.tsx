import { OutwardMapping } from '@/modules/Mappings/types.ts'
import { MOCK_DEVICE_TAGS, mockProtocolAdapter } from '@/api/hooks/useProtocolAdapters/__handlers__'
import { GENERATE_DATA_MODELS } from '@/api/hooks/useDomainModel/__handlers__'

import MappingDrawer from '@/components/rjsf/MqttTransformation/components/MappingDrawer.tsx'
import { MockAdapterType } from '@/__test-utils__/adapters/types.ts'

const MOCK_SUBS: OutwardMapping = {
  tag: 'my-tag',
  mqttTopicFilter: 'my-topic',
  fieldMapping: [{ source: { propertyPath: 'dropped-property' }, destination: { propertyPath: 'Second String' } }],
}

describe('MappingDrawer', () => {
  beforeEach(() => {
    cy.viewport(1200, 900)
    cy.intercept('/api/v1/management/protocol-adapters/types', { items: [mockProtocolAdapter] })
    cy.intercept('/api/v1/management/domain/tags/schema?*', GENERATE_DATA_MODELS(false, 'my-tag'))
    cy.intercept('/api/v1/management/domain/topics/schema?*', GENERATE_DATA_MODELS(true, 'my-topic'))

    cy.intercept('/api/v1/management/topic-filters', {
      items: [
        {
          topicFilter: 'my-tag',
          description: 'This is a topic filter',
        },
        {
          topicFilter: 'my-topic',
          description: 'This is a topic filter',
        },
      ],
    })
    cy.intercept('/api/v1/management/protocol-adapters/adapters/**/tags', {
      items: MOCK_DEVICE_TAGS('opcua-1', MockAdapterType.OPC_UA),
    }).as('getTags')
  })

  it('should render properly', () => {
    const onClose = cy.stub().as('onClose')
    const onSubmit = cy.stub().as('onSubmit')
    const onChange = cy.stub().as('onChange')
    cy.mountWithProviders(
      <MappingDrawer adapterId="testid" onClose={onClose} onSubmit={onSubmit} onChange={onChange} item={MOCK_SUBS} />
    )

    cy.wait('@getTags')

    cy.get('header').should('have.text', 'Mapping Editor')
    cy.get('footer').find('button').as('modalCTAs')

    cy.get('@modalCTAs').eq(0).should('have.text', 'Cancel')
    cy.get('@modalCTAs').eq(1).should('have.text', 'Save')

    cy.get('@onChange').should('not.have.been.called')
    cy.getByAriaLabel('Clear selected options').eq(1).click()
    cy.get('@onChange').should('have.been.calledWith', 'tag', null)

    cy.get('@onSubmit').should('not.have.been.called')
    cy.get('@modalCTAs').eq(1).click()
    cy.get('@onSubmit').should('have.been.calledWith', {
      tag: 'my-tag',
      mqttTopicFilter: 'my-topic',
      fieldMapping: [
        {
          source: {
            propertyPath: 'dropped-property',
          },
          destination: {
            propertyPath: 'Second String',
          },
        },
      ],
    })

    cy.get('@onClose').should('not.have.been.called')
    cy.get('@modalCTAs').eq(0).click()
    cy.get('@onClose').should('have.been.called')
  })

  it('should be accessible ', () => {
    cy.injectAxe()

    cy.mountWithProviders(
      <MappingDrawer
        adapterId="testid"
        onClose={cy.stub()}
        onSubmit={cy.stub()}
        onChange={cy.stub()}
        item={MOCK_SUBS}
      />
    )

    cy.wait('@getTags')

    cy.get('header').should('have.text', 'Mapping Editor')
    cy.checkAccessibility()
  })
})
