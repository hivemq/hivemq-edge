import { IdSchema } from '@rjsf/utils'
import { BatchModeStore, ValidationColumns } from '@/components/rjsf/BatchSubscription/types.ts'
import ConfirmStep from '@/components/rjsf/BatchSubscription/components/ConfirmStep.tsx'
import { RJSFSchema } from '@rjsf/utils/src/types.ts'

const MOCK_ID_SCHEMA: IdSchema<unknown> = { $id: 'my-id' }
const MOCK_SCHEMA: RJSFSchema = {
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
}
const MOCK_VALIDATION: ValidationColumns[] = [{ row: 0, isError: false, message: 'MQTT topic validation failed' }]
const MOCK_STORE: BatchModeStore = {
  idSchema: MOCK_ID_SCHEMA,
  schema: MOCK_SCHEMA,
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

describe('ConfirmStep', () => {
  beforeEach(() => {
    cy.viewport(600, 600)
  })

  it('should render an error if no valid subscriptions', () => {
    cy.mountWithProviders(<ConfirmStep onContinue={cy.stub()} store={MOCK_STORE} />)
    cy.get('[role="alert"]').should('have.attr', 'data-status', 'error')
    cy.getByTestId('batch-confirm-title').should('have.text', 'No valid subscriptions')
    cy.getByTestId('batch-confirm-description').should(
      'contain.text',
      'There was a problem loading valid subscriptions from your configuration. Please try again.'
    )
    cy.getByTestId('batch-confirm-submit').should('contain.text', 'Upload records').should('be.disabled')
  })

  it('should render a proper confirmation', () => {
    cy.mountWithProviders(
      <ConfirmStep onContinue={cy.stub()} store={{ ...MOCK_STORE, subscriptions: MOCK_VALIDATION }} />
    )
    cy.get('[role="alert"]').should('have.attr', 'data-status', 'success')
    cy.getByTestId('batch-confirm-title').should('have.text', 'One subscription created')
    cy.getByTestId('batch-confirm-description').should(
      'contain.text',
      'Your subscription is ready to be uploaded. Any existing subscription already defined in the adapter will be deleted.'
    )
    cy.getByTestId('batch-confirm-submit').should('contain.text', 'Upload records').should('not.be.disabled')
  })

  it('should validate the form', () => {
    cy.mountWithProviders(
      <ConfirmStep
        onContinue={cy.stub().as('onContinue')}
        onBatchUpload={cy.stub().as('onBatchUpload')}
        onClose={cy.stub().as('onClose')}
        store={{ ...MOCK_STORE, subscriptions: MOCK_VALIDATION }}
      />
    )

    cy.get('@onClose').should('not.have.been.called')
    cy.get('@onBatchUpload').should('not.have.been.called')
    cy.get('@onContinue').should('not.have.been.called')
    cy.getByTestId('batch-confirm-submit').should('not.be.disabled').click()

    const { row, isError, ...rest } = MOCK_VALIDATION[0]
    cy.get('@onBatchUpload').should('have.been.calledWith', { $id: 'my-id' }, [rest])
    cy.get('@onContinue').should('have.been.calledWith', {
      fileName: undefined,
      mapping: undefined,
      subscriptions: undefined,
      worksheet: undefined,
    })
    cy.get('@onClose').should('have.been.called')
  })

  it('should be accessible', () => {
    cy.injectAxe()
    cy.mountWithProviders(
      <ConfirmStep onContinue={cy.stub()} store={{ ...MOCK_STORE, subscriptions: MOCK_VALIDATION }} />
    )
    cy.checkAccessibility()
    cy.percySnapshot('Component: ConfirmStep')
  })
})
