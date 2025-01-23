import PolicyEditorLoader, {
  BehaviorPolicyLoader,
  DataPolicyLoader,
} from '@datahub/components/pages/PolicyEditorLoader.tsx'

describe('PolicyEditorLoader', () => {
  beforeEach(() => {
    cy.viewport(800, 800)
  })

  it('should render the right loader', () => {
    cy.mountWithProviders(<PolicyEditorLoader />)
  })

  describe('DataPolicyLoader', () => {
    it('should render loading error', () => {
      cy.intercept('/api/v1/data-hub/data-validation/policies/*', { statusCode: 404 })
      cy.intercept('/api/v1/data-hub/schemas', { statusCode: 404 })
      cy.intercept('/api/v1/data-hub/scripts', { statusCode: 404 })

      cy.mountWithProviders(<DataPolicyLoader policyId="my-policy" />)

      cy.get('[role="alert"]').should('have.attr', 'data-status', 'error')
      cy.get('[role="alert"] div div[data-status]').eq(0).should('have.text', 'Not identified')
      cy.get('[role="alert"] div div[data-status]').eq(1).should('have.text', 'Resource not found')
    })

    it('should render the table component', () => {
      cy.mountWithProviders(<DataPolicyLoader policyId="my-policy" />)
    })

    it('should be accessible', () => {
      cy.injectAxe()
      cy.mountWithProviders(<DataPolicyLoader policyId="my-policy" />)
      cy.checkAccessibility()
    })
  })

  describe('BehaviorPolicyLoader', () => {
    beforeEach(() => {
      cy.viewport(800, 800)
    })

    it('should render the table component', () => {
      cy.mountWithProviders(<BehaviorPolicyLoader policyId="my-policy" />)
    })

    it('should be accessible', () => {
      cy.injectAxe()
      cy.mountWithProviders(<BehaviorPolicyLoader policyId="my-policy" />)
      cy.checkAccessibility()
    })
  })
})
