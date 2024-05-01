import PrivacyConsentBanner from '@/components/PrivacyConsentBanner.tsx'

describe('PrivacyConsentBanner', () => {
  beforeEach(() => {
    // run these tests as if in a desktop
    // browser with a 720p monitor
    cy.viewport(800, 250)
  })

  it('should render properly', () => {
    cy.mountWithProviders(<PrivacyConsentBanner />)

    cy.getAllLocalStorage().then((sss) => {
      const localStorage = Object.values(sss)[0]
      expect(localStorage['edge.privacy']).to.be.undefined
    })

    cy.get('header').should('contain.text', 'Privacy Settings')
    cy.get('[role="dialog"]').should(
      'contain.text',
      'This web application uses third-party website tracking technologies'
    )

    cy.getByTestId('privacy-info').should('have.attr', 'href', 'https://www.hivemq.com/legal/imprint/')
    cy.getByTestId('privacy-optOut').should('contain.text', 'Deny')
    cy.getByTestId('privacy-optIn').should('contain.text', 'Accept all')
  })

  it('should support opt out', () => {
    cy.mountWithProviders(<PrivacyConsentBanner />)

    cy.getAllLocalStorage().then((sss) => {
      const localStorage = Object.values(sss)[0]
      expect(localStorage['edge.privacy']).to.be.undefined
    })

    cy.getByTestId('privacy-optOut').click()

    cy.getAllLocalStorage().then((sss) => {
      const localStorage = Object.values(sss)[0]
      expect(localStorage['edge.privacy']).to.equal(JSON.stringify({ heapAnalytics: false, sentry: false }))
    })
    cy.get('[role="dialog"]').should('not.exist')
  })

  it('should support opt in', () => {
    cy.mountWithProviders(<PrivacyConsentBanner />)

    cy.getAllLocalStorage().then((sss) => {
      const localStorage = Object.values(sss)[0]
      expect(localStorage['edge.privacy']).to.be.undefined
    })

    cy.getByTestId('privacy-optIn').click()

    cy.getAllLocalStorage().then((sss) => {
      const localStorage = Object.values(sss)[0]
      expect(localStorage['edge.privacy']).to.equal(JSON.stringify({ heapAnalytics: true, sentry: true }))
    })
    cy.get('[role="dialog"]').should('not.exist')
  })
})
