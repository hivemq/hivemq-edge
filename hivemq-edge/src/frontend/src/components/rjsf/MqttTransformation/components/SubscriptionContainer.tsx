import { FC } from 'react'
import { Button, ButtonGroup, HStack, Stack, VStack } from '@chakra-ui/react'

import DataModelDestination from '@/components/rjsf/MqttTransformation/components/DataModelDestination.tsx'
import DataModelSources from '@/components/rjsf/MqttTransformation/components/DataModelSources.tsx'
import MappingEditor from '@/components/rjsf/MqttTransformation/components/MappingEditor.tsx'
import SourceSelector from '@/components/rjsf/MqttTransformation/components/SourceSelector.tsx'
import { OutwardSubscription } from '@/modules/Subscriptions/types.ts'
import { JsonNode } from '@/api/__generated__'

interface SubscriptionContainerProps {
  item: OutwardSubscription
  onClose: () => void
  onSubmit: (newItem: OutwardSubscription) => void
  onChange: (id: keyof OutwardSubscription, v: JsonNode | string | string[] | undefined) => void
}

const SubscriptionContainer: FC<SubscriptionContainerProps> = ({ item, onClose, onSubmit, onChange }) => {
  return (
    <VStack alignItems="stretch" gap={4}>
      <Stack gap={2} flexDirection="row">
        <VStack flex={2} alignItems="stretch">
          <SourceSelector topics={item['mqtt-topic']} multiple onChange={(v) => onChange('mqtt-topic', v)} />
          <DataModelSources flex={1} topics={item['mqtt-topic']} />
        </VStack>
        <MappingEditor flex={3} />
        <VStack flex={2} alignItems="stretch">
          <SourceSelector isTag topics={[item.node]} onChange={(v) => onChange('node', v)} />
          <DataModelDestination flex={1} topic={item.node} />
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
