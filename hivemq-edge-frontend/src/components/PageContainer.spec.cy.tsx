/// <reference types="cypress" />
import PageContainer from './PageContainer.tsx'
import { Button } from '@chakra-ui/react'

const MOCK_HEADER = 'This is a test'
const MOCK_SUBHEADER = 'This is below the test'
const MOCK_CONTENT = <div data-testid="the-test-id">This is a dummy component</div>
const MOCK_CTA = <Button>Button</Button>

describe('PageContainer', () => {
  beforeEach(() => {
    cy.viewport(800, 250)
  })

  it('should render properly', () => {
    cy.mountWithProviders(
      <PageContainer title={MOCK_HEADER} subtitle={MOCK_SUBHEADER} cta={MOCK_CTA}>
        {MOCK_CONTENT}
      </PageContainer>
    )
    cy.get('header h1').should('have.text', 'This is a test')
    cy.get('header h1 + p').should('have.text', 'This is below the test')
    cy.getByTestId('page-container-cta').find('button').should('have.text', 'Button')
    cy.getByTestId('the-test-id').should('have.text', 'This is a dummy component')
  })

  it('should be accessible', () => {
    cy.injectAxe()
    cy.mountWithProviders(
      <PageContainer title={MOCK_HEADER} subtitle={MOCK_SUBHEADER} cta={<Button>Button</Button>}>
        {MOCK_CONTENT}
      </PageContainer>
    )

    cy.checkAccessibility()
  })
})
