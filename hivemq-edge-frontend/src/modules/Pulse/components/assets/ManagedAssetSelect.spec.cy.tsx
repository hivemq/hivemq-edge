import { DataIdentifierReference } from '@/api/__generated__'
import { MOCK_CAPABILITY_PULSE_ASSETS } from '@/api/hooks/useFrontendServices/__handlers__'
import {
  MOCK_PULSE_EXT_ASSET_UNMAPPED_2,
  MOCK_PULSE_EXT_ASSETS_LIST,
} from '@/api/hooks/usePulse/__handlers__/pulse-mocks.ts'
import ManagedAssetSelect from '@/modules/Pulse/components/assets/ManagedAssetSelect.tsx'

describe('ManagedAssetSelect', () => {
  beforeEach(() => {
    cy.viewport(1000, 600)
    cy.intercept('/api/v1/frontend/capabilities', { items: [MOCK_CAPABILITY_PULSE_ASSETS] })
    cy.intercept('/api/v1/management/pulse/managed-assets', MOCK_PULSE_EXT_ASSETS_LIST)
  })

  it('should render properly', () => {
    const onChange = cy.stub().as('onChange')
    cy.mountWithProviders(<ManagedAssetSelect mappings={[]} onChange={onChange} />)

    cy.get('#react-select-asset-placeholder').should('have.text', 'Select an asset to map...')
    cy.get('#combiner-asset-select').click()
    cy.get('[role="listbox"] [role="option"]').should('have.length', 2)
    cy.get('[role="listbox"] [role="option"]')
      .eq(0)
      .within(() => {
        cy.getByTestId('combiner-asset-name').should('have.text', 'Test asset unmapped')
        cy.getByTestId('combiner-asset-status').should('have.text', 'UNMAPPED')
        cy.getByTestId('combiner-asset-description').should('have.text', 'The short description of the asset')
      })

    cy.get('[role="listbox"] [role="option"]')
      .eq(1)
      .within(() => {
        cy.getByTestId('combiner-asset-name').should('have.text', 'Test other asset unmapped')
        cy.getByTestId('combiner-asset-status').should('have.text', 'UNMAPPED')
        cy.getByTestId('combiner-asset-description').should('have.text', 'The short description of the asset')
      })

    cy.getByTestId('combiner-mappings-add-asset').should('be.disabled')
    cy.get('[role="listbox"] [role="option"]').eq(1).click()

    cy.get('[role="listbox"] [role="option"]').should('not.exist')
    cy.getByTestId('combiner-asset-selected-value').should('have.text', 'Test other asset unmapped')

    cy.get('@onChange').should('not.have.been.called')
    cy.getByTestId('combiner-mappings-add-asset').should('not.be.disabled')
    cy.getByTestId('combiner-mappings-add-asset').click()
    cy.get('@onChange').should('have.been.calledWith', MOCK_PULSE_EXT_ASSETS_LIST.items[1])

    cy.get('#combiner-asset-select').click()
    cy.get('[role="listbox"] [role="option"]').eq(1).click()

    cy.get('#combiner-asset-select [role="button"][aria-label="Clear selected options"]').click()
    cy.getByTestId('combiner-mappings-add-asset').should('be.disabled')
    cy.get('#react-select-asset-placeholder').should('have.text', 'Select an asset to map...')
    cy.getByTestId('combiner-asset-selected-value').should('not.exist')
  })

  it('should render mapped assets properly', () => {
    cy.mountWithProviders(
      <ManagedAssetSelect
        mappings={[
          {
            id: MOCK_PULSE_EXT_ASSET_UNMAPPED_2.id,
            sources: {
              primary: { id: '', type: DataIdentifierReference.type.TAG },
              tags: ['my/tag/t1', 'my/tag/t3'],
              topicFilters: ['my/topic/+/temp'],
            },
            destination: {
              assetId: MOCK_PULSE_EXT_ASSET_UNMAPPED_2.id,
              topic: MOCK_PULSE_EXT_ASSET_UNMAPPED_2.topic,
              schema: MOCK_PULSE_EXT_ASSET_UNMAPPED_2.schema,
            },
            instructions: [],
          },
        ]}
        onChange={cy.stub}
      />
    )

    cy.get('#combiner-asset-select').click()
    cy.get('[role="listbox"] [role="option"]').should('have.length', 2)
    cy.get('[role="listbox"] [role="option"]').eq(1).should('have.attr', 'aria-disabled', 'true')
  })

  it('should be accessible', () => {
    const onChange = cy.stub()
    cy.injectAxe()
    cy.mountWithProviders(<ManagedAssetSelect mappings={[]} onChange={onChange} />)
    cy.get('#combiner-asset-select').click()
    cy.checkAccessibility(undefined, {
      rules: {
        // TODO[NVL] ReactSelect not tagging properly the listbox
        'aria-input-field-name': { enabled: false },
      },
    })
  })
})
