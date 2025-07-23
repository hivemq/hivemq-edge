import { Page } from '../Page.ts'
import { EDGE_MENU_LINKS } from 'cypress/utils/constants.utils.ts'

export class DesignerPage extends Page {
  leftArrows = '{leftArrow}'.repeat(15)

  get navLink() {
    return cy.get('nav [role="list"]').eq(0).find('li').eq(EDGE_MENU_LINKS.DATAHUB)
  }

  get canvas() {
    return cy.get('[role="region"][data-testid="rf__wrapper"]')
  }

  designer = {
    mode(type: string) {
      return cy.get(`[role="button"][data-testid^="rf__node-${type}_"]`)
    },

    handle(node: string, type?: string) {
      if (!type) {
        return cy.get(`[data-nodeid^="${node}_"][data-handleid]`)
      }
      return cy.get(`[data-nodeid^="${node}_"][data-handleid="${type}"]`)
    },

    // TODO[NVL] This is not clean, refactor it
    connectNodes(source: string, aourceHandle: string, target: string, targetHandle: string) {
      this.handle(source, aourceHandle).drag(`[data-nodeid^="${target}_"][data-handleid="${targetHandle}"]`)
      cy.get(`[data-nodeid^="${target}_"][data-handleid="${targetHandle}"]`).click()
    },

    createOnDrop(source: string, aourceHandle: string) {
      this.handle(source, aourceHandle).drag('[role="region"][data-testid="rf__wrapper"]', {
        target: { position: 'right' },
      })
    },
  }

  toolbox = {
    get trigger() {
      return cy.getByTestId('toolbox-trigger')
    },

    get topicFilter() {
      return cy.getByTestId('toolbox-container').find('button[aria-label="Topic Filter"]')
    },

    get dataPolicy() {
      return cy.getByTestId('toolbox-container').find('button[aria-label="Data Policy"]')
    },

    get validator() {
      return cy.getByAriaLabel('Policy Validator')
    },

    get schema() {
      return cy.getByAriaLabel('Schema')
    },
  }

  controls = {
    get fit() {
      return cy.getByAriaLabel('Fit to the canvas')
    },

    get zoomIn() {
      return cy.getByAriaLabel('Zoom in')
    },
  }
}

export const datahubDesignerPage = new DesignerPage()
