/// <reference types="cypress" />

import { Node } from 'reactflow'

import { MOCK_DEFAULT_NODE } from '@/__test-utils__/react-flow/nodes.ts'

import { MockChecksStoreWrapper } from '@datahub/__test-utils__/MockStoreWrapper.tsx'
import { ToolboxDryRun } from '@datahub/components/controls/ToolboxDryRun.tsx'
import { DataHubNodeType, DataPolicyData } from '@datahub/types.ts'

const MOCK_NODE_DATA_POLICY: Node<DataPolicyData> = {
  id: 'node-id',
  type: DataHubNodeType.DATA_POLICY,
  data: {},
  ...MOCK_DEFAULT_NODE,
  position: { x: 0, y: 0 },
}

const wrapper: React.JSXElementConstructor<{ children: React.ReactNode }> = ({ children }) => (
  <MockChecksStoreWrapper
    config={{
      node: MOCK_NODE_DATA_POLICY,
    }}
  >
    {children}
  </MockChecksStoreWrapper>
)

describe('ToolboxDryRun', () => {
  beforeEach(() => {
    cy.viewport(800, 600)
  })

  it('should renders properly', () => {
    cy.mountWithProviders(<ToolboxDryRun />, { wrapper })

    cy.getByTestId('toolbox-policy-check').should('have.text', 'Check')
    cy.getByTestId('toolbox-policy-check-status').should('not.exist')

    cy.getByTestId('toolbox-policy-check').click()

    cy.getByTestId('toolbox-policy-check').should('contain.text', 'Checking ...')
    cy.getByTestId('toolbox-policy-check-status').should('be.visible')
    cy.getByTestId('toolbox-policy-check-status').should('have.attr', 'data-status', 'warning')

    cy.getByAriaLabel('Close').click()
  })

  it('should be accessible', () => {
    cy.injectAxe()
    cy.mountWithProviders(<ToolboxDryRun />, { wrapper })
    cy.getByTestId('toolbox-policy-check').click()
    cy.checkAccessibility()
    cy.percySnapshot('Component: ToolboxDryRun')
  })
})
