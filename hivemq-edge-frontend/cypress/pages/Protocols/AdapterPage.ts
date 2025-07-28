import { Page } from '../Page.ts'
import { EDGE_MENU_LINKS } from 'cypress/utils/constants.utils.ts'

export class AdapterPage extends Page {
  get navLink() {
    cy.get('nav [role="list"]')
      .eq(0)
      .within(() => {
        cy.get('li').eq(EDGE_MENU_LINKS.ADAPTERS).as('link')
      })
    return cy.get('@link')
  }

  get addNewAdapter() {
    cy.getByTestId('page-container-cta').within(() => {
      cy.get('button').as('button')
    })
    return cy.get('@button')
  }

  protocols = {
    get list() {
      return cy.get('[role="list"][aria-label="List of protocol adapters"]')
    },

    createNewConnection(index: number) {
      cy.get('[role="list"][aria-label="List of protocol adapters"] [role="listitem"]')
        .eq(index)
        .within(() => {
          cy.getByTestId('protocol-create-adapter').as('button')
        })
      return cy.get('@button')
    },
  }

  config = {
    get panel() {
      return cy.get('[role="dialog"][aria-label="Protocol Adapter Editor"]')
    },

    get title() {
      return cy.get('[role="dialog"] header > p')
    },

    get subTitle() {
      return cy.get('[role="dialog"] header > p + div')
    },

    get submitButton() {
      return cy.get('[role="dialog"] footer  button[type="submit"]')
    },

    get errorSummary() {
      return cy.get('[role="dialog"] form div[role="alert"] ul li')
    },

    errorSummaryFocus(index: number) {
      cy.get('[role="dialog"] form div[role="alert"] ul li')
        .eq(index)
        .within(() => {
          cy.getByAriaLabel('Jump to error').as('button')
        })
      return cy.get('@button')
    },

    get formTabs() {
      return cy.get('[role="dialog"] form [role="tablist"]')
    },

    formTab(index: number) {
      this.formTabs.within(() => {
        cy.get('button').eq(index).as('tab')
      })
      return cy.get('@tab')
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

export const adapterPage = new AdapterPage()
