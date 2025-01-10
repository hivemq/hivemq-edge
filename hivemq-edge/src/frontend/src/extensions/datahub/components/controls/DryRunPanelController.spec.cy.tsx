import { FC, PropsWithChildren } from 'react'
import { ReactFlowProvider } from 'reactflow'
import { Route, Routes } from 'react-router-dom'
import DryRunPanelController from '@datahub/components/controls/DryRunPanelController.tsx'

const wrapper: FC<PropsWithChildren> = ({ children }) => (
  <ReactFlowProvider>
    <Routes>
      <Route path="/datahub/:policyType/:policyId?/validation/" element={children}></Route>
    </Routes>
  </ReactFlowProvider>
)

describe('DryRunPanelController', () => {
  beforeEach(() => {
    cy.viewport(600, 800)
  })

  it('should renders errors', () => {
    cy.mountWithProviders(<DryRunPanelController />, {
      wrapper,
      routerProps: { initialEntries: [`/datahub/CREATE_POLICY/validation/`] },
    })

    cy.getByTestId('policy-validity-report').as('panel')
    cy.get('@panel').should('be.visible')
    cy.get('@panel').find('header').should('have.text', 'Report on policy validity')
    cy.get('@panel').find('[role="alert"]').should('have.attr', 'data-status', 'error')
    cy.get('@panel').find('[role="alert"] > div > div').eq(0).should('have.text', 'No information to display')
    cy.get('@panel')
      .find('[role="alert"] > div > div')
      .eq(1)
      .should('have.text', 'Run a check before trying to publish')
  })

  it('should renders properly', () => {
    cy.mountWithProviders(<DryRunPanelController />, {
      wrapper,
      routerProps: { initialEntries: [`/datahub/CREATE_POLICY/validation/`] },
    })
  })

  it('should be accessible', () => {
    cy.injectAxe()
    cy.mountWithProviders(<DryRunPanelController />, {
      wrapper,
      routerProps: { initialEntries: [`/datahub/CREATE_POLICY/validation/`] },
    })

    cy.checkAccessibility()
    cy.percySnapshot('Component: DryRunPanelController')
  })
})
