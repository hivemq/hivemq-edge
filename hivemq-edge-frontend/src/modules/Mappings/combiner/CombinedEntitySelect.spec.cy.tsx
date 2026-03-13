import type { FC, PropsWithChildren } from 'react'
import { Box, FormControl, FormLabel } from '@chakra-ui/react'
import type { MultiValue } from 'chakra-react-select'
import type { UseQueryResult } from '@tanstack/react-query'

import { MockAdapterType } from '@/__test-utils__/adapters/types'
import { useGetCombinedEntities } from '@/api/hooks/useDomainModel/useGetCombinedEntities'
import { mockCombiner } from '@/api/hooks/useCombiners/__handlers__'
import { MOCK_DEVICE_TAGS } from '@/api/hooks/useProtocolAdapters/__handlers__'
import type { DomainTag, DomainTagList, TopicFilter, TopicFilterList } from '@/api/__generated__'
import { DataIdentifierReference, EntityType } from '@/api/__generated__'
import type { CombinerContext, SelectedSources } from '@/modules/Mappings/types'

import CombinedEntitySelect from './CombinedEntitySelect'

const mockTagQuery = (tags: DomainTag[]): UseQueryResult<DomainTagList, Error> =>
  ({
    data: { items: tags },
    isLoading: false,
    isSuccess: true,
    isError: false,
  }) as UseQueryResult<DomainTagList, Error>

interface EntityReferenceSelectProps {
  selectedSources?: SelectedSources
  onChange: (value: MultiValue<unknown>) => void
}

const CombinedEntitySelectWrapper: FC<PropsWithChildren<EntityReferenceSelectProps>> = ({
  children,
  selectedSources,
  onChange,
}) => {
  const sources = useGetCombinedEntities(mockCombiner.sources.items)

  // Build entityQueries for the new API
  const entityQueries = mockCombiner.sources.items.map((entity, index) => ({
    entity,
    query: sources[index],
  }))

  return (
    <Box>
      <FormControl>
        <FormLabel htmlFor="my-id" id="my-label-id">
          Select many
        </FormLabel>
        <CombinedEntitySelect
          id="my-id"
          onChange={onChange}
          formContext={{
            selectedSources,
            entityQueries,
            // Backward compatibility
            queries: sources,
            entities: mockCombiner.sources.items,
          }}
          maxW="75vw"
        />
      </FormControl>
      {children}
    </Box>
  )
}

describe('CombinedEntitySelect', () => {
  beforeEach(() => {
    cy.viewport(800, 800)

    cy.intercept('/api/v1/management/protocol-adapters/adapters/my-adapter/tags', {
      items: MOCK_DEVICE_TAGS('my-adapter', MockAdapterType.OPC_UA),
    })
    cy.intercept('/api/v1/management/protocol-adapters/adapters/my-other-adapter/tags', {
      items: MOCK_DEVICE_TAGS('my-other-adapter', MockAdapterType.OPC_UA),
    })
  })

  it('should render properly', () => {
    const onChange = cy.stub().as('onChange')

    const selectedSources: SelectedSources = {
      tags: [{ id: 'opcua-1/power/off', type: DataIdentifierReference.type.TAG, scope: null }],
      topicFilters: [{ id: 'topicFilter/t3', type: DataIdentifierReference.type.TOPIC_FILTER, scope: null }],
    }

    cy.mountWithProviders(<CombinedEntitySelectWrapper selectedSources={selectedSources} onChange={onChange} />)

    cy.get('#combiner-entity-select')
      .should('contain.text', 'opcua-1/power/off')
      .should('contain.text', 'topicFilter/t3')
    cy.get('#combiner-entity-select').realClick()
    cy.get('#combiner-entity-select').type('m')
    cy.get('#react-select-entity-listbox').find('[role="option"]').as('options')
    cy.get('@options').should('have.length', 4)
    cy.get('@options')
      .filter(':contains("my-adapter/power/off")')
      .should('have.length', 1)
      .should('contain.text', 'Tag')
  })

  it('should handle duplicate tag names from different adapters', () => {
    // Two adapters with tags having the same name - use deprecated tags prop for now
    const contextWithDuplicates: CombinerContext = {
      entityQueries: [
        {
          entity: { id: 'modbus-adapter', type: EntityType.ADAPTER },
          query: mockTagQuery([
            { name: 'temperature', description: 'Modbus temperature sensor' } as DomainTag,
            { name: 'pressure', description: 'Modbus pressure sensor' } as DomainTag,
          ]) as UseQueryResult<DomainTagList, Error>,
        },
        {
          entity: { id: 'opcua-adapter', type: EntityType.ADAPTER },
          query: mockTagQuery([
            { name: 'temperature', description: 'OPC-UA temperature sensor' } as DomainTag,
            { name: 'humidity', description: 'OPC-UA humidity sensor' } as DomainTag,
          ]) as UseQueryResult<DomainTagList, Error>,
        },
      ],
    }

    const onChange = cy.stub().as('onChange')

    cy.mountWithProviders(
      <Box>
        <FormControl>
          <FormLabel htmlFor="duplicate-test">Select tags</FormLabel>
          <CombinedEntitySelect id="duplicate-test" formContext={contextWithDuplicates} onChange={onChange} />
        </FormControl>
      </Box>
    )

    // Open the select
    cy.get('#combiner-entity-select').click()

    // ✅ CRITICAL TEST: Should have 4 options total (not 3!)
    // Without the fix, the second "temperature" would be deduplicated
    cy.get('#react-select-entity-listbox').find('[role="option"]').as('options')
    cy.get('@options').should('have.length', 4)

    // ✅ Verify both temperature tags are present in the list
    cy.get('@options').filter(':contains("temperature")').should('have.length', 2)

    // ✅ Verify one is from modbus, one from opcua (by description)
    cy.get('@options').filter(':contains("Modbus temperature")').should('have.length', 1)
    cy.get('@options').filter(':contains("OPC-UA temperature")').should('have.length', 1)

    // ✅ Verify onClick structure - select first temperature tag
    cy.get('@options').filter(':contains("Modbus temperature")').click()
    cy.get('@onChange').should('have.been.calledOnce')
    cy.get('@onChange').its('firstCall.args.0.0').should('deep.include', {
      value: 'temperature',
      adapterId: 'modbus-adapter',
    })
  })

  describe('chip ownership display', () => {
    it('should show ownership string in chip when scope is set', () => {
      const context: CombinerContext = {
        selectedSources: {
          tags: [{ id: 'temperature', type: DataIdentifierReference.type.TAG, scope: 'opcua-adapter' }],
          topicFilters: [],
        },
      }

      cy.mountWithProviders(
        <Box>
          <FormControl>
            <FormLabel htmlFor="chip-test">Sources</FormLabel>
            <CombinedEntitySelect id="chip-test" formContext={context} onChange={cy.stub()} />
          </FormControl>
        </Box>
      )

      cy.getByTestId('topic-wrapper').should('have.length', 1)
      cy.getByTestId('topic-wrapper').should('contain.text', 'opcua-adapter :: temperature')
    })

    it('should show plain tag name in chip when scope is null', () => {
      const context: CombinerContext = {
        selectedSources: {
          tags: [{ id: 'temperature', type: DataIdentifierReference.type.TAG, scope: null }],
          topicFilters: [],
        },
      }

      cy.mountWithProviders(
        <Box>
          <FormControl>
            <FormLabel htmlFor="chip-test-no-scope">Sources</FormLabel>
            <CombinedEntitySelect id="chip-test-no-scope" formContext={context} onChange={cy.stub()} />
          </FormControl>
        </Box>
      )

      cy.getByTestId('topic-wrapper').should('have.length', 1)
      cy.getByTestId('topic-wrapper').should('contain.text', 'temperature')
      cy.getByTestId('topic-wrapper').should('not.contain.text', '::')
    })

    it('should show distinct chips when two adapters share the same tag name', () => {
      const context: CombinerContext = {
        selectedSources: {
          tags: [
            { id: 'temperature', type: DataIdentifierReference.type.TAG, scope: 'opcua-adapter' },
            { id: 'temperature', type: DataIdentifierReference.type.TAG, scope: 'modbus-adapter' },
          ],
          topicFilters: [],
        },
      }

      cy.mountWithProviders(
        <Box>
          <FormControl>
            <FormLabel htmlFor="chip-test-two-adapters">Sources</FormLabel>
            <CombinedEntitySelect id="chip-test-two-adapters" formContext={context} onChange={cy.stub()} />
          </FormControl>
        </Box>
      )

      cy.getByTestId('topic-wrapper').should('have.length', 2)
      cy.getByTestId('topic-wrapper').eq(0).should('contain.text', 'opcua-adapter :: temperature')
      cy.getByTestId('topic-wrapper').eq(1).should('contain.text', 'modbus-adapter :: temperature')
    })
  })

  describe('option list ownership display', () => {
    const contextWithDuplicates: CombinerContext = {
      entityQueries: [
        {
          entity: { id: 'modbus-adapter', type: EntityType.ADAPTER },
          query: mockTagQuery([
            { name: 'temperature', description: 'Modbus temperature sensor' } as DomainTag,
            { name: 'pressure', description: 'Modbus pressure sensor' } as DomainTag,
          ]),
        },
        {
          entity: { id: 'opcua-adapter', type: EntityType.ADAPTER },
          query: mockTagQuery([
            { name: 'temperature', description: 'OPC-UA temperature sensor' } as DomainTag,
            { name: 'humidity', description: 'OPC-UA humidity sensor' } as DomainTag,
          ]),
        },
      ],
    }

    it('should show ownership string (adapterId :: tag) in each tag option row', () => {
      cy.mountWithProviders(
        <Box>
          <FormControl>
            <FormLabel htmlFor="option-ownership-test">Sources</FormLabel>
            <CombinedEntitySelect id="option-ownership-test" formContext={contextWithDuplicates} onChange={cy.stub()} />
          </FormControl>
        </Box>
      )

      cy.get('#combiner-entity-select').click()
      cy.get('#react-select-entity-listbox').find('[role="option"]').as('options')

      // Each tag option now shows "adapterId :: tagName" ownership format
      cy.get('@options').filter(':contains("modbus-adapter :: temperature")').should('have.length', 1)
      cy.get('@options').filter(':contains("modbus-adapter :: pressure")').should('have.length', 1)
      cy.get('@options').filter(':contains("opcua-adapter :: temperature")').should('have.length', 1)
      cy.get('@options').filter(':contains("opcua-adapter :: humidity")').should('have.length', 1)
    })

    it('should sort options by tag name so same-named tags from different adapters are adjacent', () => {
      cy.mountWithProviders(
        <Box>
          <FormControl>
            <FormLabel htmlFor="option-sort-test">Sources</FormLabel>
            <CombinedEntitySelect id="option-sort-test" formContext={contextWithDuplicates} onChange={cy.stub()} />
          </FormControl>
        </Box>
      )

      cy.get('#combiner-entity-select').click()
      cy.get('#react-select-entity-listbox').find('[role="option"]').as('options')

      // Alphabetical by tag name: humidity, pressure, temperature × 2
      cy.get('@options').should('have.length', 4)
      cy.get('@options').eq(0).should('contain.text', 'humidity')
      cy.get('@options').eq(1).should('contain.text', 'pressure')
      // both temperature entries are adjacent (eq 2 and eq 3)
      cy.get('@options').eq(2).should('contain.text', 'temperature')
      cy.get('@options').eq(3).should('contain.text', 'temperature')
    })

    it('should filter options by adapter name when typed in search', () => {
      cy.mountWithProviders(
        <Box>
          <FormControl>
            <FormLabel htmlFor="option-filter-test">Sources</FormLabel>
            <CombinedEntitySelect id="option-filter-test" formContext={contextWithDuplicates} onChange={cy.stub()} />
          </FormControl>
        </Box>
      )

      cy.get('#option-filter-test').type('opcua')
      cy.get('#react-select-entity-listbox').find('[role="option"]').as('options')

      cy.get('@options').should('have.length', 2)
      cy.get('@options').each(($el) => {
        cy.wrap($el).should('contain.text', 'opcua-adapter')
      })
    })
  })

  it.skip('screenshot: sources dropdown — before/after', () => {
    const mockTopicFilterQuery = (filters: TopicFilter[]): UseQueryResult<TopicFilterList, Error> =>
      ({
        data: { items: filters },
        isLoading: false,
        isSuccess: true,
        isError: false,
      }) as UseQueryResult<TopicFilterList, Error>

    // Two tags are pre-selected (shown as chips, hidden from dropdown).
    // Remaining options cover a mix of tags and topic filters, including a "temp" family:
    //   temp/raw (tag, modbus), temporal/value (tag, opcua), temp/+/readings (topic filter)
    // to illustrate how the adapter scope helps distinguish same-prefix entries.
    const contextForScreenshot: CombinerContext = {
      entityQueries: [
        {
          entity: { id: 'modbus-adapter', type: EntityType.ADAPTER },
          query: mockTagQuery([
            { name: 'temperature', description: 'Modbus temperature sensor reading' } as DomainTag, // selected → hidden
            { name: 'temp/raw', description: 'Raw temperature before calibration' } as DomainTag,
            { name: 'pressure', description: 'Pressure sensor in bar' } as DomainTag,
          ]),
        },
        {
          entity: { id: 'opcua-adapter', type: EntityType.ADAPTER },
          query: mockTagQuery([
            { name: 'temporal/value', description: 'Temporal shift value (OPC-UA node)' } as DomainTag,
            { name: 'humidity', description: 'Humidity level sensor' } as DomainTag,
          ]),
        },
        {
          entity: { id: 'broker', type: EntityType.EDGE_BROKER },
          query: mockTopicFilterQuery([
            { topicFilter: 'factory/+/alerts', description: 'Factory alert events' } as TopicFilter, // selected → hidden
            { topicFilter: 'temp/+/readings', description: 'Live temperature readings stream' } as TopicFilter,
            { topicFilter: 'factory/+/reports', description: 'Factory daily summary reports' } as TopicFilter,
          ]) as UseQueryResult<DomainTagList | TopicFilterList, Error>,
        },
      ],
      selectedSources: {
        tags: [{ id: 'temperature', type: DataIdentifierReference.type.TAG, scope: 'modbus-adapter' }],
        topicFilters: [{ id: 'factory/+/alerts', type: DataIdentifierReference.type.TOPIC_FILTER, scope: null }],
      },
    }

    cy.viewport(700, 650)
    cy.mountWithProviders(
      <Box p={6} maxW="500px">
        <FormControl>
          <FormLabel htmlFor="screenshot-test">Sources</FormLabel>
          <CombinedEntitySelect id="screenshot-test" formContext={contextForScreenshot} onChange={cy.stub()} />
        </FormControl>
      </Box>
    )

    cy.get('#combiner-entity-select').realClick()
    cy.get('#react-select-entity-listbox').should('be.visible')

    cy.screenshot('sources-dropdown', { overwrite: true, capture: 'viewport' })
  })

  it('should be accessible', () => {
    cy.injectAxe()
    cy.mountWithProviders(
      <CombinedEntitySelectWrapper
        selectedSources={{
          tags: [{ id: 'opcua-1/power/off', type: DataIdentifierReference.type.TAG, scope: null }],
          topicFilters: [{ id: 'topicFilter/t3', type: DataIdentifierReference.type.TOPIC_FILTER, scope: null }],
        }}
        onChange={cy.stub}
      />
    )

    cy.get('#combiner-entity-select').click()
    cy.checkAccessibility(undefined, {
      rules: {
        // TODO[NVL] ReactSelect not tagging properly the listbox
        'aria-input-field-name': { enabled: false },
      },
    })
  })
})
