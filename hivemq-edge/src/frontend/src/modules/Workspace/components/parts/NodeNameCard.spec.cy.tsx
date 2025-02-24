import { NodeTypes } from '@/modules/Workspace/types.ts'
import anyLogo from '@/assets/edge/05-icon-industrial-hivemq-edge.svg'

import NodeNameCard from './NodeNameCard.tsx'
import { mockProtocolAdapter } from '@/api/hooks/useProtocolAdapters/__handlers__'

describe('NodeNameCard', () => {
  beforeEach(() => {
    cy.viewport(400, 400)
    cy.intercept('/api/v1/management/protocol-adapters/types', { items: [mockProtocolAdapter] }).as('getConfig')
  })

  it('should render adapter properly', () => {
    cy.mountWithProviders(
      <NodeNameCard type={NodeTypes.ADAPTER_NODE} name="The adapter" description="The adapter type" icon={anyLogo} />
    )

    cy.getByTestId('node-type-icon').should('exist').should('have.attr', 'data-nodeicon', NodeTypes.ADAPTER_NODE)
    cy.getByTestId('node-name').should('contain.text', 'The adapter')
    cy.getByTestId('node-description').should('contain.text', 'The adapter type')
  })

  it('should render bridge properly', () => {
    cy.mountWithProviders(<NodeNameCard type={NodeTypes.BRIDGE_NODE} name="The Bridge" description="Bridge" />)

    cy.getByTestId('node-type-icon').should('exist').should('have.attr', 'data-nodeicon', NodeTypes.BRIDGE_NODE)
    cy.getByTestId('node-name').should('contain.text', 'The Bridge')
    cy.getByTestId('node-description').should('contain.text', 'Bridge')
  })

  it('should render groups properly', () => {
    cy.mountWithProviders(<NodeNameCard type={NodeTypes.CLUSTER_NODE} name="The Group" description="Group" />)

    cy.getByTestId('node-type-icon').should('exist').should('have.attr', 'data-nodeicon', NodeTypes.CLUSTER_NODE)
    cy.getByTestId('node-name').should('contain.text', 'The Group')
    cy.getByTestId('node-description').should('contain.text', 'Group')
  })

  it('should render the edge properly', () => {
    cy.mountWithProviders(<NodeNameCard type={NodeTypes.EDGE_NODE} name="The Edge" />)

    cy.getByTestId('node-type-icon').should('exist').should('have.attr', 'data-nodeicon', NodeTypes.EDGE_NODE)
    cy.getByTestId('node-name').should('contain.text', 'The Edge')
    cy.getByTestId('node-description').should('not.exist')
  })

  it('should render the devices properly', () => {
    cy.mountWithProviders(<NodeNameCard type={NodeTypes.DEVICE_NODE} />)

    cy.getByTestId('node-type-icon').should('exist').should('have.attr', 'data-nodeicon', NodeTypes.DEVICE_NODE)
    cy.getByTestId('node-name').should('not.exist')
    cy.getByTestId('node-description').should('not.exist')
  })

  it('should render the combiner properly', () => {
    cy.mountWithProviders(<NodeNameCard type={NodeTypes.COMBINER_NODE} />)

    cy.getByTestId('node-type-icon').should('exist').should('have.attr', 'data-nodeicon', NodeTypes.COMBINER_NODE)
    cy.getByTestId('node-name').should('not.exist')
    cy.getByTestId('node-description').should('not.exist')
  })
})
