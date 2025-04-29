import { getPolicyPublishWrapper, MOCK_NODE_DATA_POLICY } from '@datahub/__test-utils__/react-flow.mocks.tsx'
import { ToolbarShowReport } from '@datahub/components/toolbar/ToolbarShowReport.tsx'

describe('ToolbarShowReport', () => {
  beforeEach(() => {
    cy.viewport(800, 250)
  })

  it('should render without report', () => {
    cy.mountWithProviders(<ToolbarShowReport />, { wrapper: getPolicyPublishWrapper() })
    cy.get('button').should('have.text', 'Show Report').should('be.disabled')
  })

  it('should render with report', () => {
    cy.mountWithProviders(<ToolbarShowReport />, {
      wrapper: getPolicyPublishWrapper([{ node: MOCK_NODE_DATA_POLICY }]),
    })
    cy.get('button').should('have.text', 'Show Report').should('not.be.disabled')
    cy.getByTestId('test-dashboard').should('have.text', '/')
    cy.get('button').click()
    cy.getByTestId('test-dashboard').should('have.text', '/validation/')
  })

  it('should be accessible', () => {
    cy.injectAxe()
    cy.mountWithProviders(<ToolbarShowReport />, {
      wrapper: getPolicyPublishWrapper([{ node: MOCK_NODE_DATA_POLICY }]),
    })

    cy.checkAccessibility()
  })
})
