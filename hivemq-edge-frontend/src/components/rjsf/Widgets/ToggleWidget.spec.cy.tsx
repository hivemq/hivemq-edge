import type { JSONSchema7 } from 'json-schema'
import type { IChangeEvent } from '@rjsf/core'
import type { UiSchema } from '@rjsf/utils'

import { CustomFormTesting } from '@/__test-utils__/rjsf/CustomFormTesting'
import ToggleWidget from '@/components/rjsf/Widgets/ToggleWidget.tsx'
import i18nConfig from '@/config/i18n.config.ts'
import FunctionCreatableSelect from '@datahub/components/forms/FunctionCreatableSelect.tsx'

const mockToggleWidgetUISchema: UiSchema = {
  loopPreventionEnabled: {
    'ui:widget': ToggleWidget,
    'ui:title': i18nConfig.t('bridge.options.loopPrevention.label'),
    'ui:description': i18nConfig.t('bridge.options.loopPrevention.helper'),
  },
}

const mockToggleWidgetSchema: JSONSchema7 = {
  $ref: '#/definitions/LoopPreventionEnabled',
  definitions: {
    LoopPreventionEnabled: {
      properties: {
        loopPreventionEnabled: {
          type: 'boolean',
          description: `Is loop prevention enabled on the connection`,
          default: false,
        },
      },
    },
  },
}

const mockFunctionCreatableSelect = (initialState: boolean, onSubmit?: (data: IChangeEvent) => void) => (
  <CustomFormTesting
    schema={mockToggleWidgetSchema}
    uiSchema={mockToggleWidgetUISchema}
    formData={{
      loopPreventionEnabled: initialState,
    }}
    widgets={{
      'datahub:function-selector': FunctionCreatableSelect,
    }}
    onSubmit={onSubmit}
  />
)

describe('ToggleWidget', () => {
  beforeEach(() => {
    cy.viewport(800, 800)
  })

  it('should render the unchecked toggle', () => {
    cy.mountWithProviders(mockFunctionCreatableSelect(false))
    cy.getByTestId('root_loopPreventionEnabled').within(() => {
      cy.get('label[for="root_loopPreventionEnabled"]').should('contain.text', 'Enable loop prevention')
      cy.get('label[for="root_loopPreventionEnabled"]+label').should('not.have.attr', 'data-checked')

      cy.get('label[for="root_loopPreventionEnabled"]').click()
      cy.get('label[for="root_loopPreventionEnabled"]+label').should('have.attr', 'data-checked')
    })
  })

  it('should render the checked toggle', () => {
    cy.mountWithProviders(mockFunctionCreatableSelect(true))
    cy.getByTestId('root_loopPreventionEnabled').within(() => {
      cy.get('label[for="root_loopPreventionEnabled"]').should('contain.text', 'Enable loop prevention')
      cy.get('label[for="root_loopPreventionEnabled"]+label').should('have.attr', 'data-checked')

      cy.get('label[for="root_loopPreventionEnabled"]').click()
      cy.get('label[for="root_loopPreventionEnabled"]+label').should('not.have.attr', 'data-checked')
    })
  })

  it('should submit the proper formData', () => {
    const onSubmit = cy.stub().as('onSubmit')
    cy.mountWithProviders(mockFunctionCreatableSelect(true, onSubmit))

    cy.get('@onSubmit').should('not.have.been.called')
    cy.get('button[type="submit"]').click()
    cy.get('@onSubmit')
      .should('have.been.calledWith', Cypress.sinon.match.object)
      .its('firstCall.args.0')
      .should('deep.include', {
        formData: { loopPreventionEnabled: true },
      })
  })

  it('should be accessible', () => {
    cy.injectAxe()
    cy.mountWithProviders(mockFunctionCreatableSelect(true))

    cy.checkAccessibility(undefined, {
      rules: {
        // TODO[NVL] ReactSelect not tagging properly the listbox
        'aria-input-field-name': { enabled: false },
        'scrollable-region-focusable': { enabled: false },
      },
    })
  })
})
