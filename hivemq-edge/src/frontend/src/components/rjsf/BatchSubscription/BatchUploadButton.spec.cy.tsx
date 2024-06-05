import BatchUploadButton from '@/components/rjsf/BatchSubscription/BatchUploadButton.tsx'
import { IdSchema } from '@rjsf/utils'

const MOCK_SCHEMA = {}
const MOCK_ID_SCHEMA: IdSchema<unknown> = { $id: 'my-id' }

describe('BatchUploadButton', () => {
  beforeEach(() => {
    cy.viewport(800, 800)
  })
  it('should renders the CTA', () => {
    cy.mountWithProviders(<BatchUploadButton idSchema={MOCK_ID_SCHEMA} schema={MOCK_SCHEMA} />)

    cy.getByTestId('array-field-batch-cta').should('contain.text', 'Upload').click()
    cy.get('section[role="dialog"]').should('exist')
    cy.get('header').should('contain.text', 'Bulk Subscription Creation')
    cy.get('header + button').as('closeModal').should('have.attr', 'aria-label', 'Close')

    cy.get('footer > [role="group"] > button').as('stepperButton').should('have.length', 3)
    cy.get('@stepperButton').eq(0).should('have.text', 'Previous').should('be.disabled')
    cy.get('@stepperButton').eq(1).should('have.text', 'Next')
    cy.get('@stepperButton').eq(2).should('have.text', 'Cancel')

    cy.get('@closeModal').click()
    cy.get('section[role="dialog"]').should('not.exist')
  })
})
