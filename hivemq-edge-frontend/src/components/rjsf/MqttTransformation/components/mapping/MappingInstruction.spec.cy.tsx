import { DataIdentifierReference, type Instruction } from '@/api/__generated__'
import { formatTopicString } from '@/components/MQTT/topic-utils.ts'
import type { FlatJSONSchema7 } from '@/components/rjsf/MqttTransformation/utils/json-schema.utils.ts'
import MappingInstruction from './MappingInstruction.tsx'

// ─── Mock properties ──────────────────────────────────────────────────────────

const MOCK_PROPERTY_OBJECT: FlatJSONSchema7 = {
  description: undefined,
  path: [],
  key: 'billing-address',
  title: 'Billing address',
  type: 'object',
}

const MOCK_PROPERTY_STRING: FlatJSONSchema7 = {
  description: undefined,
  path: [],
  key: 'billing-address',
  title: 'Billing address',
  type: 'string',
}

const MOCK_PROPERTY_STRING_REQUIRED: FlatJSONSchema7 = {
  description: undefined,
  path: [],
  key: 'name',
  title: 'Name',
  type: 'string',
  required: true,
}

const MOCK_PROPERTY_READONLY: FlatJSONSchema7 = {
  description: undefined,
  path: [],
  key: 'system-id',
  title: 'System ID',
  type: 'string',
  readOnly: true,
}

// ─── Mock instructions ────────────────────────────────────────────────────────

const MOCK_INSTRUCTION_NO_REF: Instruction = {
  source: 'this is a mapping',
  destination: 'my-node',
}

const MOCK_INSTRUCTION_TAG_WITH_SCOPE: Instruction = {
  source: 'value',
  destination: 'billing-address',
  sourceRef: {
    id: 'my/tag/t1',
    type: DataIdentifierReference.type.TAG,
    scope: 'my-adapter',
  },
}

const MOCK_INSTRUCTION_TAG_NO_SCOPE: Instruction = {
  source: 'value',
  destination: 'billing-address',
  sourceRef: {
    id: 'my/tag/t1',
    type: DataIdentifierReference.type.TAG,
    scope: null,
  },
}

const MOCK_INSTRUCTION_TAG_LONG: Instruction = {
  source: 'value',
  destination: 'billing-address',
  sourceRef: {
    id: 'industrial/plant/floor2/machine-a/sensor-12',
    type: DataIdentifierReference.type.TAG,
    scope: 'opcua-production-adapter',
  },
}

const MOCK_INSTRUCTION_TOPIC_FILTER: Instruction = {
  source: 'value',
  destination: 'billing-address',
  sourceRef: {
    id: 'factory/+/sensors/temperature',
    type: DataIdentifierReference.type.TOPIC_FILTER,
    scope: null,
  },
}

const MOCK_INSTRUCTION_PULSE_ASSET: Instruction = {
  source: 'value',
  destination: 'billing-address',
  sourceRef: {
    id: 'asset/pump-station-a',
    type: DataIdentifierReference.type.PULSE_ASSET,
    scope: null,
  },
}

// ─── Tests ────────────────────────────────────────────────────────────────────

describe('MappingInstruction', () => {
  beforeEach(() => {
    cy.viewport(800, 900)
  })

  // ── Existing behaviour: no sourceRef ────────────────────────────────────────

  it('should render required property without mapping with Required alert', () => {
    cy.mountWithProviders(
      <MappingInstruction
        property={MOCK_PROPERTY_STRING_REQUIRED}
        showTransformation={false}
        onChange={cy.stub()}
        instruction={undefined}
      />
    )

    cy.getByAriaLabel('Clear mapping').should('be.disabled')
    cy.getByTestId('property-name').should('have.text', 'Name')
    cy.getByAriaLabel('Property').should('have.attr', 'data-type', 'string').should('not.have.attr', 'draggable')
    cy.getByTestId('mapping-instruction-dropzone').should('have.text', 'Drag a source property here')
    cy.get('[role="alert"]').should('contain.text', 'Required').should('have.attr', 'data-status', 'error')
    cy.getByTestId('mapping-instruction-source-owner').should('not.exist')
  })

  it('should render optional property without mapping with no alert', () => {
    cy.mountWithProviders(
      <MappingInstruction
        property={MOCK_PROPERTY_STRING}
        showTransformation={false}
        onChange={cy.stub()}
        instruction={undefined}
      />
    )

    cy.getByAriaLabel('Clear mapping').should('be.disabled')
    cy.getByTestId('property-name').should('have.text', 'Billing address')
    cy.getByTestId('mapping-instruction-dropzone').should('have.text', 'Drag a source property here')
    cy.get('[role="alert"]').should('not.exist')
    cy.getByTestId('mapping-instruction-source-owner').should('not.exist')
  })

  it('should render mapping without sourceRef (legacy data)', () => {
    cy.mountWithProviders(
      <MappingInstruction
        property={MOCK_PROPERTY_STRING}
        showTransformation={false}
        onChange={cy.stub()}
        instruction={MOCK_INSTRUCTION_NO_REF}
      />
    )

    cy.getByAriaLabel('Clear mapping').should('not.be.disabled')
    cy.getByTestId('property-name').should('have.text', 'Billing address')
    cy.getByTestId('mapping-instruction-dropzone').should('have.text', 'this is a mapping')
    cy.get('[role="alert"]').should('contain.text', 'Matching').should('have.attr', 'data-status', 'success')
    // No sourceRef → ownership row must not render
    cy.getByTestId('mapping-instruction-source-owner').should('not.exist')
  })

  it('should render object type as not-supported', () => {
    cy.mountWithProviders(
      <MappingInstruction
        property={MOCK_PROPERTY_OBJECT}
        showTransformation={false}
        onChange={cy.stub()}
        instruction={undefined}
      />
    )

    cy.getByTestId('property-name').should('have.text', 'Billing address')
    cy.getByAriaLabel('Property').should('have.attr', 'data-type', 'object').should('not.have.attr', 'draggable')
    cy.getByTestId('mapping-instruction-dropzone').should('not.exist')
    cy.getByAriaLabel('Clear mapping').should('not.exist')
    cy.get('[role="alert"]').should('contain.text', 'Not supported').should('have.attr', 'data-status', 'warning')
  })

  it('should render readonly property properly', () => {
    cy.mountWithProviders(
      <MappingInstruction
        property={MOCK_PROPERTY_READONLY}
        showTransformation={false}
        onChange={cy.stub()}
        instruction={undefined}
      />
    )

    cy.getByTestId('mapping-instruction-readonly').should('exist')
    cy.getByTestId('property-name').should('have.text', 'System ID')
    cy.getByAriaLabel('Property').should('have.attr', 'data-type', 'string').should('not.have.attr', 'draggable')
    cy.getByTestId('property-readonly').should('exist')
    cy.getByTestId('mapping-instruction-dropzone').should('not.exist')
    cy.getByAriaLabel('Clear mapping').should('not.exist')
    cy.get('[role="alert"]').should('contain.text', 'Read-only').should('have.attr', 'data-status', 'info')
  })

  // ── Ownership row: TAG sources ───────────────────────────────────────────────

  it('should render ownership row with PLCTag for TAG source with scope', () => {
    cy.mountWithProviders(
      <MappingInstruction
        property={MOCK_PROPERTY_STRING}
        showTransformation={false}
        onChange={cy.stub()}
        instruction={MOCK_INSTRUCTION_TAG_WITH_SCOPE}
      />
    )

    cy.getByTestId('mapping-instruction-source-owner').should('exist')
    // PLCTag applies formatTopicString to the full tagTitle (scope :: id).
    cy.getByTestId('mapping-instruction-source-owner')
      .getByTestId('topic-wrapper')
      .should('contain.text', formatTopicString('my-adapter :: my/tag/t1'))
    // Drop zone and status are unaffected
    cy.getByTestId('mapping-instruction-dropzone').should('contain.text', 'value')
    cy.get('[role="alert"]').should('contain.text', 'Matching')
  })

  it('should render ownership row with PLCTag for TAG source without scope', () => {
    cy.mountWithProviders(
      <MappingInstruction
        property={MOCK_PROPERTY_STRING}
        showTransformation={false}
        onChange={cy.stub()}
        instruction={MOCK_INSTRUCTION_TAG_NO_SCOPE}
      />
    )

    cy.getByTestId('mapping-instruction-source-owner').should('exist')
    cy.getByTestId('mapping-instruction-source-owner')
      .getByTestId('topic-wrapper')
      .should('contain.text', formatTopicString('my/tag/t1'))
    // No scope prefix — no :: separator
    cy.getByTestId('mapping-instruction-source-owner').getByTestId('topic-wrapper').should('not.contain.text', '::')
  })

  it('should render ownership row with PLCTag for TAG source with long ID', () => {
    cy.mountWithProviders(
      <MappingInstruction
        property={MOCK_PROPERTY_STRING}
        showTransformation={false}
        onChange={cy.stub()}
        instruction={MOCK_INSTRUCTION_TAG_LONG}
      />
    )

    cy.getByTestId('mapping-instruction-source-owner').should('exist')
    cy.getByTestId('mapping-instruction-source-owner')
      .getByTestId('topic-wrapper')
      .should(
        'contain.text',
        formatTopicString('opcua-production-adapter :: industrial/plant/floor2/machine-a/sensor-12')
      )
  })

  // ── Ownership row: TOPIC_FILTER source ───────────────────────────────────────

  it('should render ownership row with TopicFilter for TOPIC_FILTER source', () => {
    cy.mountWithProviders(
      <MappingInstruction
        property={MOCK_PROPERTY_STRING}
        showTransformation={false}
        onChange={cy.stub()}
        instruction={MOCK_INSTRUCTION_TOPIC_FILTER}
      />
    )

    cy.getByTestId('mapping-instruction-source-owner').should('exist')
    cy.getByTestId('mapping-instruction-source-owner')
      .getByTestId('topic-wrapper')
      .should('contain.text', formatTopicString('factory/+/sensors/temperature'))
    // TopicFilter never has a scope — no :: separator
    cy.getByTestId('mapping-instruction-source-owner').getByTestId('topic-wrapper').should('not.contain.text', '::')
  })

  // ── Ownership row: PULSE_ASSET source ────────────────────────────────────────

  it('should not render ownership row for PULSE_ASSET source (not yet supported)', () => {
    cy.mountWithProviders(
      <MappingInstruction
        property={MOCK_PROPERTY_STRING}
        showTransformation={false}
        onChange={cy.stub()}
        instruction={MOCK_INSTRUCTION_PULSE_ASSET}
      />
    )

    cy.getByTestId('mapping-instruction-source-owner').should('not.exist')
  })

  // ── Drop zone is unaffected by the ownership row ─────────────────────────────

  it('should keep drop zone structurally unchanged when ownership row is present', () => {
    cy.mountWithProviders(
      <MappingInstruction
        property={MOCK_PROPERTY_STRING}
        showTransformation={false}
        onChange={cy.stub()}
        instruction={MOCK_INSTRUCTION_TAG_WITH_SCOPE}
      />
    )

    // Drop zone retains its role, aria-label, and testid
    cy.getByTestId('mapping-instruction-dropzone')
      .should('have.attr', 'role', 'group')
      .should('have.attr', 'aria-label')
    // Source path is still displayed inside the drop zone
    cy.getByTestId('mapping-instruction-dropzone').find('code').should('contain.text', 'value')
    // Clear button is enabled
    cy.getByAriaLabel('Clear mapping').should('not.be.disabled')
  })

  // ── Responsive: lg-panel viewport ────────────────────────────────────────────

  it('should render ownership row at lg-panel viewport (504px)', () => {
    cy.viewport(504, 900)
    cy.mountWithProviders(
      <MappingInstruction
        property={MOCK_PROPERTY_STRING}
        showTransformation={false}
        onChange={cy.stub()}
        instruction={MOCK_INSTRUCTION_TAG_WITH_SCOPE}
      />
    )

    cy.getByTestId('mapping-instruction-source-owner').should('be.visible')
    cy.getByTestId('mapping-instruction-dropzone').should('be.visible')
    cy.get('[role="alert"]').should('be.visible')
  })

  // ── Accessibility ─────────────────────────────────────────────────────────────

  it('should be accessible with readonly property', () => {
    cy.injectAxe()
    cy.mountWithProviders(
      <MappingInstruction
        property={MOCK_PROPERTY_READONLY}
        showTransformation={false}
        onChange={cy.stub()}
        instruction={undefined}
      />
    )
    cy.checkAccessibility()
  })

  it('should be accessible without instruction', () => {
    cy.injectAxe()
    cy.mountWithProviders(
      <MappingInstruction
        property={MOCK_PROPERTY_STRING}
        showTransformation={false}
        onChange={cy.stub()}
        instruction={undefined}
      />
    )
    cy.checkAccessibility()
  })

  it('should be accessible with TAG sourceRef', () => {
    cy.injectAxe()
    cy.mountWithProviders(
      <MappingInstruction
        property={MOCK_PROPERTY_STRING}
        showTransformation={false}
        onChange={cy.stub()}
        instruction={MOCK_INSTRUCTION_TAG_WITH_SCOPE}
      />
    )
    cy.checkAccessibility()
  })

  it('should be accessible with TOPIC_FILTER sourceRef', () => {
    cy.injectAxe()
    cy.mountWithProviders(
      <MappingInstruction
        property={MOCK_PROPERTY_STRING}
        showTransformation={false}
        onChange={cy.stub()}
        instruction={MOCK_INSTRUCTION_TOPIC_FILTER}
      />
    )
    cy.checkAccessibility()
  })
})
