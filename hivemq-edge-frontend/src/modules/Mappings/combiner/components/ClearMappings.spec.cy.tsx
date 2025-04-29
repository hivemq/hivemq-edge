import type { DataCombining, Instruction } from '@/api/__generated__'
import { DataIdentifierReference } from '@/api/__generated__'

import { ClearMappings } from './ClearMappings'

const mockFormData: DataCombining = {
  id: '58677276-fc48-4a9a-880c-41c755f5063b',
  sources: {
    primary: { id: 'my/tag/t1', type: DataIdentifierReference.type.TAG },
    tags: ['my/tag/t1', 'my/tag/t3'],
    topicFilters: ['my/topic/+/temp'],
  },
  destination: { topic: 'my/topic' },
  instructions: [],
}

const mockInstructions: Instruction[] = [
  {
    source: '$.dropped-property',
    destination: '$.lastName',
  },
]

describe('ClearMappings', () => {
  beforeEach(() => {
    cy.viewport(800, 800)
  })

  it('should render properly', () => {
    const onClick = cy.stub().as('onClick')

    cy.mountWithProviders(
      <ClearMappings formData={{ ...mockFormData, instructions: mockInstructions }} onChange={onClick} />
    )

    cy.get('button').should('have.attr', 'aria-label', 'Clear mappings')
    cy.get('button').should('not.be.disabled')

    cy.get('@onClick').should('not.have.been.called')
    cy.get('button').click()
    cy.get('@onClick').should('have.been.called')
  })

  it('should render properly with no data', () => {
    cy.mountWithProviders(<ClearMappings formData={mockFormData} onChange={cy.stub} />)
    cy.get('button').should('have.attr', 'aria-label', 'Clear mappings')
    cy.get('button').should('be.disabled')
  })

  it('should be accessible', () => {
    cy.injectAxe()
    cy.mountWithProviders(
      <ClearMappings formData={{ ...mockFormData, instructions: mockInstructions }} onChange={cy.stub} />
    )

    cy.checkAccessibility()
  })
})
