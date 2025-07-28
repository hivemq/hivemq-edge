import { Page } from '../Page.ts'
import { EDGE_MENU_LINKS } from 'cypress/utils/constants.utils.ts'

export class BridgePage extends Page {
  get navLink() {
    return cy.get('nav [role="list"]').eq(0).find('li').eq(EDGE_MENU_LINKS.BRIDGES)
  }

  get addNewBridge() {
    return cy.getByTestId('page-container-cta').find('button')
  }

  table = {
    get container() {
      return cy.get('table[aria-label="List of bridges"]')
    },

    get status() {
      return cy.get('table[aria-label="List of bridges"] tbody tr td[colspan="6"] div[role="alert"]')
    },

    get rows() {
      return cy.get('table[aria-label="List of bridges"] tbody tr')
    },

    row(index: number) {
      return this.rows.eq(index)
    },

    cell(
      row: number,
      column: number | 'id' | 'localSubscriptions' | 'remoteSubscriptions' | 'status' | 'lastStarted' | 'actions'
    ) {
      const map = ['id', 'localSubscriptions', 'remoteSubscriptions', 'status', 'lastStarted', 'actions']
      if (typeof column === 'string') return this.rows.eq(row).get('td').eq(map.indexOf(column))
      else return this.rows.eq(row).get('td').eq(column)
    },

    action(row: number, action: 'stop' | 'start' | 'restart' | 'edit' | 'delete') {
      const map = {
        stop: 'device-action-stop',
        start: 'device-action-start',
        restart: 'device-action-restart',
        edit: 'bridge-action-edit',
        delete: 'bridge-action-delete',
      }
      this.cell(row, 5).within(() => {
        cy.get('button').click()
      })
      return cy.get(`[role="menu"] button[data-testid=${map[action]}]`)
    },
  }

  modal = {
    get dialog() {
      return cy.get('[role="alertdialog"]')
    },

    get title() {
      return this.dialog.find('header')
    },

    get prompt() {
      return this.dialog.find('p[data-testid="confirmation-message"]')
    },

    get confirm() {
      return this.dialog.find('button[data-testid="confirmation-submit"]')
    },
  }

  config = {
    get panel() {
      return cy.get('[role="dialog"][aria-label="Edit bridge configuration"]')
    },

    get title() {
      return cy.get('[role="dialog"] header')
    },

    get submitButton() {
      return cy.get('[role="dialog"] footer  button[type="submit"]')
    },

    get errorSummary() {
      return cy.get('[role="dialog"] form div[role="alert"] ul li')
    },

    errorSummaryFocus(index: number) {
      return cy
        .get('[role="dialog"] form div[role="alert"] ul li')
        .eq(index)
        .within(() => {
          return cy.getByAriaLabel('Jump to error')
        })
    },

    get formTabs() {
      return cy.get('[role="dialog"] form [role="tablist"]')
    },

    formTab(index: number) {
      this.formTabs.within(() => {
        cy.get('button').eq(index).as('nthTab')
      })
      return cy.get('@nthTab')
    },

    get formTabPanel() {
      return cy.get('[role="dialog"] form [role="tabpanel"]')
    },

    // This is experimental and should only be used within the form
    get formField() {
      return cy.get('div > div.field > [role="group"]')
    },
  }
}

export const bridgePage = new BridgePage()
