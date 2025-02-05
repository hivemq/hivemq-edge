import type { FC, PropsWithChildren } from 'react'
import { ReactFlowProvider } from 'reactflow'
import DesignerToolbox from '@datahub/components/controls/DesignerToolbox.tsx'

const wrapper: FC<PropsWithChildren> = ({ children }) => <ReactFlowProvider>{children}</ReactFlowProvider>

describe('DesignerToolbox', () => {
  beforeEach(() => {
    cy.viewport(850, 600)
  })

  it('should render properly', () => {
    cy.mountWithProviders(<DesignerToolbox />, { wrapper })
    cy.getByTestId('toolbox-trigger').should('have.attr', 'aria-expanded', 'false')
    cy.getByTestId('toolbox-container').should('not.be.visible')

    cy.getByTestId('toolbox-trigger').click()
    cy.getByTestId('toolbox-trigger').should('have.attr', 'aria-expanded', 'true')
    cy.getByTestId('toolbox-container').should('be.visible')

    cy.getByTestId('toolbox-container').find('header').should('have.text', 'Policy Toolbox')
  })

  it('should be accessible', () => {
    cy.injectAxe()
    cy.mountWithProviders(<DesignerToolbox />, { wrapper })
    cy.getByTestId('toolbox-trigger').click()

    cy.wait(100) // Wait for dropdown (ugly)

    cy.checkAccessibility()
    cy.percySnapshot('Component: DesignerToolbox')
  })
})
