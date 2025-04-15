import type { FC } from 'react'
import { useMemo } from 'react'
import { Box, Card, CardBody, CardHeader, Code, chakra as Chakra, List, ListItem } from '@chakra-ui/react'

import { MockAdapterType } from '@/__test-utils__/adapters/types'
import type { DataCombining, Instruction } from '@/api/__generated__'
import { DataIdentifierReference } from '@/api/__generated__'
import { mockCombiner } from '@/api/hooks/useCombiners/__handlers__'
import { useGetCombinedEntities } from '@/api/hooks/useDomainModel/useGetCombinedEntities'
import { GENERATE_DATA_MODELS } from '@/api/hooks/useDomainModel/__handlers__'
import { MOCK_DEVICE_TAGS } from '@/api/hooks/useProtocolAdapters/__handlers__'
import { MOCK_TOPIC_FILTER_SCHEMA_VALID } from '@/api/hooks/useTopicFilters/__handlers__'
import { getPropertyListFrom } from '@/components/rjsf/MqttTransformation/utils/json-schema.utils'
import { validateSchemaFromDataURI } from '@/modules/TopicFilters/utils/topic-filter.schema'

import { AutoMapping } from './AutoMapping'

const mockFormData: DataCombining = {
  id: '58677276-fc48-4a9a-880c-41c755f5063b',
  sources: {
    primary: { id: '', type: DataIdentifierReference.type.TAG },
    tags: ['my-adapter/power/off'],
  },
  destination: { topic: 'my/topic', schema: MOCK_TOPIC_FILTER_SCHEMA_VALID },
  instructions: [
    {
      source: '$.dropped-property',
      destination: '$.lastName',
    },
  ],
}

interface AutoMappingWrapperProps {
  formData?: DataCombining
  onChange?: (instructions: Instruction[]) => void
}

const AutoMappingWrapper: FC<AutoMappingWrapperProps> = ({ formData, onChange }) => {
  const sources = useGetCombinedEntities(mockCombiner.sources.items)

  const props = useMemo(() => {
    if (!formData?.destination?.schema) return []

    const handler = validateSchemaFromDataURI(formData?.destination?.schema)
    return handler.schema ? getPropertyListFrom(handler.schema) : []
  }, [formData?.destination?.schema])

  return (
    <Box>
      <AutoMapping
        formData={formData}
        formContext={{ queries: sources, entities: mockCombiner.sources.items }}
        onChange={onChange}
      />
      <Card mt={50} variant="filled" size="sm">
        <CardHeader>Testing Dashboard</CardHeader>
        <CardBody data-testid="test-context" as={Code}>
          <List>
            <ListItem>source: {sources.length}</ListItem>
            <ListItem>destination:{props.length}</ListItem>
          </List>
        </CardBody>
      </Card>
    </Box>
  )
}

describe('AutoMapping', () => {
  beforeEach(() => {
    cy.viewport(800, 800)

    cy.intercept('/api/v1/management/protocol-adapters/adapters/my-adapter/tags', {
      items: MOCK_DEVICE_TAGS('my-adapter', MockAdapterType.OPC_UA),
    })
    cy.intercept('/api/v1/management/protocol-adapters/adapters/my-other-adapter/tags', {
      items: MOCK_DEVICE_TAGS('my-other-adapter', MockAdapterType.OPC_UA),
    })

    cy.intercept(
      '/api/v1/management/protocol-adapters/writing-schema/**',
      GENERATE_DATA_MODELS(true, 'my-adapter/power/off')
    )
  })

  it('should render properly', () => {
    const onClick = cy.stub().as('onClick')

    cy.mountWithProviders(<AutoMappingWrapper onChange={onClick} formData={mockFormData} />)

    cy.get('button').should('not.be.disabled').should('have.attr', 'aria-label', 'Suggest appropriate mappings')

    cy.get('@onClick').should('not.have.been.called')
    cy.get('button').click()
    cy.get('@onClick').should('have.been.calledWith', [
      {
        sourceRef: {
          id: 'my-adapter/power/off',
          type: 'TAG',
        },
        destination: '$.description',
        source: '$.firstName',
      },
      {
        sourceRef: {
          id: 'my-adapter/power/off',
          type: 'TAG',
        },
        destination: '$.name',
        source: '$.subItems.name',
      },
    ])
  })

  it('should render properly', () => {
    const onClick = cy.stub().as('onClick')

    cy.mountWithProviders(<AutoMappingWrapper onChange={onClick} />)

    cy.get('button').should('be.disabled')
  })

  it('should be accessible', () => {
    cy.injectAxe()
    cy.mountWithProviders(<AutoMappingWrapper onChange={cy.stub} formData={mockFormData} />)

    cy.checkAccessibility()
  })
})
