import { EDGE_MENU_LINKS } from 'cypress/utils/constants.utils.ts'
import { ShellPage } from '../ShellPage.ts'
// eslint-disable-next-line @typescript-eslint/no-unused-vars
import { DuplicateCombinerModal, CombinerMappingsList } from '@/modules/Workspace/components/modals'

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

  canvasToolbar = {
    get expandButton() {
      return cy.getByTestId('toolbox-search-expand')
    },

    get collapseButton() {
      return cy.getByTestId('toolbox-search-collapse')
    },
  }

  layoutControls = {
    get panel() {
      return cy.getByTestId('layout-controls-panel')
    },

    get algorithmSelector() {
      return cy.getByTestId('workspace-layout-selector')
    },

    get applyButton() {
      return cy.getByTestId('workspace-apply-layout')
    },

    get presetsButton() {
      return cy.get('button[aria-label*="preset"]').first()
    },

    get optionsButton() {
      return cy.getByTestId('workspace-layout-options')
    },

    presetsMenu: {
      get saveOption() {
        return cy.getByTestId('workspace-layout-preset-save')
      },

      presetItem(name: string) {
        return cy.get('[role="menu"]').within(() => {
          cy.contains('[role="menuitem"]', name)
        })
      },

      get emptyMessage() {
        return cy.get('[role="menu"]').contains('No saved presets')
      },
    },

    optionsDrawer: {
      get drawer() {
        return cy.get('[role="dialog"][id="chakra-modal-layout-options-drawer"]')
      },

      get title() {
        return cy.get('[role="dialog"] header')
      },

      get form() {
        return cy.get('form#layout-options-form')
      },

      get cancelButton() {
        return cy.getByTestId('workspace-options-cancel')
      },

      get applyButton() {
        return cy.getByTestId('workspace-options-apply')
      },
    },

    savePresetModal: {
      get modal() {
        return cy.get('[role="dialog"]')
      },

      get nameInput() {
        return cy.getByTestId('workspace-preset-input')
      },

      get cancelButton() {
        return cy.getByTestId('workspace-preset-cancel')
      },

      get saveButton() {
        return cy.getByTestId('workspace-preset-save')
      },
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
    return cy.get('[data-testid="rf__wrapper"] [role="group"][data-id="edge"]')
  }

  adapterNode(id: string) {
    return cy.get(`[role="group"][data-id="adapter@${id}"]`)
  }

  bridgeNodes() {
    return cy.get(`[role="group"][data-id^="bridge@"]`)
  }

  bridgeNode(id: string) {
    return cy.get(`[role="group"][data-id="bridge@${id}"]`)
  }

  /**
   * Device are identified by the unique id of their adapter
   * @param id The unique id of the device
   */
  deviceNode(id: string) {
    return cy.get(`[role="group"][data-id="device@adapter@${id}"]`)
  }

  combinerNode(id: string) {
    return cy.get(`[role="group"][data-id="${id}"]`)
  }

  combinerNodeContent(id: string) {
    return {
      get title() {
        return cy.get(`[role="group"][data-id="${id}"] [data-testid="combiner-description"]`)
      },

      get topic() {
        return cy.get(`[role="group"][data-id="${id}"] [data-testid="topic-wrapper"]`)
      },
    }
  }

  /**
   * Page Object for the Duplicate Combiner Detection Modal
   * @see DuplicateCombinerModal
   */
  duplicateCombinerModal = {
    /**
     * Main modal container
     * @see DuplicateCombinerModal
     */
    get modal() {
      return cy.getByTestId('duplicate-combiner-modal')
    },

    /**
     * Modal title text
     * @see DuplicateCombinerModal
     */
    get title() {
      return cy.getByTestId('modal-title')
    },

    /**
     * Existing combiner name display
     * @see DuplicateCombinerModal
     */
    get combinerName() {
      return cy.getByTestId('modal-combiner-name')
    },

    /**
     * Modal description text
     * @see DuplicateCombinerModal
     */
    get description() {
      return cy.getByTestId('modal-description')
    },

    /**
     * Label for the mappings section
     * @see DuplicateCombinerModal
     */
    get mappingsLabel() {
      return cy.getByTestId('modal-mappings-label')
    },

    /**
     * Container for the list of combiner mappings
     * @see CombinerMappingsList
     * @note Use `cy.get('[data-testid^="mapping-item-"]')` to select individual mappings with dynamic UUIDs
     */
    get mappingsList() {
      return cy.getByTestId('mappings-list')
    },

    /**
     * Empty state message when no mappings exist
     * @see CombinerMappingsList
     */
    get mappingsListEmpty() {
      return cy.getByTestId('mappings-list-empty')
    },

    /**
     * Badge showing count of mappings
     * @see CombinerMappingsList
     */
    get mappingsCountBadge() {
      return cy.getByTestId('mappings-count-badge')
    },

    /**
     * Modal prompt text asking user what to do
     * @see CombinerMappingsList
     */
    get prompt() {
      return cy.getByTestId('modal-prompt')
    },

    /**
     * Close button (X) in modal header
     * @see DuplicateCombinerModal
     */
    get closeButton() {
      return cy.getByTestId('modal-close-button')
    },

    /**
     * Action buttons in modal footer
     * @see DuplicateCombinerModal
     */
    buttons: {
      /**
       * Cancel button - closes modal without action
       */
      get cancel() {
        return cy.getByTestId('modal-button-cancel')
      },

      /**
       * Create New Anyway button - creates duplicate combiner
       */
      get createNew() {
        return cy.getByTestId('modal-button-create-new')
      },

      /**
       * Use Existing button - navigates to existing combiner (primary action)
       */
      get useExisting() {
        return cy.getByTestId('modal-button-use-existing')
      },
    },
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
