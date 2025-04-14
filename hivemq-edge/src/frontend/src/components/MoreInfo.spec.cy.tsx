import { WAIT_FOR_ANIMATION } from '../../cypress/config.utils'
import MoreInfo from './MoreInfo'

const MOCK_STATUS_TEXT = 'This is a test'

describe('MoreInfo', () => {
  beforeEach(() => {
    cy.viewport(800, 250)
  })

  it('should render properly', () => {
    cy.mountWithProviders(<MoreInfo description={MOCK_STATUS_TEXT} />)
    cy.getByTestId('more-info-trigger').should('have.attr', 'aria-label', 'More information')

    cy.getByTestId('more-info-popover').should('not.be.visible')
    cy.getByTestId('more-info-trigger').click()

    cy.getByTestId('more-info-popover').should('be.visible')
    cy.getByTestId('more-info-message').should('have.text', 'This is a test')
    cy.getByTestId('more-info-link').should('not.exist')
  })

  it('should render properly with link', () => {
    cy.mountWithProviders(<MoreInfo description={MOCK_STATUS_TEXT} link={'http://my.example.com'} />)
    cy.getByTestId('more-info-trigger').click()
    cy.getByTestId('more-info-popover').should('be.visible')
    cy.getByTestId('more-info-message').should('have.text', 'This is a test')
    cy.getByTestId('more-info-link')
      .should('contain.text', 'More information')
      .should('have.attr', 'target', '_blank')
      .should('have.attr', 'href')
  })

  it.only('should be accessible', () => {
    cy.injectAxe()
    cy.mountWithProviders(<MoreInfo description={MOCK_STATUS_TEXT} link={'http://my.example.com'} />)
    cy.getByTestId('more-info-trigger').realClick()

    cy.wait(WAIT_FOR_ANIMATION)
    cy.getByTestId('more-info-popover').should('be.visible')
    cy.checkAccessibility()
  })
})
