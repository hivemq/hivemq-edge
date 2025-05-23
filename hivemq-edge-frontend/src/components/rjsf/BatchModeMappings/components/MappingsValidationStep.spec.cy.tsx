import type { BatchModeStore } from '@/components/rjsf/BatchModeMappings/types.ts'
import MappingsValidationStep from '@/components/rjsf/BatchModeMappings/components/MappingsValidationStep.tsx'
import {
  MOCK_ID_SCHEMA,
  MOCK_MAPPING,
  MOCK_SCHEMA,
  MOCK_WORKSHEET,
} from '@/components/rjsf/BatchModeMappings/__test-utils__/store.mocks.ts'

const MOCK_STORE: BatchModeStore = {
  idSchema: MOCK_ID_SCHEMA,
  schema: MOCK_SCHEMA,
  worksheet: MOCK_WORKSHEET,
  mapping: MOCK_MAPPING,
}

describe('SubscriptionsValidationStep', () => {
  beforeEach(() => {
    cy.viewport(800, 600)
  })

  it('should render the steps', () => {
    cy.mountWithProviders(<MappingsValidationStep onContinue={cy.stub()} store={MOCK_STORE} />)

    cy.get('table').as('table').should('have.attr', 'aria-label', 'List of subscriptions to be created')
    cy.get('@table').find('thead tr').eq(0).find('th').should('have.length', 2)
    cy.get('@table').find('thead tr').eq(1).find('th').as('rowHeader').should('have.length', 4)
    cy.get('@rowHeader').eq(0).find('label > input').should('have.attr', 'aria-label', 'Select all rows')
    cy.get('@rowHeader').eq(0).find('label > span').should('not.have.attr', 'data-checked')
    cy.get('@rowHeader').eq(0).find('label > span').should('not.have.attr', 'data-indeterminate')
    cy.get('@rowHeader').eq(1).should('have.text', 'row')
    cy.get('@rowHeader').eq(2).should('have.text', 'Destination MQTT topic')
    cy.get('@rowHeader').eq(3).should('have.text', 'Source Node ID')

    cy.get('@table').find('tbody tr').as('body').should('have.length', 2)
    cy.get('@body').find('td > label > input').should('have.attr', 'aria-label', 'Select the row')
    cy.get('@body').find('td > label > span').should('not.have.attr', 'data-checked')
    cy.get('@body').find('td > label > span').should('not.have.attr', 'data-indeterminate')

    cy.get('@table').find('tfoot tr').eq(1).as('footer')
    cy.get('@footer').find('td').eq(0).find('button').should('have.text', 'Delete rows')
    cy.get('@footer').find('td').eq(1).find('[role="group"]').should('have.text', 'Show only errors')
  })
})
