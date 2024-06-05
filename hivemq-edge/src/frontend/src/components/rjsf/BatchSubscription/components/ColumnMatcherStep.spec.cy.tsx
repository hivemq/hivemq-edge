import { IdSchema } from '@rjsf/utils'
import { BatchModeStore } from '@/components/rjsf/BatchSubscription/types.ts'
import ColumnMatcherStep from '@/components/rjsf/BatchSubscription/components/ColumnMatcherStep.tsx'

const MOCK_ID_SCHEMA: IdSchema<unknown> = { $id: 'my-id' }
const MOCK_STORE: BatchModeStore = {
  idSchema: MOCK_ID_SCHEMA,
  schema: {
    type: 'array',
    items: {
      type: 'object',
      properties: {
        'message-expiry-interval': {
          type: 'integer',
          title: 'MQTT message expiry interval [s]',
        },
        'mqtt-topic': {
          type: 'string',
          title: 'Destination MQTT topic',
        },
        node: {
          type: 'string',
          title: 'Source Node ID',
        },
        'publishing-interval': {
          type: 'integer',
          title: 'OPC UA publishing interval [ms]',
        },
      },
      required: ['mqtt-topic', 'node'],
    },
  },
  worksheet: [
    {
      a: 1,
      b: 2,
      c: 3,
      d: 4,
    },
    {
      a: 2,
      b: 3,
      c: 3,
      d: 5,
    },
  ],
}

describe('DataSourceStep', () => {
  beforeEach(() => {
    cy.viewport(600, 600)
  })

  it('should render the mapping form', () => {
    cy.mountWithProviders(<ColumnMatcherStep onContinue={cy.stub()} store={MOCK_STORE} />)

    cy.get('form#batch-mapping-form').find('[role="group"]').as('mapper')
    cy.get('@mapper').should('have.length', 2)
    cy.get('input[name="mapping.0.column"]').should('have.value', '')
    cy.get('input[name="mapping.0.subscription"]').should('have.value', 'Destination MQTT topic')
    cy.get('#mapping\\.0\\.error').should(
      'contain.text',
      'The subscription property is required so a column must be selected'
    )
    cy.get('input[name="mapping.1.column"]').should('have.value', 'd')
    cy.get('input[name="mapping.1.subscription"]').should('have.value', 'Source Node ID')

    cy.get('#mapping\\.0\\.column').click()
    cy.get('#react-select-mapping\\.0\\.column-listbox').find('[role="option"]').as('columnHeaders')
    cy.get('@columnHeaders').should('have.length', 4)
    cy.get('@columnHeaders').first().should('contain.text', 'a')
  })

  it('should validate the form', () => {
    cy.mountWithProviders(<ColumnMatcherStep onContinue={cy.stub().as('onContinue')} store={MOCK_STORE} />)
    cy.get('@onContinue').should('have.been.calledWith', { mapping: undefined })

    cy.get('#mapping\\.0\\.error').should(
      'contain.text',
      'The subscription property is required so a column must be selected'
    )

    cy.get('input[name="mapping.0.column"]').should('have.value', '')
    cy.get('#mapping\\.0\\.column').click()
    cy.get('#react-select-mapping\\.0\\.column-listbox').find('[role="option"]').as('columnHeaders')
    cy.get('@columnHeaders').first().should('contain.text', 'a')
    cy.get('@columnHeaders').first().click()
    cy.get('input[name="mapping.0.column"]').should('have.value', 'a')
    cy.get('@onContinue').should('have.been.calledWith', {
      mapping: [
        {
          column: 'a',
          subscription: 'Destination MQTT topic',
        },
        {
          column: 'd',
          subscription: 'Source Node ID',
        },
      ],
    })
  })

  it('should be accessible', () => {
    cy.injectAxe()
    cy.mountWithProviders(<ColumnMatcherStep onContinue={cy.stub()} store={MOCK_STORE} />)
    cy.checkAccessibility()
    cy.percySnapshot('Component: ColumnMatcherStep')
  })
})
