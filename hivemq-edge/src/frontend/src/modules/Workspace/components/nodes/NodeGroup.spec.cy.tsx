/// <reference types="cypress" />

import NodeGroup from './NodeGroup.tsx'
import { NodeTypes } from '@/modules/Workspace/types.ts'
import { MOCK_NODE_GROUP } from '@/__test-utils__/react-flow/nodes.ts'
import { CustomNodeTesting } from '@/__test-utils__/react-flow/CustomNodeTesting.tsx'

describe('NodeGroup', () => {
  beforeEach(() => {
    cy.viewport(400, 400)
  })

  it('should render unselected group properly', () => {
    cy.mountWithProviders(
      <CustomNodeTesting
        nodes={[{ ...MOCK_NODE_GROUP, position: { x: 50, y: 100 } }]}
        nodeTypes={{ [NodeTypes.CLUSTER_NODE]: NodeGroup }}
      />
    )

    cy.get(`#node-group-${MOCK_NODE_GROUP.id}`).should('contain.text', 'The group title')

    cy.get('[role="toolbar"]').should('have.length', 0)

    cy.getByTestId('test-navigate-pathname').should('have.text', '/')
    cy.get(`#node-group-${MOCK_NODE_GROUP.id}`).rightclick()
    cy.getByTestId('test-navigate-pathname').should('have.text', `/`)
  })

  it('should render selected group properly', () => {
    cy.mountWithProviders(
      <CustomNodeTesting
        nodes={[{ ...MOCK_NODE_GROUP, position: { x: 50, y: 100 }, selected: true }]}
        nodeTypes={{ [NodeTypes.CLUSTER_NODE]: NodeGroup }}
      />
    )

    cy.get(`#node-group-${MOCK_NODE_GROUP.id}`).should('contain.text', 'The group title')

    cy.get('[role="toolbar"]').should('have.length', 1)

    cy.getByTestId('test-navigate-pathname').should('have.text', '/')
    cy.get(`#node-group-${MOCK_NODE_GROUP.id}`).rightclick()
    cy.getByTestId('test-navigate-pathname').should('have.text', `/workspace/group/${MOCK_NODE_GROUP.id}`)
  })

  it('should render the toolbar properly', () => {
    cy.mountWithProviders(
      <CustomNodeTesting
        nodes={[{ ...MOCK_NODE_GROUP, position: { x: 50, y: 100 }, selected: true }]}
        nodeTypes={{ [NodeTypes.CLUSTER_NODE]: NodeGroup }}
      />
    )

    cy.get(`#node-group-${MOCK_NODE_GROUP.id}`)
      .should('contain.text', 'The group title')
      .should('have.attr', 'data-groupopen', 'true')

    cy.getByTestId('node-group-toolbar-expand').should('have.attr', 'aria-label', 'Collapse group')
    cy.getByTestId('node-group-toolbar-ungroup').should('have.attr', 'aria-label', 'Ungroup')
    cy.getByTestId('node-group-toolbar-group').should('have.attr', 'aria-label', 'Group the selected adapters')

    cy.getByTestId('node-group-toolbar-panel').should('have.attr', 'aria-label', 'Open the overview panel')

    cy.getByTestId('node-group-toolbar-expand').click()
    cy.get(`#node-group-${MOCK_NODE_GROUP.id}`).should('have.attr', 'data-groupopen', 'false')

    cy.get('[role="alertdialog"]').should('not.exist')
    cy.getByTestId('node-group-toolbar-ungroup').click()
    cy.get('[role="alertdialog"]').should('contain.text', 'Ungroup the adapters')
    cy.getByTestId('confirmation-cancel').click()

    cy.getByTestId('test-navigate-pathname').should('have.text', '/')
    cy.getByTestId('node-group-toolbar-panel').click()
    cy.getByTestId('test-navigate-pathname').should('have.text', `/workspace/group/${MOCK_NODE_GROUP.id}`)
  })

  it('should be accessible', () => {
    cy.injectAxe()
    cy.mountWithProviders(
      <CustomNodeTesting
        nodes={[{ ...MOCK_NODE_GROUP, position: { x: 100, y: 100 }, selected: true }]}
        nodeTypes={{ [NodeTypes.CLUSTER_NODE]: NodeGroup }}
      />
    )
    cy.get("[role='button']").click({ force: true })
    cy.checkAccessibility()
    cy.percySnapshot('Component: NodeGroup')
  })
})
