import { FC } from 'react'
import { HStack, Tag, TagLabel, VStack } from '@chakra-ui/react'
import { PLCTag, Topic } from '@/components/MQTT/EntityTag.tsx'

const MAX_MAPPINGS = 2

interface MappingBadgeProps {
  destinations: string[]
  isTag?: boolean
}

const MappingBadge: FC<MappingBadgeProps> = ({ destinations, isTag = false }) => {
  const Component = isTag ? PLCTag : Topic
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
