import { RJSFomField } from '../RJSF/RJSFomField.ts'

export class BridgeFormPage extends RJSFomField {
  get form() {
    return cy.get('[role="dialog"][aria-label="Bridge Overview"]')
  }

  get modifyBridge() {
    return cy.get('[role="dialog"][aria-label="Bridge Overview"] footer button[data-testid="protocol-create-adapter"] ')
  }

  get startBridge() {
    return cy.get('[role="dialog"][aria-label="Bridge Overview"] footer ').within(() => {
      return cy.getByTestId('device-action-start')
    })
  }

  get stopBridge() {
    return cy.get('[role="dialog"][aria-label="Bridge Overview"] footer').within(() => {
      return cy.getByTestId('device-action-stop')
    })
  }

  get restartBridge() {
    return cy.get('[role="dialog"][aria-label="Bridge Overview"] footer ').within(() => {
      return cy.getByTestId('device-action-restart')
    })
  }
}

export const workspaceBridgePanel = new BridgeFormPage()
