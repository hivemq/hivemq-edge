/// <reference types="cypress" />

import { Node } from 'reactflow'

import { MOCK_DEFAULT_NODE } from '@/__test-utils__/react-flow/nodes.ts'

import { MockChecksStoreWrapper } from '@datahub/__test-utils__/MockStoreWrapper.tsx'
import { ToolbarDryRun } from '@datahub/components/toolbar/ToolbarDryRun.tsx'
import { DataHubNodeType, DataPolicyData, PolicyDryRunStatus } from '@datahub/types.ts'

const MOCK_NODE_DATA_POLICY: Node<DataPolicyData> = {
  id: 'node-id',
  type: DataHubNodeType.DATA_POLICY,
  data: { id: 'my-policy-id' },
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

describe('ToolbarDryRun', () => {
  beforeEach(() => {
    cy.viewport(800, 600)
  })

  it('should renders properly', () => {
    cy.mountWithProviders(<ToolbarDryRun />, { wrapper })

    cy.getByTestId('toolbox-policy-check')
      .should('have.text', 'Check')
      .should('have.attr', 'data-status', PolicyDryRunStatus.IDLE)
    cy.getByTestId('toolbox-policy-check').click()

    cy.getByTestId('toolbox-policy-check')
      .should('contain.text', 'Checking ...')
      .should('have.attr', 'data-status', PolicyDryRunStatus.RUNNING)

    cy.getByTestId('toolbox-policy-check')
      .should('have.text', 'Check')
      .should('have.attr', 'data-status', PolicyDryRunStatus.FAILURE)

    cy.getByTestId('toolbox-policy-clear').click()
  })

  it('should be accessible', () => {
    cy.injectAxe()
    cy.mountWithProviders(<ToolbarDryRun />, { wrapper })
    cy.getByTestId('toolbox-policy-check').click()

    cy.checkAccessibility()
    cy.percySnapshot('Component: ToolbarDryRun')
  })
})
