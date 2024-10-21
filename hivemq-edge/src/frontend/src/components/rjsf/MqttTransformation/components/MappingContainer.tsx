import { FC, useState } from 'react'
import { Box, Button, ButtonGroup, HStack, Stack, VStack } from '@chakra-ui/react'

import { JsonNode } from '@/api/__generated__'
import DataModelDestination from '@/components/rjsf/MqttTransformation/components/DataModelDestination.tsx'
import DataModelSources from '@/components/rjsf/MqttTransformation/components/DataModelSources.tsx'
import MappingEditor from '@/components/rjsf/MqttTransformation/components/MappingEditor.tsx'
import {
  SelectDestinationTag,
  SelectSourceTopics,
} from '@/components/rjsf/MqttTransformation/components/EntitySelector.tsx'
import { useMappingValidation } from '@/components/rjsf/MqttTransformation/hooks/useMappingValidation.tsx'
import { OutwardMapping } from '@/modules/Mappings/types.ts'

export enum MappingStrategy {
  EXACT = 'EXACT',
  TYPED = 'TYPED',
  TRANSFORMED = 'TRANSFORMED',
}

interface SubscriptionContainerProps {
  item: OutwardMapping
  adapterType?: string
  adapterId?: string
  onClose: () => void
  onSubmit: (newItem: OutwardMapping) => void
  onChange: (id: keyof OutwardMapping, v: JsonNode | string | string[] | undefined) => void
}

const MappingContainer: FC<SubscriptionContainerProps> = ({
  adapterId,
  adapterType,
  item,
  onClose,
  onSubmit,
  onChange,
}) => {
  const [strategy] = useState<MappingStrategy>(MappingStrategy.TYPED)
  const validation = useMappingValidation(item)

  return (
    <VStack alignItems="stretch" gap={4}>
      <Stack gap={2} flexDirection="row">
        <VStack flex={1} alignItems="stretch" maxW="40vw">
          <Box>
            <SelectSourceTopics value={item.mqttTopicFilter} onChange={(v) => onChange('mqttTopicFilter', v)} />
          </Box>
          <DataModelSources flex={1} topics={[item.mqttTopicFilter]} />
        </VStack>
        <VStack flex={2} alignItems="stretch">
          <Box maxW="50%">
            <SelectDestinationTag
              adapterId={adapterId}
              adapterType={adapterType}
              value={item.tag}
              onChange={(v) => onChange('tag', v)}
            />
          </Box>
          <HStack alignItems="stretch">
            {strategy != MappingStrategy.EXACT && (
              <MappingEditor
                flex={1}
                topic={item.tag}
                mapping={item.fieldMapping}
                showTransformation={strategy === MappingStrategy.TRANSFORMED}
                onChange={(mappings) => onChange('fieldMapping', mappings)}
              />
            )}
            <DataModelDestination flex={1} topic={item.tag} validation={validation} />
          </HStack>
        </VStack>
      </Stack>
      <HStack justifyContent="flex-end">
        <ButtonGroup size="sm">
          <Button onClick={onClose}>Cancel</Button>
          <Button onClick={() => onSubmit(item)} variant="primary">
            Save
          </Button>
        </ButtonGroup>
      </HStack>
    </VStack>
  )
}

export default MappingContainer
