import { FC, useState } from 'react'
import { FaRightFromBracket } from 'react-icons/fa6'
import { Button, ButtonGroup, HStack, Icon, Stack, VStack } from '@chakra-ui/react'

import { JsonNode } from '@/api/__generated__'
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
          <SelectSourceTopics value={item.mqttTopicFilter} onChange={(v) => onChange('mqttTopicFilter', v)} />
          <DataModelSources flex={1} topics={item.mqttTopicFilter ? [item.mqttTopicFilter] : []} minH={250} />
        </VStack>
        <VStack justifyContent="flex-start">
          <HStack height={38}>
            <Icon as={FaRightFromBracket} />
          </HStack>
        </VStack>
        <VStack flex={1} alignItems="stretch" maxW="50vw">
          <SelectDestinationTag
            adapterId={adapterId}
            adapterType={adapterType}
            value={item.tag}
            onChange={(v) => onChange('tag', v)}
          />
          <MappingEditor
            flex={1}
            topic={item.tag}
            mapping={item.fieldMapping}
            showTransformation={strategy === MappingStrategy.TRANSFORMED}
            onChange={(mappings) => onChange('fieldMapping', mappings)}
          />
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
