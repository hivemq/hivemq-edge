/// <reference types="cypress" />

import { MOCK_OPC_UA_TLS_SCHEMA, MOCK_OPC_UA_TLS_UI_SCHEMA } from '@/__test-utils__/adapters/opc-ua-tls'

import ChakraRJSForm from './ChakraRJSForm.tsx'

/**
 * The OPC UA certificate-validation surface, driven through the form the product actually renders.
 *
 * Two doors configure it and they are mutually exclusive: `tlsChecks` is a named preset, `tlsChecksFull`
 * is the six raw axes. The backend keeps them exclusive by carrying neither a schema `default`, because
 * RJSF materializes schema defaults into the data it submits — a default on either door would set both
 * doors at once on an adapter that had set neither, and the adapter would refuse to start. That claim is
 * about RJSF's behaviour, so it is only worth anything asserted against RJSF.
 *
 * The rendering test is the P1 that shipped: `tls.ui:order` listed four of the six TLS properties and
 * carried no `"*"` wildcard, and RJSF refuses to render a whole object whose `ui:order` neither names a
 * property nor admits it through a wildcard. The entire TLS section vanished from the adapter form.
 */
describe('ChakraRJSForm', () => {
  beforeEach(() => {
    cy.viewport(800, 1200)
  })

  const submittedTls = (assertion: (tls: Record<string, unknown>) => void) =>
    cy.get('@onSubmit').then((stub) => {
      const onSubmit = stub as unknown as Cypress.Agent<sinon.SinonStub>
      const { formData } = onSubmit.args[0][0]
      assertion(formData.tls)
    })

  const mountTlsForm = (formData: Record<string, unknown>) =>
    cy.mountWithProviders(
      <ChakraRJSForm
        id="opcua-tls"
        schema={MOCK_OPC_UA_TLS_SCHEMA}
        uiSchema={MOCK_OPC_UA_TLS_UI_SCHEMA}
        formData={formData}
        onSubmit={cy.stub().as('onSubmit')}
      />
    )

  describe('OPC UA certificate validation', () => {
    it('should render every TLS property, not just the ordered ones', () => {
      // ui:order omitting a property is not a cosmetic problem: RJSF throws and the section is gone.
      mountTlsForm({ tls: { enabled: true } })

      cy.get('#root_tls_enabled').should('exist')
      cy.get('#root_tls_tlsChecks').should('exist')
      cy.get('#root_tls_tlsChecksFull_trustMode').should('exist')
      cy.get('#root_tls_tlsChecksFull_keyUsage').should('exist')
      cy.get('#root_tls_allowList_path').should('exist')
      cy.get('#root_tls_keystore_path').should('exist')
      cy.get('#root_tls_truststore_path').should('exist')
    })

    it('should submit neither door when neither was configured', () => {
      // The adapter that has never touched certificate validation. Submitting either key would write a
      // setting the operator did not choose - and writing both is a configuration the adapter refuses.
      mountTlsForm({ tls: { enabled: true } })

      cy.get('@onSubmit').should('not.have.been.called')
      cy.get('button[type="submit"]').click()
      cy.get('@onSubmit').should('have.been.calledOnce')

      submittedTls((tls) => {
        expect(tls).to.not.have.property('tlsChecks')
        expect(tls).to.not.have.property('tlsChecksFull')
      })
    })

    it('should submit the preset alone and keep the allow-list it needs', () => {
      // SELF_SIGNED is the preset whose trust mode is ALLOW_LIST, so the allow-list path has to survive
      // the round trip - losing it turns a working adapter into one that cannot establish trust at all.
      mountTlsForm({
        tls: { enabled: true, tlsChecks: 'SELF_SIGNED', allowList: { path: '/etc/hivemq/allow-list.txt' } },
      })

      cy.get('button[type="submit"]').click()

      submittedTls((tls) => {
        expect(tls).to.have.property('tlsChecks', 'SELF_SIGNED')
        expect(tls).to.not.have.property('tlsChecksFull')
        expect(tls.allowList).to.deep.equal({ path: '/etc/hivemq/allow-list.txt' })
      })
    })

    it('should submit the axes alone and leave the omitted ones omitted', () => {
      // An omitted axis is not the same as an axis set to its default: it resolves to the strictest
      // value at read time. Filling the four unset axes in here would freeze today's defaults into the
      // operator's configuration file on the next save.
      mountTlsForm({
        tls: { enabled: true, tlsChecksFull: { trustMode: 'ALLOW_LIST', hostname: 'NONE' } },
      })

      cy.get('button[type="submit"]').click()

      submittedTls((tls) => {
        expect(tls).to.not.have.property('tlsChecks')
        expect(tls.tlsChecksFull).to.deep.equal({ trustMode: 'ALLOW_LIST', hostname: 'NONE' })
      })
    })

    it('should be accessible', () => {
      cy.injectAxe()
      mountTlsForm({ tls: { enabled: true } })
      cy.checkAccessibility()
    })
  })
})
