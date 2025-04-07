import { Page } from '../Page.ts'

export class WorkspacePage extends Page {
  get navLink() {
    cy.get('nav [role="list"]')
      .eq(0)
      .within(() => {
        cy.get('li').eq(1).as('link')
      })
    return cy.get('@link')
  }

  get canvas() {
    return cy.getByTestId('rf__wrapper')
  }

  toolbox = {
    get fit() {
      return cy.getByAriaLabel('Fit to the canvas')
    },
  }

  get nodeToolbar() {
    return cy.get('[data-testid="rf__wrapper"] [role="toolbar"][aria-label="Node toolbar"]')
  }

  toolbar = {
    get title() {
      return cy.getByTestId('toolbar-title')
    },

    get topicFilter() {
      return cy.getByAriaLabel('Manage topic filters')
    },

    get combine() {
      return cy.getByTestId('node-group-toolbar-combiner')
    },

    get group() {
      return cy.getByTestId('node-group-toolbar-group')
    },

    get overview() {
      return cy.getByTestId('node-group-toolbar-panel')
    },
  }

  get edgeNode() {
    return cy.get('[data-testid="rf__wrapper"] [role="button"][data-id="edge"]')
  }

  adapterNode(id: string) {
    return cy.get(`[role="button"][data-id="adapter@${id}"]`)
  }

  /**
   * Device are identified by the unique id of their adapter
   * @param id The unique id of the device
   */
  deviceNode(id: string) {
    return cy.get(`[role="button"][data-id="device@adapter@${id}"]`)
  }
}

export const workspacePage = new WorkspacePage()
