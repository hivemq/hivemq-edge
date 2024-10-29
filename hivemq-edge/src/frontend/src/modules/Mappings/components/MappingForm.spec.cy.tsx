/// <reference types="cypress" />

import { mockAdapter_OPCUA, mockProtocolAdapter_OPCUA } from '@/api/hooks/useProtocolAdapters/__handlers__'
import MappingForm from '@/modules/Mappings/components/MappingForm.tsx'
import { MappingType } from '@/modules/Mappings/types.ts'

describe('MappingForm', () => {
  beforeEach(() => {
    cy.viewport(800, 800)
    cy.intercept('/api/v1/management/protocol-adapters/types', { items: [mockProtocolAdapter_OPCUA] }).as('getProtocol')
    cy.intercept('/api/v1/management/protocol-adapters/adapters', { items: [mockAdapter_OPCUA] }).as('getAdapter')
    cy.intercept('/api/v1/management/bridges', { items: [] })
    cy.intercept('http://json-schema.org/draft/2020-12/schema', { statusCode: 404 })
  })

  it('should render properly', () => {
    cy.mountWithProviders(
      <MappingForm adapterId={mockAdapter_OPCUA.id} type={MappingType.OUTWARD} onSubmit={cy.stub} />,
      {
        routerProps: { initialEntries: [`/node/wrong-adapter`] },
      }
    )

    cy.get('table').should('have.attr', 'aria-label', 'List of mappings')
    cy.get('table thead tr th').should('have.length', 3)
    cy.get('table thead tr th').eq(0).should('have.text', 'Sources')
    cy.get('table thead tr th').eq(1).should('have.text', 'Destination')
    cy.get('table thead tr th').eq(2).should('have.text', 'Actions')

    cy.get('table tbody tr td').should('have.length', 1).should('have.attr', 'colspan', 3)
    cy.get('table tbody tr td')
      .eq(0)
      .find('[role="alert"]')
      .should('have.attr', 'data-status', 'info')
      .should('have.text', 'No data received yet.')

    cy.get('table tfoot tr td').should('have.length', 3)
    cy.get('table tfoot tr td').eq(2).find('button').should('have.attr', 'aria-label', 'Add a new mapping')
  })

  it('should create mapping', () => {
    cy.mountWithProviders(
      <MappingForm adapterId={mockAdapter_OPCUA.id} type={MappingType.OUTWARD} onSubmit={cy.stub} />,
      {
        routerProps: { initialEntries: [`/node/wrong-adapter`] },
      }
    )

    cy.get('table').should('have.attr', 'aria-label', 'List of mappings')
    cy.get('table tfoot tr td').eq(2).find('button').as('addMapping')

    cy.get('table tbody tr td').should('have.length', 1).should('have.attr', 'colspan', 3)
    cy.get('@addMapping').click()
    cy.get('table tbody tr td').should('have.length', 3)
    cy.get('table tbody tr td').eq(0).should('have.text', '< unset >')
    cy.get('table tbody tr td').eq(1).should('have.text', '< unset >')
    cy.get('table tbody tr td').eq(2).find('button').as('mappingCTAs')
    cy.get('@mappingCTAs').eq(0).should('have.attr', 'aria-label', 'Edit mapping')
    cy.get('@mappingCTAs').eq(1).should('have.attr', 'aria-label', 'Delete mapping')

    cy.get('form').find('[role="alert"]').should('have.attr', 'data-status', 'error')
    cy.get('form').find('[role="alert"] ul li').should('have.length', 2)
  })

  it('should delete mapping', () => {
    cy.mountWithProviders(
      <MappingForm adapterId={mockAdapter_OPCUA.id} type={MappingType.OUTWARD} onSubmit={cy.stub} />,
      {
        routerProps: { initialEntries: [`/node/wrong-adapter`] },
      }
    )

    cy.get('table').should('have.attr', 'aria-label', 'List of mappings')
    cy.get('table tfoot tr td').eq(2).find('button').as('addMapping')

    cy.get('table tbody tr td').should('have.length', 1)
    cy.get('@addMapping').click()
    cy.get('table tbody tr td').should('have.length', 3)
    cy.get('table tbody tr td').eq(2).find('button').as('mappingCTAs')
    cy.get('@mappingCTAs').eq(1).should('have.attr', 'aria-label', 'Delete mapping')
    cy.get('@mappingCTAs').eq(1).click()
    cy.get('table tbody tr td').should('have.length', 1)
  })

  it('should edit mapping', () => {
    cy.mountWithProviders(
      <MappingForm adapterId={mockAdapter_OPCUA.id} type={MappingType.OUTWARD} onSubmit={cy.stub} />,
      {
        routerProps: { initialEntries: [`/node/wrong-adapter`] },
      }
    )

    cy.get('table').should('have.attr', 'aria-label', 'List of mappings')
    cy.get('table tfoot tr td').eq(2).find('button').as('addMapping')

    cy.get('@addMapping').click()
    cy.get('table tbody tr td').eq(2).find('button').as('mappingCTAs')
    cy.get('@mappingCTAs').eq(0).should('have.attr', 'aria-label', 'Edit mapping')

    cy.get('[role="dialog"]').should('not.exist')
    cy.get('@mappingCTAs').eq(0).click()
    cy.get('[role="dialog"]').should('be.visible')
    cy.get('[role="dialog"]').find('header').should('have.text', 'Mapping Editor')
  })
})
