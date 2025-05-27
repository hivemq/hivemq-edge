// MUST BE USED WITHIN a field element in a RJSF form
export class RJSFomField {
  get form() {
    return cy.get('[role="dialog"]')
  }

  header = {
    get title() {
      return cy.get('')
    },
  }

  /**
   * Retrieve all fields in the form
   */
  fields() {
    return cy.get(`[role="group"][data-testid]`)
  }

  field(id: string | string[]) {
    const safeId = 'root_' + (typeof id === 'string' ? id : id.join('_'))
    const rootSelector = `[role="group"][data-testid="${safeId}"]`

    return {
      get root() {
        return cy.get(rootSelector)
      },
      get title() {
        return cy.get(`${rootSelector} div#${safeId}__title h2`)
      },

      get description() {
        return cy.get(`${rootSelector} sup#${safeId}__description`)
      },

      get label() {
        return cy.get(`[role="group"][data-testid="${safeId}"] label#${safeId}-label:not(:has([role="presentation"]))`)
      },

      get requiredLabel() {
        return cy.get(`${rootSelector} label#${safeId}-label:has([role="presentation"])`)
      },

      get input() {
        return cy.get(`${rootSelector} input#${safeId}`)
      },

      get inputUpDown() {
        // TODO checking for [value] on div might not always work; spinbutton is more definitive
        return cy.get(`${rootSelector} label + div[value] > input[role="spinbutton"]`)
      },

      get checkBox() {
        return cy.get(`${rootSelector} input#${safeId} + span`)
      },

      /**
       * Breaks the pattern from the other widgets. Fix in RJSF
       */
      get select() {
        return cy.get(`${rootSelector} label#${safeId}-label + div`)
      },

      /**
       * Breaks the pattern from the other widgets. Fix in RJSF
       */
      get checkBoxLabel() {
        return cy.get(`${rootSelector} label:has(input#${safeId})`)
      },

      get helperText() {
        return cy.get(`${rootSelector} #${safeId}-helptext`)
      },

      get errors() {
        return cy.get(`${rootSelector} #${safeId}__error`)
      },

      table: {
        get noDataMessage() {
          return cy.get(`${rootSelector} tbody tr td [role="alert"][data-status="info"]`)
        },

        get rows() {
          return cy.get(`${rootSelector} tbody tr`)
        },

        row(index: number) {
          return {
            get edit() {
              cy.get(`${rootSelector} tbody tr`)
                .eq(index)
                .within(() => {
                  cy.getByTestId('combiner-mapping-list-edit').as('edit')
                })
              return cy.get('@edit')
            },
          }
        },

        get addItem() {
          // Should be made more generic; remove reference to specific deployment (combiner)
          return cy.get(`${rootSelector} tfoot button[data-testid="combiner-mapping-list-add"]`)
        },
      },
    }
  }

  /**
   * @deprecated Should use the field(id) method
   */
  get title() {
    return cy.get('h2')
  }

  /**
   * @deprecated Should use the field(id) method
   */
  get subtitle() {
    return cy.get('div:has(h2) >  p > sup')
  }

  /**
   * @deprecated Should use the field(id) method
   */
  get label() {
    return cy.get('label:not(:has([role="presentation"]))')
  }

  /**
   * @deprecated Should use the field(id) method
   */
  get requiredLabel() {
    return cy.get('label:has([role="presentation"])')
  }

  /**
   * @deprecated Should use the field(id) method
   */
  get input() {
    return cy.get('label + input')
  }

  /**
   * @deprecated Should use the field(id) method
   */
  get inputUpDown() {
    // TODO checking for [value] on div might not always work; spinbutton is more definitive
    return cy.get('label + div[value] > input[role="spinbutton"]')
  }

  /**
   * @deprecated Should use the field(id) method
   */
  get checkBox() {
    return cy.get('label > input + span')
  }

  /**
   * @deprecated Should use the field(id) method
   */
  get select() {
    // return cy.get('div:has(div+div:has(input[role="combobox"]))')
    return cy.get('label+div ')
  }

  /**
   * @deprecated Should use the field(id) method
   */
  get helperText() {
    // return cy.get('div:has(label) + div sup')
    return cy.get('div sup')
  }

  /**
   * @deprecated Should use the field(id) method
   */
  get errors() {
    return cy.get('ul li div')
  }
}

export const rjsf = new RJSFomField()
