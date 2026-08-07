/// <reference types="cypress" />

import type { RJSFSchema } from '@rjsf/utils'

import type { Instruction } from '@/api/__generated__'
import { MappingInstructionList } from './MappingInstructionList'

// A southbound value shape with one genuinely read-only field: the envelope is already gone from the
// southbound schema, so read-only can only occur on a device-level non-writable field.
const MOCK_SCHEMA: RJSFSchema = {
  type: 'object',
  properties: {
    setpoint: { type: 'number', title: 'setpoint' },
    unit: { type: 'string', title: 'unit', readOnly: true },
  },
}

const MOCK_INSTRUCTIONS: Instruction[] = [
  { source: '$.a', destination: '$.setpoint' },
  { source: '$.b', destination: '$.unit' },
]

describe('MappingInstructionList', () => {
  beforeEach(() => {
    cy.viewport(800, 600)
  })

  it('should hide read-only properties instead of rendering a neutralised card', () => {
    // EDG-59: a read-only property is not a writable destination, so it must not appear in the list at all.
    cy.mountWithProviders(<MappingInstructionList schema={MOCK_SCHEMA} instructions={[]} onChange={cy.stub()} />)

    cy.get('[role="list"] li').should('have.length', 1)
    cy.getByTestId('property-name').should('have.text', 'setpoint')
    cy.get('[role="alert"]').should('not.exist')
  })

  it('should prune persisted instructions that target a read-only property', () => {
    // An instruction created before the property became read-only must not survive the next edit.
    const onChange = cy.stub().as('onChange')
    cy.mountWithProviders(
      <MappingInstructionList schema={MOCK_SCHEMA} instructions={MOCK_INSTRUCTIONS} onChange={onChange} />
    )

    cy.getByTestId('mapping-instruction-dropzone').should('have.length', 1).should('have.text', 'a')
    cy.getByAriaLabel('Clear mapping').click()
    cy.get('@onChange').should('have.been.calledWith', [])
  })

  it('should be accessible', () => {
    cy.injectAxe()
    cy.mountWithProviders(
      <MappingInstructionList schema={MOCK_SCHEMA} instructions={MOCK_INSTRUCTIONS} onChange={cy.stub()} />
    )
    cy.checkAccessibility()
  })
})
