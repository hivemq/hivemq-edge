import { AssetMapping } from '@/api/__generated__'
import AssetStatusBadge from '@/modules/Pulse/components/assets/AssetStatusBadge.tsx'

describe('AssetStatusBadge', () => {
  beforeEach(() => {
    cy.viewport(800, 250)
  })

  interface TestStatus {
    status: AssetMapping.status
    expected: string
  }

  const testCases: TestStatus[] = [
    { status: AssetMapping.status.UNMAPPED, expected: 'Unmapped' },
    { status: AssetMapping.status.DRAFT, expected: 'Draft' },
    { status: AssetMapping.status.STREAMING, expected: 'Streaming' },
    { status: AssetMapping.status.REQUIRES_REMAPPING, expected: 'Requires Remapping' },
  ]

  it.each(testCases)(
    ({ status }) => `should render ${status}`,
    ({ status, expected }) => {
      cy.injectAxe()
      cy.mountWithProviders(<AssetStatusBadge status={status} />)
      cy.getByTestId('asset-status').should('have.text', expected).should('have.attr', 'data-status', status)
      cy.checkAccessibility()
    }
  )
})
