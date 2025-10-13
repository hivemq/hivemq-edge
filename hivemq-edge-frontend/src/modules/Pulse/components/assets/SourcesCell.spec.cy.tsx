import type { Adapter } from '@/api/__generated__'
import { type Combiner, EntityType } from '@/api/__generated__'
import { MOCK_COMBINER_ASSET } from '@/api/hooks/useCombiners/__handlers__'
import { mockAdapter, mockProtocolAdapter } from '@/api/hooks/useProtocolAdapters/__handlers__'
import SourcesCell from '@/modules/Pulse/components/assets/SourcesCell.tsx'

describe('SourcesCell', () => {
  it('should render errors with 4xx', () => {
    cy.intercept('GET', '/api/v1/management/pulse/asset-mappers', { statusCode: 404 }).as('getCombiner')
    cy.mountWithProviders(<SourcesCell mappingId="wrong-combiner" />)
    cy.getByTestId('loading-spinner').should('be.visible')
    cy.wait('@getCombiner')
    cy.getByTestId('loading-spinner').should('not.exist')

    cy.getByTestId('sources-error').should('have.text', '< not found >')
  })

  it('should render errors with wrong combiner', () => {
    cy.intercept('GET', '/api/v1/management/pulse/asset-mappers', { items: [MOCK_COMBINER_ASSET] }).as('getCombiner')
    cy.mountWithProviders(<SourcesCell mappingId="wrong-combiner" />)
    cy.getByTestId('loading-spinner').should('be.visible')
    cy.wait('@getCombiner')
    cy.getByTestId('loading-spinner').should('not.exist')

    cy.getByTestId('sources-error').should('have.text', '< not found >')
  })

  it('should render properly', () => {
    cy.intercept('GET', '/api/v1/management/pulse/asset-mappers', { items: [MOCK_COMBINER_ASSET] }).as('getCombiner')
    cy.intercept('/api/v1/management/protocol-adapters/types', { items: [mockProtocolAdapter] })
    cy.intercept('api/v1/management/protocol-adapters/adapters', { items: [mockAdapter] })

    cy.mountWithProviders(<SourcesCell mappingId="ff02efff-7b4c-4f8c-8bf6-74d0756283fb" />)

    cy.getByTestId('loading-spinner').should('be.visible')
    cy.wait('@getCombiner')
    cy.getByTestId('sources-container').children().should('have.length', 1)
    cy.getByTestId('sources-container').within(() => {
      cy.getByTestId('node-name').should('have.text', 'my-adapter')
      cy.getByTestId('node-description').should('have.text', 'Simulated Edge Device')
    })
  })

  it('should render long list', () => {
    const testCases = Array.from(new Array(5).keys())
    const mockCombiner: Combiner = {
      ...MOCK_COMBINER_ASSET,
      sources: {
        items: testCases.map((e) => ({
          type: EntityType.ADAPTER,
          id: `my-adapter${e + 1}`,
        })),
      },
    }

    const mockAdapters: Adapter[] = testCases.map((e) => ({ ...mockAdapter, id: `my-adapter${e + 1}` }))

    cy.intercept('GET', '/api/v1/management/pulse/asset-mappers', { items: [mockCombiner] }).as('getCombiner')
    cy.intercept('/api/v1/management/protocol-adapters/types', { items: [mockProtocolAdapter] })
    cy.intercept('api/v1/management/protocol-adapters/adapters', { items: mockAdapters })

    cy.mountWithProviders(<SourcesCell mappingId="ff02efff-7b4c-4f8c-8bf6-74d0756283fb" />)

    cy.getByTestId('loading-spinner').should('be.visible')
    cy.wait('@getCombiner')
    cy.getByTestId('sources-container').children().should('have.length', 5)
    cy.getByTestId('sources-container').within(() => {
      cy.getByTestId('node-name').eq(0).should('have.text', 'my-adapter1')
      cy.getByTestId('node-description').eq(0).should('have.text', 'Simulated Edge Device')

      cy.getByTestId('node-name').eq(3).should('have.text', 'my-adapter4')
      cy.getByTestId('node-description').eq(3).should('have.text', 'Simulated Edge Device')
    })

    cy.getByTestId('sources-container').children().eq(4).should('have.text', '+1 more')
  })
})
