import { NodeTypes } from '@/modules/Workspace/types.ts'
import anyLogo from '@/assets/edge/05-icon-industrial-hivemq-edge.svg'
import { Button } from '@chakra-ui/react'

import NodeNameCard from './NodeNameCard.tsx'
import { mockProtocolAdapter } from '@/api/hooks/useProtocolAdapters/__handlers__'

describe('NodeNameCard', () => {
  beforeEach(() => {
    cy.viewport(400, 400)
    cy.intercept('/api/v1/management/protocol-adapters/types', { items: [mockProtocolAdapter] })
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

  it('should render the pulse agent properly', () => {
    cy.mountWithProviders(<NodeNameCard type={NodeTypes.PULSE_NODE} name="Pulse Agent" description="the description" />)

    cy.getByTestId('node-type-icon').should('exist').should('have.attr', 'data-nodeicon', NodeTypes.PULSE_NODE)
    cy.getByTestId('node-name').should('have.text', 'Pulse Agent')
    cy.getByTestId('node-description').should('have.text', 'the description')
  })

  it('should render the assets properly', () => {
    cy.mountWithProviders(
      <NodeNameCard type={NodeTypes.ASSETS_NODE} name="Asset Mapper" description="the description" />
    )

    cy.getByTestId('node-type-icon').should('exist').should('have.attr', 'data-nodeicon', NodeTypes.ASSETS_NODE)
    cy.getByTestId('node-name').should('have.text', 'Asset Mapper')
    cy.getByTestId('node-description').should('have.text', 'the description')
  })

  it('should render the right addon', () => {
    const onClick = cy.stub().as('onClick')
    cy.mountWithProviders(
      <NodeNameCard
        type={NodeTypes.ASSETS_NODE}
        name="Asset Mapper"
        description="the description"
        rightElement={
          <Button data-testid="right-element" onClick={onClick}>
            Click me
          </Button>
        }
      />
    )

    cy.get('@onClick').should('not.have.been.called')
    cy.getByTestId('right-element').should('have.text', 'Click me')
    cy.getByTestId('right-element').click()
    cy.get('@onClick').should('have.been.called')
  })
})
