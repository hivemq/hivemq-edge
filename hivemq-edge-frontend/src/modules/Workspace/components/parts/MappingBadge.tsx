import type { FC } from 'react'
import { useMemo } from 'react'
import { HStack, Tag, TagLabel, VStack } from '@chakra-ui/react'
import { PLCTag, Topic, TopicFilter } from '@/components/MQTT/EntityTag.tsx'
import { SelectEntityType } from '@/components/MQTT/types'

const MAX_MAPPINGS = 2

interface MappingBadgeProps {
  destinations: string[]
  type: SelectEntityType
}

const MappingBadge: FC<MappingBadgeProps> = ({ destinations, type }) => {
  const Component = useMemo(() => {
    switch (type) {
      case SelectEntityType.TOPIC:
        return Topic
      case SelectEntityType.TAG:
        return PLCTag
      case SelectEntityType.TOPIC_FILTER:
      default:
        return TopicFilter
    }
  }, [type])

  return (
    <HStack spacing="4" data-testid="topics-container">
      <VStack alignItems="flex-start">
        {destinations.slice(0, MAX_MAPPINGS).map((destination, i) => (
          <Component key={`local-${i}`} tagTitle={destination} />
        ))}
      </VStack>
      {destinations.length > MAX_MAPPINGS && (
        <Tag size="sm" data-testid="topics-show-more">
          <TagLabel>+{destinations.length - MAX_MAPPINGS}</TagLabel>
        </Tag>
      )}
    </HStack>
  )
}

export default MappingBadge
