import type { TypeIdentifier } from '@/api/__generated__'
import { mockCombiner, mockCombinerId } from '@/api/hooks/useCombiners/__handlers__'

import { SourceCombiner } from './SourceCombiner'

const mockCombinerIdentifier: TypeIdentifier = {
  // TODO[31411] The COMBINER type identifier is missing from the OpenAPI specs
  // @ts-ignore
  type: 'DATA_COMBINING',
  identifier: mockCombinerId,
}

describe('SourceCombiner', () => {
  beforeEach(() => {
    cy.viewport(800, 600)
  })

  it('should render properly', () => {
    cy.intercept('GET', '/api/v1/management/combiners/**', mockCombiner)
    cy.mountWithProviders(<SourceCombiner source={mockCombinerIdentifier} />)

    cy.get('a').should('have.text', 'my-combiner')
    cy.get('a').should('have.attr', 'href', `/workspace/combiner/${mockCombinerId}`)

    cy.get('a > svg').should('have.attr', 'data-type', 'DATA_COMBINING')
  })

  it('should render non-existent combiner', () => {
    cy.intercept('GET', '/api/v1/management/combiners/**', { statusCode: 404 })
    cy.mountWithProviders(<SourceCombiner source={mockCombinerIdentifier} />)

    cy.get('a').should('not.exist')
    cy.get('p').should('have.text', mockCombinerId)
  })

  it('should be accessible', () => {
    cy.intercept('GET', '/api/v1/management/combiners/**', mockCombiner)
    cy.injectAxe()
    cy.mountWithProviders(<SourceCombiner source={mockCombinerIdentifier} />)
    cy.checkAccessibility()
  })
})
