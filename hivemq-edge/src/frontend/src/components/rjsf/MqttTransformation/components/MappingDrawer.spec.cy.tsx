import { MockAdapterType } from '@/__test-utils__/adapters/types.ts'
import { MOCK_DEVICE_TAGS } from '@/api/hooks/useProtocolAdapters/__handlers__'
import { GENERATE_DATA_MODELS } from '@/api/hooks/useDomainModel/__handlers__'

import MappingDrawer from '@/components/rjsf/MqttTransformation/components/MappingDrawer.tsx'
import { MOCK_SOUTHBOUND_MAPPING } from '@/api/hooks/useProtocolAdapters/__handlers__/mapping.mocks.ts'

const mockAdapterId = 'adapterId'

describe('MappingDrawer', () => {
  beforeEach(() => {
    cy.viewport(1200, 900)
    cy.intercept('/api/v1/management/topic-filters', {
      items: [
        {
          topicFilter: 'my/filter',
          description: 'This is a topic filter',
        },
        {
          topicFilter: 'my/other/filter',
          description: 'This is a topic filter',
        },
      ],
    }).as('getTopicFilters')

    cy.intercept(`/api/v1/management/protocol-adapters/adapters/${mockAdapterId}/tags`, {
      items: MOCK_DEVICE_TAGS(mockAdapterId, MockAdapterType.OPC_UA),
    }).as('getTags')

    cy.intercept(
      `/api/v1/management/protocol-adapters/writing-schema/${mockAdapterId}/${encodeURIComponent('my/tag')}`,
      {
        configSchema: GENERATE_DATA_MODELS(true, 'mockTopic'),
        protocolId: 'my-type',
      }
    )

    cy.intercept('/api/v1/management/sampling/topic/**', { items: [] })
    cy.intercept('/api/v1/management/sampling/schema/*', GENERATE_DATA_MODELS(true, 'my-topic'))
  })

  it('should render properly', () => {
    const onClose = cy.stub().as('onClose')
    const onSubmit = cy.stub().as('onSubmit')
    const onChange = cy.stub().as('onChange')
    cy.mountWithProviders(
      <MappingDrawer
        adapterId={mockAdapterId}
        adapterType={MockAdapterType.OPC_UA}
        onClose={onClose}
        onSubmit={onSubmit}
        onChange={onChange}
        item={MOCK_SOUTHBOUND_MAPPING}
      />
    )

    cy.wait('@getTags')

    cy.get('header').should('have.text', 'Mapping Editor')
    cy.get('footer').find('button').as('modalCTAs')

    cy.get('@modalCTAs').eq(0).should('have.text', 'Cancel')
    cy.get('@modalCTAs').eq(1).should('have.text', 'Save')

    cy.get('@onChange').should('not.have.been.called')
    cy.getByAriaLabel('Clear selected options').eq(1).click()
    cy.get('@onChange').should('have.been.calledWith', 'tagName', null)

    cy.get('@onSubmit').should('not.have.been.called')
    cy.get('@modalCTAs').eq(1).click()
    cy.get('@onSubmit').should(
      'have.been.calledWith',
      Cypress.sinon.match({
        tagName: 'my/tag',
        topicFilter: 'my/filter',
        fieldMapping: {
          instructions: [
            {
              source: 'dropped-property',
              destination: 'lastName',
            },
          ],
        },
      })
    )

    cy.get('@onClose').should('not.have.been.called')
    cy.get('@modalCTAs').eq(0).click()
    cy.get('@onClose').should('have.been.called')
  })

  it('should be accessible ', () => {
    cy.injectAxe()

    cy.mountWithProviders(
      <MappingDrawer
        adapterId="testid"
        adapterType="my-type"
        onClose={cy.stub()}
        onSubmit={cy.stub()}
        onChange={cy.stub()}
        item={MOCK_SOUTHBOUND_MAPPING}
      />
    )

    cy.get('header').should('have.text', 'Mapping Editor')
    cy.checkAccessibility(undefined, {
      rules: {
        // h5 used for sections is not in order. Not detected on other tests
        'heading-order': { enabled: false },
      },
    })
  })
})
