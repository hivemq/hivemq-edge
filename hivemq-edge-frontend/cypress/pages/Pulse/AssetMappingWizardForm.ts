export class AssetMappingWizardForm {
  get form() {
    return cy.get('section[role="dialog"]#chakra-modal-wizard-mapper')
  }

  header = {
    get title() {
      return cy.get('section[role="dialog"]#chakra-modal-wizard-mapper').find('header')
    },
  }

  get submit() {
    return cy.get('section[role="dialog"]#chakra-modal-wizard-mapper').find('footer button')
  }

  // get selectMapper() {
  //   return cy.getByTestId('wizard-mapper-selector-container')
  // }

  get selectMapper() {
    return {
      get root() {
        return cy.getByTestId('wizard-mapper-selector-container')
      },

      get label() {
        return cy.getByTestId('wizard-mapper-selector-container').find('label[for="asset-mapper"]')
      },

      get moreInfo() {
        return cy.getByTestId('wizard-mapper-selector-container').find('[data-testid="more-info-trigger"]')
      },

      get select() {
        return cy.getByTestId('wizard-mapper-selector-container').find('#wizard-mapper-selector')
      },

      get value() {
        return cy.getByTestId('wizard-mapper-selector-container').find('#react-select-mapper-value')
      }, //react-select-mapper-placeholder

      get placeholder() {
        return cy.getByTestId('wizard-mapper-selector-container').find('#react-select-mapper-placeholder')
      },

      get helperText() {
        return cy
          .getByTestId('wizard-mapper-selector-container')
          .find('[data-testid="wizard-mapper-selector-instruction"]')
      },
    }
  }

  get selectSources() {
    return {
      get root() {
        return cy.getByTestId('wizard-mapper-entities-container')
      },

      get label() {
        return cy.getByTestId('wizard-mapper-entities-container').find('label[for="mapper-sources"]')
      },

      get moreInfo() {
        return cy.getByTestId('wizard-mapper-entities-container').find('[data-testid="more-info-trigger"]')
      },

      get select() {
        return cy.getByTestId('wizard-mapper-entities-container').find('#wizard-mapper-sources')
      },

      get values() {
        return cy.getByTestId('wizard-mapper-entities-container').find('[data-testid="multi-selected-value"]')
      }, //react-select-mapper-placeholder

      // get placeholder() {
      //   return cy.getByTestId('wizard-mapper-entities-container').find('#react-select-mapper-placeholder')
      // },

      get helperText() {
        return cy.getByTestId('wizard-mapper-entities-container').find('#react-select-sources-helper')
      },
    }
  }
}

export const assetMappingWizard = new AssetMappingWizardForm()
