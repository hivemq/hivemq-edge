/// <reference types="cypress" />

import type { FC, PropsWithChildren } from 'react'
import { Box, FormControl, FormLabel } from '@chakra-ui/react'
import type { MultiValue } from 'chakra-react-select'

import { MockAdapterType } from '@/__test-utils__/adapters/types'
import { useGetCombinedEntities } from '@/api/hooks/useDomainModel/useGetCombinedEntities'
import { mockCombiner } from '@/api/hooks/useCombiners/__handlers__'
import { MOCK_DEVICE_TAGS } from '@/api/hooks/useProtocolAdapters/__handlers__'

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
          formContext={{ queries: sources, entities: mockCombiner.sources.items }}
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
    cy.get('#combiner-entity-select').click()
    cy.get('#react-select-entity-listbox').find('[role="option').as('options')
    cy.get('@options').should('have.length', 4)
    cy.get('@options').eq(0).should('have.text', 'my-adapter/power/off')
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
