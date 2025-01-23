import type { FC, PropsWithChildren } from 'react'
import { useLocalStorage } from '@uidotdev/usehooks'
import { Text } from '@chakra-ui/react'
import type { PrivacySourceGranted } from '@/modules/Trackers/PrivacyConsentBanner.tsx'
import PrivacyConsentBanner from '@/modules/Trackers/PrivacyConsentBanner.tsx'
import { mockGatewayConfiguration } from '@/api/hooks/useFrontendServices/__handlers__'

const Wrapper: FC<PropsWithChildren> = ({ children }) => {
  const [localStorage] = useLocalStorage<PrivacySourceGranted | undefined>('edge.privacy', undefined)
  return (
    <>
      <Text data-testid="stdout">{JSON.stringify(localStorage || { error: 'no content' })}</Text>
      {children}
    </>
  )
}

describe('PrivacyConsentBanner', () => {
  beforeEach(() => {
    // run these tests as if in a desktop
    // browser with a 720p monitor
    cy.viewport(800, 250)
    cy.intercept('/api/v1/frontend/configuration', mockGatewayConfiguration)
  })

  it('should render properly', () => {
    cy.mountWithProviders(<PrivacyConsentBanner />, { wrapper: Wrapper })

    cy.getAllLocalStorage().then((sss) => {
      const localStorage = Object.values(sss)[0]
      expect(localStorage['edge.privacy']).to.be.undefined
    })

    cy.get('header').should('be.visible')
    cy.get('header').should('contain.text', 'Privacy Settings')
    cy.get('[role="dialog"]').should(
      'contain.text',
      'This web application uses third-party tracking technologies to continually improve our services.'
    )

    cy.getByTestId('privacy-info').should('have.attr', 'href', 'https://github.com/hivemq/hivemq-edge')
    cy.getByTestId('privacy-optOut').should('contain.text', 'Deny')
    cy.getByTestId('privacy-optIn').should('contain.text', 'Accept all')
  })

  it.skip('should support opt out', () => {
    cy.mountWithProviders(<PrivacyConsentBanner />, { wrapper: Wrapper })

    cy.getAllLocalStorage().then((sss) => {
      const localStorage = Object.values(sss)[0]
      expect(localStorage['edge.privacy']).to.be.undefined
    })

    cy.get('header').should('be.visible')
    cy.getByTestId('privacy-optOut').click()

    cy.getAllLocalStorage().then((sss) => {
      const localStorage = Object.values(sss)[0]
      expect(localStorage['edge.privacy']).to.equal(JSON.stringify({ heapAnalytics: false, sentry: false }))
    })
    cy.get('[role="dialog"]').should('not.exist')
  })

  it.skip('should support opt in', () => {
    cy.mountWithProviders(<PrivacyConsentBanner />, { wrapper: Wrapper })

    cy.getAllLocalStorage().then((sss) => {
      const localStorage = Object.values(sss)[0]
      expect(localStorage['edge.privacy']).to.be.undefined
    })

    cy.get('header').should('be.visible')
    cy.getByTestId('privacy-optIn').click()

    cy.getAllLocalStorage().then((sss) => {
      const localStorage = Object.values(sss)[0]
      expect(localStorage['edge.privacy']).to.equal(JSON.stringify({ heapAnalytics: true, sentry: true }))
    })
    cy.get('[role="dialog"]').should('not.exist')
  })

  it('should be accessible', () => {
    cy.injectAxe()
    cy.mountWithProviders(<PrivacyConsentBanner />, { wrapper: Wrapper })
    cy.get('header').should('be.visible')
    cy.checkAccessibility()
    cy.percySnapshot('Component: PrivacyConsentBanner')
  })
})
