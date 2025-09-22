import { WrapperTestRoute } from '@/__test-utils__/hooks/WrapperTestRoute.tsx'

import type { Combiner, ManagedAsset } from '@/api/__generated__'
import { AssetMapping } from '@/api/__generated__'
import { MOCK_COMBINER_ASSET } from '@/api/hooks/useCombiners/__handlers__'
import { MOCK_CAPABILITY_PULSE_ASSETS } from '@/api/hooks/useFrontendServices/__handlers__'
import { mockAdapter, mockProtocolAdapter } from '@/api/hooks/useProtocolAdapters/__handlers__'
import {
  MOCK_PULSE_EXT_ASSET_MAPPERS_LIST,
  MOCK_PULSE_EXT_ASSETS_LIST,
} from '@/api/hooks/usePulse/__handlers__/pulse-mocks.ts'
import AssetsTable from '@/modules/Pulse/components/assets/AssetsTable.tsx'
import { NodeTypes, WorkspaceNavigationCommand } from '@/modules/Workspace/types.ts'

const cy_getCell = (row: number, column: number) => cy.get('table tbody tr').eq(row).find('td').eq(column)
const cy_getHeader = (column: number) => cy.get('table thead th').eq(column)

describe('AssetsTable', () => {
  beforeEach(() => {
    cy.viewport(1000, 700)
    cy.intercept('/api/v1/frontend/capabilities', { items: [MOCK_CAPABILITY_PULSE_ASSETS] })
    cy.intercept('GET', '/api/v1/management/pulse/asset-mappers', { items: [MOCK_COMBINER_ASSET] })
  })

  it('should render errors', () => {
    cy.intercept('/api/v1/management/pulse/managed-assets', { statusCode: 404 }).as('getStatus')

    cy.mountWithProviders(<AssetsTable />)

    cy.get('.chakra-skeleton').should('have.length', 18)
    cy.wait('@getStatus')

    cy.get("[role='alert']")
      .should('be.visible')
      .should('have.attr', 'data-status', 'error')
      .should('contain.text', 'Not Found')
      .should('contain.text', 'We cannot load the assets at this time. Please try again later')
  })

  it('should render the empty list', () => {
    cy.intercept('/api/v1/management/pulse/managed-assets', { items: [] }).as('getStatus')

    cy.mountWithProviders(<AssetsTable />)

    cy.get('.chakra-skeleton').should('have.length', 18)

    cy.wait('@getStatus')
    cy.get('table[aria-label="List of Pulse assets"] td[colspan="6"]').within(() => {
      cy.get('[role="alert"]').should('have.text', 'No assets found').should('have.attr', 'data-status', 'info')
    })
  })

  it('should render properly', () => {
    cy.intercept('/api/v1/management/pulse/managed-assets', MOCK_PULSE_EXT_ASSETS_LIST).as('getStatus')
    cy.intercept('GET', '/api/v1/management/pulse/asset-mappers', MOCK_PULSE_EXT_ASSET_MAPPERS_LIST)
    cy.intercept('/api/v1/management/protocol-adapters/types', { items: [mockProtocolAdapter] })
    cy.intercept('api/v1/management/protocol-adapters/adapters', { items: [mockAdapter] })

    cy.mountWithProviders(<AssetsTable />)

    cy.get('.chakra-skeleton').should('have.length', 18)

    cy.wait('@getStatus')

    cy.get('table').should('have.attr', 'aria-label', 'List of Pulse assets')
    cy.get('table thead tr th').should('have.length', 6)
    cy.get('table thead tr th button').eq(0).should('have.text', 'Name')
    cy.get('table thead tr th button').eq(1).should('have.text', 'Description')
    cy.get('table thead tr th button').eq(2).should('contain.text', 'Topic')
    cy.get('table thead tr th button').eq(3).should('contain.text', 'Status')
    cy.get('table thead tr th').eq(4).should('contain.text', 'Sources')
    cy.get('table thead tr th').eq(5).should('have.text', 'Actions')

    cy.getByTestId('table-search-control').find('input').should('have.attr', 'placeholder', 'Search for ...')
    cy.getByTestId('table-filters-clearAll').should('have.text', 'Clear all filters')

    cy_getCell(0, 0).should('have.text', 'Test asset unmapped')
    cy_getCell(1, 1).should('have.text', 'The short description of the asset')
    cy_getCell(2, 2).should('have.text', 'test / topic / 2')
    cy_getCell(3, 3).should('have.text', 'Requires Remapping')
    cy_getCell(0, 4).should('have.text', '< unset >')
    // cy_getCell(3, 4).find('[data-testid="topic-wrapper"]').should('have.length', 2)
    cy_getCell(0, 5).find('button').should('have.attr', 'aria-label', 'Actions')

    cy.get('table tbody tr').should('have.length', 4)
    cy.get('table tbody tr').find('mark').should('have.length', 0)

    cy.getByTestId('table-search-control').within(() => {
      cy.get('input').clear()
      cy.get('input').type('mapped')
    })
    cy.get('table tbody tr').should('have.length', 3)
    cy.get('table tbody tr').find('mark').should('have.length', 3)

    cy.getByTestId('table-search-control').within(() => {
      cy.get('input').clear()
      cy.get('input').type('almost')
    })
    cy.get('table tbody tr').should('have.length', 1)
    cy.get('table tbody tr').find('mark').should('have.length', 0)

    cy.getByTestId('table-filters-clearAll').click()
    cy.get('table tbody tr').should('have.length', 4)
    cy.get('table tbody tr').find('mark').should('have.length', 0)

    cy_getHeader(2).within(() => {
      cy.get('input#topic').type('test/topic/2{enter}')
    })
    cy.get('table tbody tr').should('have.length', 1)
    cy.get('table tbody tr').find('mark').should('have.length', 0)

    cy_getHeader(3).within(() => {
      cy.get('input#mapping_status').type('STREAMING{enter}')
    })
    cy.get('table tbody tr').should('have.length', 1)
    cy.get('table tbody tr').find('mark').should('have.length', 0)

    cy_getHeader(3).within(() => {
      cy.getByAriaLabel('Clear selected options').click()
    })

    cy.get('table tbody tr').should('have.length', 1)
    cy.get('table tbody tr').find('mark').should('have.length', 0)

    cy.getByTestId('table-filters-clearAll').click()
    cy.get('table tbody tr').should('have.length', 4)
    cy.get('table tbody tr').find('mark').should('have.length', 0)
  })

  it('should render the summary version properly', () => {
    cy.intercept('/api/v1/management/pulse/managed-assets', MOCK_PULSE_EXT_ASSETS_LIST).as('getStatus')
    cy.intercept('GET', '/api/v1/management/pulse/asset-mappers', MOCK_PULSE_EXT_ASSET_MAPPERS_LIST)
    cy.mountWithProviders(<AssetsTable variant="summary" />)

    cy.get('.chakra-skeleton').should('have.length', 16)

    cy.wait('@getStatus')

    cy.get('table').should('have.attr', 'aria-label', 'List of Pulse assets')
    cy.get('table thead tr th').should('have.length', 4)
    cy.get('table thead tr th button').eq(0).should('have.text', 'Name')
    cy.get('table thead tr th button').eq(1).should('contain.text', 'Topic')
    cy.get('table thead tr th button').eq(2).should('contain.text', 'Status')
    cy.get('table thead tr th').eq(3).should('have.text', 'Actions')

    cy_getCell(1, 3).find('button').click()
    // This is not a great discriminator
    cy.get("[role='menu']")
      .eq(1)
      .within(() => {
        cy.get('button').should('have.length', 3)
      })
  })

  it('should be accessible', () => {
    cy.intercept('/api/v1/management/pulse/managed-assets', MOCK_PULSE_EXT_ASSETS_LIST).as('getStatus')
    cy.intercept('GET', '/api/v1/management/pulse/asset-mappers', MOCK_PULSE_EXT_ASSET_MAPPERS_LIST)
    cy.intercept('/api/v1/management/protocol-adapters/types', { items: [mockProtocolAdapter] })
    cy.intercept('api/v1/management/protocol-adapters/adapters', { items: [mockAdapter] })

    cy.injectAxe()
    cy.mountWithProviders(<AssetsTable />)
    cy.wait('@getStatus')
    cy.checkAccessibility()
  })

  describe('Action', () => {
    beforeEach(() => {
      cy.intercept('/api/v1/management/pulse/managed-assets', MOCK_PULSE_EXT_ASSETS_LIST).as('getStatus')
      cy.intercept('GET', '/api/v1/management/pulse/asset-mappers', MOCK_PULSE_EXT_ASSET_MAPPERS_LIST)
      cy.intercept('/api/v1/management/protocol-adapters/types', { items: [mockProtocolAdapter] })
      cy.intercept('/api/v1/management/protocol-adapters/adapters', { items: [mockAdapter] })
    })

    it('should handle navigation and initial filters', () => {
      cy.mountWithProviders(<AssetsTable />, {
        wrapper: WrapperTestRoute,
        routerProps: { initialEntries: [`/pulse-assets?mapping_status=${AssetMapping.status.UNMAPPED}`] },
      })

      cy.wait('@getStatus')
      cy.getByTestId('test-pathname').should('have.text', '/pulse-assets')
      cy.getByTestId('test-search').should('have.text', '?mapping_status=UNMAPPED')

      cy.get('table tbody tr').should('have.length', 2)
      cy_getCell(0, 3).should('have.text', 'Unmapped')
      cy_getCell(1, 3).should('have.text', 'Unmapped')
    })

    it('should handle view detail', () => {
      cy.mountWithProviders(<AssetsTable />, {
        wrapper: WrapperTestRoute,
        routerProps: { initialEntries: ['/pulse-assets'] },
      })

      cy.getByTestId('test-pathname').should('have.text', '/pulse-assets')
      cy_getCell(0, 5).find('button').click()
      cy.get("[role='menu']#menu-list-asset-actions button").eq(0).click()

      cy.getByTestId('test-pathname').should('have.text', '/pulse-assets/3b028f58-1111-4de1-9b8b-c1a35b1643a4')
    })

    it('should handle unmapping', () => {
      cy.intercept('PUT', 'api/v1/management/pulse/managed-assets/*', (req) => {
        req.reply({ statusCode: 404 })
      }).as('unmapRequest')

      cy.mountWithProviders(<AssetsTable />, {
        wrapper: WrapperTestRoute,
        routerProps: { initialEntries: ['/pulse-assets'] },
      })

      cy.getByTestId('test-pathname').should('have.text', '/pulse-assets')
      cy_getCell(3, 5).find('button').click()
      cy.get("[role='menu']#menu-list-asset-actions button").eq(2).click()

      cy.get('[role="alertdialog"]').within(() => {
        cy.get('header').should('have.text', 'Unmapping asset "Test asset remapping"')
        cy.getByTestId('confirmation-message').should(
          'have.text',
          'Do you want to remove all mapping instructions from the asset? The action cannot be reversed.'
        )
        cy.get('footer button').eq(1).click()
      })

      cy.get('[role="status"] > div').should('have.attr', 'data-status', 'error')
      cy.get('[role="status"] > div').should(
        'contain.text',
        'There was a problem trying to remove the mapping from the asset' + 'Managed asset not found'
      )

      cy.wait('@unmapRequest').then(({ request }) => {
        const body = request.body as Partial<ManagedAsset>
        expect(body).to.deep.include({
          id: '3b028f58-3333-4de1-9b8b-c1a35b1643a4',
        })
        expect(body.mapping).to.deep.equal({
          status: 'UNMAPPED',
        })
      })
    })

    describe('Mapping', () => {
      it('should handle mapping in an existing mapper', () => {
        cy.intercept('PUT', 'api/v1/management/pulse/managed-assets/*', (req) => {
          req.reply({ statusCode: 404 })
        })

        cy.intercept<Combiner>('PUT', 'api/v1/management/pulse/asset-mappers/*', (req) => {
          const combiner = req.body
          req.reply({ body: { updated: combiner.id }, statusCode: 200 })
        }).as('updateMapperRequest')

        cy.mountWithProviders(<AssetsTable />, {
          wrapper: WrapperTestRoute,
          routerProps: { initialEntries: ['/pulse-assets'] },
        })

        cy.getByTestId('test-pathname').should('have.text', '/pulse-assets')
        cy_getCell(0, 5).find('button').click()
        cy.get("[role='menu']#menu-list-asset-actions button").eq(1).click()

        cy.get('section[role="dialog"][aria-modal="true"]').within(() => {
          cy.get('header').should('have.text', 'Mapping asset "Test asset unmapped"')

          cy.getByTestId('wizard-mapper-selector-container').click()
          cy.getByTestId('node-source-id').eq(0).click()

          cy.get('footer button').should('have.text', 'Edit the asset mapper')
          cy.get('footer button').click()
        })

        cy.wait('@updateMapperRequest').then((e) => {
          expect(e.response?.body).to.deep.equal({ updated: 'e9af7f82-aaaa-4d07-8c0f-e4591148af19' })
        })

        cy.get('[role="status"] > div').should('have.attr', 'data-status', 'success')
        cy.get('[role="status"]').should('contain.text', "We've successfully updated the asset mapper for you.")

        cy.getByTestId('test-pathname').should('have.text', '/workspace')
        cy.getByTestId('test-state')
          .invoke('text')
          .then((text) => {
            const state = JSON.parse(text)
            expect(state).to.have.nested.property('selectedAdapter.adapterId', 'e9af7f82-aaaa-4d07-8c0f-e4591148af19')
            expect(state).to.have.nested.property('selectedAdapter.type', NodeTypes.COMBINER_NODE)
            expect(state).to.have.nested.property('selectedAdapter.command', WorkspaceNavigationCommand.ASSET_MAPPER)
          })
      })

      it.only('should handle mapping in a new mapper', () => {
        cy.intercept('PUT', 'api/v1/management/pulse/managed-assets/*', (req) => {
          req.reply({ statusCode: 404 })
        })

        cy.intercept<Combiner>('POST', 'api/v1/management/pulse/asset-mappers', (req) => {
          req.reply({ body: { created: 'new-UUID' }, statusCode: 200 })
        }).as('updateMapperRequest')

        cy.mountWithProviders(<AssetsTable />, {
          wrapper: WrapperTestRoute,
          routerProps: { initialEntries: ['/pulse-assets'] },
        })

        cy.getByTestId('test-pathname').should('have.text', '/pulse-assets')
        cy_getCell(0, 5).find('button').click()
        cy.get("[role='menu']#menu-list-asset-actions button").eq(1).click()

        cy.get('section[role="dialog"][aria-modal="true"]').within(() => {
          cy.get('header').should('have.text', 'Mapping asset "Test asset unmapped"')

          cy.getByTestId('wizard-mapper-selector-container').type('New mapper{enter}')
          cy.getByTestId('wizard-mapper-selector-instruction').should(
            'have.text',
            'A new asset mapper will be created in the Workspace, with a predefined mapping for this asset'
          )

          cy.get('footer button').should('have.text', 'Create a new asset mapper')
          cy.get('footer button').click()
        })

        cy.wait('@updateMapperRequest').then((e) => {
          expect(e.response?.body).to.deep.equal({ created: 'new-UUID' })
        })

        cy.get('[role="status"] > div').should('have.attr', 'data-status', 'success')
        cy.get('[role="status"]').should('contain.text', "We've successfully created the mapper for you.")
      })
    })
  })
})
