import { WrapperTestRoute } from '@/__test-utils__/hooks/WrapperTestRoute.tsx'
import type { Combiner } from '@/api/__generated__'
import { MOCK_COMBINER_ASSET } from '@/api/hooks/useCombiners/__handlers__'
import { MOCK_CAPABILITY_PULSE_ASSETS } from '@/api/hooks/useFrontendServices/__handlers__'
import { mockBridge } from '@/api/hooks/useGetBridges/__handlers__'
import { mockAdapter } from '@/api/hooks/useProtocolAdapters/__handlers__'
import { MOCK_PULSE_ASSET, MOCK_PULSE_ASSET_LIST } from '@/api/hooks/usePulse/__handlers__'
import AssetMapperWizard from '@/modules/Pulse/components/assets/AssetMapperWizard.tsx'

describe('AssetMapperWizard', () => {
  beforeEach(() => {
    cy.viewport(800, 600)
    cy.intercept('/api/v1/frontend/capabilities', { items: [MOCK_CAPABILITY_PULSE_ASSETS] })
    cy.intercept('/api/v1/management/bridges', { items: [mockBridge] })

    cy.intercept('/api/v1/management/protocol-adapters/adapters', { items: [mockAdapter] })
  })

  it('should handle errors', () => {
    cy.mountWithProviders(<AssetMapperWizard assetId="fake-id" isOpen onClose={cy.stub} onSubmit={cy.stub} />)

    cy.get('[role="alert"]')
      .should('have.attr', 'data-status', 'error')
      .should('contain.text', 'Asset fake-id cannot be found')
  })

  it('should render properly', () => {
    const onClose = cy.stub().as('onClose')
    const onSubmit = cy.stub().as('onSubmit')

    cy.intercept('/api/v1/management/pulse/managed-assets', MOCK_PULSE_ASSET_LIST).as('getAssets')
    cy.intercept('GET', '/api/v1/management/pulse/asset-mappers', { items: [MOCK_COMBINER_ASSET] })

    cy.mountWithProviders(
      <AssetMapperWizard assetId={MOCK_PULSE_ASSET.id} isOpen onClose={onClose} onSubmit={onSubmit} />,
      {
        wrapper: WrapperTestRoute,
        routerProps: { initialEntries: [`/pulse-assets`] },
      }
    )

    cy.wait('@getAssets')
    cy.get('@onSubmit').should('not.have.been.called')
    cy.get('@onClose').should('not.have.been.called')
    cy.getByTestId('test-pathname').should('have.text', '/pulse-assets')

    cy.get('header').should('have.text', 'Mapping asset "Test asset"')
    cy.getByTestId('wizard-mapper-instruction').should(
      'have.text',
      'To map an asset, you first have to select an existing mapper or create a new one'
    )

    cy.getByTestId('wizard-mapper-selector-container').within(() => {
      cy.get("label[for='asset-mapper']").should('contain.text', 'Asset mappers')
      cy.get("label[for='asset-mapper']").within(() => {
        cy.getByTestId('more-info-trigger').click()
        cy.get('[data-testid="more-info-popover"] p').should(
          'have.text',
          'Asset mappers group selected sources of data together, to restrict access to integration points (tags and topic filters) and to enable the destination schema from Pulse.'
        )
        cy.get('[data-testid="more-info-popover"] a').should(
          'have.attr',
          'href',
          'https://docs.hivemq.com/hivemq-edge/pulse-asset-mapping.html'
        )
        cy.getByTestId('more-info-trigger').click()
        cy.get('[data-testid="more-info-popover"]').should('not.be.visible')
      })

      cy.get('#wizard-mapper-selector').within(() => {
        cy.getByTestId('wizard-mapper-selector-leftAdd').should('be.visible')
        cy.get('#react-select-mapper-placeholder').should('have.text', 'Type or select ...')
        cy.get('#react-select-mapper-value').should('not.exist')
      })
    })

    cy.getByTestId('wizard-mapper-selector-container').click()
    cy.get('#react-select-mapper-listbox [role="listbox"]').within(() => {
      cy.get('[role="option"]').should('have.length', 1)
      cy.get('[role="option"]').within(() => {
        cy.getByTestId('node-name').should('have.text', 'my-combiner-for-asset')
        cy.getByTestId('node-description').should(
          'have.text',
          "This is a description for the asset mapper 'my-combiner-for-asset'"
        )
        const sourceIds = ['my-adapter', 'the Pulse Agent']
        cy.getByTestId('node-source-id').should('have.length', sourceIds.length)
        cy.getByTestId('node-source-id').each(($sourceId, idx) => {
          cy.wrap($sourceId).should('have.text', sourceIds[idx])
        })

        cy.getByTestId('node-source-id').eq(0).click()
      })
    })

    cy.get('#wizard-mapper-selector').within(() => {
      cy.get('#react-select-mapper-placeholder').should('not.exist')
      cy.get('#react-select-mapper-value').should('have.text', 'my-combiner-for-asset')
    })

    cy.get('footer button').should('have.text', 'Edit the asset mapper')
    cy.get('footer button').click()

    cy.get('@onSubmit').should(
      'have.been.calledWith',
      Cypress.sinon.match((arg) => {
        const mapper = arg as Combiner
        return mapper.mappings.items.length === 2
      })
    )

    cy.getByTestId('wizard-mapper-selector-container').click()
    cy.getByTestId('wizard-mapper-selector-container').type('12343')
    cy.get('#react-select-mapper-listbox [role="listbox"]').within(() => {
      cy.get('[role="option"]').should('have.length', 1)
      cy.get('[role="option"]').should('have.text', 'Create a new mapper: 12343')
      cy.get('[role="option"]').click()
    })

    cy.get('#wizard-mapper-selector').within(() => {
      cy.get('#react-select-mapper-placeholder').should('not.exist')
      cy.get('#react-select-mapper-value').should('have.text', '12343 (new)')
    })

    cy.get('footer button').should('have.text', 'Create a new asset mapper')
    cy.get('footer button').click()

    // This is a hack but will do for the time being
    cy.getByAriaLabel('Clear selected options').eq(1).click()

    cy.get('@onSubmit').should('have.been.called').its('lastCall.args.1').should('be.true')
    cy.get('@onSubmit')
      .should('have.been.called')
      .its('lastCall.args.0')
      .should('deep.include', {
        name: '12343 (new)',
        sources: {
          items: [
            {
              id: 'edge',
              type: 'EDGE_BROKER',
            },
            {
              id: 'idPulse',
              type: 'PULSE_AGENT',
            },
          ],
        },
      })
  })

  it('should be accessible', () => {
    cy.intercept('/api/v1/management/pulse/managed-assets', MOCK_PULSE_ASSET_LIST)
    cy.intercept('GET', '/api/v1/management/pulse/asset-mappers', { items: [MOCK_COMBINER_ASSET] })

    cy.injectAxe()
    cy.mountWithProviders(<AssetMapperWizard assetId={MOCK_PULSE_ASSET.id} isOpen onClose={cy.stub} />)
    cy.getByTestId('wizard-mapper-selector-container').click()
    cy.checkAccessibility(undefined, {
      rules: {
        // TODO[NVL] ReactSelect not tagging properly the listbox
        'aria-input-field-name': { enabled: false },
      },
    })
  })
})
