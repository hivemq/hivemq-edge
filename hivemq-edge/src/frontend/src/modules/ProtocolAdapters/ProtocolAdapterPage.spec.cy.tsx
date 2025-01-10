import { FC, PropsWithChildren } from 'react'
import { Card, CardBody, CardHeader, Code } from '@chakra-ui/react'
import { useLocation } from 'react-router-dom'

import ProtocolAdapterPage from '@/modules/ProtocolAdapters/ProtocolAdapterPage.tsx'

const Wrapper: FC<PropsWithChildren> = ({ children }) => {
  const { pathname } = useLocation()

  return (
    <>
      {children}
      <Card mt={50} variant="filled">
        <CardHeader>Testing Dashboard</CardHeader>
        <CardBody data-testid="test-pathname" as={Code}>
          {pathname}
        </CardBody>
      </Card>
    </>
  )
}

describe('ProtocolAdapterPage', () => {
  beforeEach(() => {
    cy.viewport(800, 900)
  })

  it('should render the page header', () => {
    cy.mountWithProviders(<ProtocolAdapterPage />, {
      wrapper: Wrapper,
      routerProps: { initialEntries: [`/protocol-adapters`] },
    })

    cy.getByTestId('page-container-header').find('h1').should('have.text', 'Protocol Adapters')

    cy.getByTestId('page-container-header')
      .find('h1 + p')
      .should(
        'contain.text',
        'Protocol adapters provide the ability to connect your HiveMQ Edge installation to local and remote devices'
      )

    cy.getByTestId('test-pathname').should('have.text', '/protocol-adapters')

    cy.getByTestId('page-container-cta').find('button').should('have.text', 'Add a new adapter')
    cy.getByTestId('page-container-cta').find('button').click()

    cy.getByTestId('page-container-cta').find('button').should('have.text', 'Back to active adapters')
    cy.getByTestId('test-pathname').should('have.text', '/protocol-adapters/catalog')

    cy.getByTestId('page-container-cta').find('button').click()
    cy.getByTestId('test-pathname').should('have.text', '/protocol-adapters')
  })

  it('should be accessible', () => {
    cy.injectAxe()
    cy.mountWithProviders(<ProtocolAdapterPage />, {
      wrapper: Wrapper,
      routerProps: { initialEntries: [`/protocol-adapters`] },
    })
    cy.checkAccessibility()
  })
})
