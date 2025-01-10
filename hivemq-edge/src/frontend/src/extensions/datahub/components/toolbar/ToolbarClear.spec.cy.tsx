import { getPolicyPublishWrapper, MOCK_NODE_DATA_POLICY } from '@datahub/__test-utils__/react-flow.mocks.tsx'
import { ToolbarClear } from '@datahub/components/toolbar/ToolbarClear.tsx'

describe('ToolbarClear', () => {
  beforeEach(() => {
    cy.viewport(800, 250)
  })

  it('should render without report', () => {
    cy.mountWithProviders(<ToolbarClear />, { wrapper: getPolicyPublishWrapper() })
    cy.get('button').should('have.attr', 'aria-label', 'Clear Validity Report').should('be.disabled')
  })

  it('should render with report', () => {
    cy.mountWithProviders(<ToolbarClear />, { wrapper: getPolicyPublishWrapper([{ node: MOCK_NODE_DATA_POLICY }]) })
    cy.get('button').should('have.attr', 'aria-label', 'Clear Validity Report').should('not.be.disabled')
    cy.get('button').click()
    cy.get('button').should('be.disabled')
  })

  it('should be accessible', () => {
    cy.injectAxe()
    cy.mountWithProviders(<ToolbarClear />, { wrapper: getPolicyPublishWrapper([{ node: MOCK_NODE_DATA_POLICY }]) })

    cy.checkAccessibility()
  })

  // TODO
})
