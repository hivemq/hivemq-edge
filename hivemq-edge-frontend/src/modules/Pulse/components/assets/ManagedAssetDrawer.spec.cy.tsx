/// <reference types="cypress" />

import type { FC, PropsWithChildren } from 'react'
import { Route, Routes } from 'react-router-dom'

import { WrapperTestRoute } from '@/__test-utils__/hooks/WrapperTestRoute.tsx'
import { MOCK_PULSE_ASSET_LIST } from '@/api/hooks/usePulse/__handlers__'
import ManagedAssetDrawer from '@/modules/Pulse/components/assets/ManagedAssetDrawer.tsx'

const wrapper: FC<PropsWithChildren> = ({ children }) => (
  <WrapperTestRoute>
    <Routes>
      <Route path="/pulse-assets" element={<div>Home</div>}></Route>
      <Route path="/pulse-assets/:assetId" element={children}></Route>
    </Routes>
  </WrapperTestRoute>
)

describe('ManagedAssetDrawer', () => {
  beforeEach(() => {
    cy.viewport(800, 800)
  })

  it('should handle errors', () => {
    const assetId = '3b028f58-f949-4de1-xxxxxxx-c1a35b1643a4'
    cy.intercept('/api/v1/management/pulse/managed-assets', { statusCode: 404 }).as('getStatus')
    cy.mountWithProviders(<ManagedAssetDrawer />, {
      wrapper,
      routerProps: { initialEntries: [`/pulse-assets/${assetId}`] },
    })

    cy.getByTestId('test-pathname').should('have.text', `/pulse-assets/${assetId}`)
    cy.wait('@getStatus')
    cy.getByTestId('test-pathname').should('have.text', '/pulse-assets')
    cy.get("[role='status'] > [data-status='error']").within(() => {
      cy.get(`#toast-${assetId}-title`).should('have.text', 'Asset information')
      cy.get(`#toast-${assetId}-description`).should('have.text', 'There was a problem loading the asset: Not Found')
    })
  })

  it('should handle errors', () => {
    const assetId = '3b028f58-f949-4de1-xxxxxxx-c1a35b1643a4'
    cy.intercept('/api/v1/management/pulse/managed-assets', MOCK_PULSE_ASSET_LIST).as('getStatus')
    cy.mountWithProviders(<ManagedAssetDrawer />, {
      wrapper,
      routerProps: { initialEntries: [`/pulse-assets/${assetId}`] },
    })

    cy.getByTestId('test-pathname').should('have.text', `/pulse-assets/${assetId}`)
    cy.wait('@getStatus')
    cy.getByTestId('test-pathname').should('have.text', '/pulse-assets')
    cy.get("[role='status'] > [data-status='error']").within(() => {
      cy.get(`#toast-${assetId}-title`).should('have.text', 'Asset information')
      cy.get(`#toast-${assetId}-description`).should(
        'have.text',
        `There was a problem loading the asset: Asset ${assetId} cannot be found`
      )
    })
  })

  it('should render properly', () => {
    const assetId = '3b028f58-f949-4de1-9b8b-c1a35b1643a5'
    cy.intercept('/api/v1/management/pulse/managed-assets', MOCK_PULSE_ASSET_LIST).as('getStatus')

    cy.mountWithProviders(<ManagedAssetDrawer />, {
      wrapper,
      routerProps: { initialEntries: [`/pulse-assets/${assetId}`] },
    })

    cy.wait('@getStatus')
    cy.get("[role='dialog']").should('be.visible')
    cy.get("[role='dialog']").within(() => {
      cy.get('header').should('have.text', 'Pulse Agent Overview')
      cy.getByAriaLabel('Expand').should('be.visible').should('have.attr', 'data-expanded', 'false')
      cy.get('#asset-editor').within(() => {
        cy.get('[role="tablist"] [role="tab"]').should('have.length', 3)
        cy.get('[role="tablist"] [role="tab"]').eq(0).should('have.text', 'Configuration')
        cy.get('[role="tablist"] [role="tab"]').eq(1).should('have.text', 'Destination')
        cy.get('[role="tablist"] [role="tab"]').eq(2).should('have.text', 'Mapping')

        cy.getByTestId('root_id').within(() => {
          cy.get('input').should('have.value', assetId)
        })

        cy.getByTestId('root_name').within(() => {
          cy.get('input').should('have.value', 'Test mapped asset')
        })

        cy.getByTestId('root_description').within(() => {
          cy.get('input').should('have.value', 'The short description of the mapped asset')
        })

        cy.get('[role="tablist"] [role="tab"]').eq(1).click()

        cy.getByTestId('root_topic').within(() => {
          cy.get('#root_topic > div')
            .eq(0)
            .should('have.attr', 'data-disabled', 'true')
            .should('have.text', 'test/topic/2')
        })

        cy.getByTestId('root_schema').within(() => {
          cy.get('label[for="root_schema"]').should('have.attr', 'data-disabled')
          cy.get('h3').should('have.text', 'CustomStruct: ns=3;s=TE_"User_data_type_6"')
          cy.get('[role="list"] li').should('have.length', 1)
          cy.get('[role="list"] li')
            .eq(0)
            .within(() => {
              cy.getByTestId('property-type').should('have.attr', 'aria-label', 'String')
              cy.getByTestId('property-name').should('have.attr', 'aria-label', 'value').should('have.text', 'value')
            })
        })

        cy.get('[role="tablist"] [role="tab"]').eq(2).click()

        cy.getByTestId('root_mapping').within(() => {
          cy.get('h2').should('have.text', 'Mapping')
        })

        cy.getByTestId('root_mapping_status').within(() => {
          cy.get('label').should('have.text', 'Mapping status*')
          cy.get('label + div').should('have.text', 'STREAMING')
        })

        cy.getByTestId('root_mapping_mappingId').within(() => {
          cy.get('label').should('have.text', 'Mapping ID')
          cy.get('input').should('have.value', 'ff02efff-7b4c-4f8c-8bf6-74d0756283fb')
        })
      })
    })

    cy.get("[role='dialog']").within(() => {
      cy.getByAriaLabel('Close').click()
    })

    cy.getByTestId('test-pathname').should('have.text', `/pulse-assets`)
  })

  it('should be accessible', () => {
    cy.injectAxe()
    const assetId = '3b028f58-f949-4de1-9b8b-c1a35b1643a5'
    cy.intercept('/api/v1/management/pulse/managed-assets', MOCK_PULSE_ASSET_LIST).as('getStatus')

    cy.mountWithProviders(<ManagedAssetDrawer />, {
      wrapper,
      routerProps: { initialEntries: [`/pulse-assets/${assetId}`] },
    })

    cy.wait('@getStatus')
    cy.get("[role='dialog']").should('be.visible')
    cy.checkAccessibility()
    cy.get('[role="tablist"] [role="tab"]').eq(1).click()
    cy.checkAccessibility()
    cy.get('[role="tablist"] [role="tab"]').eq(2).click()
    cy.checkAccessibility()
  })
})
