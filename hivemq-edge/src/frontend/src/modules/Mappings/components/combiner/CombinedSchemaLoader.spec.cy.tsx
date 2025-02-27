import type { DataCombining } from '@/api/__generated__'
import { DataIdentifierReference } from '@/api/__generated__'
import { mockCombinerMapping } from '@/api/hooks/useCombiners/__handlers__'
import type { CombinerContext } from '@/modules/Mappings/types'

import { CombinedSchemaLoader } from './CombinedSchemaLoader'

const mockFormData: DataCombining = {
  ...mockCombinerMapping,
  sources: {
    primary: { id: '', type: DataIdentifierReference.type.TAG },
    tags: ['test'],
    topicFilters: ['truc'],
  },
}

const mockFormContext: CombinerContext = {
  queries: [],
  entities: [],
}

describe('CombinedSchemaLoader', () => {
  beforeEach(() => {
    cy.viewport(800, 800)
  })

  it('should render warning', () => {
    cy.mountWithProviders(<CombinedSchemaLoader />)

    cy.get('[role="alert"]')
      .should('have.attr', 'data-status', 'info')
      .should('have.text', 'There are no schemas available yet')
  })

  it('should render properly', () => {
    cy.mountWithProviders(<CombinedSchemaLoader formData={mockFormData} formContext={mockFormContext} />)

    // TODO[NVL] Need to build a wrapper and generate the mocks
    cy.get('[role="alert"]')
      .should('have.attr', 'data-status', 'info')
      .should('have.text', 'There are no schemas available yet')
  })

  it('should be accessible', () => {
    cy.injectAxe()
    cy.mountWithProviders(<CombinedSchemaLoader />)

    cy.checkAccessibility()
  })
})
