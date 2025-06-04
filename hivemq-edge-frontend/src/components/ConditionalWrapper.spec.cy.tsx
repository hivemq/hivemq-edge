import { ConditionalWrapper } from '@/components/ConditonalWrapper.tsx'
import { Button, Text } from '@chakra-ui/react'

describe('ConditionalWrapper', () => {
  beforeEach(() => {
    cy.viewport(800, 250)
  })
  it('should renders', () => {
    cy.mountWithProviders(
      <ConditionalWrapper condition={true} wrapper={(children) => <Button data-testid="wrapper">{children}</Button>}>
        <Text data-testid="content">Test</Text>
      </ConditionalWrapper>
    )
    cy.getByTestId('content').should('be.visible')
    cy.getByTestId('wrapper').should('be.visible')
  })

  it('should renders', () => {
    cy.mountWithProviders(
      <ConditionalWrapper condition={false} wrapper={(children) => <Button data-testid="wrapper">{children}</Button>}>
        <Text data-testid="content">Test</Text>
      </ConditionalWrapper>
    )
    cy.getByTestId('content').should('be.visible')
    cy.getByTestId('wrapper').should('not.exist')
  })
})
