/// <reference types="cypress" />

import CombinerMappingsList from './CombinerMappingsList.tsx'
import type { DataCombining } from '@/api/__generated__'
import { DataIdentifierReference } from '@/api/__generated__'

describe('CombinerMappingsList', () => {
  const createMockMapping = (id: string, overrides?: Partial<DataCombining>): DataCombining => ({
    id,
    sources: {
      primary: { id: `source-${id}`, type: DataIdentifierReference.type.TAG },
    },
    destination: {
      topic: `test/topic/${id}`,
    },
    instructions: [],
    ...overrides,
  })

  it('should render empty state when no mappings provided', () => {
    cy.mountWithProviders(<CombinerMappingsList mappings={[]} />)

    cy.getByTestId('mappings-list-empty').should('be.visible').and('contain.text', 'No mappings defined yet')
  })

  it('should render a single mapping', () => {
    const mapping = createMockMapping('1')

    cy.mountWithProviders(<CombinerMappingsList mappings={[mapping]} />)

    cy.getByTestId('mappings-list').should('be.visible')
    cy.getByTestId('mappings-count-badge').should('contain.text', '1 mapping')
    cy.getByTestId('mapping-item-1').should('be.visible')
    cy.getByTestId('mapping-source-1').should('contain.text', 'source-1')
    cy.getByTestId('mapping-destination-1').should('contain.text', 'test/topic/1')
  })

  it('should render multiple mappings', () => {
    const mappings = [createMockMapping('1'), createMockMapping('2'), createMockMapping('3')]

    cy.mountWithProviders(<CombinerMappingsList mappings={mappings} />)

    cy.getByTestId('mappings-count-badge').should('contain.text', '3 mappings')
    cy.getByTestId('mapping-item-1').should('be.visible')
    cy.getByTestId('mapping-item-2').should('be.visible')
    cy.getByTestId('mapping-item-3').should('be.visible')
  })

  it('should display instruction count when instructions exist', () => {
    const mapping = createMockMapping('1', {
      instructions: [
        {
          source: '$.field1',
          destination: '$.output1',
          sourceRef: { id: 'ref1', type: DataIdentifierReference.type.TAG },
        },
        {
          source: '$.field2',
          destination: '$.output2',
          sourceRef: { id: 'ref2', type: DataIdentifierReference.type.TAG },
        },
      ],
    })

    cy.mountWithProviders(<CombinerMappingsList mappings={[mapping]} />)

    cy.getByTestId('mapping-instructions-1').should('be.visible').and('contain.text', '2 instructions')
  })

  it('should be accessible', () => {
    cy.injectAxe()

    const mappings = [
      createMockMapping('1', {
        sources: { primary: { id: 'temperature-sensor', type: DataIdentifierReference.type.TAG } },
        destination: { topic: 'factory/floor1/temperature' },
        instructions: [
          {
            source: '$.temp',
            destination: '$.temperature',
            sourceRef: { id: 'temp-ref', type: DataIdentifierReference.type.TAG },
          },
          {
            source: '$.unit',
            destination: '$.unit',
            sourceRef: { id: 'unit-ref', type: DataIdentifierReference.type.TAG },
          },
        ],
      }),
      createMockMapping('2', {
        sources: { primary: { id: 'pressure-sensor', type: DataIdentifierReference.type.TAG } },
        destination: { topic: 'factory/floor1/pressure' },
        instructions: [
          {
            source: '$.pressure',
            destination: '$.value',
            sourceRef: { id: 'pressure-ref', type: DataIdentifierReference.type.TAG },
          },
        ],
      }),
      createMockMapping('3', {
        sources: { primary: { id: 'mqtt-bridge', type: DataIdentifierReference.type.TOPIC_FILTER } },
        destination: { assetId: 'asset-manufacturing-line' },
        instructions: [],
      }),
      createMockMapping('4', {
        sources: { primary: { id: 'humidity-sensor', type: DataIdentifierReference.type.TAG } },
        destination: { topic: 'factory/floor1/humidity' },
        instructions: [
          {
            source: '$.humidity',
            destination: '$.value',
            sourceRef: { id: 'humidity-ref', type: DataIdentifierReference.type.TAG },
          },
        ],
      }),
      createMockMapping('5', {
        sources: { primary: { id: 'vibration-sensor', type: DataIdentifierReference.type.TAG } },
        destination: { topic: 'factory/floor1/vibration' },
        instructions: [],
      }),
      createMockMapping('6', {
        sources: { primary: { id: 'power-meter', type: DataIdentifierReference.type.TAG } },
        destination: { topic: 'factory/floor1/power' },
        instructions: [
          {
            source: '$.power',
            destination: '$.value',
            sourceRef: { id: 'power-ref', type: DataIdentifierReference.type.TAG },
          },
        ],
      }),
    ]

    cy.mountWithProviders(<CombinerMappingsList mappings={mappings} />)

    // Check accessibility - should pass scrollable-region-focusable rule with tabIndex={0}
    cy.checkAccessibility()
  })
})
