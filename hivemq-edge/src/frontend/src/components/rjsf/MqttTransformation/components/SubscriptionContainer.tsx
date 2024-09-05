import { FC, useState } from 'react'
import { Box, Button, ButtonGroup, HStack, Stack, VStack } from '@chakra-ui/react'

import DataModelDestination from '@/components/rjsf/MqttTransformation/components/DataModelDestination.tsx'
import DataModelSources from '@/components/rjsf/MqttTransformation/components/DataModelSources.tsx'
import MappingEditor from '@/components/rjsf/MqttTransformation/components/MappingEditor.tsx'
import SourceSelector from '@/components/rjsf/MqttTransformation/components/SourceSelector.tsx'
import { OutwardSubscription } from '@/modules/Subscriptions/types.ts'
import { JsonNode } from '@/api/__generated__'
import { useMappingValidation } from '@/components/rjsf/MqttTransformation/hooks/useMappingValidation.tsx'

export enum MappingStrategy {
  EXACT = 'EXACT',
  TYPED = 'TYPED',
  TRANSFORMED = 'TRANSFORMED',
}

interface SubscriptionContainerProps {
  item: OutwardSubscription
  onClose: () => void
  onSubmit: (newItem: OutwardSubscription) => void
  onChange: (id: keyof OutwardSubscription, v: JsonNode | string | string[] | undefined) => void
}

const SubscriptionContainer: FC<SubscriptionContainerProps> = ({ item, onClose, onSubmit, onChange }) => {
  const [strategy] = useState<MappingStrategy>(MappingStrategy.TYPED)
  const validation = useMappingValidation(item)

  return (
    <VStack alignItems="stretch" gap={4}>
      <Stack gap={2} flexDirection="row">
        <VStack flex={1} alignItems="stretch">
          <SourceSelector topics={item['mqtt-topic']} multiple onChange={(v) => onChange('mqtt-topic', v)} />
          <DataModelSources flex={1} topics={item['mqtt-topic']} />
        </VStack>
        <VStack flex={2} alignItems="stretch">
          <Box maxW="50%">
            <SourceSelector isTag topics={[item.node]} onChange={(v) => onChange('node', v)} />
          </Box>
          <HStack alignItems="stretch">
            {strategy != MappingStrategy.EXACT && (
              <MappingEditor
                flex={1}
                topic={item.node}
                mapping={item.mapping}
                showTransformation={strategy === MappingStrategy.TRANSFORMED}
                onChange={(m) => onChange('mapping', m)}
              />
            )}
            <DataModelDestination flex={1} topic={item.node} validation={validation} />
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

export default SubscriptionContainer
