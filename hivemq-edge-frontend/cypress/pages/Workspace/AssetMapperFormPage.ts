import { RJSFormField } from '../RJSF/RJSFormField.ts'

export class AssetMapperFormPage extends RJSFormField {
  get formTabs() {
    return cy.get('[role="dialog"] form [role="tablist"]')
  }

  formTab(index: number) {
    return this.formTabs.find('button').eq(index)
  }

  get submit() {
    return cy.get('[role="dialog"][aria-label="Managing Pulse Asset Mappings"] footer button[type="submit"]')
  }

  get assetMappings() {
    return {
      get table() {
        return cy.get('table[aria-label="The list of mappings created for this combiner"]')
      },

      action(row: number, action: 'edit' | 'delete') {
        return cy
          .get('table[aria-label="The list of mappings created for this combiner"] tbody tr')
          .eq(row)
          .find(`[data-testid="combiner-mapping-list-${action}"]`)
      },
    }
  }
}

export const assetMapperForm = new AssetMapperFormPage()
