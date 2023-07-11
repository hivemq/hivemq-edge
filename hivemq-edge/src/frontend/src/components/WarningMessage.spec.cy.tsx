/// <reference types="cypress" />
import WarningMessage from '@/components/WarningMessage.tsx'

const MOCK_PROMPT = 'This is a prompt'
const MOCK_TITLE = 'Nothing in here'
const MOCK_ALT = 'my image'

describe('WarningMessage', () => {
  beforeEach(() => {
    // run these tests as if in a desktop
    // browser with a 720p monitor
    cy.viewport(800, 500)
  })

  it('should renders', () => {
    cy.mountWithProviders(<WarningMessage title={MOCK_TITLE} prompt={MOCK_PROMPT} alt={MOCK_ALT} />)
    cy.get('h2').should('contain.text', MOCK_TITLE)
    cy.get(`img[alt="${MOCK_ALT}"]`).should('exist')
  })

  it('should be accessible', () => {
    cy.injectAxe()
    cy.mountWithProviders(<WarningMessage title={MOCK_TITLE} prompt={MOCK_PROMPT} alt={MOCK_ALT} />)
    cy.checkAccessibility()
    cy.percySnapshot('Component: WarningMessage')
  })
})
