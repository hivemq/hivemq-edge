import { rjsf, RJSFomField } from '../RJSF/RJSFomField.ts'

export class CombinerFormPage extends RJSFomField {
  get form() {
    return cy.get('[role="dialog"][aria-label="Manage Data combining mappings"]')
  }

  get submit() {
    return cy.get('[role="dialog"][aria-label="Manage Data combining mappings"] footer button[type="submit"]')
  }

  get delete() {
    return cy.get('[role="dialog"][aria-label="Manage Data combining mappings"] footer button[type="button"]')
  }

  table = {
    get destination() {
      rjsf
        .field('mappings')
        .table.rows.eq(0)
        .within(() => {
          cy.get('td').eq(0).as('columnDestination')
        })
      return cy.get('@columnDestination')
    },

    get sources() {
      rjsf
        .field('mappings')
        .table.rows.eq(0)
        .within(() => {
          cy.get('td').eq(1).as('columnSources')
        })
      return cy.get('@columnSources')
    },
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

    get submit() {
      return cy.get('[role="dialog"][aria-label="Data combining mapping"] footer button[type="submit"]')
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

      get schema() {
        combinerForm.mappingEditor.form.within(() => {
          cy.getByTestId('combining-editor-destination-schema').within(() => {
            cy.get('ul#destination-mapping-editor li').as('destSchema')
          })
        })
        return cy.get('@destSchema')
      },

      schemaProperty(index: number) {
        return this.schema.eq(index)
      },
    },

    instruction(index: number) {
      return {
        get mapping() {
          cy.get('@destSchema')
            .eq(index)
            .within(() => {
              cy.getByTestId('mapping-instruction-dropzone').as('dropzone')
            })
          return cy.get('@dropzone')
        },
        get status() {
          cy.get('@destSchema')
            .eq(index)
            .within(() => {
              cy.get('[role="alert"]').as('status')
            })
          return cy.get('@status')
        },
      }
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

  confirmDelete = {
    get modal() {
      return cy.get('[role="alertdialog"]')
    },

    get submit() {
      return cy.get('[role="alertdialog"] footer button[data-testid="confirmation-submit"]')
    },
  }
}

export const combinerForm = new CombinerFormPage()
