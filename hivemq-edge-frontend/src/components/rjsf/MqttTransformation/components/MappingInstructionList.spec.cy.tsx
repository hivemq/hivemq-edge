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

/**
 * The southbound document served for an OPC-UA A&C condition tag — the case EDG-845 exists for, where the
 * write target {eventId, method, comment} is not a projection of the read shape.
 *
 * This is a verbatim copy of the backend output, pinned by
 * `TagSchemaCreationOutputImplSchemaBuilderTest.test_southboundSchema_conditionCommandDocumentIsPinned`.
 * Do not "tidy" it: the readOnly on the root is really there (it is on the wrapper Edge builds, not on the
 * adapter's command schema), and the absence of readOnly on the three command fields is exactly what this
 * fixture is here to prove — an earlier hand-written fixture omitted those annotations altogether and so
 * could not have caught a backend that marked every field read-only.
 */
const MOCK_SOUTHBOUND_CONDITION_SCHEMA: RJSFSchema = {
  type: 'object',
  properties: {
    value: {
      type: 'object',
      properties: {
        eventId: { type: 'string' },
        method: { type: 'integer' },
        comment: { type: 'string' },
      },
      required: ['eventId', 'method'],
    },
  },
  required: ['value'],
  readOnly: true,
  $schema: 'https://json-schema.org/draft/2019-09/schema',
}

// A read-only container whose child carries no flag of its own — the shape an uploaded or inferred combiner
// destination schema routinely has, since JSON Schema does not require readOnly to be repeated on every leaf.
const MOCK_READONLY_ANCESTOR_SCHEMA: RJSFSchema = {
  type: 'object',
  properties: {
    deviceInfo: {
      type: 'object',
      readOnly: true,
      properties: {
        serial: { type: 'string' },
      },
    },
    setpoint: { type: 'number', title: 'setpoint' },
  },
}

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

  it('should offer every command field of an explicit southbound schema as a destination', () => {
    // The defining EDG-845 case. If the adapter's command schema were built without .writable(), every field
    // would render readOnly and this list would be empty — which is the whole feature failing silently.
    cy.mountWithProviders(
      <MappingInstructionList schema={MOCK_SOUTHBOUND_CONDITION_SCHEMA} instructions={[]} onChange={cy.stub()} />
    )

    cy.getByTestId('property-name').should('have.length', 4)
    cy.getByTestId('property-name').eq(1).should('have.text', 'value.eventId')
    cy.getByTestId('property-name').eq(2).should('have.text', 'value.method')
    cy.getByTestId('property-name').eq(3).should('have.text', 'value.comment')

    // `value` is the object wrapper, which is not itself mappable; the three command fields are.
    cy.getByTestId('mapping-instruction-dropzone').should('have.length', 3)
    cy.getByTestId('property-readonly').should('not.exist')
  })

  it('should emit the pruned instructions on load, without any user interaction', () => {
    // The renderer hiding a stale instruction is not enough: unless the sanitised list reaches the parent, the
    // instruction stays in the form data, invisible, and is still submitted and executed.
    const onChange = cy.stub().as('onChange')
    cy.mountWithProviders(
      <MappingInstructionList schema={MOCK_SCHEMA} instructions={MOCK_INSTRUCTIONS} onChange={onChange} />
    )

    cy.get('@onChange').should('have.been.calledOnceWith', [{ source: '$.a', destination: '$.setpoint' }])
  })

  it('should not emit anything when there is nothing to prune', () => {
    // Guards against an effect that re-fires and fights the parent's state on every render.
    const onChange = cy.stub().as('onChange')
    cy.mountWithProviders(
      <MappingInstructionList
        schema={MOCK_SCHEMA}
        instructions={[{ source: '$.a', destination: '$.setpoint' }]}
        onChange={onChange}
      />
    )

    cy.getByTestId('mapping-instruction-dropzone').should('have.length', 1)
    cy.get('@onChange').should('not.have.been.called')
  })

  it('should prune persisted instructions that target a read-only property on the next edit', () => {
    const onChange = cy.stub().as('onChange')
    cy.mountWithProviders(
      <MappingInstructionList schema={MOCK_SCHEMA} instructions={MOCK_INSTRUCTIONS} onChange={onChange} />
    )

    cy.getByTestId('mapping-instruction-dropzone').should('have.length', 1).should('have.text', 'a')
    cy.getByAriaLabel('Clear mapping').click()
    cy.get('@onChange').should('have.been.calledWith', [])
  })

  it('should treat descendants of a read-only object as non-writable', () => {
    // A read-only container cannot be written, so nothing beneath it can be either — even though `serial`
    // carries no flag of its own.
    const onChange = cy.stub().as('onChange')
    cy.mountWithProviders(
      <MappingInstructionList
        schema={MOCK_READONLY_ANCESTOR_SCHEMA}
        instructions={[
          { source: '$.a', destination: '$.setpoint' },
          { source: '$.b', destination: '$.deviceInfo.serial' },
        ]}
        onChange={onChange}
      />
    )

    cy.getByTestId('property-name').should('have.length', 1).should('have.text', 'setpoint')
    cy.get('@onChange').should('have.been.calledOnceWith', [{ source: '$.a', destination: '$.setpoint' }])
  })

  it('should be accessible', () => {
    cy.injectAxe()
    cy.mountWithProviders(
      <MappingInstructionList schema={MOCK_SCHEMA} instructions={MOCK_INSTRUCTIONS} onChange={cy.stub()} />
    )
    cy.checkAccessibility()
  })
})
