import { FC } from 'react'
import { OutwardSubscription } from '@/modules/Subscriptions/types.ts'
import { Button, ButtonGroup, HStack, Stack, VStack } from '@chakra-ui/react'
import DataModelDestination from '@/components/rjsf/MqttTransformation/components/DataModelDestination.tsx'
import DataModelSources from '@/components/rjsf/MqttTransformation/components/DataModelSources.tsx'
import MappingEditor from '@/components/rjsf/MqttTransformation/components/MappingEditor.tsx'

interface SubscriptionContainerProps {
  item: OutwardSubscription
  onClose: () => void
  onSubmit: (newItem: OutwardSubscription) => void
}

const SubscriptionContainer: FC<SubscriptionContainerProps> = ({ item, onClose, onSubmit }) => {
  return (
    <VStack alignItems="stretch" gap={4}>
      <Stack gap={2} flexDirection="row">
        <DataModelSources flex={2} />
        <MappingEditor flex={3} />
        <DataModelDestination flex={2} />
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
