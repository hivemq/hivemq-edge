/// <reference types="cypress" />

import { MockAdapterType } from '@/__test-utils__/adapters/types'
import { GENERATE_DATA_MODELS } from '@/api/hooks/useDomainModel/__handlers__'
import { MOCK_DEVICE_TAGS } from '@/api/hooks/useProtocolAdapters/__handlers__'
import { TagSchemaPanel } from './TagSchemaPanel'

const mocTag = MOCK_DEVICE_TAGS('opcua-1', MockAdapterType.OPC_UA)[0]

describe('TagSchemaPanel', () => {
  beforeEach(() => {
    cy.viewport(800, 800)
    cy.intercept('/api/v1/management/protocol-adapters/schema/**', GENERATE_DATA_MODELS(true, 'test'))
  })

  it('should render properly', () => {
    cy.mountWithProviders(<TagSchemaPanel adapterId="test" tag={mocTag} />)

    cy.getByTestId('tag-schema-header').should('have.text', 'Tag')
    cy.getByTestId('topic-wrapper').should('have.text', 'opcua-1 / power / off')
    cy.getByTestId('tag-schema-panel').within(() => {
      cy.get('label').should('have.text', 'Current schema')
      cy.get('h3').should('have.text', 'test')
      cy.get('[role="list"] li').should('length', 8)
      cy.get('#tag-schema-panel-helptext').should('have.text', 'Reading and writing this tag use the same schema.')
    })
    cy.getByTestId('tag-schema-download').should('have.text', 'Download the schema').should('not.be.disabled')
  })

  it('should show a single schema when reading and writing are identical', () => {
    cy.mountWithProviders(<TagSchemaPanel adapterId="test" tag={mocTag} />)

    // Both directions return the same mock, so only the read panel is rendered.
    cy.getByTestId('tag-schema-read').should('be.visible')
    cy.getByTestId('tag-schema-panel-write').should('not.exist')
    cy.getByTestId('tag-schema-panel').find('label').should('have.text', 'Current schema')
  })

  it('should show both schemas when reading and writing differ', () => {
    // A tag whose write shape is not a projection of its read shape, e.g. an OPC-UA condition tag.
    // The read request carries no `direction` (it is the default), so only WRITE needs its own intercept;
    // reads fall through to the wildcard registered in beforeEach.
    cy.intercept(
      { method: 'GET', pathname: '**/protocol-adapters/schema/**', query: { direction: 'WRITE' } },
      GENERATE_DATA_MODELS(true, 'writeModel')
    )

    cy.mountWithProviders(<TagSchemaPanel adapterId="test" tag={mocTag} />)

    cy.getByTestId('tag-schema-panel').within(() => {
      cy.get('label').should('have.text', 'Read schema')
      cy.get('#tag-schema-panel-helptext').should(
        'have.text',
        'The data published for this tag. A valid source for data transformation.'
      )
    })

    cy.getByTestId('tag-schema-panel-write').within(() => {
      cy.get('label').should('have.text', 'Write schema')
      cy.get('#tag-schema-panel-write-helptext').should(
        'have.text',
        'The shape of a write to this tag. Whether an individual field can be written is shown per field.'
      )
    })
  })

  it('should be accessible', () => {
    cy.injectAxe()
    cy.mountWithProviders(<TagSchemaPanel adapterId="test" tag={mocTag} />)

    cy.checkAccessibility()
  })
})
