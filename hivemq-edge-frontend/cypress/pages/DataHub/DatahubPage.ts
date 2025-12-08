import { Page } from '../Page.ts'
import { EDGE_MENU_LINKS } from 'cypress/utils/constants.utils.ts'

/**
 * DatahubEntityTable is a base class for tables in the DataHub page.
 * TODO[NVL] It could be reused for all other tables in Edge
 */
class DatahubEntityTable {
  private label: string

  constructor(label: string) {
    this.label = label
  }

  get container() {
    return cy.get(`table[aria-label="${this.label}"]`)
  }

  get status() {
    return cy.get(`table[aria-label="${this.label}"] tbody tr td[colspan="5"] > div[role="alert"]`)
  }

  get rows() {
    return cy.get(`table[aria-label="${this.label}"] tbody tr`)
  }

  row(index: number) {
    return this.rows.eq(index)
  }

  // eslint-disable-next-line @typescript-eslint/no-unused-vars
  cell(row: number, column: unknown) {
    throw new Error('You have to implement the method cell!')
  }

  // eslint-disable-next-line @typescript-eslint/no-unused-vars
  action(row: number, action: unknown) {
    throw new Error('You have to implement the method action!')
  }
}

class PolicyTable extends DatahubEntityTable {
  cell(row: number, column: number | 'id' | 'type' | 'matching' | 'created' | 'actions') {
    const map = ['id', 'type', 'matching', 'created', 'actions']
    if (typeof column === 'string') return this.rows.eq(row).find('td').eq(map.indexOf(column))
    else return this.rows.eq(row).find('td').eq(column)
  }

  action(row: number, action: 'draft' | 'edit' | 'download' | 'delete') {
    const map = {
      draft: 'list-action-view',
      edit: 'list-action-view',
      download: 'list-action-download',
      delete: 'list-action-delete',
    }
    return this.cell(row, 4).find(`[role="group"] button[data-testid=${map[action]}]`)
  }
}

class SchemaTable extends DatahubEntityTable {
  get createButton() {
    return cy.getByTestId('schema-create-new-button')
  }

  cell(row: number, column: number | 'name' | 'type' | 'version' | 'created' | 'actions') {
    const map = ['name', 'type', 'version', 'created', 'actions']
    if (typeof column === 'string') return this.rows.eq(row).find('td').eq(map.indexOf(column))
    else return this.rows.eq(row).find('td').eq(column)
  }

  action(row: number, action: 'edit' | 'download' | 'delete') {
    const map = {
      edit: 'list-action-edit',
      download: 'list-action-download',
      delete: 'list-action-delete',
    }
    // Actions are available directly, no action menu button needed
    return this.cell(row, 'actions').find(`button[data-testid="${map[action]}"]`)
  }
}

class ScriptTable extends DatahubEntityTable {
  get createButton() {
    return cy.getByTestId('script-create-new-button')
  }

  cell(row: number, column: number | 'name' | 'type' | 'version' | 'description' | 'created' | 'actions') {
    const map = ['name', 'type', 'version', 'description', 'created', 'actions']
    if (typeof column === 'string') return this.rows.eq(row).find('td').eq(map.indexOf(column))
    else return this.rows.eq(row).find('td').eq(column)
  }

  action(row: number, action: 'edit' | 'download' | 'delete') {
    const map = {
      edit: 'list-action-edit',
      download: 'list-action-download',
      delete: 'list-action-delete',
    }
    // Actions are available directly, no action menu button needed
    return this.cell(row, 'actions').find(`button[data-testid="${map[action]}"]`)
  }
}

export class DatahubPage extends Page {
  get navLink() {
    return cy.get('nav [role="list"]').eq(0).find('li').eq(EDGE_MENU_LINKS.DATAHUB)
  }

  get addNewPolicy() {
    return cy.getByTestId('page-container-cta').find('button')
  }

  get policiesTab() {
    return cy.get('[role="tablist"] button').contains('Policies')
  }

  get schemasTab() {
    return cy.get('[role="tablist"] button').contains('Schemas')
  }

  get scriptsTab() {
    return cy.get('[role="tablist"] button').contains('Scripts')
  }

  schemasTable = new SchemaTable('List of schemas')
  scriptsTable = new ScriptTable('List of scripts')

  schemaEditor = {
    get drawer() {
      return cy.getByTestId('schema-editor-drawer')
    },
    get title() {
      return this.drawer.find('header')
    },
    get nameField() {
      return cy.get('#root_name')
    },
    get typeField() {
      return cy.get('#root_type')
    },
    selectType(type: 'JSON' | 'PROTOBUF') {
      this.typeField.click()
      this.drawer.within(() => {
        cy.contains('[role="option"]', type).click()
      })
    },
    get versionDisplay() {
      // Version is readonly - check the display text, not input value
      return cy.get('label#root_version-label + div')
    },
    get messageTypeField() {
      // For Protobuf schemas - React Select component for message type
      return cy.get('label#root_messageType-label + div')
    },
    selectMessageType(messageType: string) {
      this.messageTypeField.should('exist')
      this.messageTypeField.scrollIntoView()
      this.messageTypeField.click()
      this.drawer.within(() => {
        cy.contains('[role="option"]', messageType).click()
      })
    },
    get definitionEditorContainer() {
      // Returns the container - use monaco.setValue() to set content
      return cy.get('#root_schemaSource')
    },
    get saveButton() {
      return cy.getByTestId('save-schema-button')
    },
    get cancelButton() {
      return cy.getByTestId('cancel-schema-button')
    },
  }

  scriptEditor = {
    get drawer() {
      return cy.getByTestId('script-editor-drawer')
    },
    get title() {
      return this.drawer.find('header')
    },
    get nameField() {
      return cy.get('#root_name')
    },
    get descriptionField() {
      return cy.get('#root_description')
    },
    get versionDisplay() {
      // Version is readonly - check the display text, not input value
      return cy.get('label#root_version-label + div')
    },
    get sourceEditorContainer() {
      // Returns the container - use monaco.setValue() to set content
      return cy.get('#root_sourceCode')
    },
    get saveButton() {
      return cy.getByTestId('save-script-button')
    },
    get cancelButton() {
      return cy.getByTestId('cancel-script-button')
    },
  }

  policiesTable = new PolicyTable('List of policies')

  confirmDraft = {
    get dialog() {
      return cy.get('[role="alertdialog"]')
    },

    get title() {
      return this.dialog.find('header')
    },

    get prompt() {
      return this.dialog.find('header + div')
    },

    get closeButton() {
      return this.dialog.find('button[data-testid="confirmation-cancel"]')
    },

    get openDraft() {
      return this.dialog.find('button[data-testid="confirmation-navigate-draft"]')
    },

    get newDraft() {
      return this.dialog.find('button[data-testid="confirmation-submit"]')
    },
  }
}

export const datahubPage = new DatahubPage()
