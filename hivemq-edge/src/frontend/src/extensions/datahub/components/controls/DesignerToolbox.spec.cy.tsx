import { FC, PropsWithChildren } from 'react'
import { ReactFlowProvider } from 'reactflow'
import DesignerToolbox from '@datahub/components/controls/DesignerToolbox.tsx'

const wrapper: FC<PropsWithChildren> = ({ children }) => <ReactFlowProvider>{children}</ReactFlowProvider>

describe('DesignerToolbox', () => {
  beforeEach(() => {
    cy.viewport(850, 250)
  })

  it('should renders properly', () => {
    cy.mountWithProviders(<DesignerToolbox />, { wrapper })
    cy.getByTestId('toolbox-trigger').should('have.attr', 'aria-expanded', 'false')
    cy.getByTestId('toolbox-container').should('not.be.visible')

    cy.getByTestId('toolbox-trigger').click()
    cy.getByTestId('toolbox-trigger').should('have.attr', 'aria-expanded', 'true')
    cy.getByTestId('toolbox-container').should('be.visible')
  })

  it('should be accessible', () => {
    cy.injectAxe()
    cy.mountWithProviders(<DesignerToolbox />, { wrapper })
    cy.getByTestId('toolbox-trigger').click()
    cy.checkAccessibility()
    cy.percySnapshot('Component: DesignerToolbox')
  })
})
