import { EDGE_MENU_LINKS } from 'cypress/utils/constants.utils.ts'
import { ShellPage } from '../ShellPage.ts'

export class WorkspacePage extends ShellPage {
  get navLink() {
    cy.get('nav [role="list"]')
      .eq(0)
      .within(() => {
        cy.get('li').eq(EDGE_MENU_LINKS.WORKSPACE).as('link')
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

    get zoomIn() {
      return cy.getByAriaLabel('Zoom in')
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

  bridgeNodes() {
    return cy.get(`[role="button"][data-id^="bridge@"]`)
  }

  bridgeNode(id: string) {
    return cy.get(`[role="button"][data-id="bridge@${id}"]`)
  }

  /**
   * Device are identified by the unique id of their adapter
   * @param id The unique id of the device
   */
  deviceNode(id: string) {
    return cy.get(`[role="button"][data-id="device@adapter@${id}"]`)
  }

  combinerNode(id: string) {
    return cy.get(`[role="button"][data-id="${id}"]`)
  }

  act = {
    /**
     * @todo This "double-back" sequence is necessary to effectively select multiple nodes. Need to check for better alternative
     * @param nodes
     */
    selectReactFlowNodes(nodes: string[]) {
      const [first, ...rest] = nodes
      workspacePage.adapterNode(first).type('{meta}', { release: false })
      workspacePage.adapterNode(first).click()
      rest.forEach((node) => {
        workspacePage.adapterNode(node).click()
      })
      workspacePage.adapterNode(first).type('{meta}')
      // workspacePage.adapterNode('first').type('{leftArrow}{leftArrow}')
    },
  }
}

export const workspacePage = new WorkspacePage()
