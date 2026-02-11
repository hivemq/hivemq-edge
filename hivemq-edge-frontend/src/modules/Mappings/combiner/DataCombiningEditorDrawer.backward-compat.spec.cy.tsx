import type { DataCombining, EntityReference } from '@/api/__generated__'
import { DataIdentifierReference, EntityType } from '@/api/__generated__'
import type { RJSFSchema, UiSchema } from '@rjsf/utils'
import type { CombinerContext } from '@/modules/Mappings/types'
import DataCombiningEditorDrawer from './DataCombiningEditorDrawer'

/**
 * Backward Compatibility Tests for DataCombiningEditorDrawer
 *
 * Purpose: Ensure legacy combiner data (created before scope field was added)
 * still loads and can be edited without breaking the application.
 *
 * Context: Task 38936 added required `scope` field to DataIdentifierReference.
 * Legacy data will have TAG types without scope field, which must be handled gracefully.
 *
 * Note: These tests focus on component stability and validation. Full form field
 * interactions require integration tests with the complete schema generation pipeline.
 */
describe('DataCombiningEditorDrawer - Backward Compatibility', () => {
  // Legacy payload: TAG primary source WITHOUT scope field (old data)
  const legacyPayload: DataCombining = {
    id: 'legacy-combiner-123',
    sources: {
      // ❗ Missing scope field - this is old data!
      primary: { id: 'opcua/tag1', type: DataIdentifierReference.type.TAG } as DataCombining['sources']['primary'],
      tags: ['opcua/tag1', 'opcua/tag2'],
      topicFilters: [],
    },
    destination: {
      topic: 'test/legacy/topic',
      schema: 'data:application/json;base64,eyJ0eXBlIjoib2JqZWN0In0=',
    },
    instructions: [],
  }

  // Modern payload: same structure but WITH scope field
  const modernPayload: DataCombining = {
    ...legacyPayload,
    id: 'modern-combiner-456',
    sources: {
      ...legacyPayload.sources,
      primary: {
        id: 'opcua/tag1',
        type: DataIdentifierReference.type.TAG,
        scope: 'testopcua', // ✅ Has scope!
      },
    },
  }

  const mockEntities: EntityReference[] = [
    { id: 'testopcua', type: EntityType.ADAPTER },
    { id: 'edge-broker', type: EntityType.EDGE_BROKER },
  ]

  const mockCombinerContext: CombinerContext = {
    entities: mockEntities,
    queries: [],
  }

  const mockSchema: RJSFSchema = {
    title: 'Data Combining',
    type: 'object',
    properties: {
      sources: { type: 'object' },
      destination: { type: 'object' },
    },
  }

  const mockUiSchema: UiSchema = {
    'ui:submitButtonOptions': {
      norender: true,
    },
    sources: {
      'ui:title': 'Sources',
    },
    destination: {
      'ui:title': 'Destination',
    },
  }

  beforeEach(() => {
    cy.viewport(1200, 800)

    cy.intercept('/api/v1/management/protocol-adapters/types', {
      statusCode: 200,
      body: { items: [] },
    })

    cy.intercept('/api/v1/management/protocol-adapters/adapters', {
      statusCode: 200,
      body: { items: [] },
    })
  })

  it('should load legacy combiner payload without crashing', () => {
    const onClose = cy.stub().as('onClose')
    const onSubmit = cy.stub().as('onSubmit')

    cy.mountWithProviders(
      <DataCombiningEditorDrawer
        onClose={onClose}
        onSubmit={onSubmit}
        schema={mockSchema}
        uiSchema={mockUiSchema}
        formData={legacyPayload}
        formContext={mockCombinerContext}
      />
    )

    cy.get('header').should('contain.text', 'Data combining')
    cy.get('form').should('be.visible')

    cy.getByTestId('root_sources').should('exist')
    cy.getByTestId('root_destination').should('exist')

    cy.get('footer').within(() => {
      cy.get('button').contains('Cancel').should('be.visible')
      cy.get('button').contains('Save').should('be.visible')
    })

    cy.get('@onClose').should('not.have.been.called')
    cy.get('@onSubmit').should('not.have.been.called')
  })

  it('should show validation warning for missing scope in legacy data', () => {
    const onClose = cy.stub()
    const onSubmit = cy.stub()

    cy.mountWithProviders(
      <DataCombiningEditorDrawer
        onClose={onClose}
        onSubmit={onSubmit}
        schema={mockSchema}
        uiSchema={mockUiSchema}
        formData={legacyPayload}
        formContext={mockCombinerContext}
      />
    )

    cy.get('header').should('contain.text', 'Data combining')

    cy.get('footer').within(() => {
      cy.get('button').contains('Save').click()
    })

    cy.get('form').should('contain.text', 'scope')

    cy.screenshot('backward-compat-validation-warning', {
      capture: 'viewport',
      overwrite: true,
    })
  })

  it('should load modern combiner payload with scope field', () => {
    const onClose = cy.stub().as('onClose')
    const onSubmit = cy.stub().as('onSubmit')

    cy.mountWithProviders(
      <DataCombiningEditorDrawer
        onClose={onClose}
        onSubmit={onSubmit}
        schema={mockSchema}
        uiSchema={mockUiSchema}
        formData={modernPayload}
        formContext={mockCombinerContext}
      />
    )

    cy.get('header').should('contain.text', 'Data combining')
    cy.get('form').should('be.visible')

    cy.getByTestId('root_sources').should('exist')
    cy.getByTestId('root_destination').should('exist')

    cy.get('footer').within(() => {
      cy.get('button').contains('Cancel').should('be.visible')
      cy.get('button').contains('Save').should('be.visible')
    })

    cy.get('@onClose').should('not.have.been.called')
    cy.get('@onSubmit').should('not.have.been.called')
  })

  it('should be accessible with legacy payload', () => {
    cy.injectAxe()

    cy.mountWithProviders(
      <DataCombiningEditorDrawer
        onClose={cy.stub()}
        onSubmit={cy.stub()}
        schema={mockSchema}
        uiSchema={mockUiSchema}
        formData={legacyPayload}
        formContext={mockCombinerContext}
      />
    )

    cy.checkAccessibility()
  })
})
