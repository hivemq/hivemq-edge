import { MOCK_PRE_LOGIN_NOTICE, mockGatewayConfiguration } from '@/api/hooks/useFrontendServices/__handlers__'
import PreLoginNoticeForm from '@/modules/Login/components/PreLoginNoticeForm.tsx'

describe('ConfidentialityForm', () => {
  beforeEach(() => {
    cy.viewport(800, 900)
  })

  it('should render properly', () => {
    const onAccept = cy.stub().as('onAccept')
    cy.intercept('/api/v1/frontend/configuration', mockGatewayConfiguration)

    cy.mountWithProviders(<PreLoginNoticeForm notice={MOCK_PRE_LOGIN_NOTICE} onAccept={onAccept} />)

    cy.get('@onAccept').should('not.have.been.called')

    cy.get('h1').eq(1).should('have.text', 'Notice: Entering a Protected System')
    cy.getByTestId('confidentiality-form-content').should('have.text', MOCK_PRE_LOGIN_NOTICE.message)
    cy.getByTestId('confidentiality-form-agreement')
      .should('have.text', MOCK_PRE_LOGIN_NOTICE.consent)
      .should('not.have.attr', 'data-checked')
    cy.getByTestId('confidentiality-form-submit').should('be.disabled').should('have.text', 'Proceed to login')

    cy.getByTestId('confidentiality-form-agreement').click()
    cy.getByTestId('confidentiality-form-agreement').should('have.attr', 'data-checked')

    cy.getByTestId('confidentiality-form-submit').should('not.be.disabled')

    cy.getByTestId('confidentiality-form-submit').click()
    cy.get('@onAccept').should('have.been.called')
  })

  it('should force reading', () => {
    const onAccept = cy.stub().as('onAccept')
    cy.mountWithProviders(<PreLoginNoticeForm notice={MOCK_PRE_LOGIN_NOTICE} onAccept={onAccept} forceReading />)

    cy.get('@onAccept').should('not.have.been.called')
    cy.getByTestId('confidentiality-form-agreement').find('input').should('be.disabled')
    cy.getByTestId('confidentiality-form-content').scrollTo('bottom')
    cy.getByTestId('confidentiality-form-agreement').find('input').should('not.be.disabled')
    cy.getByTestId('confidentiality-form-agreement').click()
    cy.getByTestId('confidentiality-form-agreement').should('have.attr', 'data-checked')

    cy.getByTestId('confidentiality-form-submit').should('not.be.disabled')

    cy.getByTestId('confidentiality-form-submit').click()
    cy.get('@onAccept').should('have.been.called')
  })

  it('should bypass consent if not defined', () => {
    const onAccept = cy.stub().as('onAccept')
    cy.mountWithProviders(
      <PreLoginNoticeForm notice={{ ...MOCK_PRE_LOGIN_NOTICE, consent: undefined }} onAccept={onAccept} />
    )

    cy.get('@onAccept').should('not.have.been.called')
    cy.getByTestId('confidentiality-form-agreement').should('not.exist')

    cy.getByTestId('confidentiality-form-submit').should('not.be.disabled')

    cy.getByTestId('confidentiality-form-submit').click()
    cy.get('@onAccept').should('have.been.called')
  })

  it('should be accessible', () => {
    cy.injectAxe()
    cy.mountWithProviders(<PreLoginNoticeForm notice={MOCK_PRE_LOGIN_NOTICE} onAccept={cy.stub} />)
    cy.checkAccessibility()
  })
})
