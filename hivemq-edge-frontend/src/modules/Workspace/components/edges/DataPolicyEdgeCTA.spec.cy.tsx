import DataPolicyEdgeCTA from './DataPolicyEdgeCTA.tsx'

describe('DataPolicyEdgeCTA', () => {
  beforeEach(() => {
    cy.viewport(400, 400)
  })

  it('should not render the group', () => {
    cy.mountWithProviders(<DataPolicyEdgeCTA policyRoutes={[]} />)
    cy.getByTestId('reactFlow-edge-policy-group').should('not.exist')
  })

  it('should render the buttons', () => {
    cy.mountWithProviders(<DataPolicyEdgeCTA policyRoutes={['1', '2']} onClickPolicy={cy.stub().as('onPolicy')} />)
    cy.getByTestId('reactFlow-edge-policy-group').should('be.visible')
    cy.get('button').should('have.length', 2)
    cy.get('@onPolicy').should('not.have.been.called')
    cy.get('button').eq(0).click()
    cy.get('@onPolicy').should('have.been.calledWith', '1')
    cy.get('@onPolicy').should('not.have.been.calledWith', '2')
    cy.get('button').eq(1).click()
    cy.get('@onPolicy').should('have.been.calledWith', '2')
  })

  it('should render the buttons', () => {
    cy.mountWithProviders(
      <DataPolicyEdgeCTA
        policyRoutes={['1', '2', '3', '4']}
        onClickPolicy={cy.stub().as('onPolicy')}
        onClickAll={cy.stub().as('onAllPolicy')}
      />
    )
    cy.getByTestId('reactFlow-edge-policy-group').should('be.visible')
    cy.get('button').should('have.length', 3)
    cy.get('@onPolicy').should('not.have.been.called')
    cy.get('button').eq(0).click()
    cy.get('@onPolicy').should('have.been.calledWith', '1')

    cy.get('@onAllPolicy').should('not.have.been.called')
    cy.get('button').eq(2).click()
    cy.get('@onAllPolicy').should('have.been.called')
  })

  it('should be accessible', () => {
    cy.injectAxe()
    cy.mountWithProviders(<DataPolicyEdgeCTA policyRoutes={['1', '2', '3', '4']} />)
    cy.checkAccessibility()
    cy.percySnapshot('Component: DataPolicyEdgeCTA')
  })
})
