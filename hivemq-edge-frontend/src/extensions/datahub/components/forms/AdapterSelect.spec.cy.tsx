/// <reference types="cypress" />

import type { WidgetProps } from '@rjsf/utils'
import { AdapterSelect } from '@datahub/components/forms/AdapterSelect.tsx'

// @ts-ignore No need for the whole props for testing
const MOCK_ADAPTER_PROPS: WidgetProps = {
  id: 'adapter-widget',
  label: 'Adapter',
  name: 'adapter-select',
  onBlur: () => undefined,
  onChange: () => undefined,
  onFocus: () => undefined,
  schema: {},
  options: {},
}

describe('AdapterSelect', () => {
  beforeEach(() => {
    cy.viewport(800, 600)
    cy.intercept('/api/v1/management/protocol-adapters/adapters', { items: [] }).as('getAdapters')
  })

  it('should render the AdapterSelect', () => {
    cy.mountWithProviders(<AdapterSelect {...MOCK_ADAPTER_PROPS} />)
  })
})
