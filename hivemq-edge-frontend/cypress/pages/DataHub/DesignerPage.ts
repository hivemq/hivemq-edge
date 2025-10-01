import { Page } from '../Page.ts'
import { EDGE_MENU_LINKS } from 'cypress/utils/constants.utils.ts'

export class DesignerPage extends Page {
  leftArrows = '{leftArrow}'.repeat(15)

  get navLink() {
    return cy.get('nav [role="list"]').eq(0).find('li').eq(EDGE_MENU_LINKS.DATAHUB)
  }

  get canvas() {
    return cy.get('[role="application"][data-testid="rf__wrapper"]')
  }

  designer = {
    mode(type: string) {
      return cy.get(`[role="group"][data-testid^="rf__node-${type}_"]`)
    },

    modes() {
      return cy.get(`[role="group"][data-testid^="rf__node-"]`)
    },

    edges() {
      // xy-edge__FUNCTION_8be4812a-59a1-4b27-87e3-99268cc4e3bd-OPERATION_77b42291-6889-43d7-8205-81799c4ae7b7function
      return cy.get(`[role="group"][data-id^="xy-edge__"]`)
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
      this.handle(source, aourceHandle).drag('[role="application"][data-testid="rf__wrapper"]', {
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
