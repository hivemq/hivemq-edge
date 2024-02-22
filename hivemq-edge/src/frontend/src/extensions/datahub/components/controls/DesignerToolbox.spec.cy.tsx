/// <reference types="cypress" />

import DesignerToolbox from '@datahub/components/controls/DesignerToolbox.tsx'
import { FC, PropsWithChildren } from 'react'
import { ReactFlowProvider } from 'reactflow'

type StepStatus = 'active' | 'incomplete' | 'complete'

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

    cy.getByTestId('toolbox-step-build').find('div').first().should('have.attr', 'data-status', 'active')
    cy.getByTestId('toolbox-step-build').find('h2').should('not.exist')
    cy.getByTestId('toolbox-navigation-prev').should('be.disabled')
    cy.getByTestId('toolbox-navigation-next').should('not.be.disabled')

    cy.getByTestId('toolbox-step-check').find('div').first().should('have.attr', 'data-status', 'incomplete')
    cy.getByTestId('toolbox-step-check').find('h2').should('be.visible')

    cy.getByTestId('toolbox-step-publish').find('div').first().should('have.attr', 'data-status', 'incomplete')
    cy.getByTestId('toolbox-step-publish').find('h2').should('be.visible')
  })

  const checkPanels = (build: StepStatus, check: StepStatus, publish: StepStatus) => {
    cy.getByTestId('toolbox-step-build').find('div').first().should('have.attr', 'data-status', build)
    cy.getByTestId('toolbox-step-check').find('div').first().should('have.attr', 'data-status', check)
    cy.getByTestId('toolbox-step-publish').find('div').first().should('have.attr', 'data-status', publish)
    cy.getByTestId('toolbox-navigation-prev').should(build === 'active' ? 'be.disabled' : 'not.be.disabled')
    cy.getByTestId('toolbox-navigation-next').should(publish === 'active' ? 'be.disabled' : 'not.be.disabled')
  }

  it('should support navigation between panels', () => {
    cy.mountWithProviders(<DesignerToolbox />, { wrapper })

    cy.getByTestId('toolbox-trigger').click()
    checkPanels('active', 'incomplete', 'incomplete')

    cy.getByTestId('toolbox-navigation-next').click()
    checkPanels('complete', 'active', 'incomplete')

    cy.getByTestId('toolbox-navigation-next').click()
    checkPanels('complete', 'complete', 'active')

    cy.getByTestId('toolbox-navigation-prev').click()
    checkPanels('complete', 'active', 'incomplete')
  })

  it('should be accessible', () => {
    cy.injectAxe()
    cy.mountWithProviders(<DesignerToolbox />, { wrapper })
    cy.getByTestId('toolbox-trigger').click()
    cy.getByTestId('toolbox-navigation-next').click()
    cy.checkAccessibility()
    cy.percySnapshot('Component: DesignerToolbox')
  })
})
