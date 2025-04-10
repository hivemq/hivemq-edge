import type { MappingValidation } from '@/modules/Mappings/types.ts'
import DataModelDestination from './DataModelDestination.tsx'
import { GENERATE_DATA_MODELS } from '@/api/hooks/useDomainModel/__handlers__'

const MOCK_SUBS: MappingValidation = {
  status: 'error',
  errors: [],
}

const mockTopic = 'test/topic'

describe('DataModelDestination', () => {
  beforeEach(() => {
    cy.viewport(800, 900)
    cy.intercept('/api/v1/management/protocol-adapters/writing-schema/*/*', GENERATE_DATA_MODELS(true, mockTopic))
  })

  it('should render properly', () => {
    cy.mountWithProviders(
      <DataModelDestination topic={mockTopic} adapterId="my-adapter" adapterType="my-type" validation={MOCK_SUBS} />
    )

    cy.get('h3').should('have.text', 'Destination output')
    cy.get('[role=alert]').should('have.attr', 'data-status', 'error')

    // loading
    cy.getByTestId('loading-spinner').should('be.visible')
    cy.getByTestId('loading-spinner').should('not.exist')
    cy.get('[role=list]').find('li > div').as('properties')
    cy.get('@properties').should('have.length', 8)

    cy.get('@properties')
      .eq(0)
      .should('have.text', 'First String')
      .should('have.attr', 'data-type', 'string')
      .should('not.have.attr', 'draggable')
    cy.get('@properties')
      .eq(1)
      .should('have.text', 'Second String')
      .should('have.attr', 'data-type', 'string')
      .should('not.have.attr', 'draggable')

    cy.get('@properties')
      .eq(2)
      .should('have.text', 'Integer')
      .should('have.attr', 'data-type', 'integer')
      .should('not.have.attr', 'draggable')
  })

  it('should be accessible ', () => {
    cy.injectAxe()

    cy.mountWithProviders(
      <DataModelDestination topic={mockTopic} adapterId="my-adapter" adapterType="my-type" validation={MOCK_SUBS} />
    )
    cy.checkAccessibility(undefined, {
      rules: {
        // h5 used for sections is not in order. Not detected on other tests
        'heading-order': { enabled: false },
      },
    })
  })
})
