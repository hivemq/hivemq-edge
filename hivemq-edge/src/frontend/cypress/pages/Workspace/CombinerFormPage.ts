import { RJSFomField } from '../RJSF/RJSFomField.ts'

export class CombinerFormPage extends RJSFomField {
  get form() {
    return cy.get('[role="dialog"][aria-label="Manage Data combining mappings"]')
  }
}

export const combinerForm = new CombinerFormPage()
