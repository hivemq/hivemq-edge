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

export class DatahubPage extends Page {
  get navLink() {
    return cy.get('nav [role="list"]').eq(0).find('li').eq(EDGE_MENU_LINKS.DATAHUB)
  }

  get addNewPolicy() {
    return cy.getByTestId('page-container-cta').find('button')
  }

  policiesTable = new PolicyTable('List of policies')

  // policiesTable = {
  //   get container() {
  //     return cy.get('table[aria-label="List of policies"]')
  //   },
  //
  //   get status() {
  //     return cy.get('table[aria-label="List of policies"] tbody tr td[colspan="6"] div[role="alert"]')
  //   },
  //
  //   get rows() {
  //     return cy.get('table[aria-label="List of policies"] tbody tr')
  //   },
  //
  //   row(index: number) {
  //     return this.rows.eq(index)
  //   },
  //
  //   cell(
  //     row: number,
  //     column: number | 'id' | 'localSubscriptions' | 'remoteSubscriptions' | 'status' | 'lastStarted' | 'actions'
  //   ) {
  //     const map = ['id', 'localSubscriptions', 'remoteSubscriptions', 'status', 'lastStarted', 'actions']
  //     if (typeof column === 'string') return this.rows.eq(row).get('td').eq(map.indexOf(column))
  //     else return this.rows.eq(row).get('td').eq(column)
  //   },
  //
  //   action(row: number, action: 'stop' | 'start' | 'restart' | 'edit' | 'delete') {
  //     const map = {
  //       stop: 'device-action-stop',
  //       start: 'device-action-start',
  //       restart: 'device-action-restart',
  //       edit: 'bridge-action-edit',
  //       delete: 'bridge-action-delete',
  //     }
  //     this.cell(row, 5).within(() => {
  //       cy.get('button').click()
  //     })
  //     return cy.get(`[role="menu"] button[data-testid=${map[action]}]`)
  //   },
  // }

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
