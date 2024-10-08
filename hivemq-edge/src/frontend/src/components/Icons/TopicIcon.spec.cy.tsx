import { ClientIcon, PLCTagIcon, TopicIcon } from '@/components/Icons/TopicIcon.tsx'

describe('TopicIcon', () => {
  beforeEach(() => {
    cy.viewport(100, 100)
  })

  it('should render properly ', () => {
    cy.mountWithProviders(<TopicIcon />)
    cy.get('svg').should('have.attr', 'aria-label', 'Topic')
    cy.percySnapshot('Component: TopicIcon')
  })
})

describe('PLCTagIcon', () => {
  beforeEach(() => {
    cy.viewport(100, 100)
  })

  it('should render properly ', () => {
    cy.mountWithProviders(<PLCTagIcon />)
    cy.get('svg').should('have.attr', 'aria-label', 'Tag')
    cy.percySnapshot('Component: PLCTagIcon')
  })
})

describe('ClientIcon', () => {
  beforeEach(() => {
    cy.viewport(100, 100)
  })

  it('should render properly ', () => {
    cy.mountWithProviders(<ClientIcon />)
    cy.get('svg').should('have.attr', 'aria-label', 'Client')
    cy.percySnapshot('Component: ClientIcon')
  })
})
