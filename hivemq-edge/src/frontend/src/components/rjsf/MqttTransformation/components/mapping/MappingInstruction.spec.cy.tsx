import type { FlatJSONSchema7 } from '@/components/rjsf/MqttTransformation/utils/json-schema.utils.ts'
import { OutwardSubscription } from '@/modules/Subscriptions/types.ts'
import MappingInstruction from './MappingInstruction.tsx'

const MOCK_SUBS: OutwardSubscription = {
  node: 'my-node',
  'mqtt-topic': ['my-topic'],
  mapping: [],
}

const MOCK_PROPERTY: FlatJSONSchema7 = {
  description: undefined,
  path: [],
  title: 'Billing address',
  type: 'object',
}
describe('MappingInstruction', () => {
  beforeEach(() => {
    cy.viewport(800, 900)
  })

  it('should render properly', () => {
    cy.mountWithProviders(
      <MappingInstruction
        property={MOCK_PROPERTY}
        showTransformation={false}
        onChange={cy.stub()}
        mapping={MOCK_SUBS.mapping[0]}
      />
    )

    cy.getByAriaLabel('Clear mapping').should('be.disabled')
    cy.get('ul li')
      .eq(0)
      .should('have.text', 'Billing address')
      .should('have.attr', 'data-type', 'object')
      .should('not.have.attr', 'draggable')
    cy.getByTestId('mapping-instruction-dropzone').should('have.text', 'Drag a source property here')

    // TODO[NVL] Test drag and drop
  })

  it('should be accessible ', () => {
    cy.injectAxe()

    cy.mountWithProviders(
      <MappingInstruction
        property={MOCK_PROPERTY}
        showTransformation={false}
        onChange={cy.stub()}
        mapping={MOCK_SUBS.mapping[0]}
      />
    )

    cy.checkAccessibility()
  })
})
