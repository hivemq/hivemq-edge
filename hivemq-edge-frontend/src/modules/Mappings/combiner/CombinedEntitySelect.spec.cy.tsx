import type { FC, PropsWithChildren } from 'react'
import { Box, FormControl, FormLabel } from '@chakra-ui/react'
import type { MultiValue } from 'chakra-react-select'
import type { UseQueryResult } from '@tanstack/react-query'

import { MockAdapterType } from '@/__test-utils__/adapters/types'
import { useGetCombinedEntities } from '@/api/hooks/useDomainModel/useGetCombinedEntities'
import { mockCombiner } from '@/api/hooks/useCombiners/__handlers__'
import { MOCK_DEVICE_TAGS } from '@/api/hooks/useProtocolAdapters/__handlers__'
import type { DomainTag, DomainTagList } from '@/api/__generated__'
import { EntityType } from '@/api/__generated__'
import type { CombinerContext } from '@/modules/Mappings/types'

import CombinedEntitySelect from './CombinedEntitySelect'

interface EntityReferenceSelectProps {
  tags?: Array<string>
  topicFilters?: Array<string>
  onChange: (value: MultiValue<unknown>) => void
}

const CombinedEntitySelectWrapper: FC<PropsWithChildren<EntityReferenceSelectProps>> = ({
  children,
  tags,
  topicFilters,
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
          tags={tags}
          topicFilters={topicFilters}
          onChange={onChange}
          formContext={{
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

    cy.mountWithProviders(
      <CombinedEntitySelectWrapper tags={['opcua-1/power/off']} topicFilters={['topicFilter/t3']} onChange={onChange} />
    )

    cy.get('#combiner-entity-select')
      .should('contain.text', 'opcua-1/power/off')
      .should('contain.text', 'topicFilter/t3')
    cy.get('#combiner-entity-select').realClick()
    cy.get('#combiner-entity-select').type('m')
    cy.get('#react-select-entity-listbox').find('[role="option"]').as('options')
    cy.get('@options').should('have.length', 4)
    cy.get('@options').eq(0).should('contain.text', 'my-adapter/power/off').should('contain.text', 'Tag')
  })

  it('should handle duplicate tag names from different adapters', () => {
    // Helper to create mock query with tags
    const mockTagQuery = (tags: DomainTag[]): Partial<UseQueryResult<DomainTagList, Error>> => {
      return {
        data: { items: tags },
        isLoading: false,
        isSuccess: true,
        isError: false,
      }
    }

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
          <CombinedEntitySelect
            id="duplicate-test"
            formContext={contextWithDuplicates}
            tags={[]} // Start with empty selection
            onChange={onChange}
          />
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

  it('should be accessible', () => {
    cy.injectAxe()
    cy.mountWithProviders(
      <CombinedEntitySelectWrapper tags={['opcua-1/power/off']} topicFilters={['topicFilter/t3']} onChange={cy.stub} />
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
