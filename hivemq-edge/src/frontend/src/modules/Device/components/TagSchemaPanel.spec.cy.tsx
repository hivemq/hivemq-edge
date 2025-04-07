/// <reference types="cypress" />

import { MockAdapterType } from '@/__test-utils__/adapters/types'
import { GENERATE_DATA_MODELS } from '@/api/hooks/useDomainModel/__handlers__'
import { MOCK_DEVICE_TAGS } from '@/api/hooks/useProtocolAdapters/__handlers__'
import { TagSchemaPanel } from './TagSchemaPanel'

const mocTag = MOCK_DEVICE_TAGS('opcua-1', MockAdapterType.OPC_UA)[0]

describe('TagSchemaPanel', () => {
  beforeEach(() => {
    cy.viewport(800, 800)
    cy.intercept('/api/v1/management/protocol-adapters/writing-schema/**', GENERATE_DATA_MODELS(true, 'test'))
  })

  it('should render properly', () => {
    cy.mountWithProviders(<TagSchemaPanel adapterId={'test'} tag={mocTag} />)

    cy.getByTestId('tag-schema-header').should('have.text', 'Tag')
    cy.getByTestId('topic-wrapper').should('have.text', 'opcua-1 / power / off')
    cy.getByTestId('tag-schema-panel').within(() => {
      cy.get('label').should('have.text', 'Current schema')
      cy.get('h3').should('have.text', 'test')
      cy.get('[role="list"] li').should('length', 8)
      cy.get('#tag-schema-panel-helptext').should(
        'have.text',
        'This schema will be a valid source for data transformation'
      )
    })
    cy.getByTestId('tag-schema-download').should('have.text', 'Download the schema').should('not.be.disabled')
  })

  it('should be accessible', () => {
    cy.injectAxe()
    cy.mountWithProviders(<TagSchemaPanel adapterId={'test'} tag={mocTag} />)

    cy.checkAccessibility()
  })
})
