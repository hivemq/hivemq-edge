/// <reference types="cypress" />

import NodeGroup from './NodeGroup.tsx'
import { NodeTypes } from '@/modules/Workspace/types.ts'
import { MOCK_NODE_GROUP } from '@/__test-utils__/react-flow/nodes.ts'
import { CustomNodeTesting } from '@/__test-utils__/react-flow/CustomNodeTesting.tsx'

describe('NodeGroup', () => {
  beforeEach(() => {
    cy.viewport(400, 400)
  })

  it('should render properly', () => {
    cy.mountWithProviders(
      <CustomNodeTesting
        nodes={[{ ...MOCK_NODE_GROUP, position: { x: 50, y: 100 } }]}
        nodeTypes={{ [NodeTypes.CLUSTER_NODE]: NodeGroup }}
      />
    )

    cy.getByTestId('node-group-toolbar-expand').should('not.exist')

    cy.get("[role='button']").should('contain.text', 'The group title')
    cy.get("[role='button']").click({ force: true })

    cy.getByTestId('node-group-toolbar-panel')
      .should('exist')
      .should('have.attr', 'aria-label', 'Open the overview panel')

    cy.getByTestId('node-group-toolbar-expand').should('exist').should('have.text', 'Collapse group')

    cy.getByTestId('node-group-toolbar-expand').click()
    cy.getByTestId('node-group-toolbar-expand').should('have.text', 'Expand group')

    cy.getByTestId('node-group-toolbar-ungroup').click()
    cy.get("[role='alertdialog']").find('header').should('have.text', 'Ungroup the adapters')
    cy.get("[role='alertdialog']")
      .find('div')
      .should(
        'have.text',
        'Are you sure you want to delete this group? The adapters inside the group will revert to their original state.'
      )
    cy.get("[role='alertdialog']").find('button').eq(0).should('have.text', 'Cancel').click()
    cy.get("[role='alertdialog']").should('not.exist')

    cy.getByTestId('node-group-toolbar-ungroup').click()
    cy.get("[role='alertdialog']").find('button').eq(1).should('have.text', 'Delete').click()
    cy.get("[role='alertdialog']").should('not.exist')
  })

  it('should be accessible', () => {
    cy.injectAxe()
    cy.mountWithProviders(
      <CustomNodeTesting
        nodes={[{ ...MOCK_NODE_GROUP, position: { x: 0, y: 0 } }]}
        nodeTypes={{ [NodeTypes.CLUSTER_NODE]: NodeGroup }}
      />
    )
    cy.checkAccessibility()
    cy.percySnapshot('Component: NodeGroup')
  })
})
