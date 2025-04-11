import { RJSFomField } from '../RJSF/RJSFomField.ts'

export class CombinerFormPage extends RJSFomField {
  get form() {
    return cy.get('[role="dialog"][aria-label="Manage Data combining mappings"]')
  }

  inferSchema = {
    get modal() {
      return cy.get('[role="dialog"]#chakra-modal-destination-schema')
    },

    get submit() {
      cy.get('[role="dialog"]#chakra-modal-destination-schema footer').within(() => {
        cy.getByTestId('schema-infer-generate').as('submit')
      })
      return cy.get('@submit')
    },
  }

  mappingEditor = {
    get form() {
      return cy.get('[role="dialog"][aria-label="Data combining mapping"]')
    },

    sources: {
      get selector() {
        combinerForm.mappingEditor.form.within(() => {
          cy.get('#combiner-entity-select').as('selectSources')
        })
        return cy.get('@selectSources')
      },
      get options() {
        combinerForm.mappingEditor.form.within(() => {
          cy.get('#react-select-entity-listbox > [role="listbox"] > [role="option"]').as('options')
        })
        return cy.get('@options')
      },
      get schema() {
        combinerForm.mappingEditor.form.within(() => {
          cy.getByTestId('combining-editor-sources-schemas').within(() => {
            cy.get('label+div ul li').as('properties')
          })
        })
        return cy.get('@properties')
      },
    },

    destination: {
      get selector() {
        combinerForm.mappingEditor.form.within(() => {
          cy.get('#destination').as('selectDestination')
        })
        return cy.get('@selectDestination')
      },

      get inferSchema() {
        combinerForm.mappingEditor.form.within(() => {
          cy.getByTestId('combiner-destination-infer').as('destinationInfer')
        })
        return cy.get('@destinationInfer')
      },
    },

    primary: {
      get selector() {
        combinerForm.mappingEditor.form.within(() => {
          cy.get('#mappings-primary').as('selectPrimary')
        })
        return cy.get('@selectPrimary')
      },
    },
  }
}

export const combinerForm = new CombinerFormPage()
