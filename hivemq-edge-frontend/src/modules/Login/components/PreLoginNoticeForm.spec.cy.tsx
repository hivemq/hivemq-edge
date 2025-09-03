import { MOCK_PRE_LOGIN_NOTICE, mockGatewayConfiguration } from '@/api/hooks/useFrontendServices/__handlers__'
import PreLoginNoticeForm from '@/modules/Login/components/PreLoginNoticeForm.tsx'

describe('PreLoginNoticeForm', () => {
  beforeEach(() => {
    cy.viewport(800, 900)
  })

  it('should render properly', () => {
    const onAccept = cy.stub().as('onAccept')
    cy.intercept('/api/v1/frontend/configuration', mockGatewayConfiguration)

    cy.mountWithProviders(<PreLoginNoticeForm notice={MOCK_PRE_LOGIN_NOTICE} onAccept={onAccept} />)

    cy.get('@onAccept').should('not.have.been.called')

    cy.get('h1').eq(1).should('have.text', 'Notice: Entering a Protected System')
    cy.getByTestId('prelogin-notice-form-message').should('have.text', MOCK_PRE_LOGIN_NOTICE.message)
    cy.getByTestId('prelogin-notice-form-consent')
      .should('have.text', MOCK_PRE_LOGIN_NOTICE.consent)
      .should('not.have.attr', 'data-checked')
    cy.getByTestId('prelogin-notice-form-submit').should('be.disabled').should('have.text', 'Proceed to login')

    cy.getByTestId('prelogin-notice-form-consent').click()
    cy.getByTestId('prelogin-notice-form-consent').should('have.attr', 'data-checked')

    cy.getByTestId('prelogin-notice-form-submit').should('not.be.disabled')

    cy.getByTestId('prelogin-notice-form-submit').click()
    cy.get('@onAccept').should('have.been.called')
  })

  it('should force reading', () => {
    const onAccept = cy.stub().as('onAccept')
    cy.mountWithProviders(<PreLoginNoticeForm notice={MOCK_PRE_LOGIN_NOTICE} onAccept={onAccept} forceReading />)

    cy.get('@onAccept').should('not.have.been.called')
    cy.getByTestId('prelogin-notice-form-consent').find('input').should('be.disabled')
    cy.getByTestId('prelogin-notice-form-message').scrollTo('bottom')
    cy.getByTestId('prelogin-notice-form-consent').find('input').should('not.be.disabled')
    cy.getByTestId('prelogin-notice-form-consent').click()
    cy.getByTestId('prelogin-notice-form-consent').should('have.attr', 'data-checked')

    cy.getByTestId('prelogin-notice-form-submit').should('not.be.disabled')

    cy.getByTestId('prelogin-notice-form-submit').click()
    cy.get('@onAccept').should('have.been.called')
  })

  it('should bypass consent if not defined', () => {
    const onAccept = cy.stub().as('onAccept')
    cy.mountWithProviders(
      <PreLoginNoticeForm notice={{ ...MOCK_PRE_LOGIN_NOTICE, consent: undefined }} onAccept={onAccept} />
    )

    cy.get('@onAccept').should('not.have.been.called')
    cy.getByTestId('prelogin-notice-form-consent').should('not.exist')

    cy.getByTestId('prelogin-notice-form-submit').should('not.be.disabled')

    cy.getByTestId('prelogin-notice-form-submit').click()
    cy.get('@onAccept').should('have.been.called')
  })

  it('should be accessible', () => {
    cy.injectAxe()
    cy.mountWithProviders(<PreLoginNoticeForm notice={MOCK_PRE_LOGIN_NOTICE} onAccept={cy.stub} />)
    cy.checkAccessibility()
  })
})
