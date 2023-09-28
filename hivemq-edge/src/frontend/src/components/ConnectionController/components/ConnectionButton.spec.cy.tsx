/// <reference types="cypress" />

import ConnectionButton from './ConnectionButton.tsx'
import { StatusTransitionCommand } from '@/api/__generated__'

const MOCK_ID = 'my-id'

describe('ConnectionButton', () => {
  beforeEach(() => {
    cy.viewport(400, 150)
  })

  it('should be accessible', () => {
    cy.injectAxe()
    cy.mountWithProviders(<ConnectionButton id={'my-id'} isRunning />)
    cy.checkAccessibility()
    cy.percySnapshot('Component: ConnectionButton')
  })

  it('should render stop CTAs when device is running', () => {
    const onChangeStatus = cy.stub().as('onChangeStatus')
    cy.mountWithProviders(<ConnectionButton id={MOCK_ID} isRunning onChangeStatus={onChangeStatus} />)

    cy.getByTestId('device-action-start').should('not.exist')
    cy.getByTestId('device-action-stop').should('have.attr', 'aria-label', 'Stop')
    cy.getByTestId('device-action-stop').click()
    cy.get('@onChangeStatus').should('have.been.calledWith', MOCK_ID, StatusTransitionCommand.command.STOP)

    cy.getByTestId('device-action-restart').should('have.attr', 'aria-label', 'Restart')
    cy.getByTestId('device-action-restart').click()
    cy.get('@onChangeStatus').should('have.been.calledWith', MOCK_ID, StatusTransitionCommand.command.RESTART)
  })

  it('should render start CTAs when device is not running', () => {
    const onChangeStatus = cy.stub().as('onChangeStatus')
    cy.mountWithProviders(<ConnectionButton id={MOCK_ID} isRunning={false} onChangeStatus={onChangeStatus} />)

    cy.getByTestId('device-action-stop').should('not.exist')
    cy.getByTestId('device-action-start').should('have.attr', 'aria-label', 'Start')
    cy.getByTestId('device-action-start').click()
    cy.get('@onChangeStatus').should('have.been.calledWith', MOCK_ID, StatusTransitionCommand.command.START)

    cy.getByTestId('device-action-restart').should('have.attr', 'aria-label', 'Restart')
    cy.getByTestId('device-action-restart').should('be.disabled')
  })

  it('should render disabled states when isLoading (and running)', () => {
    cy.mountWithProviders(<ConnectionButton id={MOCK_ID} isRunning isLoading />)

    cy.getByTestId('device-action-stop').should('be.disabled')
    cy.getByTestId('device-action-restart').should('be.disabled')
  })

  it('should render disabled states when isLoading (and not running)', () => {
    cy.mountWithProviders(<ConnectionButton id={MOCK_ID} isRunning={false} isLoading />)

    cy.getByTestId('device-action-start').should('be.disabled')
    cy.getByTestId('device-action-restart').should('be.disabled')
  })
})
