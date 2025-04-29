import { ClientIcon, PLCTagIcon, TopicFilterIcon, TopicIcon } from '@/components/Icons/TopicIcon.tsx'

describe('TopicIcon', () => {
  beforeEach(() => {
    cy.viewport(100, 100)
  })

  it('should render properly ', () => {
    cy.injectAxe()
    cy.mountWithProviders(<TopicIcon />)
    cy.get('svg').should('have.attr', 'aria-label', 'Topic')
    cy.checkAccessibility()
  })
})

describe('PLCTagIcon', () => {
  beforeEach(() => {
    cy.viewport(100, 100)
  })

  it('should render properly ', () => {
    cy.injectAxe()
    cy.mountWithProviders(<PLCTagIcon />)
    cy.get('svg').should('have.attr', 'aria-label', 'Tag')
    cy.checkAccessibility()
  })
})

describe('ClientIcon', () => {
  beforeEach(() => {
    cy.viewport(100, 100)
  })

  it('should render properly ', () => {
    cy.injectAxe()
    cy.mountWithProviders(<ClientIcon />)
    cy.get('svg').should('have.attr', 'aria-label', 'Client')
    cy.checkAccessibility()
  })
})

describe('TopicFilterIcon', () => {
  beforeEach(() => {
    cy.viewport(100, 100)
  })

  it('should render properly ', () => {
    cy.injectAxe()
    cy.mountWithProviders(<TopicFilterIcon />)
    cy.get('svg').should('have.attr', 'aria-label', 'Topic Filter')
    cy.checkAccessibility()
  })
})
