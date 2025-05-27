import type { WidgetProps } from '@rjsf/utils'

import { mockDataPointOPCUA } from '@/api/hooks/useProtocolAdapters/__handlers__'
import AdapterTagSelect from '@/components/rjsf/Widgets/AdapterTagSelect.tsx'
import type { AdapterContext } from '@/modules/ProtocolAdapters/types.ts'

const MOCK_ADAPTER_PROPS: WidgetProps = {
  id: 'adapter-widget',
  label: 'Adapter',
  name: 'adapter-select',
  onBlur: () => undefined,
  onChange: () => undefined,
  onFocus: () => undefined,
  schema: {},
  options: {},
  formContext: {
    adapterId: '1234',
    adapterType: 'opc-au',
    isDiscoverable: true,
  } as AdapterContext,
  registry: {
    // @ts-ignore No need for the whole props for testing
    templates: {
      BaseInputTemplate: () => <p>This is the default template</p>,
    },
  },
}

describe('AdapterTagSelect', () => {
  beforeEach(() => {
    cy.viewport(800, 600)
    cy.intercept('/api/v1/management/protocol-adapters/**/discover', mockDataPointOPCUA)
  })

  it('should throw an error without the context defined', () => {
    const props = { ...MOCK_ADAPTER_PROPS, formContext: { isEditAdapter: false, isDiscoverable: false } }
    cy.on('uncaught:exception', (err) => {
      expect(err.message).to.contains('The adapter has not been added to the form context')
      return false
    })
    cy.mountWithProviders(<AdapterTagSelect {...props} />)
  })

  it('should render the default template if not discoverable', () => {
    const props = {
      ...MOCK_ADAPTER_PROPS,
      formContext: { isEditAdapter: false, isDiscoverable: false, adapterId: '1234', adapterType: 'opc-au' },
    }
    cy.mountWithProviders(<AdapterTagSelect {...props} />)
    cy.get('p').should('contain.text', 'This is the default template')
  })

  it('should render the select component with the right data points', () => {
    const props = {
      ...MOCK_ADAPTER_PROPS,
    }
    cy.mountWithProviders(<AdapterTagSelect {...props} />)
    cy.get('#react-select-dataPoint-container').should('be.visible')

    cy.get('#react-select-dataPoint-container').should('contain.text', 'Select...')

    // cy.get('#react-select-dataPoint-listbox').should('not.be.visible')
    cy.get('#react-select-dataPoint-container').click()
    cy.get('#react-select-dataPoint-listbox').should('be.visible')
    cy.get('#react-select-dataPoint-listbox').find('[role="option"]').as('options')
    cy.get('@options').should('have.length', 8)
    cy.get('@options').eq(0).find('[data-testid="dataPoint-name"]').should('have.text', 'Constant')
    cy.get('@options').eq(0).find('[data-testid="dataPoint-id"]').should('have.text', 'ns=3;i=1001')
    cy.get('@options')
      .eq(0)
      .find('[data-testid="dataPoint-description"]')
      .should(
        'have.text',
        'New range of formal shirts are designed keeping you in mind. With fits and styling that will make you stand apart'
      )

    cy.get('@options').eq(3).click()
    cy.get('#react-select-dataPoint-container').should('contain.text', 'ns=3;i=1004')
  })

  it('should be accessible', () => {
    cy.injectAxe()
    const props = {
      ...MOCK_ADAPTER_PROPS,
    }
    cy.mountWithProviders(<AdapterTagSelect {...props} />)
    cy.get('#react-select-dataPoint-container').click()

    cy.checkAccessibility(undefined, {
      rules: {
        // TODO[NVL] ReactSelect not tagging properly the listbox
        'aria-input-field-name': { enabled: false },
        'scrollable-region-focusable': { enabled: false },
      },
    })
    cy.percySnapshot('Component: AdapterTagSelect')
  })
})
