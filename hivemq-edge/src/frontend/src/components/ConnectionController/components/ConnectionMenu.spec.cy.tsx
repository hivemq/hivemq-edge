/// <reference types="cypress" />

import { StatusTransitionCommand } from '@/api/__generated__'
import ConnectionMenu from '@/components/ConnectionController/components/ConnectionMenu.tsx'
import { Menu, MenuButton, MenuList } from '@chakra-ui/react'
import { FC, PropsWithChildren } from 'react'

const MOCK_ID = 'my-id'

const Wrapper: FC<PropsWithChildren> = ({ children }) => (
  <Menu>
    <MenuButton data-testid={'mock-trigger'}>my custom menu</MenuButton>
    <MenuList>{children}</MenuList>
  </Menu>
)

describe('ConnectionMenu', () => {
  beforeEach(() => {
    cy.viewport(400, 300)
  })

  it('should be accessible', () => {
    cy.injectAxe()
    cy.mountWithProviders(
      <Wrapper>
        <ConnectionMenu id={'my-id'} isRunning />
      </Wrapper>
    )
    cy.checkAccessibility()
    cy.getByTestId('mock-trigger').click()
    cy.percySnapshot('Component: ConnectionMenu')
  })

  it('should render stop CTAs when device is running', () => {
    const onChangeStatus = cy.stub().as('onChangeStatus')

    cy.mountWithProviders(
      <Wrapper>
        <ConnectionMenu id={'my-id'} isRunning onChangeStatus={onChangeStatus} />
      </Wrapper>
    )

    cy.getByTestId('mock-trigger').click()

    cy.getByTestId('device-action-start').should('not.exist')
    cy.getByTestId('device-action-stop').should('have.text', 'Stop')
    cy.getByTestId('device-action-stop').click()
    cy.get('@onChangeStatus').should('have.been.calledWith', MOCK_ID, StatusTransitionCommand.command.STOP)
  })

  it('should render start CTAs when device is not running', () => {
    const onChangeStatus = cy.stub().as('onChangeStatus')

    cy.mountWithProviders(
      <Wrapper>
        <ConnectionMenu id={'my-id'} isRunning={false} onChangeStatus={onChangeStatus} />
      </Wrapper>
    )

    cy.getByTestId('mock-trigger').click()

    cy.getByTestId('device-action-stop').should('not.exist')
    cy.getByTestId('device-action-start').should('have.text', 'Start')
    cy.getByTestId('device-action-start').click()
    cy.get('@onChangeStatus').should('have.been.calledWith', MOCK_ID, StatusTransitionCommand.command.START)
  })

  it('should render restart CTA (not running)', () => {
    const onChangeStatus = cy.stub().as('onChangeStatus')

    cy.mountWithProviders(
      <Wrapper>
        <ConnectionMenu id={'my-id'} isRunning={false} onChangeStatus={onChangeStatus} />
      </Wrapper>
    )

    cy.getByTestId('mock-trigger').click()

    cy.getByTestId('device-action-restart').should('have.text', 'Restart')
    cy.getByTestId('device-action-restart').should('be.disabled')
  })

  it('should render restart CTA (running)', () => {
    const onChangeStatus = cy.stub().as('onChangeStatus')

    cy.mountWithProviders(
      <Wrapper>
        <ConnectionMenu id={'my-id'} isRunning={true} onChangeStatus={onChangeStatus} />
      </Wrapper>
    )

    cy.getByTestId('mock-trigger').click()

    cy.getByTestId('device-action-restart').should('have.text', 'Restart')
    cy.getByTestId('device-action-restart').click()
    cy.get('@onChangeStatus').should('have.been.calledWith', MOCK_ID, StatusTransitionCommand.command.RESTART)
  })

  it('should render disable states when isLoading (running)', () => {
    cy.mountWithProviders(
      <Wrapper>
        <ConnectionMenu id={'my-id'} isRunning isLoading />
      </Wrapper>
    )

    cy.getByTestId('device-action-stop').should('be.disabled')
    cy.getByTestId('device-action-restart').should('be.disabled')
  })

  it('should render disable states when isLoading (not running)', () => {
    cy.mountWithProviders(
      <Wrapper>
        <ConnectionMenu id={'my-id'} isRunning={false} isLoading />
      </Wrapper>
    )

    cy.getByTestId('device-action-start').should('be.disabled')
    cy.getByTestId('device-action-restart').should('be.disabled')
  })
})
