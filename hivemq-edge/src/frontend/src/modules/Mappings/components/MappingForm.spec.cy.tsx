/// <reference types="cypress" />

import { mockAdapter_OPCUA, mockProtocolAdapter_OPCUA } from '@/api/hooks/useProtocolAdapters/__handlers__'
import MappingForm from '@/modules/Mappings/components/MappingForm.tsx'
import { MappingType } from '@/modules/Mappings/types.ts'

describe('MappingForm', () => {
  beforeEach(() => {
    cy.viewport(800, 800)
    cy.intercept('/api/v1/management/protocol-adapters/types', { items: [mockProtocolAdapter_OPCUA] }).as('getProtocol')
    cy.intercept('/api/v1/management/protocol-adapters/adapters', { items: [mockAdapter_OPCUA] }).as('getAdapter')
    cy.intercept('/api/v1/management/bridges', { items: [] })
    cy.intercept('http://json-schema.org/draft/2020-12/schema', { statusCode: 404 })
  })

  it('should render properly', () => {
    cy.mountWithProviders(
      <MappingForm adapterId={mockAdapter_OPCUA.id} type={MappingType.OUTWARD} onSubmit={cy.stub} />,
      {
        routerProps: { initialEntries: [`/node/wrong-adapter`] },
      }
    )

    cy.getByTestId('mapping-editor-switch').should('be.visible')

    // TODO[NVL] To do after validation and array template updated
  })
})
