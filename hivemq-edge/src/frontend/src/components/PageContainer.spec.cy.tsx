/// <reference types="cypress" />
import PageContainer from './PageContainer.tsx'

const MOCK_STATUS_TEXT = 'This is a test'
const MOCK_CONTENT = <div data-testid="the-test-id">This is a dummy component</div>

describe('PageContainer', () => {
  beforeEach(() => {
    // run these tests as if in a desktop
    // browser with a 720p monitor
    cy.viewport(800, 250)
  })
  it('should renders', () => {
    cy.mountWithProviders(<PageContainer title={MOCK_STATUS_TEXT}>{MOCK_CONTENT}</PageContainer>)
    // cy.get('.chakra-alert__title').should('contain.text', 'This is a test')
    // cy.get('.chakra-alert__desc').should('contain.text', 'This is a title')
    // cy.get("[role='alert']").should('have.attr', 'data-status', 'error')
  })

  it('should also render', () => {
    cy.mountWithProviders(<PageContainer>{MOCK_CONTENT}</PageContainer>)
  })
})
