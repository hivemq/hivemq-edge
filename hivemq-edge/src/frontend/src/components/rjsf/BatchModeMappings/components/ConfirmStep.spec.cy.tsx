import { BatchModeStore, ValidationColumns } from '@/components/rjsf/BatchModeMappings/types.ts'
import ConfirmStep from '@/components/rjsf/BatchModeMappings/components/ConfirmStep.tsx'
import {
  MOCK_ID_SCHEMA,
  MOCK_SCHEMA,
  MOCK_WORKSHEET,
} from '@/components/rjsf/BatchModeMappings/__test-utils__/store.mocks.ts'

const MOCK_VALIDATION: ValidationColumns[] = [{ row: 0, isError: false, message: 'MQTT topic validation failed' }]
const MOCK_STORE: BatchModeStore = {
  idSchema: MOCK_ID_SCHEMA,
  schema: MOCK_SCHEMA,
  worksheet: MOCK_WORKSHEET,
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
