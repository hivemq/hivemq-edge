/// <reference types="cypress" />

import { Node } from 'reactflow'

import { MOCK_DEFAULT_NODE } from '@/__test-utils__/react-flow/nodes.ts'

import { MockChecksStoreWrapper } from '@datahub/__test-utils__/MockStoreWrapper.tsx'
import { ToolbarDryRun } from '@datahub/components/toolbar/ToolbarDryRun.tsx'
import { DataHubNodeType, DataPolicyData } from '@datahub/types.ts'

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

    cy.getByTestId('toolbox-policy-check').should('have.text', 'Check')
    cy.getByTestId('toolbox-policy-check-status').should('not.exist')

    cy.getByTestId('toolbox-policy-check').click()

    cy.getByTestId('toolbox-policy-check').should('contain.text', 'Checking ...')
    cy.getByTestId('toolbox-policy-check-status').should('be.visible')
    cy.getByTestId('toolbox-policy-check-status').should('have.attr', 'data-status', 'warning')

    cy.getByAriaLabel('Close').click()
  })

  it('should allow access to node', () => {
    cy.mountWithProviders(<ToolbarDryRun />, { wrapper })
    cy.getByTestId('toolbox-policy-check').click()
    cy.get('h2 button').eq(0).click()

    cy.get('@onShowNode').should('not.have.been.calledWith')
    cy.getByTestId('report-error-fitView').click()
    cy.get('@onShowNode').should('have.been.calledWithMatch', { id: 'node-id', type: DataHubNodeType.DATA_POLICY })

    cy.get('@onShowEditor').should('not.have.been.calledWith')
    cy.getByTestId('report-error-config').click()
    cy.get('@onShowEditor').should('have.been.calledWithMatch', { id: 'node-id', type: DataHubNodeType.DATA_POLICY })
    cy.getByAriaLabel('Close').click()
  })

  it('should be accessible', () => {
    cy.injectAxe()
    cy.mountWithProviders(<ToolbarDryRun />, { wrapper })
    cy.getByTestId('toolbox-policy-check').click()
    cy.get('h2 button').eq(0).click()
    cy.checkAccessibility(undefined, {
      rules: {
        // TODO[NVL] CTooltip seems to generate false positives
        'color-contrast': { enabled: false },
      },
    })
    cy.percySnapshot('Component: ToolboxDryRun')
  })
})
