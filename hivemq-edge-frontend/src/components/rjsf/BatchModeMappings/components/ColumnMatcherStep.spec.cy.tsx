import type { BatchModeStore } from '@/components/rjsf/BatchModeMappings/types.ts'
import ColumnMatcherStep from '@/components/rjsf/BatchModeMappings/components/ColumnMatcherStep.tsx'
import {
  MOCK_ID_SCHEMA,
  MOCK_SCHEMA,
  MOCK_WORKSHEET,
} from '@/components/rjsf/BatchModeMappings/__test-utils__/store.mocks.ts'

const MOCK_STORE: BatchModeStore = {
  idSchema: MOCK_ID_SCHEMA,
  schema: MOCK_SCHEMA,
  worksheet: MOCK_WORKSHEET,
}

describe('ColumnMatcherStep', () => {
  beforeEach(() => {
    cy.viewport(600, 600)
  })

  it('should render the mapping form', () => {
    cy.mountWithProviders(<ColumnMatcherStep onContinue={cy.stub()} store={MOCK_STORE} />)

    cy.get('form#batch-mapping-form').find('[role="group"]').as('mapper')
    cy.get('@mapper').should('have.length', 2)
    cy.get('input[name="mapping.0.column"]').should('have.value', '')
    cy.get('input[name="mapping.0.subscription"]').should('have.value', 'mqtt-topic')
    cy.get('#mapping\\.0\\.error').should(
      'contain.text',
      'The subscription property is required so a column must be selected'
    )
    cy.get('input[name="mapping.1.column"]').should('have.value', 'd')
    cy.get('input[name="mapping.1.subscription"]').should('have.value', 'node')

    cy.get('#mapping\\.0\\.column').click()
    cy.get('#react-select-mapping\\.0\\.column-listbox').find('[role="option"]').as('columnHeaders')
    cy.get('@columnHeaders').should('have.length', 4)
    cy.get('@columnHeaders').first().should('contain.text', 'a')
  })

  it('should validate the form', () => {
    cy.mountWithProviders(<ColumnMatcherStep onContinue={cy.stub().as('onContinue')} store={MOCK_STORE} />)
    cy.get('@onContinue').should('have.been.calledWith', { mapping: undefined })

    cy.get('#mapping\\.0\\.error').should(
      'contain.text',
      'The subscription property is required so a column must be selected'
    )

    cy.get('input[name="mapping.0.column"]').should('have.value', '')
    cy.get('#mapping\\.0\\.column').click()
    cy.get('#react-select-mapping\\.0\\.column-listbox').find('[role="option"]').as('columnHeaders')
    cy.get('@columnHeaders').first().should('contain.text', 'a')
    cy.get('@columnHeaders').first().click()
    cy.get('input[name="mapping.0.column"]').should('have.value', 'a')
    cy.get('@onContinue').should('have.been.calledWith', {
      mapping: [
        {
          column: 'a',
          subscription: 'mqtt-topic',
        },
        {
          column: 'd',
          subscription: 'node',
        },
      ],
    })
  })

  it('should be accessible', () => {
    cy.injectAxe()
    cy.mountWithProviders(<ColumnMatcherStep onContinue={cy.stub()} store={MOCK_STORE} />)
    cy.checkAccessibility()
    cy.percySnapshot('Component: ColumnMatcherStep')
  })
})
