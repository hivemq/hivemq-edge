import { RJSFomField } from '../RJSF/RJSFomField.ts'

export class ActivationFormPage extends RJSFomField {
  get trigger() {
    return cy.getByTestId('pulse-activation-trigger')
  }

  get form() {
    return cy.get('[role="dialog"][aria-label="Pulse Client Activation"]')
  }

  get status() {
    return this.form.find('[role="alert"][data-status="info"]')
  }

  get submitButton() {
    return this.form.find('footer button[type="submit"]')
  }
}

export const pulseActivationPanel = new ActivationFormPage()
